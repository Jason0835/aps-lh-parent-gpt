package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.enums.TmReleaseStatusTransition;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.validator.TmInsertPositionValidator;
import com.zlt.aps.tm.mapper.TmDispatcherLogMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 胎面人工排程操作统一入口。
 *
 * <p>该门面统一处理请求规范化、数据库真实状态读取、多机台锁、短事务、快照和调度日志，
 * 避免插单、调量、转机台和撤销从不同入口绕过同一组安全约束。</p>
 */
@Service
@RequiredArgsConstructor
public class TmManualOperationFacade {

    private static final String LOCK_PREFIX = "TM_SCHEDULE:OPER_LOCK:";

    private static final String UNDO_STATUS_NORMAL = "0";

    private static final String UNDO_STATUS_DONE = "1";

    private final RedissonClient redissonClient;

    private final PlatformTransactionManager platformTransactionManager;

    private final TmScheduleResultMapper tmScheduleResultMapper;

    private final TmDispatcherLogMapper tmDispatcherLogMapper;

    private final TmManualInsertRollingService tmManualInsertRollingService;

    /**
     * 执行人工插单。
     *
     * @param scheduleResult 插单排程结果
     * @return 新增行数
     * @throws ServiceException 参数、锁或插单位置校验失败时抛出
     */
    public int insertTask(TmScheduleResult scheduleResult) {
        this.normalizeInsertRequest(scheduleResult);
        List<String> machineCodes = Collections.singletonList(scheduleResult.getMachineCode());
        return this.executeWithMachineLocks(scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate(), machineCodes,
                () -> this.executeInTransaction(() -> {
                    this.validateInsertAfterSecondSequence(scheduleResult);
                    List<TmScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(scheduleResult, machineCodes);
                    int changedCount = tmManualInsertRollingService.insertAndRoll(scheduleResult);
                    List<TmScheduleResult> afterList = this.loadManualOpSnapshot(scheduleResult, machineCodes);
                    scheduleResult.setBaseVale(scheduleResult.getId());
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
    public int changeQty(TmScheduleResult scheduleResult) {
        TmScheduleResult persisted = this.loadOperationResult(scheduleResult,
                "ui.data.alert.tm.schedule.changeQtyIdEmpty", "调量排程结果不能为空",
                "ui.data.alert.tm.schedule.changeQtyResultNotFound", "调量排程结果不存在或已失效");
        this.normalizeExistingOperationRequest(scheduleResult, persisted, false);
        List<String> machineCodes = Collections.singletonList(persisted.getMachineCode());
        return this.executeWithMachineLocks(persisted.getFactoryCode(), persisted.getScheduleDate(), machineCodes,
                () -> this.executeInTransaction(() -> {
                    TmScheduleResult current = this.reloadAndValidateOperationResult(scheduleResult.getId(),
                            "ui.data.alert.tm.schedule.changeQtyResultNotFound", "调量排程结果不存在或已失效");
                    this.validateLockedSourceMachine(persisted, current);
                    this.normalizeExistingOperationRequest(scheduleResult, current, false);
                    List<TmScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(current, machineCodes);
                    int changedCount = tmManualInsertRollingService.changeQtyAndRoll(scheduleResult);
                    List<TmScheduleResult> afterList = this.loadManualOpSnapshot(current, machineCodes);
                    scheduleResult.setBaseVale(scheduleResult.getId());
                    this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult, beforeList, afterList);
                    return changedCount;
                }));
    }

    /**
     * 执行人工转机台。
     *
     * @param scheduleResult 转机台请求
     * @return 更新行数
     * @throws ServiceException 记录不存在、正在发布或任一机台锁获取失败时抛出
     */
    public int changeMachine(TmScheduleResult scheduleResult) {
        TmScheduleResult persisted = this.loadOperationResult(scheduleResult,
                "ui.data.alert.tm.schedule.changeMachineIdEmpty", "转机台排程结果不能为空",
                "ui.data.alert.tm.schedule.changeMachineResultNotFound", "转机台排程结果不存在或已失效");
        String targetMachineCode = StrUtil.trim(scheduleResult.getMachineCode());
        this.normalizeExistingOperationRequest(scheduleResult, persisted, true);
        scheduleResult.setMachineCode(targetMachineCode);
        List<String> machineCodes = Arrays.asList(persisted.getMachineCode(), targetMachineCode);
        return this.executeWithMachineLocks(persisted.getFactoryCode(), persisted.getScheduleDate(), machineCodes,
                () -> this.executeInTransaction(() -> {
                    TmScheduleResult current = this.reloadAndValidateOperationResult(scheduleResult.getId(),
                            "ui.data.alert.tm.schedule.changeMachineResultNotFound", "转机台排程结果不存在或已失效");
                    this.validateLockedSourceMachine(persisted, current);
                    this.normalizeExistingOperationRequest(scheduleResult, current, true);
                    scheduleResult.setMachineCode(targetMachineCode);
                    List<TmScheduleResult> beforeList = this.lockAndValidateManualOpSnapshot(current, machineCodes);
                    int changedCount = tmManualInsertRollingService.changeMachineAndRoll(scheduleResult);
                    List<TmScheduleResult> afterList = this.loadManualOpSnapshot(current, machineCodes);
                    scheduleResult.setBaseVale(scheduleResult.getId());
                    this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult, beforeList, afterList);
                    return changedCount;
                }));
    }

    /**
     * 撤销指定的最新人工操作。
     *
     * @param dispatcherLogId 调度日志 ID
     * @return 恢复行数
     * @throws ServiceException 日志非最新、快照不一致或锁获取失败时抛出
     */
    public int undoLastOperation(Long dispatcherLogId) {
        TmDispatcherLog dispatcherLog = this.loadUndoLog(dispatcherLogId);
        List<TmScheduleResult> beforeList = this.parseSnapshot(dispatcherLog.getAffectedBeforeJson());
        List<TmScheduleResult> afterList = this.parseSnapshot(dispatcherLog.getAffectedAfterJson());
        List<String> machineCodes = this.collectSnapshotMachineCodes(beforeList, afterList);
        String factoryCode = StrUtil.blankToDefault(StrUtil.trim(dispatcherLog.getFactoryCode()),
                FactoryConstant.DEFAULT_FACTORY_CODE);
        Date scheduleDate = dispatcherLog.getScheduleDate() == null ? null : DateUtil.beginOfDay(dispatcherLog.getScheduleDate());
        return this.executeWithMachineLocks(factoryCode, scheduleDate, machineCodes,
                () -> this.executeInTransaction(() -> this.undoInsideTransaction(dispatcherLogId)));
    }

    /**
     * 在短事务内执行撤销并再次读取、校验日志和当前排程状态。
     *
     * @param dispatcherLogId 调度日志 ID
     * @return 恢复行数
     * @throws ServiceException 日志或排程状态已变化时抛出
     */
    private int undoInsideTransaction(Long dispatcherLogId) {
        TmDispatcherLog dispatcherLog = this.loadUndoLog(dispatcherLogId);
        List<TmScheduleResult> beforeList = this.parseSnapshot(dispatcherLog.getAffectedBeforeJson());
        List<TmScheduleResult> afterList = this.parseSnapshot(dispatcherLog.getAffectedAfterJson());
        List<String> machineCodes = this.collectSnapshotMachineCodes(beforeList, afterList);
        this.validateLatestUndoLog(dispatcherLog, machineCodes);
        List<TmScheduleResult> currentList = this.loadManualOpSnapshot(this.snapshotReference(dispatcherLog), machineCodes);
        if (!this.isSameSnapshot(currentList, afterList)) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoSnapshotChanged",
                    "当前排程已发生变化，不能撤销该操作"));
        }

        Set<Long> beforeIds = beforeList.stream().map(TmScheduleResult::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> insertedIds = afterList.stream().map(TmScheduleResult::getId)
                .filter(Objects::nonNull).filter(id -> !beforeIds.contains(id)).collect(Collectors.toSet());
        int restoredCount = 0;
        for (Long insertedId : insertedIds) {
            int deletedRows = tmScheduleResultMapper.deleteById(insertedId);
            if (deletedRows != 1) {
                throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoSnapshotChanged",
                        "当前排程已发生变化，不能撤销该操作"));
            }
            restoredCount += deletedRows;
        }
        for (TmScheduleResult beforeResult : beforeList) {
            TmScheduleResult currentResult = tmScheduleResultMapper.selectById(beforeResult.getId());
            if (currentResult == null) {
                throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoSnapshotChanged",
                        "当前排程已发生变化，不能撤销该操作"));
            }
            this.copySchedulingFields(currentResult, beforeResult);
            int updatedRows = tmScheduleResultMapper.updateById(currentResult);
            if (updatedRows != 1) {
                throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoSnapshotChanged",
                        "当前排程已发生变化，不能撤销该操作"));
            }
            restoredCount += updatedRows;
        }

        dispatcherLog.setUndoStatus(UNDO_STATUS_DONE);
        if (tmDispatcherLogMapper.updateById(dispatcherLog) != 1) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationFailed",
                    "人工排程操作失败"));
        }
        this.recordUndoLog(dispatcherLog, afterList, beforeList);
        return restoredCount;
    }

    /**
     * 加载并校验人工操作日志。
     *
     * @param dispatcherLogId 调度日志 ID
     * @return 可撤销日志
     * @throws ServiceException 日志不存在或已撤销时抛出
     */
    private TmDispatcherLog loadUndoLog(Long dispatcherLogId) {
        if (dispatcherLogId == null) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoLogNotFound", "调度日志不存在"));
        }
        TmDispatcherLog dispatcherLog = tmDispatcherLogMapper.selectById(dispatcherLogId);
        if (dispatcherLog == null) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoLogNotFound", "调度日志不存在"));
        }
        if (UNDO_STATUS_DONE.equals(dispatcherLog.getUndoStatus())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoAlreadyDone",
                    "该操作已撤销，不可重复撤销"));
        }
        return dispatcherLog;
    }

    /**
     * 校验目标日志是相关机台的最新未撤销人工操作。
     *
     * @param dispatcherLog 目标日志
     * @param machineCodes  相关机台编码
     * @throws ServiceException 存在更新的人工操作时抛出
     */
    private void validateLatestUndoLog(TmDispatcherLog dispatcherLog, List<String> machineCodes) {
        LambdaQueryWrapper<TmDispatcherLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmDispatcherLog::getFactoryCode, dispatcherLog.getFactoryCode());
        wrapper.eq(TmDispatcherLog::getScheduleDate, dispatcherLog.getScheduleDate());
        wrapper.eq(TmDispatcherLog::getUndoStatus, UNDO_STATUS_NORMAL);
        wrapper.in(TmDispatcherLog::getOperType, Arrays.asList(ApsConstant.DISPATCHER_OPER_INSERT_ORDER,
                ApsConstant.DISPATCHER_OPER_PLAN, ApsConstant.DISPATCHER_OPER_MACHINE));
        wrapper.orderByDesc(TmDispatcherLog::getId);
        List<TmDispatcherLog> candidateList = tmDispatcherLogMapper.selectList(wrapper);
        TmDispatcherLog latestLog = candidateList.stream()
                .filter(item -> machineCodes.contains(item.getBeforeMachineCode())
                        || machineCodes.contains(item.getAfterMachineCode()))
                .findFirst().orElse(null);
        if (latestLog == null || !Objects.equals(latestLog.getId(), dispatcherLog.getId())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.undoNotLatest",
                    "只能撤销相关机台的最新人工操作"));
        }
    }

    /**
     * 规范化插单请求。
     *
     * @param scheduleResult 插单请求
     * @throws ServiceException 必填字段为空时抛出
     */
    private void normalizeInsertRequest(TmScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.insertTaskEmpty", "插单排程结果不能为空"));
        }
        scheduleResult.setFactoryCode(StrUtil.blankToDefault(StrUtil.trim(scheduleResult.getFactoryCode()),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        if (scheduleResult.getScheduleDate() == null) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.insertScheduleDateEmpty",
                    "插单排程日期不能为空"));
        }
        scheduleResult.setScheduleDate(DateUtil.beginOfDay(scheduleResult.getScheduleDate()));
        scheduleResult.setMachineCode(StrUtil.trim(scheduleResult.getMachineCode()));
        scheduleResult.setBatchNo(StrUtil.trim(scheduleResult.getBatchNo()));
        scheduleResult.setTreadCode(StrUtil.trim(scheduleResult.getTreadCode()));
        if (StrUtil.isBlank(scheduleResult.getMachineCode())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.machineCodeEmpty", "排程机台不能为空"));
        }
        if (StrUtil.isBlank(scheduleResult.getTreadCode())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.insertTreadCodeEmpty", "插单胎面不能为空"));
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
    private TmScheduleResult loadOperationResult(TmScheduleResult scheduleResult, String emptyMessageKey, String emptyMessage,
                                                  String missingKey, String missingMessage) {
        if (scheduleResult == null || scheduleResult.getId() == null) {
            throw new ServiceException(this.resolveTmMessage(emptyMessageKey, emptyMessage));
        }
        TmScheduleResult persisted = tmScheduleResultMapper.selectById(scheduleResult.getId());
        if (persisted == null) {
            throw new ServiceException(this.resolveTmMessage(missingKey, missingMessage));
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
    private TmScheduleResult reloadAndValidateOperationResult(Long resultId, String missingKey, String missingMessage) {
        TmScheduleResult current = tmScheduleResultMapper.selectById(resultId);
        if (current == null) {
            throw new ServiceException(this.resolveTmMessage(missingKey, missingMessage));
        }
        if (tmScheduleResultMapper.isReleasingOrTimeoutByIds(new Long[]{resultId}) > 0) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        if (!TmReleaseStatusTransition.isEditable(current.getReleaseStatus())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.illegalReleaseTransition"));
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
    private void normalizeExistingOperationRequest(TmScheduleResult request, TmScheduleResult persisted,
                                                     boolean keepTargetMachine) {
        String targetMachineCode = request.getMachineCode();
        request.setFactoryCode(StrUtil.blankToDefault(StrUtil.trim(persisted.getFactoryCode()),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        request.setScheduleDate(DateUtil.beginOfDay(persisted.getScheduleDate()));
        request.setBatchNo(persisted.getBatchNo());
        request.setOrderNo(persisted.getOrderNo());
        request.setTreadCode(persisted.getTreadCode());
        request.setGlueCode(persisted.getGlueCode());
        request.setBaseGlueCode(persisted.getBaseGlueCode());
        request.setWholeGlueCode(persisted.getWholeGlueCode());
        request.setGlueSeq(persisted.getGlueSeq());
        request.setMouthPlateCode(persisted.getMouthPlateCode());
        request.setTailFlag(persisted.getTailFlag());
        request.setReleaseStatus(persisted.getReleaseStatus());
        request.setMachineCode(keepTargetMachine ? StrUtil.trim(targetMachineCode) : persisted.getMachineCode());
        if (StrUtil.isBlank(request.getMachineCode())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.machineCodeEmpty", "排程机台不能为空"));
        }
    }

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
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationFailed",
                    "人工排程锁范围不完整"));
        }
        if (machineCodes == null) {
            return Collections.emptyList();
        }
        return machineCodes.stream().filter(StrUtil::isNotBlank).map(StrUtil::trim).distinct().sorted()
                .map(machineCode -> LOCK_PREFIX + normalizedFactoryCode + ":" + DateUtil.formatDate(scheduleDate)
                        + ":" + machineCode)
                .collect(Collectors.toList());
    }

    /**
     * 校验获锁后数据库源机台未变化。
     *
     * @param initialResult 获锁前读取的排程结果
     * @param currentResult 获锁后重新读取的排程结果
     * @throws ServiceException 源机台已变化时抛出
     */
    void validateLockedSourceMachine(TmScheduleResult initialResult, TmScheduleResult currentResult) {
        String initialMachineCode = initialResult == null ? null : StrUtil.trim(initialResult.getMachineCode());
        String currentMachineCode = currentResult == null ? null : StrUtil.trim(currentResult.getMachineCode());
        if (StrUtil.isBlank(initialMachineCode) || !Objects.equals(initialMachineCode, currentMachineCode)) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationFailed",
                    "排程机台已变化，请刷新后重试"));
        }
    }
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
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.machineCodeEmpty", "排程机台不能为空"));
        }
        RLock[] lockArray = lockKeyList.stream().map(redissonClient::getLock).toArray(RLock[]::new);
        RedissonMultiLock multiLock = new RedissonMultiLock(lockArray);
        boolean locked = false;
        try {
            locked = multiLock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operating"));
            }
            return action.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operating"));
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
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationFailed", "人工排程操作失败"));
        }
        return result;
    }

    /**
     * 加载人工操作涉及机台的排程快照。
     *
     * @param reference    排程范围参考
     * @param machineCodes 机台编码
     * @return 当前有效排程结果快照
     */
    private List<TmScheduleResult> loadManualOpSnapshot(TmScheduleResult reference, List<String> machineCodes) {
        if (reference == null || reference.getScheduleDate() == null || machineCodes.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, reference.getScheduleDate());
        wrapper.eq(StrUtil.isNotBlank(reference.getBatchNo()), TmScheduleResult::getBatchNo, reference.getBatchNo());
        wrapper.in(TmScheduleResult::getMachineCode, machineCodes.stream().filter(StrUtil::isNotBlank)
                .map(StrUtil::trim).distinct().collect(Collectors.toList()));
        wrapper.orderByAsc(TmScheduleResult::getMachineCode, TmScheduleResult::getId);
        return tmScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 加锁并校验人工滚动窗口内的全部结果。
     *
     * <p>先按机台范围读取候选主键，再按排序后的主键加数据库行锁，并重新读取范围。
     * 这样发布状态修改与人工滚动不会在“校验完成、写入之前”互相覆盖。</p>
     *
     * @param reference    排程范围参考
     * @param machineCodes 受影响机台编码
     * @return 已加锁且全部允许人工编辑的排程结果快照
     * @throws ServiceException 结果集合或发布状态并发变化时抛出
     */
    private List<TmScheduleResult> lockAndValidateManualOpSnapshot(TmScheduleResult reference,
                                                                    List<String> machineCodes) {
        List<TmScheduleResult> candidateList = this.loadManualOpSnapshot(reference, machineCodes);
        List<Long> candidateIds = candidateList.stream().map(TmScheduleResult::getId).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList());
        if (!candidateIds.isEmpty()) {
            List<TmScheduleResult> lockedList = tmScheduleResultMapper.selectBatchIdsForUpdate(candidateIds);
            if (lockedList == null || lockedList.size() != candidateIds.size()) {
                throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationConcurrentChanged",
                        "排程状态已变化，请刷新后重试"));
            }
        }
        List<TmScheduleResult> lockedSnapshot = this.loadManualOpSnapshot(reference, machineCodes);
        Set<Long> lockedIds = lockedSnapshot.stream().map(TmScheduleResult::getId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!lockedIds.equals(new LinkedHashSet<>(candidateIds))) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationConcurrentChanged",
                    "排程状态已变化，请刷新后重试"));
        }
        boolean containsNonEditableResult = lockedSnapshot.stream()
                .anyMatch(result -> !TmReleaseStatusTransition.isEditable(result.getReleaseStatus()));
        if (containsNonEditableResult) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return lockedSnapshot;
    }
    /**
     * 记录人工操作及其前后快照。
     *
     * @param operType      操作类型
     * @param scheduleResult 操作请求
     * @param beforeList    操作前快照
     * @param afterList     操作后快照
     */
    private void recordDispatcherLog(String operType, TmScheduleResult scheduleResult,
                                      List<TmScheduleResult> beforeList, List<TmScheduleResult> afterList) {
        TmDispatcherLog dispatcherLog = new TmDispatcherLog();
        dispatcherLog.setScheduleId(scheduleResult.getId());
        dispatcherLog.setOperType(operType);
        dispatcherLog.setScheduleDate(scheduleResult.getScheduleDate());
        dispatcherLog.setTreadCode(scheduleResult.getTreadCode());
        dispatcherLog.setFactoryCode(scheduleResult.getFactoryCode());
        dispatcherLog.setBatchNo(scheduleResult.getBatchNo());
        TmScheduleResult beforePrimary = beforeList.stream()
                .filter(item -> Objects.equals(item.getId(), scheduleResult.getId())).findFirst().orElse(null);
        if (beforePrimary != null) {
            this.copyBeforePlanQty(dispatcherLog, beforePrimary);
        }
        this.copyAfterPlanQty(dispatcherLog, scheduleResult);
        dispatcherLog.setUndoStatus(UNDO_STATUS_NORMAL);
        dispatcherLog.setAffectedBeforeJson(JSON.toJSONString(beforeList));
        dispatcherLog.setAffectedAfterJson(JSON.toJSONString(afterList));
        if (tmDispatcherLogMapper.insert(dispatcherLog) != 1) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationFailed",
                    "人工排程操作失败"));
        }
    }

    /**
     * 新增撤销审计日志。
     *
     * @param sourceLog  原人工操作日志
     * @param beforeList 撤销前快照
     * @param afterList  撤销后快照
     */
    private void recordUndoLog(TmDispatcherLog sourceLog, List<TmScheduleResult> beforeList,
                               List<TmScheduleResult> afterList) {
        TmDispatcherLog undoLog = new TmDispatcherLog();
        undoLog.setScheduleId(sourceLog.getScheduleId());
        undoLog.setOperType(sourceLog.getOperType());
        undoLog.setScheduleDate(sourceLog.getScheduleDate());
        undoLog.setTreadCode(sourceLog.getTreadCode());
        undoLog.setFactoryCode(sourceLog.getFactoryCode());
        undoLog.setBatchNo(sourceLog.getBatchNo());
        undoLog.setBeforeMachineCode(sourceLog.getAfterMachineCode());
        undoLog.setAfterMachineCode(sourceLog.getBeforeMachineCode());
        undoLog.setUndoStatus(UNDO_STATUS_DONE);
        undoLog.setAffectedBeforeJson(JSON.toJSONString(beforeList));
        undoLog.setAffectedAfterJson(JSON.toJSONString(afterList));
        undoLog.setRemark("UNDO_SOURCE_LOG_ID=" + sourceLog.getId());
        if (tmDispatcherLogMapper.insert(undoLog) != 1) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationFailed",
                    "人工排程操作失败"));
        }
    }

    /**
     * 复制操作前计划量到调度日志兼容字段。
     *
     * @param dispatcherLog 调度日志
     * @param scheduleResult 排程结果
     */
    private void copyBeforePlanQty(TmDispatcherLog dispatcherLog, TmScheduleResult scheduleResult) {
        dispatcherLog.setBeforeMachineCode(scheduleResult.getMachineCode());
        dispatcherLog.setBeforeClass1PlanQty(scheduleResult.getClass1PlanQty());
        dispatcherLog.setBeforeClass2PlanQty(scheduleResult.getClass2PlanQty());
        dispatcherLog.setBeforeClass3PlanQty(scheduleResult.getClass3PlanQty());
        dispatcherLog.setBeforeClass4PlanQty(scheduleResult.getClass4PlanQty());
        dispatcherLog.setBeforeClass5PlanQty(scheduleResult.getClass5PlanQty());
        dispatcherLog.setBeforeClass6PlanQty(scheduleResult.getClass6PlanQty());
    }

    /**
     * 复制操作后计划量到调度日志兼容字段。
     *
     * @param dispatcherLog 调度日志
     * @param scheduleResult 排程结果
     */
    private void copyAfterPlanQty(TmDispatcherLog dispatcherLog, TmScheduleResult scheduleResult) {
        dispatcherLog.setAfterMachineCode(scheduleResult.getMachineCode());
        dispatcherLog.setAfterClass1PlanQty(scheduleResult.getClass1PlanQty());
        dispatcherLog.setAfterClass2PlanQty(scheduleResult.getClass2PlanQty());
        dispatcherLog.setAfterClass3PlanQty(scheduleResult.getClass3PlanQty());
        dispatcherLog.setAfterClass4PlanQty(scheduleResult.getClass4PlanQty());
        dispatcherLog.setAfterClass5PlanQty(scheduleResult.getClass5PlanQty());
        dispatcherLog.setAfterClass6PlanQty(scheduleResult.getClass6PlanQty());
    }

    /**
     * 校验人工插单不能插入已生产的第二个顺序之前。
     *
     * @param scheduleResult 插单请求
     * @throws ServiceException 插入位置非法时抛出
     */
    private void validateInsertAfterSecondSequence(TmScheduleResult scheduleResult) {
        Integer shiftOrder = TmInsertPositionValidator.resolveShiftOrder(scheduleResult);
        Integer insertSequence = TmInsertPositionValidator.resolveSequence(scheduleResult, shiftOrder);
        if (shiftOrder == null || insertSequence == null || StrUtil.isBlank(scheduleResult.getMachineCode())) {
            return;
        }
        List<TmScheduleResult> resultList = this.loadManualOpSnapshot(scheduleResult,
                Collections.singletonList(scheduleResult.getMachineCode()));
        List<Integer> inProductionSequenceList = resultList.stream()
                .filter(item -> TmInsertPositionValidator.getFinishQty(item, shiftOrder).compareTo(BigDecimal.ZERO) > 0)
                .map(item -> TmInsertPositionValidator.resolveSequence(item, shiftOrder))
                .filter(Objects::nonNull).sorted().collect(Collectors.toList());
        if (inProductionSequenceList.size() >= 2 && insertSequence <= inProductionSequenceList.get(1)) {
            TmScheduleErrorCodeEnum errorCode = TmScheduleErrorCodeEnum.TM_INSERT_POSITION_INVALID;
            throw new ServiceException(this.resolveTmMessage(errorCode.getMessageKey(), errorCode.getDefaultMessage()));
        }
    }

    /**
     * 判断数据库当前排程是否与操作后快照完全一致。
     *
     * @param currentList 当前排程
     * @param afterList   操作后快照
     * @return true 表示可安全撤销
     */
    private boolean isSameSnapshot(List<TmScheduleResult> currentList, List<TmScheduleResult> afterList) {
        Map<Long, TmScheduleResult> currentMap = currentList.stream().filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TmScheduleResult::getId, item -> item));
        Map<Long, TmScheduleResult> afterMap = afterList.stream().filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TmScheduleResult::getId, item -> item));
        if (!currentMap.keySet().equals(afterMap.keySet())) {
            return false;
        }
        return afterMap.entrySet().stream()
                .allMatch(entry -> this.isSameSchedulingFields(currentMap.get(entry.getKey()), entry.getValue()));
    }

    /**
     * 比较撤销所需的机台、状态、六班排程和完成量字段。
     *
     * @param current 当前排程
     * @param snapshot 快照排程
     * @return true 表示字段一致
     */
    private boolean isSameSchedulingFields(TmScheduleResult current, TmScheduleResult snapshot) {
        if (current == null || !Objects.equals(current.getMachineCode(), snapshot.getMachineCode())
                || !Objects.equals(current.getReleaseStatus(), snapshot.getReleaseStatus())) {
            return false;
        }
        List<String> fieldTemplateList = Arrays.asList(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE);
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            for (String fieldTemplate : fieldTemplateList) {
                String fieldName = String.format(fieldTemplate, shiftOrder);
                if (!this.isSameFieldValue(current.getFieldValueByFieldName(fieldName),
                        snapshot.getFieldValueByFieldName(fieldName))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 比较快照字段值，BigDecimal 忽略小数位差异。
     *
     * @param currentValue  当前值
     * @param snapshotValue 快照值
     * @return true 表示业务值相等
     */
    private boolean isSameFieldValue(Object currentValue, Object snapshotValue) {
        if (currentValue instanceof BigDecimal && snapshotValue instanceof BigDecimal) {
            return ((BigDecimal) currentValue).compareTo((BigDecimal) snapshotValue) == 0;
        }
        return Objects.equals(currentValue, snapshotValue);
    }

    /**
     * 恢复快照中的排程相关字段。
     *
     * @param target 当前数据库结果
     * @param source 操作前快照
     */
    private void copySchedulingFields(TmScheduleResult target, TmScheduleResult source) {
        target.setMachineCode(source.getMachineCode());
        target.setReleaseStatus(source.getReleaseStatus());
        List<String> fieldTemplateList = Arrays.asList(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE);
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            for (String fieldTemplate : fieldTemplateList) {
                String fieldName = String.format(fieldTemplate, shiftOrder);
                target.setFieldValueByFieldName(fieldName, source.getFieldValueByFieldName(fieldName));
            }
        }
    }

    /**
     * 从日志 JSON 解析排程快照。
     *
     * @param snapshotJson 快照 JSON
     * @return 排程快照列表
     */
    private List<TmScheduleResult> parseSnapshot(String snapshotJson) {
        return StrUtil.isBlank(snapshotJson) ? new ArrayList<>()
                : JSON.parseArray(snapshotJson, TmScheduleResult.class);
    }

    /**
     * 收集快照涉及的全部机台编码。
     *
     * @param beforeList 操作前快照
     * @param afterList  操作后快照
     * @return 去重后的机台列表
     */
    private List<String> collectSnapshotMachineCodes(List<TmScheduleResult> beforeList,
                                                      List<TmScheduleResult> afterList) {
        Set<String> machineCodeSet = new LinkedHashSet<>();
        beforeList.stream().map(TmScheduleResult::getMachineCode).filter(StrUtil::isNotBlank)
                .map(StrUtil::trim).forEach(machineCodeSet::add);
        afterList.stream().map(TmScheduleResult::getMachineCode).filter(StrUtil::isNotBlank)
                .map(StrUtil::trim).forEach(machineCodeSet::add);
        return new ArrayList<>(machineCodeSet);
    }

    /**
     * 使用日志构建快照查询范围参考。
     *
     * @param dispatcherLog 调度日志
     * @return 排程范围参考
     */
    private TmScheduleResult snapshotReference(TmDispatcherLog dispatcherLog) {
        TmScheduleResult reference = new TmScheduleResult();
        reference.setFactoryCode(dispatcherLog.getFactoryCode());
        reference.setBatchNo(dispatcherLog.getBatchNo());
        reference.setScheduleDate(dispatcherLog.getScheduleDate());
        return reference;
    }

    /**
     * 读取胎面排程国际化提示，未命中时回退默认文案。
     *
     * @param messageKey     国际化 key
     * @param defaultMessage 默认提示
     * @return 当前语言环境下的提示文案
     */
    private String resolveTmMessage(String messageKey, String defaultMessage) {
        String message = I18nUtil.getMessage(messageKey);
        return StringUtils.isBlank(message) || messageKey.equals(message) ? defaultMessage : message;
    }
}
