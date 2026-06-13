package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 胎面排程未排列表 实体类
 */
@ApiModel(value = "胎面排程未排列表对象", description = "胎面排程未排列表对象")
@Data
@TableName(value = "T_TM_SCHEDULE_UNPLANNED")
public class TmScheduleUnplanned extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 排程日期 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 胎面编码 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.treadCode")
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /** 主胶料编码 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.glueCode")
    @ApiModelProperty(value = "主胶料编码", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    /** 口型板编码 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.mouthPlateCode")
    @ApiModelProperty(value = "口型板编码", name = "mouthPlateCode")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /** 未排原因编码 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.unplannedReasonCode")
    @ApiModelProperty(value = "未排原因编码", name = "unplannedReasonCode")
    @TableField(value = "UNPLANNED_REASON_CODE")
    private String unplannedReasonCode;

    /** 未排原因说明 */
    @Excel(name = "ui.data.column.tm.scheduleUnplanned.unplannedReasonDesc")
    @ApiModelProperty(value = "未排原因说明", name = "unplannedReasonDesc")
    @TableField(value = "UNPLANNED_REASON_DESC")
    private String unplannedReasonDesc;

    /** 未排证据文本 */
    @ApiModelProperty(value = "未排证据文本", name = "unplannedEvidenceJson")
    @TableField(value = "UNPLANNED_EVIDENCE_JSON")
    private String unplannedEvidenceJson;
}
