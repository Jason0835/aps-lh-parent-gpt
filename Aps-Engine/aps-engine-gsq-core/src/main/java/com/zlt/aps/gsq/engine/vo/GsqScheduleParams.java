package com.zlt.aps.gsq.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 钢丝圈排产参数
 *
 */
@Data
public class GsqScheduleParams {
    /**
     * 仅投产阶段规格排产标识
     */
    private String productionStage;
    /**
     * 工装容量
     */
    private Double toolCapacity;
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
     * 库存预留系数
     */
    private BigDecimal stockRatio;
    /**
     * 强制排在夜班的规格
     */
    private String[] midSpec;
    /**
     * 强制排在早班的规格
     */
    private String[] nightSpec;
    
    /**
     * 供应时长
     */
    private Double supplyTime;
    
    /**
     * 均分阈值
     */
    private BigDecimal equalShareThreshold;
}
