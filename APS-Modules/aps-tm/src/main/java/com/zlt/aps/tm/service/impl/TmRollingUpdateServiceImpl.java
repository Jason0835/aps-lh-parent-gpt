package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmShiftStock;
import com.zlt.aps.tm.api.domain.vo.TmRollingRecalcResponseVO;
import com.zlt.aps.tm.api.enums.TmReleaseStatusTransition;
import com.zlt.aps.tm.api.enums.TmScheduleEventTypeEnum;
import com.zlt.aps.tm.domain.TmRollingAdjustment;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.event.TmScheduleEvent;
import com.zlt.aps.tm.engine.event.TmScheduleEventPublisher;
import com.zlt.aps.tm.mapper.TmDispatcherLogMapper;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmShiftStockMapper;
import com.zlt.aps.tm.service.ITmRollingUpdateService;
import com.zlt.aps.tm.service.TmRollingWindowService;
import com.zlt.aps.tm.service.loader.TmAutoScheduleDataLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 胎面自动滚动重算服务实现。
 *
 * <p>滚动计算复用自动排程数据加载服务，因此 BOM、RECIPE、班次偏移和施工版本择一口径与自动排程一致。
 * 结果写入复用人工滚动服务，所有目标行锁、调整、未排变化和审计日志在同一个数据库事务内完成。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmRollingUpdateServiceImpl implements ITmRollingUpdateService {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private static final String STATUS_SKIPPED = "SKIPPED";

    private static final String SUMMARY_PREFIX = "ROLLING_SUMMARY=";

    private static final String SKIP_STOCK_MISSING = "STOCK_MISSING";

    private static final String SKIP_TARGET_RESULT_MISSING = "TARGET_RESULT_MISSING";

    private static final String SKIP_DEMAND_MISSING = "DEMAND_WINDOW_MISSING";

    private static final String SKIP_WINDOW_INSUFFICIENT = "DEMAND_WINDOW_INSUFFICIENT";

    private static final String SKIP_THRESHOLD_NOT_REACHED = "THRESHOLD_NOT_REACHED";

    private final RedissonClient redissonClient;

    private final PlatformTransactionManager platformTransactionManager;

    private final TmParamsMapper tmParamsMapper;

    private final TmShiftStockMapper tmShiftStockMapper;

    private final TmRollingWindowService tmRollingWindowService;

    private final TmScheduleResultMapper tmScheduleResultMapper;

    private final TmDispatcherLogMapper tmDispatcherLogMapper;

    private final TmAutoScheduleDataLoadService tmAutoScheduleDataLoadService;

    private final TmManualInsertRollingService tmManualInsertRollingService;

    private final TmScheduleEventPublisher tmScheduleEventPublisher;

    /**
     * 手动执行自动滚动。
     *
     * @param request 滚动请求
     * @return 滚动结果
     * @throws ServiceException 参数、锁、状态或事务执行失败时抛出
     */
    @Override
    public TmRollingRecalcResponseVO rollingRecalc(TmRollingRecalcRequestDTO request) {
        return this.execute(request, false);
    }

    /**
     * 定时执行自动滚动。
     *
     * @param request 滚动请求
     * @return 滚动结果
     * @throws ServiceException 工厂未启用或执行失败时抛出
     */
    @Override
    public TmRollingRecalcResponseVO rollingRecalcAutomatically(TmRollingRecalcRequestDTO request) {
        return this.execute(request, true);
    }

    /**
     * 在分布式锁范围内执行幂等检查、数据加载和事务写入。
     *
     * @param request 滚动请求
     * @param automatic true 表示定时触发
     * @return 滚动结果
     */
    private TmRollingRecalcResponseVO execute(TmRollingRecalcRequestDTO request, boolean automatic) {
        this.validateRequest(request);
        request.setFactoryCode(StrUtil.trim(request.getFactoryCode()));
        request.setScheduleDate(DateUtil.beginOfDay(request.getScheduleDate()));
        this.resolveStockDate(request);
        request.setOperator(StrUtil.blankToDefault(StrUtil.trim(request.getOperator()), automatic ? "TM_ROLLING_JOB" : "TM_ROLLING_API"));
        if (automatic && !this.isRollingEnabled(request.getFactoryCode())) {
            return this.buildDisabledResponse(request);
        }
        this.ensureShiftStockExists(request);

        String runKey = this.buildRunKey(request);
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String lockKey = TmScheduleConstants.ROLLING_LOCK_KEY_PREFIX + request.getFactoryCode() + ":"
                + DateUtil.formatDate(request.getScheduleDate()) + ":" + request.getTargetShiftOrder();
        RLock rollingLock = redissonClient.getLock(lockKey);
        if (!rollingLock.tryLock()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingLocked"));
        }
        try {
            TmRollingRecalcResponseVO existingResponse = this.loadExistingResponse(request, runKey, traceId);
            if (existingResponse != null) {
                return existingResponse;
            }
            TmScheduleContext context = this.loadRollingContext(request, runKey, traceId);
            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            TmRollingRecalcResponseVO response = transactionTemplate.execute(status ->
                    this.executeInsideTransaction(request, context, runKey, traceId));
            if (response == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingFailed"));
            }
            this.publishRollingEvent(request, response, context);
            return response;
        } finally {
            if (rollingLock.isHeldByCurrentThread()) {
                rollingLock.unlock();
            }
        }
    }

    /**
     * 执行数据库事务内的行锁、计算、滚动写入和审计。
     *
     * @param request 滚动请求
     * @param context 自动排程加载上下文
     * @param runKey 运行键
     * @param traceId 追踪号
     * @return 滚动响应
     */
    private TmRollingRecalcResponseVO executeInsideTransaction(TmRollingRecalcRequestDTO request,
                                                                TmScheduleContext context,
                                                                String runKey,
                                                                String traceId) {
        TmRollingRecalcResponseVO existingResponse = this.loadExistingResponse(request, runKey, traceId);
        if (existingResponse != null) {
            return existingResponse;
        }
        List<TmScheduleResult> initialResultList = this.loadScheduleResults(request);
        List<Long> resultIds = initialResultList.stream().map(TmScheduleResult::getId).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList());
        if (!resultIds.isEmpty()) {
            List<TmScheduleResult> lockedResultList = tmScheduleResultMapper.selectBatchIdsForUpdate(resultIds);
            if (lockedResultList == null || lockedResultList.size() != resultIds.size()) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationConcurrentChanged"));
            }
        }
        List<TmScheduleResult> beforeList = this.loadScheduleResults(request);
        List<TmRollingAdjustment> adjustmentList = this.calculateAdjustments(request, context, beforeList);
        this.validateAffectedReleaseStatuses(beforeList, adjustmentList, request.getTargetShiftOrder());

        List<TmScheduleResult> changeRequestList = new ArrayList<>();
        for (TmRollingAdjustment adjustment : adjustmentList) {
            this.appendAdjustmentRequests(request, adjustment, changeRequestList);
        }
        this.validateRollingMachineRelations(context, beforeList, changeRequestList);
        int updateCount = changeRequestList.isEmpty() ? 0
                : tmManualInsertRollingService.changeQtyAndRollBatch(changeRequestList);
        List<TmScheduleResult> afterList = this.loadScheduleResults(request);
        TmRollingRecalcResponseVO response = this.buildResponse(request, runKey, traceId,
                beforeList, afterList, adjustmentList, context, updateCount);
        this.recordRollingLog(request, runKey, beforeList, afterList, response);
        return response;
    }

    /**
     * 校验自动滚动新增计划量仍满足口型板、胶料机台关系。
     *
     * <p>减量、清零和计划量未变化时不阻断；校验复用本次滚动已加载的自动排程关系快照，
     * 保证与自动派机的关系生效条件一致。</p>
     *
     * @param context 自动排程加载上下文
     * @param currentResultList 当前结果快照
     * @param changeRequestList 滚动调量请求
     * @throws ServiceException 调增任务所在机台未命中关系白名单时抛出
     */
    private void validateRollingMachineRelations(TmScheduleContext context,
                                                   List<TmScheduleResult> currentResultList,
                                                   List<TmScheduleResult> changeRequestList) {
        Map<Long, TmScheduleResult> currentResultMap = currentResultList.stream()
                .filter(result -> result.getId() != null)
                .collect(Collectors.toMap(TmScheduleResult::getId, Function.identity(), (first, ignored) -> first));
        for (TmScheduleResult changeRequest : changeRequestList) {
            TmScheduleResult currentResult = currentResultMap.get(changeRequest.getId());
            if (currentResult == null || !this.hasPlanIncrease(currentResult, changeRequest)) {
                continue;
            }
            if (StrUtil.isBlank(currentResult.getMouthPlateCode())) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.mouthPlateInvalid"));
            }
            Optional<TmMachineCandidate> candidateOptional = context.getMachineCandidateList().stream()
                            .filter(candidate -> Objects.equals(candidate.getMachineCode(),
                                    currentResult.getMachineCode()))
                            .findFirst();
            boolean mouthPlateMatched = !context.getConfiguredMouthPlateCodeSet()
                    .contains(currentResult.getMouthPlateCode())
                    || candidateOptional.map(candidate -> candidate.getConfiguredMouthPlateCodes()
                            .contains(currentResult.getMouthPlateCode())).orElse(false);
            if (!mouthPlateMatched) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.mouthPlateRejected"));
            }
            boolean glueMatched = !context.getConfiguredGlueCodeSet().contains(currentResult.getGlueCode())
                    || candidateOptional.map(candidate -> candidate.getConfiguredGlueCodes()
                            .contains(currentResult.getGlueCode())).orElse(false);
            if (!glueMatched) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.glueMachineRejected"));
            }
        }
    }

    /**
     * 判断调量请求是否增加任一班次计划量。
     *
     * @param currentResult 当前数据库结果
     * @param changeRequest 调量请求
     * @return true 表示至少一个班次增加
     */
    private boolean hasPlanIncrease(TmScheduleResult currentResult, TmScheduleResult changeRequest) {
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.readShiftQty(changeRequest, shiftOrder, false)
                    .compareTo(this.readShiftQty(currentResult, shiftOrder, false)) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 使用同一版本匹配加载逻辑构建滚动计算上下文。
     *
     * @param request 滚动请求
     * @param runKey 运行键
     * @param traceId 追踪号
     * @return 已加载任务、参数和班次信息的上下文
     */
    private TmScheduleContext loadRollingContext(TmRollingRecalcRequestDTO request, String runKey, String traceId) {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode(request.getFactoryCode());
        context.setScheduleDate(request.getScheduleDate());
        context.setBatchNo(runKey);
        context.setTraceId(traceId);
        context.setOperator(request.getOperator());
        tmAutoScheduleDataLoadService.loadAllData(context);
        return context;
    }

    /**
     * 按胎面计算上修或下修目标。
     *
     * @param request 滚动请求
     * @param context 加载上下文
     * @param resultList 当前排程结果
     * @return 需要实际修改的胎面调整指令
     */
    private List<TmRollingAdjustment> calculateAdjustments(TmRollingRecalcRequestDTO request,
                                                            TmScheduleContext context,
                                                            List<TmScheduleResult> resultList) {
        Map<String, List<TmTaskDraft>> taskMap = context.getTaskDraftList().stream()
                .filter(Objects::nonNull).filter(task -> StrUtil.isNotBlank(task.getTreadCode()))
                .collect(Collectors.groupingBy(TmTaskDraft::getTreadCode, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<TmScheduleResult>> resultMap = resultList.stream()
                .filter(result -> StrUtil.isNotBlank(result.getTreadCode()))
                .collect(Collectors.groupingBy(TmScheduleResult::getTreadCode, LinkedHashMap::new, Collectors.toList()));
        Map<String, TmShiftStock> stockMap = this.loadStockMap(request);
        Set<String> targetTreadCodes = taskMap.entrySet().stream()
                .filter(entry -> this.sumDemand(entry.getValue(), request.getTargetShiftOrder()).compareTo(BigDecimal.ZERO) > 0)
                .map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));

        BigDecimal upThreshold = this.readPositiveDecimalParam(request.getFactoryCode(),
                TmScheduleConstants.PARAM_ROLLING_UP_THRESHOLD,
                new BigDecimal(TmScheduleConstants.DEFAULT_ROLLING_UP_THRESHOLD));
        BigDecimal downThreshold = this.readPositiveDecimalParam(request.getFactoryCode(),
                TmScheduleConstants.PARAM_ROLLING_DOWN_THRESHOLD,
                new BigDecimal(TmScheduleConstants.DEFAULT_ROLLING_DOWN_THRESHOLD));
        int rollingShiftCount = this.readPositiveIntegerParam(request.getFactoryCode(),
                TmScheduleConstants.PARAM_ROLLING_SHIFT_COUNT,
                TmScheduleConstants.DEFAULT_ROLLING_SHIFT_COUNT);
        BigDecimal downTarget = this.readPositiveDecimalParam(request.getFactoryCode(),
                TmScheduleConstants.PARAM_ROLLING_DOWN_TARGET,
                BigDecimal.valueOf(rollingShiftCount));
        List<TmRollingAdjustment> adjustmentList = new ArrayList<>();
        for (String treadCode : targetTreadCodes) {
            TmShiftStock stock = stockMap.get(treadCode);
            if (stock == null) {
                this.incrementSkip(context, treadCode, SKIP_STOCK_MISSING);
                continue;
            }
            List<TmScheduleResult> treadResultList = resultMap.getOrDefault(treadCode, Collections.emptyList());
            BigDecimal beforePlanQty = this.sumResultQty(treadResultList, request.getTargetShiftOrder(), false);
            if (treadResultList.isEmpty() || beforePlanQty.compareTo(BigDecimal.ZERO) <= 0) {
                this.incrementSkip(context, treadCode, SKIP_TARGET_RESULT_MISSING);
                continue;
            }
            List<TmTaskDraft> treadTaskList = taskMap.getOrDefault(treadCode, Collections.emptyList());
            BigDecimal targetDemand = this.calculateWindowDemand(treadTaskList,
                    request.getTargetShiftOrder(), upThreshold);
            if (targetDemand.compareTo(BigDecimal.ZERO) <= 0) {
                this.incrementSkip(context, treadCode, SKIP_DEMAND_MISSING);
                continue;
            }
            BigDecimal expectedStock = this.calculateExpectedStock(stock);
            BigDecimal availableQty = expectedStock.add(beforePlanQty);
            BigDecimal targetPlanQty = null;
            String direction = null;
            boolean downWindowEnough = this.hasDemandWindow(treadTaskList, request.getTargetShiftOrder(), downThreshold);
            if (availableQty.compareTo(targetDemand) < 0) {
                targetPlanQty = targetDemand.subtract(expectedStock).max(BigDecimal.ZERO);
                direction = "UP";
            } else if (downWindowEnough) {
                BigDecimal downDemand = this.calculateWindowDemand(treadTaskList,
                        request.getTargetShiftOrder(), downThreshold);
                if (availableQty.compareTo(downDemand) > 0) {
                    BigDecimal downTargetDemand = this.calculateWindowDemand(treadTaskList,
                            request.getTargetShiftOrder(), downTarget);
                    targetPlanQty = downTargetDemand.subtract(expectedStock).max(BigDecimal.ZERO);
                    direction = "DOWN";
                }
            } else {
                this.incrementSkip(context, treadCode, SKIP_WINDOW_INSUFFICIENT);
            }
            if (targetPlanQty == null || targetPlanQty.compareTo(beforePlanQty) == 0) {
                if (targetPlanQty == null && downWindowEnough) {
                    this.incrementSkip(context, treadCode, SKIP_THRESHOLD_NOT_REACHED);
                }
                continue;
            }
            BigDecimal targetFinishQty = this.sumResultQty(treadResultList, request.getTargetShiftOrder(), true);
            targetPlanQty = targetPlanQty.max(targetFinishQty);
            if (targetPlanQty.compareTo(beforePlanQty) == 0) {
                this.incrementSkip(context, treadCode, SKIP_THRESHOLD_NOT_REACHED);
                continue;
            }
            TmRollingAdjustment adjustment = new TmRollingAdjustment();
            adjustment.setTreadCode(treadCode);
            adjustment.setBeforePlanQty(beforePlanQty);
            adjustment.setTargetPlanQty(targetPlanQty);
            adjustment.setDirection(direction);
            adjustment.getEvidence().put("expectedStock", expectedStock);
            adjustment.getEvidence().put("beforePlanQty", beforePlanQty);
            adjustment.getEvidence().put("availableQty", availableQty);
            adjustment.getEvidence().put("upThresholdDemand", targetDemand);
            adjustment.getEvidence().put("downWindowEnough", downWindowEnough);
            adjustment.getEvidence().put("rollingShiftCount", rollingShiftCount);
            adjustment.getEvidence().put("targetPlanQty", targetPlanQty);
            adjustmentList.add(adjustment);
        }
        context.getCurrentDayShutdownEvidenceMap().put(request.getTargetShiftOrder(),
                this.extractRollingSkipEvidence(context));
        return adjustmentList;
    }

    /**
     * 按目标胎面和班次生成调量命令，全部调整在同一运行态上下文计算。
     *
     * @param request 滚动请求
     * @param adjustment 调整指令
     * @param changeRequestList 调量请求收集器
     */
    private void appendAdjustmentRequests(TmRollingRecalcRequestDTO request,
                                          TmRollingAdjustment adjustment,
                                          List<TmScheduleResult> changeRequestList) {
        List<TmScheduleResult> currentList = this.loadTreadResults(request, adjustment.getTreadCode());
        Comparator<TmScheduleResult> comparator = Comparator
                .comparing((TmScheduleResult result) -> this.readShiftSequence(result, request.getTargetShiftOrder()),
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TmScheduleResult::getId, Comparator.nullsLast(Long::compareTo));
        currentList.sort(comparator);
        BigDecimal currentTotal = this.sumResultQty(currentList, request.getTargetShiftOrder(), false);
        BigDecimal delta = adjustment.getTargetPlanQty().subtract(currentTotal);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            TmScheduleResult target = currentList.get(currentList.size() - 1);
            BigDecimal currentQty = this.readShiftQty(target, request.getTargetShiftOrder(), false);
            changeRequestList.add(this.buildChangeRequest(target, request.getTargetShiftOrder(), currentQty.add(delta)));
            return;
        }

        BigDecimal remainingReduceQty = delta.abs();
        ListIterator<TmScheduleResult> iterator = currentList.listIterator(currentList.size());
        while (iterator.hasPrevious() && remainingReduceQty.compareTo(BigDecimal.ZERO) > 0) {
            TmScheduleResult target = iterator.previous();
            BigDecimal currentQty = this.readShiftQty(target, request.getTargetShiftOrder(), false);
            BigDecimal finishQty = this.readShiftQty(target, request.getTargetShiftOrder(), true);
            BigDecimal reducibleQty = currentQty.subtract(finishQty).max(BigDecimal.ZERO);
            BigDecimal reduceQty = reducibleQty.min(remainingReduceQty);
            if (reduceQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            changeRequestList.add(this.buildChangeRequest(target, request.getTargetShiftOrder(), currentQty.subtract(reduceQty)));
            remainingReduceQty = remainingReduceQty.subtract(reduceQty);
        }
        if (remainingReduceQty.compareTo(BigDecimal.ZERO) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingFinishLimit"));
        }
    }

    /**
     * 构建自动滚动调量请求。
     *
     * @param current 当前结果
     * @param shiftOrder 目标班次
     * @param planQty 新计划量
     * @return 调量请求
     */
    private TmScheduleResult buildChangeRequest(TmScheduleResult current, int shiftOrder, BigDecimal planQty) {
        TmScheduleResult changeRequest = new TmScheduleResult();
        changeRequest.setId(current.getId());
        changeRequest.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty);
        changeRequest.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder),
                "ROLLING_RECALC");
        return changeRequest;
    }

    /**
     * 校验所有可能被滚动影响的机台结果均处于可编辑状态。
     *
     * @param resultList 当前日期结果
     * @param adjustmentList 调整指令
     * @param shiftOrder 目标班次
     */
    private void validateAffectedReleaseStatuses(List<TmScheduleResult> resultList,
                                                  List<TmRollingAdjustment> adjustmentList,
                                                  int shiftOrder) {
        Set<String> treadCodes = adjustmentList.stream().map(TmRollingAdjustment::getTreadCode)
                .collect(Collectors.toSet());
        Set<String> machineCodes = resultList.stream()
                .filter(result -> treadCodes.contains(result.getTreadCode()))
                .filter(result -> this.readShiftQty(result, shiftOrder, false).compareTo(BigDecimal.ZERO) > 0)
                .map(TmScheduleResult::getMachineCode).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        boolean containsInvalidStatus = resultList.stream()
                .filter(result -> machineCodes.contains(result.getMachineCode()))
                .anyMatch(result -> !TmReleaseStatusTransition.isEditable(result.getReleaseStatus()));
        if (containsInvalidStatus) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingReleaseStatusInvalid"));
        }
    }

    /**
     * 计算目标班前预计库存。
     *
     * @param stock 班次开始前同步的实时库存
     * @return 非负预计库存
     */
    BigDecimal calculateExpectedStock(TmShiftStock stock) {
        return BigDecimalUtils.valueOf(stock.getStockQty())
                .subtract(BigDecimalUtils.valueOf(stock.getBadQty()))
                .add(BigDecimalUtils.valueOf(stock.getAdjustQty()))
                .max(BigDecimal.ZERO);
    }

    /**
     * 计算包含小数班次的未来累计需求，小数部分按下一班需求比例计入。
     *
     * @param taskList 同胎面任务
     * @param startShiftOrder 起始班次
     * @param shiftCount 班次数，可包含小数
     * @return 累计需求
     */
    private BigDecimal calculateWindowDemand(List<TmTaskDraft> taskList, int startShiftOrder, BigDecimal shiftCount) {
        int fullShiftCount = shiftCount.setScale(0, RoundingMode.DOWN).intValue();
        BigDecimal fraction = shiftCount.subtract(BigDecimal.valueOf(fullShiftCount));
        BigDecimal demandQty = BigDecimal.ZERO;
        for (int offset = 0; offset < fullShiftCount; offset++) {
            demandQty = demandQty.add(this.sumDemand(taskList, startShiftOrder + offset));
        }
        if (fraction.compareTo(BigDecimal.ZERO) > 0) {
            demandQty = demandQty.add(this.sumDemand(taskList, startShiftOrder + fullShiftCount).multiply(fraction));
        }
        return demandQty;
    }

    /**
     * 判断未来需求窗口是否完整。
     *
     * @param taskList 同胎面任务
     * @param startShiftOrder 起始班次
     * @param shiftCount 所需班次数
     * @return true 表示所需最后班次仍在六班窗口内且存在需求任务
     */
    private boolean hasDemandWindow(List<TmTaskDraft> taskList, int startShiftOrder, BigDecimal shiftCount) {
        int requiredShiftCount = shiftCount.setScale(0, RoundingMode.CEILING).intValue();
        int lastShiftOrder = startShiftOrder + requiredShiftCount - 1;
        return lastShiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER && !taskList.isEmpty();
    }

    /**
     * 汇总指定班次需求。
     *
     * @param taskList 同胎面任务
     * @param shiftOrder 班次
     * @return 需求米数
     */
    private BigDecimal sumDemand(List<TmTaskDraft> taskList, int shiftOrder) {
        return taskList.stream().filter(task -> Integer.valueOf(shiftOrder).equals(task.getShiftOrder()))
                .map(TmTaskDraft::getCurrentShiftDemandQty).map(BigDecimalUtils::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 汇总排程结果指定班次计划量或完成量。
     *
     * @param resultList 排程结果
     * @param shiftOrder 班次
     * @param finish true 读取完成量，false 读取计划量
     * @return 汇总数量
     */
    private BigDecimal sumResultQty(List<TmScheduleResult> resultList, int shiftOrder, boolean finish) {
        return resultList.stream().map(result -> this.readShiftQty(result, shiftOrder, finish))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 动态读取班次计划量或完成量。
     *
     * @param result 排程结果
     * @param shiftOrder 班次
     * @param finish true 读取完成量，false 读取计划量
     * @return 数量，空值按零处理
     */
    private BigDecimal readShiftQty(TmScheduleResult result, int shiftOrder, boolean finish) {
        String fieldTemplate = finish ? TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE
                : TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE;
        return result == null ? BigDecimal.ZERO : BigDecimalUtils.valueOf(
                result.getFieldValueByFieldName(String.format(fieldTemplate, shiftOrder)));
    }

    /**
     * 动态读取班次顺序。
     *
     * @param result 排程结果
     * @param shiftOrder 班次
     * @return 顺序，空值返回 null
     */
    private Integer readShiftSequence(TmScheduleResult result, int shiftOrder) {
        Object value = result == null ? null : result.getFieldValueByFieldName(
                String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    /**
     * 查询当前工厂和日期的有效结果。
     *
     * @param request 滚动请求
     * @return 排程结果
     */
    private List<TmScheduleResult> loadScheduleResults(TmRollingRecalcRequestDTO request) {
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, request.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, request.getScheduleDate());
        wrapper.orderByAsc(TmScheduleResult::getMachineCode, TmScheduleResult::getId);
        List<TmScheduleResult> resultList = tmScheduleResultMapper.selectList(wrapper);
        return resultList == null ? Collections.emptyList() : resultList;
    }

    /**
     * 查询同一胎面的当前结果。
     *
     * @param request 滚动请求
     * @param treadCode 胎面编码
     * @return 同胎面结果
     */
    private List<TmScheduleResult> loadTreadResults(TmRollingRecalcRequestDTO request, String treadCode) {
        return this.loadScheduleResults(request).stream().filter(result -> treadCode.equals(result.getTreadCode()))
                .collect(Collectors.toList());
    }

    /**
     * 加载库存并保留同胎面首条记录，保持自动排程库存预测口径。
     *
     * @param request 滚动请求
     * @return 胎面库存映射
     */
    private Map<String, TmShiftStock> loadStockMap(TmRollingRecalcRequestDTO request) {
        LambdaQueryWrapper<TmShiftStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmShiftStock::getFactoryCode, request.getFactoryCode());
        wrapper.eq(TmShiftStock::getStockDate, request.getStockDate());
        wrapper.eq(TmShiftStock::getShiftOrder, request.getTargetShiftOrder());
        wrapper.orderByAsc(TmShiftStock::getId);
        List<TmShiftStock> stockList = tmShiftStockMapper.selectList(wrapper);
        return (stockList == null ? Collections.<TmShiftStock>emptyList() : stockList).stream()
                .filter(stock -> StrUtil.isNotBlank(stock.getTreadCode()))
                .collect(Collectors.toMap(TmShiftStock::getTreadCode, Function.identity(), (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    /**
     * 补齐并规范化MES库存物理日期。
     *
     * @param request 滚动请求
     * @throws ServiceException 班次配置无法解析时抛出
     */
    private void resolveStockDate(TmRollingRecalcRequestDTO request) {
        if (request.getStockDate() != null) {
            request.setStockDate(DateUtil.beginOfDay(request.getStockDate()));
            return;
        }
        com.zlt.aps.tm.domain.vo.TmRollingWindow window = this.tmRollingWindowService.resolveWindow(
                request.getFactoryCode(), request.getScheduleDate(), request.getTargetShiftOrder());
        if (window == null || window.getStockDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingRequestInvalid"));
        }
        request.setStockDate(DateUtil.beginOfDay(window.getStockDate()));
    }

    /**
     * 在幂等记录查询前校验整个班次库存快照存在。
     *
     * <p>库存为空时直接阻断，不写入SKIPPED日志，补齐库存后允许同一窗口重试。</p>
     *
     * @param request 滚动请求
     * @throws ServiceException 班次库存为空时抛出
     */
    private void ensureShiftStockExists(TmRollingRecalcRequestDTO request) {
        Long stockCount = this.tmShiftStockMapper.selectCount(new LambdaQueryWrapper<TmShiftStock>()
                .eq(TmShiftStock::getFactoryCode, request.getFactoryCode())
                .eq(TmShiftStock::getStockDate, request.getStockDate())
                .eq(TmShiftStock::getShiftOrder, request.getTargetShiftOrder()));
        if (stockCount == null || stockCount <= 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.shiftStockMissing"));
        }
    }

    /**
     * 查询滚动参数，空值或非法值使用默认值。
     *
     * @param factoryCode 工厂编号
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 正数参数值
     */
    private BigDecimal readPositiveDecimalParam(String factoryCode, String paramCode, BigDecimal defaultValue) {
        String value = this.readParamValue(factoryCode, paramCode, defaultValue.toPlainString());
        try {
            BigDecimal parsedValue = new BigDecimal(value);
            return parsedValue.compareTo(BigDecimal.ZERO) > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 查询正整数滚动参数，空值、零值或非法值使用默认值。
     *
     * @param factoryCode 工厂编号
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 正整数参数值
     */
    private int readPositiveIntegerParam(String factoryCode, String paramCode, int defaultValue) {
        String value = this.readParamValue(factoryCode, paramCode, String.valueOf(defaultValue));
        try {
            int parsedValue = Integer.parseInt(value);
            return parsedValue > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 查询单个工厂参数。
     *
     * @param factoryCode 工厂编号
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 生效参数值
     */
    private String readParamValue(String factoryCode, String paramCode, String defaultValue) {
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getFactoryCode, factoryCode);
        wrapper.eq(TmParams::getParamCode, paramCode);
        wrapper.eq(TmParams::getEnableStatus, "1");
        wrapper.orderByDesc(TmParams::getId);
        List<TmParams> paramsList = tmParamsMapper.selectList(wrapper);
        if (paramsList == null || paramsList.isEmpty()) {
            return defaultValue;
        }
        TmParams params = paramsList.get(0);
        return StrUtil.blankToDefault(StrUtil.trim(params.getParamValue()),
                StrUtil.blankToDefault(StrUtil.trim(params.getDefaultValue()), defaultValue));
    }

    /**
     * 判断工厂自动滚动开关是否开启。
     *
     * @param factoryCode 工厂编号
     * @return true 表示开启
     */
    private boolean isRollingEnabled(String factoryCode) {
        return "1".equals(this.readParamValue(factoryCode, TmScheduleConstants.PARAM_ROLLING_ENABLED,
                TmScheduleConstants.DEFAULT_ROLLING_ENABLED));
    }

    /**
     * 记录单胎面跳过证据。
     *
     * @param context 加载上下文
     * @param treadCode 胎面编码
     * @param reasonCode 跳过原因
     */
    private void incrementSkip(TmScheduleContext context, String treadCode, String reasonCode) {
        Map<String, Object> evidence = context.getCurrentDayShutdownEvidenceMap()
                .computeIfAbsent(0, ignored -> new LinkedHashMap<>());
        evidence.put(treadCode, reasonCode);
        log.warn("[TM_ROLLING_SKIP] factoryCode={}, scheduleDate={}, treadCode={}, reasonCode={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), treadCode, reasonCode);
    }

    /**
     * 提取本次滚动跳过证据。
     *
     * @param context 加载上下文
     * @return 胎面到原因的映射
     */
    private Map<String, Object> extractRollingSkipEvidence(TmScheduleContext context) {
        Map<String, Object> evidence = context.getCurrentDayShutdownEvidenceMap().get(0);
        return evidence == null ? new LinkedHashMap<>() : new LinkedHashMap<>(evidence);
    }

    /**
     * 构建响应并统计真正变化的结果行。
     *
     * @param request 滚动请求
     * @param runKey 运行键
     * @param traceId 追踪号
     * @param beforeList 操作前结果
     * @param afterList 操作后结果
     * @param adjustmentList 调整指令
     * @param context 加载上下文
     * @param updateCount 滚动更新次数
     * @return 滚动响应
     */
    private TmRollingRecalcResponseVO buildResponse(TmRollingRecalcRequestDTO request, String runKey, String traceId,
                                                     List<TmScheduleResult> beforeList,
                                                     List<TmScheduleResult> afterList,
                                                     List<TmRollingAdjustment> adjustmentList,
                                                     TmScheduleContext context,
                                                     int updateCount) {
        TmRollingRecalcResponseVO response = new TmRollingRecalcResponseVO();
        response.setRunKey(runKey);
        response.setStatus(adjustmentList.isEmpty() ? STATUS_SKIPPED : STATUS_SUCCESS);
        response.setScheduleDate(request.getScheduleDate());
        response.setTargetShiftOrder(request.getTargetShiftOrder());
        response.setAdjustedTreadCount(adjustmentList.size());
        response.setAffectedResultCount(this.countChangedRows(beforeList, afterList));
        response.setBeforePlanQty(this.sumResultQty(beforeList, request.getTargetShiftOrder(), false));
        response.setAfterPlanQty(this.sumResultQty(afterList, request.getTargetShiftOrder(), false));
        response.setTraceId(traceId);
        Map<String, Object> skipEvidence = this.extractRollingSkipEvidence(context);
        Map<String, Integer> skipSummary = skipEvidence.values().stream().map(String::valueOf)
                .collect(Collectors.toMap(Function.identity(), ignored -> 1, Integer::sum, LinkedHashMap::new));
        response.setSkippedTreadCount(skipEvidence.size());
        response.setSkippedReasonSummary(skipSummary);
        log.info("[TM_ROLLING] runKey={}, adjustedTreadCount={}, updateCount={}, affectedResultCount={}, skippedTreadCount={}",
                runKey, adjustmentList.size(), updateCount, response.getAffectedResultCount(), response.getSkippedTreadCount());
        return response;
    }

    /**
     * 统计操作前后任一班次计划量发生变化的结果行。
     *
     * @param beforeList 操作前结果
     * @param afterList 操作后结果
     * @return 变化结果行数量
     */
    private int countChangedRows(List<TmScheduleResult> beforeList, List<TmScheduleResult> afterList) {
        Map<Long, TmScheduleResult> beforeMap = beforeList.stream().filter(result -> result.getId() != null)
                .collect(Collectors.toMap(TmScheduleResult::getId, Function.identity(), (first, ignored) -> first));
        Map<Long, TmScheduleResult> afterMap = afterList.stream().filter(result -> result.getId() != null)
                .collect(Collectors.toMap(TmScheduleResult::getId, Function.identity(), (first, ignored) -> first));
        Set<Long> allIds = new HashSet<>(beforeMap.keySet());
        allIds.addAll(afterMap.keySet());
        int changedCount = 0;
        for (Long id : allIds) {
            TmScheduleResult before = beforeMap.get(id);
            TmScheduleResult after = afterMap.get(id);
            if (before == null || after == null || !this.hasSamePlan(before, after)) {
                changedCount++;
            }
        }
        return changedCount;
    }

    /**
     * 比较六班计划量是否一致。
     *
     * @param before 操作前结果
     * @param after 操作后结果
     * @return true 表示一致
     */
    private boolean hasSamePlan(TmScheduleResult before, TmScheduleResult after) {
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.readShiftQty(before, shiftOrder, false)
                    .compareTo(this.readShiftQty(after, shiftOrder, false)) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 写入不可撤销的自动滚动审计日志。
     *
     * @param request 滚动请求
     * @param runKey 运行键
     * @param beforeList 操作前结果
     * @param afterList 操作后结果
     * @param response 滚动响应
     */
    private void recordRollingLog(TmRollingRecalcRequestDTO request, String runKey,
                                  List<TmScheduleResult> beforeList, List<TmScheduleResult> afterList,
                                  TmRollingRecalcResponseVO response) {
        TmDispatcherLog dispatcherLog = new TmDispatcherLog();
        dispatcherLog.setFactoryCode(request.getFactoryCode());
        dispatcherLog.setBatchNo(runKey);
        dispatcherLog.setScheduleDate(request.getScheduleDate());
        dispatcherLog.setOperType(TmScheduleConstants.DISPATCHER_OPER_ROLLING);
        dispatcherLog.setUndoStatus("1");
        dispatcherLog.setAffectedBeforeJson(JSON.toJSONString(beforeList));
        dispatcherLog.setAffectedAfterJson(JSON.toJSONString(afterList));
        dispatcherLog.setCreateBy(request.getOperator());
        dispatcherLog.setRemark(SUMMARY_PREFIX + JSON.toJSONString(response));
        if (tmDispatcherLogMapper.insert(dispatcherLog) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingAuditFailed"));
        }
    }

    /**
     * 读取相同运行键的成功或跳过摘要，实现定时与手动入口幂等。
     *
     * @param request 滚动请求
     * @param runKey 运行键
     * @param traceId 本次追踪号
     * @return 已执行响应；不存在返回 null
     */
    private TmRollingRecalcResponseVO loadExistingResponse(TmRollingRecalcRequestDTO request,
                                                            String runKey, String traceId) {
        LambdaQueryWrapper<TmDispatcherLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmDispatcherLog::getFactoryCode, request.getFactoryCode());
        wrapper.eq(TmDispatcherLog::getScheduleDate, request.getScheduleDate());
        wrapper.eq(TmDispatcherLog::getBatchNo, runKey);
        wrapper.eq(TmDispatcherLog::getOperType, TmScheduleConstants.DISPATCHER_OPER_ROLLING);
        wrapper.orderByDesc(TmDispatcherLog::getId);
        List<TmDispatcherLog> logList = tmDispatcherLogMapper.selectList(wrapper);
        if (logList == null || logList.isEmpty()) {
            return null;
        }
        String remark = logList.get(0).getRemark();
        if (StrUtil.isNotBlank(remark) && remark.startsWith(SUMMARY_PREFIX)) {
            try {
                return JSON.parseObject(remark.substring(SUMMARY_PREFIX.length()), TmRollingRecalcResponseVO.class);
            } catch (RuntimeException ex) {
                log.warn("[TM_ROLLING] 解析历史运行摘要失败，runKey={}，原因={}", runKey, ex.getMessage());
            }
        }
        TmRollingRecalcResponseVO response = new TmRollingRecalcResponseVO();
        response.setRunKey(runKey);
        response.setStatus(STATUS_SKIPPED);
        response.setScheduleDate(request.getScheduleDate());
        response.setTargetShiftOrder(request.getTargetShiftOrder());
        response.setTraceId(traceId);
        return response;
    }

    /**
     * 发布低敏滚动事件摘要。
     *
     * @param request 滚动请求
     * @param response 滚动响应
     * @param context 排程上下文
     */
    private void publishRollingEvent(TmRollingRecalcRequestDTO request, TmRollingRecalcResponseVO response,
                                     TmScheduleContext context) {
        context.setBatchNo(response.getRunKey());
        context.setTraceId(response.getTraceId());
        context.setOperator(request.getOperator());
        String summary = "status=" + response.getStatus() + ",adjustedTreadCount="
                + response.getAdjustedTreadCount() + ",affectedResultCount=" + response.getAffectedResultCount();
        tmScheduleEventPublisher.publish(TmScheduleEvent.of(context, TmScheduleEventTypeEnum.ROLLING_RECALC, summary));
    }

    /**
     * 生成固定运行键。
     *
     * @param request 滚动请求
     * @return 运行键
     */
    private String buildRunKey(TmRollingRecalcRequestDTO request) {
        return TmScheduleConstants.ROLLING_RUN_KEY_PREFIX + request.getFactoryCode() + ":"
                + DateUtil.format(request.getScheduleDate(), "yyyyMMdd") + ":" + request.getTargetShiftOrder();
    }

    /**
     * 构建自动开关关闭时的跳过响应，不写数据库幂等日志。
     *
     * @param request 滚动请求
     * @return 跳过响应
     */
    private TmRollingRecalcResponseVO buildDisabledResponse(TmRollingRecalcRequestDTO request) {
        TmRollingRecalcResponseVO response = new TmRollingRecalcResponseVO();
        response.setRunKey(this.buildRunKey(request));
        response.setStatus(STATUS_SKIPPED);
        response.setScheduleDate(request.getScheduleDate());
        response.setTargetShiftOrder(request.getTargetShiftOrder());
        response.getSkippedReasonSummary().put("ROLLING_DISABLED", 1);
        response.setSkippedTreadCount(1);
        response.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        return response;
    }

    /**
     * 校验请求必填字段和班次范围。
     *
     * @param request 滚动请求
     * @throws ServiceException 请求非法时抛出
     */
    private void validateRequest(TmRollingRecalcRequestDTO request) {
        if (request == null || StrUtil.isBlank(request.getFactoryCode()) || request.getScheduleDate() == null
                || request.getTargetShiftOrder() == null
                || request.getTargetShiftOrder() < 1
                || request.getTargetShiftOrder() > TmScheduleConstants.TM_MAX_SHIFT_ORDER) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingRequestInvalid"));
        }
    }
}
