package com.zlt.aps.cxlh.cx.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author xh
 * @version 1.0
 * @Description 成型转机台DTO
 * @date 2025/3/21
 */
@Data
public class CxTransferDeskDTO implements Serializable {

    private static final long serialVersionUID = 6560463746658765063L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "成型机台编号")
    @NotBlank(message = "转成型机台编号不能为空")
    private String cxMachineCode;

    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;
}
