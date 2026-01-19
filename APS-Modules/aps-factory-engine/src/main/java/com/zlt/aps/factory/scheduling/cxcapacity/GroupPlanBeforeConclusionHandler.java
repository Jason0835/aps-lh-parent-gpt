package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模拟排产：结构提前收尾处理
 *
 * @author ZLT
 * @date 20260115
 */
@Slf4j
public class GroupPlanBeforeConclusionHandler {

    /**
     * 处理结构提前收尾
     * 实单排产量的硫化机台数低于最低硫化配比的硫化机台数，则进行提前收尾
     *
     * @param context
     * @param groupPlanInfo
     */
    public static void handlerBeforeConclusion(Context context, ProductionPlanGroupInfo groupPlanInfo) {
        String groupName = groupPlanInfo.getGroupName();
        log.info(addGroupStartBeforeConclusionLog(context, groupName));
        //获取当前模拟排产的数据
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlanInfo.getDayProductionLimitInfo();
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            //记录日志
            log.info(addNoAllocationInfoLog(context, groupName));
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MonthPlanStructureLhRatioVo> effectiveRatioList = new ArrayList<>();
        Map<MonthPlanStructureLhRatioVo, Set<String>> effectiveRationMap = new HashMap<>();
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        allCxMachineCodeSet.forEach(cxMachineCode -> {
            MonthPlanStructureLhRatioVo findLhRatio = groupPlanInfo.getLhRatio(cxMachineBaseInfo.get(cxMachineCode));
            if (null == findLhRatio) {
                return;
            }
            Set<String> cxMachineCodeSet = effectiveRationMap.get(findLhRatio);
            if (null == cxMachineCodeSet) {
                cxMachineCodeSet = new HashSet<>();
                effectiveRationMap.put(findLhRatio, cxMachineCodeSet);
            }
            cxMachineCodeSet.add(cxMachineCode);
            effectiveRatioList.add(findLhRatio);
        });
        if (CollectionUtils.isEmpty(effectiveRatioList)) {
            //记录日志
            log.info(addNoLhRatioInfoLog(context, groupName));
            return;
        }
        effectiveRatioList.sort(Comparator.comparing(MonthPlanStructureLhRatioVo::getLhMachineMaxQty));
        MonthPlanStructureLhRatioVo selectedLhRatio = effectiveRatioList.get(BigDecimal.ZERO.intValue());
        Integer minLhMachineCount = selectedLhRatio.getLhMachineMinQty();
        //转化成模具数
        Integer minMouldNumber = minLhMachineCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        //获取使用模具数低于minMouldNumber的天数数据
        List<GroupPlanCxLhCapacityLimitHelper> lowMinMouldNumberList = dayLimitList.stream().filter(singleDay -> singleDay.isLowMinMouldNumber(minMouldNumber)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinMouldNumberList)) {
            //记录日志
            log.info(addNoBeforeConclusionInfoLog(context, groupName, minLhMachineCount));
            return;
        }
        //按日期排序
        lowMinMouldNumberList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        //取得最小和最大日期
        Integer beforeConclusionDay = lowMinMouldNumberList.get(BigDecimal.ZERO.intValue()).getDay();
        List<String> selectedCxMachineList = new ArrayList<>(effectiveRationMap.get(selectedLhRatio));
        Collections.sort(selectedCxMachineList);
        String selectedCxMachineCode = selectedCxMachineList.get(BigDecimal.ZERO.intValue());
        CxMachineBaseInfoVo selectCxMachineInfo = cxMachineBaseInfo.get(selectedCxMachineCode);
        Integer deductionDay = lowMinMouldNumberList.size();
        Set<Integer> deductionDaySet = lowMinMouldNumberList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        List<CxMachineAllocationPlanHelper> allocationList = selectCxMachineInfo.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            //记录日志
            log.info(addCxMachineNoAllocationInfoLog(context, groupName, selectedCxMachineCode));
            return;
        }
        CxMachineAllocationPlanHelper lastInfo = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
        //更新数据
        updateInfoByBeforeConclusion(productionContext, minLhMachineCount, beforeConclusionDay, deductionDay, deductionDaySet, groupPlanInfo, selectCxMachineInfo, lastInfo, false);
        log.info(addBeforeConclusionResultLog(context, groupName, minLhMachineCount, beforeConclusionDay, deductionDay));
    }

    /**
     * 处理结构提前收尾
     * 实单排产量的硫化机台数低于最低硫化配比的硫化机台数，则进行提前收尾
     *
     * @param context
     * @param groupPlanInfo
     */
    public static void handlerBeforeConclusion(Context context, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo, MonthPlanStructureLhRatioVo cxLhRatio) {
        String groupName = groupPlanInfo.getGroupName();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        log.info(addGroupStartBeforeConclusionLog(context, groupName, cxMachineCode));
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = cxMachineInfo.getCxLhRatioMap();
        if (CollectionUtils.isEmpty(cxLhRatioMap)) {
            //记录日志
            log.info(addNoAllocationInfoLog(context, groupName));
            return;
        }
        List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            //记录日志
            log.info(addCxMachineNoAllocationInfoLog(context, groupName, cxMachineCode));
            return;
        }
        Integer lastIndex = allocationList.size() - BigDecimal.ONE.intValue();
        CxMachineAllocationPlanHelper lastInfo = allocationList.get(lastIndex);
        Integer startDay = lastInfo.getStartDay();
        Integer endDay = lastInfo.getEndDay();
        Integer minLhMachineCount = cxLhRatio.getLhMachineMinQty();
        List<CxLhProductionHelper> cxLhGroupList = new ArrayList<>(cxLhRatioMap.values());
        List<CxLhProductionHelper> hasProductionList = cxLhGroupList.stream().filter(singleGroup -> !CollectionUtils.isEmpty(singleGroup.getProductionMouldSet())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            Set<Integer> deductionDaySet = cxMachineInfo.getLastProductionDayInfo();
            Integer beforeConclusionDay = lastInfo.getStartDay();
            Integer deductionDay = lastInfo.getAllocationDay();
            updateInfoByBeforeConclusion(productionContext, minLhMachineCount, beforeConclusionDay, deductionDay, deductionDaySet, groupPlanInfo, cxMachineInfo, lastInfo, true);
            log.info(addBeforeConclusionResultLog(context, groupName, minLhMachineCount, beforeConclusionDay, deductionDay));
            return;
        }
        List<CxMachineUsedLhInfo> productionUsedLhInfoList = new ArrayList<>();
        for (int productionDay = startDay; productionDay <= endDay; productionDay++) {
            if (cxMachineInfo.getStopDayInfo().contains(productionDay)) {
                continue;
            }
            Integer matchDay = productionDay;
            List<CxLhProductionHelper> productionList = hasProductionList.stream().filter(singleGroup -> singleGroup.getProductionDay() >= matchDay).collect(Collectors.toList());
            productionUsedLhInfoList.add(CxMachineUsedLhInfo.build(matchDay, productionList.size()));
        }
        //获取使用硫化机台数低于minLhMachineCount的数据
        List<CxMachineUsedLhInfo> lowMinLhMachineCountList = productionUsedLhInfoList.stream().filter(single -> single.getUsedLhMachineCount() < minLhMachineCount).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinLhMachineCountList)) {
            //记录日志
            log.info(addNoBeforeConclusionInfoLog(context, groupName, minLhMachineCount));
            return;
        }
        //按日期排序
        lowMinLhMachineCountList.sort(Comparator.comparing(CxMachineUsedLhInfo::getProductionDay));
        //取得最小和最大日期
        Integer beforeConclusionDay = lowMinLhMachineCountList.get(BigDecimal.ZERO.intValue()).getProductionDay();
        Integer deductionDay = lowMinLhMachineCountList.size();
        Set<Integer> deductionDaySet = lowMinLhMachineCountList.stream().map(CxMachineUsedLhInfo::getProductionDay).collect(Collectors.toSet());
        //更新数据
        updateInfoByBeforeConclusion(productionContext, minLhMachineCount, beforeConclusionDay, deductionDay, deductionDaySet, groupPlanInfo, cxMachineInfo, lastInfo, true);
        log.info(addBeforeConclusionResultLog(context, groupName, minLhMachineCount, beforeConclusionDay, deductionDay));
    }

    /**
     * 根据提前收尾日及收尾天数，更新数据
     * 1、分组计划-标记分配完成
     * 2、成型机台更新剩余天数
     * 3、分配信息更新调整分配信息
     * 4、是否需要清除排产信息？
     *
     * @param productionContext   排产上下文
     * @param minLhMachineCount   最低硫化配比
     * @param beforeConclusionDay 提前收尾日
     * @param deductionDay        提前收尾的天数
     * @param deductionDaySet     需要提前收尾的天集合
     * @param groupPlanInfo       分组计划
     * @param cxMachineInfo       成型机台
     * @param allocationInfo      成型机台分配详情
     * @param isSingleMachine     是否单机台
     */
    private static void updateInfoByBeforeConclusion(TbrProductionContext productionContext, Integer minLhMachineCount, Integer beforeConclusionDay, Integer deductionDay, Set<Integer> deductionDaySet, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper allocationInfo, boolean isSingleMachine) {
        //重新计算(分组)分配的天数: 需要排产天数 - 还需排产天数 - 收尾天数
        Integer leftOverNeedAllocationDays = groupPlanInfo.getLeftOverNeedAllocationDays();
        Integer theoryDays = groupPlanInfo.getTheoryDays();
        Integer realAllocationDayBeforeConclusion = theoryDays - leftOverNeedAllocationDays - deductionDay;
        Integer minAllocationDays = productionContext.getBaseDataContainer().getParamConfiguration().getMinAllocationDays();
        if (realAllocationDayBeforeConclusion < minAllocationDays) {
            groupPlanInfo.setNoProductionLowMinLhMachineNoReachMinProductionDays(minLhMachineCount, minAllocationDays);
        }
        //标记结构分配完成
        groupPlanInfo.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
        //更新成型剩余天数
        Integer remainingDays = cxMachineInfo.getRemainingDays();
        cxMachineInfo.setRemainingDays(remainingDays + deductionDay);
        Map<String, ProductionMouldInfoVo> allMouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
        //更新分配信息
        allocationInfo.beforeConclusion(beforeConclusionDay, deductionDay);
        //如果分配量为零，则直接删除
        if (allocationInfo.getAllocationDay() <= BigDecimal.ZERO.intValue()) {
            cxMachineInfo.getAllocationList().remove(allocationInfo);
        }
        if (CollectionUtils.isEmpty(deductionDaySet)) {
            return;
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionInfoMap;
        //已排产的模具信息？
        if (isSingleMachine) {
            dayProductionInfoMap = cxMachineInfo.getDayProductionLimitInfo();
        } else {
            dayProductionInfoMap = groupPlanInfo.getDayProductionLimitInfo();
        }
        deductionDaySet.forEach(singleDeductionDay -> {
            GroupPlanCxLhCapacityLimitHelper productionDayLimit = dayProductionInfoMap.get(singleDeductionDay);
            if (null == productionDayLimit) {
                return;
            }
            Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo = productionDayLimit.getProductionSkuQtyInfo();
            if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
                return;
            }
            productionSkuQtyInfo.forEach((materialDesc, skuDayProductionInfo) -> {
                Set<String> usedMouldSet = skuDayProductionInfo.getUsedMouldSet();
                if (CollectionUtils.isEmpty(usedMouldSet)) {
                    return;
                }
                usedMouldSet.forEach(mouldCode -> {
                    ProductionMouldInfoVo mouldInfo = allMouldInfoMap.get(mouldCode);
                    if (null == mouldInfo) {
                        return;
                    }
                    List<CxMouldDayProductionHelper> dayProductionList = mouldInfo.getDayProductionInfo().get(singleDeductionDay);
                    if (CollectionUtils.isEmpty(dayProductionList)) {
                        return;
                    }
                    List<CxMouldDayProductionHelper> reserveList = new ArrayList<>();
                    dayProductionList.forEach(singleProduction -> {
                        if (!materialDesc.equals(singleProduction.getMaterialDesc())) {
                            reserveList.add(singleProduction);
                        }
                    });
                    mouldInfo.getDayProductionInfo().put(singleDeductionDay, reserveList);
                });
            });
        });
    }

    /**
     * 增加开始结构提前收尾日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 进入提前收尾判断业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    private static String addGroupStartBeforeConclusionLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 进入提前收尾判断业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加开始结构提前收尾日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 进入提前收尾判断业务 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 排产机台
     * @return
     */
    private static String addGroupStartBeforeConclusionLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 进入提前收尾判断业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加没有分配信息-退出结构提前收尾业务逻辑日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有分配信息，退出结构收尾业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    private static String addNoAllocationInfoLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有分配信息，退出结构收尾业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加没有成型硫化配比，退出结构提前收尾业务日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有结构成型硫化配比信息，退出结构收尾业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    private static String addNoLhRatioInfoLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有结构成型硫化配比信息，退出结构收尾业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加没有低于最低硫化数，无需结构提前收尾日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 高于最低成型硫化配比：%s，无需提前收尾 ====
     *
     * @param context    排程上下文
     * @param groupName  分组
     * @param minLhRatio 最低硫化配比
     * @return
     */
    private static String addNoBeforeConclusionInfoLog(Context context, String groupName, Integer minLhRatio) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 高于最低成型硫化配比：%s，无需提前收尾 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, minLhRatio);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加成型机台没有分配信息错误，导致结构提前收尾业务错误日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 成型机台：%s 没有分配信息导致错误 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 成型机台
     * @return
     */
    private static String addCxMachineNoAllocationInfoLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 成型机台：%s 没有分配信息导致错误 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加结构提前收尾结果日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 已低于最低成型硫化配比：%s，需在[%s]提前收尾，提前收尾天数[%s] ====
     *
     * @param context             排程上下文
     * @param groupName           分组
     * @param minLhRatio          最低硫化配比
     * @param beforeConclusionDay 提前收尾日
     * @param deductionDay        提前收尾天数
     * @return
     */
    private static String addBeforeConclusionResultLog(Context context, String groupName, Integer minLhRatio, Integer beforeConclusionDay, Integer deductionDay) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 已低于最低成型硫化配比：%s，需在[%s]提前收尾，提前收尾天数[%s] ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, minLhRatio, beforeConclusionDay, deductionDay);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }
}

/**
 * 使用硫化组信息
 *
 * @author ZLT
 * @date 20260115
 */
@Getter
class CxMachineUsedLhInfo {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 排产数
     */
    private Integer usedLhMachineCount;

    /**
     * 创建对象
     *
     * @param productionDay
     * @param usedLhMachineCount
     * @return
     */
    public static CxMachineUsedLhInfo build(Integer productionDay, Integer usedLhMachineCount) {
        return new CxMachineUsedLhInfo(productionDay, usedLhMachineCount);
    }

    /**
     * 构造函数
     *
     * @param productionDay
     * @param usedLhMachineCount
     */
    private CxMachineUsedLhInfo(Integer productionDay, Integer usedLhMachineCount) {
        this.productionDay = productionDay;
        this.usedLhMachineCount = usedLhMachineCount;
    }
}