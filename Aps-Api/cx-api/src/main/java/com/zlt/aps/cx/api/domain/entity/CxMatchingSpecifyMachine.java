package com.zlt.aps.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * 定点机台对象 t_specify_machine
 *
 * @author zlt
 * @date 2021-06-11
 */
@ApiModel(value = "定点机台对象", description = "定点机台对象")
public class CxMatchingSpecifyMachine extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 项目描述
     */
    @ImportValidated(maxLength = 200)
    @Excel(name = "ui.data.column.cx.machine.projectDesc",sort = 6)
    @ApiModelProperty(value = "项目描述", position = 40)
    private String projectDesc;

    /**
     * SAP品号(不需要外SAP+胎胚确定唯一，SAP品号可以为空)
     */
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.data.column.cx.machine.sap",sort = 2)
    @ApiModelProperty(value = "SAP品号", position = 20)
    private String sap;

    /**
     * 规格描述
     */
    @ImportValidated(maxLength = 50)
    @Excel(name = "ui.data.column.cx.machine.specDesc",sort = 3,type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "规格描述", position = 30)
    private String specDesc;

    /**
     * 胎胚代码(不允许为空)
     */
    @ImportValidated(maxLength = 20, isCode = true, required = true)
    @Excel(name = "ui.data.column.cx.machine.embryoCode",sort = 1)
    @ApiModelProperty(value = "胎胚代码", position = 10)
    private String embryoCode;

    /**
     * 线路，数据维护在数据字典：0-生产线、1-备用线
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.cx.machine.lineType", dictType = "LINE_TYPE",sort = 4)
    @ApiModelProperty(value = "线路", position = 50)
    private String lineType;

    /**
     * 作业类型，数据维护在数据字典：0-限制作业；1-不可作业
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.cx.machine.jobType", dictType = "JOB_TYPE",sort = 5)
    @ApiModelProperty(value = "作业类型", position = 60)
    private String jobType;

    /**
     * 删除标识（0未删除；1已删除）
     */
    private String delFlag;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark",sort = 7)
    @ApiModelProperty(value = "备注", position = 70)
    private String remark;
    /**
     * 定点机台配置列信息
     */
    private List<CxMatchingSpecifyMachineList> tSpecifyMachineListList;

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

    public String getProjectDesc() {
        return projectDesc;
    }

    public void setProjectDesc(String projectDesc) {
        this.projectDesc = projectDesc;
    }

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

    public List<CxMatchingSpecifyMachineList> getTSpecifyMachineListList() {
        return tSpecifyMachineListList;
    }

    public void setTSpecifyMachineListList(List<CxMatchingSpecifyMachineList> tSpecifyMachineListList) {
        this.tSpecifyMachineListList = tSpecifyMachineListList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("projectDesc", getProjectDesc())
                .append("sap", getSap())
                .append("specDesc", getSpecDesc())
                .append("embryoCode", getEmbryoCode())
                .append("lineType", getLineType())
                .append("jobType", getJobType())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .append("remark", getRemark())
                .append("tSpecifyMachineListList", getTSpecifyMachineListList())
                .toString();
    }
}
