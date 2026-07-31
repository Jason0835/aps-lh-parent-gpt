package com.zlt.aps.tc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcShiftStock;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.api.enums.TcBackgroundTaskTypeEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.vo.TcRollingWindow;
import com.zlt.aps.tc.mapper.TcAutoScheduleTaskMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcShiftStockMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 胎侧自动滚动任务事务性创建服务。
 */
@Service
@RequiredArgsConstructor
public class TcAutoRollingTaskCreationService {

    private final TcAutoScheduleTaskMapper taskMapper;
    private final TcScheduleResultMapper scheduleResultMapper;
    private final TcShiftStockMapper shiftStockMapper;
    private final TcBackgroundTaskService backgroundTaskService;

    /**
     * 在独立事务代理内按固定窗口幂等键创建或复用自动滚动任务。
     *
     * @param window 已同步并校验库存的窗口
     * @return 创建或复用的任务；无当前排程结果时返回null
     * @throws ServiceException 任务写入失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public TcAutoScheduleTask createTask(TcRollingWindow window) {
        TcAutoScheduleTask activeTask = this.backgroundTaskService.findActive(
                window.getFactoryCode(), window.getScheduleDate());
        if (activeTask != null) {
            return TcBackgroundTaskTypeEnum.AUTO_ROLLING.getCode().equals(activeTask.getTaskType())
                    ? activeTask : null;
        }
        List<TcScheduleResult> resultList = this.loadCurrentBatchResults(window);
        if (resultList.isEmpty()) {
            return null;
        }
        String inputVersion = this.buildInputVersion(window, resultList);
        String idempotencyKey = this.buildIdempotencyKey(window);
        TcAutoScheduleTask duplicateTask = this.taskMapper.selectOne(
                new LambdaQueryWrapper<TcAutoScheduleTask>()
                        .eq(TcAutoScheduleTask::getIdempotencyKey, idempotencyKey)
                        .in(TcAutoScheduleTask::getTaskStatus, Arrays.asList(
                                TcAutoScheduleTaskStatusEnum.PENDING.getCode(),
                                TcAutoScheduleTaskStatusEnum.RUNNING.getCode(),
                                TcAutoScheduleTaskStatusEnum.SUCCESS.getCode()))
                        .orderByDesc(TcAutoScheduleTask::getCreateTime)
                        .last("limit 1"));
        if (duplicateTask != null) {
            return duplicateTask;
        }
        TcAutoScheduleTask task = new TcAutoScheduleTask();
        task.setTaskId(TcScheduleConstants.ROLLING_TASK_ID_PREFIX
                + IdUtil.fastSimpleUUID().toUpperCase());
        task.setTaskType(TcBackgroundTaskTypeEnum.AUTO_ROLLING.getCode());
        task.setFactoryCode(window.getFactoryCode());
        task.setScheduleDate(window.getScheduleDate());
        task.setBatchNo(resultList.get(0).getBatchNo());
        task.setTraceId(IdUtil.fastSimpleUUID().toUpperCase());
        task.setTargetShiftOrder(window.getTargetShiftOrder());
        task.setInputVersion(inputVersion);
        task.setIdempotencyKey(idempotencyKey);
        task.setTaskStatus(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(I18nUtil.getMessage("ui.tc.schedule.rolling.pending"));
        task.setRequestSnapshot(JSON.toJSONString(window));
        task.setSummaryJson(JSON.toJSONString(Collections.singletonMap("schemaVersion", 1)));
        task.setCreateBy("AUTO_ROLLING");
        if (this.taskMapper.insert(task) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.rolling.createFailed"));
        }
        return task;
    }

    /**
     * 加载当前批次结果。
     *
     * @param window 滚动窗口
     * @return 当前批次结果
     */
    private List<TcScheduleResult> loadCurrentBatchResults(TcRollingWindow window) {
        TcScheduleResult latestResult = this.scheduleResultMapper.selectOne(
                new LambdaQueryWrapper<TcScheduleResult>()
                        .eq(TcScheduleResult::getFactoryCode, window.getFactoryCode())
                        .eq(TcScheduleResult::getScheduleDate, window.getScheduleDate())
                        .orderByDesc(TcScheduleResult::getCreateTime)
                        .last("limit 1"));
        if (latestResult == null || StrUtil.isBlank(latestResult.getBatchNo())) {
            return Collections.emptyList();
        }
        return this.scheduleResultMapper.selectList(new LambdaQueryWrapper<TcScheduleResult>()
                .eq(TcScheduleResult::getFactoryCode, window.getFactoryCode())
                .eq(TcScheduleResult::getScheduleDate, window.getScheduleDate())
                .eq(TcScheduleResult::getBatchNo, latestResult.getBatchNo())
                .orderByAsc(TcScheduleResult::getId));
    }

    /**
     * 按班次库存数量和结果任务版本构造输入审计指纹。
     *
     * @param window 滚动窗口
     * @param resultList 当前结果
     * @return SHA-256指纹
     */
    private String buildInputVersion(TcRollingWindow window, List<TcScheduleResult> resultList) {
        List<TcShiftStock> stockList = this.shiftStockMapper.selectList(new LambdaQueryWrapper<TcShiftStock>()
                .eq(TcShiftStock::getFactoryCode, window.getFactoryCode())
                .eq(TcShiftStock::getStockDate, window.getStockDate())
                .eq(TcShiftStock::getShiftOrder, window.getTargetShiftOrder())
                .orderByAsc(TcShiftStock::getSidewallCode));
        List<String> partList = new ArrayList<>();
        partList.add(window.getFactoryCode());
        partList.add(DateUtil.formatDate(window.getScheduleDate()));
        partList.add(DateUtil.formatDate(window.getStockDate()));
        partList.add(String.valueOf(window.getTargetShiftOrder()));
        CollectionUtils.emptyIfNull(stockList).stream().map(stock -> StrUtil.blankToDefault(
                stock.getSidewallCode(), "") + ":" + Objects.toString(stock.getStockQty(), "")
                + ":" + Objects.toString(stock.getBadQty(), "")
                + ":" + Objects.toString(stock.getAdjustQty(), ""))
                .forEach(partList::add);
        resultList.stream().map(result -> result.getId() + ":"
                + (result.getTaskVersion() == null ? 0L : result.getTaskVersion()))
                .forEach(partList::add);
        return DigestUtils.sha256Hex(String.join("|", partList));
    }

    /**
     * 构造固定窗口幂等键，不随重复同步的库存版本变化。
     *
     * @param window 滚动窗口
     * @return 固定窗口幂等键
     */
    private String buildIdempotencyKey(TcRollingWindow window) {
        return "TC_ROLLING:" + window.getFactoryCode() + ":"
                + DateUtil.formatDate(window.getScheduleDate()) + ":" + window.getTargetShiftOrder();
    }
}
