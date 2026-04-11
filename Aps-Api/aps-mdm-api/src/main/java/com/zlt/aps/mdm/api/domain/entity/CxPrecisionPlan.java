package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成型精度计划实体（MDM侧复用）。
 * <p>
 * 对应表：T_CX_PRECISION_PLAN
 * <br/>
 * 说明：
 * <ul>
 *   <li>字段与 cx-api 的 CxPrecisionPlan 保持一致，便于跨模块传递</li>
 *   <li>包含查询辅助字段（exist=false）供条件检索使用</li>
 *   <li>保留少量调度兼容字段（planShift/estimatedHours，非数据库列）</li>
 * </ul>
 */
@Data
@TableName("T_CX_PRECISION_PLAN")
@ApiModel(value = "成型精度计划(MDM)", description = "成型机台精度保养计划")
public class CxPrecisionPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分厂编码 */
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 机台编号 */
    @TableField("MACHINE_CODE")
    private String machineCode;

    /** 精度类型 */
    @TableField("PRECISION_TYPE")
    private String precisionType;

    /** 计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PLAN_DATE")
    private LocalDate planDate;

    /** 实际执行日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("ACTUAL_DATE")
    private LocalDate actualDate;

    /** 到期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("DUE_DATE")
    private LocalDate dueDate;

    /** 距离到期日剩余天数 */
    @TableField("DAYS_TO_DUE")
    private Integer daysToDue;

    /** 上次保养日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("LAST_MAINTENANCE_DATE")
    private LocalDate lastMaintenanceDate;

    /** 完成状态：0-未完成，1-已完成 */
    @TableField("COMPLETION_STATUS")
    private String completionStatus;

    /** 计划年度 */
    @TableField("YEAR")
    private BigDecimal year;

    /** 预警状态：0-未预警，1-已预警 */
    @TableField("WARNING_STATUS")
    private String warningStatus;

    /** 预警触发日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("WARNING_DATE")
    private LocalDate warningDate;

    /** 是否已发送预警：0-未发送，1-已发送 */
    @TableField("IS_WARNING_SENT")
    private String isWarningSent;

    /** 数据来源：0-同步，1-自动生成 */
    @TableField("DATA_SOURCE")
    private String dataSource;

    /** MES来源ID */
    @TableField("MES_SOURCE_ID")
    private Long mesSourceId;

    /** 分公司编码 */
    @TableField("COMPANY_CODE")
    private String companyCode;

    /** 备注 */
    @TableField("REMARK")
    private String remark;

    /** 以下为查询辅助字段（非数据库列） */
    /** 计划日期开始（搜索用） */
    @ApiModelProperty(value = "计划日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateStart;

    /** 计划日期结束（搜索用） */
    @ApiModelProperty(value = "计划日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateEnd;

    /** 实际日期开始（搜索用） */
    @ApiModelProperty(value = "实际日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDateStart;

    /** 实际日期结束（搜索用） */
    @ApiModelProperty(value = "实际日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDateEnd;

    /** 以下为调度侧兼容字段（非数据库列） */
    /** 计划班次（调度兼容） */
    @TableField(exist = false)
    private String planShift;

    /** 预计时长（小时，调度兼容） */
    @TableField(exist = false)
    private BigDecimal estimatedHours;
}
