package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 定点机台配置列对象 t_specify_machine_list
 *
 * @author zlt
 * @date 2021-06-11
 */
@ApiModel(value = "定点机台配置对象", description = "定点机台配置对象 ")
public class CxMatchingSpecifyMachineList extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    /**
     * 定点机台主表ID
     */
    @ApiModelProperty(value = "定点机台主表ID", position = 20)
    private Long specifyMachineId;

    /**
     * 一下三个字段作为导出时用（胎胚代码,规格描述,SAP品号）
     */
    @Excel(name = "ui.data.column.cx.machine.embryoCode")
    private String embryoCode;
    @Excel(name = "ui.data.column.cx.machine.sap")
    private String sap;
    @Excel(name = "ui.data.column.cx.machine.specDesc")
    private String specDesc;


    /**
     * 工序数据维护在数据字典(PROCEDURE_CODE)：0-硫化，1-成型，2胎面，3-胎侧，4-内衬，5-胎圈，6-钢丝圈，7 -15度裁断，8-90度裁断，9-钢带压延，10-纤维压延
     */
    @ImportValidated(required = true, isCode = true)
    @Excel(name = "ui.data.column.cx.machine.procedureCode", dictType = "PROCEDURE_CODE")
    @ApiModelProperty(value = "工序", position = 30)
    private String procedureCode;

    /**
     * 机台id，可以直接从V_MACHINE_INFO视图，根据PROCEDURE_CODE和MACHINE_ID来关联不同工序的机台信息
     */
    @ImportValidated(required = true)
    @ApiModelProperty(value = "机台id", position = 40)
    private String machineId;

    /**
     * 机台code
     */
    @ApiModelProperty(value = "机台code", position = 50)
    private String machineCode;

    /**
     * 删除标识（0未删除；1已删除）
     */
    private String delFlag;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.cx.machine.machineName")
    @ImportValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "机台名称", position = 60)
    private String machineName;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 70)
    private String remark;


    public String getSap() {
        return sap;
    }

    public void setSap(String sap) {
        this.sap = sap;
    }

    public String getSpecDesc() {
        return specDesc;
    }

    public void setSpecDesc(String specDesc) {
        this.specDesc = specDesc;
    }

    public String getEmbryoCode() {
        return embryoCode;
    }

    public void setEmbryoCode(String embryoCode) {
        this.embryoCode = embryoCode;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSpecifyMachineId() {
        return specifyMachineId;
    }

    public void setSpecifyMachineId(Long specifyMachineId) {
        this.specifyMachineId = specifyMachineId;
    }

    public String getProcedureCode() {
        return procedureCode;
    }

    public void setProcedureCode(String procedureCode) {
        this.procedureCode = procedureCode;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
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
                .append("specifyMachineId", getSpecifyMachineId())
                .append("procedureCode", getProcedureCode())
                .append("machineId", getMachineId())
                .append("machineCode", getMachineCode())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .append("remark", getRemark())
                .toString();
    }
}
