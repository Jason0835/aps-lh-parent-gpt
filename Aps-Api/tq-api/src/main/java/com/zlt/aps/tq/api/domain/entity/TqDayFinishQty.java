package com.zlt.aps.tq.api.domain.entity;

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
 * APS胎圈排程日完成量回报
 *
 * @author APS Team
 * @since 2026/06/18
 */
@ApiModel(value = "APS胎圈排程日完成量回报", description = "APS胎圈排程日完成量回报")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TQ_DAY_FINISH_QTY")
public class TqDayFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.tqDayFinishQty.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    @Excel(name = "ui.data.column.tqDayFinishQty.beadCode")
    @ApiModelProperty(value = "胎圈代码")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    @Excel(name = "ui.data.column.tqDayFinishQty.finishQty")
    @ApiModelProperty(value = "完成量")
    @TableField(value = "FINISH_QTY")
    private BigDecimal finishQty;

    @Excel(name = "ui.data.column.tqDayFinishQty.dataVersion")
    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @Excel(name = "ui.data.column.tqDayFinishQty.companyCode")
    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @Excel(name = "ui.data.column.tqDayFinishQty.factoryCode")
    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
