package com.zlt.aps.tm.api.domain.dto;

import java.math.BigDecimal;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "胎面卷曲信息维护导入模板", description = "胎面卷曲信息维护导入模板")
public class TmCurlRollDto {

	private Long id;
	
    @ApiModelProperty(value = "胎面代码")
    @Excel(name = "ui.data.column.quota.treadCode")
    @ImportValidated(name = "ui.data.column.quota.treadCode", required = true, isCode = true, maxLength = 30)
    private String treadCode;

    @ApiModelProperty(value = "卷曲长度。胎面一卷的最大长度，单位：米。")
    @Excel(name = "ui.curlRoll.column.length")
    @ImportValidated(name = "ui.curlRoll.column.length", required = true)
    private String curlLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
