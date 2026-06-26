package com.zlt.aps.gdyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 钢带压延库存信息。
 */
@Data
@ApiModel(value = "钢带压延库存信息", description = "钢带压延库存信息")
@TableName("T_GDYY_STOCK")
public class GdyyStock extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.gdyyStock.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 库存日期 */
    @ApiModelProperty("库存日期")
    @ImportExcelValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("STOCK_DATE")
    @Excel(name = "ui.data.column.gdyyStock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date stockDate;

    /** 钢带大卷编号 */
    @ApiModelProperty("钢带大卷编号")
    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.gdyyStock.bigRollCode")
    private String bigRollCode;

    /** 钢带大卷条码 */
    @ApiModelProperty("钢带大卷条码")
    @TableField("BIG_ROLL_BARCODE")
    @Excel(name = "ui.data.column.gdyyStock.bigRollBarcode")
    private String bigRollBarcode;

    /** 入库时间 */
    @ApiModelProperty("入库时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("INBOUND_TIME")
    @Excel(name = "ui.data.column.gdyyStock.inboundTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date inboundTime;

    /** MES库存量（个） */
    @ApiModelProperty("MES库存量（个）")
    @TableField("STOCK_NUM")
    @Excel(name = "ui.data.column.gdyyStock.stockNum")
    private BigDecimal stockNum;

    /** 大卷库存数（卷） */
    @ApiModelProperty("大卷库存数（卷）")
    @TableField("STOCK_ROLL_NUM")
    @Excel(name = "ui.data.column.gdyyStock.stockRollNum")
    private BigDecimal stockRollNum;

    /** 大卷米数（米） */
    @ApiModelProperty("大卷米数（米）")
    @TableField("STOCK_METERS")
    @Excel(name = "ui.data.column.gdyyStock.stockMeters")
    private BigDecimal stockMeters;

    /** MES库存修正数量 */
    @ApiModelProperty("MES库存修正数量")
    @TableField("MODIFY_NUM")
    @Excel(name = "ui.data.column.gdyyStock.modifyNum")
    private BigDecimal modifyNum;

    /** MES库存不良数量 */
    @ApiModelProperty("MES库存不良数量")
    @TableField("BAD_NUM")
    @Excel(name = "ui.data.column.gdyyStock.badNum")
    private BigDecimal badNum;

    /** 预计库存标记，0为预计库存，其他为正式库存 */
    @ApiModelProperty("预计库存标记")
    @TableField("ESTIMATE_STOCK_FLAG")
    @Excel(name = "ui.data.column.gdyyStock.estimateStockFlag")
    private String estimateStockFlag;

    /** 查询用：开始日期 */
    @TableField(exist = false)
    private String startTime;

    /** 查询用：结束日期 */
    @TableField(exist = false)
    private String endTime;
}
