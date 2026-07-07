package com.zlt.aps.cd15.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.service.ICd15ScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 斜裁排程结果业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15ScheduleResultServiceImpl extends AbstractDocService<Cd15ScheduleResult> implements ICd15ScheduleResultService {

    private static final String TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String TASK_STAGE_ENGINE_PENDING = "ENGINE_NOT_IMPLEMENTED";

    /**
     * 斜裁自动排程 Service 入口。
     * 首期只打通页面到 Service 的链路，Engine 暂不实现。
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
        return this.successTask("AUTO_SCHEDULE");
    }

    @Override
    public AjaxResult getAutoScheduleTask(String taskId) {
        return this.successTaskView(taskId, "AUTO_SCHEDULE");
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
     * 首期只做参数契约和任务入口返回，Engine 暂不实现。
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
        return this.successTask("INSERT_ORDER");
    }

    @Override
    public AjaxResult getInsertTask(String taskId) {
        return this.successTaskView(taskId, "INSERT_ORDER");
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
     * 首期只做参数契约和任务入口返回，Engine 暂不实现。
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
        return this.successTask("TRANSFER_MACHINE");
    }

    @Override
    public AjaxResult getTransferMachineTask(String taskId) {
        return this.successTaskView(taskId, "TRANSFER_MACHINE");
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
     * 首期只做参数契约和任务入口返回，Engine 暂不实现。
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
        return this.successTask("CHANGE_QTY");
    }

    @Override
    public AjaxResult getChangeQtyTask(String taskId) {
        return this.successTaskView(taskId, "CHANGE_QTY");
    }

    @Override
    public AjaxResult publish(Cd15ScheduleResult dto, String ids) {
        return this.successTask("PUBLISH");
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

    private AjaxResult successTask(String taskType) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", "CD15-" + taskType + "-" + System.currentTimeMillis());
        data.put("taskType", taskType);
        data.put("taskStatus", TASK_STATUS_SUCCESS);
        data.put("progress", 100);
        data.put("currentStageName", TASK_STAGE_ENGINE_PENDING);
        data.put("engineImplemented", false);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"), data);
    }

    private AjaxResult successTaskView(String taskId, String taskType) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("taskType", taskType);
        data.put("taskStatus", TASK_STATUS_SUCCESS);
        data.put("progress", 100);
        data.put("currentStageName", TASK_STAGE_ENGINE_PENDING);
        data.put("engineImplemented", false);
        return AjaxResult.success(data);
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
