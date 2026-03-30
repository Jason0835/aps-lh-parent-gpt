package com.zlt.aps.lh.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化规格代码参数接受
 * @date 2025/3/20
 */
@Data
public class LhSpecCodeParamDTO implements Serializable {


    private static final long serialVersionUID = 2596693985833561181L;

    @ApiModelProperty(value = "规格代码")
    private String specCode;


    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

}
