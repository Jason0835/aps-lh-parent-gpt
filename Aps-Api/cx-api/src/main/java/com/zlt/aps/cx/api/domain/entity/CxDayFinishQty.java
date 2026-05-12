package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * APS成型排程日完成量
 *
 * @author APS Team
 * @since 2026/04/09
 */
@ApiModel(value = "APS成型排程日完成量", description = "APS成型排程日完成量")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_DAY_FINISH_QTY")
public class CxDayFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 完成日期 */
    @Excel(name = "ui.data.column.cxDayFinishQty.finishDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "完成日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "FINISH_DATE")
    private Date finishDate;

    /** 胚胎日完成量 */
    @Excel(name = "ui.data.column.cxDayFinishQty.dayFinishQty")
    @ApiModelProperty(value = "胚胎日完成量")
    @TableField(value = "DAY_FINISH_QTY")
    private BigDecimal dayFinishQty;

    /** 成型胚胎物料编码 */
    @Excel(name = "ui.data.column.cxDayFinishQty.embryoCode")
    @ApiModelProperty(value = "成型胚胎物料编码")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 示方类型 */
    @Excel(name = "ui.data.column.cxDayFinishQty.exampleType")
    @ApiModelProperty(value = "示方类型")
    @TableField(value = "EXAMPLE_TYPE")
    private String exampleType;

    /** 胚胎施工版本号 */
    @Excel(name = "ui.data.column.cxDayFinishQty.bomDataVersion")
    @ApiModelProperty(value = "胚胎施工版本号")
    @TableField(value = "BOM_DATA_VERSION")
    private String bomDataVersion;

    /** 版本号 */
    @Excel(name = "ui.data.column.cxDayFinishQty.dataVersion")
    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /** 厂别 */
    @Excel(name = "ui.data.column.cxDayFinishQty.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 完成日期开始（搜索用，非数据库列） */
    @ApiModelProperty(value = "完成日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date finishDateStart;

    /** 完成日期结束（搜索用，非数据库列） */
    @ApiModelProperty(value = "完成日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date finishDateEnd;
}
