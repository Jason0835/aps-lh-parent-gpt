package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 90°裁断库存信息对象
 *
 * @author zlt
 * @date 2021-05-31
 */
@Data
@ApiModel(value = "90°裁断库存信息对象", description = "90°裁断库存信息对象")
public class Cd90Stock extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "库存日期", position = 20)
    private Date stockDate;

    @ApiModelProperty(value = "查询库存的开始日期yyyy-MM-dd", position = 21)
    private String startTime;

    @ApiModelProperty(value = "查询库存的结束日期yyyy-MM-dd", position = 22)
    private String endTime;

    /**
     * 库存物料编号
     */
    @Excel(name = "ui.data.column.stock.clothCode")
    @ImportValidated(required = true, maxLength = 50, isCode = true)
    @ApiModelProperty(value = "库存物料编号", position = 30)
    private String materialCode;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", position = 40)
    @ImportValidated(number = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.stock.stockNum")
    private BigDecimal stockNum;

    /**
     * 修正数量
     */
    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modifyNum")
    @ImportValidated(number = true, min = -999999, max = 999999)
    private BigDecimal modifyNum;

    /**
     * 不良数量
     */
    @ApiModelProperty(value = "不良数量", position = 60)
    @ImportValidated(number = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.stock.badNum")
    private BigDecimal badNum;

    /**
     * 库存量(卷)
     */
    @ApiModelProperty(value = "库存量(卷)", position = 70)
    @Excel(name = "ui.data.column.stock.rollStockNum", scale = 1)
    @ImportValidated(name = "ui.data.column.stock.rollStockNum", number = true, min = 0, max = 999999)
    private BigDecimal rollStockNum;

    /**
     * 修正数量(卷)
     */
    @ApiModelProperty(value = "修正数量(卷)", position = 80)
    @Excel(name = "ui.data.column.stock.rollModifyNum")
    @ImportValidated(name = "ui.data.column.stock.rollModifyNum", number = true, min = -999999, max = 999999)
    private BigDecimal rollModifyNum;

    /**
     * 不良数量(卷)
     */
    @ApiModelProperty(value = "不良数量(卷)", position = 90)
    @Excel(name = "ui.data.column.stock.rollBadNum")
    @ImportValidated(name = "ui.data.column.stock.rollBadNum", number = true, min = 0, max = 999999)
    private BigDecimal rollBadNum;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(maxLength = 300)
    private String remark;

    /**
     * 删除标识：0--正常，1-删除
     */
    private String delFlag;

    /**
     * 卷曲长度。此胎面一卷的最大长度，单位：米。
     */
    @ApiModelProperty(value = "卷曲长度。此胎面一卷的最大长度，单位：米。")
    @TableField(exist = false)
    private BigDecimal curlLength;
}
