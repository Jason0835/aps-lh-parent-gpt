package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 返回胶日返回率对象 t_fh_glue_return_rate
 * 
 * @author zlt
 * @date 2022-11-28
 */
@ApiModel(value = "返回胶日返回率对象", description = "返回胶日返回率对象 ")
@TableName("t_fh_glue_return_rate")
@KeySequence(value = "seq_t_fh_glue_return_rate", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class FhGlueReturnRate extends ZltBaseEntity {

	private static final long serialVersionUID = 1L;

	/** 主键ID，对应自增序列为：SEQ_T_FACTORY_GLUE_AREA_RELA */
	@ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_FH_GLUE_RETURN_RATE", position = 10)
	private Long id;

	/** 密炼区(对应数据字典code：MIX_AREA) */
	@Excel(name = "setting.fhGlueRate.mixArea", dictType = "MIX_AREA")
	@ImportValidated(name = "setting.fhGlueRate.mixArea", maxLength = 10)
	@ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
	private String mixArea;

	/** 胶料 */
	@Excel(name = "setting.fhGlueRate.glue")
	@ImportValidated(name = "setting.fhGlueRate.glue", maxLength = 30)
	@ApiModelProperty(value = "胶料", position = 30)
	private String glue;

	/** 返回率 */
	@Excel(name = "setting.fhGlueRate.returnRate")
	@ImportValidated(name = "setting.fhGlueRate.returnRate", number = true, min = 0, max = 9999)
	@ApiModelProperty(value = "返回率", position = 40)
	private Double returnRate;

	/** 备注 */
	@Excel(name = "setting.fhGlueRate.remark")
	@ImportValidated(name = "setting.fhGlueRate.remark", maxLength = 300)
	@ApiModelProperty(value = "备注", position = 50)
	private String remark;

}
