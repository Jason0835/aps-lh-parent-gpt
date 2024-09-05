package com.zlt.aps.gsq.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 钢丝圈定额设定对象 t_gsq_quota_setting
 *
 * @author zlt
 * @date 2021-06-29
 */
@ApiModel(value = "钢丝圈定额设定对象", description = "钢丝圈定额设定对象 ")
public class GsqQuotaSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_QUOTA_SETTING
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 钢丝圈代码
     */
    @Excel(name = "ui.data.column.quota.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码")
    @ImportValidated(isCode = true, maxLength = 20)
    private String steelRingCode;

    /**
     * 机台id（对应T_GSQ_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.machine.machineName", importName = "ui.data.column.machine.machineCode")
    @ApiModelProperty(value = "机台名称")
    @ImportValidated(maxLength = 30)
    private String machineName;

    /**
     * 定额
     */
    @Excel(name = "ui.data.column.quota.quota")
    @ApiModelProperty(value = "定额")
    @ImportValidated(required = true, number = true, min = 0, max = 9999999)
    private BigDecimal quota;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @ImportValidated(maxLength = 300)
    private String remark;


    private String delFlag;


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setSteelRingCode(String steelRingCode) {
        this.steelRingCode = steelRingCode;
    }

    public String getSteelRingCode() {
        return steelRingCode;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public BigDecimal getQuota() {
        return quota;
    }

    public void setQuota(BigDecimal quota) {
        this.quota = quota;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "GsqQuotaSetting{" +
                "id=" + id +
                ", steelRingCode='" + steelRingCode + '\'' +
                ", machineId=" + machineId +
                ", machineName='" + machineName + '\'' +
                ", quota=" + quota +
                ", remark='" + remark + '\'' +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }
}
