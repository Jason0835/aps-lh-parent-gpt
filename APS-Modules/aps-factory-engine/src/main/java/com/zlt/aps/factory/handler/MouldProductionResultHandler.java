package com.zlt.aps.factory.handler;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模具排产结果业务处理
 * TBR 为结构
 * PCR 为寸口
 *
 * @author ZLT
 * @date 20260101
 */
@Slf4j
public class MouldProductionResultHandler {
    /**
     * 得到模具排产结果
     *
     * @param productionContext 排产上下文
     * @return
     */
    public static List<FactoryMonthPlanMouldDayDetail> getMouldProductionResult(TbrProductionContext productionContext) {
        Map<String, ProductionMouldInfoVo> mouldProductionList = productionContext.getBaseDataContainer().getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldProductionList)) {
            return Collections.emptyList();
        }
        List<FactoryMonthPlanMouldDayDetail> detailLogList = new ArrayList<>();
        mouldProductionList.forEach((mouldCode, productionMouldInfo) -> {
            Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfo = productionMouldInfo.getDayProductionInfo();
            if (CollectionUtils.isEmpty(dayProductionInfo)) {
                return;
            }
            Map<Long, FactoryMonthPlanMouldDayDetail> mouldProductionLogMap = new HashMap<>();
            //转化成基础的日志明细对象
            convertMouldProductionLogList(dayProductionInfo, mouldProductionLogMap);
            if (CollectionUtils.isEmpty(mouldProductionLogMap)) {
                return;
            }
            mouldProductionLogMap.forEach((monthPlanId, planMouldInfo) -> {
                MonthPlanProductionRequirePlanVo planInfo = productionContext.getAllProductionPlan().get(monthPlanId);
                if (null == planInfo) {
                    return;
                }
                //补充计划信息
                fullDetailInfo(planMouldInfo, planInfo, productionContext);
                detailLogList.add(planMouldInfo);
            });
        });
        return detailLogList;
    }

    /**
     * 汇总信息
     *
     * @param detailLogList
     * @param productionContext
     * @return
     */
    public static List<FactoryMonthPlanMouldDayResult> getSummaryBySkuResult(List<FactoryMonthPlanMouldDayDetail> detailLogList, TbrProductionContext productionContext) {
        if (CollectionUtils.isEmpty(detailLogList)) {
            return Collections.emptyList();
        }
        List<MonthPlanProductionRequirePlanVo> allRequireList = productionContext.getAllProductionPlan().values().stream().collect(Collectors.toList());
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupRequireMap = allRequireList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        List<FactoryMonthPlanMouldDayResult> resultList = new ArrayList<>();
        Map<String, List<FactoryMonthPlanMouldDayDetail>> skuGroupDetailMap = detailLogList.stream().collect(Collectors.groupingBy(FactoryMonthPlanMouldDayDetail::getMaterialDesc));
        skuGroupDetailMap.forEach((materialDesc, detailLogInfo) -> {
            if (CollectionUtils.isEmpty(detailLogInfo)) {
                return;
            }
            List<MonthPlanProductionRequirePlanVo> requireList = skuGroupRequireMap.get(materialDesc);
            if (CollectionUtils.isEmpty(requireList)) {
                return;
            }
            FactoryMonthPlanMouldDayResult dayResult = buildBaseInfo(requireList);
            //未排原因
            mergeNoProductionReason(dayResult, requireList);
            //排产信息 开始日期、结束日期、排产量、日排产量、硫化时间
            detailLogInfo.forEach(productionInfo -> summaryDayQtyInfo(dayResult, productionInfo));
            dayResult.setDifferenceQty(dayResult.getFactProdReqQty() - dayResult.getTotalQty());
            resultList.add(dayResult);
        });
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Map<String, List<ProductionMouldInfoVo>> groupMainPatternMouldRelationMap = baseDataContainer.getGroupMainPatternMouldRelationMap();
        Map<String, List<MonthPlanProductMouldInfoVo>> skuMouldRelationMap = baseDataContainer.getSkuMouldRelationMap();
        Map<String, ProductionPlanGroupInfo> allGroupPlanInfo = productionContext.getGroupProductionInfo();
        //模具信息
        resultList.forEach(singleResult -> {
            List<ProductionMouldInfoVo> maxMouldList = groupMainPatternMouldRelationMap.get(singleResult.getGroupAndMainPattern());
            if (CollectionUtils.isEmpty(maxMouldList)) {
                singleResult.setMouldCavityQty(BigDecimal.ZERO.intValue());
            } else {
                singleResult.setMouldCavityQty(maxMouldList.size());
            }
            List<MonthPlanProductMouldInfoVo> skuMaxUsedList = skuMouldRelationMap.get(singleResult.getMaterialDesc());
            if (CollectionUtils.isEmpty(skuMaxUsedList)) {
                singleResult.setTypeBlockQty(BigDecimal.ZERO.intValue());
            } else {
                singleResult.setTypeBlockQty(skuMaxUsedList.size());
            }
            setMouldUsedInfo(singleResult);
            Set<String> cxMachineCodeSet = allGroupPlanInfo.get(singleResult.getStructureName()).getAllocationCxMachineCodeSet();
            if (!CollectionUtils.isEmpty(cxMachineCodeSet)) {
                singleResult.setCxMachineCode(String.join(StringConstant.COMMA, cxMachineCodeSet));
            }
        });
        return resultList;
    }

    /**
     * 将模具日排产信息转化成模具排产日志存储
     *
     * @param mouldDayProductionInfo 某个模具的所有日排产信息
     * @param productionLogMap       模具排产日志：以模具+计划Id
     */
    private static void convertMouldProductionLogList(Map<Integer, List<CxMouldDayProductionHelper>> mouldDayProductionInfo, Map<Long, FactoryMonthPlanMouldDayDetail> productionLogMap) {
        if (CollectionUtils.isEmpty(mouldDayProductionInfo)) {
            return;
        }
        mouldDayProductionInfo.forEach((productionDay, productionPlanList) -> {
            if (CollectionUtils.isEmpty(productionPlanList)) {
                return;
            }
            productionPlanList.forEach(singlePlanProductionInfo -> {
                Long monthPlanId = singlePlanProductionInfo.getMonthPlanId();
                FactoryMonthPlanMouldDayDetail detail = productionLogMap.get(monthPlanId);
                if (null == detail) {
                    detail = createInitDetailLog(singlePlanProductionInfo);
                }
                setProductionDateQty(detail, singlePlanProductionInfo.getProductionDate(), singlePlanProductionInfo.getProductionQty());
                productionLogMap.put(monthPlanId, detail);
            });
        });
    }

    /**
     * 填充信息
     * 物料信息、版本信息、施工信息、需求量信息
     *
     * @param logDetail         模具排产明细日志
     * @param planInfo          排产计划
     * @param productionContext 排产上下文
     */
    private static void fullDetailInfo(FactoryMonthPlanMouldDayDetail logDetail, MonthPlanProductionRequirePlanVo planInfo, TbrProductionContext productionContext) {
        //版本信息
        logDetail.setFactoryCode(productionContext.getFactoryCode());
        logDetail.setMonth(productionContext.getMonth());
        logDetail.setYear(productionContext.getYear());
        logDetail.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        logDetail.setProductionVersion(productionContext.getProductionVersion());
        logDetail.setPlanType(planInfo.getPlanType());
        //物料信息
        logDetail.setProductTypeCode(planInfo.getProductTypeCode());
        logDetail.setProductStatus(planInfo.getProductStatus());
        logDetail.setBrand(planInfo.getBrand());
        logDetail.setMesMaterialCode(planInfo.getMesMaterialCode());
        logDetail.setMainMaterialDesc(planInfo.getMainMaterialDesc());
        //硫化施工信息
        logDetail.setConstructionStage(planInfo.getConstructionStage());
        logDetail.setDayVulcanizationQty(planInfo.getDayVulcanizationQty());
        BigDecimal curingTime = planInfo.getCuringTime();
        logDetail.setCuringTime(curingTime.intValue());
        //需求量信息
        logDetail.setHeightQty(planInfo.getHeightQty());
        logDetail.setProdReqPlan(planInfo.getNetQty());
        Integer lossQty = planInfo.getHeightLossQty() + planInfo.getFactProdReqQty() - planInfo.getHeightQty() - planInfo.getNetQty();
        logDetail.setFactProdReqQty(planInfo.getFactProdReqQty() + lossQty);
        logDetail.setAverageSaleQty(planInfo.getAverageSaleQty());
        logDetail.setInventorySalesRatio(BigDecimal.valueOf(planInfo.getInventorySalesRatio()));
        //统计总排产量
        Integer totalValue = DayProductionHandler.summaryDayQty(logDetail, FactoryConstant.PRODUCTION_CYCLE);
        logDetail.setTotalQty(totalValue.intValue());
        //总硫化时间
        logDetail.setTotalVulcanizationMinutes(curingTime.multiply(BigDecimal.valueOf(totalValue)).divide(BigDecimal.valueOf(ProductionConstant.HOUR_SECOND), 1, BigDecimal.ROUND_FLOOR));
    }

    /**
     * 根据需求构建初始排产结果信息
     *
     * @param requireList
     * @return
     */
    private static FactoryMonthPlanMouldDayResult buildBaseInfo(List<MonthPlanProductionRequirePlanVo> requireList) {
        FactoryMonthPlanMouldDayResult dayResult = new FactoryMonthPlanMouldDayResult();
        MonthPlanProductionRequirePlanVo requirePlan = requireList.get(BigDecimal.ZERO.intValue());
        BeanUtils.copyProperties(requirePlan, dayResult);
        dayResult.setId(null);
        dayResult.setIsImport(YesOrNoEnum.NO.getCode());
        dayResult.setCuringTime(requirePlan.getCuringTime().intValue());
        //设置年月值
        Integer year = requirePlan.getYear();
        Integer month = requirePlan.getMonth();
        String yearAndMonth = String.format("%s%02d", year, month);
        dayResult.setYearMonth(Integer.valueOf(yearAndMonth));
        /**
         * 汇总需求信息-净需求(含损耗)、高优先级数量
         */
        //高优先级量
        Integer heightNetQty = requireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightQty).sum();
        dayResult.setHeightQty(heightNetQty.intValue());
        //总需求(不含损耗)
        Integer sumNetQty = requireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getNetQty).sum();
        dayResult.setProdReqPlan(sumNetQty.intValue());
        //总需求(含损耗)
        Integer heightLossQty = requireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightLossQty).sum();
        Integer noHeightLossQty = requireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getFactProdReqQty).sum();
        Integer lossQty = (heightLossQty - heightNetQty) + (noHeightLossQty - sumNetQty);
        dayResult.setFactProdReqQty(sumNetQty + lossQty);
        //排产量置为零
        dayResult.setHeightProductionQty(BigDecimal.ZERO.intValue());
        dayResult.setMidProductionQty(BigDecimal.ZERO.intValue());
        dayResult.setCycleProductionQty(BigDecimal.ZERO.intValue());
        dayResult.setConventionProductionQty(BigDecimal.ZERO.intValue());
        dayResult.setPostponeProductionQty(BigDecimal.ZERO.intValue());
        //备注信息
        requireList.forEach(singlePlan -> addRemarkInfo(dayResult, singlePlan));
        return dayResult;
    }

    /**
     * 设置基础信息
     *
     * @param singleProductionInfo
     * @return
     */
    private static FactoryMonthPlanMouldDayDetail createInitDetailLog(CxMouldDayProductionHelper singleProductionInfo) {
        FactoryMonthPlanMouldDayDetail log = new FactoryMonthPlanMouldDayDetail();
        log.setMonthPlanId(singleProductionInfo.getMonthPlanId());
        log.setMouldCode(singleProductionInfo.getMouldCode());
        log.setMaterialCode(singleProductionInfo.getMaterialCode());
        log.setMaterialDesc(singleProductionInfo.getMaterialDesc());
        log.setMainMaterialDesc(singleProductionInfo.getMainPattern());
        log.setStructureName(singleProductionInfo.getStructureName());
        log.setPattern(singleProductionInfo.getPattern());
        log.setMainPattern(singleProductionInfo.getMainPattern());
        log.setProSize(singleProductionInfo.getProSize());
        log.setSpecifications(singleProductionInfo.getSpecifications());
        return log;
    }

    /**
     * 设置日排产信息
     *
     * @param productionDetail 排产明细对象
     * @param productionDate   排产日
     * @param productionQty    排产量
     */
    private static void setProductionDateQty(FactoryMonthPlanMouldDayDetail productionDetail, Integer productionDate, Integer productionQty) {
        String fieldName;
        if (productionDate > 0) {
            fieldName = "day";
        } else {
            fieldName = "preDay";
        }
        fieldName = fieldName + Math.abs(productionDate);
        Integer oldValue;
        Object value = productionDetail.getFieldValueByFieldName(fieldName);
        if (null == value) {
            oldValue = BigDecimal.ZERO.intValue();
        } else {
            oldValue = (Integer) value;
        }
        Integer newValue = oldValue + productionQty;
        productionDetail.setFieldValueByFieldName(fieldName, newValue);
        //起始日赋值
        Integer beginDay = productionDetail.getBeginDay();
        if (null == beginDay) {
            productionDetail.setBeginDay(productionDate);
        } else {
            productionDetail.setBeginDay(Math.min(beginDay, productionDate));
        }
        Integer endDay = productionDetail.getEndDay();
        if (null == endDay) {
            productionDetail.setEndDay(productionDate);
        } else {
            productionDetail.setEndDay(Math.max(endDay, productionDate));
        }
    }

    /**
     * 叠加备注信息
     *
     * @param dayResult       SKU天排产结果对象(合并)
     * @param requirementPlan SKU需求计划
     */
    private static void addRemarkInfo(FactoryMonthPlanMouldDayResult dayResult, MonthPlanProductionRequirePlanVo requirementPlan) {
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
     * 合并未排原因
     *
     * @param dayResult   汇总数据
     * @param requireList 未排原因集合
     */
    private static void mergeNoProductionReason(FactoryMonthPlanMouldDayResult dayResult, List<MonthPlanProductionRequirePlanVo> requireList) {
        if (CollectionUtils.isEmpty(requireList)) {
            return;
        }
        String reason = "";
        for (MonthPlanProductionRequirePlanVo requirement : requireList) {
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

    /**
     * 统计值
     *
     * @param dayResult  汇总数据对象
     * @param singleData 单条数据
     */
    private static void summaryDayQtyInfo(FactoryMonthPlanMouldDayResult dayResult, FactoryMonthPlanMouldDayDetail singleData) {
        //总硫化时长
        BigDecimal total = dayResult.getTotalVulcanizationMinutes();
        if (null == total) {
            total = BigDecimal.ZERO;
        }
        BigDecimal singleDataTotal = singleData.getTotalVulcanizationMinutes();
        if (null == singleDataTotal) {
            singleDataTotal = BigDecimal.ZERO;
        }
        total = total.add(singleDataTotal);
        dayResult.setTotalVulcanizationMinutes(total);
        //总排产量
        Integer totalQty = dayResult.getTotalQty();
        if (null == totalQty) {
            totalQty = BigDecimal.ZERO.intValue();
        }
        totalQty = totalQty + singleData.getTotalQty();
        dayResult.setTotalQty(totalQty);
        //起始日期
        Integer beginDay = dayResult.getBeginDay();
        if (null == beginDay) {
            dayResult.setBeginDay(singleData.getBeginDay());
        } else {
            dayResult.setBeginDay(Math.min(beginDay, singleData.getBeginDay()));
        }
        Integer endDate = dayResult.getEndDay();
        if (null == endDate) {
            dayResult.setEndDay(singleData.getEndDay());
        } else {
            dayResult.setEndDay(Math.max(endDate, singleData.getEndDay()));
        }
        DayProductionHandler.addDayQty(dayResult, singleData, FactoryConstant.PRODUCTION_CYCLE);
    }

    /**
     * 根据每日排产量，得到模具使用变化
     *
     * @param result
     */
    private static void setMouldUsedInfo(FactoryMonthPlanMouldDayResult result) {
        Map<Integer, Integer> dayProductionQty = DayProductionHandler.getDayQty(result, FactoryConstant.PRODUCTION_CYCLE);
        if (CollectionUtils.isEmpty(dayProductionQty)) {
            return;
        }
        Integer singleLhMachineCapacity = result.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        List<MouldDayUsedNumber> lhMachineCountList = new ArrayList<>();
        dayProductionQty.forEach((day, productionQty) ->{
            Integer usedLhMachineNumber = BigDecimal.valueOf(productionQty).divide(BigDecimal.valueOf(singleLhMachineCapacity), 0, RoundingMode.UP).intValue();
            MouldDayUsedNumber used = new MouldDayUsedNumber(day, usedLhMachineNumber);
            lhMachineCountList.add(used);
        });
        //日期从小到大，判断第一天如果小于第二天则舍弃第一天的模具数
        lhMachineCountList.sort(Comparator.comparing(MouldDayUsedNumber::getProductionDay));
        int size = lhMachineCountList.size();
        int startIndex = BigDecimal.ZERO.intValue();
        if (size > BigDecimal.ONE.intValue()) {
            Integer firstMouldNumber = lhMachineCountList.get(BigDecimal.ZERO.intValue()).getUsedLhMachineCount();
            Integer secondMouldNumber = lhMachineCountList.get(BigDecimal.ONE.intValue()).getUsedLhMachineCount();
            if (firstMouldNumber < secondMouldNumber) {
                startIndex = BigDecimal.ONE.intValue();
            }
        }
        List<MouldDayUsedNumber> resultList = lhMachineCountList.subList(startIndex, size - startIndex);
        //对排产量，保留第一个排产日，日期从小到大
        Map<Integer, Integer> lhMachineNumberDayMap = new HashMap<>();
        resultList.sort(Comparator.comparing(MouldDayUsedNumber::getProductionDay));
        resultList.forEach(mouldDayUsedNumber ->{
            Integer lhMachineCount = mouldDayUsedNumber.getUsedLhMachineCount();
            if (null == lhMachineCount) {
                return;
            }
            Integer day = mouldDayUsedNumber.getProductionDay();
            if (null == day) {
                return;
            }
            if (lhMachineNumberDayMap.containsKey(lhMachineCount)) {
                return;
            }
            lhMachineNumberDayMap.put(lhMachineCount, day);
        });
        if (CollectionUtils.isEmpty(lhMachineNumberDayMap)) {
            return;
        }
        //获取模具使用数变化集合，按排产日由小到大顺序遍历
        List<String> resultLhMachineCount = new ArrayList<>();
        lhMachineNumberDayMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> {
            Integer mouldNumber = entry.getKey() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            resultLhMachineCount.add(String.valueOf(mouldNumber));
        });
        result.setMouldChangeInfo(String.join(StringConstant.DASH, resultLhMachineCount));
    }
}

/**
 * 模具日使用数
 */
@Getter
class MouldDayUsedNumber {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 使用的硫化机台数
     */
    private Integer usedLhMachineCount;

    public MouldDayUsedNumber(Integer productionDay, Integer usedLhMachineCount) {
        this.productionDay = productionDay;
        this.usedLhMachineCount = usedLhMachineCount;
    }
}
