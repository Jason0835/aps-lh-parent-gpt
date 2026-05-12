package com.zlt.aps.mp.api.domain.vo;


import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AdjustsCxMachineVo {

    /** 成型机台 */
    @Excel(name = "ui.data.column.mpAdjustResult.cxMachineCode")
    @ApiModelProperty(value = "成型机台", name = "cxMachineCode")
    private String cxMachineCode;

    /** 产品结构 */
    @Excel(name = "ui.data.column.mpAdjustResult.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    private String structureName;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.beginDay")
    @ApiModelProperty(value = "开始日期", name = "beginDay")
    private Integer beginDay;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.endDay")
    @ApiModelProperty(value = "结束日期", name = "endDay")
    private Integer endDay;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.mpAdjustResult.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 排产计划版本
     */
    @Excel(name = "ui.data.column.mpAdjustResult.productionVersion")
    @ApiModelProperty(value = "排产计划版本", name = "productionVersion")
    private String productionVersion;

}
