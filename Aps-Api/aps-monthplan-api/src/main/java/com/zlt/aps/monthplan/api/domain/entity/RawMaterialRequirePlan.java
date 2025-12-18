package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;
import java.util.Map;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawMaterialRequirePlan.java
 * 描    述：原材料需求计划对象 t_raw_material_require_plan
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "原材料需求计划对象", description = "原材料需求计划对象 ")
@Data
@TableName(value = "T_RAW_MATERIAL_REQUIRE_PLAN")
public class RawMaterialRequirePlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 所属年份 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.year")
    @ApiModelProperty(value = "所属年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 所属月份 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.month")
    @ApiModelProperty(value = "所属月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 材料代码 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.materialCode")
    @ApiModelProperty(value = "材料代码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 材料描述 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialDesc")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "材料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 物料类型            数据字典 biz_rawMaterial_type 01 常规产品 04 特殊材料            匹配特殊原材料，则 类型 = 04 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialType",dictType = "biz_rawMaterial_type")
    @ImportExcelValidated(required = true, dictType = "biz_rawMaterial_type")
    @ApiModelProperty(value = "物料类型 biz_rawMaterial_type")
    @TableField(value = "MATERIAL_TYPE")
    private String materialType;

    /** 当月需求量 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.curMonthQty")
    @ApiModelProperty(value = "当月需求量", name = "curMonthQty")
    @TableField(value = "CUR_MONTH_QTY")
    private BigDecimal curMonthQty;

    /** 当月EUDR */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.curMonthRudrQty")
    @ApiModelProperty(value = "当月EUDR", name = "curMonthRudrQty")
    @TableField(value = "CUR_MONTH_RUDR_QTY")
    private BigDecimal curMonthRudrQty;

    /** 次月需求量(T月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.tMonthQty")
    @ApiModelProperty(value = "次月需求量(T月)", name = "tMonthQty")
    @TableField(value = "T_MONTH_QTY")
    private BigDecimal tMonthQty;

    /** 次月EUDR(T月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.tMonthEudrQty")
    @ApiModelProperty(value = "次月EUDR(T月)", name = "tMonthEudrQty")
    @TableField(value = "T_MONTH_EUDR_QTY")
    private BigDecimal tMonthEudrQty;

    /** 次次月需求量(T+1月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t1MonthQty")
    @ApiModelProperty(value = "次次月需求量(T+1月)", name = "t1MonthQty")
    @TableField(value = "T1_MONTH_QTY")
    private BigDecimal t1MonthQty;

    /** 次次月EUDR(T+1月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t1MonthEudrQty")
    @ApiModelProperty(value = "次次月EUDR(T+1月)", name = "t1MonthEudrQty")
    @TableField(value = "T1_MONTH_EUDR_QTY")
    private BigDecimal t1MonthEudrQty;

    /** 次次次月需求量(T+2月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t2MonthQty")
    @ApiModelProperty(value = "次次次月需求量(T+2月)", name = "t2MonthQty")
    @TableField(value = "T2_MONTH_QTY")
    private BigDecimal t2MonthQty;

    /** 次次次月EUDR(T+2月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t2MonthEudrQty")
    @ApiModelProperty(value = "次次次月EUDR(T+2月)", name = "t2MonthEudrQty")
    @TableField(value = "T2_MONTH_EUDR_QTY")
    private BigDecimal t2MonthEudrQty;



    public RawMaterialRequirePlan() {
        super();
    }

    public RawMaterialRequirePlan(String materialCode) {
        this.materialCode = materialCode;
    }

    /**
     * 合并另一个需求计划的数据到当前对象
     * @param other 另一个需求计划
     */
    public void merge(RawMaterialRequirePlan other) {
        if (other == null) {
            return;
        }

        // 合并当月需求量
        if (other.getCurMonthQty() != null) {
            if (this.curMonthQty == null) {
                this.curMonthQty = other.getCurMonthQty();
            } else {
                this.curMonthQty = this.curMonthQty.add(other.getCurMonthQty());
            }
        }

        // 合并当月EUDR
        if (other.getCurMonthRudrQty() != null) {
            if (this.curMonthRudrQty == null) {
                this.curMonthRudrQty = other.getCurMonthRudrQty();
            } else {
                this.curMonthRudrQty = this.curMonthRudrQty.add(other.getCurMonthRudrQty());
            }
        }

        // 合并次月需求量
        if (other.getTMonthQty() != null) {
            if (this.tMonthQty == null) {
                this.tMonthQty = other.getTMonthQty();
            } else {
                this.tMonthQty = this.tMonthQty.add(other.getTMonthQty());
            }
        }

        // 合并次月EUDR
        if (other.getTMonthEudrQty() != null) {
            if (this.tMonthEudrQty == null) {
                this.tMonthEudrQty = other.getTMonthEudrQty();
            } else {
                this.tMonthEudrQty = this.tMonthEudrQty.add(other.getTMonthEudrQty());
            }
        }

        // 合并次次月需求量
        if (other.getT1MonthQty() != null) {
            if (this.t1MonthQty == null) {
                this.t1MonthQty = other.getT1MonthQty();
            } else {
                this.t1MonthQty = this.t1MonthQty.add(other.getT1MonthQty());
            }
        }

        // 合并次次月EUDR
        if (other.getT1MonthEudrQty() != null) {
            if (this.t1MonthEudrQty == null) {
                this.t1MonthEudrQty = other.getT1MonthEudrQty();
            } else {
                this.t1MonthEudrQty = this.t1MonthEudrQty.add(other.getT1MonthEudrQty());
            }
        }

        // 合并次次次月需求量
        if (other.getT2MonthQty() != null) {
            if (this.t2MonthQty == null) {
                this.t2MonthQty = other.getT2MonthQty();
            } else {
                this.t2MonthQty = this.t2MonthQty.add(other.getT2MonthQty());
            }
        }

        // 合并次次次月EUDR
        if (other.getT2MonthEudrQty() != null) {
            if (this.t2MonthEudrQty == null) {
                this.t2MonthEudrQty = other.getT2MonthEudrQty();
            } else {
                this.t2MonthEudrQty = this.t2MonthEudrQty.add(other.getT2MonthEudrQty());
            }
        }
    }

    /**
     * 判断是否需要更新
     * @param other 另一个需求计划
     * @return 是否需要更新
     */
    public boolean needsUpdate(RawMaterialRequirePlan other) {
        if (other == null) {
            return false;
        }

        // 检查是否有任何字段的值不同
        return !equalsQuantity(this.curMonthQty, other.getCurMonthQty())
                || !equalsQuantity(this.curMonthRudrQty, other.getCurMonthRudrQty())
                || !equalsQuantity(this.tMonthQty, other.getTMonthQty())
                || !equalsQuantity(this.tMonthEudrQty, other.getTMonthEudrQty())
                || !equalsQuantity(this.t1MonthQty, other.getT1MonthQty())
                || !equalsQuantity(this.t1MonthEudrQty, other.getT1MonthEudrQty())
                || !equalsQuantity(this.t2MonthQty, other.getT2MonthQty())
                || !equalsQuantity(this.t2MonthEudrQty, other.getT2MonthEudrQty());
    }

    /**
     * 比较两个BigDecimal数量是否相等
     */
    private boolean equalsQuantity(BigDecimal qty1, BigDecimal qty2) {
        if (qty1 == null && qty2 == null) {
            return true;
        }
        if (qty1 == null || qty2 == null) {
            return false;
        }
        return qty1.compareTo(qty2) == 0;
    }

    /**
     * 获取总需求量（所有月份之和）
     * @return 总需求量
     */
    public BigDecimal getTotalRequirement() {
        BigDecimal total = BigDecimal.ZERO;
        if (curMonthQty != null) total = total.add(curMonthQty);
        if (tMonthQty != null) total = total.add(tMonthQty);
        if (t1MonthQty != null) total = total.add(t1MonthQty);
        if (t2MonthQty != null) total = total.add(t2MonthQty);
        return total;
    }

    /**
     * 获取总EUDR需求量（所有月份之和）
     * @return 总EUDR需求量
     */
    public BigDecimal getTotalEudrRequirement() {
        BigDecimal total = BigDecimal.ZERO;
        if (curMonthRudrQty != null) total = total.add(curMonthRudrQty);
        if (tMonthEudrQty != null) total = total.add(tMonthEudrQty);
        if (t1MonthEudrQty != null) total = total.add(t1MonthEudrQty);
        if (t2MonthEudrQty != null) total = total.add(t2MonthEudrQty);
        return total;
    }

    /**
     * 检查是否为空需求（所有字段都为null或0）
     * @return 是否为空需求
     */
    public boolean isEmptyRequirement() {
        return (curMonthQty == null || curMonthQty.compareTo(BigDecimal.ZERO) == 0)
                && (curMonthRudrQty == null || curMonthRudrQty.compareTo(BigDecimal.ZERO) == 0)
                && (tMonthQty == null || tMonthQty.compareTo(BigDecimal.ZERO) == 0)
                && (tMonthEudrQty == null || tMonthEudrQty.compareTo(BigDecimal.ZERO) == 0)
                && (t1MonthQty == null || t1MonthQty.compareTo(BigDecimal.ZERO) == 0)
                && (t1MonthEudrQty == null || t1MonthEudrQty.compareTo(BigDecimal.ZERO) == 0)
                && (t2MonthQty == null || t2MonthQty.compareTo(BigDecimal.ZERO) == 0)
                && (t2MonthEudrQty == null || t2MonthEudrQty.compareTo(BigDecimal.ZERO) == 0);
    }

    /**
     * 获取需求计划的唯一标识键
     * @return 唯一标识键
     */
    public String getUniqueKey() {
        return String.format("%s_%d_%d_%s",
                factoryCode, year, month, materialCode);
    }
}