package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.math.BigDecimal;

/**
 * 胎侧排程未排列表 实体类
 */
@ApiModel(value = "胎侧排程未排列表对象", description = "胎侧排程未排列表对象")
@Data
@TableName(value = "T_TC_SCHEDULE_UNPLANNED")
public class TcScheduleUnplanned extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 稳定任务业务键，用于关联未排解释 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.taskBusinessKey")
    @ApiModelProperty(value = "稳定任务业务键", name = "taskBusinessKey")
    @TableField(value = "TASK_BUSINESS_KEY")
    private String taskBusinessKey;

    /** 排程日期 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 胎侧编码 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.sidewallCode")
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    @TableField(value = "SIDEWALL_CODE")
    private String sidewallCode;

    /** 主胶料编码 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.glueCode")
    @ApiModelProperty(value = "主胶料编码", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    /** 口型板编码 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.mouthPlateCode")
    @ApiModelProperty(value = "口型板编码", name = "mouthPlateCode")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /** 班次顺序 */
    @Excel(name = "ui.tc.schedule.unplanned.shiftOrder")
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    @TableField(value = "SHIFT_ORDER")
    private Integer shiftOrder;

    /** 成型需求量，单位米 */
    @Excel(name = "ui.tc.schedule.unplanned.demandQty")
    @ApiModelProperty(value = "需求量", name = "demandQty")
    @TableField(value = "DEMAND_QTY")
    private BigDecimal demandQty;

    /** 期望计划量，单位米 */
    @Excel(name = "ui.tc.schedule.unplanned.planQty")
    @ApiModelProperty(value = "期望计划量", name = "planQty")
    @TableField(value = "PLAN_QTY")
    private BigDecimal planQty;

    /** 未排原因编码 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.unplannedReasonCode")
    @ApiModelProperty(value = "未排原因编码", name = "unplannedReasonCode")
    @TableField(value = "UNPLANNED_REASON_CODE")
    private String unplannedReasonCode;

    /** 未排原因说明 */
    @Excel(name = "ui.tc.schedule.scheduleUnplanned.unplannedReasonDesc")
    @ApiModelProperty(value = "未排原因说明", name = "unplannedReasonDesc")
    @TableField(value = "UNPLANNED_REASON_DESC")
    private String unplannedReasonDesc;

    /** 未排证据文本 */
    @ApiModelProperty(value = "未排证据文本", name = "unplannedEvidenceJson")
    @TableField(value = "UNPLANNED_EVIDENCE_JSON")
    private String unplannedEvidenceJson;
}
