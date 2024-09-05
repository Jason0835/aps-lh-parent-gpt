package com.zlt.aps.xwyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 纤维压延外协规格对象 t_xwyy_assist_spec
 * 
 * @author Joran.Zhang
 * @date 2022-05-10
 */
@ApiModel(value = "纤维压延大卷胶料车数关系表", description = "纤维压延大卷胶料车数关系对象 ")
@Data
@TableName("T_XWYY_BG_RUB_CAR_RELATION")
@EqualsAndHashCode(callSuper = false)
@KeySequence(value = "SEQ_XWYY_BG_RUB_CAR_RELATION", clazz = Long.class)
public class XwyyBigRollRubberCarRelation extends ApsBaseEntity {

	private static final long serialVersionUID = 1L;

	/** 主键ID，对应自增序列为：SEQ_PUBLIC */
	@ApiModelProperty(value = "id")
	private Long id;

	/** 帘布代码 */
	@Excel(name = "ui.data.column.carRelation.bigRollCode")
	@ImportValidated(required = true, isCode = true, maxLength = 30)
	@ApiModelProperty(value = "帘线大卷代码")
	private String bigRollCode;

	/** 胶料号 */
	@Excel(name = "ui.data.column.carRelation.rubberCode")
	@ImportValidated(required = true, isCode = true, maxLength = 30)
	@ApiModelProperty(value = "胶料号")
	private String rubberCode;

	/** 车数 */
	@Excel(name = "ui.data.column.carRelation.carNumber")
	@ImportValidated(required = true, isCode = true, maxLength = 30)
	@ApiModelProperty(value = "车数")
	private BigDecimal carNumber;

	/** 删除标识：0--正常，1-删除 */
	@ApiModelProperty(value = "删除标识")
	private String delFlag;

	/** 备注 */
	@Excel(name = "ui.common.column.remark")
	@ImportValidated(maxLength = 300)
	@ApiModelProperty(value = "备注")
	private String remark;
}
