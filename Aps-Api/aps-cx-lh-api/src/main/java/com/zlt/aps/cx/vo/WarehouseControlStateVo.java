package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 立库库容管控状态上下文
 *
 * <p>封装立库库容管控所需的所有参数、预计算数据和运行时追踪状态。
 * 核心设计：所有硫化机同时消耗胎胚库存，因此本班次总硫化消耗在班次开始时
 * 一次性预扣，后续仅累加成型产出，避免逐任务扣减导致的时序偏差。
 *
 * <p>通过 {@link #validate()} 方法进行数据完整性校验，
 * 确保核心管控逻辑执行前数据齐备；校验失败时 {@link #isEnabled()} 返回 false，
 * 立库管控自动降级跳过，不影响排程主流程。
 *
 * @author APS Team
 */
@Data
public class WarehouseControlStateVo {

    // ====== 参数配置项 ======

    /** 立库总库容（条），0 = 不启用管控 */
    private int warehouseCapacity;
    /** 库容预警比例（0~1） */
    private double warehouseCapacityRatio;
    /** 库容预警线 = warehouseCapacity × warehouseCapacityRatio */
    private int warehouseThreshold;
    /** 可供硫化时长封顶阈值（小时） */
    private int stockHoursCap;
    /** 可供硫化时长封顶开关 */
    private boolean stockHoursCapEnabled;

    // ====== 预计算数据项 ======

    /** 胎胚维度初始库存（embryoCode -> 库存条数） */
    private Map<String, Integer> embryoTotalStockMap;
    /** 胎胚维度本班次硫化消耗（embryoCode -> 消耗条数，预扣） */
    private Map<String, Integer> embryoShiftVulcConsumptionMap;
    /** 本班次总硫化消耗（所有胎胚合计，预扣） */
    private int totalShiftVulcConsumption;

    // ====== 运行时追踪 ======

    /** 胎胚维度累计成型产出（embryoCode -> 产出条数） */
    private Map<String, Integer> shiftFormingOutputMap;
    /** 胎胚维度累计硫化消耗（预填充，供每胎胚日志计算 projectedStock 使用） */
    private Map<String, Integer> shiftVulcanizingConsumptionMap;
    /** 立库预计总库存（运行总计 = 初始总库存 - 总硫化消耗 + 累计成型产出） */
    private int runningTotalProjectedStock;

    /**
     * 立库管控是否启用
     */
    public boolean isEnabled() {
        return warehouseCapacity > 0;
    }

    /**
     * 立库空间维度检查：预计总库存是否超过库容预警线
     *
     * @param projectedStock 预计总库存
     * @return true = 超限需封顶/跳过, false = 未超限或管控未启用
     */
    public boolean isSpaceExceeded(int projectedStock) {
        return isEnabled() && projectedStock >= warehouseThreshold;
    }

    /**
     * 数据完整性校验：确保立库管控所需的关键数据均已成功收集
     *
     * <p>校验范围：
     * <ul>
     *   <li>参数有效性：库容非负、比例在 [0,1] 范围、封顶阈值为正</li>
     *   <li>数据齐备性：启用管控时，胎胚库存和硫化消耗数据必须已收集</li>
     * </ul>
     *
     * @return 校验错误描述列表，空列表 = 校验通过
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (warehouseCapacity < 0) {
            errors.add("立库总库容为负数: " + warehouseCapacity);
        }
        if (warehouseCapacityRatio < 0 || warehouseCapacityRatio > 1) {
            errors.add("库容预警比例超出有效范围[0,1]: " + warehouseCapacityRatio);
        }
        if (stockHoursCapEnabled && stockHoursCap <= 0) {
            errors.add("可供硫化时长封顶阈值无效: " + stockHoursCap);
        }

        if (isEnabled()) {
            if (embryoTotalStockMap == null) {
                errors.add("胎胚库存数据(embryoTotalStockMap)未收集");
            }
            if (embryoShiftVulcConsumptionMap == null) {
                errors.add("胎胚硫化消耗数据(embryoShiftVulcConsumptionMap)未计算");
            }
        }

        return errors;
    }
}
