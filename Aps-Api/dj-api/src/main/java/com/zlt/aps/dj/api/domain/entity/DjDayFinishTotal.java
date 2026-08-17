package com.zlt.aps.dj.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 垫胶排程计划每日完成量
 * @TableName T_DJ_DAY_FINISH_QTY
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName(value = "T_DJ_DAY_FINISH_TOTAL")
public class DjDayFinishTotal extends BaseEntity {

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 排程时间
     */
    @ImportExcelValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.dayFinishQty.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程时间", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 垫胶代码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.dj.code")
    @ApiModelProperty(value = "垫胶代码", name = "paddingCode")
    @TableField(value = "PADDING_CODE")
    private String paddingCode;

    /**
     * 完成量
     */
    @Excel(name = "ui.data.column.dayFinishQty.nightFinishQty")
    @ApiModelProperty(value = "完成量", name = "nightFinishQty")
    @TableField(value = "FINISH_QTY")
    private BigDecimal nightFinishQty = BigDecimal.ZERO;

    private static final long serialVersionUID = 1L;

}