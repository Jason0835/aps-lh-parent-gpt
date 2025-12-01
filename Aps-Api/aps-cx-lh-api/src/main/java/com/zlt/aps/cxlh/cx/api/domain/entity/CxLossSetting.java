package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 成型损耗率设定
 * </p>
 *
 * @author chen
 * @since 2021-07-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CX_LOSS_SETTING")
@ApiModel(value = "CxLossSetting对象", description = "成型损耗率设定")
public class CxLossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LOSS_SETTING")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "成型机台编号")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @ApiModelProperty(value = "损耗率")
    @TableField("LOSS_RATE")
    private Double lossRate;

}
