package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 硫化机专供成型机配置实体。
 *
 * <p>定义硫化机台号到成型机台号的专供（转供）关系：部分硫化机有专供需求，
 * 只能（优先）由指定成型机供应胎胚。一个硫化机可配置多个专供成型机（多行记录），
 * 均衡分配时优先把该硫化机对应的任务分到专供成型机，专供机满负荷后可回退到其他成型机。
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_LH_MACHINE_SUPPLY")
@ApiModel(value = "硫化机专供成型机配置")
public class CxLhMachineSupplyConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "硫化机台号（去 L/R 后缀）")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "专供成型机台号")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
