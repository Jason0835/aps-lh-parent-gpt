package com.zlt.aps.template.tq;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value="胎圈导入模板", description="胎圈导入模板")
public class TqMouthPlateTemp {

    @Excel(name = "ui.data.column.mouthPlateCode", sort = 10)
    @ApiModelProperty(value = "口型板编号。", position = 20)
    private String mouthPlateCode;

    @Excel(name = "ui.specifyMachine.column.machineName", sort = 20)
    @ApiModelProperty(value = "生产线", position = 50)
    private String machineName;

    @Excel(name = "ui.data.column.mouthPlateStatus", sort = 30, dictType = "STATUS")
    @ApiModelProperty(value = "状态，0--启用，1--禁用。", position = 40)
    private String status;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
