package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 保证范围逐班成型余量封顶日志快照。
 *
 * <p>保存计划量计算实际使用的逐班条数与长度，仅用于过程日志，不参与排程计算。</p>
 */
@Data
public class ScheduleGuardDemandCapLogSnapshot {

    /**
     * 封顶前逐班保证需求条数。
     */
    private Map<Integer, BigDecimal> rawFormingQtyByShift = new LinkedHashMap<>();
    /**
     * 封顶后逐班保证需求条数，包含零值班次。
     */
    private Map<Integer, BigDecimal> cappedFormingQtyByShift = new LinkedHashMap<>();
    /**
     * 各班计算时实际使用的产品长度，单位米/条。
     */
    private Map<Integer, BigDecimal> productLengthByShift = new LinkedHashMap<>();
    /**
     * 来源成型行原始成型余量，保留空值和负值。
     */
    private BigDecimal cxRemainQty;

    /**
     * 设置封顶前逐班条数并复制容器。
     *
     * @param rawFormingQtyByShift 封顶前逐班需求条数
     */
    public void setRawFormingQtyByShift(Map<Integer, BigDecimal> rawFormingQtyByShift) {
        this.rawFormingQtyByShift = rawFormingQtyByShift == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(rawFormingQtyByShift);
    }

    /**
     * 设置封顶后逐班条数并复制容器。
     *
     * @param cappedFormingQtyByShift 封顶后逐班需求条数
     */
    public void setCappedFormingQtyByShift(Map<Integer, BigDecimal> cappedFormingQtyByShift) {
        this.cappedFormingQtyByShift = cappedFormingQtyByShift == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(cappedFormingQtyByShift);
    }

    /**
     * 设置逐班长度并复制容器。
     *
     * @param productLengthByShift 各班实际使用长度，单位米/条
     */
    public void setProductLengthByShift(Map<Integer, BigDecimal> productLengthByShift) {
        this.productLengthByShift = productLengthByShift == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(productLengthByShift);
    }

    /**
     * 创建包含独立班次映射的快照副本。
     *
     * @return 当前快照副本
     */
    public ScheduleGuardDemandCapLogSnapshot copy() {
        ScheduleGuardDemandCapLogSnapshot copy = new ScheduleGuardDemandCapLogSnapshot();
        copy.setRawFormingQtyByShift(this.rawFormingQtyByShift);
        copy.setCappedFormingQtyByShift(this.cappedFormingQtyByShift);
        copy.setProductLengthByShift(this.productLengthByShift);
        copy.setCxRemainQty(this.cxRemainQty);
        return copy;
    }
}
