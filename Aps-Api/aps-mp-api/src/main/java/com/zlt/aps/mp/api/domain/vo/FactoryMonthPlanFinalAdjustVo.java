package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 月计划定稿表For调整
 * @author Sandy
 * @date 2025-12-22
 */
@Data
public class FactoryMonthPlanFinalAdjustVo extends FactoryMonthPlanProductionFinalResult {

    @ApiModelProperty(value = "是否含特殊材料", name = "hasSpecialMaterial")
    @TableField(exist = false)
    private String hasSpecialMaterial;

    /**
     * 锁定量
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "锁定量", name = "lockQty")
    private Integer lockQty;

    /**
     * 实际调整量
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "实际调整量", name = "actualAdjustQty")
    private Integer actualAdjustQty;

    /**
     * 锁定日前的计划量汇总值
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "锁定日前的计划量汇总值", name = "sumPlanQtyBeforeLockDay")
    private Integer sumPlanQtyBeforeLockDay;
    
    /**
     * 搭配量
     */
    private Integer matchQtyDay1;
    private Integer matchQtyDay2;
    private Integer matchQtyDay3;
    private Integer matchQtyDay4;
    private Integer matchQtyDay5;
    private Integer matchQtyDay6;
    private Integer matchQtyDay7;
    private Integer matchQtyDay8;
    private Integer matchQtyDay9;
    private Integer matchQtyDay10;
    private Integer matchQtyDay11;
    private Integer matchQtyDay12;
    private Integer matchQtyDay13;
    private Integer matchQtyDay14;
    private Integer matchQtyDay15;
    private Integer matchQtyDay16;
    private Integer matchQtyDay17;
    private Integer matchQtyDay18;
    private Integer matchQtyDay19;
    private Integer matchQtyDay20;
    private Integer matchQtyDay21;
    private Integer matchQtyDay22;
    private Integer matchQtyDay23;
    private Integer matchQtyDay24;
    private Integer matchQtyDay25;
    private Integer matchQtyDay26;
    private Integer matchQtyDay27;
    private Integer matchQtyDay28;
    private Integer matchQtyDay29;
    private Integer matchQtyDay30;
    private Integer matchQtyDay31;
    /**
     * 记录原始的总量
     */
    private Integer oriTotalQty;

    /**
     * 调整明细
     */
    private StringBuilder adjustDetail;

    /**
     * 已经移动的标志
     */
    private boolean moveFlag = false;

    /**
     * 调整明细ID
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "调整明细ID", name = "adjustDetailId")
    private String adjustDetailId;

}
