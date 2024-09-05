package com.zlt.aps.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 定点机台对象 t_cx_specify_machine
 *
 * @author zlt
 * @date 2021-07-21
 */
@ApiModel(value = "定点机台对象", description = "定点机台对象 ")
public class CxSpecifyMachine extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * SAP品号
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.specifyMachine.sapCode")
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /**
     * 胎胚代码
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.specifyMachine.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号")
    private String machineCode;

    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.machine.machineName")
    private String machineName;

    /**
     * 线路
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路")
    private String lineType;

    /**
     * 作业类型
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.jobType", dictType = "JOB_TYPE")
    @ApiModelProperty(value = "作业类型")
    private String jobType;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.remark")
    private String remark;

    /**
     * 删除标识
     */
    private String delFlag;

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSapCode() {
        return sapCode;
    }

    public void setSapCode(String sapCode) {
        this.sapCode = sapCode;
    }

    public String getEmbryoCode() {
        return embryoCode;
    }

    public void setEmbryoCode(String embryoCode) {
        this.embryoCode = embryoCode;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("sapCode", getSapCode())
                .append("embryoCode", getEmbryoCode())
                .append("machineCode", getMachineCode())
                .append("lineType", getLineType())
                .append("jobType", getJobType())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .append("remark", getRemark())
                .toString();
    }

}
