package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldFormalProductionLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSimulateProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 在机结构在产机台续作排产处理
 * 1、续作Sku排产
 * 2、续作同规格同花纹Sku排产
 * 3、续作同生胎同模具Sku排产
 *
 * @author ZLT
 * @date 20260101
 */
@Slf4j
public class OnLineGroupOnLineMachineHandler {

    /**
     * 排产续作部分
     * 1、续作Sku
     * 2、同规格同花纹
     * 3、共生胎同模具
     *
     * @param cxAddSkuProductionHandler 处理器
     * @param productionStage           排产阶段
     * @param productionContext         排产上下文
     * @param allContinueInfo           续作Sku信息
     * @param allGroupPlanInfo          所有分组计划
     */
    public void productionContinue(CxAddSkuProductionHandler cxAddSkuProductionHandler, ProductionStageEnum productionStage, TbrProductionContext productionContext, Map<String, CxContinueInfoHelper> allContinueInfo, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo) {
        //1、续作先排
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            productionContinueByType(cxAddSkuProductionHandler, productionStage, productionContext, allGroupPlanInfo, structureName, cxContinueInfo, ContinueTypeEnum.SAME_SKU);
        });
        //2、todo 不同结构共用模具-分配比例调整

        //3、接着进行同规格同花纹的续作高优先级部分进行排产
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            productionContinueByType(cxAddSkuProductionHandler, productionStage, productionContext, allGroupPlanInfo, structureName, cxContinueInfo, ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN);
        });
        //4、接着进行共生胎，同模具的续作高优级部分进行排产
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            productionContinueByType(cxAddSkuProductionHandler, productionStage, productionContext, allGroupPlanInfo, structureName, cxContinueInfo, ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD);
        });
    }

    /**
     * 单分组，续作重新排产
     *
     * @param cxAddSkuProductionHandler 新增sku处理器
     * @param productionStage           排产阶段
     * @param productionContext         排产上下文
     * @param groupName                 分组名
     * @param cxContinueInfo            分组续作信息
     * @param allGroupPlanInfo          所有分组
     */
    public void productionContinueBySingleGroup(CxAddSkuProductionHandler cxAddSkuProductionHandler, ProductionStageEnum productionStage, TbrProductionContext productionContext, String groupName, CxContinueInfoHelper cxContinueInfo, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo) {
        //1、续作先排
        productionContinueByType(cxAddSkuProductionHandler, productionStage, productionContext, allGroupPlanInfo, groupName, cxContinueInfo, ContinueTypeEnum.SAME_SKU);
        //2、接着进行同规格同花纹的续作高优先级部分进行排产
        productionContinueByType(cxAddSkuProductionHandler, productionStage, productionContext, allGroupPlanInfo, groupName, cxContinueInfo, ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN);
        //3、接着进行共生胎，同模具的续作高优级部分进行排产
        productionContinueByType(cxAddSkuProductionHandler, productionStage, productionContext, allGroupPlanInfo, groupName, cxContinueInfo, ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD);
    }

    /**
     * 进行续作排产
     *
     * @param cxAddSkuProductionHandler 处理器
     * @param productionStage           排产阶段
     * @param context                   排产上下文
     * @param allGroupPlanInfo          所有分组计划对象
     * @param groupName                 分组名
     * @param cxContinueInfo            对应的续作信息
     * @param type                      续作类型
     */
    private void productionContinueByType(CxAddSkuProductionHandler cxAddSkuProductionHandler, ProductionStageEnum productionStage, Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, String groupName, CxContinueInfoHelper cxContinueInfo, ContinueTypeEnum type) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        recordStartProductionLog(productionContext, groupName, productionStage, type);
        ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(groupName);
        if (null == groupPlan) {
            if (ProductionStageEnum.FORMAL_STAGE == productionStage) {
                log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupNoGroupPlanLog(context, groupName, type));
            }
            if (ProductionStageEnum.SIMULATE_STAGE == productionStage) {
                log.info(TbrSimulateProductionLogRecorder.addProductionContinueGroupNoGroupPlanLog(context, groupName, type));
            }
            return;
        }
        Set<String> allocationCxMachineCodeSet = groupPlan.getAllocationCxMachineCodeSet();
        if (CollectionUtils.isEmpty(allocationCxMachineCodeSet)) {
            if (ProductionStageEnum.FORMAL_STAGE == productionStage) {
                log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupNoAllocationCxMachineLog(context, groupName, type));
            }
            if (ProductionStageEnum.SIMULATE_STAGE == productionStage) {
                log.info(TbrSimulateProductionLogRecorder.addProductionContinueGroupNoAllocationCxMachineLog(context, groupName, type));
            }
            return;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap = cxContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
            if (ProductionStageEnum.FORMAL_STAGE == productionStage) {
                log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupNoContinueSkuLog(context, groupName, type));
            }
            if (ProductionStageEnum.SIMULATE_STAGE == productionStage) {
                log.info(TbrSimulateProductionLogRecorder.addProductionContinueGroupNoContinueSkuLog(context, groupName, type));
            }
            return;
        }
        //续作Sku
        if (ContinueTypeEnum.SAME_SKU == type) {
            CxContinueProductionHandler.productionContinueSku(productionContext, productionStage, groupPlan, continueSkuInfoMap);
            return;
        }
        Integer deadLineDay = groupPlan.getContinueSkuDeadLineDay(context);
        // 设置当前结构 剩余的每日硫化机台数 sandy+ 2026.3.22
        cxAddSkuProductionHandler.setRemainLhMachineCount(context, allGroupPlanInfo, groupName);
        //4.1 初始日产能限制信息，用于统计使用
        groupPlan.initMpDailyCapacityLimit(context);
        //同规格同花纹 or 共生胎同模具
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == type || ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == type) {
            CxContinueProductionHandler.productionContinueByType(context, productionStage, groupPlan, type, deadLineDay, continueSkuInfoMap, new HashSet<>(), new HashSet<>());
            //4.3 重新计算统计产能
            groupPlan.reCalcMpDailyCapacityLimit(context);
            return;
        }
    }

    /**
     * 增加开始排产日志
     *
     * @param productionContext 排产上下文
     * @param structureName     分组名
     * @param productionStage   阶段
     * @param continueType      类型
     */
    private void recordStartProductionLog(TbrProductionContext productionContext, String structureName, ProductionStageEnum productionStage, ContinueTypeEnum continueType) {
        if (ProductionStageEnum.FORMAL_STAGE == productionStage) {
            log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupSingleGroupLog(productionContext, structureName, continueType));
        }
        if (ProductionStageEnum.SIMULATE_STAGE == productionStage) {
            log.info(TbrSimulateProductionLogRecorder.addProductionContinueGroupSingleGroupLog(productionContext, structureName, continueType));
        }
    }
}
