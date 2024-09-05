package com.zlt.aps.xwyy.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 帘布大卷颜色提示信息表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
@Data
@ApiModel(value = "XwyyBigRollColor对象", description = "帘布大卷颜色提示信息表")
public class XwyyBigRollColorDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "帘布大卷编号")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.bigRollColor.column.bigRollCode", sort = 10)
    private String bigRollCode;

    @ApiModelProperty(value = "颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）")
    @ImportValidated(required = true)
    @Excel(name = "ui.bigRollColor.column.colorType", sort = 30, dictType = "BIG_ROLL_COLOR")
    private String colorType;

    @ApiModelProperty(value = "颜色代码，例如：#000000")
    @ImportValidated(colorCode = true, maxLength = 15)
    @Excel(name = "ui.bigRollColor.column.colorCode", sort = 20)
    private String colorCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "生效开始时间")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "生效结束时间")
    private Date endTime;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @ImportValidated(required = true)
    @Excel(name = "ui.bigRollColor.column.status", sort = 40, dictType = "STATUS")
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 50)
    private String remark;
}
