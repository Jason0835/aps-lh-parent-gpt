package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 月计划定稿表For调整
 *
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
     * 搭配开始日期
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "搭配开始日期", name = "matchBeginDay")
    private Integer matchBeginDay;

    /**
     * 搭配结束日期
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "搭配结束日期", name = "matchBeginDay")
    private Integer matchEndDay;

    /**
     * OEM标识
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "OEM标识", name = "oemFlag")
    private String oemFlag;

    /**
     * 物料优先
     */
    @ApiModelProperty(value = "物料优先")
    @TableField(exist = false)
    private String scmPriority;

    /**
     * 结构优先
     */
    @ApiModelProperty(value = "结构优先")
    @TableField(exist = false)
    private String structurePriority;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量")
    @TableField(exist = false)
    private Integer stockQty;

    /**
     * 调整优先级
     */
    @ApiModelProperty(value = "调整优先级")
    @TableField(exist = false)
    private Integer adjustPriority;

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
    @ApiModelProperty(value = "调整明细ID", name = "adjustDetailId")
    @TableField(exist = false)
    private String adjustDetailId;

    /**
     * 排产净需求
     */
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    @TableField(exist = false)
    private Integer netQty;

    /**
     * 高优先级
     */
    @ApiModelProperty(value = "高优先级", name = "heightQty")
    @TableField(exist = false)
    private Integer heightQty;

    /**
     * 中优先级
     */
    @ApiModelProperty(value = "中优先级", name = "midQty")
    @TableField(exist = false)
    private Integer midQty;

    /**
     * 周期排产储备
     */
    @ApiModelProperty(value = "周期排产储备", name = "cycleReserveQty")
    @TableField(exist = false)
    private Integer cycleReserveQty;

    /**
     * 常规储备
     */
    @ApiModelProperty(value = "常规储备", name = "conventionReserveQty")
    @TableField(exist = false)
    private Integer conventionReserveQty;

    /**
     * 暂缓订单
     */
    @ApiModelProperty(value = "暂缓订单", name = "POSTPONE_QTY")
    @TableField(exist = false)
    private Integer postponeQty;

    /**
     * 是否锁定上机日期：0-否，1-是
     */
    @ApiModelProperty(value = "是否锁定上机日期：0-否，1-是", name = "isLockSchedule")
    @TableField(exist = false)
    private String isLockSchedule;

    /**
     * 版本规则：ADJ+年月日+3位流水号；
     */
    @ApiModelProperty(value = "版本规则：ADJ+年月日+3位流水号；", name = "version")
    @TableField(value = "VERSION")
    private String version;

    /**
     * 调整标识，0-没有调整,1-有调整
     */
    @ApiModelProperty(value = "调整标识", name = "adjustFlag")
    @TableField(exist = false)
    private String adjustFlag;

    /**
     * 待调整量（净需求 - 生产余量[上个月生产余量+本月生产余量]）
     */
    @ApiModelProperty(value = "待调整量", name = "pendingQty")
    @TableField(exist = false)
    private Integer pendingQty;

    /**
     * 生产实际排产量合计,
     */
    @ApiModelProperty(value = "生产实际排产量合计", name = "sumTotalQty")
    @TableField(exist = false)
    private Integer sumTotalQty;

    /**
     * 当前调整版本,
     */
    @ApiModelProperty(value = "当前调整版本", name = "currentAdjustVersion")
    @TableField(exist = false)
    private String currentAdjustVersion;
    /**
     * 模壳标准,
     */
    @ApiModelProperty(value = "模壳标准", name = "mouldShell")
    @TableField(exist = false)
    private String mouldShell;
    /**
     * 每日模具数
     */
    private Integer mouldQtyDay1;
    private Integer mouldQtyDay2;
    private Integer mouldQtyDay3;
    private Integer mouldQtyDay4;
    private Integer mouldQtyDay5;
    private Integer mouldQtyDay6;
    private Integer mouldQtyDay7;
    private Integer mouldQtyDay8;
    private Integer mouldQtyDay9;
    private Integer mouldQtyDay10;
    private Integer mouldQtyDay11;
    private Integer mouldQtyDay12;
    private Integer mouldQtyDay13;
    private Integer mouldQtyDay14;
    private Integer mouldQtyDay15;
    private Integer mouldQtyDay16;
    private Integer mouldQtyDay17;
    private Integer mouldQtyDay18;
    private Integer mouldQtyDay19;
    private Integer mouldQtyDay20;
    private Integer mouldQtyDay21;
    private Integer mouldQtyDay22;
    private Integer mouldQtyDay23;
    private Integer mouldQtyDay24;
    private Integer mouldQtyDay25;
    private Integer mouldQtyDay26;
    private Integer mouldQtyDay27;
    private Integer mouldQtyDay28;
    private Integer mouldQtyDay29;
    private Integer mouldQtyDay30;
    private Integer mouldQtyDay31;



    /**
     * 获取待调整量的key
     *
     * @return
     */
    public String getPendingQtyKey() {
        String groupKeyFormat = "%s|*|%s";
        return String.format(groupKeyFormat, getMaterialDesc(), getConstructionStage());
    }
}
