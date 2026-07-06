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
}
