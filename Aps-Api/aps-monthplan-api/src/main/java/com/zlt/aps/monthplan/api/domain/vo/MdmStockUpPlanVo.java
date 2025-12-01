package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.MdmStockUpPlan;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 备货计划--前端对象
 *
 * @author zlt
 * @version 1.0
 * @date 20250911
 */
@Data
@ApiModel(value = "备货计划列表查询结果Vo", description = "备货计划列表查询结果Vo")
public class MdmStockUpPlanVo extends MdmStockUpPlan {
    /**
     * 模具号
     */
    @ApiModelProperty(value = "模具号", name = "mouldNo")
    private String mouldNo;
    /**
     * 生胎号
     */
    @ApiModelProperty(value = "生胎号", name = "embryoCode")
    private String embryoCode;
}
