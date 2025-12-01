package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密炼机常用大规格设置对象 t_glue_common_demand
 * 
 * @author zlt
 * @date 2023-02-05
 */
@ApiModel(value = "密炼机常用大规格设置对象", description = "密炼机常用大规格设置对象 ")
@TableName("t_glue_common_demand")
@KeySequence(value = "seq_t_glue_common_demand", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueCommonDemand extends ZltBaseEntity {

	private static final long serialVersionUID = 1L;

	/** 主键ID，对应自增序列为：SEQ_GLUE_UNCLAIMED */
	@ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_GLUE_COMMON_DEMAND", position = 10)
	private Long id;
	
	/** 密炼区(对应数据字典code：MIX_AREA) */
	@Excel(name = "setting.glueCommonDemand.mixArea", dictType = "MIX_AREA")
	@ImportValidated(name = "setting.glueCommonDemand.mixArea", maxLength = 10, required = true)
	@ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
	private String mixArea;

	/**
	 * 机台名称，查询、导出用
	 */
	@Excel(name = "setting.glueCommonDemand.machineCode")
	@ImportValidated(name = "setting.glueCommonDemand.machineCode", required = true)
    @TableField(exist = false)
	private String machineName;
	
	/** 机台编号（对应T_MIX_MACHINE表编号） */
	@ApiModelProperty(value = "机台编号（对应T_MIX_MACHINE表编号）", position = 30)
	private String machineCode;
	
	/** 胶料 */
	@Excel(name = "setting.glueCommonDemand.glue")
	@ImportValidated(name = "setting.glueCommonDemand.glue", maxLength = 16, required = true)
	@ApiModelProperty(value = "胶料", position = 40)
	private String glue;
	
	/** 备注 */
	@Excel(name = "setting.glueCommonDemand.remark")
	@ImportValidated(name = "setting.glueCommonDemand.remark", maxLength = 300)
	@ApiModelProperty(value = "备注", position = 50)
	private String remark;
}
