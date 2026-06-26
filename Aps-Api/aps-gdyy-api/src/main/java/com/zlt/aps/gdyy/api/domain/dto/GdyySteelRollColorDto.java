package com.zlt.aps.gdyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 钢带大卷颜色提示信息表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-10
 */
@Data
@ApiModel(value="GdyySteelRollColor对象", description="钢带大卷颜色提示信息表")
public class GdyySteelRollColorDto extends ApsBaseDto implements Serializable{

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "钢压大卷代号")
    @Excel(name = "ui.steelRollColor.column.bigRollCode")
    @ImportValidated(name = "ui.steelRollColor.column.bigRollCode", required = true, isCode = true, maxLength = 20)
    private String bigRollCode;

    @ApiModelProperty(value = "颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）")
    @Excel(name = "ui.bigRollColor.column.colorType",dictType="BIG_ROLL_COLOR")
    @ImportValidated(name = "ui.bigRollColor.column.colorType", required = true, isCode = true, maxLength = 1)
    private String colorType;

    @ApiModelProperty(value = "颜色代码，例如：#000000")
    @Excel(name = "ui.bigRollColor.column.colorCode")
    @ImportValidated(colorCode = true, maxLength = 50)
    private String colorCode;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @ApiModelProperty(value = "生效开始时间")
//    @Excel(name = "ui.steelRollColor.column.startTime", dateFormat = "yyyy-MM-dd hh:mm:ss")
//    @ImportValidated(name = "ui.steelRollColor.column.startTime", date = true)
//    private Date startTime;
//
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @ApiModelProperty(value = "生效结束时间")
//    @Excel(name = "ui.steelRollColor.column.endTime", dateFormat = "yyyy-MM-dd hh:mm:ss")
//    @ImportValidated(name = "ui.steelRollColor.column.endTime", date = true)
//    private Date endTime;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @Excel(name = "ui.bigRollColor.column.status",dictType="STATUS")
    @ImportValidated(name = "ui.bigRollColor.column.status", required = true, isCode = true, maxLength = 1)
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
