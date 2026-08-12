package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.constant.GsqScheduleConstants;
import com.zlt.aps.gsq.mapper.GsqDispatcherLogMapper;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 钢丝圈人工排程操作统一门面。
 *
 * <p>对齐胎圈 {@code TqManualOperationFacade}（因钢丝圈无 taskVersion，采用行锁+状态校验，不用乐观锁）
 * 与胎侧 {@code TcManualOperationFacade}（含 {@code changeQtyForAutoRolling} 供异步执行器复用）。</p>
 *
 * <p>统一处理请求规范化、数据库真实状态读取、多机台分布式锁、短事务、行锁和调度日志，
 * 避免插单、调量、转机台和删除从不同入口绕过同一组安全约束。</p>
 *
 * <p>与胎圈差异：</p>
 * <ul>
 *   <li>钢丝圈 {@link GsqDispatcherLog} 表无 affectedBeforeJson/affectedAfterJson/undoStatus 字段，
 *       因此不实现撤销机制（undoLastOperation），仅用现有 beforeClassNPlan/afterClassNPlan
 *       字段记录6班次计划量快照。</li>
 *   <li>钢丝圈复用 {@link GsqManualInsertRollingService} 的 insertAndRoll/changeQtyAndRoll/
 *       changeMachineAndRollBatch/deleteAndRollBatch 等任务链路径方法作为底层执行。</li>
 *   <li>钢丝圈 IS_RELEASE 取值：0-未发布 1-已发布 2-发布中 3-超时失败；
 *       仅 0/3 允许人工编辑（对齐 {@code GsqRollingUpdateServiceImpl.isEditableReleaseStatus}），
 *       1/2 不可编辑。</li>
 *   <li>调度日志 operType 复用 {@link ApsConstant#DISPATCHER_OPER_MACHINE}(0)、
 *       {@link ApsConstant#DISPATCHER_OPER_PLAN}(1)、{@link ApsConstant#DISPATCHER_OPER_INSERT_ORDER}(2)、
 *       {@link ApsConstant#DISPATCHER_OPER_DELETE}(3)；自动滚动用
 *       {@link GsqScheduleConstants#DISPATCHER_OPER_AUTO_ROLLING}(4)。</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GsqManualOperationFacade {

    /** 人工操作分布式锁前缀（对齐胎圈 TqManualOperationFacade.LOCK_PREFIX） */
    private static final String LOCK_PREFIX = GsqScheduleConstants.MANUAL_OPERATION_LOCK_KEY_PREFIX;

    /** 同一工厂排程日全局约束锁后缀，用于串行化跨机台工装池重放 */
    private static final String CONSTRAINT_LOCK_SUFFIX = ":__CONSTRAINT__";

    private final RedissonClient redissonClient;

    private final PlatformTransactionManager platformTransactionManager;

    private final GsqScheduleResultMapper gsqScheduleResultMapper;

    private final GsqDispatcherLogMapper gsqDispatcherLogMapper;

    private final GsqManualInsertRollingService gsqManualInsertRollingService;

    // ==================== 公开操作入口 ====================

    /**
     * 执行人工插单。
     *
     * @param scheduleResult 插单排程结果
     * @return 新增行数
     * @throws ServiceException 参数、锁或插单位置校验失败时抛出
     */
    public int insertTask(GsqScheduleResult scheduleResult) {
        this.normalizeInsertRequest(scheduleResult);
        List<String> machineCodes = Collections.singletonList(scheduleResult.getMachineCode());
        return this.executeWithMachineLocks(scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate(), machineCodes,
                () -> this.executeInTransaction(() -> {
                    List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(scheduleResult, machineCodes);
                    int changedCount = gsqManualInsertRollingService.insertAndRoll(scheduleResult);
                    List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(scheduleResult, machineCodes);
                    this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResult, beforeList, afterList);
                    return changedCount;
                }));
    }

    /**
     * 执行人工调量。
     *
     * @param scheduleResult 调量请求
     * @return 更新行数
     * @throws ServiceException 记录不存在、正在发布或锁获取失败时抛出
     */
    public int changeQty(GsqScheduleResult scheduleResult) {
        GsqScheduleResult persisted = this.loadOperationResult(scheduleResult,
                "ui.gsq.schedule.changeQty.idEmpty", "调量排程结果不能为空",
                "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效");
        this.validateChangeQtyEditableFields(scheduleResult, persisted);
        this.normalizeExistingOperationRequest(scheduleResult, persisted, false);
        List<String> machineCodes = Collections.singletonList(persisted.getMachineCode());
        return this.executeWithMachineLocks(persisted.getFactoryCode(), persisted.getScheduleDate(), machineCodes,
                () -> this.executeInTransaction(() -> {
                    GsqScheduleResult current = this.reloadAndValidateOperationResult(scheduleResult.getId(),
                            "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效");
                    this.validateLockedSourceMachine(persisted, current);
                    this.normalizeExistingOperationRequest(scheduleResult, current, false);
                    List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(current, machineCodes);
                    int changedCount = gsqManualInsertRollingService.changeQtyAndRoll(scheduleResult);
                    List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(current, machineCodes);
                    this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult, beforeList, afterList);
                    return changedCount;
                }));
    }

    /**
     * 执行单条人工转机台。
     *
     * @param scheduleResult 转机台请求（machineCode 为目标机台）
     * @return 更新行数
     * @throws ServiceException 记录不存在、正在发布或任一机台锁获取失败时抛出
     */
    public int changeMachine(GsqScheduleResult scheduleResult) {
        GsqScheduleResult persisted = this.loadOperationResult(scheduleResult,
                "ui.gsq.schedule.changeMachine.idEmpty", "转机台排程结果不能为空",
                "ui.gsq.schedule.changeMachine.resultNotFound", "转机台排程结果不存在或已失效");
        String targetMachineCode = this.normalizeTargetMachineCode(scheduleResult.getMachineCode());
        this.normalizeExistingOperationRequest(scheduleResult, persisted, true);
        scheduleResult.setMachineCode(targetMachineCode);
        List<String> machineCodes = Arrays.asList(persisted.getMachineCode(), targetMachineCode);
        return this.executeWithMachineLocks(persisted.getFactoryCode(), persisted.getScheduleDate(), machineCodes,
                () -> this.executeInTransaction(() -> this.changeMachineInsideTransaction(
                        targetMachineCode, scheduleResult, persisted)));
    }

    /**
     * 在一个分布式锁范围和一个数据库事务中批量转机台。
     *
     * <p>先完整读取全部源记录并生成全局排序锁键，获得全部源、目标机台锁后才开始写入。
     * 任一记录校验、滚动或审计失败都会使前面已经执行的记录一起回滚。</p>
     *
     * @param scheduleResultList 待转机的排程结果（每条 machineCode 为各自目标机台）
     * @return 更新行数
     * @throws ServiceException 请求为空、记录重复或任一转机失败时抛出
     */
    public int changeMachineBatch(List<GsqScheduleResult> scheduleResultList) {
        List<GsqScheduleResult> requestList = this.normalizeBatchRequests(scheduleResultList,
                "ui.gsq.schedule.changeMachine.batchEmpty", "批量转机台记录不能为空");
        List<GsqScheduleResult> initialResultList = requestList.stream()
                .map(request -> this.loadOperationResult(request,
                        "ui.gsq.schedule.changeMachine.idEmpty", "转机台排程结果不能为空",
                        "ui.gsq.schedule.changeMachine.resultNotFound", "转机台排程结果不存在或已失效"))
                .collect(Collectors.toList());
        List<String> lockKeyList = this.buildBatchChangeMachineLockKeys(initialResultList);
        return this.executeWithLockKeys(lockKeyList,
                () -> this.executeBatchChangeMachineTransaction(requestList, initialResultList));
    }

    /**
     * 批量调量（走任务链路径）。
     *
     * @param scheduleResultList 调量请求列表
     * @return 更新行数
     * @throws ServiceException 请求为空或任一调量失败时抛出
     */
    public int batchChangeQty(List<GsqScheduleResult> scheduleResultList) {
        List<GsqScheduleResult> requestList = this.normalizeBatchRequests(scheduleResultList,
                "ui.gsq.schedule.changeQty.batchEmpty", "批量调量请求不能为空");
        List<GsqScheduleResult> initialResultList = requestList.stream()
                .map(request -> this.loadOperationResult(request,
                        "ui.gsq.schedule.changeQty.idEmpty", "调量排程结果不能为空",
                        "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效"))
                .collect(Collectors.toList());
        List<String> lockKeyList = this.buildBatchLockKeys(initialResultList);
        return this.executeWithLockKeys(lockKeyList,
                () -> this.executeInTransaction(() -> {
                    List<GsqScheduleResult> currentList = new ArrayList<>();
                    for (int index = 0; index < requestList.size(); index++) {
                        GsqScheduleResult current = this.reloadAndValidateOperationResult(requestList.get(index).getId(),
                                "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效");
                        this.validateLockedSourceMachine(initialResultList.get(index), current);
                        this.normalizeExistingOperationRequest(requestList.get(index), current, false);
                        currentList.add(current);
                    }
                    List<String> machineCodes = currentList.stream().map(GsqScheduleResult::getMachineCode)
                            .distinct().collect(Collectors.toList());
                    List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(currentList.get(0), machineCodes);
                    int changedCount = gsqManualInsertRollingService.changeQtyAndRollBatch(requestList);
                    List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(currentList.get(0), machineCodes);
                    for (GsqScheduleResult request : requestList) {
                        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, request, beforeList, afterList);
                    }
                    return changedCount;
                }));
    }

    /**
     * 批量删除未发布排程结果。
     *
     * <p>按工厂、日期、机台生成全局有序分布式锁，在同一短事务中完成目标行锁、
     * 局部滚动、逻辑删除和调度日志写入，任一步失败整批回滚。</p>
     *
     * @param ids 排程结果 ID
     * @return 删除行数
     * @throws ServiceException ID 为空、记录缺失、状态非法、锁失败或并发变化时抛出
     */
    public int deleteTasks(List<Long> ids) {
        List<Long> normalizedIds = this.normalizeDeleteIds(ids);
        List<GsqScheduleResult> initialResultList = gsqScheduleResultMapper.selectBatchIds(normalizedIds);
        this.validateDeleteResults(normalizedIds, initialResultList);
        List<String> lockKeyList = this.buildDeleteLockKeys(initialResultList);
        return this.executeWithLockKeys(lockKeyList,
                () -> this.executeInTransaction(() -> this.deleteTasksInsideTransaction(normalizedIds, lockKeyList)));
    }

    // ==================== 自动滚动复用入口（对齐胎侧 TcManualOperationFacade） ====================

    /**
     * 自动滚动任务复用人工调量的锁、行锁、横表滚动和审计闭环。
     *
     * @param changeResult 自动调量请求
     * @param reason 自动滚动原因
     * @param rollingTaskId 当前自动滚动任务ID
     * @return 受影响行数
     */
    public int changeQtyForAutoRolling(GsqScheduleResult changeResult, String reason, String rollingTaskId) {
        GsqScheduleResult persisted = this.loadOperationResult(changeResult,
                "ui.gsq.schedule.changeQty.idEmpty", "调量排程结果不能为空",
                "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效");
        this.normalizeExistingOperationRequest(changeResult, persisted, false);
        List<String> machineCodes = Collections.singletonList(persisted.getMachineCode());
        return this.executeWithMachineLocks(persisted.getFactoryCode(), persisted.getScheduleDate(), machineCodes,
                () -> this.executeInTransaction(() -> {
                    GsqScheduleResult current = this.reloadAndValidateOperationResult(changeResult.getId(),
                            "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效");
                    this.validateLockedSourceMachine(persisted, current);
                    this.normalizeExistingOperationRequest(changeResult, current, false);
                    List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(current, machineCodes);
                    int changedCount = gsqManualInsertRollingService.changeQtyAndRoll(changeResult);
                    List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(current, machineCodes);
                    this.recordDispatcherLogWithReason(GsqScheduleConstants.DISPATCHER_OPER_AUTO_ROLLING,
                            changeResult, beforeList, afterList, reason);
                    return changedCount;
                }));
    }

    /**
     * 自动滚动在同一锁定快照和短事务内批量调量，只执行一次任务链计算和持久化。
     *
     * @param changeResultList 自动调量请求列表
     * @param reason 自动滚动原因
     * @param rollingTaskId 当前自动滚动任务 ID
     * @return 受影响结果行数
     */
    public int changeQtyBatchForAutoRolling(List<GsqScheduleResult> changeResultList, String reason, String rollingTaskId) {
        if (changeResultList == null || changeResultList.isEmpty()) {
            return 0;
        }
        List<GsqScheduleResult> requestList = this.normalizeBatchRequests(changeResultList,
                "ui.gsq.schedule.changeQty.batchEmpty", "批量调量请求不能为空");
        List<GsqScheduleResult> initialResultList = requestList.stream()
                .map(request -> this.loadOperationResult(request,
                        "ui.gsq.schedule.changeQty.idEmpty", "调量排程结果不能为空",
                        "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效"))
                .collect(Collectors.toList());
        List<String> lockKeyList = this.buildBatchLockKeys(initialResultList);
        return this.executeWithLockKeys(lockKeyList,
                () -> this.executeInTransaction(() -> {
                    List<GsqScheduleResult> currentList = new ArrayList<>();
                    for (int index = 0; index < requestList.size(); index++) {
                        GsqScheduleResult current = this.reloadAndValidateOperationResult(requestList.get(index).getId(),
                                "ui.gsq.schedule.changeQty.resultNotFound", "调量排程结果不存在或已失效");
                        this.validateLockedSourceMachine(initialResultList.get(index), current);
                        this.normalizeExistingOperationRequest(requestList.get(index), current, false);
                        currentList.add(current);
                    }
                    List<String> machineCodes = currentList.stream().map(GsqScheduleResult::getMachineCode)
                            .distinct().collect(Collectors.toList());
                    List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(currentList.get(0), machineCodes);
                    int changedCount = gsqManualInsertRollingService.changeQtyAndRollBatch(requestList);
                    List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(currentList.get(0), machineCodes);
                    GsqScheduleResult referenceRequest = requestList.get(0);
                    this.recordDispatcherLogWithReason(GsqScheduleConstants.DISPATCHER_OPER_AUTO_ROLLING,
                            referenceRequest, beforeList, afterList, reason);
                    return changedCount;
                }));
    }

    // ==================== 内部事务执行方法 ====================

    /**
     * 在短事务内执行批量转机台。
     *
     * @param requestList 转机请求
     * @param initialResultList 获锁前读取的源记录
     * @return 更新行数
     * @throws ServiceException 任一记录并发变化或转机失败时抛出
     */
    int executeBatchChangeMachineTransaction(List<GsqScheduleResult> requestList,
                                              List<GsqScheduleResult> initialResultList) {
        return this.executeInTransaction(() -> {
            List<GsqScheduleResult> currentList = new ArrayList<>();
            for (int index = 0; index < requestList.size(); index++) {
                GsqScheduleResult current = this.reloadAndValidateOperationResult(requestList.get(index).getId(),
                        "ui.gsq.schedule.changeMachine.resultNotFound", "转机台排程结果不存在或已失效");
                this.validateLockedSourceMachine(initialResultList.get(index), current);
                this.normalizeExistingOperationRequest(requestList.get(index), current, true);
                currentList.add(current);
            }
            List<String> machineCodes = currentList.stream().map(GsqScheduleResult::getMachineCode)
                    .collect(Collectors.toCollection(ArrayList::new));
            requestList.stream().map(GsqScheduleResult::getMachineCode).forEach(machineCodes::add);
            machineCodes = machineCodes.stream().distinct().collect(Collectors.toList());
            List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(currentList.get(0), machineCodes);
            int changedCount = gsqManualInsertRollingService.changeMachineAndRollBatch(requestList);
            List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(currentList.get(0), machineCodes);
            for (GsqScheduleResult request : requestList) {
                this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, request, beforeList, afterList);
            }
            return changedCount;
        });
    }

    /**
     * 执行单条转机台的行锁校验、滚动和审计。
     *
     * @param targetMachineCode 目标机台编码
     * @param scheduleResult 转机请求
     * @param persisted 获锁前读取的源记录
     * @return 更新行数
     * @throws ServiceException 记录并发变化、滚动或审计失败时抛出
     */
    private int changeMachineInsideTransaction(String targetMachineCode,
                                                GsqScheduleResult scheduleResult,
                                                GsqScheduleResult persisted) {
        GsqScheduleResult current = this.reloadAndValidateOperationResult(scheduleResult.getId(),
                "ui.gsq.schedule.changeMachine.resultNotFound", "转机台排程结果不存在或已失效");
        this.validateLockedSourceMachine(persisted, current);
        this.normalizeExistingOperationRequest(scheduleResult, current, true);
        scheduleResult.setMachineCode(targetMachineCode);
        List<String> machineCodes = Arrays.asList(current.getMachineCode(), targetMachineCode);
        List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(current, machineCodes);
        int changedCount = gsqManualInsertRollingService.changeMachineAndRoll(scheduleResult);
        List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(current, machineCodes);
        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult, beforeList, afterList);
        return changedCount;
    }

    /**
     * 在短事务内执行批量逻辑删除、滚动重排和审计写入。
     *
     * @param ids 规范化后的排程结果 ID
     * @param expectedLockKeyList 获锁前计算的锁键
     * @return 删除行数
     * @throws ServiceException 数据或锁定范围发生变化时抛出
     */
    private int deleteTasksInsideTransaction(List<Long> ids, List<String> expectedLockKeyList) {
        List<GsqScheduleResult> lockedTargetList = gsqScheduleResultMapper.selectBatchIdsForUpdate(ids);
        this.validateDeleteResults(ids, lockedTargetList);
        if (!expectedLockKeyList.equals(this.buildDeleteLockKeys(lockedTargetList))) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.operation.concurrentChanged", "排程状态已变化，请刷新后重试"));
        }

        List<GsqScheduleResult> sortedTargetList = lockedTargetList.stream()
                .sorted(Comparator.comparing(GsqScheduleResult::getId)).collect(Collectors.toList());
        List<String> machineCodes = sortedTargetList.stream().map(GsqScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        List<GsqScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(sortedTargetList.get(0), machineCodes);
        // 对齐胎圈：用 beforeList 构造 currentTargetList，保证行锁内最新值
        List<GsqScheduleResult> currentTargetList = new ArrayList<>();
        for (GsqScheduleResult target : sortedTargetList) {
            GsqScheduleResult before = beforeList.stream()
                    .filter(item -> Objects.equals(item.getId(), target.getId())).findFirst().orElse(null);
            if (before == null) {
                throw new ServiceException(this.resolveGsqMessage(
                        "ui.gsq.schedule.operation.concurrentChanged", "排程状态已变化，请刷新后重试"));
            }
            currentTargetList.add(before);
        }
        int deletedCount = gsqManualInsertRollingService.deleteAndRollBatch(currentTargetList);
        List<GsqScheduleResult> afterList = this.loadManualOpSnapshot(sortedTargetList.get(0), machineCodes);
        for (GsqScheduleResult currentTarget : currentTargetList) {
            this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_DELETE, currentTarget, beforeList, afterList);
        }
        return deletedCount;
    }

    // ==================== 请求规范化与校验 ====================

    /**
     * 规范化插单请求。
     *
     * @param scheduleResult 插单请求
     * @throws ServiceException 必填字段为空时抛出
     */
    private void normalizeInsertRequest(GsqScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.insertTask.empty", "插单排程结果不能为空"));
        }
        scheduleResult.setFactoryCode(StrUtil.blankToDefault(StrUtil.trim(scheduleResult.getFactoryCode()),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        if (scheduleResult.getScheduleDate() == null) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.insertTask.scheduleDateEmpty", "插单排程日期不能为空"));
        }
        scheduleResult.setScheduleDate(DateUtil.beginOfDay(scheduleResult.getScheduleDate()));
        scheduleResult.setMachineCode(StrUtil.trim(scheduleResult.getMachineCode()));
        scheduleResult.setBatchNo(StrUtil.trim(scheduleResult.getBatchNo()));
        scheduleResult.setSteelRingCode(StrUtil.trim(scheduleResult.getSteelRingCode()));
        if (StrUtil.isBlank(scheduleResult.getMachineCode())) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.machineCode.empty", "排程机台不能为空"));
        }
        if (StrUtil.isBlank(scheduleResult.getSteelRingCode())) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.insertTask.steelRingCodeEmpty", "插单钢丝圈不能为空"));
        }
        this.validateInsertShiftFields(scheduleResult);
    }

    /**
     * 校验插单班次字段：计划量和顺序成对，且至少有一个班次有效。
     *
     * @param scheduleResult 插单排程结果
     * @throws ServiceException 班次字段不符合插单契约时抛出
     */
    private void validateInsertShiftFields(GsqScheduleResult scheduleResult) {
        boolean hasPlanQty = false;
        for (int shiftOrder = 1; shiftOrder <= GsqScheduleConstants.GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            String planQtyField = String.format(GsqScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            String sequenceField = String.format(GsqScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
            String analysisField = String.format(GsqScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder);
            Object planQtyObj = scheduleResult.getFieldValueByFieldName(planQtyField);
            BigDecimal planQty = this.toBigDecimal(planQtyObj);
            Integer sequence = (Integer) scheduleResult.getFieldValueByFieldName(sequenceField);
            Object analysis = scheduleResult.getFieldValueByFieldName(analysisField);
            if (planQty == null) {
                if (sequence != null || analysis != null) {
                    throw new ServiceException(this.resolveGsqMessage(
                            "ui.gsq.schedule.insertTask.shiftPairRequired", "插单班次计划量和顺序必须成对填写"));
                }
                continue;
            }
            if (planQty.compareTo(BigDecimal.ZERO) <= 0 || sequence == null || sequence < 1) {
                throw new ServiceException(this.resolveGsqMessage(
                        "ui.gsq.schedule.insertTask.shiftPairRequired", "插单班次计划量和顺序必须成对填写"));
            }
            hasPlanQty = true;
        }
        if (!hasPlanQty) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.insertTask.planQtyRequired", "至少填写一个班次的计划量和顺序"));
        }
    }

    /**
     * 加载人工操作对应的数据库结果。
     *
     * @param scheduleResult  操作请求
     * @param emptyMessageKey ID 为空提示 key
     * @param emptyMessage    ID 为空默认提示
     * @param missingKey      记录不存在提示 key
     * @param missingMessage  记录不存在默认提示
     * @return 数据库当前结果
     * @throws ServiceException 参数或记录无效时抛出
     */
    private GsqScheduleResult loadOperationResult(GsqScheduleResult scheduleResult, String emptyMessageKey, String emptyMessage,
                                                   String missingKey, String missingMessage) {
        if (scheduleResult == null || scheduleResult.getId() == null) {
            throw new ServiceException(this.resolveGsqMessage(emptyMessageKey, emptyMessage));
        }
        GsqScheduleResult persisted = gsqScheduleResultMapper.selectById(scheduleResult.getId());
        if (persisted == null) {
            throw new ServiceException(this.resolveGsqMessage(missingKey, missingMessage));
        }
        return persisted;
    }

    /**
     * 获锁后重新查询并校验人工操作结果。
     *
     * @param resultId      结果 ID
     * @param missingKey    记录不存在提示 key
     * @param missingMessage 记录不存在默认提示
     * @return 数据库当前结果
     * @throws ServiceException 记录不存在或处于不可人工调整状态时抛出
     */
    private GsqScheduleResult reloadAndValidateOperationResult(Long resultId, String missingKey, String missingMessage) {
        GsqScheduleResult current = gsqScheduleResultMapper.selectById(resultId);
        if (current == null) {
            throw new ServiceException(this.resolveGsqMessage(missingKey, missingMessage));
        }
        if (gsqScheduleResultMapper.isReleasingOrTimeoutByIds(new Long[]{resultId}) > 0) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        if (!this.isEditableReleaseStatus(current.getIsRelease())) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.illegalReleaseTransition"));
        }
        return current;
    }

    /**
     * 使用数据库真实值规范化人工操作的范围字段。
     *
     * @param request          人工操作请求
     * @param persisted        数据库当前结果
     * @param keepTargetMachine 是否保留请求中的目标机台
     */
    private void normalizeExistingOperationRequest(GsqScheduleResult request, GsqScheduleResult persisted,
                                                     boolean keepTargetMachine) {
        String targetMachineCode = request.getMachineCode();
        request.setFactoryCode(StrUtil.blankToDefault(StrUtil.trim(persisted.getFactoryCode()),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        request.setScheduleDate(DateUtil.beginOfDay(persisted.getScheduleDate()));
        request.setBatchNo(persisted.getBatchNo());
        request.setOrderNo(persisted.getOrderNo());
        request.setSteelRingCode(persisted.getSteelRingCode());
        request.setTwiningDiscCode(persisted.getTwiningDiscCode());
        request.setProSize(persisted.getProSize());
        request.setTqBatchNo(persisted.getTqBatchNo());
        request.setStockQty(persisted.getStockQty());
        request.setTqClass1Plan(persisted.getTqClass1Plan());
        request.setTqClass2Plan(persisted.getTqClass2Plan());
        request.setTqClass3Plan(persisted.getTqClass3Plan());
        request.setTqClass4Plan(persisted.getTqClass4Plan());
        request.setTqClass5Plan(persisted.getTqClass5Plan());
        request.setTqClass6Plan(persisted.getTqClass6Plan());
        request.setIsRelease(persisted.getIsRelease());
        request.setDataSource(persisted.getDataSource());
        request.setRemark(persisted.getRemark());
        if (!keepTargetMachine) {
            this.normalizeProtectedShiftFields(request, persisted);
        }
        request.setMachineCode(keepTargetMachine ? StrUtil.trim(targetMachineCode) : persisted.getMachineCode());
        if (StrUtil.isBlank(request.getMachineCode())) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.machineCode.empty", "排程机台不能为空"));
        }
    }

    /**
     * 校验调量请求只修改计划量和原因分析。
     *
     * @param request   调量请求
     * @param persisted 数据库当前排程结果
     * @throws ServiceException 请求显式篡改非调量字段时抛出
     */
    void validateChangeQtyEditableFields(GsqScheduleResult request, GsqScheduleResult persisted) {
        this.validateProtectedField("factoryCode", request.getFactoryCode(), persisted.getFactoryCode());
        this.validateProtectedField("batchNo", request.getBatchNo(), persisted.getBatchNo());
        this.validateProtectedField("orderNo", request.getOrderNo(), persisted.getOrderNo());
        this.validateProtectedField("scheduleDate", request.getScheduleDate(), persisted.getScheduleDate());
        this.validateProtectedField("machineCode", request.getMachineCode(), persisted.getMachineCode());
        this.validateProtectedField("steelRingCode", request.getSteelRingCode(), persisted.getSteelRingCode());
        this.validateProtectedField("twiningDiscCode", request.getTwiningDiscCode(), persisted.getTwiningDiscCode());
        this.validateProtectedField("proSize", request.getProSize(), persisted.getProSize());
        this.validateProtectedField("tqBatchNo", request.getTqBatchNo(), persisted.getTqBatchNo());
        this.validateProtectedField("stockQty", request.getStockQty(), persisted.getStockQty());
        this.validateProtectedField("isRelease", request.getIsRelease(), persisted.getIsRelease());
        this.validateProtectedField("dataSource", request.getDataSource(), persisted.getDataSource());
        this.validateProtectedField("remark", request.getRemark(), persisted.getRemark());
        for (int shiftOrder = 1; shiftOrder <= GsqScheduleConstants.GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            this.validateProtectedShiftField(request, persisted,
                    String.format(GsqScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
            this.validateProtectedShiftField(request, persisted,
                    String.format(GsqScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder));
            this.validateProtectedShiftField(request, persisted,
                    String.format(GsqScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder));
            this.validateProtectedShiftField(request, persisted,
                    String.format(GsqScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder));
            this.validateProtectedShiftField(request, persisted,
                    String.format(GsqScheduleConstants.SHIFT_TASK_STATUS_FIELD_TEMPLATE, shiftOrder));
        }
    }

    /**
     * 使用数据库值覆盖调量请求中的班次受保护字段。
     *
     * @param request   调量请求
     * @param persisted 数据库当前排程结果
     */
    private void normalizeProtectedShiftFields(GsqScheduleResult request, GsqScheduleResult persisted) {
        List<String> protectedFieldTemplates = Arrays.asList(
                GsqScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE,
                GsqScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE,
                GsqScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE,
                GsqScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE,
                GsqScheduleConstants.SHIFT_TASK_STATUS_FIELD_TEMPLATE);
        for (int shiftOrder = 1; shiftOrder <= GsqScheduleConstants.GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            for (String fieldTemplate : protectedFieldTemplates) {
                String fieldName = String.format(fieldTemplate, shiftOrder);
                request.setFieldValueByFieldName(fieldName, persisted.getFieldValueByFieldName(fieldName));
            }
        }
    }

    /**
     * 校验单个班次受保护字段。
     *
     * @param request   调量请求
     * @param persisted 数据库当前排程结果
     * @param fieldName 字段名
     * @throws ServiceException 请求显式篡改受保护字段时抛出
     */
    private void validateProtectedShiftField(GsqScheduleResult request, GsqScheduleResult persisted, String fieldName) {
        this.validateProtectedField(fieldName, request.getFieldValueByFieldName(fieldName),
                persisted.getFieldValueByFieldName(fieldName));
    }

    /**
     * 校验单个受保护字段。
     *
     * @param fieldName      字段名
     * @param requestValue   请求值
     * @param persistedValue 数据库值
     * @throws ServiceException 请求值非空且与数据库值不同时抛出
     */
    private void validateProtectedField(String fieldName, Object requestValue, Object persistedValue) {
        if (requestValue == null || this.isSameFieldValue(requestValue, persistedValue)) {
            return;
        }
        throw new ServiceException(MessageFormat.format(
                I18nUtil.getMessage("ui.gsq.schedule.changeQty.fieldNotAllowed"), fieldName));
    }

    // ==================== 锁键构建 ====================

    /**
     * 生成排序后的机台锁键。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param machineCodes 机台编码
     * @return 已去重并按机台编码排序的锁键
     * @throws ServiceException 工厂或排程日期为空时抛出
     */
    List<String> buildMachineLockKeys(String factoryCode, Date scheduleDate, List<String> machineCodes) {
        String normalizedFactoryCode = StrUtil.trim(factoryCode);
        if (StrUtil.isBlank(normalizedFactoryCode) || scheduleDate == null) {
            throw new ServiceException(this.resolveGsqMessage("ui.gsq.schedule.operation.failed",
                    "人工排程锁范围不完整"));
        }
        if (machineCodes == null) {
            return Collections.emptyList();
        }
        List<String> machineLockKeyList = machineCodes.stream()
                .filter(StrUtil::isNotBlank).map(StrUtil::trim).distinct().sorted()
                .map(machineCode -> LOCK_PREFIX + normalizedFactoryCode + ":" + DateUtil.formatDate(scheduleDate)
                        + ":" + machineCode)
                .collect(Collectors.toList());
        if (machineLockKeyList.isEmpty()) {
            return machineLockKeyList;
        }
        machineLockKeyList.add(LOCK_PREFIX + normalizedFactoryCode + ":" + DateUtil.formatDate(scheduleDate)
                + CONSTRAINT_LOCK_SUFFIX);
        return machineLockKeyList.stream().distinct().sorted().collect(Collectors.toList());
    }

    /**
     * 规范化并校验目标机台编码。
     *
     * @param machineCode 目标机台编码
     * @return 去除首尾空白后的机台编码
     * @throws ServiceException 目标机台为空时抛出
     */
    private String normalizeTargetMachineCode(String machineCode) {
        String targetMachineCode = StrUtil.trim(machineCode);
        if (StrUtil.isBlank(targetMachineCode)) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.machineCode.empty", "排程机台不能为空"));
        }
        return targetMachineCode;
    }

    /**
     * 规范化批量请求并拒绝空记录、空 ID 和重复 ID。
     *
     * @param scheduleResultList 批量请求
     * @param emptyKey 空请求提示 key
     * @param emptyMessage 空请求默认提示
     * @return 保持原操作顺序的请求副本
     * @throws ServiceException 请求为空或存在重复 ID 时抛出
     */
    List<GsqScheduleResult> normalizeBatchRequests(List<GsqScheduleResult> scheduleResultList,
                                                     String emptyKey, String emptyMessage) {
        if (scheduleResultList == null || scheduleResultList.isEmpty()) {
            throw new ServiceException(this.resolveGsqMessage(emptyKey, emptyMessage));
        }
        Set<Long> resultIdSet = new LinkedHashSet<>();
        List<GsqScheduleResult> requestList = new ArrayList<>();
        for (GsqScheduleResult scheduleResult : scheduleResultList) {
            if (scheduleResult == null || scheduleResult.getId() == null) {
                throw new ServiceException(this.resolveGsqMessage(
                        "ui.gsq.schedule.operation.idEmpty", "排程结果ID不能为空"));
            }
            if (!resultIdSet.add(scheduleResult.getId())) {
                throw new ServiceException(this.resolveGsqMessage(
                        "ui.gsq.schedule.batch.duplicate", "批量操作包含重复排程结果"));
            }
            requestList.add(scheduleResult);
        }
        return requestList;
    }

    /**
     * 生成批量转机台所需的全局排序锁键。
     *
     * @param initialResultList 获锁前读取的源记录
     * @return 所有工厂、日期、源机台和目标机台的去重排序锁键
     */
    List<String> buildBatchChangeMachineLockKeys(List<GsqScheduleResult> initialResultList) {
        if (initialResultList == null) {
            return Collections.emptyList();
        }
        return initialResultList.stream()
                .flatMap(result -> this.buildMachineLockKeys(result.getFactoryCode(), result.getScheduleDate(),
                        Arrays.asList(result.getMachineCode(), result.getMachineCode())).stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 生成批量调量所需的全局排序锁键。
     *
     * @param initialResultList 获锁前读取的源记录
     * @return 所有工厂、日期、机台的去重排序锁键
     */
    List<String> buildBatchLockKeys(List<GsqScheduleResult> initialResultList) {
        if (initialResultList == null) {
            return Collections.emptyList();
        }
        return initialResultList.stream()
                .flatMap(result -> this.buildMachineLockKeys(result.getFactoryCode(), result.getScheduleDate(),
                        Collections.singletonList(result.getMachineCode())).stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 规范化批量删除 ID。
     *
     * @param ids 原始 ID
     * @return 去空、去重并排序后的 ID
     * @throws ServiceException 未提供有效 ID 时抛出
     */
    List<Long> normalizeDeleteIds(List<Long> ids) {
        List<Long> normalizedIds = ids == null ? Collections.emptyList()
                : ids.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (normalizedIds.isEmpty()) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.delete.idsEmpty", "删除排程结果不能为空"));
        }
        return normalizedIds;
    }

    /**
     * 校验批量删除记录完整且全部处于可编辑状态。
     *
     * @param ids 请求 ID
     * @param resultList 数据库排程结果
     * @throws ServiceException 记录缺失或任一状态非可编辑时抛出
     */
    void validateDeleteResults(List<Long> ids, List<GsqScheduleResult> resultList) {
        if (resultList == null || resultList.size() != ids.size()) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.result.notFound", "排程结果不存在或已删除"));
        }
        boolean containsInvalidStatus = resultList.stream()
                .anyMatch(result -> !this.isEditableReleaseStatus(result.getIsRelease()));
        if (containsInvalidStatus) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.delete.statusInvalid", "只有未发布/超时失败排程结果允许删除"));
        }
    }

    /**
     * 构建批量删除涉及的全局有序机台锁键。
     *
     * @param resultList 待删除排程结果
     * @return 去重并排序后的锁键
     * @throws ServiceException 记录缺少工厂、日期或机台时抛出
     */
    List<String> buildDeleteLockKeys(List<GsqScheduleResult> resultList) {
        boolean containsInvalidScope = resultList == null || resultList.stream().anyMatch(result -> result == null
                || StrUtil.isBlank(result.getFactoryCode()) || result.getScheduleDate() == null
                || StrUtil.isBlank(result.getMachineCode()));
        if (containsInvalidScope) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.operation.failed", "人工排程操作失败"));
        }
        List<String> machineLockKeyList = resultList.stream()
                .map(result -> LOCK_PREFIX + StrUtil.trim(result.getFactoryCode()) + ":"
                        + DateUtil.formatDate(result.getScheduleDate()) + ":" + StrUtil.trim(result.getMachineCode()))
                .distinct().sorted().collect(Collectors.toList());
        List<String> constraintLockKeyList = resultList.stream()
                .map(result -> LOCK_PREFIX + StrUtil.trim(result.getFactoryCode()) + ":"
                        + DateUtil.formatDate(result.getScheduleDate()) + CONSTRAINT_LOCK_SUFFIX)
                .distinct().collect(Collectors.toList());
        machineLockKeyList.addAll(constraintLockKeyList);
        return machineLockKeyList.stream().distinct().sorted().collect(Collectors.toList());
    }

    /**
     * 校验获锁后数据库源机台未变化。
     *
     * @param initialResult 获锁前读取的排程结果
     * @param currentResult 获锁后重新读取的排程结果
     * @throws ServiceException 源机台已变化时抛出
     */
    void validateLockedSourceMachine(GsqScheduleResult initialResult, GsqScheduleResult currentResult) {
        String initialMachineCode = initialResult == null ? null : StrUtil.trim(initialResult.getMachineCode());
        String currentMachineCode = currentResult == null ? null : StrUtil.trim(currentResult.getMachineCode());
        if (StrUtil.isBlank(initialMachineCode) || !Objects.equals(initialMachineCode, currentMachineCode)) {
            throw new ServiceException(this.resolveGsqMessage("ui.gsq.schedule.operation.failed",
                    "排程机台已变化，请刷新后重试"));
        }
    }

    // ==================== 锁与事务执行 ====================

    /**
     * 按规范化锁键排序后获取相关机台的联锁。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param machineCodes 机台编码
     * @param action       获锁后动作
     * @param <T>          返回类型
     * @return 动作结果
     * @throws ServiceException 锁获取失败或线程被中断时抛出
     */
    private <T> T executeWithMachineLocks(String factoryCode, Date scheduleDate, List<String> machineCodes,
                                            Supplier<T> action) {
        List<String> lockKeyList = this.buildMachineLockKeys(factoryCode, scheduleDate, machineCodes);
        if (lockKeyList.isEmpty()) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.machineCode.empty", "排程机台不能为空"));
        }
        return this.executeWithLockKeys(lockKeyList, action);
    }

    /**
     * 获取已排序的分布式锁键并执行操作。
     *
     * @param lockKeyList 分布式锁键
     * @param action 获锁后动作
     * @param <T> 返回类型
     * @return 动作结果
     * @throws ServiceException 锁获取失败或线程被中断时抛出
     */
    private <T> T executeWithLockKeys(List<String> lockKeyList, Supplier<T> action) {
        if (lockKeyList == null || lockKeyList.isEmpty()) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.machineCode.empty", "排程机台不能为空"));
        }
        RLock[] lockArray = lockKeyList.stream().map(redissonClient::getLock).toArray(RLock[]::new);
        RedissonMultiLock multiLock = new RedissonMultiLock(lockArray);
        boolean locked = false;
        try {
            locked = multiLock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operating"));
            }
            return action.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.operating"));
        } finally {
            if (locked) {
                multiLock.unlock();
            }
        }
    }

    /**
     * 在独立短事务中执行业务修改与日志写入。
     *
     * @param action 事务动作
     * @param <T>    返回类型
     * @return 事务动作结果
     * @throws ServiceException 事务未返回结果时抛出
     */
    private <T> T executeInTransaction(Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        T result = transactionTemplate.execute(status -> action.get());
        if (result == null) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.operation.failed", "人工排程操作失败"));
        }
        return result;
    }

    // ==================== 快照加载与行锁校验 ====================

    /**
     * 加载人工操作涉及机台的排程快照。
     *
     * @param reference    排程范围参考
     * @param machineCodes 机台编码
     * @return 当前有效排程结果快照
     */
    private List<GsqScheduleResult> loadManualOpSnapshot(GsqScheduleResult reference, List<String> machineCodes) {
        if (reference == null || reference.getScheduleDate() == null || machineCodes.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(GsqScheduleResult::getScheduleDate, reference.getScheduleDate());
        wrapper.eq(StrUtil.isNotBlank(reference.getBatchNo()), GsqScheduleResult::getBatchNo, reference.getBatchNo());
        wrapper.in(GsqScheduleResult::getMachineCode, machineCodes.stream().filter(StrUtil::isNotBlank)
                .map(StrUtil::trim).distinct().collect(Collectors.toList()));
        wrapper.orderByAsc(GsqScheduleResult::getMachineCode, GsqScheduleResult::getId);
        return gsqScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 加锁并校验人工滚动窗口内的全部结果。
     *
     * <p>先按机台范围读取候选主键，再按排序后的主键加数据库行锁，并重新读取范围。
     * 这样发布状态修改与人工滚动不会在"校验完成、写入之前"互相覆盖。</p>
     *
     * @param reference    排程范围参考
     * @param machineCodes 受影响机台编码
     * @return 已加锁且全部允许人工编辑的排程结果快照
     * @throws ServiceException 结果集合或发布状态并发变化时抛出
     */
    private List<GsqScheduleResult> lockAndValidateManualOpSnapshot(GsqScheduleResult reference,
                                                                     List<String> machineCodes) {
        List<GsqScheduleResult> candidateList = this.loadManualOpSnapshot(reference, machineCodes);
        List<Long> candidateIds = candidateList.stream().map(GsqScheduleResult::getId).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList());
        if (!candidateIds.isEmpty()) {
            List<GsqScheduleResult> lockedList = gsqScheduleResultMapper.selectBatchIdsForUpdate(candidateIds);
            if (lockedList == null || lockedList.size() != candidateIds.size()) {
                throw new ServiceException(this.resolveGsqMessage(
                        "ui.gsq.schedule.operation.concurrentChanged", "排程状态已变化，请刷新后重试"));
            }
        }
        List<GsqScheduleResult> lockedSnapshot = this.loadManualOpSnapshot(reference, machineCodes);
        Set<Long> lockedIds = lockedSnapshot.stream().map(GsqScheduleResult::getId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!lockedIds.equals(new LinkedHashSet<>(candidateIds))) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.operation.concurrentChanged", "排程状态已变化，请刷新后重试"));
        }
        boolean containsNonEditableResult = lockedSnapshot.stream()
                .anyMatch(result -> !this.isEditableReleaseStatus(result.getIsRelease()));
        if (containsNonEditableResult) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return lockedSnapshot;
    }

    // ==================== 审计日志 ====================

    /**
     * 记录人工操作及其前后6班次计划量快照。
     *
     * <p>钢丝圈 GsqDispatcherLog 表无 affectedBeforeJson/affectedAfterJson 字段，
     * 此处仅记录 beforeClassNPlan/afterClassNPlan 等6班次计划量字段。</p>
     *
     * @param operType      操作类型
     * @param scheduleResult 操作请求
     * @param beforeList    操作前快照
     * @param afterList     操作后快照
     */
    private void recordDispatcherLog(String operType, GsqScheduleResult scheduleResult,
                                      List<GsqScheduleResult> beforeList, List<GsqScheduleResult> afterList) {
        this.recordDispatcherLogWithReason(operType, scheduleResult, beforeList, afterList, null);
    }

    /**
     * 记录人工操作及其前后6班次计划量快照（带操作原因）。
     *
     * @param operType      操作类型
     * @param scheduleResult 操作请求
     * @param beforeList    操作前快照
     * @param afterList     操作后快照
     * @param reason        操作原因（自动滚动场景使用）
     */
    private void recordDispatcherLogWithReason(String operType, GsqScheduleResult scheduleResult,
                                                List<GsqScheduleResult> beforeList,
                                                List<GsqScheduleResult> afterList, String reason) {
        GsqDispatcherLog dispatcherLog = new GsqDispatcherLog();
        dispatcherLog.setScheduleId(scheduleResult.getId());
        dispatcherLog.setOperType(operType);
        dispatcherLog.setScheduleDate(scheduleResult.getScheduleDate());
        dispatcherLog.setSteelRingCode(scheduleResult.getSteelRingCode());
        if (StrUtil.isNotBlank(reason)) {
            dispatcherLog.setRemark(reason);
        }
        GsqScheduleResult beforePrimary = beforeList.stream()
                .filter(item -> Objects.equals(item.getId(), scheduleResult.getId())).findFirst().orElse(null);
        if (beforePrimary != null) {
            this.copyBeforePlanQty(dispatcherLog, beforePrimary);
        }
        boolean deleteOperation = ApsConstant.DISPATCHER_OPER_DELETE.equals(operType);
        if (!deleteOperation) {
            this.copyAfterPlanQty(dispatcherLog, scheduleResult);
        }
        if (gsqDispatcherLogMapper.insertGsqDispatcherLog(dispatcherLog) != 1) {
            throw new ServiceException(this.resolveGsqMessage(
                    "ui.gsq.schedule.operation.failed", "人工排程操作失败"));
        }
    }

    /**
     * 复制操作前计划量到调度日志兼容字段。
     *
     * @param dispatcherLog 调度日志
     * @param scheduleResult 排程结果
     */
    private void copyBeforePlanQty(GsqDispatcherLog dispatcherLog, GsqScheduleResult scheduleResult) {
        dispatcherLog.setBeforeMachineCode(scheduleResult.getMachineCode());
        dispatcherLog.setBeforeClass1Plan(scheduleResult.getClass1PlanQty());
        dispatcherLog.setBeforeClass2Plan(scheduleResult.getClass2PlanQty());
        dispatcherLog.setBeforeClass3Plan(scheduleResult.getClass3PlanQty());
        dispatcherLog.setBeforeClass4Plan(scheduleResult.getClass4PlanQty());
        dispatcherLog.setBeforeClass5Plan(scheduleResult.getClass5PlanQty());
        dispatcherLog.setBeforeClass6Plan(scheduleResult.getClass6PlanQty());
    }

    /**
     * 复制操作后计划量到调度日志兼容字段。
     *
     * @param dispatcherLog 调度日志
     * @param scheduleResult 排程结果
     */
    private void copyAfterPlanQty(GsqDispatcherLog dispatcherLog, GsqScheduleResult scheduleResult) {
        dispatcherLog.setAfterMachineCode(scheduleResult.getMachineCode());
        dispatcherLog.setAfterClass1Plan(scheduleResult.getClass1PlanQty());
        dispatcherLog.setAfterClass2Plan(scheduleResult.getClass2PlanQty());
        dispatcherLog.setAfterClass3Plan(scheduleResult.getClass3PlanQty());
        dispatcherLog.setAfterClass4Plan(scheduleResult.getClass4PlanQty());
        dispatcherLog.setAfterClass5Plan(scheduleResult.getClass5PlanQty());
        dispatcherLog.setAfterClass6Plan(scheduleResult.getClass6PlanQty());
    }

    // ==================== 辅助工具 ====================

    /**
     * 判断释放状态是否可编辑。
     *
     * <p>对齐 {@code GsqRollingUpdateServiceImpl.isEditableReleaseStatus}，
     * 仅未发布(0)、超时失败(3)允许人工操作；已发布(1)、发布中(2)不可编辑。</p>
     *
     * @param isRelease 释放状态
     * @return true 表示可编辑
     */
    private boolean isEditableReleaseStatus(String isRelease) {
        if (StrUtil.isBlank(isRelease)) {
            return true;
        }
        return ApsConstant.NO_RELEASE.equals(isRelease)
                || "3".equals(isRelease);
    }

    /**
     * 比较两个字段值，BigDecimal 忽略小数位差异。
     *
     * @param currentValue  当前值
     * @param snapshotValue 快照值
     * @return true 表示业务值相等
     */
    private boolean isSameFieldValue(Object currentValue, Object snapshotValue) {
        if (currentValue instanceof BigDecimal && snapshotValue instanceof BigDecimal) {
            return ((BigDecimal) currentValue).compareTo((BigDecimal) snapshotValue) == 0;
        }
        if (currentValue instanceof Number && snapshotValue instanceof Number) {
            return ((Number) currentValue).doubleValue() == ((Number) snapshotValue).doubleValue();
        }
        return Objects.equals(currentValue, snapshotValue);
    }

    /**
     * 将对象转换为 BigDecimal，null/非数字返回 null。
     *
     * @param value 原始值
     * @return BigDecimal 值
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 读取钢丝圈排程国际化提示，未命中时回退默认文案。
     *
     * @param messageKey     国际化 key
     * @param defaultMessage 默认提示
     * @return 当前语言环境下的提示文案
     */
    private String resolveGsqMessage(String messageKey, String defaultMessage) {
        String message = I18nUtil.getMessage(messageKey);
        return StringUtils.isBlank(message) || messageKey.equals(message) ? defaultMessage : message;
    }
}
