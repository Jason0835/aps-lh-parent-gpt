package com.zlt.aps.factory.handler;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
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
            //使用模具数量信息
            dayResult.setDifferenceQty(dayResult.getFactProdReqQty() - dayResult.getTotalQty());
            resultList.add(dayResult);
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
        //硫化施工信息
        logDetail.setConstructionStage(planInfo.getConstructionStage());
        logDetail.setDayVulcanizationQty(planInfo.getDayVulcanizationQty().intValue());
        logDetail.setCuringTime(planInfo.getCuringTime().intValue());
        //需求量信息
        logDetail.setHeightQty(planInfo.getHeightQty().intValue());
        logDetail.setProdReqPlan(planInfo.getNetQty().intValue());
        logDetail.setFactProdReqQty(planInfo.getHeightLossQty().intValue() + planInfo.getFactProdReqQty().intValue());
        logDetail.setAverageQty(planInfo.getAverageSaleQty().intValue());
        logDetail.setInventorySalesRatio(BigDecimal.valueOf(planInfo.getInventorySalesRatio()));
        //统计总排产量
        Integer totalValue = DayProductionHandler.summaryDayQty(logDetail, FactoryConstant.PRODUCTION_CYCLE);
        logDetail.setTotalQty(totalValue.intValue());
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
        Integer heightQty = requireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightLossQty).sum();
        Integer noHeightQty = requireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getFactProdReqQty).sum();
        dayResult.setFactProdReqQty(heightQty.intValue() + noHeightQty.intValue());
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
        log.setMainMaterialDesc(singleProductionInfo.getEmbryoCode());
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
        total = total.add(singleData.getTotalVulcanizationMinutes());
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
}
