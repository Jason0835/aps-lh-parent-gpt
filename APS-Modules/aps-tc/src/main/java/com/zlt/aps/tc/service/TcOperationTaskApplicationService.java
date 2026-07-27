package com.zlt.aps.tc.service;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.vo.*;
import com.zlt.aps.tc.api.enums.TcBackgroundTaskTypeEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.vo.TcOperationRequestSnapshot;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎侧人工操作异步任务应用服务。
 */
@Service
@RequiredArgsConstructor
public class TcOperationTaskApplicationService {

    private final TcScheduleResultMapper scheduleResultMapper;

    private final TcBackgroundTaskService backgroundTaskService;

    private final TcOperationAsyncExecutor asyncExecutor;

    /**
     * 提交人工插单任务。
     *
     * @param request 插单请求
     * @return 初始任务
     */
    public TcOperationTaskVo submitInsert(TcInsertTaskRequestVo request) {
        if (request == null || StrUtil.isBlank(request.getFactoryCode()) || request.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.operationTaskArgumentsInvalid"));
        }
        TcOperationRequestSnapshot snapshot = new TcOperationRequestSnapshot();
        snapshot.setInsertRequest(request);
        return this.createAndExecute(TcBackgroundTaskTypeEnum.MANUAL_INSERT, request.getFactoryCode(),
                request.getScheduleDate(), snapshot);
    }

    /**
     * 提交调量任务。
     *
     * @param request 调量请求
     * @return 初始任务
     */
    public TcOperationTaskVo submitChangeQty(TcChangeQtyRequestVo request) {
        TcScheduleResult current = this.requireCurrent(request == null ? null : request.getResultId());
        TcOperationRequestSnapshot snapshot = new TcOperationRequestSnapshot();
        snapshot.setChangeQtyRequest(request);
        return this.createAndExecute(TcBackgroundTaskTypeEnum.MANUAL_CHANGE_QTY, current.getFactoryCode(),
                current.getScheduleDate(), snapshot);
    }

    /**
     * 提交单条或批量转机台任务。
     *
     * @param request 转机台请求
     * @return 初始任务
     */
    public TcOperationTaskVo submitChangeMachine(TcChangeMachineRequestVo request) {
        List<Long> resultIdList = request == null || request.getTaskList() == null ? Collections.emptyList()
                : request.getTaskList().stream().filter(Objects::nonNull)
                .map(TcChangeMachineTaskVo::getResultId).collect(Collectors.toList());
        List<TcScheduleResult> currentList = this.loadCurrentList(resultIdList);
        TcScheduleResult reference = currentList.get(0);
        this.validateSameRange(reference, currentList);
        TcOperationRequestSnapshot snapshot = new TcOperationRequestSnapshot();
        snapshot.setChangeMachineRequest(request);
        return this.createAndExecute(TcBackgroundTaskTypeEnum.MANUAL_CHANGE_MACHINE,
                reference.getFactoryCode(), reference.getScheduleDate(), snapshot);
    }

    /**
     * 提交删除任务。
     *
     * @param resultIdList 结果ID
     * @return 初始任务
     */
    public TcOperationTaskVo submitDelete(List<Long> resultIdList) {
        List<TcScheduleResult> currentList = this.loadCurrentList(resultIdList);
        TcScheduleResult reference = currentList.get(0);
        this.validateSameRange(reference, currentList);
        TcOperationRequestSnapshot snapshot = new TcOperationRequestSnapshot();
        snapshot.setResultIdList(resultIdList.stream().filter(Objects::nonNull).distinct().sorted()
                .collect(Collectors.toList()));
        return this.createAndExecute(TcBackgroundTaskTypeEnum.MANUAL_DELETE,
                reference.getFactoryCode(), reference.getScheduleDate(), snapshot);
    }

    /**
     * 查询指定人工任务。
     *
     * @param taskId 任务编号
     * @return 任务响应
     */
    public TcOperationTaskVo getTask(String taskId) {
        TcAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        if (task == null || !TcBackgroundTaskTypeEnum.manualOperationCodes().contains(task.getTaskType())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.operationTaskNotFound"));
        }
        return this.backgroundTaskService.toOperationTaskVo(task);
    }

    /**
     * 查询最近人工任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务，不存在返回null
     */
    public TcOperationTaskVo getLatestTask(String factoryCode, Date scheduleDate) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.operationTaskArgumentsInvalid"));
        }
        return this.backgroundTaskService.toOperationTaskVo(
                this.backgroundTaskService.findLatestOperation(factoryCode, scheduleDate));
    }

    /**
     * 创建任务并触发异步执行。
     *
     * @param taskType 任务类型
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param snapshot 请求快照
     * @return 初始任务
     */
    private TcOperationTaskVo createAndExecute(TcBackgroundTaskTypeEnum taskType, String factoryCode,
                                               Date scheduleDate, TcOperationRequestSnapshot snapshot) {
        TcAutoScheduleTask task = this.backgroundTaskService.createOperationPending(taskType.getCode(),
                factoryCode, scheduleDate, snapshot, this.currentUsername());
        this.asyncExecutor.execute(task.getTaskId());
        return this.backgroundTaskService.toOperationTaskVo(task);
    }

    /**
     * 查询当前结果。
     *
     * @param resultId 结果ID
     * @return 当前结果
     */
    private TcScheduleResult requireCurrent(Long resultId) {
        if (resultId == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.operationTaskArgumentsInvalid"));
        }
        TcScheduleResult current = this.scheduleResultMapper.selectById(resultId);
        if (current == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
        }
        return current;
    }

    /**
     * 批量查询当前结果并核对完整性。
     *
     * @param resultIdList 结果ID
     * @return 当前结果
     */
    private List<TcScheduleResult> loadCurrentList(List<Long> resultIdList) {
        List<Long> normalizedIdList = resultIdList == null ? Collections.emptyList()
                : resultIdList.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (normalizedIdList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.operationTaskArgumentsInvalid"));
        }
        List<TcScheduleResult> currentList = this.scheduleResultMapper.selectBatchIds(normalizedIdList);
        if (currentList.size() != normalizedIdList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
        }
        return currentList;
    }

    /**
     * 校验批量结果属于同一工厂和排程日期。
     *
     * @param reference 基准结果
     * @param currentList 当前结果
     */
    private void validateSameRange(TcScheduleResult reference, List<TcScheduleResult> currentList) {
        boolean invalid = currentList.stream().anyMatch(item -> !Objects.equals(reference.getFactoryCode(),
                item.getFactoryCode()) || !Objects.equals(reference.getScheduleDate(), item.getScheduleDate()));
        if (invalid) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.batchRangeInvalid"));
        }
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人
     */
    private String currentUsername() {
        return StrUtil.blankToDefault(SecurityUtils.getUsername(), "system");
    }
}
