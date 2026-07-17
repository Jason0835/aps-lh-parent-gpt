package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineShiftConfigMapper;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15BatchDataCheckResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleBatchDataValidator;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.model.Cd15ScheduleOverwriteDecision;
import com.zlt.aps.cd15.service.Cd15AutoScheduleAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15InsertOrderAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15ScheduleOverwriteValidator;
import com.zlt.aps.cd15.service.Cd15TimedRollingCheckService;
import com.zlt.aps.cd15.service.ICd15ScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private Cd15AutoScheduleInputVersionService inputVersionService;

    @Resource
    private Cd15ScheduleOverwriteValidator overwriteValidator;

    @Resource
    private Cd15InsertOrderAsyncExecutor insertOrderAsyncExecutor;

    @Resource
    private Cd15TimedRollingCheckService timedRollingCheckService;

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
            return AjaxResult.success("当前日期已有斜裁排程任务正在执行", this.toTaskData(activeTask));
        }
        String inputVersion = inputVersionService.fingerprint(scheduleResult.getFactoryCode(), localScheduleDate);
        String snapshot = "factoryCode=" + scheduleResult.getFactoryCode()
                + ",scheduleDate=" + scheduleResult.getScheduleDate()
                + ",forceRegenerate=" + Boolean.TRUE.equals(scheduleResult.getForceRegenerate());
        Cd15ScheduleTask task = taskService.createPending(scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate(),
                Cd15ScheduleTaskType.AUTO_SCHEDULE, "MANUAL", snapshot, inputVersion, null);
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
                    item.put("shiftName", shift.getShiftName());
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
        return this.validatePlanAndProduceOrder(request);
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
            return AjaxResult.error("当前日期已有斜裁排程任务正在执行");
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
        if (this.isBlank(request.getSourceMachineCode())) {
            return this.required("sourceMachineCode");
        }
        if (this.isBlank(request.getTargetMachineCode())) {
            return this.required("targetMachineCode");
        }
        if (request.getSourceMachineCode().equals(request.getTargetMachineCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.parameter.error"));
        }
        return AjaxResult.success();
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
            return AjaxResult.error("当前日期已有斜裁排程任务正在执行");
        }
        Cd15ScheduleTask task = taskService.createPending(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.TRANSFER_MACHINE, "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.executeTransfer(task.getTaskId(), request);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), this.toTaskData(task));
    }
    @Override
    public AjaxResult getTransferMachineTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.TRANSFER_MACHINE);
    }

    @Override
    public AjaxResult validateChangeQty(Cd15ChangeQtyRequest request) {
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
        boolean hasTargetQty = request.getTargetPlanQty() != null
                || IntStream.rangeClosed(1, CLASS_COUNT)
                .mapToObj(classIndex -> this.readPlanQty(request, classIndex))
                .anyMatch(Objects::nonNull);
        if (!hasTargetQty) {
            return this.required("targetPlanQty");
        }
        return AjaxResult.success();
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
        AjaxResult validation = this.validateChangeQty(request);
        if (!Objects.equals(200, validation.get("code"))) {
            return validation;
        }
        AjaxResult batchValidation = this.validateBatchData(request.getFactoryCode(), request.getScheduleDate());
        if (batchValidation != null) {
            return batchValidation;
        }
        Cd15ScheduleTask activeTask = taskService.findActive(request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error("当前日期已有斜裁排程任务正在执行");
        }
        Cd15ScheduleTask task = taskService.createPending(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.CHANGE_QTY, "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.executeChangeQty(task.getTaskId(), request);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), this.toTaskData(task));
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
        return this.taskView(taskId, Cd15ScheduleTaskType.TIMED_ROLLING);
    }

    @Override
    public AjaxResult publish(Cd15ScheduleResult dto, String ids) {
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
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
    private AjaxResult createTask(String factoryCode, Date scheduleDate, String taskType, String requestSnapshot) {
        return this.createTask(factoryCode, scheduleDate, taskType, requestSnapshot, null);
    }

    private AjaxResult createTask(String factoryCode, Date scheduleDate, String taskType,
                                  String requestSnapshot, String inputVersion) {
        Cd15ScheduleTask task = taskService.createPending(factoryCode, scheduleDate, taskType,
                "MANUAL", requestSnapshot, inputVersion, null);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), this.toTaskData(task));
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
        data.put("inputVersion", task.getInputVersion());
        data.put("errorMessage", task.getErrorMessage());
        data.put("engineImplemented", Cd15ScheduleTaskType.AUTO_SCHEDULE.equals(task.getTaskType())
                || Cd15ScheduleTaskType.INSERT_ORDER.equals(task.getTaskType())
                || Cd15ScheduleTaskType.TRANSFER_MACHINE.equals(task.getTaskType())
                || Cd15ScheduleTaskType.CHANGE_QTY.equals(task.getTaskType())
                || Cd15ScheduleTaskType.TIMED_ROLLING.equals(task.getTaskType()));
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