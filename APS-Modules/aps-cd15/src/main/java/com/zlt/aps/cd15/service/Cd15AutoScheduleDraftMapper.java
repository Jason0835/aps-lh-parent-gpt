package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleShiftSlotDraft;
import com.zlt.aps.cd15.engine.model.Cd15UnscheduledResultModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** 将Engine输出草稿转换为斜裁业务持久化实体。 */
@Component
public class Cd15AutoScheduleDraftMapper {

    /**
     * 转换排程主结果。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 批次号
     * @param orderNo 工单号
     * @param draft 输出草稿
     * @return 排程主结果实体
     */
    public Cd15ScheduleResult toScheduleResult(String factoryCode, LocalDate scheduleDate,
                                               String batchNo, String orderNo,
                                               Cd15ScheduleResultDraft draft) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setFactoryCode(factoryCode);
        result.setScheduleDate(date(scheduleDate));
        result.setCd15BatchNo(batchNo);
        result.setOrderNo(orderNo);
        result.setGroupNo(draft.getSplitGroupKey() == null ? null : orderNo);
        result.setSteelStripCode(draft.getSteelStripCode());
        result.setMaterialKey(draft.getMaterialKey());
        result.setCraftWidth(draft.getCraftWidth());
        result.setUnitConsumeMillimeter(draft.getUnitConsumeMillimeter());
        result.setCurlLength(draft.getCurlLength());
        result.setCordWidth(draft.getCordWidth());
        result.setBigRollConsumeQty(draft.getBigRollConsumeQuantity());
        result.setCxBatchNo(draft.getCxBatchNo());
        result.setCxMachineCodes(draft.getCxMachineCodes());
        result.setPlanSurplusQty(draft.getPlanSurplusQty());
        result.setBigRollCode(draft.getBigRollCode());
        result.setCuttingAngle(draft.getCuttingAngle());
        result.setCutMode(draft.getCutMode());
        result.setMachineCode(draft.getMachineCode());
        result.setStorageLaneCode(draft.getPrimaryLaneCode());
        result.setSourceType("AUTO");
        result.setReleaseStatus("0");
        result.setPublishSuccessCount(0);
        result.setIsLocked("0");
        if (draft.getShiftSlots() != null) {
            draft.getShiftSlots().forEach(slot -> applySlot(result, slot));
        }
        return result;
    }

    /** 将未排内存模型原样转换为持久化实体。 */
    public Cd15UnscheduleResult toUnscheduleResult(String factoryCode, Date scheduleDate,
                                                   String batchNo,
                                                   Cd15UnscheduledResultModel source) {
        Cd15UnscheduleResult result = new Cd15UnscheduleResult();
        result.setFactoryCode(factoryCode);
        result.setScheduleDate(scheduleDate);
        result.setBatchNo(batchNo);
        result.setSteelStripCode(source.getSteelStripCode());
        result.setBigRollCode(source.getBigRollCode());
        result.setCuttingAngle(source.getCuttingAngle());
        result.setDemandQty(source.getDemandQuantity());
        result.setScheduledQty(source.getScheduledQuantity());
        result.setUnscheduledQty(source.getUnscheduledQuantity());
        result.setFailStage(source.getFailStage());
        result.setUnscheduleReasonCode(source.getReasonCode());
        result.setReasonOrder(source.getReasonOrder());
        result.setPrimaryReason(source.isPrimaryReason() ? "1" : "0");
        result.setUnscheduledReason(source.getReasonDescription());
        result.setCandidateMachineCodes(source.getCandidateMachineCodes());
        result.setDataSource("0");
        return result;
    }

    private void applySlot(Cd15ScheduleResult result, Cd15ScheduleShiftSlotDraft slot) {
        Date scheduleDate = date(slot.getScheduleDate());
        Double plan = decimal(slot.getPlanQuantity());
        Double finish = decimal(slot.getFinishQuantity());
        Double rate = decimal(slot.getFinishRate());
        switch (slot.getClassField()) {
            case "CLASS1": result.setClass1ScheduleDate(scheduleDate); result.setClass1PlanQty(plan); result.setClass1FinishQty(finish); result.setClass1ProduceOrder(slot.getProduceOrder()); result.setClass1FinishRate(rate); result.setClass1Analysis(slot.getAnalysis()); break;
            case "CLASS2": result.setClass2ScheduleDate(scheduleDate); result.setClass2PlanQty(plan); result.setClass2FinishQty(finish); result.setClass2ProduceOrder(slot.getProduceOrder()); result.setClass2FinishRate(rate); result.setClass2Analysis(slot.getAnalysis()); break;
            case "CLASS3": result.setClass3ScheduleDate(scheduleDate); result.setClass3PlanQty(plan); result.setClass3FinishQty(finish); result.setClass3ProduceOrder(slot.getProduceOrder()); result.setClass3FinishRate(rate); result.setClass3Analysis(slot.getAnalysis()); break;
            case "CLASS4": result.setClass4ScheduleDate(scheduleDate); result.setClass4PlanQty(plan); result.setClass4FinishQty(finish); result.setClass4ProduceOrder(slot.getProduceOrder()); result.setClass4FinishRate(rate); result.setClass4Analysis(slot.getAnalysis()); break;
            case "CLASS5": result.setClass5ScheduleDate(scheduleDate); result.setClass5PlanQty(plan); result.setClass5FinishQty(finish); result.setClass5ProduceOrder(slot.getProduceOrder()); result.setClass5FinishRate(rate); result.setClass5Analysis(slot.getAnalysis()); break;
            case "CLASS6": result.setClass6ScheduleDate(scheduleDate); result.setClass6PlanQty(plan); result.setClass6FinishQty(finish); result.setClass6ProduceOrder(slot.getProduceOrder()); result.setClass6FinishRate(rate); result.setClass6Analysis(slot.getAnalysis()); break;
            case "CLASS7": result.setClass7ScheduleDate(scheduleDate); result.setClass7PlanQty(plan); result.setClass7FinishQty(finish); result.setClass7ProduceOrder(slot.getProduceOrder()); result.setClass7FinishRate(rate); result.setClass7Analysis(slot.getAnalysis()); break;
            case "CLASS8": result.setClass8ScheduleDate(scheduleDate); result.setClass8PlanQty(plan); result.setClass8FinishQty(finish); result.setClass8ProduceOrder(slot.getProduceOrder()); result.setClass8FinishRate(rate); result.setClass8Analysis(slot.getAnalysis()); break;
            default: throw new IllegalArgumentException("不支持的斜裁班次字段: " + slot.getClassField());
        }
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Double decimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
