package com.zlt.kettle.api.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


@Getter
@Setter
@ApiModel(value = "Job记录")
public class JobRecord extends BaseEntity{
    private static final long serialVersionUID = 1L;

    private Integer id;

    /**
     * Job转换ID
     */
    @ApiModelProperty(value = "Job转换ID")
    private Integer recordJobId;

    /**
     * 转换名称
     */
    @ApiModelProperty(value = "Job名称")
    private String taskName;

    /**
     * 转换描述
     */
    @ApiModelProperty(value = "转换描述")
    private String jobDescription;
    /**
     * 转换分类
     */
    @ApiModelProperty(value = "分类")
    private String categoryName;

    /**
     * 转换分类
     */
    @ApiModelProperty(value = "分类id")
    private String categoryId;
    /**
     * 启动时间
     */
    @ApiModelProperty(value = "启动时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 停止时间
     */
    @ApiModelProperty(value = "停止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date stopTime;

    /**
     * 任务执行结果（1：成功；0：失败）
     */
    @ApiModelProperty(value = "任务执行结果（1：成功；0：失败）")
    private Integer recordStatus;

    /**
     * 转换日志记录文件保存位置
     */
    @ApiModelProperty(value = "转换日志记录文件保存位置")
    private String logFilePath;

    /**
     * 转换日志记录文件保存位置
     */
    @ApiModelProperty(value = "删除标志")
    private String delFlag;
}
