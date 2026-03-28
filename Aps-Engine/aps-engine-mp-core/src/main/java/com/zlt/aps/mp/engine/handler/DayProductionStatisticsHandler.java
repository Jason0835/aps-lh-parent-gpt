package com.zlt.aps.mp.engine.handler;

import com.alibaba.fastjson.JSON;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitHelper;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.logrecorder.DayLimitLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
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
     * 获取统计信息
     *
     * @param context
     * @return
     */
    public List<MpMonthPlanStatistics> buildDayProductionStatisticsResultByAdjustType(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlanMap = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupPlanMap)) {
            return Collections.emptyList();
        }
        List<MpMonthPlanStatistics> productionStatisticsList = new ArrayList<>();
        //遍历所有结构
        String dayFieldNameFormat = "day%s";
        //加载计算产能需要的参数
        Map<String, Object> paramMap = new HashMap<>();
        ProductionCapacityParamConfiguration configuration = productionContext.getBaseDataContainer().getParamConfiguration();
        paramMap.put(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode(), configuration.getChangeTypeBlockQtyDiff());
        paramMap.put(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode(), configuration.getChangeMouldFirstQty());
        paramMap.put(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode(), configuration.getChangeTypeBlockQty());
        paramMap.put(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode(), configuration.getChangeTypeBlockMaxQty());
        allGroupPlanMap.forEach((structureName, groupPlanInfo) -> {
            // 重算
            groupPlanInfo.reCalcMpDailyCapacityLimit(productionContext);
            Map<Integer, MpDailyCapacityLimitVo> dayCapacityLimitMap = groupPlanInfo.getDailyCapacityLimitVoMap();
            if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
                return;
            }
            MpMonthPlanStatisticsResultVo statistics = buildGroupStatisticsBaseInfo(productionContext, groupPlanInfo);
            dayCapacityLimitMap.forEach((day, dayCapacityLimit) -> {
                // 3.2.2.2、构建当天的产能统计
                MpDayProductionStatisticsDetailVo detail = new MpDayProductionStatisticsDetailVo();
                detail.setEmbryoCount(dayCapacityLimit.getEmbryoCodes().size());
                detail.setLhMachines(dayCapacityLimit.getUsedLhMachines());
                detail.setChangeMould(dayCapacityLimit.getUsedChangeMould());
                String dayFieldName = String.format(dayFieldNameFormat, day);
                String dayInfo = JSON.toJSONString(detail);
                statistics.setFieldValueByFieldName(dayFieldName, dayInfo);
            });
            productionStatisticsList.add(statistics);
        });
        getDayChangeMouldInfoAndPrint(productionContext);
        buildDayCapacityStatisticsInfo(productionContext);
        return productionStatisticsList;
    }

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
        Map<Integer, Map<String, Integer>> dayChangeMouldInfo = getDayChangeMouldInfoAndPrint(productionContext);
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
        buildDayCapacityStatisticsInfo(productionContext);
        return resultList;
    }

    /**
     * 打印每日换模次数信息
     *
     * @param productionContext
     */
    private Map<Integer, Map<String, Integer>> getDayChangeMouldInfoAndPrint(TbrProductionContext productionContext) {
        //提取每日，分组对应的换模次数
        Map<Integer, Map<String, Integer>> dayChangeMouldInfo = getChangeMouldCountInfo(productionContext);
        Map<Integer, Integer> dayAllChangeMould = new HashMap<>();
        dayChangeMouldInfo.forEach((day, changeMouldInfo) -> {
            Integer sumCount = BigDecimal.ZERO.intValue();
            if (!CollectionUtils.isEmpty(changeMouldInfo)) {
                sumCount = changeMouldInfo.values().stream().mapToInt(Integer::intValue).sum();
            }
            dayAllChangeMould.put(day, sumCount);
        });
        String dayChangeMouldContent = JSON.toJSONString(dayAllChangeMould);
        DayLimitLogRecorder.addDayStatisticsInfo(productionContext, dayChangeMouldContent);
        return dayChangeMouldInfo;
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
     * 日产限制量信息
     *
     * @param context
     * @return
     */
    private void buildDayCapacityStatisticsInfo(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        if (null == baseDataContainer) {
            return;
        }
        DayCapacityLimitVo dayCapacityLimit = baseDataContainer.getDayCapacityLimit();
        if (null == dayCapacityLimit) {
            return;
        }
        Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap = dayCapacityLimit.getDayCapacityLimitMap();
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        Map<Integer, Integer> dayCapacityValue = new HashMap<>();
        Map<Integer, Integer> dayChangeGroupValue = new HashMap<>();
        dayCapacityLimitMap.forEach((day, dayCapacityLimitInfo) -> {
            if (null == dayCapacityLimitInfo) {
                return;
            }
            Integer sumDayCapacity = Optional.ofNullable(dayCapacityLimitInfo.getSumProductionCapacityQty()).orElse(BigDecimal.ZERO.intValue());
            dayCapacityValue.put(day, sumDayCapacity);
            Integer sumChangeGroup = Optional.ofNullable(dayCapacityLimitInfo.getUsedChangeCxMachineCount()).orElse(BigDecimal.ZERO.intValue());
            dayChangeGroupValue.put(day, sumChangeGroup);
        });
        if (!CollectionUtils.isEmpty(dayCapacityValue)) {
            String dayCapacityContentInfo = JSON.toJSONString(dayCapacityValue);
            DayLimitLogRecorder.addDayCapacityStatisticsInfo(productionContext, dayCapacityContentInfo);
        }
        if (!CollectionUtils.isEmpty(dayChangeGroupValue)) {
            String dayChangeContentInfo = JSON.toJSONString(dayChangeGroupValue);
            DayLimitLogRecorder.addChangeGroupStatisticsInfo(productionContext, dayChangeContentInfo);
        }
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
     * 构建统计基础信息对象
     *
     * @param productionContext 排产上下文
     * @param groupPlanInfo     结构分组
     * @return
     */
    private MpMonthPlanStatisticsResultVo buildGroupStatisticsBaseInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo groupPlanInfo) {
        String structureName = groupPlanInfo.getGroupName();
        MpMonthPlanStatisticsResultVo singleGroupBaseInfo = new MpMonthPlanStatisticsResultVo();
        singleGroupBaseInfo.setStructureName(structureName);
        singleGroupBaseInfo.setFactoryCode(productionContext.getFactoryCode());
        singleGroupBaseInfo.setYear(productionContext.getYear());
        singleGroupBaseInfo.setMonth(productionContext.getMonth());
        singleGroupBaseInfo.setYearMonth(productionContext.getFullYearAndMonth());
        singleGroupBaseInfo.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        singleGroupBaseInfo.setProductionVersion(productionContext.getProductionVersion());
        singleGroupBaseInfo.setLastMonthPlanVersion(productionContext.getMonthPlanVersion());
        MonthPlanProductionRequirePlanVo singlePlan = groupPlanInfo.getGroupPlanData().get(BigDecimal.ZERO.intValue());
        if (null != singlePlan) {
            singleGroupBaseInfo.setProSize(singlePlan.getProSize());
            singleGroupBaseInfo.setStructureType(singlePlan.getStructureType());
            singleGroupBaseInfo.setProductTypeCode(singlePlan.getProductTypeCode());
        }
        return singleGroupBaseInfo;
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
