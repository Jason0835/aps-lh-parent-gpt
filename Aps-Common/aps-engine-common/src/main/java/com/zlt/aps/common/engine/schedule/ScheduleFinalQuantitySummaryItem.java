package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;

/**
 * 按机台、产品和胎胚汇总的六班计划量结果。
 */
public class ScheduleFinalQuantitySummaryItem {

    /** 机台编码。 */
    private final String machineCode;

    /** 产品编码，TM 为胎面代码，TC 为胎侧代码。 */
    private final String productCode;

    /** 胎胚编码。 */
    private final String embryoCode;

    /** 六个班次的计划量。 */
    private final BigDecimal[] shiftQuantities;

    /** 总计划量。 */
    private BigDecimal totalQty = BigDecimal.ZERO;

    /**
     * 创建汇总项。
     *
     * @param machineCode 机台编码
     * @param productCode 产品编码
     * @param embryoCode 胎胚编码
     */
    public ScheduleFinalQuantitySummaryItem(String machineCode, String productCode, String embryoCode) {
        this.machineCode = machineCode;
        this.productCode = productCode;
        this.embryoCode = embryoCode;
        this.shiftQuantities = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    /**
     * 累加指定班次计划量。
     *
     * @param shiftOrder 班次顺序，范围 1-6
     * @param planQty 计划量
     */
    public void add(Integer shiftOrder, BigDecimal planQty) {
        if (shiftOrder == null || shiftOrder < 1 || shiftOrder > 6 || planQty == null) {
            return;
        }
        int index = shiftOrder - 1;
        this.shiftQuantities[index] = this.shiftQuantities[index].add(planQty);
        this.totalQty = this.totalQty.add(planQty);
    }

    /**
     * 获取机台编码。
     *
     * @return 机台编码
     */
    public String getMachineCode() {
        return this.machineCode;
    }

    /**
     * 获取产品编码。
     *
     * @return 产品编码
     */
    public String getProductCode() {
        return this.productCode;
    }

    /**
     * 获取胎胚编码。
     *
     * @return 胎胚编码
     */
    public String getEmbryoCode() {
        return this.embryoCode;
    }

    /**
     * 获取六班计划量的副本。
     *
     * @return 六班计划量
     */
    public BigDecimal[] getShiftQuantities() {
        return this.shiftQuantities.clone();
    }

    /**
     * 获取总计划量。
     *
     * @return 总计划量
     */
    public BigDecimal getTotalQty() {
        return this.totalQty;
    }
}
