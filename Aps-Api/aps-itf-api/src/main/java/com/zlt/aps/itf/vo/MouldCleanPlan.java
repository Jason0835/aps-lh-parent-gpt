package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "模具清洗预警中间表", description = "模具清洗预警中间表")
@Data
@TableName("MOULD_CLEAN_PLAN")
public class MouldCleanPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "硫化机台")
    @TableField(value = "LH_CODE")
    private String lhCode;

    @ApiModelProperty(value = "上机时间")
    @TableField(value = "OPER_TIME")
    private String operTime;

    @ApiModelProperty(value = "首次清洗时间")
    @TableField(value = "FIRST_WASH_TIME")
    private String firstWashTime;

    @ApiModelProperty(value = "二次清洗时间")
    @TableField(value = "SECOND_WASH_TIME")
    private String secondWashTime;

    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
