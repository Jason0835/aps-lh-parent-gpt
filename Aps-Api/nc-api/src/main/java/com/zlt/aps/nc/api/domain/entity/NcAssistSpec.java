package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 内衬外协规格对象 t_nc_assist_spec
 * 
 * @author zlt
 * @date 2026-02-14
 */
@ApiModel(value = "内衬外协规格对象", description = "内衬外协规格对象 ")
@Data
@TableName("T_NC_ASSIST_SPEC")
@EqualsAndHashCode(callSuper = false)
public class NcAssistSpec extends ApsBaseEntity {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "工厂编码")
	@TableField(value = "FACTORY_CODE")
	private String factoryCode;

	@Excel(name = "ui.common.column.assist.nc.materialCode")
	@ImportValidated(required = true, isCode = true, maxLength = 30)
	@ApiModelProperty(value = "内衬代码")
	@TableField("MATERIAL_CODE")
	private String materialCode;

	/** 备注 */
	@Excel(name = "ui.common.column.remark")
	@ImportValidated(maxLength = 300)
	@ApiModelProperty(value = "备注")
	private String remark;
}
