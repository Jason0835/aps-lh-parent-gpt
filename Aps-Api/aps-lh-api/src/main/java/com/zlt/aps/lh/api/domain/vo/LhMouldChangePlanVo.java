package com.zlt.aps.lh.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Copyright (c) 2024, All rights reserved。
 * 文件名称：LhMouldChangePlanVo.java
 * 描    述：
 *
 * @author cxy
 * @version 1.0
 * @date 2026/5/6
 */
@ApiModel(value = "模具交替计划导入导出实体", description = "模具交替计划导入导出实体")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_lh_mould_change_plan")
public class LhMouldChangePlanVo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "序号")
    @Excel(name = "ui.data.column.facMonthPlanProdResult.planSeq", cellType = Excel.ColumnType.NUMERIC)
    @TableField(exist = false)
    private Integer seq;

    @ApiModelProperty(value = "计划日期")
    @Excel(name = "ui.data.column.lhMouldChangePlan.planDate", dateFormat = "yyyy-MM-dd")
    @ImportExcelValidated(required = true)
    @TableField(value = "PLAN_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date planDate;

    @ApiModelProperty(value = "班次")
    @Excel(name = "ui.data.column.lhMouldChangePlan.classIndex", dictType = "class_num_two_mm")
    @TableField(value = "CLASS_INDEX")
    private String classIndex;

    @ApiModelProperty(value = "硫化机台编号")
    @Excel(name = "ui.data.column.lhMouldChangePlan.lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "硫化机台名称")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    @ApiModelProperty(value = "计划顺位")
    @Excel(name = "ui.data.column.lhMouldChangePlan.planOrder", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "PLAN_ORDER")
    private Integer planOrder;

    @ApiModelProperty(value = "左右模 L-左模；R-右模；LR-左右模")
    @Excel(name = "ui.data.column.lhMouldChangePlan.leftRightMould", dictType = "lr_molds")
    @TableField(value = "LEFT_RIGHT_MOULD")
    private String leftRightMould;

    @ApiModelProperty(value = "前规格物料编码")
    @Excel(name = "ui.data.column.lhMouldChangePlan.beforeMaterialCode")
    @TableField(value = "BEFORE_MATERIAL_CODE")
    private String beforeMaterialCode;

    @ApiModelProperty(value = "前规格物料描述")
    @Excel(name = "ui.data.column.lhMouldChangePlan.beforeMaterialDesc")
    @TableField(value = "BEFORE_MATERIAL_DESC")
    private String beforeMaterialDesc;

    @ApiModelProperty(value = "后规格物料编码")
    @Excel(name = "ui.data.column.lhMouldChangePlan.afterMaterialCode")
    @TableField(value = "AFTER_MATERIAL_CODE")
    private String afterMaterialCode;

    @ApiModelProperty(value = "后规格物料描述")
    @Excel(name = "ui.data.column.lhMouldChangePlan.afterMaterialDesc")
    @TableField(value = "AFTER_MATERIAL_DESC")
    private String afterMaterialDesc;

    @ApiModelProperty(value = "交替类型 数据字典：CHANGE_MOULD_TYPE")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.changeMouldType", dictType = "CHANGE_MOULD_TYPE", width = 25)
    @TableField(value = "CHANGE_MOULD_TYPE")
    private String changeMouldType;

    /**
     * 收尾类型（0-正常 1-收尾）
     */
    @ApiModelProperty(value = "收尾类型", name = "endType", notes = "0-正常 1-收尾")
    @TableField(value = "END_TYPE")
    private String endType;

    @ApiModelProperty(value = "换模类型")
    @Excel(name = "ui.data.column.lhMouldChangePlan.changeType", width = 25)
    @TableField(exist = false)
    private String changeType;

    @ApiModelProperty(value = "是否干冰清洗")
    @Excel(name = "ui.data.column.lhMouldChangePlan.changeMouldType", dictType = "biz_yes_no")
    private Integer isDryIceClean;

    @ApiModelProperty(value = "是否喷砂清洗")
    @Excel(name = "ui.data.column.lhMouldChangePlan.changeMouldType", dictType = "biz_yes_no")
    private Integer isSandblastingClean;

    @ApiModelProperty(value = "是否换活字块")
    @Excel(name = "ui.data.column.lhMouldChangePlan.changeMouldType", dictType = "biz_yes_no")
    private Integer isReplaceBlock;

    @ApiModelProperty(value = "模具号")
    @Excel(name = "ui.data.column.lhMouldChangePlan.mouldCode")
    @ImportExcelValidated(required = true, maxLength = 250)
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    @ApiModelProperty(value = "备注说明字段")
    @Excel(name = "ui.data.column.lhMouldChangePlan.remark", width = 50)
    @TableField(value = "REMARK")
    private String remark;

}
