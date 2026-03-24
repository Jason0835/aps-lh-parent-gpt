package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import lombok.Data;

import java.util.Set;

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
     * 结构需求量最小排产天数，<该值则不进行结构排产
     */
    private Integer minProductionDays;
    /**
     * 结构最低上机天数
     */
    private Integer minAllocationDays;
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
     * SKU二次上机
     */
    private Integer skuSecondProduction;
    /**
     * 可补量的排产分类集合
     */
    private Set<String> boostProductionType;
    /**
     * 主销产品或常规产品临近结构收尾最大可补量天数
     */
    private Integer matchingBoostDay;
    /**
     * 主销产品或常规产品月底最大可补量天数
     */
    private Integer maxBoostDay;

    /**
     * 每日最大产能
     */
    private Integer dayMaxCapacity;
    /**
     * 每日最小产能
     */
    private Integer dayMinCapacity;
    /**
     * 每日单台成型最大胎胚种类数
     */
    private Integer singleCxEmbryoCodeCount;
    /**
     * 每日最大切换分组次数(TBR结构)
     */
    private Integer dayChangeGroupCount;
    /**
     * 每日换模能力(硫化机台数)
     */
    private Integer changeMouldLhMachineNumber;
    /**
     * 换模首日排产量
     */
    private Integer changeMouldFirstQty;
    /**
     * 换活字块-收尾量与日硫化量差量
     */
    private Integer changeTypeBlockQtyDiff;
    /**
     * 换活字块-收尾量与日硫化量小于等于差量,后SKU的首日排产量
     */
    private Integer changeTypeBlockQty;
    /**
     * 换活字块-收尾量与日硫化量大于差量,后SKU的首日排产量
     */
    private Integer changeTypeBlockMaxQty;
    /**
     * 最小批量值
     */
    private Integer minQty;

    /**
     * 进行降膜排产的条件，使用机台数超过该值
     * 默认为3台
     */
    private Integer deductMouldMinLhMachineCount;
    /**
     * 第一次 临近收尾天数判断
     * 默认为 7天
     */
    private Integer firstNearDeadLineDay;
    /**
     * 第一次 临近收尾天数，不能超过的机台数
     * 默认为 3台
     */
    private Integer firstNearDeadLineMaxLhMachineCount;
    /**
     * 第二次 临近收尾天数判断
     * 默认为 5天
     */
    private Integer secondNearDeadLineDay;
    /**
     * 第二次 临近收尾天数判断
     * 默认为 2台
     */
    private Integer secondNearDeadLineMaxLhMachineCount;
    /**
     * 最后一次 临近收尾天数判断
     * 默认为 2天
     */
    private Integer lastNearDeadLineDay;
    /**
     * 最后一次 临近收尾天数判断
     * 默认为 1台
     */
    private Integer lastNearDeadLineMaxLhMachineCount;
    /**
     * 单台成型机的月度生产计划锁定期天数
     * 默认为3天
     */
    private Integer singleCxMachineLockDay;
    /**
     * 多台成型机的月度生产计划锁定期天数
     * 默认为4天
     */
    private Integer multiCxMachineLockDays;
    /**
     * 周程滚动调整日
     */
    private String weekRollAdjustDate;
    /**
     * 切换结构首日需要扣减的硫化机台数
     */
    private Integer deductionLhMachineCount;

    /**
     * SYS0203011 外销贴牌-品牌配置
     */
    private Set<String> oemBrandConfig;

    /**
     * SYS0203012 外销贴牌-总产量配置，单位条
     */
    private Integer oemBrandCapacity;

    /**
     * SYS0203013 周期储备量占实单的比例(%),防止储备量过大
     */
    private Integer reservePercent;

    /**
     * 参与排产的特殊原材料编码信息
     */
    private Set<String> specialMaterialCodeSet;
}
