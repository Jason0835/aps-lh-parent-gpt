package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.constant.Constant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.utils.MouldCapacityAllocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * （5）特别场景：在排产时，我们的原则是续作优先，
 * 若共用模具情况下，续作高优先级的已没有，存在续作中优先级的，但有高优先级。这时，续作中优先级的要先排？
 *  处理方案：首先，需要算一下模具的产能，如果能把高优先级+续作的中优先级全部能包过来，那么就续作优先；
 *  如果不能包过来，就需要把中优先级中途下机，下机的时间点是，剩余的模具产能，正好能把高优先级产完。
 * @author Yelq
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdjustContinueSkuProductionQtyHandler {

  private static final int DOUBLE_MOULD_COUNT = 2;
  private static final Predicate<MonthPlanProductionRequirePlanVo> HAS_PRODUCTION_FILTER =
      MonthPlanProductionRequirePlanVo::hasProduction;
  private static final Predicate<MonthPlanProductionRequirePlanVo> HAS_HEIGHT_PRODUCTION_FILTER =
      vo -> vo.getOriginHeightProductionQty() != null;
  private static final Predicate<MonthPlanProductionRequirePlanVo> HAS_PRODUCTION_QTY_FILTER =
      vo -> vo.getOriginProductionQty() != null;

  private final MouldCapacityAllocator mouldCapacityAllocator;

  /**
   * 调整续作SKU排产量
   *
   * <p>调整条件：
   * 1. 获取分配天数最长的续作结构成型产能分配
   * 2. 模具受限：续作SKU可用续作模具只有两幅
   * 3. 续作SKU的SUM(高优先级排产量)=0, SUM(可排产量)>0
   * 4. 结构向下，（同规格同花纹+共生胎同模具）可用模具也是续作SKU的那两幅模具，并且高优先级排产量>0
   * 5. productionQty = 续作SKU的SUM(可排产量)+SUM(同规格同花纹+共生胎同模具高优先级排产量)
   * 6. 计算续作SKU可用的两幅模具总产能 totalMouldCapacity
   * 7. 汇总比较totalMouldCapacity和productionQty,取最小值min(totalMouldCapacity,productionQty)作为续作SKU的可排产量
   *
   * @param allGroupPlanMap 所有分组计划映射
   * @param continueAllocationList 续作分配结果
   * @param allContinueMap 所有续作信息映射
   * @param productionContext 排产上下文
   */
  public void adjustContinueSkuProductionQty(Map<String, ProductionPlanGroupInfo> allGroupPlanMap,
                                             List<CxMachineAllocationPlanHelper> continueAllocationList,
                                             Map<String, CxContinueInfoHelper> allContinueMap,
                                             TbrProductionContext productionContext) {
    if (invalidInputs(allGroupPlanMap, continueAllocationList, allContinueMap)) {
      log.info("不合法输入参数,跳过调整");
      return;
    }

    Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap =
        groupByProductionPlan(continueAllocationList);

    allContinueMap.forEach((structureName, cxContinueInfo) ->
        processContinueInfo(structureName, cxContinueInfo, allGroupPlanMap,
            groupPlanMap, productionContext));
  }

  private boolean invalidInputs(Map<String,ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String,CxContinueInfoHelper> allContinueMap) {
    return CollectionUtils.isEmpty(allGroupPlanMap)
        || CollectionUtils.isEmpty(continueAllocationList)
        || CollectionUtils.isEmpty(allContinueMap);
  }

  private Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>>  groupByProductionPlan(List<CxMachineAllocationPlanHelper> continueAllocationList) {
    return continueAllocationList.stream().collect(Collectors.groupingBy(
            CxMachineAllocationPlanHelper::getProductionPlanInfo,
            Collectors.toList()));
  }

  private void processContinueInfo(String structureName,
                                   CxContinueInfoHelper cxContinueInfo,
                                   Map<String, ProductionPlanGroupInfo> allGroupPlanMap,
                                   Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap,
                                   TbrProductionContext productionContext) {
    // 1、获取续作结构分组
    ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
    if (groupPlanInfo == null) {
      return;
    }
    // 2、获取续作结构成型产能分配
    List<CxMachineAllocationPlanHelper> continueAllocations = groupPlanMap.get(groupPlanInfo);
    if (CollectionUtils.isEmpty(continueAllocations)) {
      return;
    }
    // 3. 获取最长分配天数
    int maxAllocationDay = calculateMaxAllocationDays(continueAllocations);
    if (maxAllocationDay == 0) {
      return;
    }
    // 4、获取续作结构内可排需求计划
    List<MonthPlanProductionRequirePlanVo> productionList = filterProductionPlans(groupPlanInfo.getGroupPlanData());
    if (CollectionUtils.isEmpty(productionList)) {
      return;
    }
    // 5、需求计划按照物料分组
    Map<String, List<MonthPlanProductionRequirePlanVo>> plansByMaterial = groupPlansByMaterialDesc(productionList);
    plansByMaterial.forEach((materialDesc, requirePlans) ->
        adjustMaterialProductionQty(materialDesc, requirePlans, plansByMaterial,
            maxAllocationDay, cxContinueInfo, productionContext));
  }

  /**
   * 获取可排需求计划数据
   * @param groupPlanData 续作结构分组
   * @return 可排需求计划数据
   */
  private List<MonthPlanProductionRequirePlanVo> filterProductionPlans(List<MonthPlanProductionRequirePlanVo> groupPlanData) {
    return groupPlanData.stream()
        .filter(HAS_PRODUCTION_FILTER)
        .collect(Collectors.toList());
  }

  /**
   *  根据物料分组需求计划列表
   * @param plans 需求计划
   * @return 物料分组需求计划列表
   */
  private Map<String, List<MonthPlanProductionRequirePlanVo>> groupPlansByMaterialDesc(List<MonthPlanProductionRequirePlanVo> plans) {
    return plans.stream().collect(Collectors.groupingBy(
            MonthPlanProductionRequirePlanVo::getMaterialDesc,
            Collectors.toList()));
  }

  /**
   *  调整续作SKU可排产量
   * @param materialDesc 续作SKU
   * @param requirePlans 需求计划列表
   * @param plansByMaterial 按照物料分组需求计划
   * @param maxAllocationDay 最大可分配天数
   * @param cxContinueInfo 续作信息
   * @param productionContext  排产上下文
   */
  private void adjustMaterialProductionQty(String materialDesc,
                                           List<MonthPlanProductionRequirePlanVo> requirePlans,
                                           Map<String, List<MonthPlanProductionRequirePlanVo>> plansByMaterial,
                                           int maxAllocationDay,
                                           CxContinueInfoHelper cxContinueInfo,
                                           TbrProductionContext productionContext) {

    requirePlans.forEach(item -> log.info("adjustMaterialProductionQty:materialDesc={},materialCode={}, heightProductionQty={},originHeightProductionQty={},productionQty={},originProductionQty={}",
        materialDesc,
        item.getMaterialCode(),
        item.getHeightProductionQty() ,
        item.getOriginHeightProductionQty(),
        item.getProductionQty(),
        item.getOriginProductionQty()));
    log.info("adjustMaterialProductionQty: materialDesc={},key={},maxAllocationDay={}",materialDesc, plansByMaterial.keySet(),maxAllocationDay);
    // 前置条件检查
    if (!validateAdjustmentConditions(materialDesc, requirePlans, productionContext)) {
      log.info("前置条件检查不通过，无需调整: materialDesc={}", materialDesc);
      return;
    }
    // 计算调整相关的高优先级产量
    int adjustHeightProductionQty = calculateAdjustHeightProductionQty(materialDesc, plansByMaterial, productionContext);
    if (adjustHeightProductionQty == 0) {
      log.info("计算调整相关的高优先级产量=0，无需调整: materialDesc={}, adjustHeightProductionQty={}", materialDesc, adjustHeightProductionQty);
      return;
    }

    // 计算总需求量和模具产能
    int totalProductionQty = calculateTotalProductionQty(requirePlans);
    totalProductionQty += adjustHeightProductionQty;
    // 检查日硫化量
    int dayVulcanizationQty = requirePlans.get(0).getDayVulcanizationQty();
    int totalMouldCapacity = calculateTotalMouldCapacity(
        dayVulcanizationQty, maxAllocationDay);

    // 判断是否需要调整
    if (totalMouldCapacity >= totalProductionQty) {
      log.info("模具产能足够，无需调整: materialDesc={}, totalMouldCapacity={}, totalProductionQty={}", materialDesc, totalMouldCapacity, totalProductionQty);
      return;
    }
    int lossHeightProductionQty = calculateHeightLossProductionQty(materialDesc, plansByMaterial,productionContext);
    // 计算并分配剩余产量
    int leftProductionQty = totalMouldCapacity - adjustHeightProductionQty - lossHeightProductionQty;
    log.info("计算并分配剩余产量: materialDesc={},dayVulcanizationQty={}, totalMouldCapacity={}, adjustHeightProductionQty={},lossHeightProductionQty={}, leftProductionQty={}", materialDesc,dayVulcanizationQty,totalMouldCapacity,adjustHeightProductionQty,lossHeightProductionQty,leftProductionQty);
    if (leftProductionQty <= 0) {
      log.info("计算出的剩余产量异常: materialDesc={},dayVulcanizationQty={}, totalMouldCapacity={}, adjustHeightProductionQty={},lossHeightProductionQty={}, leftProductionQty={}", materialDesc,dayVulcanizationQty,totalMouldCapacity,adjustHeightProductionQty,lossHeightProductionQty,leftProductionQty);
      requirePlans.forEach(item -> {
            item.setProductionQty(BigDecimal.ZERO.intValue());
            item.setOriginProductionQty(BigDecimal.ZERO.intValue());
      });
      updatePlanDemandQty(cxContinueInfo, materialDesc, BigDecimal.ZERO.intValue());
      updateIsProductionBySum(requirePlans);
      return;
    }
    // 执行分配
    executeProductionAllocation(requirePlans, leftProductionQty, cxContinueInfo, materialDesc);
  }

  private int calculateHeightLossProductionQty(String materialDesc, Map<String, List<MonthPlanProductionRequirePlanVo>> plansByMaterial, TbrProductionContext productionContext) {
    int totalHeightLossProductionQty = 0;
    ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
    Integer changeTypeBlockQty = paramConfiguration.getChangeTypeBlockQty();
    if(null == changeTypeBlockQty) {
      return totalHeightLossProductionQty;
    }
    List<ProductionMouldInfoVo> mouldInfos = productionContext.findMouldInfoByMaterialDesc(materialDesc);
    Set<String> intersectionMaterials = getIntersectionOfMaterialSets(mouldInfos);
    if (CollectionUtils.isEmpty(intersectionMaterials)) {
      return totalHeightLossProductionQty;
    }
    Set<String> otherMaterialDescs =  plansByMaterial.entrySet().stream()
        .filter(entry -> !materialDesc.equals(entry.getKey()))
        .filter(entry -> intersectionMaterials.contains(entry.getKey()))
        .filter(entry -> filterByMouldInfos(entry.getKey(),productionContext))
        .flatMap(entry -> entry.getValue().stream())
        .filter(HAS_HEIGHT_PRODUCTION_FILTER)
        .map(MonthPlanProductionRequirePlanVo::getMaterialDesc)
        .collect(Collectors.toSet());
    if(CollectionUtils.isEmpty(otherMaterialDescs)) {
      return totalHeightLossProductionQty;
    }

    for(String otherMaterialDesc : otherMaterialDescs) {
          MonthPlanProductionRequirePlanVo otherHeightPriorityRequirePlan = plansByMaterial.get(otherMaterialDesc).get(0);
          totalHeightLossProductionQty +=  calculateHeightLossProductionQty(otherHeightPriorityRequirePlan,changeTypeBlockQty);
    }
    return totalHeightLossProductionQty;
  }

  private int calculateHeightLossProductionQty(MonthPlanProductionRequirePlanVo otherHeightPriorityRequirePlan, Integer changeTypeBlockQty) {
    // 检查日硫化量
    Integer dayVulcanizationQty = otherHeightPriorityRequirePlan.getDayVulcanizationQty();
    if(null == dayVulcanizationQty) {
      return 0;
    }
    return dayVulcanizationQty - changeTypeBlockQty;
  }

  /**
   * 前置条件检查
   * @param materialDesc 物料
   * @param requirePlans 需求计划列表
   * @param productionContext 排产上下文
   * @return 是否可调整
   */
  private boolean validateAdjustmentConditions(
      String materialDesc,
      List<MonthPlanProductionRequirePlanVo> requirePlans,
      TbrProductionContext productionContext) {

    // 检查日硫化量
    Integer dayVulcanizationQty = requirePlans.get(0).getDayVulcanizationQty();
    if (dayVulcanizationQty == null) {
      log.info("日硫化量为空，跳过调整: materialDesc={}", materialDesc);
      return false;
    }

    // 检查高优先级产量
    int totalHeightProductionQty = sumHeightProductionQty(requirePlans);
    if (totalHeightProductionQty > 0) {
      log.info("高优先级产量大于0，跳过调整: materialDesc={}, totalHeightProductionQty={}",
          materialDesc, totalHeightProductionQty);
      return false;
    }

    // 检查可排产量
    int totalProductionQty = sumProductionQty(requirePlans);
    if (totalProductionQty == 0) {
      log.info("可排产量为0，跳过调整: materialDesc={}", materialDesc);
      return false;
    }

    // 检查模具数量
    List<ProductionMouldInfoVo> mouldInfos = productionContext.findMouldInfoByMaterialDesc(materialDesc);
    if (mouldInfos.size() != DOUBLE_MOULD_COUNT) {
      log.info("模具数量不符合要求: materialDesc={}, mouldCount={}", materialDesc, mouldInfos.size());
      return false;
    }

    return true;
  }

  /**
   *  计算调整相关的高优先级产量
   * @param materialDesc 续作SKU
   * @param plansByMaterial 按照续作SKU分组计划
   * @param productionContext 排产上下文
   * @return 高优先级产量
   */
  private int calculateAdjustHeightProductionQty(String materialDesc,
                                                 Map<String, List<MonthPlanProductionRequirePlanVo>> plansByMaterial,
                                                 TbrProductionContext productionContext) {
    List<ProductionMouldInfoVo> mouldInfos = productionContext.findMouldInfoByMaterialDesc(materialDesc);
    Set<String> intersectionMaterials = getIntersectionOfMaterialSets(mouldInfos);
    if (CollectionUtils.isEmpty(intersectionMaterials)) {
      log.info("calculateAdjustHeightProductionQty: materialDesc={},没有交集", materialDesc);
      return 0;
    }
    log.info("calculateAdjustHeightProductionQty: materialDesc={}, intersectionMaterials={}", materialDesc, intersectionMaterials);
    return plansByMaterial.entrySet().stream()
        .filter(entry -> !materialDesc.equals(entry.getKey()))
        .filter(entry -> intersectionMaterials.contains(entry.getKey()))
        .filter(entry -> filterByMouldInfos(entry.getKey(),productionContext))
        .flatMap(entry -> entry.getValue().stream())
        .filter(HAS_HEIGHT_PRODUCTION_FILTER)
        .mapToInt(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty)
        .sum();
  }

  /**
   * 判断续作SKU是否仅有两幅模具
   * @param materialDesc  续作SKU
   * @param productionContext 排产上下文
   * @return  是否仅有两幅模具
   */
  private boolean filterByMouldInfos(String materialDesc, TbrProductionContext productionContext) {
      List<ProductionMouldInfoVo> mouldInfos = productionContext.findMouldInfoByMaterialDesc(materialDesc);
      if(CollectionUtils.isEmpty(mouldInfos)){
        return false;
      }
      return mouldInfos.size() == DOUBLE_MOULD_COUNT;
  }

  private int calculateTotalProductionQty(List<MonthPlanProductionRequirePlanVo> requirePlans) {
    return requirePlans.stream()
        .filter(HAS_PRODUCTION_QTY_FILTER)
        .mapToInt(MonthPlanProductionRequirePlanVo::getOriginProductionQty)
        .sum();
  }

  private int calculateTotalMouldCapacity(
      int dayVulcanizationQty,
      int maxAllocationDay) {
    return maxAllocationDay * DOUBLE_MOULD_COUNT * dayVulcanizationQty;
  }

  private void executeProductionAllocation(
      List<MonthPlanProductionRequirePlanVo> requirePlans,
      int leftProductionQty,
      CxContinueInfoHelper cxContinueInfo,
      String materialDesc) {
    try {
      mouldCapacityAllocator.allocateProductionQty(requirePlans, leftProductionQty);
      updatePlanDemandQty(cxContinueInfo, materialDesc, leftProductionQty);
      updateIsProductionBySum(requirePlans);
      log.info("成功调整续作SKU排产量: materialDesc={}, leftProductionQty={}", materialDesc, leftProductionQty);
    } catch (Exception e) {
      log.info("分配产量失败: materialDesc={}, leftProductionQty={}", materialDesc, leftProductionQty, e);
      throw new BusinessException("分配产量失败", e);
    }
  }

  /**
   *  检查高优先级产量
   * @param requirePlans 需求计划列表
   * @return 高优先级产量
   */
  private int sumHeightProductionQty(List<MonthPlanProductionRequirePlanVo> requirePlans) {
    return requirePlans.stream()
        .filter(HAS_HEIGHT_PRODUCTION_FILTER)
        .mapToInt(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty)
        .sum();
  }

  /**
   *  检查可排产量
   * @param requirePlans 需求计划列表
   * @return 可排产量
   */
  private int sumProductionQty(List<MonthPlanProductionRequirePlanVo> requirePlans) {
    return requirePlans.stream()
        .filter(HAS_PRODUCTION_QTY_FILTER)
        .mapToInt(MonthPlanProductionRequirePlanVo::getOriginProductionQty)
        .sum();
  }

  /**
   * 获取模具列表关联SKU的交集
   */
  public Set<String> getIntersectionOfMaterialSets(List<ProductionMouldInfoVo> mouldInfos) {
    if (CollectionUtils.isEmpty(mouldInfos)) {
      return Collections.emptySet();
    }

    return mouldInfos.stream()
        .map(ProductionMouldInfoVo::getAssociationMaterialSet)
        .filter(Objects::nonNull)
        .filter(set -> !set.isEmpty())
        .reduce((set1, set2) -> {
          Set<String> intersection = new HashSet<>(set1);
          intersection.retainAll(set2);
          return intersection.isEmpty() ? Collections.emptySet() : intersection;
        })
        .orElse(Collections.emptySet());
  }

  /**
   * 计算最大分配天数总和
   */
  public int calculateMaxAllocationDays(List<CxMachineAllocationPlanHelper> continueAllocationList) {
    if (CollectionUtils.isEmpty(continueAllocationList)) {
      return 0;
    }
    IntSummaryStatistics stats = continueAllocationList.stream()
        .mapToInt(CxMachineAllocationPlanHelper::getAllocationDay)
        .summaryStatistics();
    if (stats.getMax() == 0) {
      return 0;
    }
    // 计算最大值的总和（最大值 × 出现次数）
    return (int) (stats.getMax() * continueAllocationList.stream()
        .filter(helper -> helper.getAllocationDay() == stats.getMax())
        .count());
  }

  /**
   *  更新排产按净需求量
   * @param requirePlans 需求计划列表
   */
  private void updateIsProductionBySum(List<MonthPlanProductionRequirePlanVo> requirePlans) {
    requirePlans.forEach(plan -> plan.setIsProductionBySum(Constant.TRUE));
  }

  /**
   *  更新计划需求量
   * @param groupContinueInfo 续作结构分组
   * @param materialDesc 物料描述
   * @param leftProductionQty 可排产量
   */
  private void updatePlanDemandQty(CxContinueInfoHelper groupContinueInfo,
                                   String materialDesc,
                                   int leftProductionQty) {
    Map<String, CxContinueSkuInfoHelper> continueSkuMap =
        groupContinueInfo.getContinueSkuMouldNumberMap();

    if (CollectionUtils.isEmpty(continueSkuMap) ||
        !continueSkuMap.containsKey(materialDesc)) {
      log.info("续作SKU映射中未找到对应物料: materialDesc={}", materialDesc);
      return;
    }

    continueSkuMap.get(materialDesc).setPlanDemandQty(leftProductionQty);
  }
}
