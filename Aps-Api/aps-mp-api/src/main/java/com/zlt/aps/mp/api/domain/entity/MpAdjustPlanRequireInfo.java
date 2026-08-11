package com.zlt.aps.mp.api.domain.entity;

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
 * 文件名称：MpAdjustPlanRequireInfo.java
 * 描    述：月度计划调整需求信息对象 t_mp_adjust_plan_require_info
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20260716
 */

@Data
@TableName(value = "T_MP_ADJUST_PLAN_REQUIRE_INFO")
@ApiModel(value = "计划调整需求信息对象", description = "计划调整需求信息对象")
public class MpAdjustPlanRequireInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编码
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, maxLength = 50, dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 内外销
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true, dictType = "biz_stor_type")
    @ApiModelProperty(value = "内外销", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 调整日期
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.adjustDate", dateFormat = "yyyy-MM-dd")
    @ImportExcelValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "调整日期", name = "adjustDate")
    @TableField(value = "ADJUST_DATE")
    private Date adjustDate;

    /**
     * 区域
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.area")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "区域", name = "area")
    @TableField(value = "AREA")
    private String area;
    /**
     * 计划调整类型 01-追加计划，02-调减计划，03-计划提前，04-计划延迟
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.planAdjustType", dictType = "biz_plan_adjust_type")
    @ImportExcelValidated(required = true, dictType = "biz_plan_adjust_type")
    @ApiModelProperty(value = "调整类型 01-追加计划，02-调减计划，03-计划提前，04-计划延迟", name = "planAdjustType")
    @TableField(value = "PLAN_ADJUST_TYPE")
    private String planAdjustType;

    /**
     * 调整原因：
     * 01-追加计划 新订单追加 在手订单未满足 补柜/补量追加 生产异常追加 试制量试追加 补充产能 按特殊材料收尾 成型/硫化不搭配 周期排产搭配
     * 02-调减计划 关单/改单调减 暂不发货调减 产能调配调减 储备排产调减 生产异常调减
     * 03-计划提前 发货需求提前 生产实际提前
     * 04-计划延迟 产能调配延迟 生产异常延迟
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.adjustReason", dictType = "biz_adjust_reason")
    @ImportExcelValidated(dictType = "biz_adjust_reason")
    @ApiModelProperty(value = "调整原因", name = "adjustReason")
    @TableField(value = "ADJUST_REASON")
    private String adjustReason;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 结构类型 01 周期结构 02 常规结构
     */
    @ApiModelProperty(value = "结构类型", name = "structureType")
    @TableField(value = "STRUCTURE_TYPE")
    private String structureType;

    /**
     * MES物料编码
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 本月计划产量
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.monthPlanQty")
    @ImportExcelValidated(number = true)
    @ApiModelProperty(value = "本月计划产量", name = "monthPlanQty")
    @TableField(value = "MONTH_PLAN_QTY")
    private Integer monthPlanQty;

    /**
     * 调整数量
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.adjustPlanQty")
    @ImportExcelValidated(number = true)
    @ApiModelProperty(value = "调整数量", name = "adjustPlanQty")
    @TableField(value = "ADJUST_PLAN_QTY")
    private Integer adjustPlanQty;

    /**
     * 调整后计划量
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.adjustFinalQty")
    @ImportExcelValidated(number = true)
    @ApiModelProperty(value = "调整后计划量", name = "adjustFinalQty")
    @TableField(value = "ADJUST_FINAL_QTY")
    private Integer adjustFinalQty;

    /**
     * 实际调整数量
     */
    @Excel(name = "ui.data.column.mpAdjustPlanInfo.realAdjustQty")
    @ImportExcelValidated(number = true)
    @ApiModelProperty(value = "实际调整数量", name = "realAdjustQty")
    @TableField(value = "REAL_ADJUST_QTY")
    private Integer realAdjustQty;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private String isImport;

    /**
     * 调整日期起（查询条件，非表字段）
     */
    @ApiModelProperty(value = "调整日期起（查询条件）", name = "adjustDateStart")
    @TableField(exist = false)
    private String adjustDateStart;

    /**
     * 调整日期止（查询条件，非表字段）
     */
    @ApiModelProperty(value = "调整日期止（查询条件）", name = "adjustDateEnd")
    @TableField(exist = false)
    private String adjustDateEnd;
}
