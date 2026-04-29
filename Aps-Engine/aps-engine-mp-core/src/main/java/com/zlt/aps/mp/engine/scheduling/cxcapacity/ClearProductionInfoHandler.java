package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.GroupContinueAllocationInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 清除排产信息数据
 * 使用场景：
 * 1、模拟排产前，需要清除在产机台进行月初释放机台测算数据
 * 2、正式排产前，需要清除模拟排产的数据
 * 3、不同分组续作Sku模具分配比例进行调整，测算调整结果后，需要清除续作排产信息，重排续作部分
 *
 * @author ZLT
 * @date 20260127
 */
@Slf4j
@Component
public class ClearProductionInfoHandler {

    /**
     * 1、在模拟排产前的处理
     * 1.1、对测算在产机台收尾点的续作部分排产清除
     * 1.2、根据在产机台分配结果，构建分组计划的在产机台排产限制信息
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有分组计划
     * @param continueAllocationList 在产机台分配情况
     * @param allContinueMap         所有续作信息
     */
    public void beforeSimulateProductionHandler(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //对测算成型产能分配的续作部分进行重排-先清空已排信息
        clearProductionInfo(productionContext);
        //清除日排产限制使用量，保留成型每日分配量及每日结构切换次数
        productionContext.clearAllDayLimitUsed();
        //在机结构对在产机台构建硫化组限制
        buildLimitByContinueMachine(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
    }

    /**
     * 1、重新模拟排产
     * 1.1、对测算在产机台收尾点的续作部分排产清除
     * 1.2、将分配信息还原至续作在产机台分时间点
     * 1.3、根据在产机台分配结果，构建分组计划的在产机台排产限制信息
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有分组计划
     * @param continueAllocationList 在产机台分配情况
     * @param allContinueMap         所有续作信息
     */
    public void resetProductionBySimulateProductionHandler(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //1、对测算成型产能分配的续作部分进行重排-先清空已排信息
        clearProductionInfo(productionContext);
        //清除日排产限制使用量--包含成型分配量和每日切换结构次数、成型工装占用量清空
        productionContext.clearAllDayUsedInfo();
        //2、成型产能分配-还原到在产机台的初始分配
        resetContinueMachineAllocationInfo(context, continueAllocationList);
        //3、在机结构对在产机台构建硫化组限制
        buildLimitByContinueMachine(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
    }

    /**
     * 在正式排产前重新构建分组限制信息
     * 根据结构排产分配表-构建限制信息
     *
     * @param context           排产上下文
     * @param allGroupPlanInfo  所有分组计划对象
     * @param allAllocationList 分组转产配置
     */
    public void resetBeforeFormalProduction(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, List<MpStructureAllocation> allAllocationList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //1、清除模拟排产信息
        clearProductionInfo(productionContext);
        //清除日排产限制使用量，保留成型每日分配量及每日结构切换次数
        productionContext.clearAllDayLimitUsed();
        //2、根据分组转产配置，重新构建分组的限制信息
        allGroupPlanInfo.forEach((groupName, groupProductionInfo) -> {
            List<MpStructureAllocation> groupAllocationList;
            if (CollectionUtils.isEmpty(allAllocationList)) {
                groupAllocationList = new ArrayList<>();
            } else {
                groupAllocationList = allAllocationList.stream().filter(singleAllocation -> groupName.equals(singleAllocation.getStructureName())).collect(Collectors.toList());
            }
            //重新设置分配的机台
            Set<String> allocationSet = groupAllocationList.stream().map(MpStructureAllocation::getCxMachineCode).collect(Collectors.toSet());
            groupProductionInfo.setAllocationCxMachineCodeSet(allocationSet);
            groupProductionInfo.buildDayProductionLimitInfoByStructureAllocation(context, groupAllocationList);

            //将产能限制Map 置空
            groupProductionInfo.setDailyCapacityLimitVoMap(null);
        });
    }

    /**
     * 清除排产信息
     * 场景：
     * 1、模拟排产前，清除在机结构在产机台对续作的排产测试在产机台的收尾点
     * 2、正式排产前，清除模拟排产信息
     * 3、重新模拟排产
     * 清除的信息：
     * 1、Sku记录的总排产量及总损耗量
     * 2、计划还原到排产前信息
     * 3、模具的排产信息清空
     * 4、Sku排产限制信息
     * 5、模壳每日使用量清空
     * 6、模具分配比例每日使用清空
     * 7、胶囊卡盘每日使用量清空
     * 8、特殊原材料的库存消耗量清空
     *
     * @param productionContext
     */
    private void clearProductionInfo(TbrProductionContext productionContext) {
        //物料已排产量及损耗量清空
        productionContext.resetSkuProductionAndWastageQty();
        //处理计划的待排产量及排产标记重置
        resetProductionPlanInfo(productionContext);
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        //重新构建模具排产信息，全部清空
        clearMouldProductionInfo(baseDataContainer);
        //清空Sku排产限制情况
        productionContext.clearSkuProductionLimitInfo();
        //清除模壳使用量
        productionContext.clearAllMouldShellUsed();
        //清除模具分配使用量
        productionContext.clearAllMouldAllocationUsed();
        //清除胶囊卡盘使用量
        productionContext.clearAllCapsuleChuckUsed();
        //清除特殊原材料的消耗量
        productionContext.clearSpecialMaterialInfoSkuAllocationQty();
    }

    /**
     * 重置计划的排产量信息--还原
     *
     * @param productionContext 排产上下文
     */
    private void resetProductionPlanInfo(TbrProductionContext productionContext) {
        Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan();
        if (CollectionUtils.isEmpty(allSinglePlanMap)) {
            return;
        }
        allSinglePlanMap.forEach((monthPlanId, singlePlan) -> singlePlan.resetProductionDataInfo());
    }

    /**
     * 清空模具排产信息
     *
     * @param baseDataContainer 容器对象
     */
    private void clearMouldProductionInfo(BaseDataContainer baseDataContainer) {
        if (null == baseDataContainer) {
            return;
        }
        Map<String, ProductionMouldInfoVo> allMouldInfoMap = baseDataContainer.getMouldInfoMap();
        if (CollectionUtils.isEmpty(allMouldInfoMap)) {
            return;
        }
        allMouldInfoMap.forEach((mouldCode, singleMouldInfo) -> {
            singleMouldInfo.setFinishDaySet(new HashSet<>());
            singleMouldInfo.setDayProductionInfo(new HashMap<>(64));
        });
    }

    /**
     * 根据续作测算分配，构建排产限制信息
     *
     * @param productionContext      排产上下文
     * @param allGroupPlanMap        所有分组对象集合
     * @param continueAllocationList 所有在产分组的分配信息集合
     * @param allContinueMap         所有续作Sku信息
     */
    private void buildLimitByContinueMachine(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        if (CollectionUtils.isEmpty(allContinueMap)) {
            return;
        }
        //在机结构对在产机台构建硫化组限制
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        allContinueMap.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
            List<CxMachineAllocationPlanHelper> continueCxMachineAllocation = groupPlanMap.get(groupPlanInfo);
            if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
                log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(productionContext, structureName, null, null));
                return;
            }
            groupPlanInfo.buildDayProductionLimitInfoByContinue(productionContext, continueCxMachineAllocation);
        });
    }

    /**
     * 将成型机台分配信息重置到在产机台测算分配阶段
     * 重置在机分组，在产机台的分配情况
     *
     * @param context                排产上下文
     * @param continueAllocationList 在产机台分配情况
     * @return
     */
    private void resetContinueMachineAllocationInfo(Context context, List<CxMachineAllocationPlanHelper> continueAllocationList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        restoreGroupAllocationInfo(productionContext);
        if (CollectionUtils.isEmpty(continueAllocationList)) {
            return;
        }
        //配置还原
        continueAllocationList.forEach(singleConfiguration -> singleConfiguration.restoreConfiguration());
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfo)) {
            return;
        }
        //机台分配信息还原
        Map<String, CxMachineAllocationPlanHelper> machineAllocationMap = continueAllocationList.stream().collect(Collectors.toMap(CxMachineAllocationPlanHelper::getCxMachineCode, Function.identity()));
        allCxMachineInfo.forEach((cxMachineCode, singleMachineInfo) -> {
            singleMachineInfo.setAllocationDaySet(Sets.newHashSet());
            singleMachineInfo.setAllocationList(Lists.newArrayList());
            singleMachineInfo.setCxLhRatioMap(Maps.newHashMap());
            singleMachineInfo.setDayProductionLimitInfo(Maps.newHashMap());
            if (!machineAllocationMap.containsKey(cxMachineCode)) {
                return;
            }
            CxMachineAllocationPlanHelper continueAllocationInfo = machineAllocationMap.get(cxMachineCode);
            if (null == continueAllocationInfo) {
                return;
            }
            singleMachineInfo.addAllocationPlanInfo(productionContext, continueAllocationInfo);
        });
    }

    /**
     * 还原分组分配信息到续作测算阶段
     *
     * @param productionContext 排产上下文
     */
    private void restoreGroupAllocationInfo(TbrProductionContext productionContext) {
        Map<String, ProductionPlanGroupInfo> allGroupInfo = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupInfo)) {
            return;
        }
        Map<String, GroupContinueAllocationInfoVo> backupInfo = Optional.ofNullable(productionContext.getContinueCalculationAllocationInfo()).orElse(Collections.emptyMap());
        allGroupInfo.forEach((groupName, groupInfo) -> {
            GroupContinueAllocationInfoVo singleBackup = backupInfo.get(groupName);
            if (null == singleBackup) {
                groupInfo.setLeftOverNeedAllocationDays(groupInfo.getTheoryDays());
                groupInfo.setIsAllocationFinish(YesOrNoEnum.NO.getValue());
                return;
            }
            groupInfo.setLeftOverNeedAllocationDays(singleBackup.getLeftOverNeedAllocationDays());
            groupInfo.setIsAllocationFinish(singleBackup.getIsAllocationFinish());
        });
    }

}
