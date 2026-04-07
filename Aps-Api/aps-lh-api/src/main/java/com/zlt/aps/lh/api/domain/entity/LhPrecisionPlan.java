package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 硫化精度计划实体
 *
 * 业务规则：
 * 1. 硫化机台每年都要维保1次
 * 2. 维保计划7个小时（8:00-15:00）
 * 3. 第1次数据源于MES，后面APS自动按自然年度自动推算（前年的实际时间）
 * 4. 需要提前30天预警
 *
 * @author APS Team
 */
@Data
@TableName("T_LH_PRECISION_PLAN")
@ApiModel(value = "硫化精度计划", description = "硫化机台精度保养计划")
public class LhPrecisionPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.lhPrecisionPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.lhPrecisionPlan.machineCode")
    @ApiModelProperty(value = "机台编号")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.lhPrecisionPlan.precisionType", dictType = "MACHINE_ACCURACY_TYPE")
    @ApiModelProperty(value = "精度类型")
    @TableField("PRECISION_TYPE")
    private String precisionType;

    @Excel(name = "ui.data.column.lhPrecisionPlan.planDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PLAN_DATE")
    private LocalDate planDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.actualDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("ACTUAL_DATE")
    private LocalDate actualDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.dueDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "到期日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("DUE_DATE")
    private LocalDate dueDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.daysToDue")
    @ApiModelProperty(value = "距离到期日剩余天数")
    @TableField("DAYS_TO_DUE")
    private Integer daysToDue;

    @Excel(name = "ui.data.column.lhPrecisionPlan.lastMaintenanceDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "上次保养日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("LAST_MAINTENANCE_DATE")
    private LocalDate lastMaintenanceDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.completionStatus", dictType = "lh_precision_completion_status")
    @ApiModelProperty(value = "完成情况：0-未完成，1-已完成")
    @TableField("COMPLETION_STATUS")
    private String completionStatus;

    @Excel(name = "ui.data.column.lhPrecisionPlan.year")
    @ApiModelProperty(value = "计划年度")
    @TableField("YEAR")
    private BigDecimal year;

    @Excel(name = "ui.data.column.lhPrecisionPlan.warningStatus", dictType = "lh_precision_warning_status")
    @ApiModelProperty(value = "预警状态：0-未预警，1-已预警")
    @TableField("WARNING_STATUS")
    private String warningStatus;

    @Excel(name = "ui.data.column.lhPrecisionPlan.warningDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "预警触发日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("WARNING_DATE")
    private LocalDate warningDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.isWarningSent", dictType = "lh_precision_warning_sent")
    @ApiModelProperty(value = "是否已发送预警：0-未发送，1-已发送")
    @TableField("IS_WARNING_SENT")
    private String isWarningSent;

    @Excel(name = "ui.data.column.lhPrecisionPlan.dataSource", dictType = "lh_precision_data_source")
    @ApiModelProperty(value = "数据来源：0-同步，1-自动生成")
    @TableField("DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "MES来源ID")
    @TableField("MES_SOURCE_ID")
    private Long mesSourceId;

    @ApiModelProperty(value = "分公司编码")
    @TableField("COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "计划日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateStart;

    @ApiModelProperty(value = "计划日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateEnd;

    @ApiModelProperty(value = "实际日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDateStart;

    @ApiModelProperty(value = "实际日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDateEnd;
}
