package com.zlt.aps.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 主计划月度生产计划对象 t_mdm_month_prod_plan
 * 
 * @author zlt
 * @date 2021-09-15
 */
@Data
@ApiModel(value = "主计划月度生产计划对象", description = "主计划月度生产计划对象 ")
public class CxMdmMonthProdPlan2 extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 物料编号 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.materialCode",sort = 2)
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 成型胎胚代码 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.embryoCode",sort = 3)
    @ApiModelProperty(value = "成型胎胚代码")
    private String embryoCode;

    /** 实际超欠产 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.actualOverProduction",sort = 35)
    @ApiModelProperty(value = "实际超欠产")
    private Long actualOverProduction;

}
