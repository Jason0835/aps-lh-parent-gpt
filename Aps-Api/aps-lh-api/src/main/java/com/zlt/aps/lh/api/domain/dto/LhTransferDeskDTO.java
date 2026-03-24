package com.zlt.aps.lh.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化转机台DTO
 * @date 2025/3/21
 */
@Data
public class LhTransferDeskDTO implements Serializable {


    private static final long serialVersionUID = 6200002603948382123L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "转硫化机台编号")
    @NotBlank(message = "转硫化机台编号不能为空")
    private String lhMachineCode;

    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;
}
