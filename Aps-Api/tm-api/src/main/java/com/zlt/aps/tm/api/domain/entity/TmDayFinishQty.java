package com.zlt.aps.tm.api.domain.entity;

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
 * APS胎面排程日完成量回报
 * 对应APS表 T_TM_DAY_FINISH_QTY（1-total 结构，复用表名）
 *
 * 注意：该表 mps 模块以 2 班结构（DAY_FINISH_QTY/NIGHT_FINISH_QTY）读写，
 * 本次胎面 MES 同步采用 1-total（FINISH_QTY）结构复用该表名，
 * 逻辑删除+插入会覆盖 mps 同日同分厂数据，存在冲突风险（详见详设文档）。
 *
 * @author APS Team
 */
@ApiModel(value = "APS胎面排程日完成量回报", description = "APS胎面排程日完成量回报")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TM_DAY_FINISH_QTY")
public class TmDayFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.tmDayFinishQty.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    @Excel(name = "ui.data.column.tmDayFinishQty.treadCode")
    @ApiModelProperty(value = "胎面代码")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    @Excel(name = "ui.data.column.tmDayFinishQty.finishQty")
    @ApiModelProperty(value = "完成量")
    @TableField(value = "FINISH_QTY")
    private BigDecimal finishQty;

    @Excel(name = "ui.data.column.tmDayFinishQty.dataVersion")
    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @Excel(name = "ui.data.column.tmDayFinishQty.companyCode")
    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @Excel(name = "ui.data.column.tmDayFinishQty.factoryCode")
    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
