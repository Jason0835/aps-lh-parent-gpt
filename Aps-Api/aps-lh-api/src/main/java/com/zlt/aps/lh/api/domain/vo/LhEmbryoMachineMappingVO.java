package com.zlt.aps.lh.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 胎胚与硫化机编码映射结果。
 */
@Data
@ApiModel("胎胚硫化机映射")
public class LhEmbryoMachineMappingVO {

    /**
     * 胎胚编号。
     */
    @ApiModelProperty("胎胚编号")
    private String embryoCode;

    /**
     * 与该胎胚匹配的一台硫化机编码。
     */
    @ApiModelProperty("硫化机编码")
    private String lhMachineCode;
}
