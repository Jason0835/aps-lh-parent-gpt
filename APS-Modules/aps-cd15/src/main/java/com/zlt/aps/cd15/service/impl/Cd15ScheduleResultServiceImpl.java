package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.algorithm.Cd15ShiftWindowResolver;
import com.zlt.aps.cd15.engine.constant.Cd15CutMode;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineShiftConfigMapper;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15BatchDataCheckResult;
import com.zlt.aps.cd15.engine.model.Cd15InsertCarryoverImpact;
import com.zlt.aps.cd15.engine.model.Cd15InsertRollingOutput;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleBatchDataValidator;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15InsertRollingService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.model.Cd15ScheduleOverwriteDecision;
import com.zlt.aps.cd15.service.Cd15AutoScheduleAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15InsertOrderAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15ScheduleOverwriteValidator;
import com.zlt.aps.cd15.service.Cd15TimedRollingCheckService;
import com.zlt.aps.cd15.service.ICd15ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 斜裁排程结果业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15ScheduleResultServiceImpl extends AbstractDocService<Cd15ScheduleResult> implements ICd15ScheduleResultService {

    private static final int CLASS_COUNT = 8;

    @Resource
    private Cd15ScheduleTaskService taskService;

    @Resource
    private Cd15ScheduleResultMapper resultMapper;

    @Resource
    private Cd15EngineShiftConfigMapper shiftConfigMapper;

    @Resource
    private Cd15ShiftWindowResolver shiftWindowResolver;

    @Resource
    private Cd15AutoScheduleAsyncExecutor autoScheduleAsyncExecutor;

    @Resource
    private Cd15AutoScheduleBatchDataValidator batchDataValidator;

    @Resource
    private Cd15AutoScheduleLockService lockService;

    @Resource
    private Cd15InsertRollingService insertRollingService;

    @Resource
    private Cd15ScheduleOverwriteValidator overwriteValidator;

    @Resource
    private Cd15InsertOrderAsyncExecutor insertOrderAsyncExecutor;

    @Resource
    private Cd15TimedRollingCheckService timedRollingCheckService;

    /**
     * 删除排程结果，不触发滚动重排；删除后只压缩同工厂、日期、机台的 CLASS1 后续生产顺位。
     *
     * @param ids 待删除排程结果主键
     * @return 删除结果
     */
    @Override
    public AjaxResult removeScheduleResults(List<Long> ids) {
        // 先过滤空主键并去重，避免重复 ID 影响删除数量校验。
        List<Long> deleteIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (deleteIds.isEmpty()) {
            return this.required("ids");
        }
        // 首次查询用于确认所有待删除记录均存在，并确定需要加锁的工厂和排程日期范围。
        List<Cd15ScheduleResult> selected = this.selectDeleteResults(deleteIds);
        if (selected.size() != deleteIds.size()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.message.parameter.error"));
        }
        // 同一请求允许包含多个排程范围；按固定顺序获取锁，避免并发批量删除产生交叉等待。
        List<Cd15ScheduleResult> scopeSamples = new ArrayList<>(selected.stream()
                .collect(Collectors.toMap(this::scheduleScopeKey,
                        result -> result, (first, second) -> first,
                        LinkedHashMap::new)).values());
        scopeSamples.sort(Comparator
                .comparing(Cd15ScheduleResult::getFactoryCode,
                        Comparator.nullsFirst(String::compareTo))
                .thenComparing(Cd15ScheduleResult::getScheduleDate,
                        Comparator.nullsFirst(Date::compareTo)));

        // 删除与自动排程共用“工厂 + 排程日期”锁，确保排程写入与删除不能并发执行。
        List<RLock> acquiredLocks = new ArrayList<>();
        boolean releaseAfterTransaction = false;
        try {
            for (Cd15ScheduleResult scope : scopeSamples) {
                if (this.isBlank(scope.getFactoryCode())
                        || scope.getScheduleDate() == null) {
                    return AjaxResult.error(I18nUtil.getMessage(
                            "ui.message.parameter.error"));
                }
                RLock lock = lockService.getLock(scope.getFactoryCode(),
                        this.toLocalDate(scope.getScheduleDate()));
                if (!lock.tryLock()) {
                    return AjaxResult.error(I18nUtil.getMessage(
                            "ui.cd15.schedule.taskActive"));
                }
                acquiredLocks.add(lock);
            }
            // 锁必须覆盖整个数据库事务，事务提交或回滚后再统一释放。
            releaseAfterTransaction = this.releaseLocksAfterTransaction(
                    acquiredLocks);

            // 获取锁后重新查询，防止加锁前后记录状态发生变化。
            selected = this.selectDeleteResults(deleteIds);
            if (selected.size() != deleteIds.size()) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.message.parameter.error"));
            }
            // 锁内再次确认没有待执行或执行中的排程任务，避免删除任务即将使用的数据。
            for (Cd15ScheduleResult scope : scopeSamples) {
                if (taskService.findActive(scope.getFactoryCode(),
                        scope.getScheduleDate()) != null) {
                    return AjaxResult.error(I18nUtil.getMessage(
                            "ui.cd15.schedule.taskActive"));
                }
            }
            // 统一校验发布成功、已有完成量以及分裁组合必须完整删除等业务规则。
            AjaxResult validation = this.validateDeleteResults(selected);
            if (validation != null) {
                return validation;
            }
            // 主表采用框架逻辑删除；删除数量不一致时抛错，使当前事务整体回滚。
            int deletedCount = this.removeByIds(deleteIds);
            if (deletedCount != deleteIds.size()) {
                throw new IllegalStateException(I18nUtil.getMessage(
                        "ui.message.operation.failed"));
            }
            // 删除成功后只压缩同机台 CLASS1 后续生产顺位，不修改其他班次，也不触发滚动重排。
            this.compactClass1ProduceOrders(selected);
            return AjaxResult.success(I18nUtil.getMessage(
                    "ui.message.operation.success"));
        } finally {
            if (!releaseAfterTransaction) {
                // 尚未注册事务回调时由当前线程兜底释放已获得的锁。
                this.unlockDeleteLocks(acquiredLocks);
            }
        }
    }

    /** 查询待删除且尚未逻辑删除的结果。 */
    private List<Cd15ScheduleResult> selectDeleteResults(List<Long> ids) {
        return resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .in(Cd15ScheduleResult::getId, ids));
    }

    /** 校验发布、完成量和分裁组合删除规则。 */
    private AjaxResult validateDeleteResults(List<Cd15ScheduleResult> selected) {
        boolean published = selected.stream().anyMatch(result ->
                result.getPublishSuccessCount() != null
                        && result.getPublishSuccessCount() > 0);
        if (published) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.scheduleResult.publishedCannotDelete"));
        }
        if (selected.stream().anyMatch(this::hasFinishQuantity)) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.scheduleResult.finishQtyCannotDelete"));
        }
        boolean missingGroupNo = selected.stream()
                .filter(result -> Cd15CutMode.SPLIT.equalsIgnoreCase(
                        result.getCutMode()))
                .anyMatch(result -> this.isBlank(result.getGroupNo()));
        if (missingGroupNo) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.scheduleResult.splitDeleteTogether"));
        }
        Map<String, List<Cd15ScheduleResult>> splitGroups = selected.stream()
                .filter(result -> Cd15CutMode.SPLIT.equalsIgnoreCase(
                        result.getCutMode()))
                .collect(Collectors.groupingBy(this::splitGroupKey,
                        LinkedHashMap::new, Collectors.toList()));
        for (List<Cd15ScheduleResult> selectedGroup : splitGroups.values()) {
            Cd15ScheduleResult sample = selectedGroup.get(0);
            List<Cd15ScheduleResult> completeGroup = resultMapper.selectList(
                    new LambdaQueryWrapper<Cd15ScheduleResult>()
                            .eq(Cd15ScheduleResult::getFactoryCode,
                                    sample.getFactoryCode())
                            .eq(Cd15ScheduleResult::getScheduleDate,
                                    sample.getScheduleDate())
                            .eq(Cd15ScheduleResult::getGroupNo,
                                    sample.getGroupNo())
                            .eq(Cd15ScheduleResult::getCutMode,
                                    Cd15CutMode.SPLIT));
            Set<Long> selectedIds = selectedGroup.stream()
                    .map(Cd15ScheduleResult::getId)
                    .collect(Collectors.toSet());
            boolean selectedAll = completeGroup.size() == selectedGroup.size()
                    && completeGroup.stream()
                    .map(Cd15ScheduleResult::getId)
                    .allMatch(selectedIds::contains)
                    && (completeGroup.size() == 1
                    || completeGroup.size() == 2
                    && completeGroup.stream()
                    .map(Cd15ScheduleResult::getSteelStripCode)
                    .filter(code -> !this.isBlank(code))
                    .distinct().count() == 2L);
            if (!selectedAll) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.cd15.scheduleResult.splitDeleteTogether"));
            }
        }
        return null;
    }

    /** 任一班次已有正完成量时禁止删除。 */
    private boolean hasFinishQuantity(Cd15ScheduleResult result) {
        return IntStream.rangeClosed(1, CLASS_COUNT)
                .mapToObj(classIndex -> result.getFieldValueByFieldName(
                        String.format("class%dFinishQty", classIndex)))
                .filter(Objects::nonNull)
                .map(Number.class::cast)
                .anyMatch(finishQuantity -> finishQuantity.doubleValue() > 0D);
    }

    /** 删除后仅压缩 CLASS1 后续生产顺位，其他班次不调整。 */
    private void compactClass1ProduceOrders(
            List<Cd15ScheduleResult> deletedResults) {
        Map<String, List<Cd15ScheduleResult>> deletedByScope = deletedResults
                .stream()
                .filter(result -> result.getClass1ProduceOrder() != null
                        && result.getClass1ProduceOrder() > 0)
                .collect(Collectors.groupingBy(this::class1OrderScopeKey,
                        LinkedHashMap::new, Collectors.toList()));
        deletedByScope.values().forEach(this::compactClass1ScopeOrders);
    }

    /** 压缩单个工厂、日期、机台范围内被删除顺位之后的 CLASS1 顺位。 */
    private void compactClass1ScopeOrders(
            List<Cd15ScheduleResult> deletedScopeResults) {
        Cd15ScheduleResult sample = deletedScopeResults.get(0);
        LambdaQueryWrapper<Cd15ScheduleResult> queryWrapper =
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .eq(Cd15ScheduleResult::getFactoryCode,
                                sample.getFactoryCode())
                        .eq(Cd15ScheduleResult::getScheduleDate,
                                sample.getScheduleDate())
                        .isNotNull(Cd15ScheduleResult::getClass1ProduceOrder)
                        .gt(Cd15ScheduleResult::getClass1ProduceOrder, 0)
                        .orderByAsc(Cd15ScheduleResult::getClass1ProduceOrder)
                        .orderByAsc(Cd15ScheduleResult::getId);
        if (sample.getMachineCode() == null) {
            queryWrapper.isNull(Cd15ScheduleResult::getMachineCode);
        } else {
            queryWrapper.eq(Cd15ScheduleResult::getMachineCode,
                    sample.getMachineCode());
        }
        List<Cd15ScheduleResult> remaining = resultMapper.selectList(
                queryWrapper);
        Set<Integer> removedOrders = deletedScopeResults.stream()
                .map(Cd15ScheduleResult::getClass1ProduceOrder)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> remainingOrders = remaining.stream()
                .map(Cd15ScheduleResult::getClass1ProduceOrder)
                .collect(Collectors.toSet());
        removedOrders.removeAll(remainingOrders);
        if (removedOrders.isEmpty()) {
            return;
        }
        remaining.forEach(result -> {
            Integer currentOrder = result.getClass1ProduceOrder();
            long removedBefore = removedOrders.stream()
                    .filter(removedOrder -> removedOrder < currentOrder)
                    .count();
            if (removedBefore <= 0) {
                return;
            }
            int targetOrder = currentOrder - (int) removedBefore;
            resultMapper.update(null,
                    new LambdaUpdateWrapper<Cd15ScheduleResult>()
                            .set(Cd15ScheduleResult::getClass1ProduceOrder,
                                    targetOrder)
                            .eq(Cd15ScheduleResult::getId,
                                    result.getId()));
        });
    }

    /** 在当前事务结束后释放删除持有的排程锁。 */
    private boolean releaseLocksAfterTransaction(List<RLock> acquiredLocks) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        List<RLock> locksToRelease = new ArrayList<>(acquiredLocks);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        Cd15ScheduleResultServiceImpl.this.unlockDeleteLocks(
                                locksToRelease);
                    }
                });
        return true;
    }

    /** 按获取逆序释放删除排程锁。 */
    private void unlockDeleteLocks(List<RLock> acquiredLocks) {
        for (int index = acquiredLocks.size() - 1; index >= 0; index--) {
            RLock lock = acquiredLocks.get(index);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 构造工厂和排程日期维度的锁排序键。 */
    private String scheduleScopeKey(Cd15ScheduleResult result) {
        return String.valueOf(result.getFactoryCode()) + "|"
                + String.valueOf(result.getScheduleDate());
    }

    /** 构造 CLASS1 顺位压缩范围键。 */
    private String class1OrderScopeKey(Cd15ScheduleResult result) {
        return this.scheduleScopeKey(result) + "|"
                + String.valueOf(result.getMachineCode());
    }

    /** 构造分裁组合删除校验键。 */
    private String splitGroupKey(Cd15ScheduleResult result) {
        return this.scheduleScopeKey(result) + "|"
                + String.valueOf(result.getGroupNo());
    }

    /**
     * 斜裁自动排程 Service 入口。
     * 当前阶段先创建真实异步任务记录，算法执行链路后续补齐。
     *
     * @param scheduleResult 自动排程条件
     * @return 任务入口结构
     */
    @Override
    public AjaxResult autoSchedule(Cd15ScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return this.required("scheduleResult");
        }
        if (this.isBlank(scheduleResult.getFactoryCode())) {
            return this.required("factoryCode");
        }
        if (scheduleResult.getScheduleDate() == null) {
            return this.required("scheduleDate");
        }
        LocalDate localScheduleDate = this.toLocalDate(scheduleResult.getScheduleDate());
        Cd15BatchDataCheckResult batchCheck = batchDataValidator.check(
                scheduleResult.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("needConfirm", false);
            data.put("batchCheckFailed", true);
            data.put("errors", this.toErrorList(batchCheck.getErrors()));
            data.put("warnings", this.toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        List<Cd15ScheduleResult> existing = resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, scheduleResult.getFactoryCode())
                .eq(Cd15ScheduleResult::getScheduleDate, scheduleResult.getScheduleDate()));
        Cd15ScheduleOverwriteDecision overwriteDecision = overwriteValidator.validate(
                existing, Boolean.TRUE.equals(scheduleResult.getForceRegenerate()));
        if (overwriteDecision.isRejected()) {
            return AjaxResult.error(overwriteDecision.getMessage());
        }
        if (overwriteDecision.isNeedConfirm()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("needConfirm", true);
            data.put("existingCount", existing.size());
            return AjaxResult.success(overwriteDecision.getMessage(), data);
        }
        Cd15ScheduleTask activeTask = taskService.findActive(
                scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.success(I18nUtil.getMessage(
                    "ui.cd15.schedule.taskActive"), this.toTaskData(activeTask));
        }
        String snapshot = "factoryCode=" + scheduleResult.getFactoryCode()
                + ",scheduleDate=" + scheduleResult.getScheduleDate()
                + ",forceRegenerate=" + Boolean.TRUE.equals(scheduleResult.getForceRegenerate());
        Cd15ScheduleTask task = taskService.createPending(
                scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate(),
                Cd15ScheduleTaskType.AUTO_SCHEDULE, "MANUAL", snapshot, null);
        autoScheduleAsyncExecutor.execute(task.getTaskId(), task.getFactoryCode(), task.getScheduleDate());
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), this.toTaskData(task));
    }

    @Override
    public AjaxResult getAutoScheduleTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.AUTO_SCHEDULE);
    }

    /**
     * 查询排程日期对应的启用班次窗口。
     *
     * @param request 查询条件，使用工厂编码和排程日期
     * @return 班次日期、时间窗口、当前班次和可编辑状态
     */
    @Override
    public AjaxResult shiftDates(Cd15InsertOrderRequest request) {
        if (request == null) {
            return this.required("request");
        }
        if (this.isBlank(request.getFactoryCode())) {
            return this.required("factoryCode");
        }
        if (request.getScheduleDate() == null) {
            return this.required("scheduleDate");
        }
        LocalDate scheduleDate = this.toLocalDate(request.getScheduleDate());
        LocalDateTime now = LocalDateTime.now();
        List<Cd15ShiftDescriptor> shifts = shiftWindowResolver.resolve(scheduleDate,
                shiftConfigMapper.selectList(Wrappers.<Cd15ShiftConfig>lambdaQuery()
                        .eq(Cd15ShiftConfig::getFactoryCode, request.getFactoryCode())));
        List<Map<String, Object>> values = shifts.stream()
                .map(shift -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("classField", shift.getClassField());
                    item.put("shiftCode", shift.getShiftCode());
                    item.put("shiftName", shift.getShiftDisplayName());
                    item.put("shiftDate", shift.getScheduleDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    item.put("startTime", shift.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    item.put("endTime", shift.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    item.put("currentShift", !now.isBefore(shift.getStartTime()) && now.isBefore(shift.getEndTime()));
                    item.put("changeQtyEditable", now.isBefore(shift.getEndTime()));
                    return item;
                })
                .collect(Collectors.toList());
        return AjaxResult.success(values);
    }
    @Override
    public AjaxResult validateInsert(Cd15InsertOrderRequest request) {
        AjaxResult requiredResult = this.validateInsertRequired(request);
        if (requiredResult != null) {
            return requiredResult;
        }
        AjaxResult planValidation = this.validatePlanAndProduceOrder(request);
        if (!this.isSuccess(planValidation)) {
            return planValidation;
        }
        return this.validateEditableClasses(request.getFactoryCode(),
                request.getScheduleDate(), this.insertClassIndexes(request),
                "ui.cd15.adjust.insertShiftEnded");
    }

    /**
     * 斜裁插单 Service 入口。
     * 当前阶段先创建真实异步任务记录，滚动重排链路后续补齐。
     *
     * @param request 插单请求
     * @return 任务入口结构
     */
    @Override
    public AjaxResult insert(Cd15InsertOrderRequest request) {
        AjaxResult validation = this.validateInsert(request);
        if (!Objects.equals(200, validation.get("code"))) {
            return validation;
        }
        AjaxResult batchValidation = this.validateBatchData(request.getFactoryCode(), request.getScheduleDate());
        if (batchValidation != null) {
            return batchValidation;
        }
        Cd15ScheduleTask activeTask = taskService.findActive(request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.schedule.taskActive"));
        }
        Cd15ScheduleTask task = taskService.createPending(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.INSERT_ORDER, "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.execute(task.getTaskId(), request);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), this.toTaskData(task));
    }
    @Override
    public AjaxResult getInsertTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.INSERT_ORDER);
    }

    @Override
    public AjaxResult validateTransferMachine(Cd15TransferMachineRequest request) {
        if (request == null) {
            return this.required("request");
        }
        if (this.isBlank(request.getFactoryCode())) {
            return this.required("factoryCode");
        }
        if (request.getScheduleDate() == null) {
            return this.required("scheduleDate");
        }
        if (request.getScheduleResultId() == null) {
            return this.required("scheduleResultId");
        }
        if (this.isBlank(request.getSourceMachineCode())) {
            return this.required("sourceMachineCode");
        }
        if (this.isBlank(request.getTargetMachineCode())) {
            return this.required("targetMachineCode");
        }
        if (this.isBlank(request.getSteelStripCode())) {
            return this.required("steelStripCode");
        }
        if (request.getSourceMachineCode().equals(request.getTargetMachineCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.transfer.sameMachine"));
        }
        Cd15ScheduleResult source = this.findSelectedResult(
                request.getScheduleResultId(), request.getFactoryCode(),
                request.getScheduleDate()).orElse(null);
        if (source == null
                || !Objects.equals(source.getMachineCode(), request.getSourceMachineCode())
                || !Objects.equals(source.getSteelStripCode(), request.getSteelStripCode())
                || (!this.isBlank(request.getGroupNo())
                && !Objects.equals(source.getGroupNo(), request.getGroupNo()))) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.adjust.resultMismatch"));
        }
        List<Cd15ScheduleResult> transferSources = this.resolveTransferSources(
                source, request.getFactoryCode(), request.getScheduleDate());
        if (transferSources.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.adjust.resultMismatch"));
        }
        List<Integer> editableClassIndexes = this.resolveTransferEditableClassIndexes(
                request.getFactoryCode(), request.getScheduleDate());
        int editableFromClassIndex = this.resolveTransferEditableFromClassIndex(
                editableClassIndexes);
        if (editableFromClassIndex > CLASS_COUNT) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.transfer.windowEnded"));
        }
        request.setStartClassField("CLASS" + editableFromClassIndex);
        boolean invalidClassOrder = IntStream.rangeClosed(1, CLASS_COUNT)
                .anyMatch(classIndex -> this.readTransferProduceOrder(request, classIndex) != null
                        && !editableClassIndexes.contains(classIndex));
        if (invalidClassOrder) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.transfer.pastShift"));
        }
        boolean zeroQuantitySelected = editableClassIndexes.stream()
                .anyMatch(classIndex -> this.readTransferProduceOrder(request, classIndex) != null
                        && !this.hasCompleteTransferPlan(transferSources, classIndex));
        if (zeroQuantitySelected) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.transfer.zeroPlan"));
        }
        boolean missingProduceOrder = editableClassIndexes.stream()
                .anyMatch(classIndex -> this.hasCompleteTransferPlan(transferSources, classIndex)
                        && this.readTransferProduceOrder(request, classIndex) == null);
        if (missingProduceOrder) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.transfer.targetOrderRequired"));
        }
        boolean hasTransferPlan = editableClassIndexes.stream()
                .anyMatch(classIndex -> this.hasCompleteTransferPlan(
                        transferSources, classIndex));
        if (!hasTransferPlan) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.transfer.noTransferPlan"));
        }
        boolean lockedTransfer = editableClassIndexes.stream()
                .filter(classIndex -> this.hasCompleteTransferPlan(
                        transferSources, classIndex))
                .anyMatch(classIndex -> this.isTransferClassLocked(
                        transferSources, classIndex));
        return lockedTransfer
                ? AjaxResult.error(I18nUtil.getMessage("ui.cd15.transfer.locked"))
                : AjaxResult.success();
    }

    /** 解析转机台选中的单裁结果或同批次完整分裁作业单元。 */
    private List<Cd15ScheduleResult> resolveTransferSources(Cd15ScheduleResult source,
                                                             String factoryCode,
                                                             Date scheduleDate) {
        if (!Cd15CutMode.SPLIT.equals(source.getCutMode())) {
            return Collections.singletonList(source);
        }
        if (this.isBlank(source.getGroupNo())) {
            return Collections.emptyList();
        }
        List<Cd15ScheduleResult> groupResults = this.selectByDateAndFactory(
                        scheduleDate, factoryCode).stream()
                .filter(item -> Objects.equals(source.getCd15BatchNo(), item.getCd15BatchNo()))
                .filter(item -> Objects.equals(source.getGroupNo(), item.getGroupNo()))
                .filter(item -> Objects.equals(source.getMachineCode(), item.getMachineCode()))
                .filter(item -> Cd15CutMode.SPLIT.equals(item.getCutMode()))
                .collect(Collectors.toList());
        if (groupResults.size() == 1) {
            return groupResults;
        }
        boolean validPair = groupResults.size() == 2
                && groupResults.stream().map(Cd15ScheduleResult::getSteelStripCode)
                .filter(Objects::nonNull).distinct().count() == 2L
                && groupResults.stream().map(Cd15ScheduleResult::getBigRollCode)
                .distinct().count() == 1L
                && groupResults.stream().map(Cd15ScheduleResult::getCuttingAngle)
                .distinct().count() == 1L;
        return validPair ? groupResults : Collections.emptyList();
    }

    /** 解析当前排程窗口内尚未结束的可转班次。 */
    private List<Integer> resolveTransferEditableClassIndexes(String factoryCode,
                                                               Date scheduleDateValue) {
        LocalDate scheduleDate = this.toLocalDate(scheduleDateValue);
        LocalDateTime now = LocalDateTime.now();
        return shiftWindowResolver.resolve(
                        scheduleDate,
                        shiftConfigMapper.selectList(Wrappers.<Cd15ShiftConfig>lambdaQuery()
                                .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)))
                .stream()
                .filter(shift -> now.isBefore(shift.getEndTime()))
                .map(Cd15ShiftDescriptor::getClassField)
                .map(this::parseTransferClassIndex)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** 获取当前可转班次的起始索引；窗口结束时返回班次数量加一。 */
    private int resolveTransferEditableFromClassIndex(List<Integer> editableClassIndexes) {
        return editableClassIndexes.stream().findFirst().orElse(CLASS_COUNT + 1);
    }

    /** 解析CLASSn字段中的班次索引。 */
    private Integer parseTransferClassIndex(String classField) {
        if (this.isBlank(classField) || !classField.startsWith("CLASS")) {
            return null;
        }
        try {
            int classIndex = Integer.parseInt(classField.substring("CLASS".length()));
            return classIndex >= 1 && classIndex <= CLASS_COUNT ? classIndex : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 读取转机台指定班次的目标生产顺序，非正数按未填写处理。 */
    private Integer readTransferProduceOrder(Cd15TransferMachineRequest request,
                                             int classIndex) {
        Integer produceOrder = (Integer) request.getFieldValueByFieldName(
                String.format("class%dProduceOrder", classIndex));
        return produceOrder != null && produceOrder > 0 ? produceOrder : null;
    }

    /** 判断单裁结果或分裁作业单元在指定班次是否都有可转计划。 */
    private boolean hasCompleteTransferPlan(List<Cd15ScheduleResult> transferSources,
                                            int classIndex) {
        return !transferSources.isEmpty() && transferSources.stream()
                .allMatch(item -> this.readTransferPlanQuantity(item, classIndex) > 0D);
    }

    /** 判断指定班次是否包含已锁定、已完成或生产中的结果。 */
    private boolean isTransferClassLocked(List<Cd15ScheduleResult> transferSources,
                                          int classIndex) {
        return transferSources.stream()
                .anyMatch(item -> this.readTransferPlanQuantity(item, classIndex) > 0D
                        && this.isTransferResultLocked(item, classIndex));
    }

    /** 读取排程结果指定班次的计划量。 */
    private double readTransferPlanQuantity(Cd15ScheduleResult result, int classIndex) {
        Double planQuantity = (Double) result.getFieldValueByFieldName(
                String.format("class%dPlanQty", classIndex));
        return planQuantity == null ? 0D : planQuantity;
    }

    /** 按CD90同口径判断转机台班次是否已锁定或已生产。 */
    private boolean isTransferResultLocked(Cd15ScheduleResult result, int classIndex) {
        Double finishQuantity = (Double) result.getFieldValueByFieldName(
                String.format("class%dFinishQty", classIndex));
        Double planQuantity = (Double) result.getFieldValueByFieldName(
                String.format("class%dPlanQty", classIndex));
        return "1".equals(result.getIsLocked())
                || (finishQuantity != null && finishQuantity > 0D)
                || ("1".equals(result.getProductionStatus())
                && planQuantity != null
                && (finishQuantity == null || finishQuantity < planQuantity));
    }
    /**
     * 斜裁转机台 Service 入口。
     * 当前阶段先创建真实异步任务记录，滚动重排链路后续补齐。
     *
     * @param request 转机台请求
     * @return 任务入口结构
     */
    @Override
    public AjaxResult transferMachine(Cd15TransferMachineRequest request) {
        AjaxResult validation = this.validateTransferMachine(request);
        if (!Objects.equals(200, validation.get("code"))) {
            return validation;
        }
        AjaxResult batchValidation = this.validateBatchData(request.getFactoryCode(), request.getScheduleDate());
        if (batchValidation != null) {
            return batchValidation;
        }
        Cd15ScheduleTask activeTask = taskService.findActive(request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.schedule.taskActive"));
        }
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            AjaxResult previewResult = this.previewTransferMachine(
                    request, this.toLocalDate(request.getScheduleDate()));
            if (previewResult != null) {
                return previewResult;
            }
        }
        Cd15ScheduleTask task = taskService.createPending(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.TRANSFER_MACHINE, "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.executeTransfer(task.getTaskId(), request);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), this.toTaskData(task));
    }
    /**
     * 使用正式滚动内核执行转机台只读预演，发现跨班顺延时返回确认明细。
     *
     * @param request 转机台请求
     * @param scheduleDate 排程日期
     * @return 需要确认时返回确认结构，无顺延时返回null
     */
    private AjaxResult previewTransferMachine(Cd15TransferMachineRequest request,
                                              LocalDate scheduleDate) {
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd15.schedule.taskActive"));
            }
            if (taskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd15.schedule.taskActive"));
            }
            Cd15InsertRollingOutput output = insertRollingService.executeTransfer(request);
            List<Cd15InsertCarryoverImpact> impacts = output.getCarryoverImpacts() == null
                    ? Collections.emptyList() : output.getCarryoverImpacts();
            if (impacts.isEmpty()) {
                return null;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("needConfirm", true);
            data.put("carryoverDetails", impacts.stream()
                    .map(this::toCarryoverDetail)
                    .collect(Collectors.toList()));
            return AjaxResult.success(
                    I18nUtil.getMessage("ui.cd15.transfer.carryoverConfirm"), data);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 将引擎顺延影响转换为前端确认结构。 */
    private Map<String, Object> toCarryoverDetail(Cd15InsertCarryoverImpact impact) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("steelStripCode", impact.getSteelStripCode());
        detail.put("affectedType", impact.getAffectedType());
        detail.put("sourceClassField", impact.getSourceClassField());
        detail.put("targetClassField", impact.getTargetClassField());
        detail.put("carryoverQty", impact.getCarryoverQty());
        detail.put("reasonCode", impact.getReasonCode());
        detail.put("reasonMessage", this.resolveCarryoverReason(impact.getReasonCode()));
        return detail;
    }

    /** 将滚动限制原因转换为用户可理解的国际化说明。 */
    private String resolveCarryoverReason(String reasonCode) {
        if ("CAPACITY_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd15.transfer.reason.capacityLimit");
        }
        if ("STORAGE_LANE_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd15.transfer.reason.storageLaneLimit");
        }
        if ("ROLL_TOOL_LIMIT".equals(reasonCode) || "TOOLING_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd15.transfer.reason.toolingLimit");
        }
        if ("BIG_ROLL_STOCK_DATA_MISSING".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd15.transfer.reason.bigRollStockDataMissing");
        }
        if ("CONSTRUCTION_MISSING".equals(reasonCode) || "DATA_MISSING".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd15.transfer.reason.constructionMissing");
        }
        if ("AGING_PERIOD_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd15.transfer.reason.agingPeriodLimit");
        }
        if ("SCHEDULE_WINDOW_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd15.transfer.reason.scheduleWindowLimit");
        }
        return I18nUtil.getMessage("ui.cd15.transfer.reason.other");
    }

    @Override
    public AjaxResult getTransferMachineTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.TRANSFER_MACHINE);
    }

    @Override
    public AjaxResult validateChangeQty(Cd15ChangeQtyRequest request) {
        AjaxResult validation = this.validateChangeQtyBasic(request);
        if (!Objects.equals(200, validation.get("code"))) {
            return validation;
        }
        AjaxResult batchValidation = this.validateBatchData(
                request.getFactoryCode(), request.getScheduleDate());
        if (batchValidation != null) {
            return batchValidation;
        }
        AjaxResult previewResult = this.previewChangeQty(
                request, this.toLocalDate(request.getScheduleDate()));
        return previewResult != null
                && !Objects.equals(200, previewResult.get("code"))
                ? previewResult : AjaxResult.success();
    }

    /** 校验调量请求的字段、目标记录、班次窗口及完成量。 */
    private AjaxResult validateChangeQtyBasic(Cd15ChangeQtyRequest request) {
        if (request == null) {
            return this.required("request");
        }
        if (this.isBlank(request.getFactoryCode())) {
            return this.required("factoryCode");
        }
        if (request.getScheduleDate() == null) {
            return this.required("scheduleDate");
        }
        if (request.getScheduleResultId() == null) {
            return this.required("scheduleResultId");
        }
        if (this.isBlank(request.getMachineCode())) {
            return this.required("machineCode");
        }
        if (this.isBlank(request.getSteelStripCode())) {
            return this.required("steelStripCode");
        }
        Map<Integer, Double> targetQtyByClass;
        try {
            targetQtyByClass = this.resolveChangeQtyTargets(request);
        } catch (IllegalArgumentException exception) {
            return AjaxResult.error(exception.getMessage());
        }
        List<Cd15ScheduleResult> latestResults = this.latestBatchResults(
                this.selectByDateAndFactory(request.getScheduleDate(),
                        request.getFactoryCode()));
        Cd15ScheduleResult source = latestResults.stream()
                .filter(item -> Objects.equals(
                        item.getId(), request.getScheduleResultId()))
                .findFirst().orElse(null);
        if (source == null
                || !Objects.equals(source.getMachineCode(), request.getMachineCode())
                || !Objects.equals(source.getSteelStripCode(), request.getSteelStripCode())
                || (!this.isBlank(request.getGroupNo())
                && !Objects.equals(source.getGroupNo(), request.getGroupNo()))) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.adjust.resultMismatch"));
        }
        List<Cd15ScheduleResult> adjustmentSources = this.resolveTransferSources(
                source, request.getFactoryCode(), request.getScheduleDate());
        if (adjustmentSources.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.adjust.resultMismatch"));
        }
        boolean allSame = targetQtyByClass.entrySet().stream().allMatch(entry ->
                BigDecimal.valueOf(this.readTransferPlanQuantity(source, entry.getKey()))
                        .compareTo(BigDecimal.valueOf(entry.getValue())) == 0);
        if (allSame) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.changeQty.same"));
        }
        List<Integer> editableClassIndexes = this.resolveTransferEditableClassIndexes(
                request.getFactoryCode(), request.getScheduleDate());
        if (editableClassIndexes.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.changeQty.windowEnded"));
        }
        boolean containsPastShift = targetQtyByClass.keySet().stream()
                .anyMatch(classIndex -> !editableClassIndexes.contains(classIndex));
        if (containsPastShift) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.changeQty.pastShift"));
        }
        boolean splitCut = Cd15CutMode.SPLIT.equals(source.getCutMode());
        if (splitCut && targetQtyByClass.values().stream()
                .anyMatch(targetQuantity -> targetQuantity <= 0D)) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.changeQty.splitPositive"));
        }
        boolean splitPlanMissing = splitCut && targetQtyByClass.keySet().stream()
                .anyMatch(classIndex -> adjustmentSources.stream()
                        .anyMatch(item -> this.readTransferPlanQuantity(
                                item, classIndex) <= 0D));
        if (splitPlanMissing) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.changeQty.splitPlanMissing"));
        }
        boolean lockedClass = targetQtyByClass.keySet().stream()
                .anyMatch(classIndex -> adjustmentSources.stream()
                        .anyMatch(item -> this.isTransferResultLocked(
                                item, classIndex)));
        if (lockedClass) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.changeQty.locked"));
        }
        boolean lessThanFinish = targetQtyByClass.entrySet().stream()
                .anyMatch(entry -> {
                    Double finishQuantity = this.readChangeQtyFinishQuantity(
                            source, entry.getKey());
                    return finishQuantity != null
                            && entry.getValue() < finishQuantity;
                });
        return lessThanFinish
                ? AjaxResult.error(I18nUtil.getMessage(
                "ui.cd15.changeQty.lessThanFinish"))
                : AjaxResult.success();
    }

    /** 解析调量目标量，兼容单班字段和逐班字段两种入参。 */
    private Map<Integer, Double> resolveChangeQtyTargets(
            Cd15ChangeQtyRequest request) {
        Map<Integer, Double> targetQtyByClass = new LinkedHashMap<>();
        boolean hasLegacyClass = !this.isBlank(request.getStartClassField());
        boolean hasLegacyQuantity = request.getTargetPlanQty() != null;
        if (hasLegacyClass || hasLegacyQuantity) {
            Integer classIndex = hasLegacyClass
                    ? this.parseTransferClassIndex(
                    request.getStartClassField().trim().toUpperCase())
                    : null;
            if (classIndex == null || !hasLegacyQuantity) {
                throw new IllegalArgumentException(I18nUtil.getMessage(
                        "ui.cd15.changeQty.invalidTarget"));
            }
            targetQtyByClass.put(classIndex, request.getTargetPlanQty());
        }
        IntStream.rangeClosed(1, CLASS_COUNT).forEach(classIndex -> {
            Double targetQuantity = this.readPlanQty(request, classIndex);
            if (targetQuantity != null) {
                targetQtyByClass.put(classIndex, targetQuantity);
            }
        });
        if (targetQtyByClass.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.cd15.changeQty.invalidTarget"));
        }
        boolean invalidTarget = targetQtyByClass.entrySet().stream()
                .anyMatch(entry -> entry.getKey() < 1 || entry.getKey() > CLASS_COUNT
                        || entry.getValue() == null
                        || !Double.isFinite(entry.getValue())
                        || entry.getValue() < 0D);
        if (invalidTarget) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.cd15.changeQty.invalidTarget"));
        }
        return targetQtyByClass;
    }

    /** 读取调量目标结果指定班次的完成量。 */
    private Double readChangeQtyFinishQuantity(Cd15ScheduleResult result,
                                               int classIndex) {
        return (Double) result.getFieldValueByFieldName(
                String.format("class%dFinishQty", classIndex));
    }

    /**
     * 斜裁调量 Service 入口。
     * 当前阶段先创建真实异步任务记录，滚动重排链路后续补齐。
     *
     * @param request 调量请求
     * @return 任务入口结构
     */
    @Override
    public AjaxResult changeQty(Cd15ChangeQtyRequest request) {
        AjaxResult validation = this.validateChangeQtyBasic(request);
        if (!Objects.equals(200, validation.get("code"))) {
            return validation;
        }
        AjaxResult batchValidation = this.validateBatchData(request.getFactoryCode(), request.getScheduleDate());
        if (batchValidation != null) {
            return batchValidation;
        }
        Cd15ScheduleTask activeTask = taskService.findActive(request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.schedule.taskActive"));
        }
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            AjaxResult previewResult = this.previewChangeQty(
                    request, this.toLocalDate(request.getScheduleDate()));
            if (previewResult != null) {
                return previewResult;
            }
        }
        Cd15ScheduleTask task = taskService.createPending(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.CHANGE_QTY, "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.executeChangeQty(task.getTaskId(), request);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), this.toTaskData(task));
    }
    /** 使用正式滚动内核预演调量，发现跨班顺延时返回确认明细。 */
    private AjaxResult previewChangeQty(Cd15ChangeQtyRequest request,
                                        LocalDate scheduleDate) {
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.cd15.schedule.taskActive"));
            }
            if (taskService.findActive(request.getFactoryCode(),
                    request.getScheduleDate()) != null) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.cd15.schedule.taskActive"));
            }
            Cd15InsertRollingOutput output = insertRollingService.executeChangeQty(request);
            List<Cd15InsertCarryoverImpact> impacts = output.getCarryoverImpacts() == null
                    ? Collections.emptyList() : output.getCarryoverImpacts();
            if (impacts.isEmpty()) {
                return null;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("needConfirm", true);
            data.put("carryoverDetails", impacts.stream()
                    .map(this::toCarryoverDetail)
                    .collect(Collectors.toList()));
            return AjaxResult.success(I18nUtil.getMessage(
                    "ui.cd15.changeQty.carryoverConfirm"), data);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public AjaxResult getChangeQtyTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.CHANGE_QTY);
    }

    @Override
    public AjaxResult checkTimedRolling(Cd15RollingCheckRequest request) {
        return timedRollingCheckService.check(request);
    }

    @Override
    public AjaxResult getTimedRollingTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.ROLLING_SCHEDULE);
    }

    @Override
    public List<Cd15ScheduleResult> selectByDateAndFactory(Date scheduleDate,
                                                           String factoryCode) {
        if (scheduleDate == null || this.isBlank(factoryCode)) {
            return new ArrayList<>();
        }
        return resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getScheduleDate, scheduleDate)
                .eq(Cd15ScheduleResult::getFactoryCode, factoryCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public int batchUpdateReleaseStatus(List<Cd15ScheduleResult> list,
                                        String targetStatus) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        Date publishTime = new Date();
        list.forEach(result -> {
            result.setReleaseStatus(targetStatus);
            if (ApsConstant.IS_RELEASE.equals(targetStatus)) {
                result.setPublishSuccessCount(
                        Optional.ofNullable(result.getPublishSuccessCount()).orElse(0) + 1);
                result.setNewestPublishTime(publishTime);
            }
        });
        this.baseDao.updateBatch(list);
        return list.size();
    }

    private AjaxResult validateInsertRequired(Cd15InsertOrderRequest request) {
        if (request == null) {
            return this.required("request");
        }
        if (this.isBlank(request.getFactoryCode())) {
            return this.required("factoryCode");
        }
        if (request.getScheduleDate() == null) {
            return this.required("scheduleDate");
        }
        if (this.isBlank(request.getMachineCode())) {
            return this.required("machineCode");
        }
        if (this.isBlank(request.getSteelStripCode())) {
            return this.required("steelStripCode");
        }
        return null;
    }

    private AjaxResult validatePlanAndProduceOrder(Cd15InsertOrderRequest request) {
        boolean pairInvalid = IntStream.rangeClosed(1, CLASS_COUNT)
                .anyMatch(classIndex -> this.isPlanAndProduceOrderPairInvalid(request, classIndex));
        if (pairInvalid) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.parameter.error"));
        }
        boolean hasPlan = IntStream.rangeClosed(1, CLASS_COUNT)
                .mapToObj(classIndex -> this.hasPositivePlan(request, classIndex))
                .anyMatch(Boolean::booleanValue);
        return hasPlan ? AjaxResult.success() : this.required("classPlanQty");
    }

    private boolean isPlanAndProduceOrderPairInvalid(Cd15InsertOrderRequest request, int classIndex) {
        Double planQuantity = (Double) request.getFieldValueByFieldName(String.format("class%dPlanQty", classIndex));
        Integer produceOrder = (Integer) request.getFieldValueByFieldName(String.format("class%dProduceOrder", classIndex));
        boolean positivePlan = planQuantity != null && planQuantity > 0D;
        return positivePlan != (produceOrder != null && produceOrder > 0);
    }

    private boolean hasPositivePlan(Cd15InsertOrderRequest request, int classIndex) {
        Double planQuantity = (Double) request.getFieldValueByFieldName(String.format("class%dPlanQty", classIndex));
        return planQuantity != null && planQuantity > 0D;
    }

    private Double readPlanQty(Cd15ChangeQtyRequest request, int classIndex) {
        return (Double) request.getFieldValueByFieldName(String.format("class%dPlanQty", classIndex));
    }

    /** 获取插单请求中实际提交计划的班次。 */
    private List<Integer> insertClassIndexes(Cd15InsertOrderRequest request) {
        return IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(classIndex -> this.hasPositivePlan(request, classIndex))
                .boxed().collect(Collectors.toList());
    }

    /** 获取转机台请求中实际选择的班次。 */
    private List<Integer> transferClassIndexes(Cd15TransferMachineRequest request) {
        return IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(classIndex -> {
                    Integer order = (Integer) request.getFieldValueByFieldName(
                            String.format("class%dProduceOrder", classIndex));
                    return order != null && order > 0;
                }).boxed().collect(Collectors.toList());
    }

    /** 获取调量请求中实际提交目标量的班次。 */
    private List<Integer> changeQtyClassIndexes(Cd15ChangeQtyRequest request) {
        List<Integer> classIndexes = IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(classIndex -> this.readPlanQty(request, classIndex) != null)
                .boxed().collect(Collectors.toList());
        if (classIndexes.isEmpty() && !this.isBlank(request.getStartClassField())
                && request.getTargetPlanQty() != null) {
            String classIndexText = request.getStartClassField().trim()
                    .toUpperCase().replace("CLASS", "");
            try {
                int classIndex = Integer.parseInt(classIndexText);
                if (classIndex >= 1 && classIndex <= CLASS_COUNT) {
                    classIndexes.add(classIndex);
                }
            } catch (NumberFormatException ignored) {
                return java.util.Collections.emptyList();
            }
        }
        return classIndexes;
    }

    /** 校验人工调整涉及的班次尚未结束。 */
    private AjaxResult validateEditableClasses(String factoryCode,
                                                Date scheduleDateValue,
                                                List<Integer> classIndexes,
                                                String endedMessageKey) {
        LocalDate scheduleDate = this.toLocalDate(scheduleDateValue);
        Map<String, Cd15ShiftDescriptor> shiftByClass = shiftWindowResolver.resolve(
                        scheduleDate,
                        shiftConfigMapper.selectList(Wrappers.<Cd15ShiftConfig>lambdaQuery()
                                .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)))
                .stream().collect(Collectors.toMap(Cd15ShiftDescriptor::getClassField,
                        shift -> shift, (first, second) -> first, LinkedHashMap::new));
        LocalDateTime now = LocalDateTime.now();
        for (Integer classIndex : classIndexes) {
            String classField = "CLASS" + classIndex;
            Cd15ShiftDescriptor shift = shiftByClass.get(classField);
            if (shift == null) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.cd15.adjust.shiftNotConfigured"));
            }
            if (!now.isBefore(shift.getEndTime())) {
                return AjaxResult.error(MessageFormat.format(
                        I18nUtil.getMessage(endedMessageKey), classField));
            }
        }
        return AjaxResult.success();
    }

    /** 只保留同日同工厂最新批次的排程结果。 */
    private List<Cd15ScheduleResult> latestBatchResults(
            List<Cd15ScheduleResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        String latestBatchNo = results.stream()
                .map(Cd15ScheduleResult::getCd15BatchNo)
                .filter(Objects::nonNull)
                .max(String::compareTo).orElse(null);
        if (latestBatchNo == null) {
            return Collections.emptyList();
        }
        return results.stream()
                .filter(item -> Objects.equals(
                        latestBatchNo, item.getCd15BatchNo()))
                .collect(Collectors.toList());
    }

    /** 按主键、工厂和排程日期定位页面选中的排程结果。 */
    private Optional<Cd15ScheduleResult> findSelectedResult(Long resultId,
                                                            String factoryCode,
                                                            Date scheduleDate) {
        return Optional.ofNullable(resultMapper.selectOne(
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .eq(Cd15ScheduleResult::getId, resultId)
                        .eq(Cd15ScheduleResult::getFactoryCode, factoryCode)
                        .eq(Cd15ScheduleResult::getScheduleDate, scheduleDate)
                        .last("limit 1")));
    }

    private boolean isSuccess(AjaxResult result) {
        return result != null && Objects.equals(200, result.get("code"));
    }

    private AjaxResult validateBatchData(String factoryCode, Date scheduleDate) {
        Cd15BatchDataCheckResult batchCheck = batchDataValidator.check(
                factoryCode, this.toLocalDate(scheduleDate));
        if (!batchCheck.isFailed()) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("needConfirm", false);
        data.put("batchCheckFailed", true);
        data.put("errors", this.toErrorList(batchCheck.getErrors()));
        data.put("warnings", this.toErrorList(batchCheck.getWarnings()));
        return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
    }

    private AjaxResult taskView(String taskId, String expectedTaskType) {
        if (this.isBlank(taskId)) {
            return this.required("taskId");
        }
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !expectedTaskType.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.parameter.error"));
        }
        return AjaxResult.success(this.toTaskData(task));
    }

    private List<Map<String, Object>> toErrorList(List<Cd15BatchDataCheckResult.CheckError> errors) {
        return errors.stream()
                .map(error -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("field", error.getField());
                    item.put("reasonCode", error.getReasonCode());
                    item.put("message", error.getMessage());
                    item.put("suggestion", error.getSuggestion());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private LocalDate toLocalDate(Date scheduleDate) {
        return scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Map<String, Object> toTaskData(Cd15ScheduleTask task) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("needConfirm", false);
        data.put("taskId", task.getTaskId());
        data.put("taskType", task.getTaskType());
        data.put("taskStatus", task.getTaskStatus());
        data.put("progress", task.getProgress());
        data.put("currentStage", task.getCurrentStage());
        data.put("currentStageName", task.getCurrentStageName());
        data.put("batchNo", task.getBatchNo());

        data.put("errorMessage", task.getErrorMessage());
        data.put("engineImplemented", Cd15ScheduleTaskType.AUTO_SCHEDULE.equals(task.getTaskType())
                || Cd15ScheduleTaskType.INSERT_ORDER.equals(task.getTaskType())
                || Cd15ScheduleTaskType.TRANSFER_MACHINE.equals(task.getTaskType())
                || Cd15ScheduleTaskType.CHANGE_QTY.equals(task.getTaskType())
                || Cd15ScheduleTaskType.ROLLING_SCHEDULE.equals(task.getTaskType()));
        return data;
    }

    private AjaxResult required(String fieldName) {
        String message = MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), fieldName);
        return AjaxResult.error(message);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected String getDocTypeCode() {
        return "CD15_SCHEDULE_RESULT";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_SCHEDULE_RESULT");
        return sysDocType;
    }
}
