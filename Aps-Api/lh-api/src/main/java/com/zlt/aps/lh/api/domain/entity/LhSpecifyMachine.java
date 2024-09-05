package com.zlt.aps.lh.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 硫化定点机台信息对象 t_lh_specify_machine
 *
 * @author zlt
 * @date 2021-07-21
 */
@ApiModel(value = "硫化定点机台信息对象", description = "硫化定点机台信息对象 ")
public class LhSpecifyMachine extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * SAP品号信息
     */
    @ImportValidated(maxLength = 20, isCode = true, required = true)
    @Excel(name = "ui.data.column.specifyMachine.sapCode")
    @ApiModelProperty(value = "SAP品号信息")
    private String sapCode;

    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.specifyMachine.column.machineName")
    private String machineName;

    /**
     * 线路类型
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路类型")
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
