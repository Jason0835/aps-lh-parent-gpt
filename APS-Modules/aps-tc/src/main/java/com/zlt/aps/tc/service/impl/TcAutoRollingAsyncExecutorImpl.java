package com.zlt.aps.tc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.domain.vo.TcRollingTaskVo;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcStockMapper;
import com.zlt.aps.tc.service.TcAutoRollingAsyncExecutor;
import com.zlt.aps.tc.service.TcBackgroundTaskService;
import com.zlt.aps.tc.service.loader.TcAutoScheduleDataLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧自动滚动库存上下界计算和任务链调整执行器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcAutoRollingAsyncExecutorImpl implements TcAutoRollingAsyncExecutor {

    private final TcBackgroundTaskService backgroundTaskService;
    private final TcAutoScheduleDataLoadService dataLoadService;
    private final TcScheduleResultMapper scheduleResultMapper;
    private final TcStockMapper stockMapper;
    private final TcParamsMapper paramsMapper;
    private final TcManualOperationFacade manualOperationFacade;

    /**
     * 异步执行目标班次滚动调量。
     *
     * @param taskId 任务ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        if (!this.backgroundTaskService.start(taskId, TcScheduleConstants.ROLLING_STAGE_CALCULATING,
                I18nUtil.getMessage("ui.tc.schedule.rolling.calculating"))) {
            return;
        }
        TcAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        try {
            TcRollingTaskVo result = this.calculateAndAdjust(task);
            this.backgroundTaskService.markSuccess(taskId, result, result.getSummary(), result.getIssues());
        } catch (Exception exception) {
            log.error("胎侧自动滚动执行失败，taskId={}", taskId, exception);
            TcAutoScheduleIssueVo issue = this.buildIssue("ROLLING_FAILED", exception.getMessage());
            this.backgroundTaskService.markFailed(taskId, exception.getMessage(), Collections.emptyMap(),
                    Collections.singletonList(issue));
        }
    }

    /**
     * 加载需求、库存与当前批次，计算上下界并复用人工滚动核心调量。
     *
     * @param task 自动滚动任务
     * @return 滚动结果
     */
    private TcRollingTaskVo calculateAndAdjust(TcAutoScheduleTask task) {
        if (task == null || task.getTargetShiftOrder() == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.rolling.taskInvalid"));
        }
        TcScheduleContext context = new TcScheduleContext();
        context.setTaskId(task.getTaskId());
        context.setFactoryCode(task.getFactoryCode());
        context.setScheduleDate(task.getScheduleDate());
        context.setBatchNo(task.getBatchNo());
        context.setTraceId(task.getTraceId());
        context.setOperator("AUTO_ROLLING");
        this.dataLoadService.loadAllData(context);
        int targetShiftOrder = task.getTargetShiftOrder();
        Map<String, BigDecimal> demandMap = context.getTaskDraftList().stream()
                .filter(draft -> Objects.equals(targetShiftOrder, draft.getShiftOrder()))
                .filter(draft -> StrUtil.isNotBlank(draft.getSidewallCode()))
                .collect(Collectors.groupingBy(TcTaskDraft::getSidewallCode, LinkedHashMap::new,
                        Collectors.mapping(draft -> BigDecimalUtils.valueOf(draft.getCurrentShiftDemandQty()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        Map<String, BigDecimal> stockMap = this.loadStockMap(task);
        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(
                new LambdaQueryWrapper<TcScheduleResult>()
                        .eq(TcScheduleResult::getFactoryCode, task.getFactoryCode())
                        .eq(TcScheduleResult::getScheduleDate, task.getScheduleDate())
                        .eq(TcScheduleResult::getBatchNo, task.getBatchNo())
                        .orderByAsc(TcScheduleResult::getId));
        Map<String, List<TcScheduleResult>> resultGroupMap = CollectionUtils.emptyIfNull(resultList).stream()
                .filter(result -> StrUtil.isNotBlank(result.getSidewallCode()))
                .collect(Collectors.groupingBy(TcScheduleResult::getSidewallCode,
                        LinkedHashMap::new, Collectors.toList()));
        int maxStockShiftCount = this.resolveMaxStockShiftCount(task);
        BigDecimal increasedQty = BigDecimal.ZERO;
        BigDecimal reducedQty = BigDecimal.ZERO;
        int adjustedSidewallCount = 0;
        int affectedResultCount = 0;
        List<TcAutoScheduleIssueVo> issueList = new ArrayList<>();
        List<Map<String, Object>> adjustmentList = new ArrayList<>();

        for (Map.Entry<String, List<TcScheduleResult>> entry : resultGroupMap.entrySet()) {
            String sidewallCode = entry.getKey();
            BigDecimal demandQty = demandMap.get(sidewallCode);
            if (demandQty == null || demandQty.signum() <= 0) {
                issueList.add(this.buildIssue("DEMAND_MISSING", sidewallCode));
                continue;
            }
            List<TcScheduleResult> sidewallResultList = entry.getValue();
            if (sidewallResultList.stream().anyMatch(result -> "3".equals(result.getReleaseStatus())
                    || "4".equals(result.getReleaseStatus()))) {
                issueList.add(this.buildIssue("RELEASE_STATUS_BLOCKED", sidewallCode));
                continue;
            }
            BigDecimal stockQty = stockMap.getOrDefault(sidewallCode, BigDecimal.ZERO);
            BigDecimal oldPlanQty = sidewallResultList.stream()
                    .map(result -> this.readQty(result, targetShiftOrder,
                            TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal finishQty = sidewallResultList.stream()
                    .map(result -> this.readQty(result, targetShiftOrder,
                            TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal desiredPlanQty = this.calculateDesiredPlanQty(stockQty, oldPlanQty,
                    demandQty, maxStockShiftCount).max(finishQty);
            int compareResult = desiredPlanQty.compareTo(oldPlanQty);
            if (compareResult == 0) {
                continue;
            }
            BigDecimal changedQty = desiredPlanQty.subtract(oldPlanQty).abs();
            int currentAffectedCount;
            if (compareResult > 0) {
                currentAffectedCount = this.increasePlan(task, sidewallResultList,
                        targetShiftOrder, changedQty);
                increasedQty = increasedQty.add(changedQty);
            } else {
                currentAffectedCount = this.reducePlan(task, sidewallResultList,
                        targetShiftOrder, changedQty);
                reducedQty = reducedQty.add(changedQty);
            }
            adjustedSidewallCount++;
            affectedResultCount += currentAffectedCount;
            adjustmentList.add(this.buildAdjustment(sidewallCode, stockQty, demandQty,
                    oldPlanQty, desiredPlanQty));
        }
        this.backgroundTaskService.updateProgress(task.getTaskId(), 90,
                TcScheduleConstants.ROLLING_STAGE_PERSISTING,
                I18nUtil.getMessage("ui.tc.schedule.rolling.persisting"), null);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("targetShiftOrder", targetShiftOrder);
        summary.put("maxStockShiftCount", maxStockShiftCount);
        summary.put("adjustedSidewallCount", adjustedSidewallCount);
        summary.put("affectedResultCount", affectedResultCount);
        summary.put("increasedQty", increasedQty);
        summary.put("reducedQty", reducedQty);
        summary.put("adjustments", adjustmentList);
        TcRollingTaskVo response = new TcRollingTaskVo();
        response.setTaskId(task.getTaskId());
        response.setTaskStatus("SUCCESS");
        response.setProgress(100);
        response.setCurrentStage(TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE);
        response.setFactoryCode(task.getFactoryCode());
        response.setTargetShiftOrder(targetShiftOrder);
        response.setInputVersion(task.getInputVersion());
        response.setIncreasedQty(increasedQty);
        response.setReducedQty(reducedQty);
        response.setIssues(issueList);
        response.setSummary(summary);
        return response;
    }

    /**
     * 调增一个胎侧任务并让横表滚动服务承接溢出。
     *
     * @param task 自动滚动任务
     * @param resultList 胎侧结果
     * @param shiftOrder 班次顺序
     * @param increaseQty 调增量
     * @return 受影响结果数
     */
    private int increasePlan(TcAutoScheduleTask task, List<TcScheduleResult> resultList,
                             int shiftOrder, BigDecimal increaseQty) {
        TcScheduleResult selectedResult = resultList.stream()
                .max(Comparator.comparing(result -> this.readSequence(result, shiftOrder)))
                .orElseThrow(IllegalStateException::new);
        TcScheduleResult current = this.scheduleResultMapper.selectById(selectedResult.getId());
        BigDecimal newPlanQty = this.readQty(current, shiftOrder,
                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE).add(increaseQty);
        return this.changeSingleResult(task, current, shiftOrder, newPlanQty);
    }

    /**
     * 从后向前按可减量调减任务，保证每条结果不低于完成量。
     *
     * @param task 自动滚动任务
     * @param resultList 胎侧结果
     * @param shiftOrder 班次顺序
     * @param reduceQty 调减量
     * @return 受影响结果数
     */
    private int reducePlan(TcAutoScheduleTask task, List<TcScheduleResult> resultList,
                           int shiftOrder, BigDecimal reduceQty) {
        List<TcScheduleResult> sortedList = resultList.stream()
                .sorted(Comparator.comparing((TcScheduleResult result) -> this.readSequence(result, shiftOrder))
                        .reversed()).collect(Collectors.toList());
        BigDecimal remainingQty = reduceQty;
        int affectedCount = 0;
        for (TcScheduleResult source : sortedList) {
            if (remainingQty.signum() <= 0) {
                break;
            }
            TcScheduleResult current = this.scheduleResultMapper.selectById(source.getId());
            if (current == null) {
                continue;
            }
            BigDecimal planQty = this.readQty(current, shiftOrder,
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE);
            BigDecimal finishQty = this.readQty(current, shiftOrder,
                    TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE);
            BigDecimal reducibleQty = planQty.subtract(finishQty).max(BigDecimal.ZERO);
            BigDecimal currentReduceQty = remainingQty.min(reducibleQty);
            if (currentReduceQty.signum() <= 0) {
                continue;
            }
            affectedCount += this.changeSingleResult(task, current, shiftOrder,
                    planQty.subtract(currentReduceQty));
            remainingQty = remainingQty.subtract(currentReduceQty);
        }
        return affectedCount;
    }

    /**
     * 复用人工门面执行单行调量。
     *
     * @param task 自动滚动任务
     * @param current 当前结果
     * @param shiftOrder 班次顺序
     * @param newPlanQty 新计划量
     * @return 受影响行数
     */
    private int changeSingleResult(TcAutoScheduleTask task, TcScheduleResult current,
                                   int shiftOrder, BigDecimal newPlanQty) {
        TcScheduleResult changeResult = new TcScheduleResult();
        changeResult.setId(current.getId());
        changeResult.setFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), newPlanQty);
        return this.manualOperationFacade.changeQtyForAutoRolling(changeResult,
                current.getTaskVersion() == null ? 0L : current.getTaskVersion(),
                I18nUtil.getMessage("ui.tc.schedule.rolling.adjustReason"), task.getTaskId());
    }

    /**
     * 计算库存上下界约束后的目标计划量。
     *
     * @param stockQty 预计库存
     * @param oldPlanQty 原计划量
     * @param demandQty 一个班需求量
     * @param maxStockShiftCount 库存上限班次数
     * @return 目标计划量
     */
    BigDecimal calculateDesiredPlanQty(BigDecimal stockQty, BigDecimal oldPlanQty,
                                       BigDecimal demandQty, int maxStockShiftCount) {
        BigDecimal availableQty = stockQty.add(oldPlanQty);
        if (availableQty.compareTo(demandQty) < 0) {
            return demandQty.subtract(stockQty).max(BigDecimal.ZERO);
        }
        BigDecimal maxStockQty = demandQty.multiply(BigDecimal.valueOf(maxStockShiftCount));
        if (availableQty.compareTo(maxStockQty) > 0) {
            return maxStockQty.subtract(stockQty).max(BigDecimal.ZERO);
        }
        return oldPlanQty;
    }

    /**
     * 查询目标日期库存并按胎侧汇总有效数量。
     *
     * @param task 自动滚动任务
     * @return 胎侧库存映射
     */
    private Map<String, BigDecimal> loadStockMap(TcAutoScheduleTask task) {
        List<TcStock> stockList = this.stockMapper.selectList(new LambdaQueryWrapper<TcStock>()
                .eq(TcStock::getFactoryCode, task.getFactoryCode())
                .eq(TcStock::getStockDate, task.getScheduleDate()));
        return CollectionUtils.emptyIfNull(stockList).stream()
                .filter(stock -> StrUtil.isNotBlank(stock.getSidewallCode()))
                .collect(Collectors.groupingBy(TcStock::getSidewallCode, LinkedHashMap::new,
                        Collectors.mapping(stock -> BigDecimalUtils.valueOf(stock.getStockQty())
                                        .add(BigDecimalUtils.valueOf(stock.getAdjustQty()))
                                        .subtract(BigDecimalUtils.valueOf(stock.getBadQty())),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    /**
     * 读取库存上限班次数参数。
     *
     * @param task 自动滚动任务
     * @return 正整数班次数
     */
    private int resolveMaxStockShiftCount(TcAutoScheduleTask task) {
        TcParams params = this.paramsMapper.selectOne(new LambdaQueryWrapper<TcParams>()
                .eq(TcParams::getFactoryCode, task.getFactoryCode())
                .eq(TcParams::getParamCode, TcScheduleConstants.PARAM_AUTO_ROLLING_MAX_STOCK_CLASS)
                .eq(TcParams::getEnableStatus, "1")
                .last("limit 1"));
        String value = params == null ? TcScheduleConstants.DEFAULT_AUTO_ROLLING_MAX_STOCK_CLASS
                : StrUtil.blankToDefault(params.getParamValue(),
                TcScheduleConstants.DEFAULT_AUTO_ROLLING_MAX_STOCK_CLASS);
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return Integer.parseInt(TcScheduleConstants.DEFAULT_AUTO_ROLLING_MAX_STOCK_CLASS);
        }
    }

    /**
     * 动态读取计划量或完成量。
     *
     * @param result 结果
     * @param shiftOrder 班次顺序
     * @param fieldTemplate 字段模板
     * @return 数量
     */
    private BigDecimal readQty(TcScheduleResult result, int shiftOrder, String fieldTemplate) {
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(
                String.format(fieldTemplate, shiftOrder)));
    }

    /**
     * 动态读取班内顺序，空顺序排在最后。
     *
     * @param result 结果
     * @param shiftOrder 班次顺序
     * @return 顺序
     */
    private int readSequence(TcScheduleResult result, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return value instanceof Number ? ((Number) value).intValue() : Integer.MAX_VALUE;
    }

    /**
     * 构造滚动调整证据。
     *
     * @param sidewallCode 胎侧编码
     * @param stockQty 库存
     * @param demandQty 班需求
     * @param oldPlanQty 原计划
     * @param newPlanQty 新计划
     * @return 证据
     */
    private Map<String, Object> buildAdjustment(String sidewallCode, BigDecimal stockQty,
                                                BigDecimal demandQty, BigDecimal oldPlanQty,
                                                BigDecimal newPlanQty) {
        Map<String, Object> adjustment = new LinkedHashMap<>();
        adjustment.put("sidewallCode", sidewallCode);
        adjustment.put("stockQty", stockQty);
        adjustment.put("demandQty", demandQty);
        adjustment.put("oldPlanQty", oldPlanQty);
        adjustment.put("newPlanQty", newPlanQty);
        adjustment.put("reason", newPlanQty.compareTo(oldPlanQty) > 0
                ? "LOWER_STOCK_BOUND" : "UPPER_STOCK_BOUND");
        return adjustment;
    }

    /**
     * 构造结构化滚动问题。
     *
     * @param category 问题分类
     * @param message 问题说明
     * @return 问题
     */
    private TcAutoScheduleIssueVo buildIssue(String category, String message) {
        TcAutoScheduleIssueVo issue = new TcAutoScheduleIssueVo();
        issue.setLevel("WARN");
        issue.setStageCode(TcScheduleConstants.ROLLING_STAGE_CALCULATING);
        issue.setStageName(I18nUtil.getMessage("ui.tc.schedule.rolling.calculating"));
        issue.setCategory(category);
        issue.setMessage(message);
        return issue;
    }
}
