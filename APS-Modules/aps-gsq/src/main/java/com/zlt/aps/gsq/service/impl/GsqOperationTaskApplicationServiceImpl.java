package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqInsertTaskRequestVo;
import com.zlt.aps.gsq.api.domain.vo.GsqOperationRequestSnapshot;
import com.zlt.aps.gsq.api.domain.vo.GsqOperationTaskVo;
import com.zlt.aps.gsq.domain.GsqAutoScheduleTask;
import com.zlt.aps.gsq.enums.GsqBackgroundTaskTypeEnum;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.GsqBackgroundTaskService;
import com.zlt.aps.gsq.service.GsqOperationAsyncExecutor;
import com.zlt.aps.gsq.service.GsqOperationTaskApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 钢丝圈人工操作任务应用服务实现。
 *
 * <p>对齐胎侧 {@code TcOperationTaskApplicationService}，作为 Controller 与
 * {@link GsqManualScheduleApplicationService} 之间的桥接层。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>请求基础校验（必填项、批量范围一致性、记录存在性）；</li>
 *   <li>构造 {@link GsqOperationRequestSnapshot} 请求快照；</li>
 *   <li>调用 {@link GsqBackgroundTaskService#createOperationPending} 持久化任务；</li>
 *   <li>触发 {@link GsqOperationAsyncExecutor#execute} 异步执行；</li>
 *   <li>返回 {@link GsqOperationTaskVo} 供前端轮询。</li>
 * </ol>
 *
 * <p>该实现不直接持有数据库事务或分布式锁，与 {@code GsqManualScheduleApplicationService}
 * 职责区分清晰：本服务面向 Controller 提供任务编排能力，
 * {@code GsqManualScheduleApplicationService} 面向异步执行器提供业务编排能力。</p>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GsqOperationTaskApplicationServiceImpl implements GsqOperationTaskApplicationService {

    /** 排程结果 Mapper（反查记录存在性与索引字段） */
    private final GsqScheduleResultMapper scheduleResultMapper;

    /** 后台任务状态服务 */
    private final GsqBackgroundTaskService backgroundTaskService;

    /** 人工操作异步执行器 */
    private final GsqOperationAsyncExecutor asyncExecutor;

    /**
     * 提交人工插单任务。
     *
     * @param request 插单请求
     * @return 初始任务
     * @throws ServiceException 参数不合法时抛出
     */
    @Override
    public GsqOperationTaskVo submitInsert(GsqInsertTaskRequestVo request) {
        if (request == null || StrUtil.isBlank(request.getFactoryCode()) || request.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operationTaskArgumentsInvalid"));
        }
        GsqOperationRequestSnapshot snapshot = new GsqOperationRequestSnapshot();
        snapshot.setInsertRequest(request);
        snapshot.setOperator(this.currentUsername());
        return this.createAndExecute(GsqBackgroundTaskTypeEnum.MANUAL_INSERT, request.getFactoryCode(),
                request.getScheduleDate(), snapshot);
    }

    /**
     * 提交批量调量任务。
     *
     * @param requestList 调量请求列表
     * @return 初始任务
     * @throws ServiceException 列表为空或批量范围不一致时抛出
     */
    @Override
    public GsqOperationTaskVo submitChangeQty(List<GsqScheduleResult> requestList) {
        GsqScheduleResult reference = this.requireSingleRange(requestList);
        GsqOperationRequestSnapshot snapshot = new GsqOperationRequestSnapshot();
        snapshot.setChangeQtyRequestList(requestList);
        snapshot.setOperator(this.currentUsername());
        return this.createAndExecute(GsqBackgroundTaskTypeEnum.MANUAL_CHANGE_QTY,
                reference.getFactoryCode(), reference.getScheduleDate(), snapshot);
    }

    /**
     * 提交批量转机台任务。
     *
     * @param requestList 转机台请求列表
     * @return 初始任务
     * @throws ServiceException 列表为空或批量范围不一致时抛出
     */
    @Override
    public GsqOperationTaskVo submitChangeMachine(List<GsqScheduleResult> requestList) {
        GsqScheduleResult reference = this.requireSingleRange(requestList);
        GsqOperationRequestSnapshot snapshot = new GsqOperationRequestSnapshot();
        snapshot.setChangeMachineRequestList(requestList);
        snapshot.setOperator(this.currentUsername());
        return this.createAndExecute(GsqBackgroundTaskTypeEnum.MANUAL_CHANGE_MACHINE,
                reference.getFactoryCode(), reference.getScheduleDate(), snapshot);
    }

    /**
     * 提交批量删除任务。
     *
     * <p>对齐胎侧 {@code TcOperationTaskApplicationService.submitDelete}：
     * 通过 {@code selectBatchIds} 反查记录以校验存在性并取得 factoryCode/scheduleDate 作为任务索引；
     * 门面内部会再次通过 {@code selectBatchIdsForUpdate} 加行锁二次校验。</p>
     *
     * @param resultIdList 排程结果 ID 列表
     * @return 初始任务
     * @throws ServiceException 列表为空、记录不存在或批量范围不一致时抛出
     */
    @Override
    public GsqOperationTaskVo submitDelete(List<Long> resultIdList) {
        List<Long> normalizedIdList = this.normalizeIdList(resultIdList);
        if (normalizedIdList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.delete.idsEmpty"));
        }
        List<GsqScheduleResult> currentList = this.scheduleResultMapper.selectBatchIds(normalizedIdList);
        if (currentList == null || currentList.size() != normalizedIdList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.manual.resultNotFound"));
        }
        GsqScheduleResult reference = currentList.get(0);
        this.validateSameRange(reference, currentList);
        GsqOperationRequestSnapshot snapshot = new GsqOperationRequestSnapshot();
        snapshot.setResultIdList(normalizedIdList);
        snapshot.setOperator(this.currentUsername());
        return this.createAndExecute(GsqBackgroundTaskTypeEnum.MANUAL_DELETE,
                reference.getFactoryCode(), reference.getScheduleDate(), snapshot);
    }

    /**
     * 查询指定人工任务。
     *
     * @param taskId 任务编号
     * @return 任务响应
     * @throws ServiceException 任务不存在或非人工操作任务时抛出
     */
    @Override
    public GsqOperationTaskVo getTask(String taskId) {
        GsqAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        if (task == null || !GsqBackgroundTaskTypeEnum.manualOperationCodes().contains(task.getTaskType())) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operationTaskNotFound"));
        }
        return this.backgroundTaskService.toOperationTaskVo(task);
    }

    /**
     * 查询最近一次人工操作任务。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务，不存在返回 null
     * @throws ServiceException 参数为空时抛出
     */
    @Override
    public GsqOperationTaskVo getLatestTask(String factoryCode, Date scheduleDate) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operationTaskArgumentsInvalid"));
        }
        return this.backgroundTaskService.toOperationTaskVo(
                this.backgroundTaskService.findLatestOperation(factoryCode, scheduleDate));
    }

    // ==================== 内部方法 ====================

    /**
     * 创建任务并触发异步执行。
     *
     * @param taskType     任务类型
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param snapshot     请求快照
     * @return 初始任务响应
     */
    private GsqOperationTaskVo createAndExecute(GsqBackgroundTaskTypeEnum taskType, String factoryCode,
                                                 Date scheduleDate, GsqOperationRequestSnapshot snapshot) {
        GsqAutoScheduleTask task = this.backgroundTaskService.createOperationPending(
                taskType.getCode(), factoryCode, scheduleDate, snapshot, snapshot.getOperator());
        this.asyncExecutor.execute(task.getTaskId());
        return this.backgroundTaskService.toOperationTaskVo(task);
    }

    /**
     * 校验批量请求列表非空且所有记录属于同一工厂和排程日期。
     *
     * @param requestList 请求列表
     * @return 基准记录（首条）
     * @throws ServiceException 列表为空或批量范围不一致时抛出
     */
    private GsqScheduleResult requireSingleRange(List<GsqScheduleResult> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operationTaskArgumentsInvalid"));
        }
        GsqScheduleResult reference = requestList.get(0);
        if (reference.getFactoryCode() == null || reference.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operationTaskArgumentsInvalid"));
        }
        this.validateSameRange(reference, requestList);
        return reference;
    }

    /**
     * 校验批量结果属于同一工厂和排程日期。
     *
     * @param reference 基准结果
     * @param currentList 当前结果列表
     * @throws ServiceException 批量范围不一致时抛出
     */
    private void validateSameRange(GsqScheduleResult reference, List<GsqScheduleResult> currentList) {
        boolean invalid = currentList.stream().anyMatch(item -> !Objects.equals(reference.getFactoryCode(),
                item.getFactoryCode()) || !Objects.equals(reference.getScheduleDate(), item.getScheduleDate()));
        if (invalid) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.manual.batchRangeInvalid"));
        }
    }

    /**
     * 规范化 ID 列表（去 null、去重、升序）。
     *
     * @param resultIdList 原始 ID 列表
     * @return 规范化后的 ID 列表
     */
    private List<Long> normalizeIdList(List<Long> resultIdList) {
        if (resultIdList == null) {
            return Collections.emptyList();
        }
        return resultIdList.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人，空时填充为 system
     */
    private String currentUsername() {
        return StrUtil.blankToDefault(SecurityUtils.getUsername(), "system");
    }
}
