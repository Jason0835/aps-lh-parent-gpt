package com.zlt.aps.tq.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 胎圈机台信息对象 t_tq_machine_info
 *
 * @author zlt
 * @date 2021-05-28
 */
@ApiModel(value = "胎圈机台信息对象", description = "胎圈机台信息对象 ")
public class TqMachineInfo extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_PUBLIC
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", position =20)
    @Excel(name = "ui.data.column.machine.machineCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String machineCode;

    /** 机台名称，比如：1线、2线 */
    @ApiModelProperty(value = "机台名称", position =30)
    @Excel(name = "ui.data.column.machine.machineName")
    @ImportValidated(required = true, maxLength = 20)
    private String machineName;

    /** 效率（每分钟） */
    @Excel(name = "ui.data.column.machine.efficiency")
    @ApiModelProperty(value = "效率", position =40)
    @ImportValidated(number = true, min = 0, max = 999999)
    private Double efficiency;

    /** 工装信息。 */
    @Excel(name = "ui.data.column.machine.toolingInfo")
    @ApiModelProperty(value = "工装信息", position =50)
    @ImportValidated(maxLength = 50)
    private String toolingInfo;

    /** 生产定额，是指单班一次能生产的量，单位：吨/班 */
    @ApiModelProperty(value = "生产定额", position =60)
    @Excel(name = "ui.data.column.machine.quata")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal quata;

    /** 班制，如：三班制，两班制；对应数据字典CLASS_SHIFT */
    @ApiModelProperty(value = "班制", position =80)
    @Excel(name = "ui.data.column.machine.classShift",dictType="CLASS_SHIFT")
    @ImportValidated(maxLength = 9,required = true)
    private String classShift;

    /** 开机班次，如：中班、夜班；对应数据字典CLASS_NUM */
    @ApiModelProperty(value = "开机班次", position =85)
    @Excel(name = "ui.data.column.machine.openMachineClass",dictType = "CLASS_NUM_THREE",dictTypeToExcelEnable = false)
    @ImportValidated(maxLength = 20)
    private String openMachineClass;

    /** 机台状态，0--启用，1--禁用。对应数据字典STATUS */
    @ApiModelProperty(value = "机台状态", position =90)
    @Excel(name = "ui.data.column.machine.status",dictType="STATUS")
    @ImportValidated(maxLength = 6 ,required = true)
    private String status;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;

    /**
     * 删除标识：0--正常，1-删除.对应数据字典DEL_FLAG
     */
    private String delFlag;

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getMachineName() {
        return machineName;
    }

    public Double getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(Double efficiency) {
        this.efficiency = efficiency;
    }

    public String getToolingInfo() {
        return toolingInfo;
    }

    public void setToolingInfo(String toolingInfo) {
        this.toolingInfo = toolingInfo;
    }

    public void setQuata(BigDecimal quata) {
        this.quata = quata;
    }

    public BigDecimal getQuata() {
        return quata;
    }

    public String getClassShift() {
        return classShift;
    }

    public void setClassShift(String classShift) {
        this.classShift = classShift;
    }

    public void setOpenMachineClass(String openMachineClass) {
        this.openMachineClass = openMachineClass;
    }

    public String getOpenMachineClass() {
        return openMachineClass;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
    }

    @Override
    public String toString() {
        return "TqMachineInfo{" +
                "id=" + id +
                ", machineCode='" + machineCode + '\'' +
                ", machineName='" + machineName + '\'' +
                ", efficiency=" + efficiency +
                ", toolingInfo='" + toolingInfo + '\'' +
                ", quata=" + quata +
                ", classShift='" + classShift + '\'' +
                ", openMachineClass='" + openMachineClass + '\'' +
                ", status='" + status + '\'' +
                ", remark='" + remark + '\'' +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }
}
