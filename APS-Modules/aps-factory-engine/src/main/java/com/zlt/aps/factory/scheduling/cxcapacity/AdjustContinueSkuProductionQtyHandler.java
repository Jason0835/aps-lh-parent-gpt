package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

  private final MouldCapacityAllocator mouldCapacityAllocator;
  /**
   *  调整续作SKU排产量,需满足以下条件:
   *  1、获取分配天数最长的续作结构成型产能分配
   *  2、模具受限: 续作SKU可用续作模具只有两幅
   *  3、续作SKU的SUM(高优先级排产量)=0,SUM(可排产量)>0
   *  4、结构向下，（同规格同花纹+共生胎同模具）可用模具也是续作SKU的那两幅模具，并且高优先级排产量>0
   *  5、productionQty = 续作SKU的SUM(可排产量)+SUM(同规格同花纹+共生胎同模具高优先级排产量)
   *  6、计算续作SKU可用的两幅模具总产能 totalMouldCapacity
   *  7、汇总比较totalMouldCapacity和productionQty,取最小值min(totalMouldCapacity,productionQty)作为续作SKU的可排产量
   * @param continueAllocationList 续作分配结果
   * @param productionContext 排产上下文
   */
  public void adjustContinueSkuProductionQty(Map<String, ProductionPlanGroupInfo> allGroupPlanMap,List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap, TbrProductionContext productionContext) {
    if(CollectionUtils.isEmpty(allGroupPlanMap) || CollectionUtils.isEmpty(continueAllocationList) || CollectionUtils.isEmpty(allContinueMap)) {
      return;
    }
    Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
    allContinueMap.forEach((structureName, cxContinueInfo) -> {
          ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
          List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
          backupProductionQty(groupPlanData);
          List<CxMachineAllocationPlanHelper> continueCxMachineAllocation = groupPlanMap.get(groupPlanInfo);
          if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return;
          }
          // 1、获取分配天数最长的续作结构成型产能分配
          int  maxAllocationDay = this.countMaxAllocationDay(continueCxMachineAllocation);
          if(maxAllocationDay == 0) {
            return;
          }
          List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList = groupPlanData.stream().filter(MonthPlanProductionRequirePlanVo::hasProduction).collect(Collectors.toList());
          if(CollectionUtils.isEmpty(leftOverHasProductionList)) {
            return;
          }
          Map<String,List<MonthPlanProductionRequirePlanVo>> mapGroupByMaterialDesc = this.getRequirePlansGroupByMaterialDesc(leftOverHasProductionList);
          mapGroupByMaterialDesc.forEach((materialDesc, requirePlansByMaterialDesc) -> adjustContinueSkuProductionQty(materialDesc,requirePlansByMaterialDesc,mapGroupByMaterialDesc,maxAllocationDay,productionContext));
    });
  }

  private void adjustContinueSkuProductionQty(String materialDesc,List<MonthPlanProductionRequirePlanVo> requirePlansByMaterialDesc,Map<String,List<MonthPlanProductionRequirePlanVo>> mapGroupByMaterialDesc,int  maxAllocationDay, TbrProductionContext productionContext) {
    Integer  dayVulcanizationQty = requirePlansByMaterialDesc.get(0).getDayVulcanizationQty();
    // 日硫化量为空,不调整
    if(null == dayVulcanizationQty) {
      return;
    }
    int totalHeightProductionQty = requirePlansByMaterialDesc.stream().filter(item -> null != item.getHeightProductionQty()).mapToInt(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
    // 汇总续作SKU高优先级可排产量,=0才可调整
    if(totalHeightProductionQty > 0) {
      return;
    }
    int totalProductionQty =  requirePlansByMaterialDesc.stream().filter(item -> null != item.getProductionQty()).mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
    // 汇总续作SKU可排产量,>0 才可调整
    if(totalProductionQty == 0) {
      return;
    }
    List<ProductionMouldInfoVo>  mouldInfos = productionContext.findMouldInfoByMaterialDesc(materialDesc);
    // 仅有2副可用模具才可调整
    if(ProductionConstant.DOUBLE_MOULD_PRODUCTION != mouldInfos.size()) {
      return;
    }
    Set<String> intersectionOfMaterialSets =  this.getIntersectionOfMaterialSets(mouldInfos);
    if(CollectionUtils.isEmpty(intersectionOfMaterialSets)) {
      return;
    }
    int adjustHeightProductionQty = 0;
    for(Map.Entry<String, List<MonthPlanProductionRequirePlanVo>> entry : mapGroupByMaterialDesc.entrySet()) {
      if(materialDesc.contains(entry.getKey()) || !intersectionOfMaterialSets.contains(entry.getKey())) {
        continue;
      }
      adjustHeightProductionQty +=  entry.getValue().stream().filter(item -> null != item.getHeightProductionQty()).mapToInt(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
    }
    if(adjustHeightProductionQty == 0) {
      return;
    }
    totalProductionQty =  totalProductionQty + adjustHeightProductionQty;
    // 模具总产能=最多分配天数总和*模具数量*日硫化量
    int totalMouldCapacity =  maxAllocationDay*mouldInfos.size()*dayVulcanizationQty;
    // 需要算一下模具的产能，如果能把高优先级+续作的中优先级全部能包过来，那么就续作优先；
    if(totalMouldCapacity >=  totalProductionQty) {
      return;
    }
    int leftProductionQty = totalMouldCapacity - adjustHeightProductionQty;
    // 如果不能包过来，就需要把中优先级中途下机，下机的时间点是，剩余的模具产能，正好能把高优先级产完。
    mouldCapacityAllocator.allocateProductionQty(requirePlansByMaterialDesc,leftProductionQty);
  }

  private void backupProductionQty(List<MonthPlanProductionRequirePlanVo> requirePlansByMaterialDesc) {
    requirePlansByMaterialDesc.forEach(requirePlan -> requirePlan.setOriginProductionQty(requirePlan.getProductionQty()));
  }


  /**
   *  取模具列表关联SKU的交集
   * @param mouldInfos 模具列表
   * @return 关联SKU的交集
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
          return intersection;
        })
        .orElse(Collections.emptySet());
  }


  private Map<String, List<MonthPlanProductionRequirePlanVo>> getRequirePlansGroupByMaterialDesc(List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList) {
    return leftOverHasProductionList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
  }

  public int countMaxAllocationDay(List<CxMachineAllocationPlanHelper> continueAllocationList) {
    // 找到最大值
    int maxDay = continueAllocationList.stream()
        .mapToInt(CxMachineAllocationPlanHelper::getAllocationDay)
        .max()
        .orElse(0);
    if(maxDay == 0) {
      return maxDay;
    }
    // 筛选出等于最大值的元素
    return continueAllocationList.stream().filter(helper -> helper.getAllocationDay() == maxDay).mapToInt(CxMachineAllocationPlanHelper::getAllocationDay).sum();
  }
}
