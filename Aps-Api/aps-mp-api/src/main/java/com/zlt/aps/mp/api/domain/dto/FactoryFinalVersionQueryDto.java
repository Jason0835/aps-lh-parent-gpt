package com.zlt.aps.mp.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分厂版本信息查询条件对象
 *
 * @author ZLT
 * @data 20250527
 */
@Data
@ApiModel(value = "分厂版本信息查询条件对象", description = "分厂版本信息查询条件对象")
public class FactoryFinalVersionQueryDto implements Serializable {
    /**
     * 排产日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排产日", name = "productionDate")
    private Date productionDate;
    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 分厂编码
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    private String factoryCode;
}
