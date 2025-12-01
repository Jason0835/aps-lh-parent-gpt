package com.zlt.aps.monthplan.factory.helper;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.vo.AdjustNoticeSubtractPlanVo;
import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划调整-模具相关业务工具类
 *
 * @author ZLT
 * @date 20250608
 */
@Slf4j
public class AdjustMouldUtils {

    /**
     * 获取同模具排产计划中其他规格从adjustStartDay后有排产计划
     *
     * @param mouldNoPlannedProductionList 模具排产计划
     * @param addQtyInfo                   调增计划信息
     * @return
     */
    public static List<FactoryMonthPlanProdFinal> getNoSameProductCodeMouldNoProductionList(List<FactoryMonthPlanProdFinal> mouldNoPlannedProductionList, AddQtyAdjustPlanHelper addQtyInfo) {
        if (CollectionUtils.isEmpty(mouldNoPlannedProductionList)) {
            return Collections.emptyList();
        }
        String productCode = addQtyInfo.getProductCode();
        List<FactoryMonthPlanProdFinal> sameMouldNoOtherProductCodeList = mouldNoPlannedProductionList.stream().filter(mouldNoPlanned -> !productCode.equals(mouldNoPlanned.getProductCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameMouldNoOtherProductCodeList)) {
            return Collections.emptyList();
        }
        //开始调整天数--处于周期内的第几天
        Integer startDay = addQtyInfo.getStartAdjustDay();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        List<FactoryMonthPlanProdFinal> recommendList = sameMouldNoOtherProductCodeList.stream().filter(recommendPlan -> AdjustProductionUtils.getTotalProductionQty(recommendPlan, startDay, monthMaxDay) > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recommendList)) {
            return Collections.emptyList();
        }
        return recommendList;
    }

    /**
     * 校验增量是否超出其模具最大产能
     *
     * @param helper            施工信息
     * @param addQtyInfo        增量信息
     * @param stopDays          停工日
     * @param maxEnableMouldMap 配置的模具信息
     * @return
     */
    public static AjaxResult checkMouldMaxCapacity(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, Set<Integer> stopDays, Map<String, MouldProductRelationDto> maxEnableMouldMap) {
        Long totalQty = BigDecimal.ZERO.longValue();
        Long singleDayMaxQty = helper.getMaxSingleMouldQty();
        Integer startDay = addQtyInfo.getStartAdjustDay();
        Date productionStartDate = addQtyInfo.getProductionVersion().getProductionStartDate();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        for (Map.Entry<String, MouldProductRelationDto> mouldEntry : maxEnableMouldMap.entrySet()) {
            Long singleMouldQty = AdjustMouldUtils.getMouldMaxProductionQty(mouldEntry.getValue(), stopDays, singleDayMaxQty, startDay, monthMaxDay, productionStartDate);
            totalQty = totalQty + singleMouldQty;
        }
        Long addQty = addQtyInfo.getAddQty();
        if (addQty > totalQty) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.passMaxCapacityError");
            return AjaxResult.error(String.format(errorInfo, addQty, totalQty));
        }
        return AjaxResult.success();
    }

    /**
     * 获取模具从startProductionDay到endProductionQty最大可排产量
     *
     * @param mouldInfo           模具信息
     * @param stopDays            停工日
     * @param singleDayMaxQty     单幅模具单天最大量
     * @param startProductionDay  开始日
     * @param endProductionDay    结束日
     * @param productionStartDate 排产周期起始日
     * @return
     */
    public static Long getMouldMaxProductionQty(MouldProductRelationDto mouldInfo, Set<Integer> stopDays, Long singleDayMaxQty, Integer startProductionDay, Integer endProductionDay, Date productionStartDate) {
        if (null == mouldInfo || null == singleDayMaxQty) {
            return BigDecimal.ZERO.longValue();
        }
        Set<Integer> realStopDaySet = new HashSet<>();
        if (!CollectionUtils.isEmpty(stopDays)) {
            realStopDaySet.addAll(stopDays);
        }
        Set<Integer> noProductionSet = mouldInfo.getNoProductionDayByCycle(productionStartDate);
        if (null == noProductionSet) {
            noProductionSet = new HashSet<>();
        }
        Long maxQty = BigDecimal.ZERO.longValue();
        for (Integer productionDay = startProductionDay; productionDay <= endProductionDay; productionDay++) {
            //停工日
            if (realStopDaySet.contains(productionDay)) {
                continue;
            }
            //不可排产日--即维修
            if (noProductionSet.contains(productionDay)) {
                continue;
            }
            maxQty = maxQty + singleDayMaxQty;
        }
        return maxQty;
    }

    /**
     * 没有排产计划按SAP配置模具关系最大产能进行校验
     *
     * @param helper            施工信息
     * @param addQtyInfo
     * @param stopDays
     * @param maxEnableMouldMap
     * @return
     */
    public static Map<Integer, Long> getDayLeftOverQtyByEmptyMould(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, Set<Integer> stopDays, Map<String, MouldProductRelationDto> maxEnableMouldMap) {
        Map<Integer, Long> dayLeftOverQtyMap = new HashMap<>();
        Integer startAdjustDay = addQtyInfo.getStartAdjustDay();
        //排产版本周期起始日
        Date productionStartDate = addQtyInfo.getProductionVersion().getProductionStartDate();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        Long singleDayMaxQty = helper.getMaxSingleMouldQty();
        for (Integer day = startAdjustDay; day <= monthMaxDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            Long dayTotalDay = getMouldTotalQty(maxEnableMouldMap, day, singleDayMaxQty, productionStartDate);
            dayLeftOverQtyMap.put(day, dayTotalDay);
        }
        return dayLeftOverQtyMap;
    }

    /**
     * 获取模具剩余最大排产量，需要扣除已排产量
     *
     * @param helper                           施工信息
     * @param addQtyInfo                       增量信息
     * @param stopDays                         停工信息
     * @param mouldMap                         SAP配置的模具
     * @param productCodePlannedProductionList SAP已排产信息集合
     * @return
     */
    public static Map<Integer, Long> getDayLeftOverQtyByPlanned(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, Set<Integer> stopDays, Map<String, MouldProductRelationDto> mouldMap, List<FactoryMonthPlanProdFinal> productCodePlannedProductionList) {
        Map<Integer, Long> dayLeftOverDayMap = new HashMap<>();
        Integer startAdjustDay = addQtyInfo.getStartAdjustDay();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        Long singleDayMaxQty = helper.getMaxSingleMouldQty();
        Date productionStartDate = addQtyInfo.getProductionVersion().getProductionStartDate();
        for (Integer day = startAdjustDay; day <= monthMaxDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            //模具最大产能
            Long dayMaxQty = getMouldTotalQty(mouldMap, day, singleDayMaxQty, productionStartDate);
            //已排产量
            Long plannedQty = AdjustProductionUtils.totalProductionDayQty(productCodePlannedProductionList, day);
            Long dayLeftOverQty;
            if (plannedQty >= dayMaxQty) {
                dayLeftOverQty = BigDecimal.ZERO.longValue();
            } else {
                dayLeftOverQty = dayMaxQty - plannedQty;
            }
            dayLeftOverDayMap.put(day, dayLeftOverQty);
        }
        return dayLeftOverDayMap;
    }

    /**
     * 有共用模具，计算排产量
     *
     * @param helper                施工信息
     * @param addQtyInfo            增量信息
     * @param stopDays              停工日
     * @param mouldMap              SAP配置模具
     * @param allMouldMap           共用模具SAP配置的所有模具
     * @param plannedProductionList 模具排产计划
     * @return
     */
    public static Map<Integer, Long> getDayLeftOverBySharePlanned(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, Set<Integer> stopDays, Map<String, MouldProductRelationDto> mouldMap, Map<String, MouldProductRelationDto> allMouldMap, List<FactoryMonthPlanProdFinal> plannedProductionList) {
        Map<Integer, Long> dayLeftOverQtyMap = new HashMap<>();
        //提取无共用模具
        Map<String, MouldProductRelationDto> noShareMouldMap = new HashMap<>();
        allMouldMap.entrySet().stream().forEach(shareOtherEntry -> {
            String mouldCode = shareOtherEntry.getKey();
            if (!mouldMap.containsKey(mouldCode)) {
                noShareMouldMap.put(mouldCode, shareOtherEntry.getValue());
            }
        });
        Integer startAdjustDay = addQtyInfo.getStartAdjustDay();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        Long singleDayMaxQty = helper.getMaxSingleMouldQty();
        Date productionStartDate = addQtyInfo.getProductionVersion().getProductionStartDate();
        for (Integer day = startAdjustDay; day <= monthMaxDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            //本身模具最大产能
            Long dayMaxQty = getMouldTotalQty(mouldMap, day, singleDayMaxQty, productionStartDate);
            //非共用模具产能
            Long noShareMaxQty = getMouldTotalQty(noShareMouldMap, day, singleDayMaxQty, productionStartDate);
            //已排产量
            Long plannedQty = AdjustProductionUtils.totalProductionDayQty(plannedProductionList, day);
            //占用了共用模具产能
            Long shareMouldQty = BigDecimal.ZERO.longValue();
            if (plannedQty >= noShareMaxQty) {
                shareMouldQty = plannedQty - noShareMaxQty;
            }
            Long dayLeftOverDay;
            if (shareMouldQty >= dayMaxQty) {
                dayLeftOverDay = BigDecimal.ZERO.longValue();
            } else {
                dayLeftOverDay = dayMaxQty - shareMouldQty;
            }
            dayLeftOverQtyMap.put(day, dayLeftOverDay);
        }
        return dayLeftOverQtyMap;
    }

    /**
     * 获取模具在productionDay的最大模具产能
     *
     * @param mouldMap            模具信息
     * @param productionDay       排产日
     * @param singleDayMaxQty     单模产能
     * @param productionStartDate 开始周期日
     * @return
     */
    private static Long getMouldTotalQty(Map<String, MouldProductRelationDto> mouldMap, Integer productionDay, Long singleDayMaxQty, Date productionStartDate) {
        Long totalQty = BigDecimal.ZERO.longValue();
        if (CollectionUtils.isEmpty(mouldMap)) {
            return totalQty;
        }
        for (Map.Entry<String, MouldProductRelationDto> mouldEntry : mouldMap.entrySet()) {
            MouldProductRelationDto mouldInfo = mouldEntry.getValue();
            Set<Integer> noProductionSet = mouldInfo.getNoProductionDayByCycle(productionStartDate);
            if (noProductionSet.contains(productionDay)) {
                continue;
            }
            totalQty = totalQty + singleDayMaxQty;
        }
        return totalQty;
    }

    /**
     * 根据增量和最大剩余量，构建返回结果
     *
     * @param addQty 增量
     * @param maxQty 最大剩余量
     * @return
     */
    private static AjaxResult buildResult(Long addQty, Long maxQty) {
        if (addQty > maxQty) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.passMaxCapacityError");
            return AjaxResult.error(String.format(errorInfo, addQty, maxQty));
        }
        AdjustNoticeSubtractPlanVo plan = new AdjustNoticeSubtractPlanVo();
        plan.setLeftOverQty(maxQty);
        return AjaxResult.success(plan);
    }

    private AdjustMouldUtils() {

    }
}
