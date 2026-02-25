package com.zlt.aps.mp.engine.handler;

import com.alibaba.fastjson.JSON;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitHelper;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日排产统计结果处理器
 *
 * @author ZLT
 * @date 20260210
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DayProductionStatisticsHandler {

    /**
     * 根据排产信息，构建日排产统计信息
     *
     * @param context
     * @return
     */
    public List<MpMonthPlanStatistics> buildDayProductionStatisticsResult(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlanList = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupPlanList)) {
            return Collections.emptyList();
        }
        //提取分组对应的日排产胎胚种类数，硫化机台数
        Map<String, MpMonthPlanStatisticsResultVo> statisticsResultInfo = buildByGroupPlan(allGroupPlanList);
        if (CollectionUtils.isEmpty(statisticsResultInfo)) {
            return Collections.emptyList();
        }
        //提取每日，分组对应的换模次数
        Map<Integer, Map<String, Integer>> dayChangeMouldInfo = getChangeMouldCountInfo(productionContext);
        statisticsResultInfo.forEach((groupName, singleStatisticsResultInfo) -> {
            singleStatisticsResultInfo.setFactoryCode(productionContext.getFactoryCode());
            singleStatisticsResultInfo.setYear(productionContext.getYear());
            singleStatisticsResultInfo.setMonth(productionContext.getMonth());
            singleStatisticsResultInfo.setYearMonth(productionContext.getFullYearAndMonth());
            singleStatisticsResultInfo.setMonthPlanVersion(productionContext.getMonthPlanVersion());
            singleStatisticsResultInfo.setProductionVersion(productionContext.getProductionVersion());
            singleStatisticsResultInfo.setLastMonthPlanVersion(productionContext.getMonthPlanVersion());
            Map<Integer, MpDayProductionStatisticsDetailVo> dayStatisticsDetailMap = singleStatisticsResultInfo.getDayStatisticsDetailMap();
            if (CollectionUtils.isEmpty(dayStatisticsDetailMap)) {
                return;
            }
            dayStatisticsDetailMap.forEach((day, dayStatisticsDetail) -> {
                Map<String, Integer> groupChangeInfo = dayChangeMouldInfo.get(day);
                if (CollectionUtils.isEmpty(groupChangeInfo)) {
                    return;
                }
                dayStatisticsDetail.setChangeMould(groupChangeInfo.get(groupName));
            });
        });
        //将日排产信息，转化成json串
        List<MpMonthPlanStatistics> resultList = new ArrayList<>();
        String dayFieldNameFormat = "day%s";
        statisticsResultInfo.forEach((groupName, singleStatisticsResultInfo) -> {
            Map<Integer, MpDayProductionStatisticsDetailVo> dayStatisticsDetailMap = singleStatisticsResultInfo.getDayStatisticsDetailMap();
            if (CollectionUtils.isEmpty(dayStatisticsDetailMap)) {
                return;
            }
            MpMonthPlanStatistics info = new MpMonthPlanStatistics();
            BeanUtils.copyProperties(singleStatisticsResultInfo, info);
            dayStatisticsDetailMap.forEach((day, detail) -> {
                String dayFieldName = String.format(dayFieldNameFormat, day);
                String dayInfo = JSON.toJSONString(detail);
                info.setFieldValueByFieldName(dayFieldName, dayInfo);
            });
            resultList.add(info);
        });
        return resultList;
    }

    /**
     * 构建基础的日排产统计信息
     * 日排产的胎胚种类数
     * 硫化机台数
     *
     * @param allGroupPlanList
     * @return
     */
    private Map<String, MpMonthPlanStatisticsResultVo> buildByGroupPlan(Map<String, ProductionPlanGroupInfo> allGroupPlanList) {
        Map<String, MpMonthPlanStatisticsResultVo> statisticsResultInfo = new HashMap<>();
        allGroupPlanList.forEach((structureName, groupPlanInfo) -> {
            Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlanInfo.getDayProductionLimitInfo();
            if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
                return;
            }
            MpMonthPlanStatisticsResultVo groupPlanStatisticsInfo = buildGroupPlanBaseProductionStatisticsInfo(structureName, dayProductionLimitInfo);
            if (null == groupPlanStatisticsInfo) {
                return;
            }
            MonthPlanProductionRequirePlanVo singlePlan = groupPlanInfo.getGroupPlanData().get(BigDecimal.ZERO.intValue());
            if (null != singlePlan) {
                groupPlanStatisticsInfo.setProSize(singlePlan.getProSize());
                groupPlanStatisticsInfo.setStructureType(singlePlan.getStructureType());
                groupPlanStatisticsInfo.setProductTypeCode(singlePlan.getProductTypeCode());
            }
            statisticsResultInfo.put(structureName, groupPlanStatisticsInfo);
        });
        return statisticsResultInfo;
    }

    /**
     * 构建日换模次数信息，按结构分
     *
     * @param productionContext 排产上下文
     * @return
     */
    private Map<Integer, Map<String, Integer>> getChangeMouldCountInfo(TbrProductionContext productionContext) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        DayCapacityLimitVo dayCapacityLimit = baseDataContainer.getDayCapacityLimit();
        Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap = dayCapacityLimit.getDayCapacityLimitMap();
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return Collections.emptyMap();
        }
        Map<String, String> skuGroupInfo = getSkuGroupInfo(productionContext);
        Map<Integer, Map<String, Integer>> dayChangeMouldInfo = new HashMap<>();
        dayCapacityLimitMap.forEach((day, dayCapacityLimitInfo) -> {
            Set<String> changeMouldInfo = dayCapacityLimitInfo.getChangeMouldInfo();
            if (CollectionUtils.isEmpty(changeMouldInfo)) {
                return;
            }
            Map<String, Integer> groupPlanChangeInfo = new HashMap<>();
            changeMouldInfo.forEach(changeInfo -> {
                String[] singleChangeMouldInfo = changeInfo.split(ProductionConstant.JOINT_SPLIT);
                String materialDesc = singleChangeMouldInfo[BigDecimal.ZERO.intValue()];
                if (StringUtils.isBlank(materialDesc)) {
                    return;
                }
                String groupName = skuGroupInfo.get(materialDesc);
                if (StringUtils.isBlank(groupName)) {
                    return;
                }
                Integer changeMouldLhMachineCount = groupPlanChangeInfo.get(groupName);
                if (null == changeMouldLhMachineCount) {
                    changeMouldLhMachineCount = BigDecimal.ONE.intValue();
                } else {
                    changeMouldLhMachineCount = changeMouldLhMachineCount + BigDecimal.ONE.intValue();
                }
                groupPlanChangeInfo.put(groupName, changeMouldLhMachineCount);
            });
            if (CollectionUtils.isEmpty(groupPlanChangeInfo)) {
                return;
            }
            dayChangeMouldInfo.put(day, groupPlanChangeInfo);
        });
        return dayChangeMouldInfo;
    }

    /**
     * 构建日排产统计对象-基础信息
     * 胎胚种类数
     * 硫化机台数
     *
     * @param groupName
     * @param dayProductionLimitInfo
     * @return
     */
    private MpMonthPlanStatisticsResultVo buildGroupPlanBaseProductionStatisticsInfo(String groupName, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo) {
        MpMonthPlanStatisticsResultVo groupStatistics = new MpMonthPlanStatisticsResultVo();
        groupStatistics.setStructureName(groupName);
        Map<Integer, MpDayProductionStatisticsDetailVo> dayStatisticsDetailMap = new HashMap<>();
        dayProductionLimitInfo.forEach((day, dayLimitInfo) -> {
            dayStatisticsDetailMap.put(day, getBaseDayStatisticsDetail(dayLimitInfo));
        });
        groupStatistics.setDayStatisticsDetailMap(dayStatisticsDetailMap);
        return groupStatistics;
    }

    /**
     * 获取结构名
     *
     * @param productionContext
     * @return
     */
    private Map<String, String> getSkuGroupInfo(TbrProductionContext productionContext) {
        Map<String, List<MonthPlanProductionRequirePlanVo>> allSkuProductionPlan = productionContext.getAllSkuProductionPlan();
        if (CollectionUtils.isEmpty(allSkuProductionPlan)) {
            return Collections.emptyMap();
        }
        Map<String, String> skuGroupInfo = new HashMap<>();
        allSkuProductionPlan.forEach((materialDesc, planList) -> {
            if (CollectionUtils.isEmpty(planList)) {
                return;
            }
            List<MonthPlanProductionRequirePlanVo> effectiveList = planList.stream().filter(singlePlan -> StringUtils.isNotBlank(singlePlan.getStructureName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(effectiveList)) {
                return;
            }
            skuGroupInfo.put(materialDesc, effectiveList.get(BigDecimal.ZERO.intValue()).getStructureName());
        });
        return skuGroupInfo;
    }

    /**
     * 根据日排产信息，构建日排产统计数据
     *
     * @param dayLimitInfo 日排产信息
     * @return
     */
    private MpDayProductionStatisticsDetailVo getBaseDayStatisticsDetail(GroupPlanCxLhCapacityLimitHelper dayLimitInfo) {
        MpDayProductionStatisticsDetailVo detail = new MpDayProductionStatisticsDetailVo();
        Integer embryoCodeSize = BigDecimal.ZERO.intValue();
        Set<String> productionEmbryoCodeSet = dayLimitInfo.getProductionEmbryoCodeSet();
        if (!CollectionUtils.isEmpty(productionEmbryoCodeSet)) {
            embryoCodeSize = productionEmbryoCodeSet.size();
        }
        Set<String> productionMouldSet = dayLimitInfo.getProductionMouldSet();
        Integer lhMachineCount = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(productionMouldSet)) {
            lhMachineCount = productionMouldSet.size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        }
        if (lhMachineCount > dayLimitInfo.getMaxLhMachineCount()) {
            lhMachineCount = dayLimitInfo.getMaxLhMachineCount();
        }
        detail.setEmbryoCount(embryoCodeSize);
        detail.setLhMachines(lhMachineCount);
        return detail;
    }

}

/**
 * 日排产结果Vo对象
 *
 * @author ZLT
 * @date 20260210
 */
@Data
class MpMonthPlanStatisticsResultVo extends MpMonthPlanStatistics {

    private Map<Integer, MpDayProductionStatisticsDetailVo> dayStatisticsDetailMap;

}
