package com.zlt.aps.monthplan.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 分厂月生产计划排产结果-列表查询对象Vo
 *
 * @author zlt
 * @date 2025-09-22
 */

@Data
@ApiModel(value = "分厂月生产计划排产结果-列表查询对象Vo", description = "分厂月生产计划排产结果-列表查询对象Vo")
public class MonthPlanProductionFinalResultVo extends FactoryMonthPlanProdFinal {

    /**
     * OE提报量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.oeOrderQty", cellType = Excel.ColumnType.NUMERIC, sort = 21)
    @ApiModelProperty(value = "OE提报量", name = "oeOrderQty")
    private Long oeOrderQty;
    /**
     * 内销提报量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.domesticOrderQty", cellType = Excel.ColumnType.NUMERIC, sort = 22)
    @ApiModelProperty(value = "内销提报量", name = "domesticOrderQty")
    private Long domesticOrderQty;
    /**
     * 外销提报量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.foreignOrderQty", cellType = Excel.ColumnType.NUMERIC, sort = 23)
    @ApiModelProperty(value = "外销提报量", name = "foreignOrderQty")
    private Long foreignOrderQty;
    /**
     * OE配套渠道量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.oeChannelQty", cellType = Excel.ColumnType.NUMERIC, sort = 24)
    @ApiModelProperty(value = "OE配套渠道量", name = "oeChannelQty")
    private Long oeChannelQty;
    /**
     * 内销途虎量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.domesticTfQty", cellType = Excel.ColumnType.NUMERIC, sort = 25)
    @ApiModelProperty(value = "内销途虎量", name = "domesticTfQty")
    private Long domesticTfQty;
    /**
     * 内销快准量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.domesticKzQty", cellType = Excel.ColumnType.NUMERIC, sort = 26)
    @ApiModelProperty(value = "内销快准量", name = "domesticKzQty")
    private Long domesticKzQty;
    /**
     * 内销RT量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.domesticRtQty", cellType = Excel.ColumnType.NUMERIC, sort = 27)
    @ApiModelProperty(value = "内销RT量", name = "domesticRtQty")
    private Long domesticRtQty;
    /**
     * 外销贴牌量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.foreignOemQty", cellType = Excel.ColumnType.NUMERIC, sort = 28)
    @ApiModelProperty(value = "外销贴牌量", name = "foreignOemQty")
    private Long foreignOemQty;
    /**
     * 外销非贴牌量
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.foreignNoOemQty", cellType = Excel.ColumnType.NUMERIC, sort = 29)
    @ApiModelProperty(value = "外销非贴牌量", name = "foreignNoOemQty")
    private Long foreignNoOemQty;
    /**
     * 标记信息
     */
    @Excel(name = "ui.data.column.monthPlanProductionFinalResult.markInfo", sort = 53)
    @ApiModelProperty(value = "标记信息", name = "markInfo")
    private String markInfo;
}
