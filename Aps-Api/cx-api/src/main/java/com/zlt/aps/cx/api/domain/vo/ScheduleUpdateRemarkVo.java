package com.zlt.aps.cx.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 修改备注请求VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "修改备注请求对象")
public class ScheduleUpdateRemarkVo {

    @ApiModelProperty(value = "排程记录ID", required = true)
    private Long id;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "夜班原因分析")
    private String class1Analysis;

    @ApiModelProperty(value = "早班原因分析")
    private String class2Analysis;

    @ApiModelProperty(value = "中班原因分析")
    private String class3Analysis;

    @ApiModelProperty(value = "第4班原因分析")
    private String class4Analysis;

    @ApiModelProperty(value = "第5班原因分析")
    private String class5Analysis;

    @ApiModelProperty(value = "第6班原因分析")
    private String class6Analysis;

    @ApiModelProperty(value = "第7班原因分析")
    private String class7Analysis;

    @ApiModelProperty(value = "第8班原因分析")
    private String class8Analysis;
}
