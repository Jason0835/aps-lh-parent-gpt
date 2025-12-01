package com.zlt.aps.nc.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "内衬卷曲信息维护导入模板", description = "内衬卷曲信息维护导入模板")
public class NcCurlRollDto extends BaseEntity {
	
	private Long id;

    @ApiModelProperty(value = "内衬代码")
    @Excel(name = "ui.data.column.quota.liningCode")
    @ImportValidated(name = "ui.data.column.quota.liningCode", required = true, isCode = true, maxLength = 30)
    private String liningCode;

    @ApiModelProperty(value = "卷曲长度。内衬一卷的最大长度，单位：米。")
    @Excel(name = "ui.curlRoll.column.length")
    @ImportValidated(name = "ui.curlRoll.column.length", required = true)
    private String curlLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
