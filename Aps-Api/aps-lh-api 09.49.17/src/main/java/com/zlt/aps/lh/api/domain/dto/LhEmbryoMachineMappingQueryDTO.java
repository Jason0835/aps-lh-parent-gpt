package com.zlt.aps.lh.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 按工厂、排程日期和胎胚编号批量查询硫化机映射的请求对象。
 */
@Data
@ApiModel("胎胚硫化机映射查询参数")
public class LhEmbryoMachineMappingQueryDTO {

    /**
     * 工厂编号。
     */
    @ApiModelProperty(value = "工厂编号", required = true)
    private String factoryCode;

    /**
     * 排程日期。
     */
    @ApiModelProperty(value = "排程日期", required = true, example = "2026-10-12")
    private LocalDate scheduleDate;

    /**
     * 需要查询的胎胚编号集合。
     */
    @ApiModelProperty(value = "胎胚编号集合", required = true)
    private List<String> embryoCodes;
}
