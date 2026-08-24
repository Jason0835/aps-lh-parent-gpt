package com.zlt.aps.tc.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎侧排程看板班次列。
 */
@Data
@ApiModel(value = "胎侧排程看板班次列")
public class TcScheduleBoardDateColumnVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 班次实际开始时间，用于前端判断班次是否已开始。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "班次实际开始时间")
    private Date shiftStartTime;

    /** 班次顺序。 */
    @ApiModelProperty(value = "班次顺序")
    private Integer shiftOrder;

    /** 班次编码。 */
    @ApiModelProperty(value = "班次编码")
    private String shiftCode;

    /** 班次名称。 */
    @ApiModelProperty(value = "班次名称")
    private String shiftName;

    /** 是否开班。 */
    @ApiModelProperty(value = "是否开班")
    private String openFlag;
}
