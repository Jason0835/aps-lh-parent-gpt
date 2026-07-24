package com.zlt.aps.tc.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.component.TcAutoScheduleExecutionGuard;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.engine.validator.TcInsertPositionValidator;
import com.zlt.aps.tc.mapper.TcDispatcherLogMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import com.zlt.aps.tc.service.TcOperationAuditContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 胎侧人工排程操作统一门面。
 *
 * <p>门面按稳定顺序持有源、目标机台 Redisson 锁，事务内重新加数据库行锁、校验任务版本，
 * 执行横表滚动并记录前后快照；业务或审计任一失败均回滚。</p>
 */
@Service
public class TcManualOperationFacade {

    /** 锁等待秒数。 */
    private static final long LOCK_WAIT_SECONDS = 3L;

    /** 锁租约秒数。 */
    private static final long LOCK_LEASE_SECONDS = 60L;

    private final RedissonClient redissonClient;

    private final PlatformTransactionManager transactionManager;

    private final TcScheduleResultMapper scheduleResultMapper;

    private final TcDispatcherLogMapper dispatcherLogMapper;

    private final TcManualInsertRollingService rollingService;

    private final TcManualMachineRuleValidator machineRuleValidator;

    private final TcAutoScheduleExecutionGuard autoScheduleExecutionGuard;

    private final TcAutoScheduleTaskService autoScheduleTaskService;

    /**
     * 构造人工排程操作门面。
     *
     * @param redissonClient Redisson 客户端
     * @param transactionManager 事务管理器
     * @param scheduleResultMapper 排程结果 Mapper
     * @param dispatcherLogMapper 调度日志 Mapper
     * @param rollingService 横表滚动服务
     * @param machineRuleValidator 目标机台规则校验器
     * @param autoScheduleExecutionGuard 自动排程互斥保护组件
     * @param autoScheduleTaskService 自动排程任务状态服务
     */
    public TcManualOperationFacade(RedissonClient redissonClient,
                                   PlatformTransactionManager transactionManager,
                                   TcScheduleResultMapper scheduleResultMapper,
                                   TcDispatcherLogMapper dispatcherLogMapper,
                                   TcManualInsertRollingService rollingService,
                                   TcManualMachineRuleValidator machineRuleValidator,
                                   TcAutoScheduleExecutionGuard autoScheduleExecutionGuard,
                                   TcAutoScheduleTaskService autoScheduleTaskService) {
        this.redissonClient = redissonClient;
        this.transactionManager = transactionManager;
        this.scheduleResultMapper = scheduleResultMapper;
        this.dispatcherLogMapper = dispatcherLogMapper;
        this.rollingService = rollingService;
        this.machineRuleValidator = machineRuleValidator;
        this.autoScheduleExecutionGuard = autoScheduleExecutionGuard;
        this.autoScheduleTaskService = autoScheduleTaskService;
    }

    /**
     * 执行人工插单。
     *
     * @param insertResult 后端已解析施工信息的插单结果
     * @param reason 操作原因
     * @return 新增结果行数
     */
    public int insertTask(TcScheduleResult insertResult, String reason) {
        return this.executeWithMachineLocks(insertResult.getFactoryCode(), insertResult.getScheduleDate(),
                Collections.singletonList(insertResult.getMachineCode()), () -> this.executeInTransaction(() -> {
                    List<TcScheduleResult> beforeList = this.lockAndLoadSnapshot(insertResult,
                            Collections.singletonList(insertResult.getMachineCode()));
                    this.validateInsertAfterSecondProduction(insertResult, beforeList);
                    for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                        Object planQty = insertResult.getFieldValueByFieldName(String.format(
                                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
                        if (planQty instanceof java.math.BigDecimal
                                && ((java.math.BigDecimal) planQty).compareTo(java.math.BigDecimal.ZERO) > 0) {
                            this.machineRuleValidator.validateTransfer(insertResult, insertResult.getMachineCode(),
                                    shiftOrder);
                        }
                    }
                    int affectedCount = this.rollingService.insertAndRoll(insertResult);
                    List<TcScheduleResult> afterList = this.loadSnapshot(insertResult,
                            Collections.singletonList(insertResult.getMachineCode()));
                    this.recordDispatcherLog("2", insertResult, reason, beforeList, afterList);
                    return affectedCount;
                }));
    }

    /**
     * 执行选中班次调量。
     *
     * @param changeResult 调量请求转换后的结果
     * @param expectedTaskVersion 期望任务版本
     * @param reason 操作原因
     * @return 受影响行数
     */
    public int changeQty(TcScheduleResult changeResult, Long expectedTaskVersion, String reason) {
        TcScheduleResult initial = this.requireResult(changeResult.getId());
        return this.executeWithMachineLocks(initial.getFactoryCode(), initial.getScheduleDate(),
                Collections.singletonList(initial.getMachineCode()), () -> this.executeInTransaction(() -> {
                    List<TcScheduleResult> beforeList = this.lockAndLoadSnapshot(initial,
                            Collections.singletonList(initial.getMachineCode()));
                    TcScheduleResult current = beforeList.stream()
                            .filter(item -> Objects.equals(item.getId(), changeResult.getId()))
                            .findFirst().orElseThrow(() -> new ServiceException(
                                    I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged")));
                    this.validateExpectedVersion(expectedTaskVersion, current);
                    this.copyOperationBaseFields(current, changeResult);
                    int affectedCount = this.rollingService.changeQtyAndRoll(changeResult);
                    List<TcScheduleResult> afterList = this.loadSnapshot(current,
                            Collections.singletonList(current.getMachineCode()));
                    this.recordDispatcherLog("1", changeResult, reason, beforeList, afterList);
                    return affectedCount;
                }));
    }

    /**
     * 自动滚动任务复用人工调量的锁、行锁、横表滚动和审计闭环。
     *
     * @param changeResult 自动调量请求
     * @param expectedTaskVersion 期望任务版本
     * @param reason 自动滚动原因
     * @param rollingTaskId 当前自动滚动任务ID
     * @return 受影响行数
     */
    public int changeQtyForAutoRolling(TcScheduleResult changeResult, Long expectedTaskVersion,
                                       String reason, String rollingTaskId) {
        TcScheduleResult initial = this.requireResult(changeResult.getId());
        return this.executeWithMachineLocks(initial.getFactoryCode(), initial.getScheduleDate(),
                Collections.singletonList(initial.getMachineCode()), rollingTaskId,
                () -> this.executeInTransaction(() -> {
                    List<TcScheduleResult> beforeList = this.lockAndLoadSnapshot(initial,
                            Collections.singletonList(initial.getMachineCode()));
                    TcScheduleResult current = beforeList.stream()
                            .filter(item -> Objects.equals(item.getId(), changeResult.getId()))
                            .findFirst().orElseThrow(() -> new ServiceException(
                                    I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged")));
                    this.validateExpectedVersion(expectedTaskVersion, current);
                    this.copyOperationBaseFields(current, changeResult);
                    int affectedCount = this.rollingService.changeQtyAndRoll(changeResult);
                    List<TcScheduleResult> afterList = this.loadSnapshot(current,
                            Collections.singletonList(current.getMachineCode()));
                    this.recordDispatcherLog("4", changeResult, reason, beforeList, afterList, "AUTO_ROLLING");
                    return affectedCount;
                }));
    }

    /**
     * 自动滚动在同一锁定快照和短事务内批量调量，只执行一次任务链计算和持久化。
     *
     * @param changeResultList 自动调量请求
     * @param expectedVersionList 与请求同顺序的期望版本
     * @param reason 自动滚动原因
     * @param rollingTaskId 当前自动滚动任务 ID
     * @return 受影响结果行数
     */
    public int changeQtyBatchForAutoRolling(List<TcScheduleResult> changeResultList,
                                             List<Long> expectedVersionList,
                                             String reason, String rollingTaskId) {
        if (changeResultList == null || changeResultList.isEmpty()
                || expectedVersionList == null || expectedVersionList.size() != changeResultList.size()) {
            return 0;
        }
        List<TcScheduleResult> initialList = changeResultList.stream()
                .map(item -> this.requireResult(item.getId())).collect(Collectors.toList());
        TcScheduleResult reference = initialList.get(0);
        this.validateSameScheduleRange(reference, initialList);
        List<String> machineCodeList = initialList.stream().map(TcScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        return this.executeWithMachineLocks(reference.getFactoryCode(), reference.getScheduleDate(),
                machineCodeList, rollingTaskId, () -> this.executeInTransaction(() -> {
                    List<TcScheduleResult> beforeList = this.lockAndLoadSnapshot(reference, machineCodeList);
                    for (int index = 0; index < changeResultList.size(); index++) {
                        TcScheduleResult request = changeResultList.get(index);
                        TcScheduleResult current = beforeList.stream()
                                .filter(item -> Objects.equals(item.getId(), request.getId()))
                                .findFirst().orElseThrow(() -> new ServiceException(
                                        I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged")));
                        this.validateExpectedVersion(expectedVersionList.get(index), current);
                        this.copyOperationBaseFields(current, request);
                    }
                    int affectedCount = this.rollingService.changeQtyAndRollBatch(changeResultList);
                    List<TcScheduleResult> afterList = this.loadSnapshot(reference, machineCodeList);
                    this.recordDispatcherLog("4", changeResultList.get(0), reason,
                            beforeList, afterList, "AUTO_ROLLING");
                    return affectedCount;
                }));
    }

    /**
     * 原子执行一组普通转机台操作。
     *
     * @param transferResultList 转机请求结果，每条只包含一个待转班次
     * @param expectedVersionList 与任务列表同顺序的期望版本
     * @param reason 操作原因
     * @return 受影响行数
     */
    public int changeMachine(List<TcScheduleResult> transferResultList, List<Long> expectedVersionList,
                             String reason) {
        if (transferResultList == null || transferResultList.isEmpty()
                || expectedVersionList == null || expectedVersionList.size() != transferResultList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.invalidRequest"));
        }
        List<TcScheduleResult> initialList = transferResultList.stream()
                .map(item -> this.requireResult(item.getId())).collect(Collectors.toList());
        TcScheduleResult reference = initialList.get(0);
        this.validateSameScheduleRange(reference, initialList);
        List<String> machineCodeList = new ArrayList<>();
        machineCodeList.addAll(initialList.stream().map(TcScheduleResult::getMachineCode).collect(Collectors.toList()));
        machineCodeList.addAll(transferResultList.stream().map(TcScheduleResult::getMachineCode)
                .collect(Collectors.toList()));
        return this.executeWithMachineLocks(reference.getFactoryCode(), reference.getScheduleDate(), machineCodeList,
                () -> this.executeInTransaction(() -> {
                    List<TcScheduleResult> beforeList = this.lockAndLoadSnapshot(reference, machineCodeList);
                    List<Long> resultIdList = initialList.stream().map(TcScheduleResult::getId).distinct().sorted()
                            .collect(Collectors.toList());
                    List<TcScheduleResult> lockedList = beforeList.stream()
                            .filter(item -> resultIdList.contains(item.getId())).collect(Collectors.toList());
                    if (lockedList == null || lockedList.size() != resultIdList.size()) {
                        throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
                    }
                    for (int index = 0; index < transferResultList.size(); index++) {
                        TcScheduleResult transferResult = transferResultList.get(index);
                        TcScheduleResult current = lockedList.stream()
                                .filter(item -> Objects.equals(item.getId(), transferResult.getId()))
                                .findFirst().orElseThrow(() -> new ServiceException(
                                        I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged")));
                        this.validateExpectedVersion(expectedVersionList.get(index), current);
                        this.copyOperationBaseFields(current, transferResult);
                        int shiftOrder = TcInsertPositionValidator.resolveShiftOrder(transferResult);
                        this.machineRuleValidator.validateTransfer(current, transferResult.getMachineCode(), shiftOrder);
                    }
                    int affectedCount = this.rollingService.changeMachineAndRollBatch(transferResultList);
                    List<TcScheduleResult> afterList = this.loadSnapshot(reference, machineCodeList);
                    this.recordDispatcherLog("0", transferResultList.get(0), reason,
                            beforeList, afterList);
                    return affectedCount;
                }));
    }

    /**
     * 按结果 ID 整行删除横向排程记录。
     *
     * @param resultIdList 结果 ID
     * @param reason 删除原因
     * @return 删除行数
     */
    public int remove(List<Long> resultIdList, String reason) {
        if (resultIdList == null || resultIdList.isEmpty()) {
            return 0;
        }
        List<TcScheduleResult> initialList = resultIdList.stream().distinct().sorted()
                .map(this::requireResult).collect(Collectors.toList());
        TcScheduleResult reference = initialList.get(0);
        this.validateSameScheduleRange(reference, initialList);
        List<String> machineCodeList = initialList.stream().map(TcScheduleResult::getMachineCode).distinct()
                .collect(Collectors.toList());
        return this.executeWithMachineLocks(reference.getFactoryCode(), reference.getScheduleDate(), machineCodeList,
                () -> this.executeInTransaction(() -> {
                    List<TcScheduleResult> beforeList = this.lockAndLoadSnapshot(reference, machineCodeList);
                    Set<Long> initialIdSet = initialList.stream().map(TcScheduleResult::getId)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    List<TcScheduleResult> lockedList = beforeList.stream()
                            .filter(item -> initialIdSet.contains(item.getId())).collect(Collectors.toList());
                    if (lockedList == null || lockedList.size() != initialList.size()) {
                        throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
                    }
                    this.validateDeleteReleaseStatus(lockedList);
                    int deletedCount = this.rollingService.deleteAndRollBatch(lockedList);
                    List<TcScheduleResult> afterList = this.loadSnapshot(reference, machineCodeList);
                    this.recordDispatcherLog("3", lockedList.get(0), reason, beforeList, afterList);
                    return deletedCount;
                }));
    }

    /**
     * 构造多机台联锁键，规范化、去重并稳定排序。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param machineCodes 机台编码
     * @return 排序后的锁键
     */
    List<String> buildMachineLockKeys(String factoryCode, Date scheduleDate, List<String> machineCodes) {
        String normalizedFactoryCode = StrUtil.trim(factoryCode);
        if (StrUtil.isBlank(normalizedFactoryCode) || scheduleDate == null || machineCodes == null) {
            return Collections.emptyList();
        }
        return machineCodes.stream().filter(StrUtil::isNotBlank).map(StrUtil::trim).distinct().sorted()
                .map(machineCode -> TcScheduleConstants.MANUAL_OPERATION_LOCK_KEY_PREFIX + normalizedFactoryCode
                        + ":" + DateUtil.formatDate(scheduleDate) + ":" + machineCode)
                .collect(Collectors.toList());
    }

    /**
     * 校验期望任务版本与数据库当前版本一致。
     *
     * @param expectedTaskVersion 期望版本
     * @param currentResult 数据库当前结果
     * @throws ServiceException 版本为空或不一致时抛出
     */
    void validateExpectedVersion(Long expectedTaskVersion, TcScheduleResult currentResult) {
        Long currentVersion = currentResult == null || currentResult.getTaskVersion() == null
                ? 0L : currentResult.getTaskVersion();
        if (expectedTaskVersion == null || !expectedTaskVersion.equals(currentVersion)) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
        }
    }

    /**
     * 校验整行删除发布状态。
     *
     * @param resultList 待删除结果
     * @throws ServiceException 存在不允许删除的发布状态时抛出
     */
    void validateDeleteReleaseStatus(List<TcScheduleResult> resultList) {
        Set<String> allowedStatusSet = new LinkedHashSet<>();
        allowedStatusSet.add("0");
        allowedStatusSet.add("2");
        allowedStatusSet.add("5");
        if (resultList == null || resultList.isEmpty()
                || resultList.stream().anyMatch(item -> !allowedStatusSet.contains(item.getReleaseStatus()))) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.remove.releaseBlocked"));
        }
    }

    /**
     * 在短事务内执行业务操作，确保审计与业务写入共同提交或回滚。
     *
     * @param action 事务动作
     * @param <T> 返回类型
     * @return 动作结果
     * @throws RuntimeException 业务或审计异常原样抛出
     */
    <T> T executeInTransaction(Supplier<T> action) {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            T result = action.get();
            this.transactionManager.commit(status);
            return result;
        } catch (RuntimeException exception) {
            this.transactionManager.rollback(status);
            throw exception;
        }
    }

    /**
     * 按稳定顺序获取全部机台锁并执行操作。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param machineCodes 机台编码
     * @param action 获锁后动作
     * @param <T> 返回类型
     * @return 动作结果
     */
    private <T> T executeWithMachineLocks(String factoryCode, Date scheduleDate, List<String> machineCodes,
                                          Supplier<T> action) {
        return this.executeWithMachineLocks(factoryCode, scheduleDate, machineCodes, null, action);
    }

    /**
     * 按稳定顺序获取机台锁，并允许当前后台任务忽略自身活跃记录。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param machineCodes 机台编码
     * @param ignoredTaskId 允许忽略的当前任务ID
     * @param action 获锁后动作
     * @param <T> 返回类型
     * @return 动作结果
     */
    private <T> T executeWithMachineLocks(String factoryCode, Date scheduleDate, List<String> machineCodes,
                                          String ignoredTaskId, Supplier<T> action) {
        List<String> lockKeyList = this.buildMachineLockKeys(factoryCode, scheduleDate, machineCodes);
        if (lockKeyList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.machineRequired"));
        }
        String autoScheduleLockToken = this.autoScheduleExecutionGuard.acquire(factoryCode, scheduleDate);
        List<RLock> acquiredLockList = new ArrayList<>();
        try {
            TcAutoScheduleTask activeTask = this.autoScheduleTaskService.findActive(factoryCode, scheduleDate);
            if (activeTask != null && !Objects.equals(activeTask.getTaskId(), ignoredTaskId)) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.concurrentTask"));
            }
            for (String lockKey : lockKeyList) {
                RLock lock = this.redissonClient.getLock(lockKey);
                if (!lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) {
                    throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.operating"));
                }
                acquiredLockList.add(lock);
            }
            return action.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.operating"));
        } finally {
            for (int index = acquiredLockList.size() - 1; index >= 0; index--) {
                RLock lock = acquiredLockList.get(index);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            this.autoScheduleExecutionGuard.release(factoryCode, scheduleDate, autoScheduleLockToken);
        }
    }

    /**
     * 加锁并加载人工操作范围快照。
     *
     * @param reference 排程范围参考
     * @param machineCodeList 机台编码
     * @return 操作前快照
     */
    private List<TcScheduleResult> lockAndLoadSnapshot(TcScheduleResult reference, List<String> machineCodeList) {
        List<TcScheduleResult> snapshot = this.loadSnapshot(reference, machineCodeList);
        List<Long> resultIdList = snapshot.stream().map(TcScheduleResult::getId).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList());
        if (!resultIdList.isEmpty()) {
            LambdaQueryWrapper<TcScheduleResult> lockWrapper = new LambdaQueryWrapper<>();
            lockWrapper.in(TcScheduleResult::getId, resultIdList);
            lockWrapper.orderByAsc(TcScheduleResult::getId);
            lockWrapper.last("FOR UPDATE");
            List<TcScheduleResult> lockedList = this.scheduleResultMapper.selectList(lockWrapper);
            if (lockedList == null || lockedList.size() != resultIdList.size()) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
            }
        }
        return this.loadSnapshot(reference, machineCodeList);
    }

    /**
     * 加载同工厂、日期、批次和指定机台的当前排程快照。
     *
     * @param reference 排程范围参考
     * @param machineCodeList 机台编码
     * @return 排程快照
     */
    private List<TcScheduleResult> loadSnapshot(TcScheduleResult reference, List<String> machineCodeList) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, reference.getScheduleDate());
        wrapper.eq(StrUtil.isNotBlank(reference.getBatchNo()), TcScheduleResult::getBatchNo, reference.getBatchNo());
        wrapper.in(TcScheduleResult::getMachineCode, machineCodeList.stream().filter(StrUtil::isNotBlank)
                .map(StrUtil::trim).distinct().collect(Collectors.toList()));
        wrapper.orderByAsc(TcScheduleResult::getMachineCode, TcScheduleResult::getId);
        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(wrapper);
        return resultList == null ? Collections.emptyList() : resultList;
    }

    /**
     * 校验插单位置在第二个在产规格之后。
     *
     * @param insertResult 插单结果
     * @param snapshot 当前机台快照
     */
    void validateInsertAfterSecondProduction(TcScheduleResult insertResult,
                                             List<TcScheduleResult> snapshot) {
        boolean hasInsertShift = false;
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            Object planQty = insertResult.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
            if (!(planQty instanceof java.math.BigDecimal)
                    || ((java.math.BigDecimal) planQty).compareTo(java.math.BigDecimal.ZERO) <= 0) {
                continue;
            }
            hasInsertShift = true;
            Integer insertSequence = TcInsertPositionValidator.resolveSequence(insertResult, shiftOrder);
            if (insertSequence == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftRequired"));
            }
            final int currentShiftOrder = shiftOrder;
            List<Integer> productionSequenceList = snapshot.stream()
                    .filter(item -> TcInsertPositionValidator.getFinishQty(item, currentShiftOrder).signum() > 0)
                    .map(item -> TcInsertPositionValidator.resolveSequence(item, currentShiftOrder))
                    .filter(Objects::nonNull).sorted().collect(Collectors.toList());
            if (productionSequenceList.size() >= 2 && insertSequence <= productionSequenceList.get(1)) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.positionInvalid"));
            }
        }
        if (!hasInsertShift) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftRequired"));
        }
    }

    /**
     * 记录人工操作前后快照。
     *
     * @param operationType 操作类型
     * @param operationResult 操作请求
     * @param reason 操作原因
     * @param beforeList 操作前快照
     * @param afterList 操作后快照
     */
    private void recordDispatcherLog(String operationType, TcScheduleResult operationResult, String reason,
                                     List<TcScheduleResult> beforeList, List<TcScheduleResult> afterList) {
        String asynchronousOperator = TcOperationAuditContext.getOperator();
        this.recordDispatcherLog(operationType, operationResult, reason, beforeList, afterList,
                StrUtil.isNotBlank(asynchronousOperator) ? asynchronousOperator : SecurityUtils.getUsername());
    }

    /**
     * 记录指定审计人的操作前后快照。
     *
     * @param operationType 操作类型
     * @param operationResult 操作请求
     * @param reason 操作原因
     * @param beforeList 操作前快照
     * @param afterList 操作后快照
     * @param createBy 审计人
     */
    private void recordDispatcherLog(String operationType, TcScheduleResult operationResult, String reason,
                                     List<TcScheduleResult> beforeList, List<TcScheduleResult> afterList,
                                     String createBy) {
        TcDispatcherLog dispatcherLog = new TcDispatcherLog();
        dispatcherLog.setFactoryCode(operationResult.getFactoryCode());
        dispatcherLog.setBatchNo(operationResult.getBatchNo());
        dispatcherLog.setScheduleId(operationResult.getId());
        dispatcherLog.setScheduleDate(operationResult.getScheduleDate());
        dispatcherLog.setSidewallCode(operationResult.getSidewallCode());
        dispatcherLog.setOperType(operationType);
        dispatcherLog.setShiftOrder(TcInsertPositionValidator.resolveShiftOrder(operationResult));
        dispatcherLog.setReason(reason);
        dispatcherLog.setAffectedBeforeJson(this.buildSnapshotJson(beforeList));
        dispatcherLog.setAffectedAfterJson(this.buildSnapshotJson(afterList));
        this.fillCompatibilitySnapshot(dispatcherLog, operationResult.getId(), beforeList, true);
        this.fillCompatibilitySnapshot(dispatcherLog, operationResult.getId(), afterList, false);
        dispatcherLog.setCreateBy(createBy);
        if (this.dispatcherLogMapper.insert(dispatcherLog) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
        }
    }

    /**
     * 构造带版本号的人工操作快照 JSON。
     *
     * @param resultList 排程结果快照
     * @return schemaVersion=1 的快照 JSON
     */
    private String buildSnapshotJson(List<TcScheduleResult> resultList) {
        Map<String, Object> snapshotMap = new LinkedHashMap<>();
        snapshotMap.put("schemaVersion", 1);
        snapshotMap.put("results", resultList);
        return JSON.toJSONString(snapshotMap);
    }

    /**
     * 回填调度日志兼容机台和六班计划量字段。
     *
     * @param dispatcherLog 调度日志
     * @param primaryResultId 主操作结果 ID
     * @param snapshotList 快照列表
     * @param beforeSnapshot true 表示操作前快照
     */
    private void fillCompatibilitySnapshot(TcDispatcherLog dispatcherLog, Long primaryResultId,
                                           List<TcScheduleResult> snapshotList, boolean beforeSnapshot) {
        TcScheduleResult primaryResult = snapshotList.stream()
                .filter(item -> Objects.equals(item.getId(), primaryResultId)).findFirst()
                .orElse(snapshotList.isEmpty() ? null : snapshotList.get(0));
        if (primaryResult == null) {
            return;
        }
        if (beforeSnapshot) {
            dispatcherLog.setBeforeMachineCode(primaryResult.getMachineCode());
        } else {
            dispatcherLog.setAfterMachineCode(primaryResult.getMachineCode());
        }
        String fieldPrefix = beforeSnapshot ? "beforeClass%dPlanQty" : "afterClass%dPlanQty";
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            Object planQty = primaryResult.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
            dispatcherLog.setFieldValueByFieldName(String.format(fieldPrefix, shiftOrder), planQty);
        }
    }

    /**
     * 按主键读取排程结果。
     *
     * @param resultId 结果 ID
     * @return 排程结果
     */
    private TcScheduleResult requireResult(Long resultId) {
        TcScheduleResult result = resultId == null ? null : this.scheduleResultMapper.selectById(resultId);
        if (result == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
        }
        return result;
    }

    /**
     * 校验批量操作仅覆盖同一工厂、日期和批次。
     *
     * @param reference 范围参考
     * @param resultList 待操作结果
     */
    private void validateSameScheduleRange(TcScheduleResult reference, List<TcScheduleResult> resultList) {
        boolean invalid = resultList.stream().anyMatch(item -> !Objects.equals(reference.getFactoryCode(),
                item.getFactoryCode()) || !Objects.equals(reference.getScheduleDate(), item.getScheduleDate())
                || !Objects.equals(reference.getBatchNo(), item.getBatchNo()));
        if (invalid) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.batchRangeInvalid"));
        }
    }

    /**
     * 从数据库当前结果补齐操作请求的审计和规则校验基础字段。
     *
     * @param source 数据库当前结果
     * @param target 操作请求结果
     */
    private void copyOperationBaseFields(TcScheduleResult source, TcScheduleResult target) {
        target.setFactoryCode(source.getFactoryCode());
        target.setBatchNo(source.getBatchNo());
        target.setScheduleDate(source.getScheduleDate());
        target.setSidewallCode(source.getSidewallCode());
        target.setConstructionVersion(source.getConstructionVersion());
        target.setSidewallCraft(source.getSidewallCraft());
        target.setGlueCode(source.getGlueCode());
        target.setBaseGlueCode(source.getBaseGlueCode());
        target.setWholeGlueCode(source.getWholeGlueCode());
        target.setMouthPlateCode(source.getMouthPlateCode());
    }
}
