package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备计划停机中间表VO
 * 对应MES中间库表 DEV_PLAN_CLOSE
 *
 * @author zlt
 * @since 2026/06/16
 */
@ApiModel(value = "设备计划停机中间表", description = "设备计划停机中间表")
@Data
@TableName("DEV_PLAN_CLOSE")
public class DevPlanCloseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    /**
     * 设备机台编码
     */
    @ApiModelProperty(value = "设备机台编码")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /**
     * 机台类型（01硫化/02成型/03压出/04裁断/05压延/06密炼）
     */
    @ApiModelProperty(value = "机台类型")
    @TableField(value = "MACHINE_TYPE")
    private String machineType;

    /**
     * 停机类型（01润滑/02巡检点检/03预见性维护/04预防性维护/05计划性维修/06临时性故障）
     */
    @ApiModelProperty(value = "停机类型")
    @TableField(value = "MACHINE_STOP_TYPE")
    private String machineStopType;

    /**
     * 计划开始时间
     */
    @ApiModelProperty(value = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "BEGIN_DATE")
    private Date beginDate;

    /**
     * 计划结束时间
     */
    @ApiModelProperty(value = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "END_DATE")
    private Date endDate;

    /**
     * 实际完成日期
     */
    @ApiModelProperty(value = "实际完成日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "ACTUAL_FINISH_DATE")
    private Date actualFinishDate;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;

    /**
     * 删除标识：0-正常，1-已删除
     */
    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 分公司编码
     */
    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 厂别
     */
    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
