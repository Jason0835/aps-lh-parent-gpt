package com.zlt.aps.factory.scheduling.matching;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.factory.domain.dto.LhProductionQtyHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.ProductionQtyModelEnum;
import com.zlt.aps.factory.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.cxcapacity.SkuNeedProductionInfo;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 搭配排产处理类
 *
 * @author hak
 */
public class MatchingProductionHandler {
	
	/**
	 * 搭配排产（调整入口）
	 * @param planList
	 */
	public static void matchingProduction(List<FactoryMonthPlanProdFinal> planList) {
		// TODO 构建参数
		TbrProductionContext productionContext = new TbrProductionContext();
		Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap = new HashMap<>();
		List<MonthPlanStructureLhRatioVo> structureLhRatioList = new ArrayList<>();
		matchingProduction(productionContext, estimateGroupCxAllocationMap, structureLhRatioList);
		// TODO 保存结果
		
	}
	
    /**
     * 搭配排产
     *
     * @param context
     * @param estimateGroupCxAllocationMap
     * @param structureLhRatioList
     */
    public static void matchingProduction(Context context,
                                          Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap,
                                          List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;

        // 按结构分组的模具排产信息
        Map<String, List<CxMouldDayProductionHelper>> mouldProductionGroup = buildMouldProductionGroup(
                productionContext);
        // 按结构分组的成型排产信息
        Map<String, List<CxMachineAllocationPlanHelper>> cxAllocationPlanGroup = buildCxAllocationPlanGroup(
                productionContext);
        // 机构模具配比
        Map<String, MonthPlanStructureLhRatioVo> structureLhRatioMap = structureLhRatioList.stream().collect(
                Collectors.toMap(MonthPlanStructureLhRatioVo::getStructureName, Function.identity(), (r1, r2) -> r1));

        // 遍历所有结构
        for (Entry<String, ProductionPlanGroupInfo> entry : estimateGroupCxAllocationMap.entrySet()) {
            String structureName = entry.getKey(); // 分组名称（TBR：结构）
            ProductionPlanGroupInfo groupInfo = entry.getValue();
            List<CxMouldDayProductionHelper> mouldDayProductionList = mouldProductionGroup.get(structureName);
            List<CxMachineAllocationPlanHelper> cxAllocationPlanList = cxAllocationPlanGroup.get(structureName);
            if (CollectionUtils.isEmpty(mouldDayProductionList) || CollectionUtils.isEmpty(cxAllocationPlanList)) {
                continue; // 成型或模具排程任意一个找不到数据都要跳过这个结构
            }

            // 按天分组硫化排产
            Map<Integer, List<CxMouldDayProductionHelper>> dayModPlanMap = mouldDayProductionList.stream()
                    .collect(Collectors.groupingBy(CxMouldDayProductionHelper::getProductionDate));
            // 按天分组成型排产
//			Map<Integer, List<CxMachineAllocationPlanHelper>> dayCxPlanMap = cxAllocationPlanList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getAllocationDay));;
            // 取出最早成型硫化配比不足的日期
            TreeMap<Integer, Integer> dayPlanMap = new TreeMap<>(); // 本结构按天汇总的日排产量，需要按key(日期)排序
            for (Entry<Integer, List<CxMouldDayProductionHelper>> modPlan : dayModPlanMap.entrySet()) {
                Integer day = modPlan.getKey();
                List<CxMouldDayProductionHelper> planList = modPlan.getValue();
                Integer planQty = planList.stream()
                        .collect(Collectors.summingInt(CxMouldDayProductionHelper::getProductionQty));
                dayPlanMap.put(day, planQty);
            }

            // 根据sku\模具等因素取定额数据
            Integer quota = groupInfo.getMinLhDayCapacityQty(); // 每日最小的日硫化产能
            Integer rate = Optional.ofNullable(structureLhRatioMap.get(structureName))
                    .map(MonthPlanStructureLhRatioVo::getLhMachineMaxQty).orElse(8);// 取出配比，默认是8
            Integer totalQuota = BigDecimal.valueOf(quota).multiply(BigDecimal.valueOf(rate)).intValue(); // 最大可排产数

            // 计算可搭配排产时间段
            Integer startDay = dayPlanMap.firstKey(); // 搭配起始日期，初始是结构第一天上机日期
            for (Entry<Integer, Integer> dayPlanEntry : dayPlanMap.entrySet()) {
                startDay = dayPlanEntry.getKey();
                Integer plan = dayPlanEntry.getValue();
                if (plan < totalQuota) { // 计划小于定额，说明有空余，则从当天开始尝试搭配排产，TODO 还要判断满足其他条件
                    break;
                }
            }
            Integer endDay = dayPlanMap.lastKey(); // 结束日期，默认是结构收尾日期
            // 从开始日期到结束日期，检查每一天是否满足配上机的约束条件
            for (int day = startDay; day <= endDay; day++) {
                // 只要有一天不满足条件，直接将结束日期提前到上一天
                Integer plan = dayPlanMap.get(day);
                if (plan >= totalQuota) {
                    endDay = day - 1;
                    break;
                }
            }
            if (startDay.compareTo(endDay) == 0) { // 如果开始时间=结束时间，说明该结构全部满产，直接看下一个结构
                continue;
            }

            // 循环取结构向下所有符合搭配生产条件的sku进行搭配排产
            do {
                // 取出收尾sku的需求计划
                List<MonthPlanProductionRequirePlanVo> productionPlanList = groupInfo.getGroupPlanData();
                // 获取优先级最高的Sku信息
                String materialDesc = getSelectedAddSku(productionContext, startDay, endDay, productionPlanList);
                if (StringUtils.isBlank(materialDesc)) {
                    // todo 记录日志
                    break;
                }
                // 计算需要排产的量
                SkuNeedProductionInfo needProductionInfo = getNeedProductionQty(productionPlanList, materialDesc);
                if (null == needProductionInfo) {
                    // todo 记录日志
                    return;
                }
                // 排产量
                Integer sumProductionQty = needProductionInfo.getSumNeedProductionQty();
                Integer dayMaxProductionQty = needProductionInfo.getDayMaxProductionQty();
                Integer realSumProductionQty = 0; // 已排产量
                Set<String> cxMachineInfoSet = groupInfo.getAllocationCxMachineCodeSet();
                LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(groupInfo, cxMachineInfoSet,
                        null, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
                // 选择模具
                List<ProductionMouldInfoVo> doubleMouldList = productionContext.selectedDoubleMouldByRange(materialDesc,
                        startDay, endDay);
                // 开始排产
                CxLhMouldProductionCalculator.lhProductionByGroupHandler(context, lhProductionQtyHelper, startDay,
                        endDay, doubleMouldList, needProductionInfo.getNeedProductionList());
            } while (true);
        }
    }

    /**
     * 构建成型组
     *
     * @param productionContext
     * @return
     */
    private static Map<String, List<CxMachineAllocationPlanHelper>> buildCxAllocationPlanGroup(
            TbrProductionContext productionContext) {
        Map<String, List<CxMachineAllocationPlanHelper>> cxAllocationPlanGroup = new HashMap<>();
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer()
                .getCxMachineBaseInfo(); // 成型排产结果
        for (CxMachineBaseInfoVo cxInfo : cxMachineBaseInfo.values()) {
            for (CxMachineAllocationPlanHelper allocationPlan : cxInfo.getAllocationList()) {
                String structureName = allocationPlan.getProductionPlanInfo().getGroupName(); // 结构
                List<CxMachineAllocationPlanHelper> groupList = cxAllocationPlanGroup.get(structureName);
                if (groupList == null) {
                    groupList = new ArrayList<>();
                    cxAllocationPlanGroup.put(structureName, groupList);
                }
                groupList.add(allocationPlan);
            }
        }
        return cxAllocationPlanGroup;
    }

    /**
     * 构建模具组
     *
     * @param productionContext
     * @return
     */
    private static Map<String, List<CxMouldDayProductionHelper>> buildMouldProductionGroup(
            TbrProductionContext productionContext) {
        Map<String, List<CxMouldDayProductionHelper>> mouldProductionGroup = new HashMap<>();
        Map<String, ProductionMouldInfoVo> mouldProductionList = productionContext.getBaseDataContainer()
                .getMouldInfoMap(); // 模具排产结果
        for (ProductionMouldInfoVo production : mouldProductionList.values()) {
            for (List<CxMouldDayProductionHelper> dayProduction : production.getDayProductionInfo().values()) {
                dayProduction.forEach(day -> {
                    String structureName = day.getStructureName(); // 结构
                    List<CxMouldDayProductionHelper> groupList = mouldProductionGroup.get(structureName);
                    if (groupList == null) {
                        groupList = new ArrayList<>();
                        mouldProductionGroup.put(structureName, groupList);
                    }
                    groupList.add(day);
                });
            }
        }
        return mouldProductionGroup;
    }

    /**
     * 从排产计划中挑选出在startDay~endDay能进行排产的sku计划
     * 1、挑选在startDay~endDay还可进行双模排产的sku，需要有未排常规储备量 4、库销比低的优先 5、成品库存超6个月的少的优先
     * 6、如果挑选的sku与其它sku是共用模具，且是存在其它sku最后两副模具(即模具受限)则，需排产量小的优先
     *
     * @param productionContext  排产上下文
     * @param startDay           排产开始日
     * @param endDay             排产结束日
     * @param productionPlanList 排产计划
     * @return
     */
    private static String getSelectedAddSku(TbrProductionContext productionContext, Integer startDay, Integer endDay,
                                            List<MonthPlanProductionRequirePlanVo> productionPlanList) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return "";
        }
        // 提取所有sku的物料描述
        Set<String> allMaterialDescSet = productionPlanList.stream()
                .map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> enableMaterialDescSet = productionContext
                .getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, allMaterialDescSet, startDay, endDay);
        if (CollectionUtils.isEmpty(enableMaterialDescSet)) {
            return "";
        }
        List<MonthPlanProductionRequirePlanVo> enablePlanList = productionPlanList.stream()
                .filter(plan -> enableMaterialDescSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enablePlanList)) {
            return "";
        }
        // 只看有常规储备的sku
        List<MonthPlanProductionRequirePlanVo> hasReserveQtyPlanList = enablePlanList.stream()
                .filter(s -> s.getConventionReserveQty() > 0).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasReserveQtyPlanList)) {
            return "";
        }
        // 库销比低的优先
        Double minInventorySalesRatio = hasReserveQtyPlanList.stream()
                .mapToDouble(MonthPlanProductionRequirePlanVo::getInventorySalesRatio).min().getAsDouble();
        List<MonthPlanProductionRequirePlanVo> minInventorySalesRatioList = hasReserveQtyPlanList.stream()
                .filter(plan -> minInventorySalesRatio.equals(plan.getInventorySalesRatio()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(minInventorySalesRatioList)) {
            minInventorySalesRatioList = hasReserveQtyPlanList;
        }
        // 成品库存超6个月的少的优先
        minInventorySalesRatioList.sort((s1, s2) -> {
            Integer stock1 = productionContext.getOverSixMonthStockMap().getOrDefault((s1.getMaterialDesc()), 0);
            Integer stock2 = productionContext.getOverSixMonthStockMap().getOrDefault((s2.getMaterialDesc()), 0);
            return stock1.compareTo(stock2);
        });
        String materialDesc = CollectionUtils.firstElement(minInventorySalesRatioList).getMaterialDesc();
        Set<String> limitShareMouldSet = productionContext.getLimitShareMouldOtherSku(materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(limitShareMouldSet)) {
            return materialDesc;
        }
        List<MonthPlanProductionRequirePlanVo> limitShareList = enablePlanList.stream()
                .filter(plan -> limitShareMouldSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(limitShareList)) {
            return materialDesc;
        }
        // 加入自己
        enablePlanList.forEach(plan -> {
            if (materialDesc.equals(plan.getMaterialDesc())) {
                limitShareList.add(plan);
            }
        });
        Map<String, Long> limitGroup = new HashMap<>();
        Map<String, List<MonthPlanProductionRequirePlanVo>> limitShareMap = limitShareList.stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        limitShareMap.forEach((limitMaterial, planList) -> limitGroup.put(limitMaterial,
                planList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getVirtualProductionQty).sum()));
        Optional<Map.Entry<String, Long>> minEntry = limitGroup.entrySet().stream().min(Map.Entry.comparingByValue());
        return minEntry.get().getKey();
    }

    /**
     * 从分组计划中获取有常规储备的Sku(selectedMaterialDesc)
     *
     * @param productionPlanList   分组排产计划(TBR-结构名)
     * @param selectedMaterialDesc 选中的Sku
     * @return
     */
    private static SkuNeedProductionInfo getNeedProductionQty(List<MonthPlanProductionRequirePlanVo> productionPlanList,
                                                              String selectedMaterialDesc) {
        if (CollectionUtils.isEmpty(productionPlanList) || StringUtils.isBlank(selectedMaterialDesc)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> selectedPlanList = productionPlanList.stream()
                .filter(plan -> plan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(selectedPlanList)) {
            return null;
        }
        // 是否有常规储备排产量
        List<MonthPlanProductionRequirePlanVo> heightList = selectedPlanList.stream()
                .filter(plan -> plan.getConventionReserveQty() > BigDecimal.ZERO.longValue())
                .collect(Collectors.toList());
        // 高优先级优先
        if (CollectionUtils.isEmpty(heightList)) {
            return null;
        }
        return new SkuNeedProductionInfo(ProductionQtyModelEnum.RESERVE_QTY, heightList);
    }
}
