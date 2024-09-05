package com.zlt.aps.cd90.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 90°裁断机台信息对象 t_cd90_machine_info
 *
 * @author zlt
 * @date 2021-05-28
 */
@ApiModel(value = "90°裁断机台信息对象", description = "90°裁断机台信息对象 ")
public class Cd90MachineInfo extends ApsBaseEntity {
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
    @Excel(name = "ui.data.column.machine.machineCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String machineCode;

    /**
     * 机台名称，比如：1线、2线
     */
    @ApiModelProperty(value = "机台名称", position = 30)
    @Excel(name = "ui.data.column.machine.machineName")
    @ImportValidated(required = true, maxLength = 20)
    private String machineName;

    @ApiModelProperty(value = "是否支持贴胶片", position = 40)
    @Excel(name = "ui.data.column.machine.isStickFilm", dictType = "IS_SUPPORTED")
    @ImportValidated(maxLength = 6)
    private String isStickFilm;

    @ApiModelProperty(value = "帘布宽度（上限）", position = 75)
    @Excel(name = "ui.data.column.machine.clothWithMax")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal clothWithMax;

    @ApiModelProperty(value = "帘布宽度（下限）", position = 75)
    @Excel(name = "ui.data.column.machine.clothWithMin")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal clothWithMin;

    /**
     * 生产定额，是指单班一次能生产的量，单位：吨/班
     */
    @ApiModelProperty(value = "生产定额", position = 75)
    @Excel(name = "ui.data.column.machine.quata")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal quata;

    /**
     * 班制，如：三班制，两班制；对应数据字典CLASS_SHIFT
     */
    @ApiModelProperty(value = "班制", position = 80)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "CLASS_SHIFT")
    @ImportValidated(maxLength = 9 ,required = true)
    private String classShift;

    /**
     * 开机班次，如：中班、夜班；对应数据字典CLASS_NUM
     */
    @ApiModelProperty(value = "开机班次", position = 85)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "CLASS_NUM_THREE",dictTypeToExcelEnable = false)
    @ImportValidated(maxLength = 20)
    private String openMachineClass;

    /**
     * 机台状态，0--启用，1--禁用。对应数据字典STATUS
     */
    @ApiModelProperty(value = "机台状态", position = 90)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
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

    public String getIsStickFilm() {
        return isStickFilm;
    }

    public void setIsStickFilm(String isStickFilm) {
        this.isStickFilm = isStickFilm;
    }

    public BigDecimal getClothWithMax() {
        return clothWithMax;
    }

    public void setClothWithMax(BigDecimal clothWithMax) {
        this.clothWithMax = clothWithMax;
    }

    public BigDecimal getClothWithMin() {
        return clothWithMin;
    }

    public void setClothWithMin(BigDecimal clothWithMin) {
        this.clothWithMin = clothWithMin;
    }

    public BigDecimal getQuata() {
        return quata;
    }

    public void setQuata(BigDecimal quata) {
        this.quata = quata;
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
        return "Cd90MachineInfo{" +
                "id=" + id +
                ", machineCode='" + machineCode + '\'' +
                ", machineName='" + machineName + '\'' +
                ", isStickFilm='" + isStickFilm + '\'' +
                ", clothWithMax=" + clothWithMax +
                ", clothWithMin=" + clothWithMin +
                ", quata=" + quata +
                ", classShift='" + classShift + '\'' +
                ", openMachineClass='" + openMachineClass + '\'' +
                ", status='" + status + '\'' +
                ", remark='" + remark + '\'' +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }
}
