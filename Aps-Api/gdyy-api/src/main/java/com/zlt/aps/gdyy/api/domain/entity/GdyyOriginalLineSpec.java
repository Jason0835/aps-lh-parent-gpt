package com.zlt.aps.gdyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 纤维压延原线规格对象 T_GDYY_ORIGINAL_LINE_SPEC
 * 
 * @date 2025-04-14
 */
@ApiModel(value = "钢带压延原线规格对象", description = "钢带压延原线规格对象 ")
@Data
@TableName("T_GDYY_ORIGINAL_LINE_SPEC")
@EqualsAndHashCode(callSuper = false)
public class GdyyOriginalLineSpec extends ApsBaseEntity {

	private static final long serialVersionUID = 1L;

	/** 主键ID，对应自增序列为：SEQ_PUBLIC */
	@ApiModelProperty(value = "id")
	private Long id;

	/** 原线规格 */
	@Excel(name = "ui.data.column.xwyy.spec.originalLineSpec")
	@ImportValidated(required = true, isCode = true, maxLength = 30)
	@ApiModelProperty(value = "原线规格")
	private String originalLineCode;

	/** 原线规格名称 */
	@Excel(name = "ui.data.column.xwyy.spec.originalLineName")
	@ImportValidated(required = true, maxLength = 100)
	@ApiModelProperty(value = "原线名称")
	private String originalLineName;

	/** 原线长度 */
	@Excel(name = "ui.data.column.xwyy.spec.originalLineLength")
	@ImportValidated(required = true, number = true, digits = true, min = 0, max = 999999)
	@ApiModelProperty(value = "原线长度")
	private String originalLineLength;

	/** 备注 */
	@Excel(name = "ui.common.column.remark")
	@ImportValidated(maxLength = 300)
	@ApiModelProperty(value = "备注")
	private String remark;
}
