package com.zlt.aps.cx.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 转机台请求VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "转机台请求对象")
public class ScheduleTransferMachineVo {

    @ApiModelProperty(value = "排程记录ID列表", required = true)
    private List<Long> ids;

    @ApiModelProperty(value = "新机台编码", required = true)
    private String newMachineCode;

    @ApiModelProperty(value = "新机台名称")
    private String newMachineName;

    @ApiModelProperty(value = "是否确认转机台（产能不足时前端再次调用传入true）")
    private Boolean confirmed;
}
