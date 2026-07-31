package com.zlt.aps.tq.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 胎圈排产参数
 *
 */
@Data
public class TqScheduleParams {
    /**
     * 工装容量
     */
    private Double toolCapacity;

    /**
     * 工装车总数（全局统一值，用于排程时计算可用工装数量限制）
     * 可用工装 = 工装车总数 - 库存占用工装（库存量/整车容量向上取整）
     */
    private Integer toolingTotal;
    /**
     * 损耗率
     */
    private Double lossRate;
    /**
     * 往前一班合并计划量阈值
     */
    private Double mergeThreshold;
    /**
     * 预生产库存天数
     */
    private Double productStockDay;
    /**
     * 库存损耗率
     */
    private Double stockLossRate;
    /**
     * 大需求量阈值
     */
    private Double largeDemand;
    /**
     * 大尺寸规格阈值
     */
    private BigDecimal bigSizeSpec;
    /**
     * 收尾提醒数量
     */
    private Double closeOutNum;
    /**
     * 单最少排产量
     */
    private Double minPlanQty;
    /**
     * 均分阈值
     */
    private BigDecimal equalShareThreshold;
    /**
     * 交接班库存基准值
     */
    private Double classStockReference;

    /**
     * 最低排产量
     */
    private BigDecimal oneRollNum;

    /**
     * 备库班数（保证成型的班次排产数），默认1
     */
    private Double backupShiftCount;

    /**
     * 胎圈需求系数（胎圈消耗量=成型需求量×系数），默认2
     */
    private Double demandCoefficient;

    /**
     * 需求算法模式：1=算法1(三班最大值×系数，线下手工排产)，2=算法2(逐班对应×系数，系统算法)，默认2
     */
    private Integer demandCalcMode;

    /**
     * 库存供应时长阈值（小时），达到后切换规格生产下一个，默认24
     */
    private Double supplyTimeThreshold;

    /**
     * 班产上限，默认3000
     */
    private Double maxClassOutput;

    /**
     * 胎圈规格班次最大班产阈值（SYS1101029）
     * <p>多规格模式下，触发备库的胎圈当班初始排产上限，默认1000</p>
     * <p>单一规格机台不受此阈值限制，仅受机台定额（quota）限制</p>
     */
    private Double backupShiftThreshold;

    /**
     * 取整合并阈值（SYS1101030）
     * <p>备库分摊时，当剩余排产量小于此阈值时，合并到当前班次排完，不再新开一班向上取整</p>
     * <p>避免为少量尾数新开一班导致排产量虚增（如剩余9.6却向上取整到500）</p>
     * <p>默认0表示不启用合并，保持原有向上取整逻辑</p>
     */
    private Double roundingMergeThreshold;

    /**
     * 机台定额超排容忍阈值（SYS1101031）
     * <p>S3阶段机台分配时，当计划量超出机台剩余产能，且超出部分≤此值时，允许当班超排（突破机台定额），不延后到下一班</p>
     * <p>避免尾数被延后到下一班单独排产，降低生产效率</p>
     * <p>默认0表示不启用超排容忍，保持原有机台定额限制</p>
     */
    private Double machineOverAssignTolerance;

    /**
     * 规格切换时长（小时），默认0.5
     */
    private Double specSwitchTime;

    /**
     * 英寸切换时长（小时），默认1
     */
    private Double inchSwitchTime;

    /**
     * 成型停产天数触发开产阈值，默认2天
     */
    private Double stopIntersectionDays;

    /**
     * 开产库存补量预值
     */
    private Double reopenStockThreshold;

    /**
     * 成型停产后胎圈预排班数
     */
    private Double moldingStopPreShiftCount;

    /**
     * 单班时长（小时），默认8
     */
    private Double classHours;

    /**
     * 三角胶切换时长（小时），默认0.8
     */
    private Double apexSwitchTime;

    /**
     * 库消比高阈值，默认2.0
     */
    private Double stockConsumeRatioHigh;

    /**
     * 库消比低阈值，默认0.5
     */
    private Double stockConsumeRatioLow;

    // ==================== 策略可插拔参数（Phase 1 新增） ====================

    /**
     * 供应时长策略编码。
     *
     * <p>可选值：BY_STOCK（算法1-线下手工排产）、BY_SHIFT（算法2-系统算法）。</p>
     * <p>为空时按旧参数 {@code demandCalcMode} 兼容路由：demandCalcMode=1 → BY_STOCK，否则 → BY_SHIFT。</p>
     */
    private String supplyTimeStrategyCode;

    /**
     * 需求量策略编码。
     *
     * <p>可选值：DEFAULT（默认收尾判断算法）。</p>
     * <p>为空时默认 DEFAULT。</p>
     */
    private String demandQtyStrategyCode;

    /**
     * 计划量策略编码。
     *
     * <p>可选值：DEFAULT（默认 6 班滚动计算+备库分摊）。</p>
     * <p>为空时默认 DEFAULT。</p>
     */
    private String planQtyStrategyCode;
}
