package com.zlt.aps.xwyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 纤维压延原线规格对象 T_XWYY_ORIGINAL_LINE_SPEC
 * 
 * @author zlt
 * @date 2022-02-14
 */
@ApiModel(value = "纤维压延原线规格对象", description = "纤维压延原线规格对象 ")
@Data
@TableName("T_XWYY_ORIGINAL_LINE_SPEC")
@EqualsAndHashCode(callSuper = false)
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class XwyyOriginalLineSpec extends ApsBaseEntity {

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

	/** 原线长度 */
//	@Excel(name = "ui.data.column.xwyy.spec.breakRollNum")
//	@ImportValidated(number = true, digits = true, min = 0, max = 999999)
	@TableField(value = "BREAK_ROLL_NUM", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	@ApiModelProperty(value = "原线可破卷数")
	private Integer breakRollNum;

	/** 备注 */
	@Excel(name = "ui.common.column.remark")
	@ImportValidated(maxLength = 300)
	@ApiModelProperty(value = "备注")
	private String remark;

	/**
	 * 查询编号，用于精确查询
	 */
	@ApiModelProperty(value = "查询编号，用于精确查询")
	@TableField(exist = false)
	private String queryCode;
}
