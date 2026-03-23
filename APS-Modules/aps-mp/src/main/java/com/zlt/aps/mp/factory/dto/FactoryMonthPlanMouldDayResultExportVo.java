package com.zlt.aps.mp.factory.dto;

import java.math.BigDecimal;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class FactoryMonthPlanMouldDayResultExportVo extends FactoryMonthPlanMouldDayResult {
    private static final long serialVersionUID = 1L;
    
    /**
     * 导出数据类型，1：明细记录，2：胎胚种类数，3：小计、4：总计
     */
    @ApiModelProperty(value = "导出数据类型", name = "dataType")
    private String dataType;
    
    /**
     * 中优先级净需求
     */
    @ApiModelProperty(value = "中优先级净需求", name = "midQty")
    private Integer midQty;

    /**
     * 周期储备需求
     */
    @ApiModelProperty(value = "周期储备需求", name = "cycleReserveQty")
    private Integer cycleReserveQty;

    /**
     * 内外销，数据字典：biz_stor_type
     */
    @ApiModelProperty(value = "内外销", name = "locationType")
    private String locationType;

    /**
     * 单胎重量
     */
    @ApiModelProperty(value = "单胎重量", name = "singleTireWeight")
    private BigDecimal singleTireWeight;

    /**
     * 上个月年份
     */
    @ApiModelProperty(value = "上个月年份", name = "lastYear")
    private Integer lastYear;

    /**
     * 上个月月份
     */
    @ApiModelProperty(value = "上个月月份", name = "lastMonth")
    private Integer lastMonth;

    /**
     * 上个月定稿版本
     */
    @ApiModelProperty(value = "上个月定稿版本", name = "lastProductionVersion")
    private String lastProductionVersion;

    /**
     * 上个月27号计划量
     */
    @ApiModelProperty(value = "上个月27号计划量", name = "last27")
    private Integer last27;

    /**
     * 上个月28号计划量
     */
    @ApiModelProperty(value = "上个月28号计划量", name = "last28")
    private Integer last28;

    /**
     * 上个月29号计划量
     */
    @ApiModelProperty(value = "上个月29号计划量", name = "last29")
    private Integer last29;

    /**
     * 上个月30号计划量
     */
    @ApiModelProperty(value = "上个月30号计划量", name = "last30")
    private Integer last30;

    /**
     * 上个月31号计划量
     */
    @ApiModelProperty(value = "上个月31号计划量", name = "last31")
    private Integer last31;
    
    /**
     * 上月末最后一天计划量
     */
    @ApiModelProperty(value = "上月末最后一天计划量", name = "lastDay1")
    private Integer lastDay1;
    
    /**
     * 上月末第二天计划量
     */
    @ApiModelProperty(value = "上月末第二天计划量", name = "lastDay2")
    private Integer lastDay2;
    
    /**
     * 实单未排产量
     */
    @ApiModelProperty(value = "实单未排产量", name = "actualOrderUnproduced")
    private Integer actualOrderUnproduced;
}
