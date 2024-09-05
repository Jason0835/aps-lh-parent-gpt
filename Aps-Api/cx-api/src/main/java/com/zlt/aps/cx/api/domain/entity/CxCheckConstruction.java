package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 检测施工对象 T_CONSTRUCTION_CHECK
 *
 * @author Gim
 * @date 2022-02-25
 */
@ApiModel(value = "检测施工对象", description = "检测施工对象 ")
@Data
@TableName("T_CONSTRUCTION_CHECK")
@EqualsAndHashCode(callSuper = false)
@KeySequence(value = "SEQ_T_CONSTRUCTION_CHECK", clazz = Long.class)
public class CxCheckConstruction extends ApsBaseEntity {

	private static final long serialVersionUID = 1L;

	/** 主键ID，对应自增序列为：SEQ_T_CONSTRUCTION_CHECK */
	@ApiModelProperty(value = "id")
	private Long id;

	@JsonFormat(pattern = "yyyy-MM")
	@Excel(name = "ui.data.column.construction.check.month2", width = 30, dateFormat = "yyyy-MM")
	@ApiModelProperty(value = "计划月份")
	private Date planMonth;

	@Excel(name = "ui.data.column.construction.check.isComplete")
	@ApiModelProperty(value = "是否完整")
	private Integer isComplete;

	@Excel(name = "ui.data.column.construction.check.file")
	@ApiModelProperty(value = "文件名称")
	private String fileName;

	private String filePath;

	/**
	 * 文件字节数组
	 */
	private byte[] fileData;
}
