package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlanInfo = productionContext.getGroupProductionInfo();
        //1、收尾时间延长一天
        ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(groupName);
        //2、重新排产模拟-先续作Sku->同规格同花纹->同模具->新增Sku
        productionContinueBySingleGroup(cxAddSkuProductionHandler, ProductionStageEnum.SIMULATE_STAGE, productionContext, groupName, cxContinueInfo, allGroupPlanInfo);
        cxAddSkuProductionHandler.productionAddSkuBySingleGroup(groupName, context, groupPlan, continueCxMachineAllocation);
        //3、检测是否还需要延长

    }

}
