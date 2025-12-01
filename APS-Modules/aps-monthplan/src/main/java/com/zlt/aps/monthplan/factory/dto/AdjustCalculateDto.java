package com.zlt.aps.monthplan.factory.dto;

import com.zlt.aps.monthplan.api.domain.entity.MouldingProductionResultHelper;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalVersionInfoVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 调整计算辅助类
 *
 * @author ZLT
 * @date 20250331
 */
@Data
public class AdjustCalculateDto {
    /**
     * 还需增加的量
     */
    private Long sumAddQty;
    /**
     * 起始日期
     */
    private Integer beginDate;
    /**
     * 结束日期
     */
    private Integer endDay;
    /**
     * 排产模具
     */
    private Set<String> productionMouldSet;
    /**
     * 新模具增加
     */
    List<MouldingProductionResultHelper> addMouldList;
    /**
     * 调整起始日--只是用来传递
     */
    private Integer startDay;
    /**
     * 月份最大日--只是用来传递
     */
    private Integer maxDay;
    /**
     * 停工日列表--只是用来传递
     */
    private Set<Integer> stopDays;
    /**
     * 最大可用模具--只是用来传递
     */
    private Map<String, MouldProductRelationDto> maxEnableMouldMap;
    /**
     * 版本信息--只是用来传递
     */
    private FactoryMonthPlanFinalVersionInfoVo finalVersionInfo;
    /**
     * 单天单模最大硫化时间 --单位到秒--只是用来传递
     */
    private BigDecimal dayMaxCuringTime;

    /**
     * @param sumAddQty          还需增加的量
     * @param beginDate          调整起始日期
     * @param endDay             调整结束日期
     * @param productionMouldSet 排产模具
     * @param startDay           调整起始日--只是用来传递
     * @param maxDay             调整截止日--只是用来传递
     * @param stopDays           停工日列表--只是用来传递
     * @param maxEnableMouldMap  最大可用模具--只是用来传递
     */
    public AdjustCalculateDto(Long sumAddQty, Integer beginDate, Integer endDay, Set<String> productionMouldSet, Integer startDay, Integer maxDay, Set<Integer> stopDays, Map<String, MouldProductRelationDto> maxEnableMouldMap) {
        this.sumAddQty = sumAddQty;
        this.beginDate = beginDate;
        this.endDay = endDay;
        this.productionMouldSet = productionMouldSet;
        this.startDay = startDay;
        this.maxDay = maxDay;
        this.stopDays = stopDays;
        this.maxEnableMouldMap = maxEnableMouldMap;
    }
}
