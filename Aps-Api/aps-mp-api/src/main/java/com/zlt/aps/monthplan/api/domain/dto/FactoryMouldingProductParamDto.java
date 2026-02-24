package com.zlt.aps.monthplan.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 排产计划切换硫化规格代码
 *
 * @author ZLT
 * @date 20250416
 */
@Data
@ApiModel(value = "从硫化排程中生产模具正在生产的品种参数对象", description = "从硫化排程中生产模具正在生产的品种参数对象")
public class FactoryMouldingProductParamDto implements Serializable {

    /**
     * 分厂编码
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 硫化排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "硫化排程日期", name = "vulcanizingDate")
    private Date vulcanizingDate;
}
