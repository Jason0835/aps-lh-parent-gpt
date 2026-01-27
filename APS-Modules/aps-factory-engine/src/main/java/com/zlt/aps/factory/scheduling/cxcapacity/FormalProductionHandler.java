package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.enums.ProductionStageEnum;
import com.zlt.aps.factory.logrecorder.TbrMouldFormalProductionLogRecorder;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Map;

/**
 * 开始正式排产，按结构进行排产
 * 此时已经确定了各个结构的机台分配情况
 *
 * @author ZLT
 * @date 20260101
 */
@Slf4j
@Component
public class FormalProductionHandler extends OnLineGroupOnLineMachineHandler {
    /**
     * 正式排产，对结构按已经分配好的机台产能进行排产
     * 先在机结构，其次新增结构
     * 1、在机结构先排产
     * 1.1、在机结构的续作Sku使用续作模具排产
     * 1.2、在机结构的续作Sku的同规格同花纹排产(还是续作模具)
     * 1.3、在机结构的续作Sku的同生胎同模具排产(还是续作模具)
     * 1.4、在机结构的新增Sku排产
     * 2、新增结构排产
     *
     * @param context          排产上下文
     * @param allGroupPlanInfo 所有结构信息
     * @param allContinueInfo  在机结构信息
     */
    public void productionContinueGroup(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<String, CxContinueInfoHelper> allContinueInfo) {
        if (CollectionUtils.isEmpty(allGroupPlanInfo) && CollectionUtils.isEmpty(allContinueInfo)) {
            //记录日志
            log.info(TbrMouldFormalProductionLogRecorder.addDataEmptyLog(context));
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        allGroupPlanInfo.forEach((structureName, groupPlanInfo) -> groupPlanInfo.setThisRoundCanProduction());
        log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupLog(productionContext));
        //续作部分排产 1、续作Sku 2、续作Sku同规格同花纹高优先级量 3、续作Sku同生胎共模具高优先级量
        productionContinue(ProductionStageEnum.FORMAL_STAGE, productionContext, allContinueInfo, allGroupPlanInfo);
        //4、在机机构新增Sku排产
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(structureName);
            if (null == groupPlan) {
                return;
            }
            log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupSingleGroupAddSkuLog(productionContext, structureName));
            CxAddSkuProductionHandler.productionAddSkuByContinueCxMachine(productionContext, groupPlan, new HashSet<>());
        });
        //5、非在机结构，新增规格排产
        allGroupPlanInfo.forEach((structureName, groupPlan) -> {
            if (allContinueInfo.containsKey(structureName)) {
                return;
            }
            log.info(TbrMouldFormalProductionLogRecorder.addProductionAddGroupSingleGroupLog(context, structureName));
            CxAddSkuProductionHandler.productionAddSkuByContinueCxMachine(productionContext, groupPlan, new HashSet<>());
        });
    }

}
