package com.zlt.aps.monthplan.common.utils;

import com.google.common.collect.Sets;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 需求计划合并分组
 * @author Yelq
 */
@Slf4j
public class DemandPlanGrouper {
  // 供应链优先级常量
  private static final Set<String> RESERVE_PRIORITIES = Sets.newHashSet(ApsConstant.SAL_PRIORITY_CYCLE_STOCK_UP, ApsConstant.SAL_PRIORITY_PRECEDENT_STOCK_UP);
  private static final Set<String> SALES_PRIORITIES = Sets.newHashSet(ApsConstant.SAL_PRIORITY_HIGHT, ApsConstant.SAL_PRIORITY_MID, ApsConstant.SAL_PRIORITY_POSTPONE);

  /**
   * 主处理方法：按业务规则分组需求计划
   */
  public static Map<String, List<DpDemandPlan>> groupDemandPlans(DpDemandPlan createCondition,List<DpDemandPlan> demandPlans) {
    if (CollectionUtils.isEmpty(demandPlans)) {
      return Collections.emptyMap();
    }
    List<DpDemandPlan> filterDemandPlans = processDemandPlansOptimized(demandPlans);
    if (CollectionUtils.isEmpty(filterDemandPlans)) {
      return Collections.emptyMap();
    }
    // 1. 按原始groupKey分组
    Map<String, List<DpDemandPlan>> originalGroups = filterDemandPlans.stream()
        .collect(Collectors.groupingBy(DpDemandPlan::getGroupKey));
    // 2. 分类处理
    return processGroups(createCondition,originalGroups);
  }


  private static List<DpDemandPlan> processDemandPlansOptimized(List<DpDemandPlan> demandPlans) {
    if (CollectionUtils.isEmpty(demandPlans)) {
      return Collections.emptyList();
    }
    // 使用自定义的GroupInfo来跟踪分组信息
    Map<String, GroupInfo> groupInfoMap = new HashMap<>();
    for (DpDemandPlan plan : demandPlans) {
      String key = plan.getGroupFactoryAndMaterialKey();
      GroupInfo info = groupInfoMap.computeIfAbsent(key, k -> new GroupInfo());
      // 累加净需求
      info.totalNetQty += (plan.getNetQty() != null ? plan.getNetQty() : 0);
      // 收集原始对象
      info.plans.add(plan);
    }
    // 构建结果列表
    List<DpDemandPlan> result = new ArrayList<>();
    for (GroupInfo info : groupInfoMap.values()) {
      if (info.totalNetQty != 0) {
        result.addAll(info.plans);
      }
    }

    return result;
  }

  /**
   * 分组信息内部类
   */
  private static class GroupInfo {
    int totalNetQty = 0;
    List<DpDemandPlan> plans = new ArrayList<>();
  }

  /**
   * 处理分组：分离纯储备订单分组并进行合并
   */
  private static Map<String, List<DpDemandPlan>> processGroups(
      DpDemandPlan createCondition,
      Map<String, List<DpDemandPlan>> originalGroups) {

    // 分离纯储备订单分组和销售订单分组
    Map<String, List<DpDemandPlan>> pureReserveGroups = new HashMap<>();
    Map<String, List<DpDemandPlan>> salesGroups = new HashMap<>();
    Map<String, List<DpDemandPlan>> mixedGroups = new HashMap<>();

    for (Map.Entry<String, List<DpDemandPlan>> entry : originalGroups.entrySet()) {
      List<DpDemandPlan> plans = entry.getValue();
      GroupType type = analyzeGroupType(plans);
      switch (type) {
        case PURE_RESERVE:
          pureReserveGroups.put(entry.getKey(), plans);
          break;
        case PURE_SALES:
          salesGroups.put(entry.getKey(), plans);
          break;
        case MIXED:
          mixedGroups.put(entry.getKey(), plans);
          break;
      }
    }
    // 构建销售订单分组的快速索引
    SalesGroupIndex salesGroupIndex = buildSalesGroupIndex(salesGroups, mixedGroups);

    // 处理纯储备订单分组
    Map<String, List<DpDemandPlan>> resultGroups = new HashMap<>(mixedGroups);
    resultGroups.putAll(salesGroups);

    mergePureReserveGroups(createCondition,pureReserveGroups, salesGroupIndex, resultGroups);

    return resultGroups;
  }

  /**
   * 分析分组类型
   */
  private static GroupType analyzeGroupType(List<DpDemandPlan> plans) {
    boolean hasReserve = false;
    boolean hasSales = false;

    for (DpDemandPlan plan : plans) {
      String priority = plan.getScmPriority();
      if (RESERVE_PRIORITIES.contains(priority)) {
        hasReserve = true;
      } else if (SALES_PRIORITIES.contains(priority)) {
        hasSales = true;
      }
    }

    if (hasReserve && hasSales) {
      return GroupType.MIXED;
    } else if (hasReserve) {
      return GroupType.PURE_RESERVE;
    } else {
      return GroupType.PURE_SALES;
    }
  }

  /**
   * 构建销售订单分组索引
   */
  private static SalesGroupIndex buildSalesGroupIndex(
      Map<String, List<DpDemandPlan>> salesGroups,
      Map<String, List<DpDemandPlan>> mixedGroups) {

    SalesGroupIndex index = new SalesGroupIndex();

    // 处理纯销售订单分组
    for (Map.Entry<String, List<DpDemandPlan>> entry : salesGroups.entrySet()) {
      index.addGroup(entry.getKey(), entry.getValue());
    }

    // 处理混合分组（包含销售订单）
    for (Map.Entry<String, List<DpDemandPlan>> entry : mixedGroups.entrySet()) {
      index.addGroup(entry.getKey(), entry.getValue());
    }

    return index;
  }

  /**
   * 合并纯储备订单分组到合适的销售订单分组
   */
  private static void mergePureReserveGroups(
      DpDemandPlan createCondition,
      Map<String, List<DpDemandPlan>> pureReserveGroups,
      SalesGroupIndex salesGroupIndex,
      Map<String, List<DpDemandPlan>> resultGroups) {

    for (Map.Entry<String, List<DpDemandPlan>> reserveEntry : pureReserveGroups.entrySet()) {
      log.info("monthPlanVersion:{},key: {},size:{}",createCondition.getMonthPlanVersion(),reserveEntry.getKey(),reserveEntry.getValue().size());
      List<DpDemandPlan> reservePlans = reserveEntry.getValue();
      // 获取储备订单的物料编码
      DpDemandPlan firstPlan = reservePlans.get(0);
      String materialCode = firstPlan.getMaterialCode();
      // 查找最佳目标分组
      SalesGroupInfo bestGroup = salesGroupIndex.findBestGroupForReserve(materialCode);
      if (bestGroup != null) {
        // 合并到目标分组
        List<DpDemandPlan> targetGroup = resultGroups.get(bestGroup.groupKey);
        targetGroup.addAll(reservePlans);
        continue;
      }
      resultGroups.put(reserveEntry.getKey(),reservePlans);
    }
  }

  /**
   * 销售订单分组信息
   */
  private static class SalesGroupInfo {
    final String groupKey;
    final String materialCode;
    final int yearWeek;
    final int dynamicBalance; // 0或1
    final int uniformity;     // 0或1
    final int priorityScore;  // 优先级分数

    SalesGroupInfo(String groupKey, List<DpDemandPlan> plans) {
      this.groupKey = groupKey;
      DpDemandPlan firstPlan = plans.get(0);
      String transformed = firstPlan.getYearWeek().substring(2) + firstPlan.getYearWeek().substring(0,2);
      int orderWeekYear = Integer.parseInt(transformed);
      this.materialCode = firstPlan.getMaterialCode();
      this.yearWeek = orderWeekYear;
      this.dynamicBalance = YesOrNoEnum.YES.getCode().equals(firstPlan.getIsDynamicBalance()) ? YesOrNoEnum.YES.getValue() : YesOrNoEnum.NO.getValue();
      this.uniformity = YesOrNoEnum.YES.getCode().equals(firstPlan.getIsUniformity()) ? YesOrNoEnum.YES.getValue() :  YesOrNoEnum.NO.getValue();
      this.priorityScore = calculatePriorityScore(dynamicBalance, uniformity);
    }

    private int calculatePriorityScore(int dynamicBalance, int uniformity) {
      if (YesOrNoEnum.NO.getValue()  == dynamicBalance && YesOrNoEnum.NO.getValue() == uniformity) {
        return 0; // 第一优先级
      } else if (YesOrNoEnum.NO.getValue()  == dynamicBalance) {
        return 1; // 第二优先级
      } else if (YesOrNoEnum.NO.getValue()  == uniformity) {
        return 2; // 第三优先级
      } else {
        return 3; // 第四优先级
      }
    }
  }

  /**
   * 销售订单分组索引
   */
  private static class SalesGroupIndex {
    // 物料编码 -> 该物料的所有销售订单分组
    private final Map<String, List<SalesGroupInfo>> index = new HashMap<>();

    void addGroup(String groupKey, List<DpDemandPlan> plans) {
      SalesGroupInfo info = new SalesGroupInfo(groupKey, plans);
      index.computeIfAbsent(info.materialCode, k -> new ArrayList<>())
          .add(info);
    }

    /**
     * 为储备订单查找最佳目标分组
     */
    SalesGroupInfo findBestGroupForReserve(String materialCode) {
      List<SalesGroupInfo> groups = index.get(materialCode);
      if (CollectionUtils.isEmpty(groups)) {
        return null;
      }

      // 找到年周号最小的分组
      int minYearWeek = groups.stream()
          .map(g -> g.yearWeek)
          .min(Integer::compareTo)
          .orElse(null);

      // 在所有年周号最小的分组中，选择优先级最高的
      return groups.stream()
          .filter(g -> minYearWeek == g.yearWeek)
          .min(Comparator.comparingInt(g -> g.priorityScore))
          .orElse(null);
    }
  }



  /**
   * 分组类型枚举
   */
  private enum GroupType {
    // 纯储备订单
    PURE_RESERVE,
    // 纯销售订单
    PURE_SALES,
    // 混合订单
    MIXED
  }

}
