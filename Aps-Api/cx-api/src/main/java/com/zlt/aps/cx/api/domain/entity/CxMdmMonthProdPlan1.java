package com.zlt.aps.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 主计划月度生产计划对象 t_mdm_month_prod_plan
 * 
 * @author zlt
 * @date 2021-09-15
 */
@Data
@ApiModel(value = "主计划月度生产计划对象", description = "主计划月度生产计划对象 ")
public class CxMdmMonthProdPlan1 extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 物料编号 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.materialCode",sort = 2)
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 成型胎胚代码 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.embryoCode",sort = 3)
    @ApiModelProperty(value = "成型胎胚代码")
    private String embryoCode;

    /** 预计超欠产 */
    @Excel(name = "ui.data.column.mdmMonthProdPlan.expectedExcessArrears",sort = 30)
    @ApiModelProperty(value = "预计超欠产")
    private Long expectedExcessArrears;

}
