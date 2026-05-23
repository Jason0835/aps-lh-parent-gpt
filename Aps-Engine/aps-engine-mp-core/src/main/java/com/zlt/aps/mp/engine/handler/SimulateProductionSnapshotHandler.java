package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模拟排产-数据快照处理器
 *
 * @author ZLT
 * @date 20260523
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulateProductionSnapshotHandler {

    /**
     * 使用场景：
     * 1、非在机结构(分组)新分配到成型机台时
     * 2、在机结构(分组)新分配到(非在产)成型机台时
     * 在进行模拟排产前，备份各Sku的待排产量
     *
     * @param context        排产上下文
     * @param productionPlan 分配信息
     */
    public void saveProductionBeforeSnapshotData(Context context, CxMachineAllocationPlanHelper productionPlan) {
        if (null == productionPlan) {
            return;
        }
        ProductionPlanGroupInfo preProductionGroup = productionPlan.getProductionPlanInfo();
        if (null == preProductionGroup) {
            return;
        }
        //先清除原有的数据
        preProductionGroup.setBeforeProductionSnapshotMap(Collections.emptyMap());
        //可排产计划
        List<MonthPlanProductionRequirePlanVo> groupPlanData = preProductionGroup.getGroupPlanData();
        List<MonthPlanProductionRequirePlanVo> hasProductionPlanList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionPlanList)) {
            return;
        }
        //按Sku分组
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuProductionPlanMap = hasProductionPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        if (CollectionUtils.isEmpty(skuProductionPlanMap)) {
            return;
        }
        Map<String, SkuProductionSnapshot> snapshotMap = Maps.newHashMap();
        skuProductionPlanMap.forEach((materialDesc, preProductionList) -> {
            if (CollectionUtils.isEmpty(preProductionList)) {
                return;
            }
            Integer sumHeightProductionQty = preProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
            Integer sumProductionQty = preProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
            SkuProductionSnapshot snapshot = SkuProductionSnapshot.buildSnapshot(materialDesc, sumHeightProductionQty, sumProductionQty);
            if (null == snapshot) {
                return;
            }
            snapshotMap.put(materialDesc, snapshot);
        });
        preProductionGroup.setBeforeProductionSnapshotMap(snapshotMap);
    }

}
