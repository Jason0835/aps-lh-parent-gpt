package com.zlt.aps.monthplan.api.domain.dto;

import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustStructureInVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;
import java.util.List;

/**
 * @author Sandy
 * @version 1.0
 * @Description 周程滚动调整上下文对象
 * @date 2025/12/19
 */
@Data
public class MpRollAdjustContextDTO implements Serializable {

    private static final long serialVersionUID = 8736122348031246577L;

    @ApiModelProperty(value = "月度计划年份")
    private Integer mpYear;

    @ApiModelProperty(value = "月度计划月份")
    private Integer mpMonth;

    @ApiModelProperty(value = "月度计划年月")
    private Integer yearMonth;

    @ApiModelProperty(value = "月度计划版本")
    private String monthPlanVersion;

    /**
     * 调整类型 01-结构内，02-结构延长，03-结构缩短，04-新增结构
     */
    @ApiModelProperty(value = "调整类型")
    private String adjustType;

    @ApiModelProperty(value = "锁定截止日")
    private Integer lockEndDay;

    @ApiModelProperty(value = "工厂编码")
    private String factoryCode;

    @ApiModelProperty(value = "结构内调整记录")
    private List<MpAdjustStructureInVo> mpAdjustStructureInList;

    @ApiModelProperty(value = "月计划调整最终结果表")
    private List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList;

    @ApiModelProperty(value = "销售订单池列表")
    private List<SalesOrderPool> salesOrderPoolList;

    @ApiModelProperty(value = "排产版本列表")
    private List<FactoryProductionVersion> factoryProductionVersionList;

    @ApiModelProperty(value = "试制量试计划列表")
    private List<MpTrialPlan> mpTrialPlanList;

    @ApiModelProperty(value = "月底计划余量列表")
    private List<MdmMonthSurplus> mdmMonthSurplusesList;

    @ApiModelProperty(value = "实时成品库存列表")
    private List<MdmProductStock> mdmProductStockList;

    @ApiModelProperty(value = "月度硫化监控列表")
    private List<MpMonthPlanMonitor> mpMonthPlanMonitorList;

    @ApiModelProperty(value = "排程过程日志")
    private StringBuilder logDetail;


}
