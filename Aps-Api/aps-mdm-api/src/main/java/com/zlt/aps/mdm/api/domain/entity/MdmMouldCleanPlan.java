package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * APS模具清洗预警计划
 *
 * @author zlt
 * @since 2025/12/25
 */
@ApiModel(value = "APS模具清洗预警计划", description = "APS模具清洗预警计划")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOULD_CLEAN_PLAN")
public class MdmMouldCleanPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    @TableField(value = "DEL_FLAG")
    private Integer delFlag;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "上机时间开始")
    private String operTimeBegin;

    @ApiModelProperty(value = "上机时间结束")
    private String operTimeEnd;

}
