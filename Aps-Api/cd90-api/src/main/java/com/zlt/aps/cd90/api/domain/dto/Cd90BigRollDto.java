package com.zlt.aps.cd90.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 帘布大卷信息维护表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-09
 */
@Data
@ApiModel(value = "Cd90BigRoll对象", description = "帘布大卷信息维护表")
public class Cd90BigRollDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "帘布大卷编号")
    @Excel(name = "ui.common.column.lb.bigRollCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String bigRollCode;

    @ApiModelProperty(value = "布卷长度。此帘布大卷一卷的最大长度，单位：米。")
    @Excel(name = "ui.bigRoll.column.clothLength")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal clothLength;

    @ApiModelProperty(value = "折合生产条数。一卷大概能生产的胎胚数量，单位：条。")
    @Excel(name = "ui.bigRoll.column.convertProduceNum")
    @ImportValidated(digits = true, min = 0, max = 999999)
    private Integer convertProduceNum;

    @ApiModelProperty(value = "实际卷取标准。此帘布大卷实际卷取的长度，单位：米。")
    @Excel(name = "ui.bigRoll.column.actClothLength")
    @ImportValidated(required = true, number = true, min = 0, max = 999999)
    private BigDecimal actClothLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
