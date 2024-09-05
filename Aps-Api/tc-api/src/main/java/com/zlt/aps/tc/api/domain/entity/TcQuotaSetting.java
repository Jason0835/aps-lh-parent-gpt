package com.zlt.aps.tc.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 胎侧定额设定对象 tc_quota_setting
 *
 * @author zlt
 * @date 2021-06-28
 */
@ApiModel(value = "胎侧定额设定对象", description = "胎侧定额设定对象 ")
public class TcQuotaSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_QUOTA_SETTING
     */
    private Long id;

    /**
     * 胎侧代码
     */
    @Excel(name = "ui.data.column.quota.sidewallCode")
    @ApiModelProperty(value = "胎侧代码")
    @ImportValidated(name = "ui.data.column.quota.sidewallCode", isCode = true, maxLength = 20)
    private String sidewallCode;

    /**
     * 机台id（对应T_TC_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.machine.machineName", importName = "ui.data.column.machine.machineCode")
    @ApiModelProperty(value = "机台名称")
    @ImportValidated(name = "ui.data.column.machine.machineCode", isCode = true, maxLength = 30)
    private String machineName;

    /**
     * 定额
     */
    @Excel(name = "ui.data.column.quota.quota")
    @ApiModelProperty(value = "定额")
    @ImportValidated(name = "ui.data.column.quota.quota", required = true, number = true, min = 0, max = 9999999)
    private BigDecimal quota;

    /**
     * 删除标识（0未删除；1已删除）
     */
    private String delFlag;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setSidewallCode(String sidewallCode) {
        this.sidewallCode = sidewallCode;
    }

    public String getSidewallCode() {
        return sidewallCode;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setQuota(BigDecimal quota) {
        this.quota = quota;
    }

    public BigDecimal getQuota() {
        return quota;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
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

    @Override
    public String toString() {
        return "TcQuotaSetting{" +
                "id=" + id +
                ", sidewallCode='" + sidewallCode + '\'' +
                ", machineId=" + machineId +
                ", quota=" + quota +
                ", delFlag='" + delFlag + '\'' +
                ", remark='" + remark + '\'' +
                ", machineName='" + machineName + '\'' +
                '}';
    }
}
