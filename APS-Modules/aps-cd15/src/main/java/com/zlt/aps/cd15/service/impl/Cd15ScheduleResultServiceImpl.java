package com.zlt.aps.cd15.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15BatchDataCheckResult;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleBatchDataValidator;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.service.ICd15ScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 斜裁排程结果业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15ScheduleResultServiceImpl extends AbstractDocService<Cd15ScheduleResult> implements ICd15ScheduleResultService {

    @Resource
    private Cd15ScheduleTaskService taskService;

    @Resource
    private Cd15AutoScheduleBatchDataValidator batchDataValidator;

    @Resource
    private Cd15AutoScheduleInputVersionService inputVersionService;

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
        String inputVersion = inputVersionService.fingerprint(scheduleResult.getFactoryCode(), localScheduleDate);
        String snapshot = "factoryCode=" + scheduleResult.getFactoryCode()
                + ",scheduleDate=" + scheduleResult.getScheduleDate()
                + ",forceRegenerate=" + Boolean.TRUE.equals(scheduleResult.getForceRegenerate());
        return this.createTask(scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate(),
                Cd15ScheduleTaskType.AUTO_SCHEDULE, snapshot, inputVersion);
    }

    @Override
    public AjaxResult getAutoScheduleTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.AUTO_SCHEDULE);
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
        return this.createTask(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.INSERT_ORDER, request.toString());
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
        return this.createTask(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.TRANSFER_MACHINE, request.toString());
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
                || IntStream.rangeClosed(1, 6)
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
        return this.createTask(request.getFactoryCode(), request.getScheduleDate(),
                Cd15ScheduleTaskType.CHANGE_QTY, request.toString());
    }

    @Override
    public AjaxResult getChangeQtyTask(String taskId) {
        return this.taskView(taskId, Cd15ScheduleTaskType.CHANGE_QTY);
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
        boolean pairInvalid = IntStream.rangeClosed(1, 6)
                .anyMatch(classIndex -> this.isPlanAndProduceOrderPairInvalid(request, classIndex));
        if (pairInvalid) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.parameter.error"));
        }
        boolean hasPlan = IntStream.rangeClosed(1, 6)
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
        data.put("engineImplemented", false);
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