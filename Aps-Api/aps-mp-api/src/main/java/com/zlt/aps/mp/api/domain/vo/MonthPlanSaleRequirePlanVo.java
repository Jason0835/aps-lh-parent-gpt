package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 生成月度销售需求计划条件对象
 *
 * @author ZLT
 * @date 20250217
 */
@Data
@ApiModel(value = "生成月度销售需求计划条件对象", description = "生成月度销售需求计划条件对象 ")
public class MonthPlanSaleRequirePlanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编码
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    private String factoryCode;

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
}
