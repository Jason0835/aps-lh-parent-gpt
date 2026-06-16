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
 * APS硫化排程日完成量
 *
 * @author APS Team
 * @since 2026/04/13
 */
@ApiModel(value = "APS硫化排程日完成量", description = "APS硫化排程日完成量")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_LH_DAY_FINISH_QTY")
public class LhDayFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhDayFinishQty.finishDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "完成日期")
    @TableField(value = "FINISH_DATE")
    private Date finishDate;

    @Excel(name = "ui.data.column.lhDayFinishQty.dayFinishQty")
    @ApiModelProperty(value = "胚胎日完成量")
    @TableField(value = "DAY_FINISH_QTY")
    private BigDecimal dayFinishQty;

    @Excel(name = "ui.data.column.lhDayFinishQty.materialCode")
    @ApiModelProperty(value = "物料编码（NC）")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @Excel(name = "ui.data.column.lhDayFinishQty.mesMaterialCode")
    @ApiModelProperty(value = "物料编码（MES）")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @Excel(name = "ui.data.column.lhDayFinishQty.dataVersion")
    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @Excel(name = "ui.data.column.lhDayFinishQty.companyCode")
    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @Excel(name = "ui.data.column.lhDayFinishQty.factoryCode")
    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.lhDayFinishQty.materialDesc")
    @ApiModelProperty(value = "物料描述")
    @TableField(exist = false)
    private String materialDesc;

    @Excel(name = "ui.data.column.lhDayFinishQty.lhNo")
    @ApiModelProperty(value = "示方号")
    @TableField(value = "LH_NO")
    private String lhNo;

    @Excel(name = "ui.data.column.lhDayFinishQty.lhType")
    @ApiModelProperty(value = "示方类型")
    @TableField(value = "LH_TYPE")
    private String lhType;
}
