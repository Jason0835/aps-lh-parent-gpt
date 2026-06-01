package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成型库存信息实体（aps-cx-lh-api 副本，供 aps-lh 模块直接查询成型库存表使用）
 * 对应表：T_CX_STOCK
 *
 * @author APS Team
 */
@ApiModel(value = "成型库存信息对象", description = "成型库存信息对象")
@Data
@TableName(value = "T_CX_STOCK")
public class CxStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.cxStock.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, maxLength = 30)
    @ApiModelProperty(value = "分厂编号")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.cxStock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库存日期，格式：yyyy-MM-dd")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    @Excel(name = "ui.data.column.cxStock.embryoCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "胎胚代码")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "胎胚描述")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.cxStock.embryoDesc", width = 60, align = Excel.Align.LEFT)
    private String embryoDesc;

    @Excel(name = "ui.data.column.cxStock.stockNum")
    @ImportExcelValidated(required = true, digits = true, max = 9999999)
    @ApiModelProperty(value = "库存量")
    @TableField(value = "STOCK_NUM")
    private Integer stockNum;

    @ApiModelProperty(value = "超期库存")
    @TableField(value = "OVER_TIME_STOCK")
    private Integer overTimeStock;

    @ApiModelProperty(value = "修正数量")
    @TableField(value = "MODIFY_NUM")
    private Integer modifyNum;

    @ApiModelProperty(value = "不良数量")
    @TableField(value = "BAD_NUM")
    private Integer badNum;

    @ApiModelProperty(value = "是否收尾SKU：0-否，1-是")
    @TableField(value = "IS_ENDING_SKU")
    private String isEndingSku;

    @Excel(name = "ui.data.column.cxStock.dataSource", dictType = "lh_precision_data_source")
    @ApiModelProperty(value = "数据来源：MES-MES同步，MANUAL-手动录入")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    @TableField(exist = false)
    private Long scheduleUseStock;

    @TableField(exist = false)
    private BigDecimal stockHours;

    @TableField(exist = false)
    private String alertStatus;

    @TableField(exist = false)
    private Date alertTime;

    @TableField(exist = false)
    private Integer vulcanizeMachineCount;

    @TableField(exist = false)
    private Integer vulcanizeMoldCount;

    @ApiModelProperty(value = "查询库存的开始日期yyyy-MM-dd")
    @TableField(exist = false)
    private String startTime;

    @ApiModelProperty(value = "查询库存的结束日期yyyy-MM-dd")
    @TableField(exist = false)
    private String endTime;

    @TableField(exist = false)
    @Excel(name = "ui.data.column.cxStock.updateDay", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateDay;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;

    @TableField(exist = false)
    private String stockDateStr;

    @TableField(exist = false)
    private Integer stockRealNum;

    @TableField(exist = false)
    private String bomDataVersion;

    public Integer getEffectiveStock() {
        int effective = stockNum != null ? stockNum : 0;
        if (overTimeStock != null) {
            effective -= overTimeStock;
        }
        if (badNum != null) {
            effective -= badNum;
        }
        if (modifyNum != null) {
            effective += modifyNum;
        }
        return Math.max(0, effective);
    }

    public Long getAvailableStock() {
        long available = getEffectiveStock();
        if (scheduleUseStock != null) {
            available -= scheduleUseStock;
        }
        return Math.max(0L, available);
    }

    public BigDecimal getAvailableHours() {
        return stockHours != null ? stockHours : BigDecimal.ZERO;
    }
}
