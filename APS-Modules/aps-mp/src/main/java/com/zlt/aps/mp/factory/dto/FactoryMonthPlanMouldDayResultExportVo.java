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
     * 断面宽
     */
    @ApiModelProperty(value = "断面宽", name = "sectionWidth")
    private String sectionWidth;

    /**
     * 上个月定稿版本
     */
    @ApiModelProperty(value = "上个月定稿版本", name = "lastProductionVersion")
    private String lastProductionVersion;
    
    /**
     * 上月末最后一天计划量
     */
    @ApiModelProperty(value = "上月末最后1天计划量", name = "lastDay1")
    private Integer lastDay1;
    
    /**
     * 上月末倒数第2天计划量
     */
    @ApiModelProperty(value = "上月末倒数第2天计划量", name = "lastDay2")
    private Integer lastDay2;

    /**
     * 上月末倒数第3天计划量
     */
    @ApiModelProperty(value = "上月末倒数第3天计划量", name = "lastDay3")
    private Integer lastDay3;

    /**
     * 上月末倒数第4天计划量
     */
    @ApiModelProperty(value = "上月末倒数第4天计划量", name = "lastDay4")
    private Integer lastDay4;

    /**
     * 上月末倒数第5天计划量
     */
    @ApiModelProperty(value = "上月末倒数第5天计划量", name = "lastDay5")
    private Integer lastDay5;

    /**
     * 上月末倒数第6天计划量
     */
    @ApiModelProperty(value = "上月末倒数第6天计划量", name = "lastDay6")
    private Integer lastDay6;

    /**
     * 上月末倒数第7天计划量
     */
    @ApiModelProperty(value = "上月末倒数第7天计划量", name = "lastDay7")
    private Integer lastDay7;

    /**
     * 上月末倒数第8天计划量
     */
    @ApiModelProperty(value = "上月末倒数第8天计划量", name = "lastDay8")
    private Integer lastDay8;

    /**
     * 上月末倒数第9天计划量
     */
    @ApiModelProperty(value = "上月末倒数第9天计划量", name = "lastDay9")
    private Integer lastDay9;

    /**
     * 上月末倒数第10天计划量
     */
    @ApiModelProperty(value = "上月末倒数第10天计划量", name = "lastDay10")
    private Integer lastDay10;
    
    /**
     * 实单未排产量
     */
    @ApiModelProperty(value = "实单未排产量", name = "actualOrderUnproduced")
    private Integer actualOrderUnproduced;
    
    /**
     * 同主花纹最大型腔数量
     */
    @ApiModelProperty(value = "同主花纹最大型腔数量", name = "maxMouldCavityQty")
    private Integer maxMouldCavityQty;

    /**
     * 同主花纹最大活块数量
     */
    @ApiModelProperty(value = "同主花纹最大活块数量", name = "maxTypeBlockQty")
    private Integer maxTypeBlockQty;
    
    /**
     * 第1周调整量
     */
    @ApiModelProperty(value = "第1周调整量", name = "adjustQty1")
    private Integer adjustQty1;
    /**
     * 调整1排产量
     */
    @ApiModelProperty(value = "调整1排产量", name = "adjustProductQty1")
    private Integer adjustProductQty1;

    /**
     * 第2周调整量
     */
    @ApiModelProperty(value = "第2周调整量", name = "adjustQty2")
    private Integer adjustQty2;
    /**
     * 调整2排产量
     */
    @ApiModelProperty(value = "调整2排产量", name = "adjustProductQty2")
    private Integer adjustProductQty2;

    /**
     * 第3周调整量
     */
    @ApiModelProperty(value = "第3周调整量", name = "adjustQty3")
    private Integer adjustQty3;
    /**
     * 调整3排产量
     */
    @ApiModelProperty(value = "调整3排产量", name = "adjustProductQty3")
    private Integer adjustProductQty3;

    /**
     * 第4周调整量
     */
    @ApiModelProperty(value = "第4周调整量", name = "adjustQty4")
    private Integer adjustQty4;
    /**
     * 调整4排产量
     */
    @ApiModelProperty(value = "调整4排产量", name = "adjustProductQty4")
    private Integer adjustProductQty4;
    
    /**
     * 净需求(不含模具受限)
     */
    @ApiModelProperty(value = "净需求(不含模具受限)", name = "unRestrictedNetQty")
    private Integer unRestrictedNetQty;
    /**
     * 模具产能受限
     */
    @ApiModelProperty(value = "模具产能受限", name = "restrictedNetQty")
    private Integer restrictedNetQty;
    /**
     * 定稿生产实际排产量
     */
    @ApiModelProperty(value = "定稿生产实际排产量", name = "originalTotalQty")
    private Integer originalTotalQty;
    /**
     * 本月生产余量
     */
    @ApiModelProperty(value = "本月生产余量", name = "productSurplus")
    private Integer productSurplus;
    /**
     * 本月生产余量
     */
    @ApiModelProperty(value = "上月生产余量", name = "lastMonthRemainQty")
    private Integer lastMonthRemainQty;
    /**
     * 待调整
     */
    @ApiModelProperty(value = "待调整", name = "pendingQty")
    private Integer pendingQty;
}
