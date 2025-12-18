package com.zlt.aps.monthplan.factory.helper;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.api.enums.MouldNoProductionType;
import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 调整业务工具类
 *
 * @author ZLT
 * @date 20250401
 */
@Slf4j
public class AdjustUtils {

    /**
     * 根据试制量试计划信息，构建SAP与施工关系对象
     *
     * @param informalConstructionPlan 试制量试计划
     * @param curingTime               硫化时间
     * @return
     */
    public static MdmProductConstruction buildProductionConstructionConfiguration(FactoryMonthPlanProdFinal informalConstructionPlan, Integer curingTime) {
        MdmProductConstruction addConfiguration = new MdmProductConstruction();
        addConfiguration.setConstructionCode(informalConstructionPlan.getProductCode());
        addConfiguration.setProductCode(informalConstructionPlan.getProductCode());
        addConfiguration.setBomVersion("01");
        addConfiguration.setMouldMethod(informalConstructionPlan.getMouldMethod());
        addConfiguration.setEmbryoCode(informalConstructionPlan.getEmbryoCode());
        addConfiguration.setSpecCode(informalConstructionPlan.getSpecCode());
        addConfiguration.setFactoryCode(informalConstructionPlan.getFactoryCode());
        addConfiguration.setCuringTime(curingTime);
        addConfiguration.setCuringTime2(curingTime);
        addConfiguration.setMoldCavity("-1");
        addConfiguration.setMouldClampingPressure(BigDecimal.valueOf(-1));
        addConfiguration.setHydraulicPressureCuringTime(curingTime);
        addConfiguration.setHydraulicPressureCuringTime2(curingTime);
        return addConfiguration;
    }

    /**
     * 根据试制量试计划信息，构建SAP与施工关系对象
     *
     * @param informalConstructionPlan 试制量试计划
     * @param curingTime               硫化时间
     * @return
     */
    public static MdmProductConstruction buildProductionConstructionConfiguration(MonthPlanProductionFinalResult informalConstructionPlan, Integer curingTime) {
        MdmProductConstruction addConfiguration = new MdmProductConstruction();
        addConfiguration.setConstructionCode(informalConstructionPlan.getProductCode());
        addConfiguration.setProductCode(informalConstructionPlan.getProductCode());
        addConfiguration.setBomVersion("01");
        addConfiguration.setMouldMethod(informalConstructionPlan.getMouldMethod());
        addConfiguration.setEmbryoCode(informalConstructionPlan.getEmbryoCode());
        addConfiguration.setSpecCode(informalConstructionPlan.getSpecCode());
        addConfiguration.setFactoryCode(informalConstructionPlan.getFactoryCode());
        addConfiguration.setCuringTime(curingTime);
        addConfiguration.setCuringTime2(curingTime);
        addConfiguration.setMoldCavity("-1");
        addConfiguration.setMouldClampingPressure(BigDecimal.valueOf(-1));
        addConfiguration.setHydraulicPressureCuringTime(curingTime);
        addConfiguration.setHydraulicPressureCuringTime2(curingTime);
        return addConfiguration;
    }

    /**
     * 创建新的排产计划
     *
     * @param finalVersionInfo 版本信息
     * @param adjustPlan       调整计划
     * @return
     */
    public static FactoryMonthPlanProdFinal buildNewProductionPlan(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlan) {
        FactoryMonthPlanProdFinal productionPlan = new FactoryMonthPlanProdFinal();
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        String yearAndMonth = String.format("%s%02d", year, month);
        //版本信息
        productionPlan.setFactoryCode(finalVersionInfo.getFactoryCode());
        productionPlan.setYear(year);
        productionPlan.setMonth(month);
        productionPlan.setYearMonth(Integer.valueOf(yearAndMonth));
        productionPlan.setMonthPlanVersion(finalVersionInfo.getMonthPlanVersion());
        productionPlan.setProductionVersion(finalVersionInfo.getProductionVersion());
        //物料信息
        productionPlan.setProductCode(adjustPlan.getProductCode());
//        productionPlan.setCuringTime(adjustPlan.getCuringTime());
//        productionPlan.setLocationType(adjustPlan.getLocationType());
//        productionPlan.setChannel(adjustPlan.getChannel());
//        //排产量
//        Long adjustNumber = Long.valueOf(adjustPlan.getAdjustNumber());
//        productionPlan.setProdReqPlan(adjustNumber);
//        productionPlan.setFactProdReqQty(adjustNumber);
//        productionPlan.setTotalQty(adjustNumber);
//        productionPlan.setDifferenceQty(BigDecimal.ZERO.longValue());
//        //模具、规格代号、生胎代码
//        String specCodeInfo = adjustPlan.getSpecCodeInfo();
//        String specCode = adjustPlan.getSpecCode();
//        productionPlan.setMouldNo(adjustPlan.getMouldNo());
//        productionPlan.setSpecCodeInfo(specCodeInfo);
//        productionPlan.setSpecCode(specCode);
//        setEmbryoCodeInfo(specCodeInfo, productionPlan, specCode);
//        //施工信息
//        ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(adjustPlan.getConstructionCode());
//        productionPlan.setConstructionStage(stage.getStage());
//        productionPlan.setMergeInfo("");
//        productionPlan.setIsImport(YesOrNoEnum.NO.getValue());
        productionPlan.setIsDeliveryDate(YesOrNoEnum.NO.getValue());
        BigDecimal totalCuringTime = BigDecimal.ONE;
        productionPlan.setTotalVulcanizationMinutes(totalCuringTime.divide(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP));
        return productionPlan;
    }

    /**
     * 填充物料相关信息
     * 规格描述、花纹、层级、寸口、品牌
     * 胎别、规格
     *
     * @param productionPlan 新增的调整计划
     * @param productInfo    物料信息
     */
    public static void fillProductInfo(FactoryMonthPlanProdFinal productionPlan, MdmMaterialInfo productInfo) {
        productionPlan.setProductDesc(productInfo.getMaterialDesc());
        productionPlan.setPattern(productInfo.getPattern());
        productionPlan.setHierarchy(productInfo.getHierarchy());
        productionPlan.setSpecifications(productInfo.getSpecifications());
        // productionPlan.setProSize(String.valueOf(productInfo.getProSize()));
        productionPlan.setBrand(productInfo.getBrand());
        productionPlan.setProductTypeCode(productInfo.getProductTypeCode());
        productionPlan.setProductTypeName(productInfo.getProductTypeName());
    }

    /**
     * 清空中间转储的属性信息
     *
     * @param adjustPlan
     */
    public static void clearInfo(FactoryMonthPlanAdjustPlanVo adjustPlan) {
        adjustPlan.setCuringTime(null);
        adjustPlan.setDayMaxCuringTime(null);
        adjustPlan.setLogBuilder(null);
        adjustPlan.setConstructionCode(null);
        adjustPlan.setSpecCodeInfo(null);
        adjustPlan.setFinalVersionInfo(null);
        adjustPlan.setConfirmSubtractList(null);
    }

    /**
     * 构建空的模具排产结果信息
     *
     * @param finalVersionInfo     版本信息
     * @param mouldProductRelation 关联关系
     * @param stopDays             停工日
     * @param dayMaxCuringTime     最大硫化时间
     * @param monthMaxDay          月份最大天数
     * @return
     */
    public static MouldingProductionResultHelper buildMouldingProductionResult(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, MouldProductRelationDto mouldProductRelation, Set<Integer> stopDays, BigDecimal dayMaxCuringTime, Integer monthMaxDay) {
        MouldingProductionResultHelper helper = new MouldingProductionResultHelper();
        helper.setFactoryCode(finalVersionInfo.getFactoryCode());
        helper.setYear(finalVersionInfo.getYear());
        helper.setMonth(finalVersionInfo.getMonth());
        helper.setMonthPlanVersion(finalVersionInfo.getMonthPlanVersion());
        helper.setProductionVersion(finalVersionInfo.getProductionVersion());
        helper.setMouldNo(mouldProductRelation.getMouldNo());
        helper.setMouldCode(mouldProductRelation.getMouldCode());
        Map<Integer, BigDecimal> dayLeftOverCuringTimeMap = new HashMap<>();
        List<NoProductionDayMouldVo> noProductionDayList = new ArrayList<>();
        Set<Integer> noProductionDaySet = mouldProductRelation.getNoProductionList();
        if (null == noProductionDaySet) {
            noProductionDaySet = new HashSet<>();
        }
        if (!CollectionUtils.isEmpty(noProductionDaySet)) {
            noProductionDaySet.forEach(noProductionDay -> {
                NoProductionDayMouldVo noProductionDayMould = new NoProductionDayMouldVo();
                noProductionDayMould.setDay(noProductionDay);
                noProductionDayMould.setNoProductionType(MouldNoProductionType.MAINTENANCE_DAY);
                noProductionDayList.add(noProductionDayMould);
            });
        }
        if (!CollectionUtils.isEmpty(stopDays)) {
            noProductionDaySet.addAll(stopDays);
            stopDays.forEach(stopDay -> {
                NoProductionDayMouldVo noProductionDayMould = new NoProductionDayMouldVo();
                noProductionDayMould.setDay(stopDay);
                noProductionDayMould.setNoProductionType(MouldNoProductionType.STOP_DAY);
                noProductionDayList.add(noProductionDayMould);
            });
        }
        if (!CollectionUtils.isEmpty(noProductionDayList)) {
            helper.setNoProductionInfo(JSON.toJSONString(noProductionDayList));
        }
        //构建剩余硫化时间
        for (Integer day = FactoryConstant.MONTH_START_DAY; day <= monthMaxDay; day++) {
            if (noProductionDaySet.contains(day)) {
                continue;
            }
            dayLeftOverCuringTimeMap.put(day, dayMaxCuringTime);
        }
        helper.setProductionCuringTimeInfo(dayLeftOverCuringTimeMap);
        return helper;
    }

    /**
     * 根据物料的月度可用及模具维修信息，得到其物料最大可用模具信息
     *
     * @param monthEnableList      物料模具关系月度可用模具
     * @param monthMaintenanceList 物料模具关系月度维修模具
     * @return
     */
    public static Map<String, MouldProductRelationDto> getMaxEnableMould(List<MouldProductRelationDto> monthEnableList, List<MouldProductRelationDto> monthMaintenanceList) {
        Map<String, MouldProductRelationDto> maxEnableMouldMap = new HashMap<>();
        //月度可用
        if (!CollectionUtils.isEmpty(monthEnableList)) {
            monthEnableList.stream().forEach(monthEnable -> maxEnableMouldMap.put(monthEnable.getMouldCode(), monthEnable));
        }
        if (CollectionUtils.isEmpty(monthMaintenanceList)) {
            return maxEnableMouldMap;
        }
        //合并维修
        monthMaintenanceList.stream().forEach(maintenance -> {
            String mouldCode = maintenance.getMouldCode();
            MouldProductRelationDto exist = maxEnableMouldMap.get(mouldCode);
            if (null == exist) {
                maintenance.setNoProductionList(maintenance.getNoProductionDay());
                maxEnableMouldMap.put(mouldCode, maintenance);
                return;
            }
            if (null == exist.getBeginDate()) {
                maintenance.setNoProductionList(maintenance.getNoProductionDay());
                maxEnableMouldMap.put(mouldCode, maintenance);
                return;
            }
            maintenance.getNoProductionList().addAll(maintenance.getNoProductionDay());
        });
        return maxEnableMouldMap;
    }

    /**
     * 判断校验是否不通过
     *
     * @param checkResult 校验信息对象
     * @return
     */
    public static boolean isCheckNoPass(AjaxResult checkResult) {
        return AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG);
    }

    /**
     * 校验计划是否能够调减，主要校验数量
     *
     * @param productionPlan 原排产计划
     * @param adjustPlan     计划调整信息对象
     * @return
     */
    public static AjaxResult checkAdjustNumberByProductionPlan(FactoryMonthPlanProdFinal productionPlan, FactoryMonthPlanAdjustPlanVo adjustPlan) {
        if (!productionPlan.getProductCode().equals(adjustPlan.getProductCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkAdjustPlanProductCode"));
        }
        //需要调减总量
        Integer adjustNumber = -adjustPlan.getAdjustNumber();
        Integer sumProductionQty = BigDecimal.ZERO.intValue();
        //调减起始日
        Date startDate = adjustPlan.getStartDate();
        Integer startDay = DateUtils.getDaysByMonth(startDate);
        //调减最大结束日
        Integer year = productionPlan.getYear();
        Integer month = productionPlan.getMonth();
        Integer endDay = DateUtils.getDaysByYearMonth(year, month);
        String fieldName;
        for (int day = startDay; day <= endDay; day++) {
            fieldName = String.format("day%d", day);
            //原有日排产量
            Long productionQty = (Long) productionPlan.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            sumProductionQty = sumProductionQty + productionQty.intValue();
        }
        if (adjustNumber > sumProductionQty) {
            return AjaxResult.error(String.format(I18nUtil.getMessage("ui.data.query.param.checkAdjustNumberMax"), sumProductionQty));
        }
        return AjaxResult.success();
    }

    /**
     * 对已排产计划减量，按adjustPlan的startDate开始，总共减少-adjustNumber的量
     * 会对productionPlan进行更新，同时会更新对应模具的剩余硫化时间
     * 计划调减时，adjustPlanLog = adjustPlan
     * 计划调增，需要其它计划调减时 adjustPlanLog ≠ adjustPlan
     *
     * @param finalVersionInfo       版本信息
     * @param adjustPlanLog          原调整计划信息
     * @param adjustPlan             需要调减计划信息
     * @param productionPlan         原排产计划
     * @param productionMouldInfoMap 排产模具信息
     */
    public static void productionPlanSubtractQty(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlanLog, FactoryMonthPlanAdjustPlanVo adjustPlan, FactoryMonthPlanProdFinal productionPlan, Map<String, MouldingProductionResultHelper> productionMouldInfoMap) {
        if (!productionPlan.getProductCode().equals(adjustPlan.getProductCode())) {
            return;
        }
        //需要调减总量
        Integer adjustNumber = -adjustPlan.getAdjustNumber();
        //调减起始日
        Date startDate = adjustPlan.getStartDate();
        Integer startDay = DateUtils.getDaysByMonth(startDate);
        //调减最大结束日
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        Integer endDay = DateUtils.getDaysByYearMonth(year, month);
        //调减物料代码
        String productCode = adjustPlan.getProductCode();
        String fieldName;
        //销售需求计划、分厂排产需求、实际排产量同降低
        Long totalQty = Long.valueOf(productionPlan.getTotalQty());
        Long factoryProdReqQty = Long.valueOf(productionPlan.getFactProdReqQty());
        Long reqPlanQty = Long.valueOf(productionPlan.getProdReqPlan());
        for (int day = startDay; day <= endDay; day++) {
            if (adjustNumber == 0) {
                break;
            }
            fieldName = String.format("day%d", day);
            //原有日排产量
            Long productionQty = (Long) productionPlan.getFieldValueByFieldName(fieldName);
            if (null == productionQty) {
                continue;
            }
            //原有日已排模具
            Map<String, MouldingProductionResultHelper> dayProductionMouldMap = getDayProductionMouldInfo(productionMouldInfoMap, day, productCode);
            if (CollectionUtils.isEmpty(dayProductionMouldMap)) {
                continue;
            }
            //可减量，日排产量与剩余调减量取最小
            Integer subtractQty = Math.min(productionQty.intValue(), adjustNumber);
            int mouldSize = dayProductionMouldMap.keySet().size();
            //均摊
            subtractQty = subtractQty / mouldSize * mouldSize;
            Long newProductionQty = productionQty - subtractQty;
            adjustNumber = adjustNumber - subtractQty;
            totalQty = totalQty - subtractQty;
            //日排产新值
            productionPlan.setFieldValueByFieldName(fieldName, newProductionQty);
            //记录日志
            String subtractNumberFormat = "在[%s]日对计划单号：%s 使用模具：[%s]调减了共[%d]的排产量";
            String mouldCodeInfo = new ArrayList<>(dayProductionMouldMap.keySet()).stream().collect(Collectors.joining(StringConstant.COMMA));
            String subtractNumberLog = String.format(subtractNumberFormat, day, adjustPlan.getProductionNo(), mouldCodeInfo, subtractQty);
            log.info(String.format("====计划调整减量：%s======", subtractNumberLog));
            adjustPlanLog.addAdjustProductionLog(adjustPlanLog.getProductionNo(), subtractNumberLog);
            //更新模具信息
            setMouldLeftOverCuringTime(dayProductionMouldMap, day, subtractQty.intValue(), BigDecimal.valueOf(productionPlan.getCuringTime()));
        }
//        BigDecimal singleCuringTime = productionPlan.getCuringTime();
//        productionPlan.setTotalVulcanizationMinutes(singleCuringTime.multiply(BigDecimal.valueOf(totalQty)).divide(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP));
//        //实际调减量
//        Long realAdjustNumber = productionPlan.getTotalQty() - totalQty;
//        productionPlan.setTotalQty(totalQty);
//        productionPlan.setFactProdReqQty(factoryProdReqQty - realAdjustNumber);
//        productionPlan.setProdReqPlan(reqPlanQty - realAdjustNumber);
//        //有可能超
//        productionPlan.setDifferenceQty(productionPlan.getFactProdReqQty() - totalQty);
    }

    /**
     * 单模具增量
     *
     * @param adjustPlanLog   存储调整日志--也是调整计划参数对象
     * @param helper          模具排产结果辅助对象
     * @param addNumberPlan   增量计划信息
     * @param calculateHelper 增量辅助类
     */
    public static void addSingleMouldQty(FactoryMonthPlanAdjustPlanVo adjustPlanLog, MouldingProductionResultHelper helper, FactoryMonthPlanProdFinal addNumberPlan, SingleMouldAdjustCalculateHelper calculateHelper) {
        BigDecimal singleCuringTime = BigDecimal.ONE;
        Integer startDay = calculateHelper.getStartDay();
        Integer maxDay = calculateHelper.getMaxDay();
        String mouldCode = calculateHelper.getMouldCode();
        Set<String> productionMouldSet = calculateHelper.getProductionMouldSet();
        Set<String> addMouldSet = calculateHelper.getAddMouldSet();
        Map<String, MouldProductRelationDto> maxEnableMouldMap = calculateHelper.getMaxEnableMouldMap();
        //加入已排标记
        productionMouldSet.add(mouldCode);
        Integer beginDate = calculateHelper.getBeginDate();
        Integer endDay = calculateHelper.getEndDay();
        Long sumAddQty = calculateHelper.getNeedAddQty();
        //不需要赋值，故而可以不new
        Set<Integer> fixedNoProductionDaySet = helper.getFixedNoProductionDayList();
        Map<Integer, BigDecimal> leftOverCuringTimeMap = helper.getDayLeftOverCuringTimeMap();
        if (null == leftOverCuringTimeMap) {
            leftOverCuringTimeMap = new HashMap<>();
        }
        for (Integer day = startDay; day <= maxDay; day++) {
            if (fixedNoProductionDaySet.contains(day)) {
                continue;
            }
            BigDecimal leftOverCuringTime = leftOverCuringTimeMap.get(day);
            if (null == leftOverCuringTime || singleCuringTime.compareTo(leftOverCuringTime) > 0) {
                continue;
            }
            if (null != addMouldSet && !addMouldSet.contains(mouldCode)) {
                addMouldSet.add(mouldCode);
            }
            if (beginDate > day) {
                beginDate = day;
            }
            if (endDay < day) {
                endDay = day;
            }
            addNumberPlan.setSpecCode(maxEnableMouldMap.get(mouldCode).getSpecCode());
            //计算增量
            Long addQty = leftOverCuringTime.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
            Integer realAddQty = Math.min(sumAddQty.intValue(), addQty.intValue());
            //计算日剩余硫化时间
            BigDecimal realLeftOverCuringTime = leftOverCuringTime.subtract(singleCuringTime.multiply(BigDecimal.valueOf(realAddQty)));
            leftOverCuringTimeMap.put(day, realLeftOverCuringTime);
            helper.setProductionCuringTimeInfo(leftOverCuringTimeMap);
            //记录日志
            String addNumberFormat = "在[%s]日对计划单号：%s 使用模具：[%s]调增了[%d]的排产量";
            String addNumberLog = String.format(addNumberFormat, day, addNumberPlan.getProductionNo(), mouldCode, realAddQty);
            log.info(String.format("====计划调整增量：%s======", addNumberLog));
            adjustPlanLog.addAdjustProductionLog(adjustPlanLog.getProductionNo(), addNumberLog);
            //加入日排产信息
            resetDayProductionInfo(helper, day, realAddQty, addNumberPlan.getProductCode());
            //日排产量值更新
            String fieldName = String.format("day%d", day);
            Long dayQty = (Long) addNumberPlan.getFieldValueByFieldName(fieldName);
            if (null == dayQty) {
                addNumberPlan.setFieldValueByFieldName(fieldName, realAddQty);
            } else {
                addNumberPlan.setFieldValueByFieldName(fieldName, dayQty + realAddQty);
            }
            sumAddQty = sumAddQty - realAddQty;
            if (sumAddQty == 0) {
                break;
            }
        }
        calculateHelper.setNeedAddQty(sumAddQty);
        calculateHelper.setBeginDate(beginDate);
        calculateHelper.setEndDay(endDay);
    }

    /**
     * 单模具单天可增加排产量
     *
     * @param adjustPlanLog   记录调整计划日志--即调整计划参数对象
     * @param helper          排产模具
     * @param addNumberPlan   增加的排产量
     * @param calculateHelper 排产辅助类
     * @param productionDay   排产天数
     */
    public static void addSingleMouldDayQty(FactoryMonthPlanAdjustPlanVo adjustPlanLog, MouldingProductionResultHelper helper, FactoryMonthPlanProdFinal addNumberPlan, SingleMouldAdjustCalculateHelper calculateHelper, Integer productionDay) {
        BigDecimal singleCuringTime = BigDecimal.ONE;
        String mouldCode = calculateHelper.getMouldCode();
        Integer beginDate = calculateHelper.getBeginDate();
        Integer endDay = calculateHelper.getEndDay();
        Long sumAddQty = calculateHelper.getNeedAddQty();
        Set<String> productionMouldSet = calculateHelper.getProductionMouldSet();
        Set<String> addMouldSet = calculateHelper.getAddMouldSet();
        Map<String, MouldProductRelationDto> maxEnableMouldMap = calculateHelper.getMaxEnableMouldMap();
        //不需要赋值，故而可以不new
        Set<Integer> fixedNoProductionDaySet = helper.getFixedNoProductionDayList();
        Map<Integer, BigDecimal> leftOverCuringTimeMap = helper.getDayLeftOverCuringTimeMap();
        if (null == leftOverCuringTimeMap) {
            leftOverCuringTimeMap = new HashMap<>();
        }
        if (fixedNoProductionDaySet.contains(productionDay)) {
            return;
        }
        BigDecimal leftOverCuringTime = leftOverCuringTimeMap.get(productionDay);
        if (null == leftOverCuringTime || singleCuringTime.compareTo(leftOverCuringTime) > 0) {
            return;
        }
        if (null != addMouldSet && !addMouldSet.contains(mouldCode)) {
            addMouldSet.add(mouldCode);
        }
        if (beginDate > productionDay) {
            beginDate = productionDay;
        }
        if (endDay < productionDay) {
            endDay = productionDay;
        }
        addNumberPlan.setSpecCode(maxEnableMouldMap.get(helper.getMouldCode()).getSpecCode());
        productionMouldSet.add(helper.getMouldCode());
        //计算增量
        Long addQty = leftOverCuringTime.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
        Integer realAddQty = Math.min(sumAddQty.intValue(), addQty.intValue());
        //计算日剩余硫化时间
        BigDecimal realLeftOverCuringTime = leftOverCuringTime.subtract(singleCuringTime.multiply(BigDecimal.valueOf(realAddQty)));
        leftOverCuringTimeMap.put(productionDay, realLeftOverCuringTime);
        helper.setProductionCuringTimeInfo(leftOverCuringTimeMap);
        //记录日志
        String addNumberFormat = "在[%s]日对计划单号：%s 使用模具：[%s]调增了[%d]的排产量";
        String addNumberLog = String.format(addNumberFormat, productionDay, addNumberPlan.getProductionNo(), mouldCode, realAddQty);
        log.info(String.format("====计划调整增量：%s======", addNumberLog));
        adjustPlanLog.addAdjustProductionLog(adjustPlanLog.getProductionNo(), addNumberLog);
        //加入日排产信息
        resetDayProductionInfo(helper, productionDay, realAddQty, addNumberPlan.getProductCode());
        //日排产量值更新
        String fieldName = String.format("day%d", productionDay);
        Long dayQty = (Long) addNumberPlan.getFieldValueByFieldName(fieldName);
        if (null == dayQty) {
            addNumberPlan.setFieldValueByFieldName(fieldName, realAddQty);
        } else {
            addNumberPlan.setFieldValueByFieldName(fieldName, dayQty + realAddQty);
        }
        sumAddQty = sumAddQty - realAddQty;
        calculateHelper.setNeedAddQty(sumAddQty);
        calculateHelper.setBeginDate(beginDate);
        calculateHelper.setEndDay(endDay);
    }

    /**
     * 根据日增加的排产量，重新设置日排产规格信息
     *
     * @param helper      模具排产结果对象
     * @param day         排产日
     * @param addQty      增加的量
     * @param productCode 排产规格
     */
    private static void resetDayProductionInfo(MouldingProductionResultHelper helper, Integer day, Integer addQty, String productCode) {
        Map<Integer, List<ProductProductionInfoVo>> dayProductionInfoMap = helper.getDayProductionInfo();
        if (CollectionUtils.isEmpty(dayProductionInfoMap)) {
            dayProductionInfoMap = new HashMap<>();
        }
        List<ProductProductionInfoVo> dayProductionInfoList = dayProductionInfoMap.get(day);
        if (CollectionUtils.isEmpty(dayProductionInfoList)) {
            dayProductionInfoList = new ArrayList<>();
        }
        ProductProductionInfoVo productProductionInfo = new ProductProductionInfoVo();
        productProductionInfo.setProductionQty(Long.valueOf(addQty));
        productProductionInfo.setProductCode(productCode);
        dayProductionInfoList.add(productProductionInfo);
        dayProductionInfoMap.put(day, dayProductionInfoList);
        helper.setDayProductionInfo(dayProductionInfoMap);
    }

    /**
     * 得到在day日可排产的模具信息
     *
     * @param productionMouldInfoMap 总排产模具
     * @param day                    排产日
     * @param singleCuringTime       单条硫化时间(包含间隔增加时间)
     * @return
     */
    public static Map<String, MouldingProductionResultHelper> getDayProductionMouldInfo(Map<String, MouldingProductionResultHelper> productionMouldInfoMap, Integer day, BigDecimal singleCuringTime) {
        if (CollectionUtils.isEmpty(productionMouldInfoMap)) {
            return Collections.emptyMap();
        }
        Map<String, MouldingProductionResultHelper> dayProductionMouldMap = new HashMap<>();
        productionMouldInfoMap.entrySet().forEach(entry -> {
            MouldingProductionResultHelper helper = entry.getValue();
            BigDecimal leftOverCuringTime = helper.getDayLeftOverCuringTimeMap().get(day);
            if (null == leftOverCuringTime) {
                return;
            }
            if (leftOverCuringTime.compareTo(singleCuringTime) > 0) {
                dayProductionMouldMap.put(entry.getKey(), helper);
            }
        });
        return dayProductionMouldMap;
    }

    /**
     * 得到在day日排产的模具信息
     *
     * @param productionMouldInfoMap 总排产模具
     * @param day                    排产日
     * @param productCode            排产物料编码
     * @return
     */
    private static Map<String, MouldingProductionResultHelper> getDayProductionMouldInfo(Map<String, MouldingProductionResultHelper> productionMouldInfoMap, Integer day, String productCode) {
        if (CollectionUtils.isEmpty(productionMouldInfoMap)) {
            return Collections.emptyMap();
        }
        Map<String, MouldingProductionResultHelper> dayProductionMouldMap = new HashMap<>();
        productionMouldInfoMap.entrySet().forEach(entry -> {
            MouldingProductionResultHelper helper = entry.getValue();
            if (helper.isProductionProductByDay(day, productCode)) {
                dayProductionMouldMap.put(helper.getMouldCode(), helper);
            }
        });
        return dayProductionMouldMap;
    }

    /**
     * 重新设置模具的日剩余时间
     *
     * @param dayProductionMouldMap 日排产模具信息
     * @param day                   排产日
     * @param subtractQty           需减少量
     */
    private static void setMouldLeftOverCuringTime(Map<String, MouldingProductionResultHelper> dayProductionMouldMap, Integer day, Integer subtractQty, BigDecimal singleCuringTime) {
        if (CollectionUtils.isEmpty(dayProductionMouldMap)) {
            return;
        }
        //平均分配
        int size = dayProductionMouldMap.keySet().size();
        int singleQty = subtractQty.intValue() / size;
        BigDecimal addCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(singleQty));
        dayProductionMouldMap.entrySet().forEach(entry -> {
            MouldingProductionResultHelper helper = entry.getValue();
            Map<Integer, BigDecimal> dayLeftOverCuringTimeMap = helper.getDayLeftOverCuringTimeMap();
            if (CollectionUtils.isEmpty(dayLeftOverCuringTimeMap)) {
                return;
            }
            BigDecimal leftOverCuringTime = dayLeftOverCuringTimeMap.get(day);
            if (null == leftOverCuringTime) {
                leftOverCuringTime = BigDecimal.ZERO;
            }
            leftOverCuringTime = leftOverCuringTime.add(addCuringTime);
            dayLeftOverCuringTimeMap.put(day, leftOverCuringTime);
            helper.setProductionCuringTimeInfo(dayLeftOverCuringTimeMap);
        });
    }

    /**
     * 设置生胎代码
     *
     * @param specCodeInfo   硫化施工信息
     * @param productionPlan 排产计划
     * @param specCode       硫化规格代码
     */
    private static void setEmbryoCodeInfo(String specCodeInfo, FactoryMonthPlanProdFinal productionPlan, String specCode) {
        if (StringUtils.isBlank(specCodeInfo) || StringUtils.isBlank(specCode)) {
            return;
        }
        List<ProductSpecInfoVo> productSpecCodeInfoList = JSON.parseArray(specCodeInfo, ProductSpecInfoVo.class);
        if (CollectionUtils.isEmpty(productSpecCodeInfoList)) {
            return;
        }
        Map<String, ProductSpecInfoVo> productSpecInfoMap = productSpecCodeInfoList.stream().collect(Collectors.toMap(ProductSpecInfoVo::getSpecCode, Function.identity()));
        if (CollectionUtils.isEmpty(productSpecInfoMap)) {
            return;
        }
        ProductSpecInfoVo productSpecInfo = productSpecInfoMap.get(specCode);
        if (null == productSpecInfo) {
            return;
        }
        productionPlan.setMouldMethod(productSpecInfo.getMouldMethod());
        productionPlan.setEmbryoCode(productSpecInfo.getEmbryoCode());
    }


    private AdjustUtils() {

    }
}
