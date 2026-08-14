package com.zlt.aps.mp.engine.handler.appoint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.*;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.GroupAppointProductionInfoVo;
import com.zlt.aps.mp.engine.handler.GroupPlanDeductionDayHandler;
import com.zlt.aps.mp.engine.handler.GroupPriorityProductionScheduler;
import com.zlt.aps.mp.engine.logrecorder.KeyInformationLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.utils.ProductionComparatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分组信息对象指定生产业务处理器
 * 支持业务场景：
 * 1、某个分组(结构)指定其最大生产天数(只在一台成型机上)
 * 2、某个分组(结构)指定其在某个成型机上固定生产周期(即上机时间，最大生产天数)
 *
 * @author ZLT
 * @date 20260713
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupAppointBusinessHandler {

    private final GroupPlanDeductionDayHandler deductionHandler;

    private final GroupPriorityProductionScheduler groupPriorityProductionScheduler;

    private final DayReduceSkuLhMachinePrioritySelector selector;

    /**
     * 根据在机结构分配信息及后结构指定机台上机信息，得到需要调整的的续作Sku信息
     * 在机结构机台分配结束日与后结构指定机台上机日有冲突时，需要重新确认续作Sku的下机日及下机硫化机台数
     * 1、在机结构为单台成型机时，则直接强行下机
     * 2、在机结构为多台成型机时，需要确定强行下机的Sku和需要进行降膜处理的Sku
     *
     * @param context                排产上下文
     * @param continueAllocationList 在机分组-在产机台分配
     * @param allContinueMap         所有续作Sku信息
     */
    public boolean hasContinueGroupAdjust(Context context, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        if (CollectionUtils.isEmpty(continueAllocationList)) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //1、获取指定机台配置信息
        List<GroupAppointCxMachineOnlineInfo> appointCxMachineEarliestInfo = getAppointCxMachineEarliestOnlineDay(productionContext);
        if (CollectionUtils.isEmpty(appointCxMachineEarliestInfo)) {
            return false;
        }
        //2、获取在机可能需要调整的分组信息-即已排产在机分组机台有指定配置
        List<AppointGroupReduceMachineInfo> needAdjustGroupList = getOnLineGroupForceOffInfo(appointCxMachineEarliestInfo, continueAllocationList);
        if (CollectionUtils.isEmpty(needAdjustGroupList)) {
            return false;
        }
        Set<ProductionPlanGroupInfo> groupForceAdjustSet = Sets.newHashSet();
        needAdjustGroupList.forEach(appointOfflineInfo -> {
            String groupName = appointOfflineInfo.getGroupName();
            ProductionPlanGroupInfo groupInfo = productionContext.getGroupProductionInfo().get(groupName);
            if (null == groupInfo) {
                return;
            }
            CxContinueInfoHelper continueSkuInfo = allContinueMap.get(groupName);
            boolean hasAdjust = calculateContinueSkuForceConclusion(productionContext, groupInfo, appointOfflineInfo, continueAllocationList, continueSkuInfo);
            if (hasAdjust) {
                groupForceAdjustSet.add(groupInfo);
            }
        });
        if (CollectionUtils.isEmpty(groupForceAdjustSet)) {
            return false;
        }
        return true;
    }

    /**
     * 指定业务：优先排产
     * 场景：有指定机台的后结构优先排产
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有分组信息对象集合
     * @param continueAllocationList 所有续作分配结果
     * @param allContinueMap         所有续作Sku
     */
    public void appointPriority(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        if (CollectionUtils.isEmpty(allGroupPlanMap)) {
            return;
        }
        //获取指定机台结构配置信息
        List<GroupAppointProductionInfoVo> appointCxMachineList = getAllAppointCxMachineConfiguration(context);
        if (CollectionUtils.isEmpty(appointCxMachineList)) {
            return;
        }
        Map<String, Set<CxMachineAllocationPlanHelper>> resultStorage = Maps.newHashMap();
        //按上机日排序
        appointCxMachineList.sort(Comparator.comparing(GroupAppointProductionInfoVo::getMonthStartDay));
        appointCxMachineList.forEach(singleCxMachineConfiguration -> {
            productionGroupAppointCxMachine(context, singleCxMachineConfiguration, allGroupPlanMap, resultStorage);
        });
        KeyInformationLogRecorder.recorderInsertAllocationGroupInfoLog(context, resultStorage);
    }

    /**
     * 判断续作机台是否需要提前下机
     * 1、如果续作成型机台没有配置特定指定排产业务，则无需提前下机
     * 2、如果有配置特定指定排产业务，则可能需要提前下机
     * 获取特定指定排产业务中最早上机的配置(monthStartDay最小的)
     * 2.1、如果续作结构理论的下机日期theoryOffDay >= 最早指定切换结构日，
     * 则需要提前下机
     * 2.2、否则，无需提前下机
     *
     * @param context       排产上下文
     * @param cxMachineInfo 成型机台信息
     * @param theoryOffDay  理论下机日
     * @return
     */
    public boolean hasAdvanceOffByContinueCxMachine(Context context, CxMachineBaseInfoVo cxMachineInfo, Integer theoryOffDay) {
        if (null == theoryOffDay || theoryOffDay < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupAppointProductionInfoVo> configurationList = getAppointConfigurationByCxMachine(productionContext, cxMachineInfo);
        if (CollectionUtils.isEmpty(configurationList)) {
            return false;
        }
        Comparator sort = Comparator.comparing(GroupAppointProductionInfoVo::getMonthStartDay);
        configurationList.sort(sort);
        GroupAppointProductionInfoVo earliest = configurationList.get(BigDecimal.ZERO.intValue());
        return theoryOffDay >= earliest.getMonthStartDay();
    }

    /**
     * 判断成型机台是否有特殊指定排产业务
     *
     * @param context       排产上下文
     * @param cxMachineInfo 成型机台信息
     * @return
     */
    public Integer getContinueCxMachineEndDayByAppoint(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupAppointProductionInfoVo> configurationList = getAppointConfigurationByCxMachine(productionContext, cxMachineInfo);
        if (CollectionUtils.isEmpty(configurationList)) {
            return null;
        }
        //按起始日从小到大排序
        Comparator sort = Comparator.comparing(GroupAppointProductionInfoVo::getMonthStartDay);
        configurationList.sort(sort);
        GroupAppointProductionInfoVo earliest = configurationList.get(BigDecimal.ZERO.intValue());
        Integer earliestOffDay = earliest.getMonthStartDay();
        Integer monthMaxDay = productionContext.getMonthDays();
        return getPreviousDay(cxMachineInfo, earliestOffDay, monthMaxDay);
    }

    /**
     * 获取针对cxMachineInfo的指定排产配置信息
     *
     * @param context       排产上下文
     * @param cxMachineInfo 成型机台信息对象
     * @return
     */
    private List<GroupAppointProductionInfoVo> getAppointConfigurationByCxMachine(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        if (null == cxMachineInfo || StringUtils.isBlank(cxMachineInfo.getCxMachineCode())) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupAppointProductionInfoVo> allConfiguration = productionContext.getBaseDataContainer().getAppointConfiguration();
        if (CollectionUtils.isEmpty(allConfiguration)) {
            return Collections.emptyList();
        }
        List<GroupAppointProductionInfoVo> appointCxMachineList = allConfiguration.stream().filter(single -> single.isAppointCxMachine(productionContext)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(appointCxMachineList)) {
            return Collections.emptyList();
        }
        List<GroupAppointProductionInfoVo> hasConfigurationList = appointCxMachineList.stream().filter(configuration -> cxMachineInfo.getCxMachineCode().equals(configuration.getCxMachineCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasConfigurationList)) {
            return Collections.emptyList();
        }
        return hasConfigurationList;
    }

    /**
     * 获取指定机台后结构最早上机日信息
     * 以指定机台为维度，并得到最早切换结构日
     * 以及该机台所有指定配置信息
     *
     * @param context 排产上下文
     * @return
     */
    private List<GroupAppointCxMachineOnlineInfo> getAppointCxMachineEarliestOnlineDay(Context context) {
        List<GroupAppointProductionInfoVo> appointCxMachineList = getAllAppointCxMachineConfiguration(context);
        if (CollectionUtils.isEmpty(appointCxMachineList)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, List<GroupAppointProductionInfoVo>> cxMachineGroupMap = appointCxMachineList.stream().collect(Collectors.groupingBy(GroupAppointProductionInfoVo::getCxMachineCode));
        Map<CxMachineBaseInfoVo, GroupAppointCxMachineOnlineInfo> appointCxMachineEarliestMap = Maps.newHashMap();
        cxMachineGroupMap.forEach((cxMachineCode, singleConfiguration) -> {
            if (CollectionUtils.isEmpty(singleConfiguration)) {
                return;
            }
            CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
            if (null == cxMachineInfo) {
                return;
            }
            singleConfiguration.sort(Comparator.comparing(GroupAppointProductionInfoVo::getMonthStartDay));
            Integer earliestChangeGroupDay = singleConfiguration.get(BigDecimal.ZERO.intValue()).getMonthStartDay();
            appointCxMachineEarliestMap.put(cxMachineInfo, new GroupAppointCxMachineOnlineInfo(cxMachineInfo, earliestChangeGroupDay, singleConfiguration));
        });
        if (CollectionUtils.isEmpty(appointCxMachineEarliestMap)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(appointCxMachineEarliestMap.values());
    }

    /**
     * 获取有指定结构，指定机台的指定业务配置信息
     *
     * @param context 排产上下文
     * @return
     */
    private List<GroupAppointProductionInfoVo> getAllAppointCxMachineConfiguration(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupAppointProductionInfoVo> configurationList = productionContext.getBaseDataContainer().getAppointConfiguration();
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        List<GroupAppointProductionInfoVo> appointCxMachineList = configurationList.stream().filter(singleConfiguration -> singleConfiguration.isAppointCxMachine(productionContext)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(appointCxMachineList)) {
            return Collections.emptyList();
        }
        return appointCxMachineList;
    }

    /**
     * 结构排产指定机台
     *
     * @param context              排产上下文
     * @param appointConfiguration 指定业务配置
     * @param allGroupPlanMap      所有分组信息
     * @param resultStorage        存储排产结果
     */
    private void productionGroupAppointCxMachine(Context context, GroupAppointProductionInfoVo appointConfiguration, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, Map<String, Set<CxMachineAllocationPlanHelper>> resultStorage) {
        if (null == appointConfiguration || StringUtils.isBlank(appointConfiguration.getGroupName()) || StringUtils.isBlank(appointConfiguration.getCxMachineCode())) {
            return;
        }
        if (CollectionUtils.isEmpty(allGroupPlanMap)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = appointConfiguration.getGroupName();
        String cxMachineCode = appointConfiguration.getCxMachineCode();
        ProductionPlanGroupInfo groupInfo = allGroupPlanMap.get(groupName);
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
        if (null == groupInfo || null == cxMachineInfo) {
            return;
        }
        //指定排产
        groupPriorityProductionScheduler.productionGroupAppointCxMachine(productionContext, groupInfo, cxMachineInfo, appointConfiguration.getMaxAllocationDay(), resultStorage);
    }

    /**
     * 获取在机分组(TBR-结构)，需要强行下机的信息
     *
     * @param appointCxMachineEarliestInfo 指定机台配置信息
     * @param continueAllocationList       在机分组分配信息(模拟排产后)
     * @return
     */
    private List<AppointGroupReduceMachineInfo> getOnLineGroupForceOffInfo(List<GroupAppointCxMachineOnlineInfo> appointCxMachineEarliestInfo, List<CxMachineAllocationPlanHelper> continueAllocationList) {
        if (CollectionUtils.isEmpty(appointCxMachineEarliestInfo) || CollectionUtils.isEmpty(continueAllocationList)) {
            return Collections.emptyList();
        }
        Map<String, AppointGroupReduceMachineInfo> onlineGroupReduceMachineMap = Maps.newHashMap();
        appointCxMachineEarliestInfo.forEach(appointInfo -> {
            CxMachineBaseInfoVo appointCxMachineInfo = appointInfo.getCxMachineInfo();
            String cxMachineCode = appointCxMachineInfo.getCxMachineCode();
            List<CxMachineAllocationPlanHelper> onlineMachineAllocationList = continueAllocationList.stream().filter(singleAllocation -> cxMachineCode.equals(singleAllocation.getCxMachineCode())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(onlineMachineAllocationList) || onlineMachineAllocationList.size() > BigDecimal.ONE.intValue()) {
                return;
            }
            CxMachineAllocationPlanHelper allocationInfo = onlineMachineAllocationList.get(BigDecimal.ZERO.intValue());
            String groupName = allocationInfo.getAllocationGroup();
            List<CxMachineAllocationPlanHelper> onlineCxMachineList = continueAllocationList.stream().filter(singleAllocation -> groupName.equals(singleAllocation.getAllocationGroup())).collect(Collectors.toList());
            AppointGroupReduceMachineInfo reduceGroup = onlineGroupReduceMachineMap.get(groupName);
            if (null == reduceGroup) {
                reduceGroup = new AppointGroupReduceMachineInfo(groupName, Lists.newArrayList(), onlineCxMachineList);
                onlineGroupReduceMachineMap.put(groupName, reduceGroup);
            }
            OnlineGroupAppointCxMachineInfo currentAppointCxMachineInfo = new OnlineGroupAppointCxMachineInfo(appointCxMachineInfo, appointInfo.getEarliestChangeGroupDay(), allocationInfo);
            reduceGroup.getForceOffMachineInfo().add(currentAppointCxMachineInfo);
        });
        if (CollectionUtils.isEmpty(onlineGroupReduceMachineMap)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(onlineGroupReduceMachineMap.values());
    }

    /**
     * 在机分组(TBR-结构)因指定了后结构上机日，导致对应机台需要强制下机
     * 强制下机则需要对续作Sku的收尾日或是降膜调整
     * 1、如果在机分组只有1台在产机台，则强制下机，调整其分配收尾日即可
     * 2、如果在机分组有多台在产机台，则需要区分续作Sku进行调整的业务处理
     * 2.1、如果因强制下机成型机台，导致胎胚种类数超出限制，则有续作Sku需要调整其收尾日
     * 2.2、调整胎胚种类数满足时，如果硫化机台数超出限制，则有续作Sku需要调整其降膜信息
     *
     * @param context                排产上下文
     * @param groupInfo              分组信息对象(TBR-结构)
     * @param appointOfflineInfo     指定信息
     * @param continueAllocationList 初始模拟的在机分组成型机台分配信息
     * @param continueSkuInfo        初始续作Sku信息
     */
    private boolean calculateContinueSkuForceConclusion(Context context, ProductionPlanGroupInfo groupInfo, AppointGroupReduceMachineInfo appointOfflineInfo, List<CxMachineAllocationPlanHelper> continueAllocationList, CxContinueInfoHelper continueSkuInfo) {
        if (null == appointOfflineInfo || CollectionUtils.isEmpty(continueAllocationList)) {
            return false;
        }
        //获取实际需要强制下机的指定机台(可以有一定的时间差)
        List<OnlineGroupAppointCxMachineInfo> forceConclusionList = appointOfflineInfo.getForceOfflineCxMachine(context);
        if (CollectionUtils.isEmpty(forceConclusionList)) {
            return false;
        }
        List<CxMachineAllocationPlanHelper> originContinueCxMachineInfo = appointOfflineInfo.getOnlineCxMachineInfo();
        if (CollectionUtils.isEmpty(originContinueCxMachineInfo)) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //只有一台在机成型机台
        if (originContinueCxMachineInfo.size() == BigDecimal.ONE.intValue()) {
            OnlineGroupAppointCxMachineInfo onlyCxMachine = forceConclusionList.get(BigDecimal.ZERO.intValue());
            CxMachineBaseInfoVo onlyCxMachineInfo = onlyCxMachine.getCxMachineInfo();
            CxMachineAllocationPlanHelper onlyAllocationInfo = originContinueCxMachineInfo.get(BigDecimal.ZERO.intValue());
            if (!onlyAllocationInfo.getCxMachineCode().equals(onlyCxMachineInfo.getCxMachineCode())) {
                return false;
            }
            //标记不可延长探测及修正其分配结束日
            adjustEndDayByForceOffline(productionContext, groupInfo, onlyCxMachine, onlyCxMachineInfo, onlyAllocationInfo);
            if (ProductionConstant.MONTH_START_DAY.equals(onlyCxMachine.getEarliestChangeGroupDay())) {
                //整段移除，即在机结构周期第一天就强行下机
                onlyCxMachineInfo.forceOffline(productionContext, onlyAllocationInfo);
                continueAllocationList.remove(onlyAllocationInfo);
            }
            return true;
        }
        //在机有多台-设置真实的结构切换日-即排除停工日的干扰
//        forceConclusionList.forEach(single -> single.setRealEarliestChangeGroupDay(context));
        List<DayReduceInfo> dayReduceInfo = getDayReduceInfoByForceOffline(productionContext, appointOfflineInfo);
        if (CollectionUtils.isEmpty(dayReduceInfo)) {
            return false;
        }
        //按日期由小到大排序
        Map<String, Integer> originHighQtyMap = groupInfo.getSkuHeightQty();
        dayReduceInfo.sort(Comparator.comparing(DayReduceInfo::getDay));
        dayReduceInfo.forEach(singleDayReduceInfo -> {
            handlerContinueInfoByForce(productionContext, groupInfo, originHighQtyMap, singleDayReduceInfo, appointOfflineInfo, continueAllocationList, continueSkuInfo);
        });
        return true;
    }

    /**
     * 根据指定机台业务信息，获取强行下机日理论需要扣减的胎胚种类数和硫化机台数
     *
     * @param context            排产上下文
     * @param appointOfflineInfo 指定信息
     * @return
     */
    private List<DayReduceInfo> getDayReduceInfoByForceOffline(Context context, AppointGroupReduceMachineInfo appointOfflineInfo) {
        if (null == appointOfflineInfo) {
            return Collections.emptyList();
        }
        List<OnlineGroupAppointCxMachineInfo> forceConclusionList = appointOfflineInfo.getForceOfflineCxMachine(context);
        if (CollectionUtils.isEmpty(forceConclusionList)) {
            return Collections.emptyList();
        }
        List<CxMachineAllocationPlanHelper> originContinueCxMachineInfo = appointOfflineInfo.getOnlineCxMachineInfo();
        if (CollectionUtils.isEmpty(originContinueCxMachineInfo) || originContinueCxMachineInfo.size() <= BigDecimal.ONE.intValue()) {
            return Collections.emptyList();
        }
        DayReduceInfo maxLimit = getMaxInfo(context, appointOfflineInfo);
        //按日分组
        Map<Integer, List<OnlineGroupAppointCxMachineInfo>> dayForceConclusionGroup = forceConclusionList.stream().collect(Collectors.groupingBy(OnlineGroupAppointCxMachineInfo::getEarliestChangeGroupDay));
        List<Integer> reduceDayList = Lists.newArrayList(dayForceConclusionGroup.keySet());
        //按日期由小到大排序：有可能多台都需要强制下机
        reduceDayList.sort(Comparator.comparing(Integer::intValue));
        List<DayReduceInfo> dayReduceInfoList = Lists.newArrayList();
        reduceDayList.forEach(reduceDay -> {
            //当日需要强制下机
            List<OnlineGroupAppointCxMachineInfo> currentReduceCxMachineInfo = dayForceConclusionGroup.get(reduceDay);
            if (CollectionUtils.isEmpty(currentReduceCxMachineInfo)) {
                return;
            }
            //得到在当日之前，所有需要强制下机
            List<OnlineGroupAppointCxMachineInfo> reduceCxMachineInfo = forceConclusionList.stream().filter(single -> single.getEarliestChangeGroupDay() >= reduceDay).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(reduceCxMachineInfo)) {
                return;
            }
            List<CxMachineAllocationPlanHelper> reduceAllocationInfo = reduceCxMachineInfo.stream().map(OnlineGroupAppointCxMachineInfo::getOnlineGroupAllocationInfo).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(reduceAllocationInfo)) {
                return;
            }
            Integer reduceEmbryoCount = reduceAllocationInfo.stream().mapToInt(CxMachineAllocationPlanHelper::getMaxEmbryoCodeCount).sum();
            Integer reduceLhMachines = reduceAllocationInfo.stream().mapToInt(CxMachineAllocationPlanHelper::getMaxRatio).sum();
            Set<String> reduceCxMachineSet = reduceAllocationInfo.stream().map(CxMachineAllocationPlanHelper::getCxMachineCode).collect(Collectors.toSet());
            Set<CxMachineBaseInfoVo> dayCxMachineInfoSet = currentReduceCxMachineInfo.stream().map(OnlineGroupAppointCxMachineInfo::getCxMachineInfo).collect(Collectors.toSet());
            dayReduceInfoList.add(new DayReduceInfo(reduceDay, dayCxMachineInfoSet, maxLimit.getMaxEmbryoCodeCount(), maxLimit.getMaxLhMachines(), reduceEmbryoCount, reduceLhMachines, reduceCxMachineSet));
        });
        if (CollectionUtils.isEmpty(dayReduceInfoList)) {
            return Collections.emptyList();
        }
        return dayReduceInfoList;
    }

    /**
     * 处理在机结构续作Sku信息，因指定业务导致的中途强制下机
     * 1、有的Sku需要强制收尾
     * 2、有的Sku需要中途进行强制降膜
     *
     * @param context
     * @param groupInfo
     * @param originHighQtyMap
     * @param dayReduceInfo
     * @param appointOfflineInfo
     * @param continueAllocationList
     * @param continueSkuInfo
     */
    private void handlerContinueInfoByForce(Context context, ProductionPlanGroupInfo groupInfo, Map<String, Integer> originHighQtyMap, DayReduceInfo dayReduceInfo, AppointGroupReduceMachineInfo appointOfflineInfo, List<CxMachineAllocationPlanHelper> continueAllocationList, CxContinueInfoHelper continueSkuInfo) {
        if (null == dayReduceInfo || null == appointOfflineInfo || CollectionUtils.isEmpty(continueAllocationList)) {
            return;
        }
        Integer productionDay = dayReduceInfo.getDay();
        if (null == productionDay || CollectionUtils.isEmpty(dayReduceInfo.getCxMachineSet())) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = appointOfflineInfo.getGroupName();
        List<ContinueSkuDayUsedInfo> continueUsedInfoList = getContinueSkuUsedInfo(productionContext, productionDay, groupName, continueSkuInfo.getContinueSkuMouldNumberMap());
        if (CollectionUtils.isEmpty(continueUsedInfoList)) {
            return;
        }
        //续作Sku减硫化机台
        reduceLhMachineByForce(productionContext, groupInfo, originHighQtyMap, dayReduceInfo, continueUsedInfoList);
        //加入到调整结果
        groupInfo.addContinueSkuAdjustInfo(continueUsedInfoList);
        //标记不可延长探测及修正其分配结束日
        markNoTimeExtensionAndUpdateEndDay(productionContext, groupInfo, dayReduceInfo, appointOfflineInfo);
        //月初第一天就要强制下机，从续作分配中移除配置
        removeAllocationByMonthStartDay(productionContext, groupInfo, continueAllocationList, dayReduceInfo, appointOfflineInfo);
    }

    /**
     * 续作Sku强制下机
     *
     * @param context
     * @param groupInfo
     * @param originHighQtyMap
     * @param dayReduceInfo
     * @param continueUsedInfoList
     */
    private void reduceLhMachineByForce(Context context, ProductionPlanGroupInfo groupInfo, Map<String, Integer> originHighQtyMap, DayReduceInfo dayReduceInfo, List<ContinueSkuDayUsedInfo> continueUsedInfoList) {
        if (CollectionUtils.isEmpty(continueUsedInfoList) || null == dayReduceInfo) {
            return;
        }
        List<ContinueSkuDayUsedInfo> leftOverList = continueUsedInfoList.stream().filter(singleSku -> !Boolean.TRUE.equals(singleSku.getIsForceOffline())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverList)) {
            return;
        }
        Map<String, List<ContinueSkuDayUsedInfo>> embryoCodeGroup = leftOverList.stream().collect(Collectors.groupingBy(ContinueSkuDayUsedInfo::getEmbryoCode));
        Integer maxEmbryoCodeCount = dayReduceInfo.getLeftOverMaxEmbryoCodeCount();
        Integer productionEmbryoCodeCount = embryoCodeGroup.keySet().size();
        if (productionEmbryoCodeCount > maxEmbryoCodeCount) {
            //需要减胎胚
            reduceEmbryoByForce(context, groupInfo, originHighQtyMap, embryoCodeGroup);
            //递归
            reduceLhMachineByForce(context, groupInfo, originHighQtyMap, dayReduceInfo, continueUsedInfoList);
        }
        Integer usedLhMachines = leftOverList.stream().mapToInt(ContinueSkuDayUsedInfo::getLeftOverUsedLhMachine).sum();
        Integer maxLhMachines = dayReduceInfo.getLeftOverMaxLhMachines();
        if (usedLhMachines <= maxLhMachines) {
            return;
        }
        //Sku需要降膜
        List<DayReduceLhMachinePriorityInfo> priorityList = selector.getPriorityBySku(context, groupInfo, originHighQtyMap, leftOverList);
        if (CollectionUtils.isEmpty(priorityList)) {
            return;
        }
        priorityList.sort(ProductionComparatorUtils.getReduceSkuPrioritySort());
        DayReduceLhMachinePriorityInfo priorityResult = priorityList.get(BigDecimal.ZERO.intValue());
        ContinueSkuDayUsedInfo find = leftOverList.stream().filter(singleSku -> priorityResult.getMaterialDesc().equals(singleSku.getMaterialDesc())).findFirst().orElse(null);
        if (null == find) {
            return;
        }
        //增加降膜数
        find.addReduceMoldNumber();
    }

    /**
     * 标记对应的成型机台分配，不可进行延长探测
     *
     * @param context            排产上下文
     * @param groupInfo          分组信息对象
     * @param dayReduceInfo      日扣减信息
     * @param appointOfflineInfo 指定信息
     */
    private void markNoTimeExtensionAndUpdateEndDay(Context context, ProductionPlanGroupInfo groupInfo, DayReduceInfo dayReduceInfo, AppointGroupReduceMachineInfo appointOfflineInfo) {
        if (null == dayReduceInfo || null == appointOfflineInfo) {
            return;
        }
        Set<CxMachineBaseInfoVo> dayCxMachineInfoSet = dayReduceInfo.getDayCxMachineInfoSet();
        if (CollectionUtils.isEmpty(dayCxMachineInfoSet)) {
            return;
        }
        List<OnlineGroupAppointCxMachineInfo> forceOffMachineInfo = appointOfflineInfo.getForceOffMachineInfo();
        if (CollectionUtils.isEmpty(forceOffMachineInfo)) {
            return;
        }
        List<CxMachineAllocationPlanHelper> onlineCxMachineInfo = appointOfflineInfo.getOnlineCxMachineInfo();
        if (CollectionUtils.isEmpty(onlineCxMachineInfo)) {
            return;
        }
        dayCxMachineInfoSet.forEach(dayCxMachineInfo -> {
            List<CxMachineAllocationPlanHelper> findList = onlineCxMachineInfo.stream().filter(singleAllocation -> dayCxMachineInfo.getCxMachineCode().equals(singleAllocation.getCxMachineCode())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(findList) || findList.size() > BigDecimal.ONE.intValue()) {
                return;
            }
            List<OnlineGroupAppointCxMachineInfo> findForceOffMachineList = forceOffMachineInfo.stream().filter(singleForce -> dayCxMachineInfo == singleForce.getCxMachineInfo()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(findForceOffMachineList) || findForceOffMachineList.size() > BigDecimal.ONE.intValue()) {
                return;
            }
            CxMachineAllocationPlanHelper continueAllocation = findList.get(BigDecimal.ZERO.intValue());
            if (null == continueAllocation) {
                return;
            }
            OnlineGroupAppointCxMachineInfo onlyCxMachine = findForceOffMachineList.get(BigDecimal.ZERO.intValue());
            adjustEndDayByForceOffline(context, groupInfo, onlyCxMachine, dayCxMachineInfo, continueAllocation);
        });
    }

    /**
     * 月初第一天强制下机，则需从整个在机续作分配中移除配置
     *
     * @param context                排产上下文
     * @param groupInfo              分组信息对象
     * @param continueAllocationList 续作分配信息
     * @param dayReduceInfo          日扣减
     * @param appointOfflineInfo     指定信息
     */
    private void removeAllocationByMonthStartDay(Context context, ProductionPlanGroupInfo groupInfo, List<CxMachineAllocationPlanHelper> continueAllocationList, DayReduceInfo dayReduceInfo, AppointGroupReduceMachineInfo appointOfflineInfo) {
        if (!ProductionConstant.MONTH_START_DAY.equals(dayReduceInfo.getDay())) {
            return;
        }
        if (CollectionUtils.isEmpty(continueAllocationList)) {
            return;
        }
        //表示月初第一天就要强制下机
        Set<CxMachineBaseInfoVo> forceCxMachineSet = dayReduceInfo.getDayCxMachineInfoSet();
        if (CollectionUtils.isEmpty(forceCxMachineSet)) {
            return;
        }
        List<CxMachineAllocationPlanHelper> onlineCxMachineInfo = appointOfflineInfo.getOnlineCxMachineInfo();
        if (CollectionUtils.isEmpty(onlineCxMachineInfo)) {
            return;
        }
        forceCxMachineSet.forEach(dayCxMachineInfo -> {
            List<CxMachineAllocationPlanHelper> findList = onlineCxMachineInfo.stream().filter(singleAllocation -> dayCxMachineInfo.getCxMachineCode().equals(singleAllocation.getCxMachineCode())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(findList) || findList.size() > BigDecimal.ONE.intValue()) {
                return;
            }
            CxMachineAllocationPlanHelper continueAllocation = findList.get(BigDecimal.ZERO.intValue());
            if (null == continueAllocation) {
                return;
            }
            continueAllocationList.remove(continueAllocation);
        });
    }

    /**
     * 调整收尾日，因指定业务导致的成型机台强制下机
     * 1、需要释放资源
     * 1.1、日产能分配量(包含特殊材料用量)
     * 1.2、成型工装用量
     * 2、需要调整分配的结束日及分配天数信息
     * 3、需要调整groupInfo剩余分配量
     *
     * @param context
     * @param groupInfo
     * @param currentOnlineInfo
     * @param dayCxMachineInfo
     */
    private void adjustEndDayByForceOffline(Context context, ProductionPlanGroupInfo groupInfo, OnlineGroupAppointCxMachineInfo currentOnlineInfo, CxMachineBaseInfoVo dayCxMachineInfo, CxMachineAllocationPlanHelper continueAllocation) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer monthEndDay = productionContext.getMonthDays();
        currentOnlineInfo.setRealEarliestChangeGroupDay(productionContext);
        Integer realEarliestOnlineDay = currentOnlineInfo.getRealEarliestChangeGroupDay();
        Integer realEndDay = getPreviousDay(dayCxMachineInfo, realEarliestOnlineDay, monthEndDay);
        Set<Integer> releaseDayInfo;
        if (null == realEndDay) {
            releaseDayInfo = dayCxMachineInfo.getAllocationDaySet();
        } else {
            releaseDayInfo = continueAllocation.getReduceDaysByForceOffline(productionContext, realEndDay);
        }
        if (CollectionUtils.isEmpty(releaseDayInfo)) {
            return;
        }
        if (null != realEndDay) {
            continueAllocation.beforeConclusion(realEarliestOnlineDay, releaseDayInfo.size());
            //只在当前场景下使用：需要修改备份，不然重排时不生效
            continueAllocation.getCloneObject().beforeConclusion(realEarliestOnlineDay, releaseDayInfo.size());
        }
        continueAllocation.markNoTimeExtension();
        //只在当前场景下使用：需要修改备份，不然重排时不生效
        continueAllocation.getCloneObject().markNoTimeExtension();
        deductionHandler.deductionDayInfoByContinueForceOffline(productionContext, dayCxMachineInfo, groupInfo, continueAllocation, releaseDayInfo);
    }

    /**
     * 获取续作Sku在productionDay的排产信息
     * 使用模具数和硫化机台数
     *
     * @param context                   排产上下文
     * @param productionDay             排产日
     * @param groupName                 分组名
     * @param continueSkuMouldNumberMap 续作Sku信息
     * @return
     */
    private List<ContinueSkuDayUsedInfo> getContinueSkuUsedInfo(Context context, Integer productionDay, String groupName, Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap) {
        if (null == productionDay || StringUtils.isBlank(groupName) || CollectionUtils.isEmpty(continueSkuMouldNumberMap)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionPlanGroupInfo groupInfo = productionContext.getGroupProductionInfo().get(groupName);
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyList();
        }
        GroupPlanCxLhCapacityLimitHelper dayLimitInfo = dayProductionLimitInfo.get(productionDay);
        if (null == dayLimitInfo) {
            return Collections.emptyList();
        }
        Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = dayLimitInfo.getSkuProductionDetailInfo();
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return Collections.emptyList();
        }
        Map<String, ContinueSkuDayUsedInfo> continueSkuUsedMap = Maps.newHashMap();
        skuProductionDetailInfo.forEach((materialDesc, detailInfo) -> {
            if (!continueSkuMouldNumberMap.containsKey(materialDesc)) {
                return;
            }
            if (CollectionUtils.isEmpty(detailInfo)) {
                return;
            }
            Set<String> usedMold = Sets.newHashSet();
            detailInfo.forEach(detail -> usedMold.addAll(detail.getUsedMouldSet()));
            if (CollectionUtils.isEmpty(usedMold)) {
                return;
            }
            Integer maxUsedMoldNumber = continueSkuMouldNumberMap.get(materialDesc).getMouldNumber();
            Integer usedMoldNumber = usedMold.size();
            Integer realContinueMoldNumber = Math.min(usedMoldNumber, maxUsedMoldNumber);
            SkuDayProductionInfoHelper productionDetail = detailInfo.get(BigDecimal.ZERO.intValue());
            ContinueSkuDayUsedInfo usedInfo = ContinueSkuDayUsedInfo.creatInitByProductionInfo(productionDetail);
            usedInfo.updateUsedLhMachine(realContinueMoldNumber);
            continueSkuUsedMap.put(materialDesc, usedInfo);
        });
        if (CollectionUtils.isEmpty(continueSkuUsedMap)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(continueSkuUsedMap.values());
    }

    /**
     * 获取最大胎胚种类数及最大硫化机台数
     *
     * @param context            排产上下文
     * @param appointOfflineInfo 指定业务信息
     * @return
     */
    private DayReduceInfo getMaxInfo(Context context, AppointGroupReduceMachineInfo appointOfflineInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = appointOfflineInfo.getGroupName();
        ProductionPlanGroupInfo groupInfo = productionContext.getGroupProductionInfo().get(groupName);
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return new DayReduceInfo(null, null, BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), null, null, null);
        }
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitInfoList = Lists.newArrayList(dayProductionLimitInfo.values());
        //最大胎胚种类数
        Integer maxEmbryoCodeCount = dayLimitInfoList.stream().mapToInt(GroupPlanCxLhCapacityLimitHelper::getMaxEmbryoCodeCount).max().orElse(BigDecimal.ZERO.intValue());
        //最大硫化机台数
        Integer maxLhMachines = dayLimitInfoList.stream().mapToInt(GroupPlanCxLhCapacityLimitHelper::getMaxLhMachineCount).max().orElse(BigDecimal.ZERO.intValue());
        return new DayReduceInfo(null, null, maxEmbryoCodeCount, maxLhMachines, null, null, null);
    }

    /**
     * 获取cxMachineInfo其在earliestOffDay的前一个排产日
     * 需要剔除其停产日
     *
     * @param cxMachineInfo  成型机信息对象
     * @param earliestOffDay 下机日
     * @param monthEndDay    月末最大日
     * @return
     */
    private Integer getPreviousDay(CxMachineBaseInfoVo cxMachineInfo, Integer earliestOffDay, Integer monthEndDay) {
        if (null == cxMachineInfo || null == earliestOffDay || null == monthEndDay) {
            return null;
        }
        if (ProductionConstant.MONTH_START_DAY.equals(earliestOffDay)) {
            return null;
        }
        if (earliestOffDay > monthEndDay) {
            return monthEndDay;
        }
        Integer startDay = earliestOffDay;
        for (; startDay >= ProductionConstant.MONTH_START_DAY; ) {
            startDay = startDay - BigDecimal.ONE.intValue();
            //非停产
            if (!cxMachineInfo.getStopDayInfo().contains(startDay)) {
                break;
            }
        }
        if (startDay < ProductionConstant.MONTH_START_DAY) {
            return null;
        }
        return startDay;
    }

    /**
     * 在机分组续作胎胚需要强制下机
     *
     * @param embryoCodeGroup 已排产胎胚信息
     */
    private void reduceEmbryoByForce(Context context, ProductionPlanGroupInfo groupInfo, Map<String, Integer> originHighQtyMap, Map<String, List<ContinueSkuDayUsedInfo>> embryoCodeGroup) {
        //需要减胎胚
        List<DayReduceEmbryoPriorityInfo> reduceEmbryoPriorityList = selector.buildPriorityByEmbryo(embryoCodeGroup);
        if (CollectionUtils.isEmpty(reduceEmbryoPriorityList)) {
            return;
        }
        Integer minLhMachine = reduceEmbryoPriorityList.stream().mapToInt(DayReduceEmbryoPriorityInfo::getUsedLhMachine).min().orElse(BigDecimal.ZERO.intValue());
        List<DayReduceEmbryoPriorityInfo> minList = reduceEmbryoPriorityList.stream().filter(single -> minLhMachine.equals(single.getUsedLhMachine())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(minList)) {
            return;
        }
        if (minList.size() == BigDecimal.ONE.intValue()) {
            //单个
            DayReduceEmbryoPriorityInfo confirmReduceEmbryo = minList.get(BigDecimal.ZERO.intValue());
            List<ContinueSkuDayUsedInfo> continueSkuInfo = confirmReduceEmbryo.getContinueSkuInfo();
            if (CollectionUtils.isEmpty(continueSkuInfo)) {
                return;
            }
            //标记强制下机
            continueSkuInfo.forEach(singleSku -> singleSku.setSkuForceOffline());
        }
        //多个时，需要根据Sku的优先级确定
        List<ContinueSkuDayUsedInfo> prioritySkuList = Lists.newArrayList();
        minList.forEach(singleEmbryo -> prioritySkuList.addAll(singleEmbryo.getContinueSkuInfo()));
        if (CollectionUtils.isEmpty(prioritySkuList)) {
            return;
        }
        List<DayReduceLhMachinePriorityInfo> priorityList = selector.getPriorityBySku(context, groupInfo, originHighQtyMap, prioritySkuList);
        if (CollectionUtils.isEmpty(priorityList)) {
            return;
        }
        priorityList.sort(ProductionComparatorUtils.getReduceSkuPrioritySort());
        String confirmReduceEmbryoCode = priorityList.get(BigDecimal.ZERO.intValue()).getEmbryoCode();
        DayReduceEmbryoPriorityInfo find = minList.stream().filter(single -> confirmReduceEmbryoCode.equals(single.getEmbryoCode())).findFirst().orElse(null);
        if (null == find) {
            return;
        }
        List<ContinueSkuDayUsedInfo> continueSkuInfo = find.getContinueSkuInfo();
        if (CollectionUtils.isEmpty(continueSkuInfo)) {
            return;
        }
        //标记强制下机
        continueSkuInfo.forEach(singleSku -> singleSku.setSkuForceOffline());
    }

}
