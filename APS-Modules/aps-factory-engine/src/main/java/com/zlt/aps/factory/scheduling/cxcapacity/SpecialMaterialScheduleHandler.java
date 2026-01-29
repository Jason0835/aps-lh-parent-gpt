package com.zlt.aps.factory.scheduling.cxcapacity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;

/**
 * 结构排产特殊材料控制服务
 *
 */
@Service
public class SpecialMaterialScheduleHandler {
    /**
     * 特殊材料排程
     * <p/>
     * 详细设计文档：03-APS详细设计-月度生产计划 <br/>
     * 详细设计模块：月度生产计划 <br/>
     * 详细设计点：产品品类【生成】-5 根据需求计划中的SKU及结构BOM、特殊材料配置，识别使用特殊材料的SKU
     * <p/>
     * 检查规格是否符合特殊材料排产要求<br/>
     * 排除掉需要特殊材料，且特殊材料库存不足的规格
     * 
     * @param productionContext 上下文
     */
    public void specialMaterialSchedule(TbrProductionContext productionContext) {
        Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap = productionContext.getBaseDataContainer()
                .getEmbryoSpecialMaterialInfoMap(); // 胎胚与特殊材料对应关系清单
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = productionContext
                .getSpecialMaterialInfoMap(); // 特殊材料库存列表
        if (embryoSpecialMaterialInfoMap == null || specialMaterialInfoMap == null) {
            return;
        }

        // 1、检查每种材料的总消耗量，是否有库存不足的，排产量扣减到库存量
        this.deductUnAlloceQty(productionContext, embryoSpecialMaterialInfoMap, specialMaterialInfoMap);

        // 2、检查现有排产量，是否符合整卷条件
        // 2.1、计算超出整卷的计划量：mod(计划量*单胎消耗量, 标准长度) / 单胎消耗量
        // 2.2、计算单台日产：硫化机台数*硫化最大日产
        // 2.3、判断超出量小于单台日产，舍弃；
        this.deductWholeRollLength(productionContext, embryoSpecialMaterialInfoMap, specialMaterialInfoMap);
    }

    /**
     * 检查每种材料的总消耗量，按标准长度对计划量取整
     * 
     * @param productionContext            上下文
     * @param embryoSpecialMaterialInfoMap 胎胚与特殊材料对应关系清单
     * @param specialMaterialInfoMap       特殊材料库存列表
     */
    private void deductWholeRollLength(TbrProductionContext productionContext,
                                       Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap,
                                       Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap) {
        Map<String, List<MonthPlanProductionRequirePlanVo>> planMaterialGroupMap = this
                .planMaterialGroupMap(productionContext, embryoSpecialMaterialInfoMap); // 需求计划根据特殊需材料分组
        Set<String> handleMaterial = new HashSet<>(); // 记录已处理过的原材料，一种规格只针对一种原材料做整卷处理，否则会互相影响

        // 按指定顺序遍历特殊材料
        List<String> specialMaterialList = planMaterialGroupMap.keySet().stream()
                .sorted(Comparator.comparing(Function.identity(), Comparator.reverseOrder())) // TODO 按编码倒序排序
                .collect(Collectors.toList());
        for (String specialMaterialCode : specialMaterialList) {
            BigDecimal singleMachineDayCapacity = BigDecimalUtils.valueOf(300); // TODO 计算单台日产：硫化机台数*硫化最大日产
            List<MonthPlanProductionRequirePlanVo> planMaterialGroupList = this.findPlanMaterialGroup(
                    planMaterialGroupMap.get(specialMaterialCode), embryoSpecialMaterialInfoMap, handleMaterial); // 使用本特殊材料的需求计划
            if (CollectionUtils.isEmpty(planMaterialGroupList)) {
                continue;
            }
            BigDecimal totalConsumeQty = this.statisticsTotalConsumeQty(specialMaterialCode, planMaterialGroupList,
                    embryoSpecialMaterialInfoMap); // 统计原材料的总消耗量
            if (totalConsumeQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 计算按标准长度取整后的待分配量，如果需要舍弃，则从需求计划扣减
            BigDecimal unAllocateQty = this.caculateWholeRollUnAllocateQty(specialMaterialCode, totalConsumeQty,
                    singleMachineDayCapacity, planMaterialGroupList, embryoSpecialMaterialInfoMap,
                    specialMaterialInfoMap);
            for (MonthPlanProductionRequirePlanVo plan : planMaterialGroupList) { // TODO 扣减是否有顺序
                if (unAllocateQty.compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
                BigDecimal productionQty = BigDecimalUtils.valueOf(plan.getProductionQty()); // 需排产量
                BigDecimal allocatedQty = BigDecimalUtils.least(unAllocateQty, productionQty); // 分配量，取待分配两和需排产量的较小值
                // 更新数据，扣减分配量
                unAllocateQty = unAllocateQty.subtract(allocatedQty);
                plan.setProductionQty(productionQty.subtract(allocatedQty).intValue());
            }
            handleMaterial.add(specialMaterialCode); // 记录已经处理的特殊材料
        }
    }

    /**
     * 对特殊材料的总消耗量按标准长度做整卷取整，返回超出整卷的量作为待分配量给对应需求计划扣减<br/>
     * 超出部分，小于300条（50*6台）舍弃
     * 
     * @param totalConsumeQty            总消耗量
     * @param singleMachineDayCapacity   单台日产
     * @param specialMaterialStandardMap 原材料库存
     * @return
     */
    private BigDecimal caculateWholeRollUnAllocateQty(String specialMaterialCode, BigDecimal totalConsumeQty,
                                                      BigDecimal singleMachineDayCapacity,
                                                      List<MonthPlanProductionRequirePlanVo> planMaterialGroupList,
                                                      Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap,
                                                      Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap) {
        Map<Long, SpecialMaterialInfoVo> specialMaterialStandardMap = specialMaterialInfoMap.get(specialMaterialCode); // 原材料库存
        if (CollectionUtils.isEmpty(specialMaterialStandardMap)) {
            return BigDecimal.ZERO;
        }
        SpecialMaterialInfoVo specialMaterialInfoVo;
        if (specialMaterialStandardMap.size() == 1) {
            specialMaterialInfoVo = specialMaterialStandardMap.values().stream().findAny().orElse(null);
        } else { // TODO 如果有多种标准长，需要判断哪个标准长比较合适
            specialMaterialInfoVo = specialMaterialStandardMap.values().stream().findAny().orElse(null);
        }
        BigDecimal standardLength = BigDecimalUtils.valueOf(specialMaterialInfoVo.getStandardLength()); // 标准长
        BigDecimal remainderQty = totalConsumeQty.remainder(standardLength); // 超出整卷的的量
        if (remainderQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        MonthPlanProductionRequirePlanVo plan = CollectionUtils.firstElement(planMaterialGroupList);
        BigDecimal singleUnitConsumeQty = embryoSpecialMaterialInfoMap.get(plan.getEmbryoCode())
                .get(specialMaterialCode);
        BigDecimal singleMachineDayConsumeQty = singleMachineDayCapacity.multiply(singleUnitConsumeQty); // 单台日产换算成消耗量
        if (remainderQty.compareTo(singleMachineDayConsumeQty) >= 0) {
            return BigDecimal.ZERO;
        }
        return remainderQty; // 待分配量，需要顺序从各规格扣减
    }

    /**
     * 统计原材料的总消耗量
     * 
     * @param specialMaterialCode          原材料编码
     * @param planMaterialGroupList        使用本特殊材料的需求计划
     * @param embryoSpecialMaterialInfoMap 胎胚与特殊材料对应关系清单
     * @return
     */
    private BigDecimal statisticsTotalConsumeQty(String specialMaterialCode,
                                                 List<MonthPlanProductionRequirePlanVo> planMaterialGroupList,
                                                 Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap) {
        BigDecimal totalConsumeQty = planMaterialGroupList.stream().map(plan -> {
            BigDecimal unitConsume = BigDecimal.ZERO; // 单胎消耗量
            Map<String, BigDecimal> embryoMaterialInfoMap = embryoSpecialMaterialInfoMap.get(plan.getEmbryoCode());
            if (embryoMaterialInfoMap != null) {
                unitConsume = embryoMaterialInfoMap.getOrDefault(specialMaterialCode, BigDecimal.ZERO);
            }
            if (unitConsume.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO; // 单耗为0的忽略这个材料（正常不存在这个场景，仅防止非法数据引起报错）
            }
            return BigDecimalUtils.multiply(BigDecimalUtils.valueOf(plan.getProductionQty()), unitConsume);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalConsumeQty;
    }

    /**
     * 过滤需要处理的需求计划数据<br/>
     * 1、过滤掉包含已处理特殊材料的规格。一种规格只针对一种原材料做整卷处理，否则会互相影响<br/>
     * 2、过滤掉需求量为0的记录
     * 
     * @param planList        需求计划列表
     * @param materialInfoMap 胎胚与特殊材料对应关系清单
     * @param handleMaterial  已处理特殊材料清单
     * @return
     */
    private List<MonthPlanProductionRequirePlanVo> findPlanMaterialGroup(List<MonthPlanProductionRequirePlanVo> planList,
                                                                         Map<String, Map<String, BigDecimal>> materialInfoMap,
                                                                         Set<String> handleMaterial) {
        return planList.stream().filter(plan -> {
            if (Optional.ofNullable(plan.getProductionQty()).orElse(0) <= 0) { // 过滤掉需求量为0的记录
                return false;
            }
            Map<String, BigDecimal> embryoMaterialInfoMap = materialInfoMap.get(plan.getEmbryoCode());
            if (embryoMaterialInfoMap == null) {
                return false;
            }
            // 过滤包含已处理特殊材料的规格（一个胎胚同时使用多种特殊材料的场景）
            return embryoMaterialInfoMap.keySet().stream()
                    .noneMatch(specialMateriCode -> handleMaterial.contains(specialMateriCode));
        }).collect(Collectors.toList());
    }

    /**
     * 需求计划根据特殊需材料分组
     * 
     * @param productionContext
     * @param embryoSpecialMaterialInfoMap
     * @return
     */
    private Map<String, List<MonthPlanProductionRequirePlanVo>> planMaterialGroupMap(TbrProductionContext productionContext,
                                                                                     Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap) {
        Map<String, List<MonthPlanProductionRequirePlanVo>> planMaterialGroupMap = new HashMap<>();
        for (MonthPlanProductionRequirePlanVo plan : productionContext.getAllProductionPlan().values()) {
            Map<String, BigDecimal> embryoMaterialInfoMap = embryoSpecialMaterialInfoMap.get(plan.getEmbryoCode()); // 胎胚的特殊材料用量清单
            if (embryoMaterialInfoMap == null) {
                continue;
            }
            embryoMaterialInfoMap.keySet().forEach(spaecialMaterialCode -> {
                List<MonthPlanProductionRequirePlanVo> planList = planMaterialGroupMap.get(spaecialMaterialCode);
                if (planList == null) {
                    planList = new ArrayList<>();
                    planMaterialGroupMap.put(spaecialMaterialCode, planList);
                }
                planList.add(plan);
            });
        }
        return planMaterialGroupMap;
    }

    /**
     * 检查每种材料的总消耗量，是否有库存不足的，排产量扣减到库存量
     * 
     * @param productionContext            上下文
     * @param embryoSpecialMaterialInfoMap 胎胚与特殊材料列表
     * @param specialMaterialInfoMap       特殊材料清单
     */
    private void deductUnAlloceQty(TbrProductionContext productionContext,
                                   Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap,
                                   Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap) {
//        // 1、遍历成型机
//        productionContext.getBaseDataContainer().getCxMachineBaseInfo().values().forEach(cxMachine -> {
//            // 2、遍历结构排产
//            for (CxMachineAllocationPlanHelper cxPlan : cxMachine.getAllocationList()) {
        BigDecimal groupUnAllocatedQty = BigDecimal.ZERO; // 结构未分配量
        // 3、遍历分配的排产量
//                for (MonthPlanProductionRequirePlanVo plan : cxPlan.getRealProductionPlanList()) {
        // 遍历各需求计划排产量
        for (MonthPlanProductionRequirePlanVo plan : productionContext.getAllProductionPlan().values()) {
            Map<String, BigDecimal> embryoMaterialInfoMap = embryoSpecialMaterialInfoMap.get(plan.getEmbryoCode()); // 胎胚的特殊材料用量清单
            if (embryoMaterialInfoMap == null) {
                continue;
            }
            plan.setIsSpecialMaterials(YesOrNoEnum.YES.getCode()); // 需求计划打上含有特殊材料的标记
            BigDecimal unAllocatedQty = this.updateProductQtyByStock(plan, specialMaterialInfoMap,
                    embryoMaterialInfoMap); // 检查需求计划每种材料的总消耗量是否有足够库存，返回因库存不足无法排产的规格
            groupUnAllocatedQty = groupUnAllocatedQty.add(unAllocatedQty);
        }
//                if (groupUnAllocatedQty.compareTo(BigDecimal.ZERO) < 0) {
//                    continue;
//                }
        // 4、如果有扣减排产量，需要检查是否需要提前收尾
//                ProductionPlanGroupInfo groupInfo = cxPlan.getProductionPlanInfo();
//                Integer sumPlanQty = groupInfo.getSumPlanQty(); // 结构总排产量
//            }
//        });
    }

    /**
     * 根据特殊材料库存更新排产量
     * 
     * @param plan                   需求计划
     * @param specialMaterialInfoMap 特殊材料清单
     * @param embryoMaterialInfoMap  胎胚的特殊材料用量清单
     * @return 未分配量
     */
    private BigDecimal updateProductQtyByStock(MonthPlanProductionRequirePlanVo plan,
                                               Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap,
                                               Map<String, BigDecimal> embryoMaterialInfoMap) {
        Integer productionQty = Optional.ofNullable(plan.getProductionQty()).orElse(0); // 需生产量
        BigDecimal maxUnAllocatedQty = BigDecimal.ZERO; // 总未分配量，取各特殊材料库存缺口的最大值
        for (Entry<String, BigDecimal> entry : embryoMaterialInfoMap.entrySet()) {
            String specialMaterialCode = entry.getKey();
            Map<Long, SpecialMaterialInfoVo> specialMaterialStandardMap = specialMaterialInfoMap
                    .get(specialMaterialCode); // 不同标准长的库存信息
            BigDecimal unitConsume = Optional.ofNullable(entry.getValue()).orElse(BigDecimal.ZERO); // 单胎消耗量
            if (unitConsume.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 单耗为0的忽略这个材料（正常不存在这个场景，仅防止非法数据引起报错）
            }
            if (CollectionUtils.isEmpty(specialMaterialStandardMap)) {
                maxUnAllocatedQty = BigDecimalUtils.valueOf(productionQty); // 任意一个材料没有库存，该规格无法生产
                break;
            }
            // 计算总消耗量
            BigDecimal totalConsume = BigDecimalUtils.multiply(productionQty, unitConsume, false);
            BigDecimal unAllocatedMaterialQty = totalConsume; // 未分配原材料量
            for (SpecialMaterialInfoVo specialMaterialInfo : specialMaterialStandardMap.values()) {
                BigDecimal stock = BigDecimalUtils.valueOf(specialMaterialInfo.getStock()); // 库存量
                BigDecimal sumProductionQty = BigDecimalUtils.valueOf(specialMaterialInfo.getSumProductionQty()); // 已排量
                BigDecimal availableStock = stock.subtract(sumProductionQty); // 有效库存 = 库存 - 已排
                BigDecimal allocatedQty = BigDecimalUtils.least(unAllocatedMaterialQty, availableStock); // 分配量，取待分配两和库存的较小值
                // 分配库存
                unAllocatedMaterialQty = unAllocatedMaterialQty.subtract(allocatedQty); // 更新未分配量
                specialMaterialInfo.setSumProductionQty(sumProductionQty.add(allocatedQty).longValue());// 更新已排量
            }
            // 如果还有未分配量，说明库存不足，需要扣减需排产量
            if (unAllocatedMaterialQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unAllocatedQty = BigDecimalUtils.ceil(unAllocatedMaterialQty, unitConsume); // 换算成未分配轮胎量
                maxUnAllocatedQty = BigDecimalUtils.greatest(maxUnAllocatedQty, unAllocatedQty); // 未分配量按最大的算
            }
        }
        plan.setProductionQty(BigDecimalUtils.sub(productionQty, maxUnAllocatedQty).intValue());
        return maxUnAllocatedQty;
    }

}
