package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 原材料预警记录
 */
@ApiModel(value = "原材料预警记录", description = "原材料预警记录")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_RAW_WARNING_RECORD")
public class RawWarningRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 工厂编码
     */
    @Excel(name = "工厂编码")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 预警类型：1-用量偏差预警 2-新材料预警
     */
    @Excel(name = "预警类型", dictType = "raw_warning_type")
    @ApiModelProperty(value = "预警类型", name = "warningType")
    @TableField(value = "WARNING_TYPE")
    private String warningType;

    /**
     * 原材料编码
     */
    @Excel(name = "原材料编码")
    @ApiModelProperty(value = "原材料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 原材料名称
     */
    @Excel(name = "原材料名称")
    @ApiModelProperty(value = "原材料名称", name = "materialName")
    @TableField(value = "MATERIAL_NAME")
    private String materialName;

    /**
     * 预警级别：1-低 2-中 3-高
     */
    @Excel(name = "预警级别", dictType = "warning_level")
    @ApiModelProperty(value = "预警级别", name = "warningLevel")
    @TableField(value = "WARNING_LEVEL")
    private String warningLevel;

    /**
     * 预警标题
     */
    @Excel(name = "预警标题")
    @ApiModelProperty(value = "预警标题", name = "warningTitle")
    @TableField(value = "WARNING_TITLE")
    private String warningTitle;

    /**
     * 预警内容
     */
    @Excel(name = "预警内容")
    @ApiModelProperty(value = "预警内容", name = "warningContent")
    @TableField(value = "WARNING_CONTENT")
    private String warningContent;

    /**
     * 相关月份（用于新材料预警）
     */
    @Excel(name = "相关月份")
    @ApiModelProperty(value = "相关月份", name = "relatedMonth")
    @TableField(value = "RELATED_MONTH")
    private String relatedMonth;

    /**
     * 相关周次（用于用量偏差预警）
     */
    @Excel(name = "相关周次")
    @ApiModelProperty(value = "相关周次", name = "relatedWeek")
    @TableField(value = "RELATED_WEEK")
    private String relatedWeek;

    /**
     * 预警数据JSON
     */
    @Excel(name = "预警数据")
    @ApiModelProperty(value = "预警数据", name = "warningData")
    @TableField(value = "WARNING_DATA")
    private String warningData;

    /**
     * 处理状态：0-未处理 1-已处理 2-处理中
     */
    @Excel(name = "处理状态", dictType = "warning_status")
    @ApiModelProperty(value = "处理状态", name = "status")
    @TableField(value = "STATUS")
    private String status;

    /**
     * 处理人
     */
    @Excel(name = "处理人")
    @ApiModelProperty(value = "处理人", name = "handler")
    @TableField(value = "HANDLER")
    private String handler;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "处理时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "处理时间", name = "handleTime")
    @TableField(value = "HANDLE_TIME")
    private Date handleTime;

    /**
     * 处理意见
     */
    @Excel(name = "处理意见")
    @ApiModelProperty(value = "处理意见", name = "handleOpinion")
    @TableField(value = "HANDLE_OPINION")
    private String handleOpinion;

    /**
     * 是否已通知：0-否 1-是
     */
    @Excel(name = "是否已通知", dictType = "sys_yes_no")
    @ApiModelProperty(value = "是否已通知", name = "notified")
    @TableField(value = "NOTIFIED")
    private Integer notified;

    /**
     * 通知时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "通知时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "通知时间", name = "notifyTime")
    @TableField(value = "NOTIFY_TIME")
    private Date notifyTime;
}