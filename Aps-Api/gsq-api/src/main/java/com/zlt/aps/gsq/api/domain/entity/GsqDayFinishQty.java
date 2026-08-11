package com.zlt.aps.gsq.api.domain.entity;

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
 * APS钢丝圈排程日完成量回报
 *
 * @author APS Team
 * @since 2026/08/11
 */
@ApiModel(value = "APS钢丝圈排程日完成量回报", description = "APS钢丝圈排程日完成量回报")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_GSQ_DAY_FINISH_QTY")
public class GsqDayFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.gsqDayFinishQty.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    @Excel(name = "ui.data.column.gsqDayFinishQty.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    @Excel(name = "ui.data.column.gsqDayFinishQty.finishQty")
    @ApiModelProperty(value = "完成量")
    @TableField(value = "FINISH_QTY")
    private BigDecimal finishQty;

    @Excel(name = "ui.data.column.gsqDayFinishQty.dataVersion")
    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @Excel(name = "ui.data.column.gsqDayFinishQty.companyCode")
    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @Excel(name = "ui.data.column.gsqDayFinishQty.factoryCode")
    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
