package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 斜裁库存管理。
 */
@Data
@ApiModel(value = "斜裁库存管理", description = "斜裁库存管理")
@TableName("t_cd15_stock")
public class Cd15Stock extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15Stock.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 库存日期 */
    @ApiModelProperty(value = "库存日期", name = "stockDate")
    @ImportExcelValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("STOCK_DATE")
    @Excel(name = "ui.data.column.cd15Stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date stockDate;

    /** 库存物料编号（钢带代码） */
    @ApiModelProperty(value = "库存物料编号（钢带代码）", name = "materialCode")
    @ImportExcelValidated(required = true, maxLength = 60)
    @TableField("MATERIAL_CODE")
    @Excel(name = "ui.data.column.cd15Stock.materialCode")
    private String materialCode;

    /** 库存量(米) */
    @ApiModelProperty(value = "库存量(米)", name = "stockNum")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("STOCK_NUM")
    @Excel(name = "ui.data.column.cd15Stock.stockNum")
    private Double stockNum;

    /** 修正数量(米) */
    @ApiModelProperty(value = "修正数量(米)", name = "modifyNum")
    @TableField("MODIFY_NUM")
    @Excel(name = "ui.data.column.cd15Stock.modifyNum")
    private Double modifyNum;

    /** 不良数量(米) */
    @ApiModelProperty(value = "不良数量(米)", name = "badNum")
    @TableField("BAD_NUM")
    @Excel(name = "ui.data.column.cd15Stock.badNum")
    private Double badNum;

    /** 库存量(卷) */
    @ApiModelProperty(value = "库存量(卷)", name = "rollStockNum")
    @TableField("ROLL_STOCK_NUM")
    // @Excel(name = "ui.data.column.cd15Stock.rollStockNum")
    private Double rollStockNum;

    /** 修正数量(卷) */
    @ApiModelProperty(value = "修正数量(卷)", name = "rollModifyNum")
    @TableField("ROLL_MODIFY_NUM")
    // @Excel(name = "ui.data.column.cd15Stock.rollModifyNum")
    private Double rollModifyNum;

    /** 不良数量(卷) */
    @ApiModelProperty(value = "不良数量(卷)", name = "rollBadNum")
    @TableField("ROLL_BAD_NUM")
    // @Excel(name = "ui.data.column.cd15Stock.rollBadNum")
    private Double rollBadNum;

    /** 库存日期开始 */
    @ApiModelProperty(value = "库存日期开始", name = "stockDateStart")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField(exist = false)
    private Date stockDateStart;

    /** 库存日期结束 */
    @ApiModelProperty(value = "库存日期结束", name = "stockDateEnd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField(exist = false)
    private Date stockDateEnd;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
