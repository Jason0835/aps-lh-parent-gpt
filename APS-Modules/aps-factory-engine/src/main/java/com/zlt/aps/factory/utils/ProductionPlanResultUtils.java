package com.zlt.aps.factory.utils;

import com.alibaba.fastjson.JSON;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.MouldDayProductionVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductProductionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.SinglePlanInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产计划结果相关业务
 * <p>
 * 针对排产模具结果构建排产结果对象
 * 构建排产结果明细，构建汇总排产结果明细
 * 构建未排产计划
 *
 * @author ZLT
 * @date 20250315
 */
@Slf4j
public class ProductionPlanResultUtils {
    /**
     * 对模具排产结果--间断处理
     *
     * @param productionContext
     */
    public static void discontinuityMove(ProductionContext productionContext) {
        Map<String, MouldInfoVO> mouldInfoMap = productionContext.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return;
        }
        mouldInfoMap.forEach((mouldCode, mouldInfo) -> {
            //模具排产列表
            Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
            if (CollectionUtils.isEmpty(dayProductionMap)) {
                return;
            }
            Set<Integer> effectiveDaySet = new HashSet<>();
            effectiveDaySet.addAll(dayProductionMap.keySet());
            Map<Integer, NoProductionDayMouldVo> noProductionDayList = mouldInfo.getNoProductionDayList();
            if (!CollectionUtils.isEmpty(noProductionDayList)) {
                effectiveDaySet.addAll(noProductionDayList.keySet());
            }
            List<Integer> effectiveDayList = new ArrayList<>(effectiveDaySet);
            List<List<Integer>> continueList = getContinuousNumbers(effectiveDayList);

            //得到最大连续排产时间段

        });
    }

    /**
     * 构建模具排产结果辅助记录信息集合
     *
     * @param productionContext
     * @return
     */
    public static List<MouldingProductionResultHelper> buildMouldProductionResult(ProductionContext productionContext) {
        Map<String, MouldInfoVO> mouldInfoMap = productionContext.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return Collections.emptyList();
        }
        Map<String, Set<String>> mouldRelationProductMap = productionContext.getMouldRelationProductMap();
        List<MouldingProductionResultHelper> mouldingDetailList = new ArrayList<>();
        mouldInfoMap.entrySet().stream().forEach(entry -> {
            String mouldCode = entry.getKey();
            MouldingProductionResultHelper helper = buildMouldingResultHelper(productionContext, mouldCode);
            MouldInfoVO mouldInfo = entry.getValue();
            helper.setMouldNo(mouldInfo.getMouldNo());
            helper.setIsContinue(mouldInfo.getIsContinue());
            Map<Integer, BigDecimal> productionDayMap = mouldInfo.getProductionDayList();
            if (!CollectionUtils.isEmpty(productionDayMap)) {
                helper.setProductionCuringTimeInfo(productionDayMap);
            }
            Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
            setDayProductionInfo(helper, dayProductionMap);
            Map<Integer, NoProductionDayMouldVo> noProductionDayMap = mouldInfo.getNoProductionDayList();
            if (!CollectionUtils.isEmpty(noProductionDayMap)) {
                List<NoProductionDayMouldVo> noProductionDayList = noProductionDayMap.entrySet().stream().map(noEntry -> noEntry.getValue()).collect(Collectors.toList());
                helper.setNoProductionInfo(JSON.toJSONString(noProductionDayList));
            }
            helper.setUsedSeconds(mouldInfo.getUsedSeconds().intValue());
            helper.setTotalSeconds(mouldInfo.getTotalSeconds().intValue());
            helper.setMouldType(mouldInfo.getMouldType());
            ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
            if (null != productionOrient) {
                helper.setProductionOrient(productionOrient.getValue());
            }
            Set<String> relationProductSet = mouldRelationProductMap.get(mouldCode);
            if (!CollectionUtils.isEmpty(relationProductSet)) {
                helper.setRelationProductInfo(relationProductSet.stream().collect(Collectors.joining(",")));
            }
            mouldingDetailList.add(helper);
        });
        return mouldingDetailList;
    }

    /**
     * 根据排产上下文中的模具排产列表，
     * 构建模具排产结果明细列表对象
     * 每个模具的日排产列表构建，按计划、模具号、规格构建一个结果明细对象
     * 模具计划日排产转化成日排产列
     *
     * @param productionContext
     * @return
     */
    public static List<MonthPlanProductionResultDetail> buildProductionResultDetailList(ProductionContext productionContext) {
        Map<String, MouldInfoVO> mouldInfoMap = productionContext.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return Collections.emptyList();
        }
        List<MonthPlanProductionResultDetail> detailList = new ArrayList<>();
        mouldInfoMap.forEach((mouldCode, mouldInfo) -> {
            //模具排产列表
            Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
            if (CollectionUtils.isEmpty(dayProductionMap)) {
                return;
            }
            //计划-日排产信息
            Map<Long, MonthPlanProductionResultDetail> mouldProductionPlanMap = new HashMap<>();
            convertMouldProductionList(dayProductionMap, mouldProductionPlanMap);
            if (CollectionUtils.isEmpty(mouldProductionPlanMap)) {
                return;
            }
            //模具排产的计划
            mouldProductionPlanMap.forEach((monthPlanId, mouldProduction) -> {
                MonthPlanManufacturingRequirementVo requirement = productionContext.getMonthPlanInitMap().get(monthPlanId);
                if (null == requirement) {
                    return;
                }
                //补充计划信息
                setPlanInfo(mouldProduction, requirement, productionContext);
                detailList.add(mouldProduction);
            });
        });
        return detailList;
    }

    /**
     * 汇总排产结果，按合并汇总维度
     *
     * @param detailList        排产计划明细
     * @param productionContext 排产上下文
     * @return
     */
    public static List<MonthPlanMouldingDayResult> getSummaryResult(List<MonthPlanProductionResultDetail> detailList, ProductionContext productionContext) {
        List<MonthPlanMouldingDayResult> summaryResultList = new ArrayList<>();
        //先进行分组
        Map<String, List<MonthPlanProductionResultDetail>> mergeGroupMap = detailList.stream().collect(Collectors.groupingBy(MonthPlanProductionResultDetail::getSummaryValue));
        mergeGroupMap.forEach((groupKey, groupDataList) -> {
            if (CollectionUtils.isEmpty(groupDataList)) {
                return;
            }
            MonthPlanMouldingDayResult daySummaryResult = new MonthPlanMouldingDayResult();
            Set<String> mouldSet = new HashSet<>();
            Set<Long> monthPlanIdSet = new HashSet<>();
            groupDataList.stream().forEach(singleData -> {
                monthPlanIdSet.add(singleData.getMonthPlanId());
                mouldSet.add(singleData.getMouldCode());
            });
            MonthPlanProductionResultDetail first = groupDataList.get(0);
            BeanUtils.copyProperties(first, daySummaryResult);
            daySummaryResult.setId(null);
            daySummaryResult.setMouldQty(mouldSet.size());
            daySummaryResult.setIsImport(YesOrNoEnum.NO.getValue());
            daySummaryResult.setCuringTime(first.getCuringTime());
            daySummaryResult.setMouldInfo(new ArrayList<>(mouldSet).stream().collect(Collectors.joining(StringConstant.COMMA)));
            //赋值拼接信息
            SinglePlanInfoHelper singlePlanHelper = new SinglePlanInfoHelper(first.getMonthPlanId(), first.getTotalQty(), first.getProductionSequence());
            daySummaryResult.setMergeInfo(JSON.toJSONString(singlePlanHelper));
            //合并统计叠加
            int max = groupDataList.size();
            for (int index = 1; index < max; index++) {
                summaryDayQtyInfo(daySummaryResult, groupDataList.get(index));
            }
            Long prodReqPlan = BigDecimal.ZERO.longValue();
            Long factProdReqQty = BigDecimal.ZERO.longValue();
            //未排原因
            String reason = "";
            for (Long monthPlanId : monthPlanIdSet) {
                MonthPlanManufacturingRequirementVo requirement = productionContext.getMonthPlanInitMap().get(monthPlanId);
                prodReqPlan = prodReqPlan + requirement.getProdReqPlan();
                factProdReqQty = factProdReqQty + requirement.getFactProdReqQty();
                String noProductionReason = requirement.getNoProductionReason();
                if (StringUtils.isBlank(noProductionReason)) {
                    continue;
                }
                if (StringUtils.isBlank(reason)) {
                    reason = noProductionReason;
                } else {
                    reason = String.format("%s,%s", reason, noProductionReason);
                }
            }
            if (!StringUtils.isBlank(reason)) {
                reason = String.format("[%s]", reason);
            }
            daySummaryResult.setProdReqPlan(prodReqPlan);
            daySummaryResult.setFactProdReqQty(factProdReqQty);
            daySummaryResult.setDifferenceQty(factProdReqQty - daySummaryResult.getTotalQty());
            daySummaryResult.setReason(reason);
            summaryResultList.add(daySummaryResult);
        });
        return summaryResultList;
    }

    /**
     * 得到汇总排产结果--一个SKU一条记录
     *
     * @param detailList        排产结果明细
     * @param productionContext 排产上下文
     * @return 按Sku合并排产结果集合
     */
    public static List<MonthPlanProductionDayResult> getSummaryByProductCodeResult(List<MonthPlanProductionResultDetail> detailList, ProductionContext productionContext) {
        List<MonthPlanProductionDayResult> productionDayList = new ArrayList<>();
        List<MonthPlanManufacturingRequirementVo> requirementList = new ArrayList<>(productionContext.getMonthPlanInitMap().values());
        //按SKU分组
        Map<String, List<MonthPlanProductionResultDetail>> productProductionGroupList = detailList.stream().collect(Collectors.groupingBy(MonthPlanProductionResultDetail::getProductCode));
        Map<String, List<MonthPlanManufacturingRequirementVo>> productRequirementGroupList = requirementList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        productProductionGroupList.forEach((productCode, productionList) -> {
            if (CollectionUtils.isEmpty(productionList)) {
                return;
            }
            List<MonthPlanManufacturingRequirementVo> productRequirementList = productRequirementGroupList.get(productCode);
            //需求基础信息
            MonthPlanProductionDayResult dayResult = getProductProductionBaseInfo(productRequirementList);
            if (null == dayResult) {
                return;
            }

            //未排产原因
            mergeNoProductionReason(dayResult, productRequirementList);
            //模具信息
            MonthPlanProductionResultDetail productionDetail = productionList.get(0);
            dayResult.setCuringTime(productionDetail.getCuringTime());
            dayResult.setMouldNo(productionDetail.getMouldNo());
            Set<String> mouldCodeSet = productionList.stream().map(MonthPlanProductionResultDetail::getMouldCode).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(mouldCodeSet)) {
                dayResult.setMouldQty(mouldCodeSet.size());
                dayResult.setMouldInfo(String.join(StringConstant.COMMA, new ArrayList<>(mouldCodeSet)));
            }
            //排产信息 开始日期、结束日期、排产量、日排产量、硫化时间
            productionList.forEach(productionInfo -> summaryDayQtyInfo(dayResult, productionInfo));
            dayResult.setDifferenceQty(dayResult.getFactProdReqQty() - dayResult.getTotalQty());
            productionDayList.add(dayResult);
        });
        return productionDayList;
    }

    /**
     * 根据排产结果信息，构建未排计划集合
     *
     * @param productionPlanMap     总排产计划信息
     * @param noProductionRecordMap 不排产计划
     * @param sumProductionMap      排产汇总
     * @return 未排产计划集合
     */
    public static List<MonthPlanNoProductionPlan> buildNoProductionPlanList(Map<Long, MonthPlanManufacturingRequirementVo> productionPlanMap, Map<Long, MonthPlanNoProductionRecord> noProductionRecordMap, Map<Long, Long> sumProductionMap) {
        if (CollectionUtils.isEmpty(productionPlanMap)) {
            return Collections.emptyList();
        }
        List<MonthPlanNoProductionPlan> noProductionPlanList = new ArrayList<>();
        List<MonthPlanManufacturingRequirementVo> productionPlantList = productionPlanMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
        productionPlantList.stream().forEach(productionPlan -> {
            //本身没有需求量的跳过
            Long factoryProductionReqQty = productionPlan.getFactProdReqQty();
            if (null == factoryProductionReqQty || factoryProductionReqQty <= BigDecimal.ZERO.longValue()) {
                return;
            }
            Long monthPlanId = productionPlan.getMonthPlanId();
            String unProductionReason = productionPlan.getNoProductionReason();
            MonthPlanNoProductionPlan noProductionPlan = new MonthPlanNoProductionPlan();
            BeanUtils.copyProperties(productionPlan, noProductionPlan);
            noProductionPlan.setId(null);
            noProductionPlan.setReason(unProductionReason);
            //20251026 ZLT 模具、规格代号、生胎代码
            noProductionPlan.setSpecCode(productionPlan.getSpecCode());
            noProductionPlan.setEmbryoCodeInfo(productionPlan.getEmbryoCode());
            noProductionPlan.setMouldNoInfo(productionPlan.getMouldNoInfo());
            //有排产计划，则取排产数量
            if (sumProductionMap.containsKey(monthPlanId)) {
                Long plannedQty = sumProductionMap.get(monthPlanId);
                Long needProductionQty = productionPlan.getFactProdReqQty();
                if (!needProductionQty.equals(plannedQty)) {
                    noProductionPlan.setUnProductionQty(needProductionQty - plannedQty);
                    noProductionPlanList.add(noProductionPlan);
                }
                return;
            }
            //不排产计划
            if (noProductionRecordMap.containsKey(monthPlanId)) {
                noProductionPlan.setUnProductionQty(noProductionRecordMap.get(monthPlanId).getQty());
                noProductionPlanList.add(noProductionPlan);
                return;
            }
            //即没有排产计划，又不是不排产计划
            Long unProductionQty = productionPlan.getNoProductionQty();
            if (StringUtils.isNotBlank(unProductionReason) && null != unProductionQty && unProductionQty >= BigDecimal.ZERO.longValue()) {
                noProductionPlan.setUnProductionQty(unProductionQty);
                noProductionPlanList.add(noProductionPlan);
            }
        });
        return noProductionPlanList;
    }

    /**
     * 获取连续天数排产list
     *
     * @param effectiveDayList 有效时间集合
     * @return 取得连续排产的时间段集合
     */
    private static List<List<Integer>> getContinuousNumbers(List<Integer> effectiveDayList) {
        if (CollectionUtils.isEmpty(effectiveDayList)) {
            return Collections.emptyList();
        }
        List<List<Integer>> result = new ArrayList<>();
        int start = 0;
        int end = 0;
        int size = effectiveDayList.size();
        while (end < size) {
            int next = end + 1;
            if ((next < size) && effectiveDayList.get(next) == effectiveDayList.get(end) + 1) {
                end++;
            } else {
                if (start != end) {
                    result.add(effectiveDayList.subList(start, next));
                }
                end++;
                start = end;
            }
        }
        return result;
    }

    /**
     * 根据排产上下文及模具号，构建模具配置结果辅助信息对象
     *
     * @param productionContext 排产上下文
     * @param mouldCode         模具号
     * @return 模具排产信息对象
     */
    private static MouldingProductionResultHelper buildMouldingResultHelper(ProductionContext productionContext, String mouldCode) {
        MouldingProductionResultHelper helper = new MouldingProductionResultHelper();
        helper.setFactoryCode(productionContext.getFactoryCode());
        helper.setYear(productionContext.getYear());
        helper.setMonth(productionContext.getMonth());
        helper.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        helper.setProductionVersion(productionContext.getProductionVersion());
        helper.setMouldCode(mouldCode);
        return helper;
    }

    /**
     * 模具排产信息转化处理
     *
     * @param dayProductionMap       模具日排产列表集合
     * @param mouldProductionPlanMap 按计划转成排产明细
     */
    private static void convertMouldProductionList(Map<Integer, List<MouldDayProductionVo>> dayProductionMap, Map<Long, MonthPlanProductionResultDetail> mouldProductionPlanMap) {
        dayProductionMap.forEach((productionDate, mouldDayProductionList) -> {
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                return;
            }
            //模具日排产计划
            mouldDayProductionList.stream().forEach(mouldDayProduction -> {
                Long monthPlanId = mouldDayProduction.getMonthPlanId();
                MonthPlanProductionResultDetail productionDetail = mouldProductionPlanMap.get(monthPlanId);
                if (null == productionDetail) {
                    productionDetail = new MonthPlanProductionResultDetail();
                    productionDetail.setMonthPlanId(mouldDayProduction.getMonthPlanId());
                    productionDetail.setProductCode(mouldDayProduction.getProductCode());
                    //模具
                    productionDetail.setMouldNo(mouldDayProduction.getMouldNo());
                    productionDetail.setMouldCode(mouldDayProduction.getMouldCode());
                    //硫化规格代号、成形法、胚胎号
                    productionDetail.setSpecCode(mouldDayProduction.getSpecCode());
                    productionDetail.setEmbryoCode(mouldDayProduction.getEmbryoCode());
                    productionDetail.setMouldMethod(mouldDayProduction.getMouldMethod());
                }
                setProductionDateQty(productionDetail, productionDate, mouldDayProduction.getProductionQty());
                mouldProductionPlanMap.put(monthPlanId, productionDetail);
            });
        });
    }


    /**
     * 补充计划相关信息
     *
     * @param productionDetail  日排产明细
     * @param requirement       排产需求计划
     * @param productionContext 排产上下文
     */
    private static void setPlanInfo(MonthPlanProductionResultDetail productionDetail, MonthPlanManufacturingRequirementVo requirement, ProductionContext productionContext) {
        BeanUtils.copyProperties(requirement, productionDetail);
        productionDetail.setId(null);
        productionDetail.setReason("");
        productionDetail.setDifferenceQty(null);
        productionDetail.setDisplaySeq(productionContext.getProductionSchedulePlanMap().get(requirement.getMonthPlanId()));
        //设置年月值
        Integer year = requirement.getYear();
        Integer month = requirement.getMonth();
        String yearAndMonth = String.format("%s%02d", year, month);
        productionDetail.setYearMonth(Integer.valueOf(yearAndMonth));
        //施工阶段
        productionDetail.setConstructionStage(requirement.getConstructionStage());
        Long totalValue = BigDecimal.ZERO.longValue();
        //统计汇总值
        Integer[] dayList = FactoryConstant.PRODUCTION_CYCLE;
        for (Integer day : dayList) {
            String fieldName = "";
            if (day > 0) {
                fieldName = "day";
            } else {
                fieldName = "preDay";
            }
            fieldName = fieldName + Math.abs(day);
            Long dayValue;
            Object value = productionDetail.getFieldValueByFieldName(fieldName);
            if (null == value) {
                dayValue = BigDecimal.ZERO.longValue();
            } else {
                dayValue = (Long) value;
            }
            totalValue = totalValue + dayValue;
        }
        productionDetail.setTotalQty(totalValue);
        BigDecimal curingTime = requirement.getCuringTime();
        if (null == curingTime) {
            curingTime = BigDecimal.ZERO;
        }
        Integer addCuringTime = (Integer) productionContext.getFactoryParams().get(FactoryConstant.SYS_PARAM_INTERVAL_TIME_OF_EMBRYO_EXCHANGE);
        if (null != addCuringTime) {
            //20250327 单条硫化间隔时间 转化成秒
            addCuringTime = BigDecimal.valueOf(addCuringTime).multiply(BigDecimal.valueOf(ProductionConstant.MINUTE_SECOND)).intValue();
            curingTime = curingTime.add(BigDecimal.valueOf(addCuringTime));
        }
        productionDetail.setProductionSequence(requirement.getProductionSequence());
        productionDetail.setCuringTime(curingTime);
        productionDetail.setTotalVulcanizationMinutes(curingTime.multiply(BigDecimal.valueOf(totalValue)).divide(BigDecimal.valueOf(ProductionConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP));
        productionDetail.setProdReqPlan(requirement.getProdReqPlan());
        productionDetail.setFactProdReqQty(requirement.getFactProdReqQty());
    }

    /**
     * 设置SKU的排产-基础需求信息
     *
     * @param requirementList SKU的排产需求计划
     * @return
     */
    private static MonthPlanProductionDayResult getProductProductionBaseInfo(List<MonthPlanManufacturingRequirementVo> requirementList) {
        if (CollectionUtils.isEmpty(requirementList)) {
            return null;
        }
        MonthPlanProductionDayResult dayResult = new MonthPlanProductionDayResult();
        MonthPlanManufacturingRequirementVo requirementBase = requirementList.get(0);
        BeanUtils.copyProperties(requirementBase, dayResult);
        dayResult.setId(null);
        dayResult.setIsImport(YesOrNoEnum.NO.getValue());
        dayResult.setMouldQty(null);
        //总需求量、净需求、备货需求、生产总需求(含损耗)，先置为零
        dayResult.setProdReqPlan(BigDecimal.ZERO.longValue());
        dayResult.setFactProdReqQty(BigDecimal.ZERO.longValue());
        dayResult.setNetDemandQty(BigDecimal.ZERO.longValue());
        dayResult.setStockUpDemandQty(BigDecimal.ZERO.longValue());
        requirementList.stream().forEach(requirementPlan -> {
            setMarkInfo(dayResult, requirementPlan);
            statisticsDemand(dayResult, requirementPlan);
            addRemarkInfo(dayResult, requirementPlan);
        });
        return dayResult;
    }

    /**
     * 统计值
     *
     * @param daySummaryResult 汇总数据对象
     * @param singleData       单条数据
     */
    private static void summaryDayQtyInfo(MonthPlanMouldingDayResult daySummaryResult, MonthPlanProductionResultDetail singleData) {
        //总硫化时长
        BigDecimal total = daySummaryResult.getTotalVulcanizationMinutes();
        if (null == total) {
            total = BigDecimal.ZERO;
        }
        total = total.add(singleData.getTotalVulcanizationMinutes());
        daySummaryResult.setTotalVulcanizationMinutes(total);
        //总排产量
        Long totalQty = daySummaryResult.getTotalQty();
        if (null == totalQty) {
            totalQty = BigDecimal.ZERO.longValue();
        }
        totalQty = totalQty + singleData.getTotalQty();
        daySummaryResult.setTotalQty(totalQty);
        //起始日期
        Integer beginDate = daySummaryResult.getBeginDate();
        if (null == beginDate) {
            daySummaryResult.setBeginDate(singleData.getBeginDate());
        } else {
            daySummaryResult.setBeginDate(Math.min(beginDate, singleData.getBeginDate()));
        }
        Integer endDate = daySummaryResult.getEndDay();
        if (null == endDate) {
            daySummaryResult.setEndDay(singleData.getEndDay());
        } else {
            daySummaryResult.setEndDay(Math.max(endDate, singleData.getEndDay()));
        }
        //合并信息
        SinglePlanInfoHelper singlePlanHelper = new SinglePlanInfoHelper(singleData.getMonthPlanId(), singleData.getTotalQty(), singleData.getProductionSequence());
        String mergeInfo = getMergeInfo(singlePlanHelper, daySummaryResult.getMergeInfo());
        daySummaryResult.setMergeInfo(mergeInfo);
        //日期合计汇总
        Integer[] dayList = FactoryConstant.PRODUCTION_CYCLE;
        for (Integer day : dayList) {
            String fieldName;
            if (day > 0) {
                fieldName = "day";
            } else {
                fieldName = "preDay";
            }
            fieldName = fieldName + Math.abs(day);
            Object value = singleData.getFieldValueByFieldName(fieldName);
            if (null != value) {
                Long productionValue = (Long) value;
                Object previousValue = daySummaryResult.getFieldValueByFieldName(fieldName);
                Long sumValue;
                if (null == previousValue) {
                    sumValue = BigDecimal.ZERO.longValue();
                } else {
                    sumValue = (Long) previousValue;
                }
                sumValue = sumValue + productionValue;
                daySummaryResult.setFieldValueByFieldName(fieldName, sumValue);
            }
        }
    }

    /**
     * 更新日排产数量
     *
     * @param productionDetail 排产明细对象
     * @param productionDate   排产日
     * @param productionQty    排产量
     */
    private static void setProductionDateQty(MonthPlanProductionResultDetail productionDetail, Integer productionDate, Long productionQty) {
        String fieldName;
        if (productionDate > 0) {
            fieldName = "day";
        } else {
            fieldName = "preDay";
        }
        fieldName = fieldName + Math.abs(productionDate);
        Long oldValue;
        Object value = productionDetail.getFieldValueByFieldName(fieldName);
        if (null == value) {
            oldValue = BigDecimal.ZERO.longValue();
        } else {
            oldValue = (Long) value;
        }
        Long newValue = oldValue + productionQty;
        productionDetail.setFieldValueByFieldName(fieldName, newValue);
        //起始日赋值
        Integer beginDate = productionDetail.getBeginDate();
        if (null == beginDate) {
            productionDetail.setBeginDate(productionDate);
        } else {
            productionDetail.setBeginDate(Math.min(beginDate, productionDate));
        }
        Integer endDay = productionDetail.getEndDay();
        if (null == endDay) {
            productionDetail.setEndDay(productionDate);
        } else {
            productionDetail.setEndDay(Math.max(endDay, productionDate));
        }
    }

    /**
     * 设置模具日排产信息json
     * 对排产日的排产信息，对前后规格合并数量
     *
     * @param helper           模具排产结果辅助对象
     * @param dayProductionMap 模具日排产信息
     */
    private static void setDayProductionInfo(MouldingProductionResultHelper helper, Map<Integer, List<MouldDayProductionVo>> dayProductionMap) {
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            return;
        }
        Map<Integer, List<ProductProductionInfoVo>> mergeProductMap = new HashMap<>();
        dayProductionMap.forEach((day, dayProductionList) -> {
            if (CollectionUtils.isEmpty(dayProductionList)) {
                return;
            }
            List<ProductProductionInfoVo> mergeProductList = new ArrayList<>();
            String currentProductCode = "";
            ProductProductionInfoVo mergeProduct = null;
            for (MouldDayProductionVo dayProduction : dayProductionList) {
                String productCode = dayProduction.getProductCode();
                if (currentProductCode.equals(productCode)) {
                    mergeProduct.setProductionQty(mergeProduct.getProductionQty() + dayProduction.getProductionQty());
                } else {
                    currentProductCode = productCode;
                    mergeProduct = new ProductProductionInfoVo();
                    mergeProduct.setProductCode(productCode);
                    mergeProduct.setProductionQty(dayProduction.getProductionQty());
                    mergeProductList.add(mergeProduct);
                }
            }
            mergeProductMap.put(day, mergeProductList);
        });
        if (!CollectionUtils.isEmpty(mergeProductMap)) {
            helper.setDayProductionInfo(mergeProductMap);
        }
    }

    /**
     * 得到拼接信息字符串
     *
     * @param singlePlanHelper 本次合并计划信息
     * @param mergeInfo        合并计划信息
     * @return 叠加合并信息后的json字符
     */
    private static String getMergeInfo(SinglePlanInfoHelper singlePlanHelper, String mergeInfo) {
        Map<Long, SinglePlanInfoHelper> mergeInfoMap = getMergeInfoMap(mergeInfo);
        if (CollectionUtils.isEmpty(mergeInfoMap)) {
            return JSON.toJSONString(singlePlanHelper);
        }
        Long planId = singlePlanHelper.getPlanId();
        SinglePlanInfoHelper origin = mergeInfoMap.get(planId);
        if (null != origin) {
            //旧计划-另外模,则更新数量
            origin.setQty(origin.getQty() + singlePlanHelper.getQty());
        } else {
            //新计划-增加
            mergeInfoMap.put(planId, singlePlanHelper);
        }
        String newMergeInfo = "";
        for (Map.Entry<Long, SinglePlanInfoHelper> entry : mergeInfoMap.entrySet()) {
            SinglePlanInfoHelper singlePlan = entry.getValue();
            String singlePlanInfo = JSON.toJSONString(singlePlan);
            if (StringUtils.isBlank(newMergeInfo)) {
                newMergeInfo = singlePlanInfo;
            } else {
                newMergeInfo = String.format("%s,%s", newMergeInfo, singlePlanInfo);
            }
        }
        return newMergeInfo;
    }

    /**
     * 根据拼接信息获取拼接计划信息map
     *
     * @param mergeInfo 拼接信息
     * @return 解析json后的结果集合
     */
    private static Map<Long, SinglePlanInfoHelper> getMergeInfoMap(String mergeInfo) {
        if (StringUtils.isBlank(mergeInfo)) {
            return Collections.emptyMap();
        }
        String jsonMergeInfo = String.format("[%s]", mergeInfo);
        List<SinglePlanInfoHelper> singlePlanList = JSON.parseArray(jsonMergeInfo, SinglePlanInfoHelper.class);
        if (CollectionUtils.isEmpty(singlePlanList)) {
            return Collections.emptyMap();
        }
        return singlePlanList.stream().collect(Collectors.toMap(SinglePlanInfoHelper::getPlanId, Function.identity()));
    }

    /**
     * 根据制造需求信息，设置其标记信息
     * 备货、欠产、急单、必保、重要客户、交期、续作
     *
     * @param dayResult       排产结果对象
     * @param requirementPlan 制造需求计划
     */
    private static void setMarkInfo(MonthPlanProductionDayResult dayResult, MonthPlanManufacturingRequirementVo requirementPlan) {
        if (YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsStockUp())) {
            dayResult.setIsStockUp(YesOrNoEnum.YES.getValue());
        }
        if (YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsContinue())) {
            dayResult.setIsContinue(YesOrNoEnum.YES.getValue());
        }
        if (YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsDebitPlan())) {
            dayResult.setIsDebitPlan(YesOrNoEnum.YES.getValue());
        }
        if (null != requirementPlan.getDeliveryDateDue()) {
            dayResult.setIsDeliveryDate(YesOrNoEnum.YES.getValue());
        }
        if (YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsImportantCustom())) {
            dayResult.setIsImportantCustom(YesOrNoEnum.YES.getValue());
        }
        if (YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsEmergency())) {
            dayResult.setIsEmergency(YesOrNoEnum.YES.getValue());
        }
        if (YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsEnsurePlan())) {
            dayResult.setIsEnsurePlan(YesOrNoEnum.YES.getValue());
        }
    }

    /**
     * 统计需求
     * 分净需求、备货需求
     * 分厂排产需求量汇总(含损耗)
     *
     * @param dayResult       按SKU合并的日排产结果对象
     * @param requirementPlan SKU的需求计划
     */
    private static void statisticsDemand(MonthPlanProductionDayResult dayResult, MonthPlanManufacturingRequirementVo requirementPlan) {
        //分厂总排产需求量(含损耗)
        Long sumFactProdReqQty = dayResult.getFactProdReqQty();
        Long factProdReqQty = requirementPlan.getFactProdReqQty();
        if (null != factProdReqQty) {
            dayResult.setFactProdReqQty(sumFactProdReqQty + factProdReqQty);
        }
        //计划需求量(区分净需求和备货需求)
        Long prodReqPlan = requirementPlan.getProdReqPlan();
        if (null == prodReqPlan) {
            return;
        }
        //总需求
        Long sumProdReqPlan = dayResult.getProdReqPlan();
        dayResult.setProdReqPlan(sumProdReqPlan + prodReqPlan);
        if (YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsStockUp())) {
            //备货需求
            Long stockUpDemandQty = dayResult.getStockUpDemandQty();
            dayResult.setStockUpDemandQty(stockUpDemandQty + prodReqPlan);
            return;
        }
        Long netDemandQty = dayResult.getNetDemandQty();
        //净需求
        dayResult.setNetDemandQty(netDemandQty + prodReqPlan);
    }

    /**
     * 叠加备注信息
     *
     * @param dayResult       SKU天排产结果对象(合并)
     * @param requirementPlan SKU需求计划
     */
    private static void addRemarkInfo(MonthPlanProductionDayResult dayResult, MonthPlanManufacturingRequirementVo requirementPlan) {
        String remark = requirementPlan.getRemark();
        if (StringUtils.isBlank(remark)) {
            return;
        }
        String remarkInfo = dayResult.getRemark();
        if (StringUtils.isBlank(remarkInfo)) {
            dayResult.setRemark(remark);
        } else {
            dayResult.setRemark(String.format("%s;%s", remarkInfo, remark));
        }
    }

    /**
     * 统计值
     *
     * @param dayResult  汇总数据对象
     * @param singleData 单条数据
     */
    private static void summaryDayQtyInfo(MonthPlanProductionDayResult dayResult, MonthPlanProductionResultDetail singleData) {
        //总硫化时长
        BigDecimal total = dayResult.getTotalVulcanizationMinutes();
        if (null == total) {
            total = BigDecimal.ZERO;
        }
        total = total.add(singleData.getTotalVulcanizationMinutes());
        dayResult.setTotalVulcanizationMinutes(total);
        //总排产量
        Long totalQty = dayResult.getTotalQty();
        if (null == totalQty) {
            totalQty = BigDecimal.ZERO.longValue();
        }
        totalQty = totalQty + singleData.getTotalQty();
        dayResult.setTotalQty(totalQty);
        //起始日期
        Integer beginDate = dayResult.getBeginDate();
        if (null == beginDate) {
            dayResult.setBeginDate(singleData.getBeginDate());
        } else {
            dayResult.setBeginDate(Math.min(beginDate, singleData.getBeginDate()));
        }
        Integer endDate = dayResult.getEndDay();
        if (null == endDate) {
            dayResult.setEndDay(singleData.getEndDay());
        } else {
            dayResult.setEndDay(Math.max(endDate, singleData.getEndDay()));
        }
        //日期合计汇总
        Integer[] dayList = FactoryConstant.PRODUCTION_CYCLE;
        for (Integer day : dayList) {
            String fieldName;
            if (day > 0) {
                fieldName = "day";
            } else {
                fieldName = "preDay";
            }
            fieldName = fieldName + Math.abs(day);
            Object value = singleData.getFieldValueByFieldName(fieldName);
            if (null == value) {
                continue;
            }
            Long productionValue = (Long) value;
            Object previousValue = dayResult.getFieldValueByFieldName(fieldName);
            Long sumValue;
            if (null == previousValue) {
                sumValue = BigDecimal.ZERO.longValue();
            } else {
                sumValue = (Long) previousValue;
            }
            sumValue = sumValue + productionValue;
            dayResult.setFieldValueByFieldName(fieldName, sumValue);
        }
    }

    /**
     * 合并未排原因
     *
     * @param dayResult              汇总数据
     * @param productRequirementList 未排原因集合
     */
    private static void mergeNoProductionReason(MonthPlanProductionDayResult dayResult, List<MonthPlanManufacturingRequirementVo> productRequirementList) {
        if (CollectionUtils.isEmpty(productRequirementList)) {
            return;
        }
        String reason = "";
        for (MonthPlanManufacturingRequirementVo requirement : productRequirementList) {
            String noProductionReason = requirement.getNoProductionReason();
            if (StringUtils.isBlank(noProductionReason)) {
                continue;
            }
            if (StringUtils.isBlank(reason)) {
                reason = noProductionReason;
            } else {
                reason = String.format("%s,%s", reason, noProductionReason);
            }
        }
        if (!StringUtils.isBlank(reason)) {
            reason = String.format("[%s]", reason);
        }
        dayResult.setReason(reason);
    }

    private ProductionPlanResultUtils() {

    }
}
