package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
        if (CollectionUtils.isEmpty(allContinueMap)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //对测算成型产能分配的续作部分进行重排-先清空已排信息
        clearProductionData(productionContext);
        //在机结构对在产机台构建硫化组限制
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        allContinueMap.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
            List<CxMachineAllocationPlanHelper> continueCxMachineAllocation = groupPlanMap.get(groupPlanInfo);
            if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
                log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(productionContext, structureName, null, null));
                return;
            }
            groupPlanInfo.buildDayProductionLimitInfoByContinue(context, continueCxMachineAllocation);
        });
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
        //1、清除模拟排产信息
        clearProductionData(context);
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
     * 清除排产数据-重置排产数据
     * 场景：
     * 1、模拟排产前，清除在机结构在产机台对续作的排产测试在产机台的收尾点
     * 2、正式排产前，清除模拟排产信息
     *
     * @param context 排产上下文
     */
    private void clearProductionData(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //物料已排产量及损耗量清空
        productionContext.resetSkuProductionAndWastageQty();
        //处理计划的待排产量及排产标记重置
        Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan();
        if (!CollectionUtils.isEmpty(allSinglePlanMap)) {
            allSinglePlanMap.forEach((monthPlanId, singlePlan) -> singlePlan.resetProductionDataInfo());
        }
        //重新构建模具排产信息，全部清空
        Map<String, ProductionMouldInfoVo> allMouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
        if (!CollectionUtils.isEmpty(allMouldInfoMap)) {
            allMouldInfoMap.forEach((mouldCode, singleMouldInfo) -> {
                singleMouldInfo.setFinishDaySet(new HashSet<>());
                singleMouldInfo.setDayProductionInfo(new HashMap<>(64));
            });
        }
        //清空Sku排产限制情况
        productionContext.clearSkuProductionLimitInfo();
        //清除模壳使用量
        productionContext.clearAllMouldShellUsed();
        //清除模具分配使用量
        productionContext.clearAllMouldAllocationUsed();
        //清除胶囊卡盘使用量
        productionContext.clearAllCapsuleChuckUsed();
        //清除日排产限制使用量
        productionContext.clearAllDayLimitUsed();
        //清除特殊原材料的消耗量
        productionContext.clearSpecialMaterialInfoSkuAllocationQty();
    }

}
