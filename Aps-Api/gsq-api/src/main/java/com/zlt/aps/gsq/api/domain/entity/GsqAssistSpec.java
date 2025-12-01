package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 钢丝圈外协规格对象 t_gsq_assist_spec
 * 
 * @author zlt
 * @date 2022-02-14
 */
@ApiModel(value = "钢丝圈外协规格对象", description = "钢丝圈外协规格对象 ")
@Data
@TableName("t_gsq_assist_spec")
@EqualsAndHashCode(callSuper = false)
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class GsqAssistSpec extends ApsBaseEntity {

	private static final long serialVersionUID = 1L;

	/** 主键ID，对应自增序列为：SEQ_PUBLIC */
	@ApiModelProperty(value = "${comment}")
	private Long id;

	@Excel(name = "ui.common.column.assist.gsq.materialCode")
	@ImportValidated(required = true, isCode = true, maxLength = 30)
	@ApiModelProperty(value = "钢丝圈代码")
	private String materialCode;

	/** 删除标识：0--正常，1-删除 */
	@ApiModelProperty(value = "删除标识")
	private String delFlag;

	/** 备注 */
	@Excel(name = "ui.common.column.remark")
	@ImportValidated(maxLength = 300)
	@ApiModelProperty(value = "备注")
	private String remark;
}
