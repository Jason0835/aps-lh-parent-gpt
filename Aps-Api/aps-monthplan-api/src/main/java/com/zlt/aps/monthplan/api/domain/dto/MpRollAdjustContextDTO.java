package com.zlt.aps.monthplan.api.domain.dto;

import com.zlt.aps.monthplan.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

    @ApiModelProperty(value = "月度计划排产版本")
    private String productionVersion;

    @ApiModelProperty(value = "需求计划版本")
    private String monthPlanVersion;

    @ApiModelProperty(value = "产品品类")
    private String productType;
    /**
     * 调整类型 01-结构内，02-结构延长，03-结构缩短，04-新增结构
     */
    @ApiModelProperty(value = "调整类型")
    private String adjustType;

    @ApiModelProperty(value = "调整日")
    private Integer adjustDay;

    @ApiModelProperty(value = "开始日期")
    private Integer startDay;

    @ApiModelProperty(value = "结束日期")
    private Integer endDay;

    @ApiModelProperty(value = "调整开始日期")
    private Integer adjustStartDay;

    @ApiModelProperty(value = "调整结束日期")
    private Integer adjustEndDay;

    @ApiModelProperty(value = "锁定截止日")
    private Integer lockEndDay;

    @ApiModelProperty(value = "工厂编码")
    private String factoryCode;

    @ApiModelProperty(value = "版本号")
    private String version;

    @ApiModelProperty(value = "产品结构")
    private String structureName;

    @ApiModelProperty(value = "排产机台")
    private String scheduledMachines;

    @ApiModelProperty(value = "结构收尾日")
    private Integer structureDeadLine;

    @ApiModelProperty(value = "结构内调整记录")
    private List<MpAdjustStructureIn> mpAdjustStructureInList;

    @ApiModelProperty(value = "结构调整记录")
    private List<MpAdjustStructureOut> mpAdjustStructureOutList;

    @ApiModelProperty(value = "月计划调整最终结果表")
    private List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList;

    @ApiModelProperty(value = "月计划结构转产表")
    private List<MpStructureAllocation> structureAllocationList;

    @ApiModelProperty(value = "月计划结构转产表-单结构")
    private List<MpStructureAllocation> oneStructureAllocationList;

    @ApiModelProperty(value = "销售订单池列表")
    private List<SalesOrderPool> salesOrderPoolList;

    @ApiModelProperty(value = "排产版本列表")
    private List<MpFactoryProductionVersion> factoryProductionVersionList;

    @ApiModelProperty(value = "试制量试计划列表")
    private List<MpTrialPlan> mpTrialPlanList;

    @ApiModelProperty(value = "月底计划余量列表")
    private List<MdmMonthSurplus> mdmMonthSurplusesList;

    @ApiModelProperty(value = "实时成品库存列表")
    private List<MdmProductStock> mdmProductStockList;

    @ApiModelProperty(value = "月度硫化监控列表")
    private List<MpMonthPlanMonitor> mpMonthPlanMonitorList;

    @ApiModelProperty(value = "物料信息列表")
    private List<MdmMaterialInfo> mdmMaterialInfoList;

    @ApiModelProperty(value = "SKU日硫化产能列表")
    private List<MdmSkuLhCapacity> mdmSkuLhCapacityList;

    @ApiModelProperty(value = "SKU与结构关系列表")
    private List<MdmSkuStructureRef> mdmSkuStructureRefList;

    @ApiModelProperty(value = "SKU与施工（示方书）关系列表")
    private List<MdmSkuConstructionRef> mdmSkuConstructionRefList;

    @ApiModelProperty(value = "排程过程日志")
    private StringBuilder logDetail;

    @ApiModelProperty(value = "调整明细列表")
    private List<MpAdjustDetailVo> adjustDetailList;

    @ApiModelProperty(value = "调整结果列表")
    private List<MpAdjustResult> adjustResultList;

    @ApiModelProperty(value = "需求计划列表")
    private List<DpDemandPlan> dpDemandPlanList;

    @ApiModelProperty(value = "日产能限制Map")
    private Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap;

    @ApiModelProperty(value = "参数")
    private Map<String, Object> paramMap;

}
