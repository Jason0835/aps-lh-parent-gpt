package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueProductInfoHelper;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在机结构，新增规格模具排产处理
 *
 * @author ZLT
 * @date 20251220
 */
@Slf4j
public class CxAddSkuProductionHandler {

    /**
     * 新增规格排产
     * 按优先级估算
     *
     * @param context            排产上下文
     * @param cxMachineCode      成型机台
     * @param productionPlanList 还需排产的计划
     * @param productionPlan     分组排产信息，包含分组名(TBR=结构名)、起始及理论收尾日期
     * @param mouldShellMap      模壳信息
     */
    public static void productionAddSku(Context context, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //获取最先收尾的硫化组
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            //todo 记录日志
            return;
        }
        CxLhProductionHelper cxLhGroup = cxMachineInfo.getEarliestConclusionLhGroup();
        Integer startDay = cxLhGroup.getProductionDay();
        //成型分配的排产范围起始日~分组收尾日
        Integer endDay = productionPlan.getEndDay();
        if (startDay >= endDay) {
            //todo 记录日志
            return;
        }
        //获取优先级最高的Sku信息
        String materialDesc = getSelectedAddSku(productionContext, startDay, endDay, productionPlanList);
        if(StringUtils.isBlank(materialDesc)){
            //todo 记录日志
            return ;
        }
        //选择模具
        List<ProductionMouldInfoVo> doubleMouldList = productionContext.selectedDoubleMouldByRange(materialDesc, startDay, endDay);
        //计算需要排产的量
        Long sumProductionQty = BigDecimal.ZERO.longValue();
//        List<MonthPlanProductionRequirePlanVo> selectedPlanList = productionPlanList.stream().filter()

        //开始排产


    }

    private static String getSelectedAddSku(TbrProductionContext productionContext, Integer startDay, Integer endDay, List<MonthPlanProductionRequirePlanVo> productionPlanList) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return "";
        }
        Set<String> allMaterialDescSet = productionPlanList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> enableMaterialDescSet = productionContext.getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, allMaterialDescSet, startDay, endDay);
        if (CollectionUtils.isEmpty(enableMaterialDescSet)) {
            return "";
        }
        List<MonthPlanProductionRequirePlanVo> enablePlanList = productionPlanList.stream().filter(plan -> enableMaterialDescSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enablePlanList)) {
            return "";
        }
        /**
         * 供应链标注"优先字样"最先排产
         * 先排产高优先级，在排产其它净需求
         * 模具产能约束 --> 库销比低优先 -> 小于50条 -> 净需求大
         * 模具产能约束，则取排产量小的
         *
         */
        List<MonthPlanProductionRequirePlanVo> hasPrioritizeList = enablePlanList.stream().filter(plan -> YesOrNoEnum.YES.getCode().equals(plan.getIsPrioritize())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasPrioritizeList)) {
            hasPrioritizeList = enablePlanList;
        }
        //高优先级优先
        List<MonthPlanProductionRequirePlanVo> heightRequireList = hasPrioritizeList.stream().filter(plan -> plan.getHeightProductionQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(heightRequireList)) {
            heightRequireList = hasPrioritizeList;
        }
        //库销比低的优先
        Double minInventorySalesRatio = heightRequireList.stream().mapToDouble(MonthPlanProductionRequirePlanVo::getInventorySalesRatio).min().getAsDouble();
        List<MonthPlanProductionRequirePlanVo> minInventorySalesRatioList = heightRequireList.stream().filter(plan -> minInventorySalesRatio.equals(plan.getInventorySalesRatio())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(minInventorySalesRatioList)) {
            minInventorySalesRatioList = heightRequireList;
        }
        //小于50条的优先
        List<MonthPlanProductionRequirePlanVo> lessMinQtyList = minInventorySalesRatioList.stream().filter(plan -> plan.isLess(Long.valueOf(productionContext.getParamConfiguration().getMinQty()))).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lessMinQtyList)) {
            lessMinQtyList = minInventorySalesRatioList;
        }
        //量大优先
        lessMinQtyList.sort(Comparator.comparing(MonthPlanProductionRequirePlanVo::getVirtualProductionQty, Comparator.reverseOrder()));
        String materialDesc = lessMinQtyList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
        //是否共用模具受限？--最后两副
        Set<String> limitShareMouldSet = productionContext.getLimitShareMouldOtherSku(materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(limitShareMouldSet)) {
            return materialDesc;
        }
        List<MonthPlanProductionRequirePlanVo> limitShareList = enablePlanList.stream().filter(plan -> limitShareMouldSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(limitShareList)) {
            return materialDesc;
        }
        //加入自己
        enablePlanList.forEach(plan -> {
            if (materialDesc.equals(plan.getMaterialDesc())) {
                limitShareList.add(plan);
            }
        });
        Map<String, Long> limitGroup = new HashMap<>();
        Map<String, List<MonthPlanProductionRequirePlanVo>> limitShareMap = limitShareList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        limitShareMap.forEach((limitMaterial, planList) -> limitGroup.put(limitMaterial, planList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getVirtualProductionQty).sum()));
        Optional<Map.Entry<String, Long>> minEntry = limitGroup.entrySet().stream().min(Map.Entry.comparingByValue());
        return minEntry.get().getKey();
    }

}
