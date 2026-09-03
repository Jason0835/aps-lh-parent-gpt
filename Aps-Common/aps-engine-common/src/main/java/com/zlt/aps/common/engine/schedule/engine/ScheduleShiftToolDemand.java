package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /** 汇总前的成型需求明细，按原始任务顺序保存，单位米。 */
    private final List<BigDecimal> formingDemandQtyDetails;

    /** 有效卷曲长度，单位米/套。 */
    private final BigDecimal curlRollLength;

    /** 同一产品汇总时是否发现有效卷曲长度不一致。 */
    private final boolean curlRollLengthConflict;

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
        this(shiftOrder, productCode, null, formingDemandQty, curlRollLength,
                Collections.singletonList(formingDemandQty));
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
        this(shiftOrder, productCode, formingCode, formingDemandQty, curlRollLength,
                Collections.singletonList(formingDemandQty));
    }

    /**
     * 创建带成型需求明细的班次级成型需求工装快照。
     *
     * @param shiftOrder 班次顺序
     * @param productCode 产品编码
     * @param formingCode 成型代码，多个代码使用英文逗号分隔
     * @param formingDemandQty 汇总后的成型当班需求量
     * @param curlRollLength 产品有效卷曲长度
     * @param formingDemandQtyDetails 汇总前的成型需求明细
     */
    public ScheduleShiftToolDemand(Integer shiftOrder, String productCode, String formingCode,
                                   BigDecimal formingDemandQty, BigDecimal curlRollLength,
                                   List<BigDecimal> formingDemandQtyDetails) {
        this(shiftOrder, productCode, formingCode, formingDemandQty, curlRollLength,
                formingDemandQtyDetails, false);
    }

    /**
     * 创建带成型需求明细和卷曲长度冲突标记的班次级成型需求工装快照。
     *
     * @param shiftOrder 班次顺序
     * @param productCode 产品编码
     * @param formingCode 成型代码，多个代码使用英文逗号分隔
     * @param formingDemandQty 汇总后的成型当班需求量
     * @param curlRollLength 产品有效卷曲长度
     * @param formingDemandQtyDetails 汇总前的成型需求明细
     * @param curlRollLengthConflict 是否存在有效卷曲长度不一致
     */
    public ScheduleShiftToolDemand(Integer shiftOrder, String productCode, String formingCode,
                                   BigDecimal formingDemandQty, BigDecimal curlRollLength,
                                   List<BigDecimal> formingDemandQtyDetails,
                                   boolean curlRollLengthConflict) {
        this.shiftOrder = shiftOrder;
        this.productCode = productCode;
        this.formingCode = formingCode;
        this.formingDemandQty = formingDemandQty;
        this.curlRollLength = curlRollLength;
        this.formingDemandQtyDetails = formingDemandQtyDetails == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(formingDemandQtyDetails));
        this.curlRollLengthConflict = curlRollLengthConflict;
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

    /**
     * 获取汇总前的成型需求明细。
     *
     * @return 按原始任务顺序保存的成型需求明细
     */
    public List<BigDecimal> getFormingDemandQtyDetails() {
        return this.formingDemandQtyDetails;
    }

    /**
     * 获取有效卷曲长度冲突标记。
     *
     * @return 存在冲突返回 true，否则返回 false
     */
    public boolean isCurlRollLengthConflict() {
        return this.curlRollLengthConflict;
    }

    /** @return 有效卷曲长度 */
    public BigDecimal getCurlRollLength() {
        return this.curlRollLength;
    }
}
