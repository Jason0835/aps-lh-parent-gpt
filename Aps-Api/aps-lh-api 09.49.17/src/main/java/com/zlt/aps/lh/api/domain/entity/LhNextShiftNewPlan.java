package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 硫化下一班次新计划实体。
 *
 * <p>对应表：T_LH_NEXT_SHIFT_NEW_PLAN。</p>
 * <p>业务说明：记录下一班次新增（提前生产）计划明细，按工厂 + 排程日期 + 批次 + 物料 + 机台维度存放班次计划量，
 * 供排程读取下一班次新增量时使用。</p>
 * <p>字段说明：ID、CREATE_BY、CREATE_TIME、UPDATE_BY、UPDATE_TIME、IS_DELETE、REMARK 由 BaseEntity 提供，本类不重复定义。</p>
 *
 * @author APS Team
 * @date 2026-09-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_LH_NEXT_SHIFT_NEW_PLAN")
@ApiModel(value = "硫化下一班次新计划", description = "硫化下一班次新计划表实体对象")
public class LhNextShiftNewPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 排程日期
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.scheduleDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 批次号
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.batchNo")
    @ApiModelProperty(value = "批次号")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.materialCode")
    @ApiModelProperty(value = "物料编码")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 产品状态/计划类型（与硫化主表 PRODUCT_STATUS 同口径，供成型余量账户匹配）
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.productStatus", dictType = "lh_trial_status")
    @ApiModelProperty(value = "产品状态/计划类型")
    @TableField(value = "PRODUCT_STATUS")
    private String productStatus;

    /**
     * 施工阶段：00 无工艺、01 试制、02 量试、03 正式
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段：00 无工艺、01 试制、02 量试、03 正式")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /**
     * 胎胚编码
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.embryoCode")
    @ApiModelProperty(value = "胎胚编码")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 结构名称（产品结构）
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.structureName")
    @ApiModelProperty(value = "结构名称（产品结构）")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 班次计划量
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.shiftPlanQty")
    @ApiModelProperty(value = "班次计划量")
    @TableField(value = "SHIFT_PLAN_QTY")
    private BigDecimal shiftPlanQty;

    /**
     * 硫化时长（与硫化主表 LH_TIME 同口径，供成型合成硫化行工艺参数使用）
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.lhTime")
    @ApiModelProperty(value = "硫化时长")
    @TableField(value = "LH_TIME")
    private Integer lhTime;

    /**
     * 使用模数（与硫化主表 MOULD_QTY 同口径，供成型按模数核算硫化机台数/单胎耗时）
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.mouldQty")
    @ApiModelProperty(value = "使用模数")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 单班硫化量（与硫化主表 SINGLE_MOULD_SHIFT_QTY 同口径）
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.singleMouldShiftQty")
    @ApiModelProperty(value = "单班硫化量")
    @TableField(value = "SINGLE_MOULD_SHIFT_QTY")
    private Integer singleMouldShiftQty;

    /**
     * 机台编码
     */
    @Excel(name = "ui.data.column.lhNextShiftNewPlan.machineCode")
    @ApiModelProperty(value = "机台编码")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;
}
