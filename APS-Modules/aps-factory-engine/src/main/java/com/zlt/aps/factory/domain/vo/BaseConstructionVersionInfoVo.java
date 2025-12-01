package com.zlt.aps.factory.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 生胎基础施工信息
 *
 * @author ZLT
 * @date 20250708
 */
@Data
public class BaseConstructionVersionInfoVo implements Serializable {

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 1#胎体布代号
     */
    @ApiModelProperty(value = "1#胎体布代号")
    private String tireFabricCode1;

    /**
     * 2#胎体布代号
     */
    @ApiModelProperty(value = "2#胎体布代号")
    private String tireFabricCode2;

    /**
     * 3#胎体布代号
     */
    @ApiModelProperty(value = "3#胎体布代号")
    private String tireFabricCode3;

    /**
     * 胎体布层级数
     *
     * @return
     */
    public Integer getLayerLevelNumber() {
        Integer number = BigDecimal.ZERO.intValue();
        if (StringUtils.isNotBlank(tireFabricCode1)) {
            number = number + BigDecimal.ONE.intValue();
        }
        if (StringUtils.isNotBlank(tireFabricCode2)) {
            number = number + BigDecimal.ONE.intValue();
        }
        if (StringUtils.isNotBlank(tireFabricCode3)) {
            number = number + BigDecimal.ONE.intValue();
        }
        return number;
    }
}
