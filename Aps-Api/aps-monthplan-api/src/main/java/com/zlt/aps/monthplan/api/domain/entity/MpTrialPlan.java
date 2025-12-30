package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpTrialPlan.java
 * 描    述：试制量试计划对象 t_mp_trial_plan
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@ApiModel(value = "试制量试计划对象", description = "试制量试计划对象 ")
@Data
@TableName(value = "T_MP_TRIAL_PLAN")
public class MpTrialPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mpTrialPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true, min = 1, max = 9999)
    @Excel(name = "ui.data.column.mpTrialPlan.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true, min = 1, max = 12)
    @Excel(name = "ui.data.column.mpTrialPlan.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.mdmMaterialInfo.materialCode")
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @ImportExcelValidated(maxLength = 256)
    @Excel(name = "ui.data.column.mdmMaterialInfo.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 分类 数据字典 biz_trial_type 01 新产品 02 产品改善
     */
    @Excel(name = "ui.data.column.mpTrialPlan.trialType", dictType = "biz_trial_type")
    @ApiModelProperty(value = "分类 数据字典 biz_trial_type 01 新产品 02 产品改善", name = "trialType")
    @TableField(value = "TRIAL_TYPE")
    private String trialType;

    /**
     * 目的
     */
    @ImportExcelValidated(maxLength = 200)
    @Excel(name = "ui.data.column.mpTrialPlan.destination")
    @ApiModelProperty(value = "目的", name = "destination")
    @TableField(value = "DESTINATION")
    private String destination;

    /**
     * 规格
     */
    @ImportExcelValidated(maxLength = 64)
    @Excel(name = "ui.data.column.mpTrialPlan.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @ImportExcelValidated(maxLength = 64)
    @Excel(name = "ui.data.column.mpTrialPlan.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 产品状态 数据字典 biz_construction_stage 2 试制 3 量试
     */
    @Excel(name = "ui.data.column.mpTrialPlan.trialStatus", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "产品状态 数据字典 biz_construction_stage 03 试制 04 量试", name = "trialStatus")
    @TableField(value = "TRIAL_STATUS")
    private String trialStatus;

    /**
     * 数量
     */
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @Excel(name = "ui.data.column.mpTrialPlan.trialQty")
    @ApiModelProperty(value = "数量", name = "trialQty")
    @TableField(value = "TRIAL_QTY")
    private Integer trialQty;

    /**
     * 紧急程度 数据字典 biz_urgency_type 01 紧急 04 普通
     */
    @Excel(name = "ui.data.column.mpTrialPlan.urgencyType", dictType = "biz_urgency_type")
    @ApiModelProperty(value = "紧急程度 数据字典 biz_urgency_type 01 紧急 04 普通", name = "urgencyType")
    @TableField(value = "URGENCY_TYPE")
    private String urgencyType;

    /**
     * 计划时间
     */
    @ImportExcelValidated(date = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpTrialPlan.planDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划时间", name = "planDate")
    @TableField(value = "PLAN_DATE")
    private Date planDate;

    /**
     * 系统排产日期
     */
    @ImportExcelValidated(date = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpTrialPlan.productionDate", width = 30, dateFormat = "yyyy-MM-dd", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "系统排产日期", name = "productionDate")
    @TableField(value = "PRODUCTION_DATE")
    private Date productionDate;

    /**
     * 完成时间
     */
    @ImportExcelValidated(date = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpTrialPlan.completeDate", width = 30, dateFormat = "yyyy-MM-dd", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "完成时间", name = "completeDate")
    @TableField(value = "COMPLETE_DATE")
    private Date completeDate;

    /**
     * 制造示方书号
     */
    @ImportExcelValidated(maxLength = 30)
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.embryoNo", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    /**
     * 制造示方
     */
    @ImportExcelValidated(maxLength = 30)
    @Excel(name = "ui.data.column.mpTrialPlan.madeInfo", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "制造示方", name = "madeInfo")
    @TableField(value = "MADE_INFO")
    private String madeInfo;

    /**
     * 文字示方
     */
    @ImportExcelValidated(maxLength = 30)
    @Excel(name = "ui.data.column.mpTrialPlan.moldingInfo", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

    /**
     * 硫化示方
     */
    @ImportExcelValidated(maxLength = 30)
    @Excel(name = "ui.data.column.mpTrialPlan.vulcanizationInfo", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(value = "LH_NO")
    private String lhNo;

    /**
     * 导入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpTrialPlan.importTime", width = 30, dateFormat = "yyyy-MM-dd", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "导入时间", name = "importTime")
    @TableField(value = "IMPORT_TIME")
    private Date importTime;

    /**
     * 部门
     */
//    @ImportExcelValidated(maxLength = 64)
//    @Excel(name = "ui.data.column.mpTrialPlan.deptName")
    @ApiModelProperty(value = "部门", name = "deptId")
    @TableField(value = "DEPT_Id")
    private Long deptId;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.mpTrialPlan.isImport", dictType = "biz_yes_no", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private String isImport;


    /**
     * 计划时间-开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划时间-开始时间", name = "planDateStartTime")
    @TableField(exist = false)
    private Date planDateStartTime;

    /**
     * 计划时间-结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划时间-结束时间", name = "planDateEndTime")
    @TableField(exist = false)
    private Date planDateEndTime;

    /**
     * 完成时间-开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "完成时间-开始时间", name = "completeDateStartTime")
    @TableField(exist = false)
    private Date completeDateStartTime;

    /**
     * 完成时间-结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "完成时间-结束时间", name = "completeDateEndTime")
    @TableField(exist = false)
    private Date completeDateEndTime;

    /**
     * 更新人名称
     */
    @ApiModelProperty(value = "更新人名称", name = "updateByName")
    @TableField(exist = false)
    private String updateByName;

    /**
     * 部门
     */
    @ImportExcelValidated(maxLength = 64)
    @Excel(name = "ui.data.column.mpTrialPlan.deptName", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "部门", name = "deptIdName")
    @TableField(exist = false)
    private String deptIdName;
}
