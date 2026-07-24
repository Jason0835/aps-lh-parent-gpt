package com.zlt.aps.tm.service;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmInsertTaskRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmOperationTaskVo;
import com.zlt.aps.tm.api.enums.TmBackgroundTaskTypeEnum;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.domain.vo.TmOperationRequestSnapshot;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎面人工操作异步任务应用服务。
 */
@Service
@RequiredArgsConstructor
public class TmOperationTaskApplicationService {

    private final TmScheduleResultMapper scheduleResultMapper;

    private final TmOperationTaskService operationTaskService;

    private final TmOperationAsyncExecutor asyncExecutor;

    /**
     * 提交人工插单任务。
     *
     * @param request 插单请求
     * @return 初始任务
     */
    public TmOperationTaskVo submitInsert(TmInsertTaskRequestVo request) {
        if (request == null || StrUtil.isBlank(request.getFactoryCode()) || request.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskArgumentsInvalid"));
        }
        TmOperationRequestSnapshot snapshot = new TmOperationRequestSnapshot();
        snapshot.setInsertRequest(request);
        return this.createAndExecute(TmBackgroundTaskTypeEnum.MANUAL_INSERT, request.getFactoryCode(),
                request.getScheduleDate(), snapshot);
    }

    /**
     * 提交调量任务。
     *
     * @param request 调量请求
     * @return 初始任务
     */
    public TmOperationTaskVo submitChangeQty(TmScheduleResult request) {
        TmScheduleResult current = this.requireCurrent(request == null ? null : request.getId());
        TmOperationRequestSnapshot snapshot = new TmOperationRequestSnapshot();
        snapshot.setScheduleResult(request);
        return this.createAndExecute(TmBackgroundTaskTypeEnum.MANUAL_CHANGE_QTY, current.getFactoryCode(),
                current.getScheduleDate(), snapshot);
    }

    /**
     * 提交单条转机台任务。
     *
     * @param request 转机台请求
     * @return 初始任务
     */
    public TmOperationTaskVo submitChangeMachine(TmScheduleResult request) {
        TmScheduleResult current = this.requireCurrent(request == null ? null : request.getId());
        TmOperationRequestSnapshot snapshot = new TmOperationRequestSnapshot();
        snapshot.setScheduleResult(request);
        return this.createAndExecute(TmBackgroundTaskTypeEnum.MANUAL_CHANGE_MACHINE, current.getFactoryCode(),
                current.getScheduleDate(), snapshot);
    }

    /**
     * 提交批量转机台任务。
     *
     * @param targetMachineCode 目标机台
     * @param requestList 转机台请求
     * @return 初始任务
     */
    public TmOperationTaskVo submitBatchChangeMachine(String targetMachineCode,
                                                      List<TmScheduleResult> requestList) {
        if (StrUtil.isBlank(targetMachineCode) || requestList == null || requestList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskArgumentsInvalid"));
        }
        List<TmScheduleResult> currentList = this.loadCurrentList(requestList.stream()
                .filter(Objects::nonNull).map(TmScheduleResult::getId).collect(Collectors.toList()));
        TmScheduleResult reference = currentList.get(0);
        this.validateSameRange(reference, currentList);
        TmOperationRequestSnapshot snapshot = new TmOperationRequestSnapshot();
        snapshot.setTargetMachineCode(targetMachineCode);
        snapshot.setScheduleResultList(requestList);
        return this.createAndExecute(TmBackgroundTaskTypeEnum.MANUAL_BATCH_CHANGE_MACHINE,
                reference.getFactoryCode(), reference.getScheduleDate(), snapshot);
    }

    /**
     * 提交删除任务。
     *
     * @param resultIdList 结果ID
     * @return 初始任务
     */
    public TmOperationTaskVo submitDelete(List<Long> resultIdList) {
        return this.submitIdOperation(resultIdList, TmBackgroundTaskTypeEnum.MANUAL_DELETE);
    }

    /**
     * 提交发布任务。
     *
     * @param resultIdList 结果ID
     * @return 初始任务
     */
    public TmOperationTaskVo submitPublish(List<Long> resultIdList) {
        return this.submitIdOperation(resultIdList, TmBackgroundTaskTypeEnum.MANUAL_PUBLISH);
    }

    /**
     * 查询指定人工任务。
     *
     * @param taskId 任务编号
     * @return 任务响应
     */
    public TmOperationTaskVo getTask(String taskId) {
        TmAutoScheduleTask task = this.operationTaskService.findByTaskId(taskId);
        if (task == null || !TmBackgroundTaskTypeEnum.manualOperationCodes().contains(task.getTaskType())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskNotFound"));
        }
        return this.operationTaskService.toResponse(task);
    }

    /**
     * 查询最近人工任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务，不存在返回null
     */
    public TmOperationTaskVo getLatestTask(String factoryCode, Date scheduleDate) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskArgumentsInvalid"));
        }
        return this.operationTaskService.toResponse(this.operationTaskService.findLatest(factoryCode, scheduleDate));
    }

    /**
     * 提交ID集合类任务。
     *
     * @param resultIdList 结果ID
     * @param taskType 任务类型
     * @return 初始任务
     */
    private TmOperationTaskVo submitIdOperation(List<Long> resultIdList, TmBackgroundTaskTypeEnum taskType) {
        List<TmScheduleResult> currentList = this.loadCurrentList(resultIdList);
        TmScheduleResult reference = currentList.get(0);
        this.validateSameRange(reference, currentList);
        TmOperationRequestSnapshot snapshot = new TmOperationRequestSnapshot();
        snapshot.setResultIdList(resultIdList.stream().filter(Objects::nonNull).distinct().sorted()
                .collect(Collectors.toList()));
        return this.createAndExecute(taskType, reference.getFactoryCode(), reference.getScheduleDate(), snapshot);
    }

    /**
     * 创建任务并触发后台执行。
     *
     * @param taskType 任务类型
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param snapshot 请求快照
     * @return 初始任务响应
     */
    private TmOperationTaskVo createAndExecute(TmBackgroundTaskTypeEnum taskType, String factoryCode,
                                               Date scheduleDate, TmOperationRequestSnapshot snapshot) {
        TmAutoScheduleTask task = this.operationTaskService.createPending(taskType.getCode(), factoryCode,
                scheduleDate, snapshot, this.currentUsername());
        this.asyncExecutor.execute(task.getTaskId());
        return this.operationTaskService.toResponse(task);
    }

    /**
     * 按ID查询当前结果。
     *
     * @param resultId 结果ID
     * @return 当前结果
     */
    private TmScheduleResult requireCurrent(Long resultId) {
        if (resultId == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskArgumentsInvalid"));
        }
        TmScheduleResult current = this.scheduleResultMapper.selectById(resultId);
        if (current == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskResultNotFound"));
        }
        return current;
    }

    /**
     * 批量查询当前结果并核对完整性。
     *
     * @param resultIdList 结果ID
     * @return 当前结果
     */
    private List<TmScheduleResult> loadCurrentList(List<Long> resultIdList) {
        List<Long> normalizedIdList = resultIdList == null ? Collections.emptyList()
                : resultIdList.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (normalizedIdList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskArgumentsInvalid"));
        }
        List<TmScheduleResult> currentList = this.scheduleResultMapper.selectBatchIds(normalizedIdList);
        if (currentList.size() != normalizedIdList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskResultNotFound"));
        }
        return currentList;
    }

    /**
     * 校验批量结果属于同一工厂和排程日期。
     *
     * @param reference 基准结果
     * @param currentList 当前结果
     */
    private void validateSameRange(TmScheduleResult reference, List<TmScheduleResult> currentList) {
        boolean invalid = currentList.stream().anyMatch(item -> !Objects.equals(reference.getFactoryCode(),
                item.getFactoryCode()) || !Objects.equals(reference.getScheduleDate(), item.getScheduleDate()));
        if (invalid) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskRangeInvalid"));
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
