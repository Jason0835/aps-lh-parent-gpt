package com.zlt.aps.tm.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

/**
 * 胎面定额设定对象 tm_quota_setting
 *
 * @author zlt
 * @date 2021-06-28
 */
@ApiModel(value = "胎面定额设定对象", description = "胎面定额设定对象 ")
public class TmQuotaSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_QUOTA_SETTING
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 胎面代码
     */
    @Excel(name = "ui.data.column.quota.treadCode")
    @ApiModelProperty(value = "胎面代码")
    @ImportValidated(name = "ui.data.column.quota.treadCode", isCode = true, maxLength = 20)
    private String treadCode;

    /**
     * 机台id（对应T_TM_MACHINE_INFO表id）
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
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
    /**
     * 删除标识（0未删除；1已删除）
     */
    private String delFlag;


    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTreadCode(String treadCode) {
        this.treadCode = treadCode;
    }

    public String getTreadCode() {
        return treadCode;
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
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("treadCode", getTreadCode())
                .append("machineId", getMachineId())
                .append("quota", getQuota())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .append("remark", getRemark())
                .toString();
    }
}
