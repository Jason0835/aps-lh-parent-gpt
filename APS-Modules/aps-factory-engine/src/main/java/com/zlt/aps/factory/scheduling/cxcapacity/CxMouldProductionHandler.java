package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxContinueProductInfoHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MouldShellBaseInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成型模具排产业务处理
 *
 * @author ZLT
 * @date 20251217
 */
@Slf4j
public class CxMouldProductionHandler {

    /**
     * 在机结构，进行模具排产
     * 1、先排续作规格
     * 1.1、续作SKU
     * 1.2、同规格同花纹
     * 1.3、换活字块-共生胎同模具
     * 2、收尾新增规格
     *
     * @param context        排产上下文
     * @param cxMachineCode  成型机台
     * @param productionPlan 排产计划信息
     * @param mouldInfoMap   模具关系信息
     * @param mouldShellMap  模壳信息
     */
    public static void continueGroupPlanMouldProduction(Context context, String cxMachineCode, CxMachineAllocationPlanHelper productionPlan, CxContinueInfoHelper cxContinueInfo, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        List<MonthPlanProductionRequirePlanVo> groupPlanData = productionPlan.getProductionPlanInfo().getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            //todo 记录日志
            return;
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionPlanList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionPlanList)) {
            //todo 记录日志
            return;
        }
        //先续作排产： 续作SKU ->同规格同花纹 -> 换活字块共生胎同模具
        Map<String, CxContinueProductInfoHelper> continueSkuMap = cxContinueInfo.getCxMachineGroup().get(cxMachineCode);
        if (!CollectionUtils.isEmpty(continueSkuMap)) {
            CxContinueSkuProductionHandler.productionContinue(context, cxMachineCode, hasProductionPlanList, continueSkuMap, productionPlan, mouldInfoMap, mouldShellMap);
        }
        //排产收尾新增规格


    }

}
