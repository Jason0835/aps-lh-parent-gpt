package com.zlt.aps.monthplan.factory.helper;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanNeedAdjustPlanVo;
import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 计划调整-排产业务相关工具类
 *
 * @author ZLT
 * @date 20250608
 */
@Slf4j
public class AdjustProductionUtils {

    /**
     * 对调减计划进行校验，判断是否存在
     *
     * @param productionCycleDay         排产周期天数即最大天数
     * @param productionStartDate        排产周期起始日
     * @param subtractProductionList     调减计划信息
     * @param subtractProductionPlanList 原有计划集合
     * @return
     */
    public static AjaxResult checkSubtractPlanInfo(Integer productionCycleDay, Date productionStartDate, List<MonthPlanNeedAdjustPlanVo> subtractProductionList, List<FactoryMonthPlanProdFinal> subtractProductionPlanList) {
        if (CollectionUtils.isEmpty(subtractProductionPlanList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.data.noExistPlan"));
        }
        Map<String, FactoryMonthPlanProdFinal> subtractPlanMap = subtractProductionPlanList.stream().collect(Collectors.toMap(FactoryMonthPlanProdFinal::getProductionNo, Function.identity()));
        for (MonthPlanNeedAdjustPlanVo expectSubtract : subtractProductionList) {
            String productionNo = expectSubtract.getProductionNo();
            FactoryMonthPlanProdFinal originPlan = subtractPlanMap.get(productionNo);
            if (null == originPlan) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.data.noExistPlan"));
            }
            Date startSubtractDate = expectSubtract.getStartAdjustDate();
            Integer startDay = AdjustNoticeUtils.getDatePhaseDiff(productionStartDate, startSubtractDate);
            Long productionQty = getTotalProductionQty(originPlan, startDay, productionCycleDay);
            long needSubtractQty = Math.abs(expectSubtract.getNeedAdjustNumber());
            if (productionQty < needSubtractQty) {
                String dateFormat = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, startSubtractDate);
                String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.executeSubtract.subtractError");
                return AjaxResult.error(String.format(errorInfo, dateFormat, productionQty, needSubtractQty));
            }
        }
        return AjaxResult.success(subtractPlanMap);
    }

    /**
     * 对计划进行调减
     *
     * @param productionCycleDay 周期最大天数
     * @param productionStartDate 排产周期起始日
     * @param subtractInfo 调减计划信息
     * @param originPlan 原有计划信息
     */
    public static void subtractQtyByPlan(Integer productionCycleDay, Date productionStartDate, MonthPlanNeedAdjustPlanVo subtractInfo, FactoryMonthPlanProdFinal originPlan) {
        Date startSubtractDate = subtractInfo.getStartAdjustDate();
        Integer startDay = AdjustNoticeUtils.getDatePhaseDiff(productionStartDate, startSubtractDate);
        long needSubtractQty = Math.abs(subtractInfo.getNeedAdjustNumber());
        String fieldName;
        for (int day = startDay; day <= productionCycleDay; day++) {
            if (needSubtractQty <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            fieldName = String.format("day%d", day);
            //原有日排产量
            Long productionQty = (Long) originPlan.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            if (needSubtractQty >= productionQty) {
                originPlan.setFieldValueByFieldName(fieldName, BigDecimal.ZERO.longValue());
                needSubtractQty = needSubtractQty - productionQty;
            } else {
                originPlan.setFieldValueByFieldName(fieldName, productionQty - needSubtractQty);
                needSubtractQty = BigDecimal.ZERO.longValue();
            }
        }
        //实际调减量
        Long realSubtractQty = Math.abs(subtractInfo.getNeedAdjustNumber()) - needSubtractQty;
//        if (null != originPlan.getFactProdReqQty()) {
//            originPlan.setFactProdReqQty(originPlan.getFactProdReqQty() - realSubtractQty);
//        }
//        if (null != originPlan.getProdReqPlan()) {
//            originPlan.setProdReqPlan(originPlan.getProdReqPlan() - realSubtractQty);
//        }
//        if (null != originPlan.getTotalQty()) {
//            originPlan.setTotalQty(originPlan.getTotalQty() - realSubtractQty);
//        }
    }

    /**
     * 对需要调整的计划，重新设置值
     * 开始排产日--即上机日期
     * 结束排产日--即收尾日期
     * 排产量
     * 差异量
     *
     * @param needAdjustPlanList
     */
    public static void resetInfo(Integer maxDays, List<FactoryMonthPlanProdFinal> needAdjustPlanList) {
        if (CollectionUtils.isEmpty(needAdjustPlanList)) {
            return;
        }
        needAdjustPlanList.stream().forEach(needAdjustPlan -> {
            Integer startDay = null;
            Integer endDay = null;
            Long totalProductionQty = BigDecimal.ZERO.longValue();
            String fieldName;
            for (int day = FactoryConstant.MONTH_START_DAY; day <= maxDays; day++) {
                fieldName = String.format("day%d", day);
                //计划日排产量
                Long productionQty = (Long) needAdjustPlan.getFieldValueByFieldName(fieldName);
                if (null == productionQty || productionQty.equals(BigDecimal.ZERO.longValue())) {
                    continue;
                }
                if (null == startDay) {
                    startDay = day;
                } else {
                    startDay = Math.min(startDay, day);
                }
                if (null == endDay) {
                    endDay = day;
                } else {
                    endDay = Math.max(endDay, day);
                }
                totalProductionQty = totalProductionQty + productionQty;
            }
//            needAdjustPlan.setTotalQty(totalProductionQty);
            needAdjustPlan.setBeginDate(startDay);
            needAdjustPlan.setEndDay(endDay);
        });
    }

    /**
     * 获取计划集合，从startDay到endDay的总排产量
     *
     * @param productionPlanList 排产计划集合
     * @param startDay           开始日
     * @param endDay             结束日
     * @return
     */
    public static Long totalProductionQty(List<FactoryMonthPlanProdFinal> productionPlanList, Integer startDay, Integer endDay) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return BigDecimal.ZERO.longValue();
        }
        if (startDay > endDay) {
            return BigDecimal.ZERO.longValue();
        }
        Long totalProductionQty = BigDecimal.ZERO.longValue();
        for (FactoryMonthPlanProdFinal productionPlan : productionPlanList) {
            Long singleTotalQty = getTotalProductionQty(productionPlan, startDay, endDay);
            totalProductionQty = totalProductionQty + singleTotalQty;
        }
        return totalProductionQty;
    }

    /**
     * 获取在productionDay这天总的排产量
     *
     * @param productionPlanList 排产计划集合
     * @param productionDay      排产日
     * @return
     */
    public static Long totalProductionDayQty(List<FactoryMonthPlanProdFinal> productionPlanList, Integer productionDay) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return BigDecimal.ZERO.longValue();
        }
        Long totalQty = BigDecimal.ZERO.longValue();
        String fieldName = String.format("day%d", productionDay);
        for (FactoryMonthPlanProdFinal productionPlan : productionPlanList) {
            //原有日排产量
            Long productionQty = (Long) productionPlan.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            totalQty = totalQty + productionQty;
        }
        return totalQty;
    }

    /**
     * 增加日排产信息
     *
     * @param insertPlan        新增计划
     * @param helper            施工信息
     * @param addQtyInfo        调增信息
     * @param maxEnableMouldMap 模具信息
     * @param dayLimitQtyMap    日产能限制量
     * @param stopDays          停工日
     */
    public static void addDayProductionInfo(FactoryMonthPlanProdFinal insertPlan, AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, Map<String, MouldProductRelationDto> maxEnableMouldMap, Map<Integer, Long> dayLimitQtyMap, Set<Integer> stopDays) {
        Integer adjustStartDay = addQtyInfo.getStartAdjustDay();
        Integer monthDay = addQtyInfo.getMonthMaxDays();
        Set<String> mouldCodeSet = new HashSet<>();
        Long addQty = addQtyInfo.getAddQty();
        Long maxDayCapacityQty = helper.getMaxSingleMouldQty();
        Date productionStartDate = addQtyInfo.getProductionVersion().getProductionStartDate();
        Map<Integer, Long> productionDayQtyMap = new HashMap<>();
        Long totalProductionQty = BigDecimal.ZERO.longValue();
        for (Map.Entry<String, MouldProductRelationDto> mouldEntry : maxEnableMouldMap.entrySet()) {
            if (totalProductionQty >= addQty) {
                break;
            }
            MouldProductRelationDto mouldInfo = mouldEntry.getValue();
            Set<Integer> noProductionDaySet = mouldInfo.getNoProductionDayByCycle(productionStartDate);
            for (Integer day = adjustStartDay; day <= monthDay; day++) {
                if (totalProductionQty >= addQty) {
                    break;
                }
                if (stopDays.contains(day)) {
                    continue;
                }
                if (noProductionDaySet.contains(day)) {
                    continue;
                }
                mouldCodeSet.add(mouldEntry.getKey());
                int mouldSize = mouldCodeSet.size();
                //剩余还需排产量
                Long leftOverPendingProductionQty = addQty - totalProductionQty;
                Long leftOverLimitQty = dayLimitQtyMap.get(day);
                Long dayProductionQty = productionDayQtyMap.get(day);
                if (null == dayProductionQty) {
                    dayProductionQty = BigDecimal.ZERO.longValue();
                }
                leftOverLimitQty = leftOverLimitQty - dayProductionQty;
                //最模具最大产能和日产能限制中最小的值则为最大可排产量
                Long minCanProductionQty = Math.min(maxDayCapacityQty, leftOverLimitQty);
                //剩余排产量与最大可排产量中最小值为理论排产量
                Long theoryProductionQty = Math.min(minCanProductionQty, leftOverPendingProductionQty);
                Long dayTotalQty = theoryProductionQty + dayProductionQty;
                if (mouldSize % 2 == 0) {
                    Long doubleProductionQty = dayTotalQty / 2 * 2;
                    theoryProductionQty = theoryProductionQty - (dayTotalQty - doubleProductionQty);
                }
                //累计排产量
                totalProductionQty = totalProductionQty + theoryProductionQty;
                //累计天排产量
                dayProductionQty = dayProductionQty + theoryProductionQty;
                productionDayQtyMap.put(day, dayProductionQty);
            }
        }
        if (CollectionUtils.isEmpty(productionDayQtyMap)) {
            return;
        }
        insertPlan.setMouldQty(mouldCodeSet.size());
        String fieldName;
        Integer realStartDay = null;
        Integer realEndDay = null;
        for (Map.Entry<Integer, Long> dayProductionEntry : productionDayQtyMap.entrySet()) {
            Integer productionDay = dayProductionEntry.getKey();
            fieldName = String.format("day%d", productionDay);
            insertPlan.setFieldValueByFieldName(fieldName, dayProductionEntry.getValue());
            if (null == realStartDay) {
                realStartDay = productionDay;
            }
            if (null == realEndDay) {
                realEndDay = productionDay;
            }
            if (realStartDay > productionDay) {
                realStartDay = productionDay;
            }
            if (realEndDay < productionDay) {
                realEndDay = productionDay;
            }
        }
        insertPlan.setBeginDate(realStartDay);
        insertPlan.setEndDay(realEndDay);
//        insertPlan.setProdReqPlan(totalProductionQty);
//        insertPlan.setFactProdReqQty(totalProductionQty);
//        insertPlan.setTotalQty(totalProductionQty);
        BigDecimal totalCuringTime = helper.getCuringTime().multiply(BigDecimal.valueOf(totalProductionQty));
        insertPlan.setTotalVulcanizationMinutes(totalCuringTime.divide(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP));
    }

    /**
     * 计算从开始调整日到月末，限制的剩余产能
     *
     * @param stopDays              停工日
     * @param limitCapacityMap      每日最大限制产能集合
     * @param addQtyInfo            增量计划信息
     * @param plannedProductionList 排产计划集合
     * @param dayMaxMouldQty        单天最大模具产能
     * @return
     */
    public static Long getLeftOverLimitCapacity(Set<Integer> stopDays, Map<Integer, Long> limitCapacityMap, AddQtyAdjustPlanHelper addQtyInfo, List<FactoryMonthPlanProdFinal> plannedProductionList, Long dayMaxMouldQty) {
        if (CollectionUtils.isEmpty(limitCapacityMap)) {
            return BigDecimal.ZERO.longValue();
        }
        //得到已排产量
        Integer startDay = addQtyInfo.getStartAdjustDay();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        Map<Integer, Long> dayPlannedProductionQtyMap = getTotalQtyByDay(plannedProductionList, stopDays, startDay, monthMaxDay);
        //计算所有剩余排产量
        Long leftOverQty = BigDecimal.ZERO.longValue();
        for (Integer day = startDay; day <= monthMaxDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            //天限制产能最大值
            Long dayQty = limitCapacityMap.get(day);
            if (null == dayQty) {
                dayQty = BigDecimal.ZERO.longValue();
            }
            Long plannedQty = dayPlannedProductionQtyMap.get(day);
            if (null == plannedQty) {
                plannedQty = BigDecimal.ZERO.longValue();
            }
            if (dayQty <= plannedQty) {
                continue;
            }
            //扣除排产量
            Long dayLeftOverQty = dayQty - plannedQty;
            //不能超出模具最大产能
            leftOverQty = leftOverQty + Math.min(dayLeftOverQty, dayMaxMouldQty);
        }
        return leftOverQty;
    }

    /**
     * 根据配置，获取天产能限制量
     *
     * @param stopDays                    停工日
     * @param addQtyInfo                  增量计划
     * @param dayLimitCapacityMap         日总产能限制
     * @param sizeLimitCapacityMap        日寸口产能限制
     * @param dayLeftOverMouldCapacityMap 每日模具剩余产能限制
     * @param dayPlannedProductionList    日排产计划
     * @param sizePlannedProductionList   日寸口排产计划
     * @return
     */
    public static Map<Integer, Long> getDayLimitQty(Set<Integer> stopDays, AddQtyAdjustPlanHelper addQtyInfo, Map<Integer, Long> dayLimitCapacityMap, Map<Integer, Long> sizeLimitCapacityMap, Map<Integer, Long> dayLeftOverMouldCapacityMap, List<FactoryMonthPlanProdFinal> dayPlannedProductionList, List<FactoryMonthPlanProdFinal> sizePlannedProductionList) {
        Integer startDay = addQtyInfo.getStartAdjustDay();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        Map<Integer, Long> dayLimitQtyMap = new HashMap<>();
        Map<Integer, Long> dayPlannedProductionQtyMap = getTotalQtyByDay(dayPlannedProductionList, stopDays, startDay, monthMaxDay);
        Map<Integer, Long> daySizePlannedProductionQtyMap = getTotalQtyByDay(sizePlannedProductionList, stopDays, startDay, monthMaxDay);
        for (Integer day = startDay; day <= monthMaxDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            Long dayLimitQty = BigDecimal.ZERO.longValue();
            Long dayMaxLimitQty = dayLimitCapacityMap.get(day);
            if (null == dayMaxLimitQty) {
                dayMaxLimitQty = BigDecimal.ZERO.longValue();
            }
            Long dayPlannedQty = dayPlannedProductionQtyMap.get(day);
            if (null == dayPlannedQty) {
                dayPlannedQty = BigDecimal.ZERO.longValue();
            }
            if (dayMaxLimitQty >= dayPlannedQty) {
                dayLimitQty = dayMaxLimitQty - dayPlannedQty;
            }
            Long daySizeLimitQty = BigDecimal.ZERO.longValue();
            Long dayMaxSizeLimitQty = sizeLimitCapacityMap.get(day);
            if (null == dayMaxSizeLimitQty) {
                dayMaxSizeLimitQty = BigDecimal.ZERO.longValue();
            }
            Long daySizePlannedQty = daySizePlannedProductionQtyMap.get(day);
            if (null == daySizePlannedQty) {
                daySizePlannedQty = BigDecimal.ZERO.longValue();
            }
            if (dayMaxSizeLimitQty > daySizePlannedQty) {
                daySizeLimitQty = dayMaxSizeLimitQty - daySizePlannedQty;
            }
            Long minDayLimitQty = Math.min(dayLimitQty, daySizeLimitQty);
            Long mouldLimitQty = dayLeftOverMouldCapacityMap.get(day);
            dayLimitQtyMap.put(day, Math.min(minDayLimitQty, mouldLimitQty));
        }
        return dayLimitQtyMap;
    }

    /**
     * 获取计划集合，从startDay到endDay的总排产量
     *
     * @param productionPlanList 排产计划集合
     * @param startDay           开始日
     * @param endDay             结束日
     * @return
     */
    public static Map<Integer, Long> getTotalQtyByDay(List<FactoryMonthPlanProdFinal> productionPlanList, Set<Integer> stopDays, Integer startDay, Integer endDay) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return Collections.emptyMap();
        }
        if (startDay > endDay) {
            return Collections.emptyMap();
        }
        Map<Integer, Long> dayProductionQtyMap = new HashMap<>();
        for (Integer day = startDay; day <= endDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            Long dayTotalQty = getTotalQtyByDay(productionPlanList, day);
            dayProductionQtyMap.put(day, dayTotalQty);
        }
        return dayProductionQtyMap;
    }


    /**
     * 获取排产计划在startDay~endDay之间的排产量
     *
     * @param productionPlan 排产计划
     * @param startDay       其实排产日
     * @param endDay         结束排产日
     * @return
     */
    public static Long getTotalProductionQty(FactoryMonthPlanProdFinal productionPlan, Integer startDay, Integer endDay) {
        if (null == productionPlan) {
            return BigDecimal.ZERO.longValue();
        }
        if (startDay > endDay) {
            return BigDecimal.ZERO.longValue();
        }
        Long totalQty = BigDecimal.ZERO.longValue();
        String fieldName;
        for (int day = startDay; day <= endDay; day++) {
            fieldName = String.format("day%d", day);
            //原有日排产量
            Long productionQty = (Long) productionPlan.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            totalQty = totalQty + productionQty;
        }
        return totalQty;
    }

    /**
     * 得到排产日的总排产量
     *
     * @param productionPlanList 排产集合
     * @param productionDate     排产日
     * @return
     */
    private static Long getTotalQtyByDay(List<FactoryMonthPlanProdFinal> productionPlanList, Integer productionDate) {
        if (null == productionDate) {
            return BigDecimal.ZERO.longValue();
        }
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return BigDecimal.ZERO.longValue();
        }
        String fieldName = String.format("day%d", productionDate);
        Long totalQty = BigDecimal.ZERO.longValue();
        for (FactoryMonthPlanProdFinal plannedPlan : productionPlanList) {
            //原有日排产量
            Long productionQty = (Long) plannedPlan.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            totalQty = totalQty + productionQty;
        }
        return totalQty;
    }

    private AdjustProductionUtils() {

    }
}
