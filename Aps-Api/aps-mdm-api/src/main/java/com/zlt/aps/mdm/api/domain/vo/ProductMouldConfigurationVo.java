package com.zlt.aps.mdm.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * SAP对应的匹配模具信息
 *
 * @author ZLT
 * @date 20250403
 */
@Data
@ApiModel(value = "SAP匹配到的模具信息对象", description = "SAP匹配到的模具信息对象 ")
public class ProductMouldConfigurationVo implements Serializable {
    /**
     * 模具
     */
    @ApiModelProperty(value = "模具", name = "mouldNo")
    private String mouldNo;
    /**
     * 对应的规格
     */
    @ApiModelProperty(value = "对应的规格", name = "specCodeList")
    private List<String> specCodeList;
}
