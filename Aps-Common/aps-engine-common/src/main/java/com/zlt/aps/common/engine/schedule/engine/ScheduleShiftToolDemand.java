package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/**
 * TM/TC 班次级成型需求工装快照。
 *
 * <p>对象在机台分配开始前创建，创建后不再修改，供班次范围解析和班次计划量计算前的工装释放使用。</p>
 */
public final class ScheduleShiftToolDemand {

    /** 实际排程班次。 */
    private final Integer shiftOrder;

    /** 产品编码。 */
    private final String productCode;

    /** 对应的成型代码，取任务的 embryoCode。 */
    private final String formingCode;

    /** 成型当班需求量，单位米。 */
    private final BigDecimal formingDemandQty;

    /** 有效卷曲长度，单位米/套。 */
    private final BigDecimal curlRollLength;

    /**
     * 创建班次级成型需求快照。
     *
     * @param shiftOrder 班次顺序
     * @param productCode 产品编码
     * @param formingDemandQty 成型当班需求量
     * @param curlRollLength 有效卷曲长度
     */
    public ScheduleShiftToolDemand(Integer shiftOrder, String productCode,
                                   BigDecimal formingDemandQty, BigDecimal curlRollLength) {
        this(shiftOrder, productCode, null, formingDemandQty, curlRollLength);
    }

    /**
     * 创建带成型代码的班次级成型需求工装快照。
     *
     * @param shiftOrder 班次顺序
     * @param productCode 产品编码
     * @param formingCode 成型代码
     * @param formingDemandQty 成型当班需求量
     * @param curlRollLength 有效卷曲长度
     */
    public ScheduleShiftToolDemand(Integer shiftOrder, String productCode, String formingCode,
                                   BigDecimal formingDemandQty, BigDecimal curlRollLength) {
        this.shiftOrder = shiftOrder;
        this.productCode = productCode;
        this.formingCode = formingCode;
        this.formingDemandQty = formingDemandQty;
        this.curlRollLength = curlRollLength;
    }

    /** @return 实际排程班次 */
    public Integer getShiftOrder() {
        return this.shiftOrder;
    }

    /** @return 产品编码 */
    public String getProductCode() {
        return this.productCode;
    }

    /**
     * 获取成型代码。
     *
     * @return 成型代码；未提供时返回 null
     */
    public String getFormingCode() {
        return this.formingCode;
    }

    /** @return 成型当班需求量 */
    public BigDecimal getFormingDemandQty() {
        return this.formingDemandQty;
    }

    /** @return 有效卷曲长度 */
    public BigDecimal getCurlRollLength() {
        return this.curlRollLength;
    }
}
