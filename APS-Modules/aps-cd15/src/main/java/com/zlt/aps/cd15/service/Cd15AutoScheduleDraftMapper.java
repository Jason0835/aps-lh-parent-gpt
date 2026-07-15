package com.zlt.aps.cd15.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** 将CD15 Engine草稿转换为持久化实体。 */
@Component
public class Cd15AutoScheduleDraftMapper {

    private static final String AUTO_SOURCE = "AUTO_SCHEDULE";
    private static final String AUTO_SOURCE_CODE = "0";
    private static final String UNRELEASED = "0";
    private static final String UNLOCKED = "0";

    private final ObjectMapper objectMapper;

    public Cd15AutoScheduleDraftMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Cd15ScheduleResult toScheduleResult(String factoryCode, LocalDate scheduleDate,
                                               String batchNo, Cd15ScheduleResultDraft draft) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setFactoryCode(factoryCode);
        result.setScheduleDate(this.date(scheduleDate));
        result.setCd15BatchNo(batchNo);
        result.setCxBatchNo(draft.getCxBatchNo());
        result.setCxMachineCodes(draft.getCxMachineCodes());
        result.setPlanSurplusQty(draft.getPlanSurplusQty());
        result.setOrderNo(draft.getOrderNo());
        result.setGroupNo(draft.getGroupNo());
        result.setReleaseStatus(UNRELEASED);
        result.setBigRollCode(draft.getBigRollCode());
        result.setCuttingAngle(draft.getCuttingAngle());
        result.setMachineCode(draft.getMachineCode());
        result.setMachineName(draft.getMachineName());
        result.setSteelStripCode(draft.getSteelStripCode());
        result.setStockQty(this.decimal(draft.getStockMetersAtSix()));
        result.setCutMode(draft.getCutMode());
        result.setSourceType(draft.getSourceType() == null ? AUTO_SOURCE : draft.getSourceType());
        result.setIsLocked(UNLOCKED);
        this.applyClassSlot(result, draft);
        return result;
    }

    public Cd15UnscheduleResult toUnscheduleResult(String factoryCode, LocalDate scheduleDate,
                                                   String batchNo, Cd15SingleShiftScheduleResult source,
                                                   int reasonOrder) {
        Cd15UnscheduleResult result = new Cd15UnscheduleResult();
        result.setFactoryCode(this.firstText(source.getFactoryCode(), factoryCode));
        result.setScheduleDate(source.getScheduleDate() == null ? this.date(scheduleDate) : source.getScheduleDate());
        result.setBatchNo(batchNo);
        result.setSteelStripCode(this.firstText(source.getSteelStripCode(), "UNKNOWN"));
        result.setBigRollCode(source.getBigRollCode());
        result.setCuttingAngle(source.getCuttingAngle());
        result.setMachineCode(source.getMachineCode());
        result.setClassField(source.getClassField());
        result.setDemandQty(source.getDemandQty());
        result.setScheduledQty(source.getScheduledQty() == null ? BigDecimal.ZERO : source.getScheduledQty());
        result.setUnscheduledQty(source.getUnscheduledQty() == null ? source.getDemandQty() : source.getUnscheduledQty());
        result.setFailStage(source.getUnscheduledReasonCode());
        result.setUnscheduleReasonCode(this.firstText(source.getUnscheduledReasonCode(), "DATA_MISSING"));
        result.setReasonOrder(reasonOrder);
        result.setPrimaryReason(reasonOrder == 1 ? "1" : "0");
        result.setUnscheduledReason(source.getUnscheduledReason());
        result.setDataSource(AUTO_SOURCE_CODE);
        result.setOrderNo(source.getOrderNo());
        result.setGroupNo(source.getGroupNo());
        return result;
    }


    public Cd15ScheduleLaneAllocation toLaneAllocation(String batchNo,
                                                       Cd15ScheduleResult parent,
                                                       Cd15ScheduleResultDraft draft,
                                                       Cd15LaneAllocationDraft laneDraft,
                                                       int allocationOrder) {
        Cd15ScheduleLaneAllocation entity = new Cd15ScheduleLaneAllocation();
        entity.setFactoryCode(parent.getFactoryCode());
        entity.setScheduleDate(parent.getScheduleDate());
        entity.setBatchNo(batchNo);
        entity.setScheduleResultId(parent.getId());
        entity.setOrderNo(parent.getOrderNo());
        entity.setGroupNo(parent.getGroupNo());
        entity.setClassField(laneDraft.getClassField());
        entity.setShiftScheduleDate(draft.getScheduleDate());
        entity.setStorageLaneCode(laneDraft.getLaneCode());
        entity.setSteelStripCode(parent.getSteelStripCode());
        entity.setBigRollCode(parent.getBigRollCode());
        entity.setCuttingAngle(parent.getCuttingAngle());
        entity.setMachineCode(parent.getMachineCode());
        entity.setAllocatedQty(laneDraft.getAllocationQuantity());
        entity.setAllocatedCartCount(laneDraft.getVehicleCount());
        entity.setAllocationOrder(allocationOrder);
        return entity;
    }
    public Cd15ScheduleResultLog toCreateLog(String taskId, String batchNo,
                                             Cd15ScheduleResult entity,
                                             Cd15ScheduleResultDraft draft) {
        Cd15ScheduleResultLog log = new Cd15ScheduleResultLog();
        log.setScheduleResultId(entity.getId());
        log.setTaskId(taskId);
        log.setLogType("AUTO_SCHEDULE");
        log.setLogTime(new Date());
        log.setReasonCode("AUTO_SCHEDULE");
        log.setReasonDetail(this.json(draft));
        log.setFactoryCode(entity.getFactoryCode());
        log.setScheduleDate(entity.getScheduleDate());
        log.setCxBatchNo(entity.getCxBatchNo());
        log.setBatchNo(batchNo);
        log.setOrderNo(entity.getOrderNo());
        log.setGroupNo(entity.getGroupNo());
        log.setBigRollCode(entity.getBigRollCode());
        log.setSteelStripCode(entity.getSteelStripCode());
        log.setCuttingAngle(entity.getCuttingAngle());
        log.setMachineCode(entity.getMachineCode());
        log.setStorageLaneCode(entity.getStorageLaneCode());
        log.setStockQty(draft.getStockMetersAtSix());
        log.setSourceType(entity.getSourceType());
        log.setReleaseStatus(entity.getReleaseStatus());
        log.setProductionStatus(entity.getProductionStatus());
        log.setClassField(draft.getClassField());
        log.setAfterJson(this.json(entity));
        log.setChangeReason("CD15自动排程生成");
        return log;
    }

    private void applyClassSlot(Cd15ScheduleResult result, Cd15ScheduleResultDraft draft) {
        int classIndex = draft.getClassIndex();
        if (classIndex <= 0 && draft.getClassField() != null && draft.getClassField().startsWith("CLASS")) {
            classIndex = Integer.parseInt(draft.getClassField().replace("CLASS", ""));
        }
        String prefix = "class" + classIndex;
        result.setFieldValueByFieldName(prefix + "ScheduleDate", draft.getScheduleDate());
        result.setFieldValueByFieldName(prefix + "CxPlanQty", this.decimal(draft.getCxPlanQty()));
        result.setFieldValueByFieldName(prefix + "PlanQty", this.decimal(draft.getPlanQty()));
        result.setFieldValueByFieldName(prefix + "FinishQty", 0D);
        result.setFieldValueByFieldName(prefix + "ProduceOrder", draft.getProduceOrder());
        result.setFieldValueByFieldName(prefix + "FinishRate", 0D);
        result.setFieldValueByFieldName(prefix + "Analysis", draft.getAnalysis());
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Double decimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String firstText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("CD15自动排程日志序列化失败", exception);
        }
    }
}