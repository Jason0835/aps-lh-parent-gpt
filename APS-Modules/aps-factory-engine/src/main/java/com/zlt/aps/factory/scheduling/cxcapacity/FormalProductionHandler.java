package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.MouldShellBaseInfoVo;
import com.zlt.aps.factory.enums.ContinueTypeEnum;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
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
public class FormalProductionHandler {
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
    public static void productionContinueGroup(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<String, CxContinueInfoHelper> allContinueInfo) {
        if (CollectionUtils.isEmpty(allGroupPlanInfo) && CollectionUtils.isEmpty(allContinueInfo)) {
            //记录日志
            log.info(TbrMouldFormalProductionLogRecorder.addDataEmptyLog(context));
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, MouldShellBaseInfoVo> mouldShellMap = productionContext.getBaseDataContainer().getMouldShellMap();
        allGroupPlanInfo.forEach((structureName, groupPlanInfo) -> groupPlanInfo.setThisRoundCanProduction());
        log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupLog(productionContext));
        //1、续作先排
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupSingleGroupLog(productionContext, structureName, ContinueTypeEnum.SAME_SKU));
            productionContinueByType(productionContext, allGroupPlanInfo, structureName, cxContinueInfo, mouldShellMap, ContinueTypeEnum.SAME_SKU);
        });
        //2、接着进行同规格同花纹的续作高优先级部分进行排产
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupSingleGroupLog(productionContext, structureName, ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN));
            productionContinueByType(productionContext, allGroupPlanInfo, structureName, cxContinueInfo, mouldShellMap, ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN);
        });
        //3、接着进行共生胎，同模具的续作高优级部分进行排产
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupSingleGroupLog(productionContext, structureName, ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD));
            productionContinueByType(productionContext, allGroupPlanInfo, structureName, cxContinueInfo, mouldShellMap, ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD);
        });
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

    /**
     * 进行续作排产
     *
     * @param context          排产上下文
     * @param allGroupPlanInfo 所有分组计划对象
     * @param groupName        分组名
     * @param cxContinueInfo   对应的续作信息
     * @param mouldShellMap    模块信息
     * @param type             续作类型
     */
    private static void productionContinueByType(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, String groupName, CxContinueInfoHelper cxContinueInfo, Map<String, MouldShellBaseInfoVo> mouldShellMap, ContinueTypeEnum type) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(groupName);
        if (null == groupPlan) {
            log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupNoGroupPlanLog(context, groupName, type));
            return;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap = cxContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
            //记录日志
            log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupNoContinueSkuLog(context, groupName, type));
            return;
        }
        Integer monthDays = productionContext.getMonthDays();
        //续作Sku
        if (ContinueTypeEnum.SAME_SKU == type) {
            CxContinueGroupAllocationHandler.productionContinueSku(productionContext, groupPlan, continueSkuInfoMap);
            return;
        }
        //同规格同花纹 or 共生胎同模具
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == type || ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == type) {
            CxContinueProductionHandler.productionContinueByType(context, groupPlan, type, monthDays, continueSkuInfoMap, mouldShellMap);
            return;
        }
    }

}
