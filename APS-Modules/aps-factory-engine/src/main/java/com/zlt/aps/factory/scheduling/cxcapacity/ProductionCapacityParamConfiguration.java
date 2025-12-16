package com.zlt.aps.factory.scheduling.cxcapacity;

import lombok.Data;

/**
 * 排产产能控制参数配置
 *
 * @author ZLT
 * @date 20251216
 */
@Data
public class ProductionCapacityParamConfiguration {
    /**
     * 断面宽差值±
     */
    private Integer sectionWidthDiffValue;
    /**
     * 结构最少排产天数量
     */
    private Integer minProductionDays;
    /**
     * 结构最低上机天数
     */
    private Integer minAllocationDays;
    /**
     * 每日最大切换分组次数(TBR结构)
     */
    private Integer dayChangeGroupCount;
    /**
     * 非周期结构最低硫化配比则收尾
     */
    private Integer noCycleProductionMinLhMachineNumber;
    /**
     * 总净需求量
     */
    private Integer sumProductionQty;
    /**
     * 高优先级与总净需求量差值
     */
    private Integer heightDiffQty;
    /**
     * 每日换模能力(硫化机台数)
     */
    private Integer changeMouldLhMachineNumber;
    /**
     * 主销产品或常规产品月均补量值
     */
    private Integer boostAverageValue;
    /**
     * 换模首日排产量
     */
    private Integer changeMouldFirstQty;
    /**
     * 换活字块-收尾量与日硫化量差量
     */
    private Integer changeTypeBlockQtyDiff;

    /**
     * 换活字块-收尾量与日硫化量小于等于差量
     */
    private Integer changeTypeBlockQty;

    /**
     * 换活字块-收尾量与日硫化量大于差量
     */
    private Integer changeTypeBlockMaxQty;
}
