package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleResultDraft;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleShiftSlotDraft;
import com.zlt.aps.cd90.engine.model.Cd90UnscheduledResultModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** 将Engine输出草稿转换为直裁业务持久化实体。 */
@Component
public class Cd90AutoScheduleDraftMapper {

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
    public Cd90ScheduleResult toScheduleResult(String factoryCode, LocalDate scheduleDate,
                                               String batchNo, String orderNo,
                                               Cd90ScheduleResultDraft draft) {
        Cd90ScheduleResult result = new Cd90ScheduleResult();
        result.setFactoryCode(factoryCode);
        result.setScheduleDate(date(scheduleDate));
        result.setBatchNo(batchNo);
        result.setOrderNo(orderNo);
        result.setClothCode(draft.getClothCode());
        result.setBigRollCode(draft.getBigRollCode());
        result.setMachineCode(draft.getMachineCode());
        result.setStorageLaneCode(draft.getPrimaryLaneCode());
        result.setDataSource(draft.getDataSource());
        result.setIsRelease("0");
        result.setPublishSuccessCount(0);
        result.setIsLocked(0);
        if (draft.getShiftSlots() != null) {
            draft.getShiftSlots().forEach(slot -> applySlot(result, slot));
        }
        return result;
    }

    /** 将未排内存模型原样转换为持久化实体。 */
    public Cd90UnscheduleResult toUnscheduleResult(String factoryCode, Date scheduleDate,
                                                   String batchNo,
                                                   Cd90UnscheduledResultModel source) {
        Cd90UnscheduleResult result = new Cd90UnscheduleResult();
        result.setFactoryCode(factoryCode);
        result.setScheduleDate(scheduleDate);
        result.setBatchNo(batchNo);
        result.setClothCode(source.getClothCode());
        result.setBigRollCode(source.getBigRollCode());
        result.setDemandQty(decimal(source.getDemandQuantity()));
        result.setScheduledQty(decimal(source.getScheduledQuantity()));
        result.setUnscheduledQty(decimal(source.getUnscheduledQuantity()));
        result.setFailStage(source.getFailStage());
        result.setReasonCode(source.getReasonCode());
        result.setReasonOrder(source.getReasonOrder());
        result.setPrimaryReason(source.isPrimaryReason() ? "1" : "0");
        result.setUnscheduledReason(source.getReasonDescription());
        result.setCandidateMachineCodes(source.getCandidateMachineCodes());
        result.setDataSource("0");
        return result;
    }

    private void applySlot(Cd90ScheduleResult result, Cd90ScheduleShiftSlotDraft slot) {
        Date scheduleDate = date(slot.getScheduleDate());
        Double plan = decimal(slot.getPlanQuantity());
        Double finish = decimal(slot.getFinishQuantity());
        Double rate = decimal(slot.getFinishRate());
        switch (slot.getClassField()) {
            case "CLASS1": result.setClass1ScheduleDate(scheduleDate); result.setClass1PlanQty(plan); result.setClass1FinishQty(finish); result.setClass1ProduceOrder(slot.getProduceOrder()); result.setClass1FinishRate(rate); break;
            case "CLASS2": result.setClass2ScheduleDate(scheduleDate); result.setClass2PlanQty(plan); result.setClass2FinishQty(finish); result.setClass2ProduceOrder(slot.getProduceOrder()); result.setClass2FinishRate(rate); break;
            case "CLASS3": result.setClass3ScheduleDate(scheduleDate); result.setClass3PlanQty(plan); result.setClass3FinishQty(finish); result.setClass3ProduceOrder(slot.getProduceOrder()); result.setClass3FinishRate(rate); break;
            case "CLASS4": result.setClass4ScheduleDate(scheduleDate); result.setClass4PlanQty(plan); result.setClass4FinishQty(finish); result.setClass4ProduceOrder(slot.getProduceOrder()); result.setClass4FinishRate(rate); break;
            case "CLASS5": result.setClass5ScheduleDate(scheduleDate); result.setClass5PlanQty(plan); result.setClass5FinishQty(finish); result.setClass5ProduceOrder(slot.getProduceOrder()); result.setClass5FinishRate(rate); break;
            case "CLASS6": result.setClass6ScheduleDate(scheduleDate); result.setClass6PlanQty(plan); result.setClass6FinishQty(finish); result.setClass6ProduceOrder(slot.getProduceOrder()); result.setClass6FinishRate(rate); break;
            case "CLASS7": result.setClass7ScheduleDate(scheduleDate); result.setClass7PlanQty(plan); result.setClass7FinishQty(finish); result.setClass7ProduceOrder(slot.getProduceOrder()); result.setClass7FinishRate(rate); break;
            case "CLASS8": result.setClass8ScheduleDate(scheduleDate); result.setClass8PlanQty(plan); result.setClass8FinishQty(finish); result.setClass8ProduceOrder(slot.getProduceOrder()); result.setClass8FinishRate(rate); break;
            default: throw new IllegalArgumentException("不支持的直裁班次字段: " + slot.getClassField());
        }
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Double decimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
