package com.zlt.aps.template.cd15;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value="钢压大卷信息导入模板", description="钢压大卷信息导入模板")
public class Cd15BigRollTemp {

    @ApiModelProperty(value = "钢压大卷编号")
    @Excel(name="ui.common.column.gy.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "布卷长度。此钢压大卷一卷的最大长度，单位：米。")
    @Excel(name="ui.bigRoll.column.clothLength")
    private BigDecimal clothLength;

    @ApiModelProperty(value = "折合生产条数。一卷大概能生产的胎胚数量，单位：条。")
    @Excel(name="ui.bigRoll.column.convertProduceNum")
    private Integer convertProduceNum;

    @ApiModelProperty(value = "实际卷取标准。此钢压大卷实际卷取的长度，单位：米。")
    @Excel(name="ui.bigRoll.column.actClothLength")
    private BigDecimal actClothLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name="ui.common.column.remark")
    private String remark;
}
