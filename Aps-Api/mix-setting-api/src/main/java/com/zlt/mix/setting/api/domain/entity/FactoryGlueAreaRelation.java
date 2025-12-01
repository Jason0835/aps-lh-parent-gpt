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
 * 分厂胶料与密炼区对应关系对象 t_factory_glue_area_relation
 * 
 * @author zlt
 * @date 2022-11-22
 */
@ApiModel(value = "分厂胶料与密炼区对应关系对象", description = "分厂胶料与密炼区对应关系对象 ")
@TableName("t_factory_glue_area_relation")
@KeySequence(value = "seq_t_factory_glue_area_rela", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class FactoryGlueAreaRelation extends ZltBaseEntity {

	private static final long serialVersionUID = 1L;

	/** 主键ID，对应自增序列为：SEQ_T_FACTORY_GLUE_AREA_RELA */
	@ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_FACTORY_GLUE_AREA_RELA", position = 10)
	private Long id;

	/** 分厂(对应数据字典code：FACTORY) */
	@Excel(name = "setting.factoryGlueAreaRelation.factory", dictType = "FACTORY")
    @ImportValidated(name = "setting.factoryGlueAreaRelation.factory", required = true, maxLength = 10)
	@ApiModelProperty(value = "分厂(对应数据字典code：FACTORY)", position = 20)
	private String factory;

	/** 胶料 */
	@Excel(name = "setting.factoryGlueAreaRelation.glue")
    @ImportValidated(name = "setting.factoryGlueAreaRelation.glue", required = true, maxLength = 30)
	@ApiModelProperty(value = "胶料", position = 30)
	private String glue;

	/** 密炼区(对应数据字典code：MIX_AREA) */
	@Excel(name = "setting.factoryGlueAreaRelation.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.factoryGlueAreaRelation.mixArea", required = true, maxLength = 10)
	@ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 40)
	private String mixArea;
	
    /**
     * 备注
     */
    @Excel(name = "setting.factoryGlueAreaRelation.remark")
    @ImportValidated(name = "setting.safeStock.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 60)
    private String remark;
    
    /**
     * 胶料搜索条件，用于模糊查询
     */
    private String searchGlue;
}
