package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分组计划-分配时间延长处理器
 *
 * @author ZLT
 * @date 20260328
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupTimeExtensionHandler extends OnLineGroupOnLineMachineHandler {

    private final CxAddSkuProductionHandler cxAddSkuProductionHandler;

    public void handlerTimeExtension(List<CxMachineAllocationPlanHelper> continueCxMachineAllocation, Context context, String groupName, CxContinueInfoHelper cxContinueInfo) {
        if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlanInfo = productionContext.getGroupProductionInfo();
        ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(groupName);
        continueCxMachineAllocation.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getEndDay));
        CxMachineAllocationPlanHelper earliestConclusion = continueCxMachineAllocation.get(BigDecimal.ZERO.intValue());
        Integer earliestConclusionDay = earliestConclusion.getEndDay();
        if (!hasTimeExtension(productionContext, groupPlan, earliestConclusionDay)) {
            return;
        }
        //1、收尾时间延长一天 取得下一天
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Integer nextDay = context.getNextHasProductionDay(earliestConclusionDay, stopDaySet);
        //创建新的收尾天数信息


        //2、重新排产模拟-先续作Sku->同规格同花纹->同模具->新增Sku
        productionContinueBySingleGroup(cxAddSkuProductionHandler, ProductionStageEnum.SIMULATE_STAGE, productionContext, groupName, cxContinueInfo, allGroupPlanInfo);
        cxAddSkuProductionHandler.productionAddSkuBySingleGroup(groupName, context, groupPlan, continueCxMachineAllocation);
        //3、检测是否还需要延长

    }

    /**
     * 判断分组是否可进行延长
     * 满足以下两个条件
     * 1、分组是否还有未排产的实单量Sku
     * 2、未排产实单的Sku有模具产能
     *
     * @param context   排产上下文
     * @param groupPlan 结构
     * @param endDay    当前收尾日
     * @return
     */
    public boolean hasTimeExtension(Context context, ProductionPlanGroupInfo groupPlan, Integer endDay) {
        //最后一天，不能延长
        if (null == groupPlan || null == endDay || context.getProductionEndDay().equals(endDay)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> groupAllPlanList = groupPlan.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupAllPlanList)) {
            return false;
        }
        //实单还有剩余排产量
        List<MonthPlanProductionRequirePlanVo> hasActualNeedProductionList = groupAllPlanList.stream().filter(singlePlan -> singlePlan.hasActualProductionQuantity()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasActualNeedProductionList)) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer conclusionLhMachine = productionContext.getBaseDataContainer().getMinConclusionLhMachineCount(groupPlan.getGroupName());
        if (conclusionLhMachine <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //取得下一天
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Integer nextDay = context.getNextHasProductionDay(endDay, stopDaySet);
        //判断模具是否还有剩余产能
        Set<String> materialDescSet = hasActualNeedProductionList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> hasMouldCapacitySet = productionContext.getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, materialDescSet, nextDay, nextDay);
        if (CollectionUtils.isEmpty(hasMouldCapacitySet)) {
            return false;
        }
        return true;
    }

    /**
     * 清除模具排产数据，因结构需要延长收尾日
     *
     * @param context                     排产上下文
     * @param groupPlan                   分组
     * @param continueCxMachineAllocation 需要清除的信息
     */
    private void clearSimulateProductionDataByTimeExtension(Context context, ProductionPlanGroupInfo groupPlan, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        //按分配的信息，逐台进行清理数据
        if(null == groupPlan || CollectionUtils.isEmpty(continueCxMachineAllocation)){
            return ;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        continueCxMachineAllocation.forEach(singleAllocation ->{
            String cxMachineCode = singleAllocation.getCxMachineCode();
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineMap.get(cxMachineCode);
            if(null == cxMachineInfo){
                return ;
            }
            Set<Integer> deductionDaySet = cxMachineInfo.getAllocationDaySet(singleAllocation);

        });

    }

}
