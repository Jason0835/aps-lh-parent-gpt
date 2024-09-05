package com.zlt.aps.lh.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 硫化机台信息对象 t_lh_machine_info
 *
 * @author zlt
 * @date 2021-05-28
 */
@ApiModel(value = "硫化机台信息对象", description = "硫化机台信息对象 ")
public class LhMachineInfo extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_PUBLIC
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号", position = 20)
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.machine.machineCode")
    private String machineCode;

    /**
     * 机台名称，比如：1线、2线
     */
    @ApiModelProperty(value = "机台名称", position = 30)
    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.machine.machineName")
    private String machineName;

    @ApiModelProperty(value = "生产寸口", position = 40)
    @ImportValidated(number = true, min = 0, max = 9999.99)
    @Excel(name = "ui.data.column.machine.dimension")
    private BigDecimal dimension;

    @ApiModelProperty(value = "生产寸口下限", position = 50)
    @ImportValidated(number = true, min = 0, max = 9999.99)
    @Excel(name = "ui.data.column.machine.dimensionMinmum")
    private BigDecimal dimensionMinmum;

    @ApiModelProperty(value = "生产寸口上限", position = 60)
    @ImportValidated(number = true, min = 0, max = 9999.99)
    @Excel(name = "ui.data.column.machine.dimensionMaximum")
    private BigDecimal dimensionMaximum;

    @ApiModelProperty(value = "向心机构", position = 70)
    @Excel(name = "ui.data.column.machine.centripetalMechanism", dictType = "CENTRIPETAL_MECHANISM")
    private String centripetalMechanism;

    /** 维护硫化机最大使用模具数量 */
    @ImportValidated(digits = true, min = 0, max = 10)
    @Excel(name = "ui.data.column.machine.maxMoldNum")
    @ApiModelProperty(value = "最大使用模具数量")
    private Long maxMoldNum;

    /**
     * 生产定额，是指单班一次能生产的量，单位：吨/班
     */
    @ApiModelProperty(value = "生产定额", position = 75)
    @ImportValidated(digits = true, min = 0, max = 9999999)
    @Excel(name = "ui.data.column.machine.quata")
    private BigDecimal quata;

    /**
     * 班制，如：三班制，两班制；对应数据字典CLASS_SHIFT
     */
    @ApiModelProperty(value = "班制", position = 80)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "CLASS_SHIFT")
    @ImportValidated(required = true)
    private String classShift;

    /**
     * 开机班次，如：中班、夜班；对应数据字典CLASS_NUM
     */
    @ApiModelProperty(value = "开机班次", position = 85)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "CLASS_NUM_THREE",dictTypeToExcelEnable = false)
    private String openMachineClass;

    /**
     * 机台状态，0--启用，1--禁用。对应数据字典STATUS
     */
    @ApiModelProperty(value = "机台状态", position = 90)
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
    private String status;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public BigDecimal getDimension() {
        return dimension;
    }

    public void setDimension(BigDecimal dimension) {
        this.dimension = dimension;
    }

    public BigDecimal getDimensionMinmum() {
        return dimensionMinmum;
    }

    public void setDimensionMinmum(BigDecimal dimensionMinmum) {
        this.dimensionMinmum = dimensionMinmum;
    }

    public BigDecimal getDimensionMaximum() {
        return dimensionMaximum;
    }

    public void setDimensionMaximum(BigDecimal dimensionMaximum) {
        this.dimensionMaximum = dimensionMaximum;
    }

    public String getCentripetalMechanism() {
        return centripetalMechanism;
    }

    public void setCentripetalMechanism(String centripetalMechanism) {
        this.centripetalMechanism = centripetalMechanism;
    }

    public BigDecimal getQuata() {
        return quata;
    }

    public void setQuata(BigDecimal quata) {
        this.quata = quata;
    }

    public Long getMaxMoldNum() {
        return maxMoldNum;
    }

    public void setMaxMoldNum(Long maxMoldNum) {
        this.maxMoldNum = maxMoldNum;
    }

    public String getClassShift() {
        return classShift;
    }

    public void setClassShift(String classShift) {
        this.classShift = classShift;
    }

    public String getOpenMachineClass() {
        return openMachineClass;
    }

    public void setOpenMachineClass(String openMachineClass) {
        this.openMachineClass = openMachineClass;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return "LhMachineInfo{" +
                "id=" + id +
                ", machineCode='" + machineCode + '\'' +
                ", machineName='" + machineName + '\'' +
                ", dimension=" + dimension +
                ", dimensionMinmum=" + dimensionMinmum +
                ", dimensionMaximum=" + dimensionMaximum +
                ", centripetalMechanism='" + centripetalMechanism + '\'' +
                ", maxMoldNum=" + maxMoldNum +
                ", quata=" + quata +
                ", classShift='" + classShift + '\'' +
                ", openMachineClass='" + openMachineClass + '\'' +
                ", status='" + status + '\'' +
                ", remark='" + remark + '\'' +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }
}
