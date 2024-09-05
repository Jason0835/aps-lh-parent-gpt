package com.zlt.aps.cx.engine.domain;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 成型损耗率设定对象 t_cx_loss_setting
 * 
 * @author Joran.zhang
 * @date 2021-06-29
 */
@Data
@ApiModel(value = "成型损耗率设定对象", description = "成型损耗率设定对象 ")
public class CxEngineLossSetting extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_LOSS_SETTING */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.lossRate.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /** 机台id（对应T_CX_MACHINE_INFO表id） */
    @Excel(name = "ui.data.column.lossRate.machineCode", readConverterExp = "成型机台编号")
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    /** 损耗率 */
    @Excel(name = "ui.data.column.lossRate.lossRate")
    @ApiModelProperty(value = "损耗率")
    private Double lossRate;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("embryoCode", getEmbryoCode())
            .append("machineCode", getMachineCode())
            .append("lossRate", getLossRate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .append("remark", getRemark())
            .toString();
    }

}
