package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.FormingMethodTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.maindata.domain.vo.DaySizeCapacityVo;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.maindata.service.IMdmProductionCalendarService;
import com.zlt.aps.maindata.service.ISizeCapacityConfigurationService;
import com.zlt.aps.maindata.utils.SizeCapacityUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionCalendar;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import com.zlt.aps.monthplan.factory.helper.*;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanAdjustMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanAdjustPlanBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 月计划-计划调整业务辅助实现类
 *
 * @author ZLT
 * @date 20250607
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryMonthPlanAdjustPlanBusinessServiceImpl implements IFactoryMonthPlanAdjustPlanBusinessService {

    private final IMdmProductConstructionService mdmProductConstructionService;

    private final FactoryMonthPlanAdjustMapper factoryMonthPlanAdjustMapper;

    private final FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    private final IMdmProductionCalendarService productionCalendarService;

    private final ISizeCapacityConfigurationService sizeCapacityConfigurationService;
    /**
     * 字符分隔符
     */
    private final static String PRODUCT_SPLIT = "\\|\\*\\|";

    @Override
    public AjaxResult checkMaxQtyByStartDate(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo) {
        FactoryProductionVersion productionVersion = addQtyInfo.getProductionVersion();
        String mouldNo = addQtyInfo.getMouldNo();
        String productCode = addQtyInfo.getProductCode();
        BigDecimal proSize = addQtyInfo.getProSize();
        if (null == proSize || StringUtils.isBlank(productCode) || StringUtils.isBlank(mouldNo)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustDate.lackMessage"));
        }
        FactoryMonthPlanFinalVersionInfoVo finalVersion = BeanCopyUtils.copyBean(productionVersion, FactoryMonthPlanFinalVersionInfoVo.class);
        Set<Integer> stopDays = getStopDays(finalVersion);
        Integer startAdjustDay = addQtyInfo.getStartAdjustDay();
        if (stopDays.contains(startAdjustDay)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustDate.startDayHasStop"));
        }
        String factoryCode = productionVersion.getFactoryCode();
        Integer year = productionVersion.getYear();
        Integer month = productionVersion.getMonth();
        List<SizeCapacityConfiguration> configurationList = sizeCapacityConfigurationService.getConfigurationByFactoryYearAndMonth(factoryCode, year, month);
        if (CollectionUtils.isEmpty(configurationList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.noSizeCapacityError"));
        }
        Map<String, MouldProductRelationDto> maxEnableMouldMap = getMaxEnableMould(productionVersion, productCode, mouldNo);
        if (CollectionUtils.isEmpty(maxEnableMouldMap)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.noMouldInfoError"));
        }
        AjaxResult checkQtyResult = AdjustMouldUtils.checkMouldMaxCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkQtyResult.get(AjaxResult.CODE_TAG)) {
            return checkQtyResult;
        }
        //获取SAP共用模具的其它SAP配置的模具
        Map<String, MouldProductRelationDto> monthShameOtherMap = getMonthMaxEnableMouldByMouldNo(productionVersion, productCode, mouldNo);
        List<FactoryMonthPlanProdFinal> plannedProductionList = getVersionProductionList(productionVersion);
        //获取mouldNo已排产量及其模具最大产能量-比较模具产能能否满足增量
        List<FactoryMonthPlanProdFinal> mouldNoPlannedProductionList = plannedProductionList.stream().filter(plannedPlan -> mouldNo.equals(plannedPlan.getMouldNo())).collect(Collectors.toList());
        Map<Integer, Long> dayLeftOverMouldCapacityMap = getMouldDayLeftOverCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap, monthShameOtherMap, mouldNoPlannedProductionList);
        Long mouldLeftOverCapacityQty = getMouldLeftOverCapacity(dayLeftOverMouldCapacityMap);
        //计算天产能分配和寸口的天产能分配
        Map<Integer, Long> dayMaxCapacityMap = new HashMap<>();
        Map<Integer, Long> daySizeMaxCapacityMap = new HashMap<>();
        calculateDayCapacity(configurationList, stopDays, addQtyInfo.getMonthMaxDays(), dayMaxCapacityMap, daySizeMaxCapacityMap, proSize);
        Long addQty = addQtyInfo.getAddQty();
        //寸口排产计划
        List<FactoryMonthPlanProdFinal> sizePlannedProductionList = plannedProductionList.stream().filter(plannedPlan -> proSize.equals(plannedPlan.getProSize())).collect(Collectors.toList());
        //综合产能限制，则先推同寸口计划，没有再推所有
        Map<Integer, Long> dayLimitQtyMap = AdjustProductionUtils.getDayLimitQty(stopDays, addQtyInfo, dayMaxCapacityMap, daySizeMaxCapacityMap, dayLeftOverMouldCapacityMap, plannedProductionList, sizePlannedProductionList);
        Long dayLimitQtySum = getTotalDayLimitQty(dayLimitQtyMap);
        if (dayLimitQtySum < addQty) {
            List<FactoryMonthPlanProdFinal> otherPlan = sizePlannedProductionList.stream().filter(plannedPlan -> !productCode.equals(plannedPlan.getProductCode())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(otherPlan)) {
                otherPlan = plannedProductionList.stream().filter(plannedPlan -> !productCode.equals(plannedPlan.getProductCode())).collect(Collectors.toList());
            }
            AdjustNoticeSubtractPlanVo result = getRecommendPlan(otherPlan, addQtyInfo, BigDecimal.ZERO.longValue());
            return AjaxResult.success(result);
        }
        //单日最大模具产能
        Long dayMaxMouldQty = helper.getMaxSingleMouldQty() * maxEnableMouldMap.size();
        //计算寸口剩余产能
        Long sizeLeftOverQty = AdjustProductionUtils.getLeftOverLimitCapacity(stopDays, daySizeMaxCapacityMap, addQtyInfo, sizePlannedProductionList, dayMaxMouldQty);
        //计算天剩余产能
        Long dayLeftOverQty = AdjustProductionUtils.getLeftOverLimitCapacity(stopDays, dayMaxCapacityMap, addQtyInfo, plannedProductionList, dayMaxMouldQty);
        //天产能不够，寸口产能够--推荐所有
        if (dayLeftOverQty < addQty && sizeLeftOverQty >= addQty) {
            List<FactoryMonthPlanProdFinal> otherPlan = plannedProductionList.stream().filter(plannedPlan -> !productCode.equals(plannedPlan.getProductCode())).collect(Collectors.toList());
            AdjustNoticeSubtractPlanVo result = getRecommendPlan(otherPlan, addQtyInfo, BigDecimal.ZERO.longValue());
            return AjaxResult.success(result);
        }
        //天产能够，寸口产能不够--推荐寸口
        if (dayLeftOverQty >= addQty && sizeLeftOverQty < addQty) {
            List<FactoryMonthPlanProdFinal> otherPlan = sizePlannedProductionList.stream().filter(plannedPlan -> !productCode.equals(plannedPlan.getProductCode())).collect(Collectors.toList());
            AdjustNoticeSubtractPlanVo result = getRecommendPlan(otherPlan, addQtyInfo, BigDecimal.ZERO.longValue());
            return AjaxResult.success(result);
        }
        //模具产能够
        if (mouldLeftOverCapacityQty >= addQty) {
            AdjustNoticeSubtractPlanVo result = new AdjustNoticeSubtractPlanVo();
            result.setLeftOverQty(mouldLeftOverCapacityQty);
            result.setSubtractPlanList(Collections.emptyList());
            return AjaxResult.success(result);
        }
        //推荐同模具规格计划
        List<FactoryMonthPlanProdFinal> otherPlan = mouldNoPlannedProductionList.stream().filter(plannedPlan -> !productCode.equals(plannedPlan.getProductCode())).collect(Collectors.toList());
        AdjustNoticeSubtractPlanVo result = getRecommendPlan(otherPlan, addQtyInfo, mouldLeftOverCapacityQty);
        return AjaxResult.success(result);
    }

    @Override
    public AjaxResult checkMaxMouldQtyByStartDate(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo) {
        FactoryProductionVersion productionVersion = addQtyInfo.getProductionVersion();
        String mouldNo = addQtyInfo.getMouldNo();
        String productCode = addQtyInfo.getProductCode();
        BigDecimal proSize = addQtyInfo.getProSize();
        if (null == proSize || StringUtils.isBlank(productCode) || StringUtils.isBlank(mouldNo)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustDate.lackMessage"));
        }
        FactoryMonthPlanFinalVersionInfoVo finalVersion = BeanCopyUtils.copyBean(productionVersion, FactoryMonthPlanFinalVersionInfoVo.class);
        Set<Integer> stopDays = getStopDays(finalVersion);
        Integer startAdjustDay = addQtyInfo.getStartAdjustDay();
        if (stopDays.contains(startAdjustDay)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustDate.startDayHasStop"));
        }
        Map<String, MouldProductRelationDto> maxEnableMouldMap = getMaxEnableMould(productionVersion, productCode, mouldNo);
        if (CollectionUtils.isEmpty(maxEnableMouldMap)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.noMouldInfoError"));
        }
        AjaxResult checkQtyResult = AdjustMouldUtils.checkMouldMaxCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkQtyResult.get(AjaxResult.CODE_TAG)) {
            return checkQtyResult;
        }
        Long addQty = addQtyInfo.getAddQty();
        //获取SAP共用模具的其它SAP配置的模具
        Map<String, MouldProductRelationDto> monthShameOtherMap = getMonthMaxEnableMouldByMouldNo(productionVersion, productCode, mouldNo);
        List<FactoryMonthPlanProdFinal> plannedProductionList = getVersionProductionList(productionVersion);
        //获取mouldNo已排产量及其模具最大产能量-比较模具产能能否满足增量
        List<FactoryMonthPlanProdFinal> mouldNoPlannedProductionList = plannedProductionList.stream().filter(plannedPlan -> mouldNo.equals(plannedPlan.getMouldNo())).collect(Collectors.toList());
        Map<Integer, Long> dayLeftOverMouldCapacityMap = getMouldDayLeftOverCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap, monthShameOtherMap, mouldNoPlannedProductionList);
        Long mouldLeftOverCapacityQty = getMouldLeftOverCapacity(dayLeftOverMouldCapacityMap);
        AdjustNoticeSubtractPlanVo result = new AdjustNoticeSubtractPlanVo();
        //模具产能够
        if (mouldLeftOverCapacityQty >= addQty) {
            result.setLeftOverQty(mouldLeftOverCapacityQty);
            result.setSubtractPlanList(Collections.emptyList());
            return AjaxResult.success(result);
        }
        //同模具非同规格的排产计划
        List<FactoryMonthPlanProdFinal> noSameProductCodeMouldNoProductionList = AdjustMouldUtils.getNoSameProductCodeMouldNoProductionList(mouldNoPlannedProductionList, addQtyInfo);
        //模具剩余产能为零，则表示不可调增
        if (mouldLeftOverCapacityQty <= BigDecimal.ZERO.longValue() && CollectionUtils.isEmpty(noSameProductCodeMouldNoProductionList)) {
            result.setLeftOverQty(BigDecimal.ZERO.longValue());
            result.setSubtractPlanList(Collections.emptyList());
            return AjaxResult.error(AjaxResult.Type.WARN.value(), I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.noAdjustByNoProductionAndAutoConfirm"));
        }
        List<FactoryMonthFinalPlanHelperVo> helperList = BeanCopyUtils.copyBeanList(noSameProductCodeMouldNoProductionList, FactoryMonthFinalPlanHelperVo.class);
        result.setLeftOverQty(mouldLeftOverCapacityQty);
        result.setSubtractPlanList(helperList);
        return AjaxResult.success(result);
    }

    @Override
    public AjaxResult checkAfterSubtractOtherPlan(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, List<FactoryMonthPlanProdFinal> updateToDateSubtractList) {
        FactoryProductionVersion productionVersion = addQtyInfo.getProductionVersion();
        FactoryMonthPlanFinalVersionInfoVo finalVersion = BeanCopyUtils.copyBean(productionVersion, FactoryMonthPlanFinalVersionInfoVo.class);
        Set<Integer> stopDays = getStopDays(finalVersion);
        String factoryCode = productionVersion.getFactoryCode();
        Integer year = productionVersion.getYear();
        Integer month = productionVersion.getMonth();
        String mouldNo = addQtyInfo.getMouldNo();
        String productCode = addQtyInfo.getProductCode();
        //校验增量，不可超出模具的最大排产量
        Map<String, MouldProductRelationDto> maxEnableMouldMap = getMaxEnableMould(productionVersion, productCode, mouldNo);
        AjaxResult checkQtyResult = AdjustMouldUtils.checkMouldMaxCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkQtyResult.get(AjaxResult.CODE_TAG)) {
            return checkQtyResult;
        }
        //获取版本排产计划，并对调减计划进行更新
        List<FactoryMonthPlanProdFinal> plannedProductionList = getVersionProductionList(productionVersion);
        Map<String, FactoryMonthPlanProdFinal> subtractPlanMap = updateToDateSubtractList.stream().collect(Collectors.toMap(FactoryMonthPlanProdFinal::getProductionNo, Function.identity()));
        plannedProductionList.stream().forEach(plannedPlan -> {
            FactoryMonthPlanProdFinal subtractPlan = subtractPlanMap.get(plannedPlan.getProductionNo());
            if (null != subtractPlan) {
                BeanUtils.copyProperties(subtractPlan, plannedPlan);
            }
        });
        Long addQty = addQtyInfo.getAddQty();
        //获取SAP共用模具的其它SAP配置的模具
        Map<String, MouldProductRelationDto> monthShameOtherMap = getMonthMaxEnableMouldByMouldNo(productionVersion, productCode, mouldNo);
        //获取mouldNo已排产量及其模具最大产能量-比较
        List<FactoryMonthPlanProdFinal> mouldNoPlannedProductionList = plannedProductionList.stream().filter(plannedPlan -> mouldNo.equals(plannedPlan.getMouldNo())).collect(Collectors.toList());
        Map<Integer, Long> dayLeftOverMouldCapacityMap = getMouldDayLeftOverCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap, monthShameOtherMap, mouldNoPlannedProductionList);
        Long mouldLeftOverCapacityQty = BigDecimal.ZERO.longValue();
        for (Map.Entry<Integer, Long> dayLeftOverQtyEntry : dayLeftOverMouldCapacityMap.entrySet()) {
            mouldLeftOverCapacityQty = mouldLeftOverCapacityQty + dayLeftOverQtyEntry.getValue();
        }
        if (addQty > mouldLeftOverCapacityQty) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.passMaxCapacityError");
            return AjaxResult.error(String.format(errorInfo, addQty, mouldLeftOverCapacityQty));
        }
        //计算天产能分配和寸口的天产能分配
        BigDecimal proSize = addQtyInfo.getProSize();
        List<SizeCapacityConfiguration> configurationList = sizeCapacityConfigurationService.getConfigurationByFactoryYearAndMonth(factoryCode, year, month);
        Map<Integer, Long> dayMaxCapacityMap = new HashMap<>();
        Map<Integer, Long> daySizeMaxCapacityMap = new HashMap<>();
        calculateDayCapacity(configurationList, stopDays, addQtyInfo.getMonthMaxDays(), dayMaxCapacityMap, daySizeMaxCapacityMap, proSize);
        //寸口排产计划
        List<FactoryMonthPlanProdFinal> sizePlannedProductionList = plannedProductionList.stream().filter(plannedPlan -> proSize.equals(plannedPlan.getProSize())).collect(Collectors.toList());
        //单日最大模具产能
        Long dayMaxMouldQty = helper.getMaxSingleMouldQty() * maxEnableMouldMap.size();
        //计算寸口剩余产能
        Long sizeLeftOverQty = AdjustProductionUtils.getLeftOverLimitCapacity(stopDays, daySizeMaxCapacityMap, addQtyInfo, sizePlannedProductionList, dayMaxMouldQty);
        if (sizeLeftOverQty < addQty) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.passMaxSizeCapacityError");
            return AjaxResult.error(String.format(errorInfo, addQty, sizeLeftOverQty));
        }
        //计算天剩余产能
        Long dayLeftOverQty = AdjustProductionUtils.getLeftOverLimitCapacity(stopDays, dayMaxCapacityMap, addQtyInfo, plannedProductionList, dayMaxMouldQty);
        if (dayLeftOverQty < addQty) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.passMaxDayCapacityError");
            return AjaxResult.error(String.format(errorInfo, addQty, dayLeftOverQty));
        }
        Map<Integer, Long> dayLimitQtyMap = AdjustProductionUtils.getDayLimitQty(stopDays, addQtyInfo, dayMaxCapacityMap, daySizeMaxCapacityMap, dayLeftOverMouldCapacityMap, plannedProductionList, sizePlannedProductionList);
        Long dayLimitQtySum = getTotalDayLimitQty(dayLimitQtyMap);
        if (dayLimitQtySum < addQty) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.passMaxDayCapacityError");
            return AjaxResult.error(String.format(errorInfo, addQty, dayLimitQtySum));
        }
        AfterSubtractPlanInfoHelper data = new AfterSubtractPlanInfoHelper(dayLimitQtyMap, maxEnableMouldMap, stopDays);
        return AjaxResult.success(data);
    }

    @Override
    public AjaxResult checkAfterSubtractOtherPlanByMould(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, List<FactoryMonthPlanProdFinal> updateToDateSubtractList) {
        FactoryProductionVersion productionVersion = addQtyInfo.getProductionVersion();
        FactoryMonthPlanFinalVersionInfoVo finalVersion = BeanCopyUtils.copyBean(productionVersion, FactoryMonthPlanFinalVersionInfoVo.class);
        Set<Integer> stopDays = getStopDays(finalVersion);
        String mouldNo = addQtyInfo.getMouldNo();
        String productCode = addQtyInfo.getProductCode();
        //校验增量，不可超出模具的最大排产量
        Map<String, MouldProductRelationDto> maxEnableMouldMap = getMaxEnableMould(productionVersion, productCode, mouldNo);
        AjaxResult checkQtyResult = AdjustMouldUtils.checkMouldMaxCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkQtyResult.get(AjaxResult.CODE_TAG)) {
            return checkQtyResult;
        }
        //获取版本排产计划，并对调减计划进行更新
        List<FactoryMonthPlanProdFinal> plannedProductionList = getVersionProductionList(productionVersion);
        Map<String, FactoryMonthPlanProdFinal> subtractPlanMap = updateToDateSubtractList.stream().collect(Collectors.toMap(FactoryMonthPlanProdFinal::getProductionNo, Function.identity()));
        plannedProductionList.stream().forEach(plannedPlan -> {
            FactoryMonthPlanProdFinal subtractPlan = subtractPlanMap.get(plannedPlan.getProductionNo());
            if (null != subtractPlan) {
                BeanUtils.copyProperties(subtractPlan, plannedPlan);
            }
        });
        Long addQty = addQtyInfo.getAddQty();
        //获取SAP共用模具的其它SAP配置的模具
        Map<String, MouldProductRelationDto> monthShameOtherMap = getMonthMaxEnableMouldByMouldNo(productionVersion, productCode, mouldNo);
        //获取mouldNo已排产量及其模具最大产能量-比较
        List<FactoryMonthPlanProdFinal> mouldNoPlannedProductionList = plannedProductionList.stream().filter(plannedPlan -> mouldNo.equals(plannedPlan.getMouldNo())).collect(Collectors.toList());
        Map<Integer, Long> dayLeftOverMouldCapacityMap = getMouldDayLeftOverCapacity(helper, addQtyInfo, stopDays, maxEnableMouldMap, monthShameOtherMap, mouldNoPlannedProductionList);
        Long mouldLeftOverCapacityQty = BigDecimal.ZERO.longValue();
        for (Map.Entry<Integer, Long> dayLeftOverQtyEntry : dayLeftOverMouldCapacityMap.entrySet()) {
            mouldLeftOverCapacityQty = mouldLeftOverCapacityQty + dayLeftOverQtyEntry.getValue();
        }
        if (addQty > mouldLeftOverCapacityQty) {
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustAdd.passMaxCapacityError");
            return AjaxResult.error(String.format(errorInfo, addQty, mouldLeftOverCapacityQty));
        }
        AfterSubtractPlanInfoHelper data = new AfterSubtractPlanInfoHelper(dayLeftOverMouldCapacityMap, maxEnableMouldMap, stopDays);
        return AjaxResult.success(data);
    }

    @Override
    public List<DaySizeCapacityConfigurationDetailVo> getDaySizeCapacityInfo(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return Collections.emptyList();
        }
        List<SizeCapacityConfiguration> configurationList = sizeCapacityConfigurationService.getConfigurationByFactoryYearAndMonth(factoryCode, year, month);
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        Set<Integer> stopDays = productionCalendarService.getStopDays(factoryCode, year, month);
        Integer monthMaxDays = productionCalendarService.getMonthDays(factoryCode, year, month);
        Map<Integer, Long> dayMaxCapacityMap = new HashMap<>();
        Map<Integer, Map<BigDecimal, Long>> daySizeMaxCapacityMap = new HashMap<>();
        calculateDayCapacity(configurationList, stopDays, monthMaxDays, dayMaxCapacityMap, daySizeMaxCapacityMap);
        List<DaySizeCapacityConfigurationDetailVo> capacityConfigurationList = new ArrayList<>();
        dayMaxCapacityMap.entrySet().stream().forEach(dayMaxCapacityEntry -> {
            Integer day = dayMaxCapacityEntry.getKey();
            DaySizeCapacityConfigurationDetailVo dayCapacityConfiguration = new DaySizeCapacityConfigurationDetailVo();
            dayCapacityConfiguration.setSumCapacityQty(dayMaxCapacityEntry.getValue());
            dayCapacityConfiguration.setDay(day);
            Map<BigDecimal, Long> daySizeMap = daySizeMaxCapacityMap.get(day);
            if (CollectionUtils.isEmpty(daySizeMap)) {
                dayCapacityConfiguration.setDetail(Collections.emptyList());
                return;
            }
            List<DaySizeCapacityDetailVo> detail = new ArrayList<>();
            daySizeMap.entrySet().stream().forEach(daySizeCapacityEntry -> {
                DaySizeCapacityDetailVo proSizeDetail = new DaySizeCapacityDetailVo();
                proSizeDetail.setProSize(daySizeCapacityEntry.getKey());
                proSizeDetail.setSizeCapacityQty(daySizeCapacityEntry.getValue());
                detail.add(proSizeDetail);
            });
            dayCapacityConfiguration.setDetail(detail);
            capacityConfigurationList.add(dayCapacityConfiguration);
        });
        capacityConfigurationList.sort(Comparator.comparing(DaySizeCapacityConfigurationDetailVo::getDay));
        return capacityConfigurationList;
    }

    @Override
    public List<DaySizeCapacityConfigurationMouldMethodDetailVo> getDaySizeCapacityInfoByMouldMethod(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return Collections.emptyList();
        }
        List<SizeCapacityConfiguration> configurationList = sizeCapacityConfigurationService.getConfigurationByFactoryYearAndMonth(factoryCode, year, month);
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        Set<BigDecimal> proSizeSet = configurationList.stream().map(SizeCapacityConfiguration::getProSize).collect(Collectors.toSet());
        Set<Integer> stopDays = productionCalendarService.getStopDays(factoryCode, year, month);
        Integer monthMaxDays = productionCalendarService.getMonthDays(factoryCode, year, month);
        Map<BigDecimal, Map<String, Map<Integer, Long>>> proSizeMouldMethodDayCapacityMap = new HashMap<>(proSizeSet.size());
        calculateDayCapacity(configurationList, stopDays, monthMaxDays, proSizeMouldMethodDayCapacityMap);
        if (CollectionUtils.isEmpty(proSizeMouldMethodDayCapacityMap)) {
            return Collections.emptyList();
        }
        List<DaySizeCapacityConfigurationMouldMethodDetailVo> configurationDetailList = new ArrayList<>();
        proSizeMouldMethodDayCapacityMap.entrySet().forEach(proSizeEntry -> {
            BigDecimal proSize = proSizeEntry.getKey();
            Map<String, Map<Integer, Long>> mouldMethodMap = proSizeEntry.getValue();
            if (CollectionUtils.isEmpty(mouldMethodMap)) {
                return;
            }
            DaySizeCapacityConfigurationMouldMethodDetailVo proSizeDetail = createProSizeMouldMethodDayCapacityDetailInfo(proSize, mouldMethodMap);
            if (null == proSizeDetail) {
                return;
            }
            configurationDetailList.add(proSizeDetail);
        });
        configurationDetailList.sort(Comparator.comparing(DaySizeCapacityConfigurationMouldMethodDetailVo::getProSize, Comparator.reverseOrder()));
        return configurationDetailList;
    }

    /**
     * 得到天产能限制总量
     *
     * @param dayLimitQtyMap 汇总的日排产限制-日模具产能、日寸口产能、日总产能
     * @return
     */
    private Long getTotalDayLimitQty(Map<Integer, Long> dayLimitQtyMap) {
        Long dayLimitQtySum = BigDecimal.ZERO.longValue();
        for (Map.Entry<Integer, Long> dayLimitEntry : dayLimitQtyMap.entrySet()) {
            Long dayLimitQty = dayLimitEntry.getValue();
            if (null == dayLimitQty) {
                dayLimitQty = BigDecimal.ZERO.longValue();
            }
            dayLimitQtySum = dayLimitQtySum + dayLimitQty;
        }
        return dayLimitQtySum;
    }

    /**
     * 汇总日模具剩余产能
     *
     * @param dayLeftOverQtyMap 日模具剩余产能信息
     * @return
     */
    private Long getMouldLeftOverCapacity(Map<Integer, Long> dayLeftOverQtyMap) {
        Long totalLeftOverQty = BigDecimal.ZERO.longValue();
        for (Map.Entry<Integer, Long> dayLeftOverQtyEntry : dayLeftOverQtyMap.entrySet()) {
            totalLeftOverQty = totalLeftOverQty + dayLeftOverQtyEntry.getValue();
        }
        return totalLeftOverQty;
    }

    /**
     * 获取模具日剩余产能信息
     *
     * @param helper                施工信息
     * @param addQtyInfo            增量信息
     * @param stopDays              停工集合
     * @param maxEnableMouldMap     S
     * @param monthShareOtherMap
     * @param plannedProductionList
     * @return
     */
    private Map<Integer, Long> getMouldDayLeftOverCapacity(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, Set<Integer> stopDays, Map<String, MouldProductRelationDto> maxEnableMouldMap, Map<String, MouldProductRelationDto> monthShareOtherMap, List<FactoryMonthPlanProdFinal> plannedProductionList) {
        //没有mouldNo的排产计划，则使用其配置的模具最大排产量
        if (CollectionUtils.isEmpty(plannedProductionList)) {
            return AdjustMouldUtils.getDayLeftOverQtyByEmptyMould(helper, addQtyInfo, stopDays, maxEnableMouldMap);
        }
        String productCode = addQtyInfo.getProductCode();
        boolean hasShare;
        if (CollectionUtils.isEmpty(monthShareOtherMap)) {
            hasShare = false;
        } else {
            hasShare = true;
        }
        //没有共用
        if (!hasShare) {
            List<FactoryMonthPlanProdFinal> productCodePlannedProductionList = plannedProductionList.stream().filter(plannedPlan -> productCode.equals(plannedPlan.getProductCode())).collect(Collectors.toList());
            //没有排产
            if (CollectionUtils.isEmpty(productCodePlannedProductionList)) {
                return AdjustMouldUtils.getDayLeftOverQtyByEmptyMould(helper, addQtyInfo, stopDays, maxEnableMouldMap);
            }
            //有排产，需要去掉已排产量
            return AdjustMouldUtils.getDayLeftOverQtyByPlanned(helper, addQtyInfo, stopDays, maxEnableMouldMap, productCodePlannedProductionList);
        }
        //有共用，有排产计划
        return AdjustMouldUtils.getDayLeftOverBySharePlanned(helper, addQtyInfo, stopDays, maxEnableMouldMap, monthShareOtherMap, plannedProductionList);
    }

    /**
     * 根据寸口产能配置--得到天的产能控制
     *
     * @param configurationList     寸口产能分配
     * @param stopDays              停工日
     * @param monthDays             月最大天数
     * @param dayMaxCapacityMap     天总产能控制
     * @param daySizeMaxCapacityMap 天寸口总产能控制
     * @param proSize               寸口
     */
    private void calculateDayCapacity(List<SizeCapacityConfiguration> configurationList, Set<Integer> stopDays, Integer monthDays, Map<Integer, Long> dayMaxCapacityMap, Map<Integer, Long> daySizeMaxCapacityMap, BigDecimal proSize) {
        List<DaySizeCapacityVo> treeList = SizeCapacityUtils.buildTree(configurationList);
        //寸口|*|工装类型|*|成型法|*|胎体布层级 产能
        Map<Integer, Map<String, Long>> daySizeCapacityMap = new HashMap<>();
        Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap = new HashMap<>();
        treeList.stream().forEach(daySizeCapacity -> SizeCapacityUtils.buildCapacityControlInfo(stopDays, daySizeCapacity, BigDecimal.ZERO.intValue(), daySizeCapacityMap, monthDays, dayMaxMouldQtyMap));
        if (CollectionUtils.isEmpty(daySizeCapacityMap)) {
            return;
        }
        String startKey = String.format("%s|*|", proSize);
        //转换成天的产能限制
        daySizeCapacityMap.entrySet().stream().forEach(entry -> {
            Integer day = entry.getKey();
            Map<String, Long> sizeCapacityByDayMap = entry.getValue();
            if (CollectionUtils.isEmpty(sizeCapacityByDayMap)) {
                dayMaxCapacityMap.put(day, BigDecimal.ZERO.longValue());
                return;
            }
            Long sumQty = BigDecimal.ZERO.longValue();
            for (Map.Entry<String, Long> sizeCapacityEntry : sizeCapacityByDayMap.entrySet()) {
                Long sizeQty = sizeCapacityEntry.getValue();
                if (null == sizeQty) {
                    sizeQty = BigDecimal.ZERO.longValue();
                }
                sumQty = sumQty + sizeQty;
                String sizeCapacityGroupKey = sizeCapacityEntry.getKey();
                if (!sizeCapacityGroupKey.startsWith(startKey)) {
                    continue;
                }
                //寸口天产能
                Long daySizeCapacityQty = daySizeMaxCapacityMap.get(day);
                if (null == daySizeCapacityQty) {
                    daySizeCapacityQty = BigDecimal.ZERO.longValue();
                }
                daySizeCapacityQty = daySizeCapacityQty + sizeQty;
                daySizeMaxCapacityMap.put(day, daySizeCapacityQty);
            }
            //天总产能
            dayMaxCapacityMap.put(day, sumQty);
        });
    }

    /**
     * 根据寸口产能配置--得到天的产能控制
     *
     * @param configurationList     寸口产能分配
     * @param stopDays              停工日
     * @param monthDays             月最大天数
     * @param dayMaxCapacityMap     天总产能控制
     * @param daySizeMaxCapacityMap 天寸口产能控制
     */
    private void calculateDayCapacity(List<SizeCapacityConfiguration> configurationList, Set<Integer> stopDays, Integer monthDays, Map<Integer, Long> dayMaxCapacityMap, Map<Integer, Map<BigDecimal, Long>> daySizeMaxCapacityMap) {
        List<DaySizeCapacityVo> treeList = SizeCapacityUtils.buildTree(configurationList);
        Set<BigDecimal> proSizeSet = configurationList.stream().map(SizeCapacityConfiguration::getProSize).collect(Collectors.toSet());
        //寸口|*|工装类型|*|成型法|*|胎体布层级 产能
        Map<Integer, Map<String, Long>> daySizeCapacityMap = new HashMap<>();
        Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap = new HashMap<>();
        treeList.stream().forEach(daySizeCapacity -> SizeCapacityUtils.buildCapacityControlInfo(stopDays, daySizeCapacity, BigDecimal.ZERO.intValue(), daySizeCapacityMap, monthDays, dayMaxMouldQtyMap));
        if (CollectionUtils.isEmpty(daySizeCapacityMap)) {
            return;
        }
        //转换成天的产能限制
        daySizeCapacityMap.entrySet().stream().forEach(entry -> {
            Integer day = entry.getKey();
            Map<String, Long> sizeCapacityByDayMap = entry.getValue();
            if (CollectionUtils.isEmpty(sizeCapacityByDayMap)) {
                dayMaxCapacityMap.put(day, BigDecimal.ZERO.longValue());
                return;
            }
            Long sumQty = BigDecimal.ZERO.longValue();
            for (Map.Entry<String, Long> sizeCapacityEntry : sizeCapacityByDayMap.entrySet()) {
                Long sizeQty = sizeCapacityEntry.getValue();
                if (null == sizeQty) {
                    sizeQty = BigDecimal.ZERO.longValue();
                }
                sumQty = sumQty + sizeQty;
            }
            //天总产能
            dayMaxCapacityMap.put(day, sumQty);
            Map<BigDecimal, Long> dayProSizeCapacityMap = new HashMap<>();
            proSizeSet.stream().forEach(proSize -> {
                Long sizeQty = BigDecimal.ZERO.longValue();
                for (Map.Entry<String, Long> sizeCapacityEntry : sizeCapacityByDayMap.entrySet()) {
                    String groupKey = sizeCapacityEntry.getKey();
                    String startKey = String.format("%s|*|", proSize);
                    if (groupKey.startsWith(startKey)) {
                        Long methodQty = sizeCapacityEntry.getValue();
                        if (null == methodQty) {
                            methodQty = BigDecimal.ZERO.longValue();
                        }
                        sizeQty = sizeQty + methodQty;
                    }
                }
                dayProSizeCapacityMap.put(proSize, sizeQty);
            });
            daySizeMaxCapacityMap.put(day, dayProSizeCapacityMap);
        });
    }


    /**
     * 根据寸口产能配置--得到天的产能控制
     *
     * @param configurationList                寸口产能分配
     * @param stopDays                         停工日
     * @param monthDays                        月最大天数
     * @param proSizeMouldMethodDayCapacityMap 构建寸口+成型法+天产能结合
     */
    private void calculateDayCapacity(List<SizeCapacityConfiguration> configurationList, Set<Integer> stopDays, Integer monthDays, Map<BigDecimal, Map<String, Map<Integer, Long>>> proSizeMouldMethodDayCapacityMap) {
        List<DaySizeCapacityVo> treeList = SizeCapacityUtils.buildTree(configurationList);
        Set<BigDecimal> proSizeSet = configurationList.stream().map(SizeCapacityConfiguration::getProSize).collect(Collectors.toSet());
        //寸口|*|工装类型|*|成型法|*|胎体布层级 产能
        Map<Integer, Map<String, Long>> daySizeCapacityMap = new HashMap<>(monthDays);
        Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap = new HashMap<>();
        treeList.stream().forEach(daySizeCapacity -> SizeCapacityUtils.buildCapacityControlInfo(stopDays, daySizeCapacity, BigDecimal.ZERO.intValue(), daySizeCapacityMap, monthDays, dayMaxMouldQtyMap));
        if (CollectionUtils.isEmpty(daySizeCapacityMap)) {
            return;
        }
        String proSizeStartFormat = "%s|*|";
        proSizeSet.stream().forEach(proSize -> {
            String proSizeStartKey = String.format(proSizeStartFormat, proSize);
            Map<String, Map<Integer, Long>> proSizeCapacityMap = getProSizeMouldMethodDayCapacityInfo(proSize, proSizeMouldMethodDayCapacityMap);
            for (Integer day = FactoryConstant.MONTH_START_DAY; day <= monthDays; day++) {
                //天产能中所有的寸口+成型法产能
                Map<String, Long> proSizeMouldMethodCapacityMap = daySizeCapacityMap.get(day);
                if (CollectionUtils.isEmpty(proSizeMouldMethodCapacityMap)) {
                    continue;
                }
                Integer putDay = day;
                //提取对应寸口
                proSizeMouldMethodCapacityMap.entrySet().stream().forEach(capacityEntry -> {
                    String capacityKey = capacityEntry.getKey();
                    if (!capacityKey.startsWith(proSizeStartKey)) {
                        return;
                    }
                    String mouldMethodInfo = capacityKey.replace(proSizeStartKey, "");
                    String mouldMethod = mouldMethodInfo.split(PRODUCT_SPLIT)[1];
                    Map<Integer, Long> mouldMethodDayCapacityMap = proSizeCapacityMap.get(mouldMethod);
                    Long dayCapacity = mouldMethodDayCapacityMap.get(putDay);
                    if (null == dayCapacity) {
                        dayCapacity = BigDecimal.ZERO.longValue();
                    }
                    Long addCapacity = capacityEntry.getValue();
                    if (null == addCapacity) {
                        addCapacity = BigDecimal.ZERO.longValue();
                    }
                    mouldMethodDayCapacityMap.put(putDay, dayCapacity + addCapacity);
                });
            }
        });
    }

    /**
     * 校验模具是否有产能
     *
     * @param mouldNoProductionList
     * @param monthMaxMap
     * @param maxEnableMouldMap
     * @return
     */
    private AjaxResult checkMouldCapacity(List<FactoryMonthPlanProdFinal> mouldNoProductionList, Map<String, MouldProductRelationDto> monthMaxMap, Map<String, MouldProductRelationDto> maxEnableMouldMap, AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo) {
        return AjaxResult.success();
    }

    /**
     * 得到推荐计划信息
     *
     * @param recommendPlanList
     * @param addQtyInfo
     * @return
     */
    private AdjustNoticeSubtractPlanVo getRecommendPlan(List<FactoryMonthPlanProdFinal> recommendPlanList, AddQtyAdjustPlanHelper addQtyInfo, Long leftOverQty) {
        AdjustNoticeSubtractPlanVo result = new AdjustNoticeSubtractPlanVo();
        result.setLeftOverQty(leftOverQty);
        if (CollectionUtils.isEmpty(recommendPlanList)) {
            return result;
        }
        Integer startDay = addQtyInfo.getStartAdjustDay();
        Integer monthMaxDay = addQtyInfo.getMonthMaxDays();
        List<FactoryMonthPlanProdFinal> recommendList = recommendPlanList.stream().filter(recommendPlan -> AdjustProductionUtils.getTotalProductionQty(recommendPlan, startDay, monthMaxDay) > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        List<FactoryMonthFinalPlanHelperVo> helperList = BeanCopyUtils.copyBeanList(recommendList, FactoryMonthFinalPlanHelperVo.class);
        result.setSubtractPlanList(helperList);
        return result;
    }

    /**
     * 根据排产版本获取其停开工日历，并转化成不可调整日信息集合
     *
     * @param finalVersion 分厂编码
     * @return
     */
    private Set<Integer> getStopDays(FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        if (null == finalVersion) {
            return Collections.emptySet();
        }
        String factoryCode = finalVersion.getFactoryCode();
        //自然月
        if (YesOrNoEnum.YES.getValue().equals(finalVersion.getIsNaturalMonth())) {
            //获取工厂停开工日历
            MdmProductionCalendar factoryMonthQuery = new MdmProductionCalendar();
            factoryMonthQuery.setFactoryCode(factoryCode);
            factoryMonthQuery.setYear(finalVersion.getYear());
            factoryMonthQuery.setMonth(finalVersion.getMonth());
            List<MdmProductionCalendar> calendarList = productionCalendarService.selectMdmProductionCalendarList(factoryMonthQuery);
            if (CollectionUtils.isEmpty(calendarList)) {
                return Collections.emptySet();
            }
            Set<Integer> stopList = DateUtils.calculateStopDays(BeanCopyUtils.copyBeanList(calendarList, ProductionDayInfoVo.class));
            return stopList;
        }
        //非自然月
        List<MdmProductionCalendar> calendarList = productionCalendarService.getDateRangeCalendarList(factoryCode, finalVersion.getProductionStartDate(), finalVersion.getProductionEndDate());
        if (CollectionUtils.isEmpty(calendarList)) {
            return Collections.emptySet();
        }
        Set<Integer> stopList = DateUtils.calculateStopDays(BeanCopyUtils.copyBeanList(calendarList, ProductionDayInfoVo.class), finalVersion);
        return stopList;
    }

    /**
     * 根据年、月、SAP代码及模具，获取其共用模具的SAP配置的同模具的其它模具配置信息
     * 1、先根据SAP及模具获取共用模具的SAP代码列表
     * 2、再根据SAP列表及模具，获取共用模具SAP的所有同模具的模具配置
     *
     * @param productionVersion 版本信息
     * @param productCode       SAP代码
     * @param mouldNo           模具-大类
     * @return
     */
    private Map<String, MouldProductRelationDto> getMonthMaxEnableMouldByMouldNo(FactoryProductionVersion productionVersion, String productCode, String mouldNo) {
        String factoryCode = productionVersion.getFactoryCode();
        Integer year = productionVersion.getYear();
        Integer month = productionVersion.getMonth();
        Date productionStartDate = productionVersion.getProductionStartDate();
        Date productionEndDate = productionVersion.getProductionEndDate();
        List<MouldProductRelationDto> shareProductList = factoryMonthPlanAdjustMapper.getShareMouldProductList(factoryCode, productCode, mouldNo);
        if (CollectionUtils.isEmpty(shareProductList)) {
            return Collections.emptyMap();
        }
        Set<String> shareMouldProductSet = shareProductList.stream().map(MouldProductRelationDto::getProductCode).collect(Collectors.toSet());
        List<String> shareMouldProductList = new ArrayList<>(shareMouldProductSet);
        List<MouldProductRelationDto> monthEnableList = factoryMonthPlanAdjustMapper.getMonthShareMouldConfiguration(factoryCode, year, month, mouldNo, shareMouldProductList);
        List<MouldProductRelationDto> monthMaintenanceList = factoryMonthPlanAdjustMapper.getFactoryMouldShareMaintenanceConfigurationByCycle(factoryCode, productionStartDate, productionEndDate, mouldNo, shareMouldProductList);
        return AdjustNoticeUtils.getMaxEnableMould(monthEnableList, monthMaintenanceList, productionStartDate);
    }

    /**
     * 获取SAP对应最大的可用模具信息
     *
     * @param productionVersion 版本信息
     * @param productCode       物料编码
     * @param mouldNo           模具
     * @return
     */
    private Map<String, MouldProductRelationDto> getMaxEnableMould(FactoryProductionVersion productionVersion, String productCode, String mouldNo) {
        String factoryCode = productionVersion.getFactoryCode();
        Integer year = productionVersion.getYear();
        Integer month = productionVersion.getMonth();
        Date productionStartDate = productionVersion.getProductionStartDate();
        Date productionEndDate = productionVersion.getProductionEndDate();
        List<MouldProductRelationDto> monthEnableList = factoryMonthPlanAdjustMapper.getMonthEnableMouldConfiguration(factoryCode, year, month, mouldNo, productCode);
        List<MouldProductRelationDto> monthMaintenanceList = factoryMonthPlanAdjustMapper.getFactoryMouldMaintenanceConfigurationByCycle(factoryCode, productionStartDate, productionEndDate, mouldNo, productCode);
        return AdjustNoticeUtils.getMaxEnableMould(monthEnableList, monthMaintenanceList, productionStartDate);
    }

    /**
     * 根据排产版本，获取mouldNo排产信息
     *
     * @param productionVersion 排产版本信息
     * @param mouldNo           模具-大类
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getProductionListByMouldNo(FactoryProductionVersion productionVersion, String mouldNo) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", productionVersion.getFactoryCode());
        queryWrapper.eq("PRODUCTION_VERSION", productionVersion.getProductionVersion());
        queryWrapper.eq("MOULD_NO", mouldNo);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
    }

    /**
     * 根据排产版本，获取productCode排产信息
     *
     * @param productionVersion 排产版本信息
     * @param productCode       SAP代码
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getProductionListByProductCode(FactoryProductionVersion productionVersion, String productCode) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", productionVersion.getFactoryCode());
        queryWrapper.eq("PRODUCTION_VERSION", productionVersion.getProductionVersion());
        queryWrapper.eq("PRODUCT_CODE", productCode);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
    }


    /**
     * 根据排产版本，获取productCode排产信息
     *
     * @param productionVersion 排产版本信息
     * @param proSize           寸口
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getProductionListByProSize(FactoryProductionVersion productionVersion, BigDecimal proSize) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", productionVersion.getFactoryCode());
        queryWrapper.eq("PRODUCTION_VERSION", productionVersion.getProductionVersion());
        queryWrapper.eq("PRO_SIZE", proSize);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
    }

    /**
     * 根据排产版本，获取排产信息
     *
     * @param productionVersion 排产版本信息
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getVersionProductionList(FactoryProductionVersion productionVersion) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", productionVersion.getFactoryCode());
        queryWrapper.eq("PRODUCTION_VERSION", productionVersion.getProductionVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
    }

    /**
     * 获取寸口的成型法天产能信息
     *
     * @param proSize                          寸口
     * @param proSizeMouldMethodDayCapacityMap 寸口+成形法+天产能集合
     * @return
     */
    private Map<String, Map<Integer, Long>> getProSizeMouldMethodDayCapacityInfo(BigDecimal proSize, Map<BigDecimal, Map<String, Map<Integer, Long>>> proSizeMouldMethodDayCapacityMap) {
        Map<String, Map<Integer, Long>> proSizeCapacityMap = proSizeMouldMethodDayCapacityMap.get(proSize);
        if (null != proSizeCapacityMap) {
            return proSizeCapacityMap;
        }
        proSizeCapacityMap = new HashMap<>();
        proSizeCapacityMap.put(FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue(), new HashMap<>());
        proSizeCapacityMap.put(FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue(), new HashMap<>());
        proSizeMouldMethodDayCapacityMap.put(proSize, proSizeCapacityMap);
        return proSizeMouldMethodDayCapacityMap.get(proSize);
    }

    /**
     * 构建寸口的月产能分布明细信息
     *
     * @param proSize        寸口
     * @param mouldMethodMap 寸口 + 成型 + 日 产能
     * @return
     */
    private DaySizeCapacityConfigurationMouldMethodDetailVo createProSizeMouldMethodDayCapacityDetailInfo(BigDecimal proSize, Map<String, Map<Integer, Long>> mouldMethodMap) {
        if (CollectionUtils.isEmpty(mouldMethodMap)) {
            return null;
        }
        Set<String> mouldMethodSet = mouldMethodMap.keySet();
        DaySizeCapacityConfigurationMouldMethodDetailVo proSizeDetail = new DaySizeCapacityConfigurationMouldMethodDetailVo();
        proSizeDetail.setProSize(proSize);
        List<MouldMethodMonthCycleCapacityDetailVo> mouldMethodDaySizeCapacityList = new ArrayList<>(mouldMethodSet.size());
        mouldMethodSet.stream().forEach(mouldMethod -> {
            MouldMethodMonthCycleCapacityDetailVo mouldMethodDetail = new MouldMethodMonthCycleCapacityDetailVo();
            mouldMethodDetail.setMouldMethod(mouldMethod);
            paddingDayCapacity(mouldMethodDetail, mouldMethodMap.get(mouldMethod));
            mouldMethodDaySizeCapacityList.add(mouldMethodDetail);
        });
        proSizeDetail.setMouldMethodList(mouldMethodDaySizeCapacityList);
        return proSizeDetail;
    }

    /**
     * @param mouldMethodDetail
     * @param dayCapacityMap
     */
    private void paddingDayCapacity(MouldMethodMonthCycleCapacityDetailVo mouldMethodDetail, Map<Integer, Long> dayCapacityMap) {
        String fieldNameFormat = "day%s";
        for (Integer day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String fieldName = String.format(fieldNameFormat, day);
            Long dayCapacity = dayCapacityMap.get(day);
            if (null == dayCapacity) {
                dayCapacity = BigDecimal.ZERO.longValue();
            }
            mouldMethodDetail.setFieldValueByFieldName(fieldName, dayCapacity);
        }
    }
}