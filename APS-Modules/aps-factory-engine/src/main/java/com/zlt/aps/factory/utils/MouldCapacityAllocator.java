package com.zlt.aps.factory.utils;

import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 模具产能分配器 - 将总模具产能按比例分配到需求计划中
 * @author Yelq
 */
@Slf4j
@Component
public class MouldCapacityAllocator {

  /**
   * 按比例分配总模具产能到需求计划列表
   *
   * @param requirePlansByMaterialDesc 需求计划列表（将直接修改列表中的对象）
   * @param totalMouldCapacity 总模具产能
   * @throws IllegalArgumentException 参数无效时抛出异常
   */
  public void allocateProductionQty(
      List<MonthPlanProductionRequirePlanVo> requirePlansByMaterialDesc,
      int totalMouldCapacity) {

    // 1. 计算原始需求总量
    int originalTotal = calculateOriginalTotal(requirePlansByMaterialDesc);
    log.info("allocateProductionQty:originalTotal={},totalMouldCapacity={}",originalTotal,totalMouldCapacity);
    // 2. 根据情况采用不同的分配策略
    if (originalTotal == 0) {
      // 情况A: 原始总量为0，采用平均分配策略
      allocateEvenly(requirePlansByMaterialDesc, totalMouldCapacity);
    } else if (originalTotal == totalMouldCapacity) {
      // 情况B: 原始总量等于总产能，无需调整
      // 保持原样
    } else {
      // 情况C: 原始总量不为0，采用按比例分配策略
      allocateProportionally(
          requirePlansByMaterialDesc,
          totalMouldCapacity,
          originalTotal
      );
    }
  }

  /**
   * 计算原始需求总量
   */
  private int calculateOriginalTotal(
      List<MonthPlanProductionRequirePlanVo> plans) {

    return plans.stream()
        .mapToInt(plan ->
            plan.getProductionQty() == null ? 0 : plan.getProductionQty()
        )
        .sum();
  }

  /**
   * 平均分配策略（当原始总量为0时使用）
   */
  private void allocateEvenly(
      List<MonthPlanProductionRequirePlanVo> plans,
      int totalMouldCapacity) {

    int planCount = plans.size();
    if (planCount == 0) {
      return;
    }

    // 计算基本分配量和余数
    int baseAllocation = totalMouldCapacity / planCount;
    int remainder = totalMouldCapacity % planCount;

    // 平均分配，余数分配给前几个计划
    for (int i = 0; i < planCount; i++) {
      int allocation = baseAllocation + (i < remainder ? 1 : 0);
      plans.get(i).setProductionQty(allocation);
    }
  }

  /**
   * 按比例分配策略（当原始总量不为0时使用）
   */
  private void allocateProportionally(
      List<MonthPlanProductionRequirePlanVo> plans,
      int totalMouldCapacity,
      int originalTotal) {

    int planCount = plans.size();

    // 1. 计算每个计划的权重和理论分配值
    List<PlanAllocationData> allocationData = new ArrayList<>(planCount);

    for (int i = 0; i < planCount; i++) {
      MonthPlanProductionRequirePlanVo plan = plans.get(i);
      int originalQty = plan.getProductionQty() == null ? 0 : plan.getProductionQty();

      // 计算权重和理论值
      double weight = (double) originalQty / originalTotal;
      double theoreticalValue = weight * totalMouldCapacity;

      // 向下取整得到基础分配量
      int baseAllocation = (int) Math.floor(theoreticalValue);

      // 计算小数部分（用于后续调整）
      double fractionalPart = theoreticalValue - baseAllocation;

      allocationData.add(new PlanAllocationData(
          i, originalQty, theoreticalValue,
          baseAllocation, fractionalPart
      ));
    }

    // 2. 计算初始分配总和及剩余量
    int allocatedTotal = allocationData.stream()
        .mapToInt(data -> data.baseAllocation)
        .sum();

    int remaining = totalMouldCapacity - allocatedTotal;

    // 3. 将剩余量按照小数部分从大到小分配
    if (remaining > 0) {
      allocateRemainingCapacity(allocationData, remaining);
    }

    // 4. 将最终分配结果设置回原计划
    for (PlanAllocationData data : allocationData) {
      log.info("allocateProportionally:monthPlanId={},productionQty={}",plans.get(data.index).getMonthPlanId(),data.finalAllocation);
      plans.get(data.index).setProductionQty(data.finalAllocation);
    }
  }

  /**
   * 分配剩余产能
   */
  private void allocateRemainingCapacity(
      List<PlanAllocationData> allocationData,
      int remaining) {

    // 按小数部分降序排序
    allocationData.sort((a, b) ->
        Double.compare(b.fractionalPart, a.fractionalPart)
    );

    // 分配剩余量（每个最多加1，直到剩余量用完）
    for (int i = 0; i < remaining && i < allocationData.size(); i++) {
      allocationData.get(i).finalAllocation++;
    }
  }

  /**
   * 计划分配数据内部类
   */
  private static class PlanAllocationData {
    final int index;                 // 原列表中的索引
    final int originalQty;           // 原始需求量
    final double theoreticalValue;   // 理论分配值
    final int baseAllocation;        // 基础分配量（向下取整）
    final double fractionalPart;     // 小数部分
    int finalAllocation;             // 最终分配量

    PlanAllocationData(int index, int originalQty, double theoreticalValue,
                       int baseAllocation, double fractionalPart) {
      this.index = index;
      this.originalQty = originalQty;
      this.theoreticalValue = theoreticalValue;
      this.baseAllocation = baseAllocation;
      this.finalAllocation = baseAllocation;
      this.fractionalPart = fractionalPart;
    }
  }
}
