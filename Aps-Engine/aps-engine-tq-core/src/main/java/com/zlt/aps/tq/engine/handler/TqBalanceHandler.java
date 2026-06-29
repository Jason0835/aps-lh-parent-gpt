package com.zlt.aps.tq.engine.handler;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S5: 班次均衡调整Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>按机台定额控制各班次产量，避免过载或空闲</li>
 *   <li>均衡D日计划（1班=D日中班）：基于交接班库存平衡基准值调整</li>
 *   <li>均衡D+1日计划（2~4班）：基于差额百分比调整</li>
 *   <li>均衡D+2日计划（5~6班）：基于差额百分比调整</li>
 *   <li>各规格班次产量不超过机台定额总产能</li>
 * </ol>
 *
 * <p>均衡策略：</p>
 * <ul>
 *   <li>各规格各班次产量受specClassQuotaMap中的定额总产能约束</li>
 *   <li>超出定额的产量调整到同日其他班次</li>
 *   <li>同日内各班次产量差额百分比控制在5%以内</li>
 * </ul>
 */
@Slf4j
@Component
public class TqBalanceHandler extends AbsTqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Resource
    private TqMachineAssignHandler machineAssignHandler;

    private static final String DIVISION = "\r\n---------------------------------------------------\r\n";

    /** 均衡差额百分比阈值 */
    private static final double BALANCE_DIFF_RATE_THRESHOLD = 5.0;

    @Override
    protected String getStepName() {
        return "S5-班次均衡调整";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        TqTotalPlanQtyVo totalPlanQtyVo = context.getTotalPlanQtyVo();

        // 1. 定额约束调整：各规格各班次产量不超过机台定额总产能
        applyQuotaConstraint(context);

        // 2. 均衡D日计划（1班=D日中班）
        equilibriumDay1(context.getScheduleList(), totalPlanQtyVo, params);

        // 3. 均衡D+1日计划（2~4班=D+1日夜早中）
        equilibriumDay2(context.getScheduleList(), totalPlanQtyVo, params);

        // 4. 均衡D+2日计划（5~6班=D+2日夜早）
        equilibriumDay3(context.getScheduleList(), totalPlanQtyVo, params);

        // 5. 刷新任务链（S5调整了计划量，需同步更新链条节点的库存和保证班数）
        machineAssignHandler.refreshTaskChain(context, null, 1);

        log.info("[S5] 班次均衡调整完成, 总计划量:{}", toJSONString(totalPlanQtyVo));
    }

    // ==================== 定额约束调整 ====================

    /**
     * 定额约束调整：各规格各班次产量不超过机台定额总产能。
     *
     * <p>遍历排程列表，检查各规格各班次产量是否超过specClassQuotaMap中的定额总产能，
     * 超出部分调整到同日其他班次或下一班次。</p>
     */
    private void applyQuotaConstraint(TqScheduleContext context) {
        Map<String, Map<Integer, Double>> specClassQuotaMap = context.getSpecClassQuotaMap();
        if (specClassQuotaMap == null || specClassQuotaMap.isEmpty()) {
            return;
        }

        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            String beadCode = scheduleVo.getBeadCode();
            Map<Integer, Double> classQuotaMap = specClassQuotaMap.get(beadCode);
            if (classQuotaMap == null || classQuotaMap.isEmpty()) {
                continue;
            }

            for (int classNum = 1; classNum <= 6; classNum++) {
                Double quota = classQuotaMap.get(classNum);
                if (quota == null || quota <= 0) {
                    continue;
                }
                double planQty = getClassPlanQty(scheduleVo, classNum);
                if (planQty > quota) {
                    // 超出定额，截断并将超出部分延后
                    double overflow = BigDecimalUtil.sub(planQty, quota);
                    setClassPlanQty(scheduleVo, classNum, quota);

                    // 延后到下一班次
                    if (classNum < 6) {
                        double nextQty = getClassPlanQty(scheduleVo, classNum + 1);
                        setClassPlanQty(scheduleVo, classNum + 1, BigDecimalUtil.add(nextQty, overflow));
                    }

                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "定额约束调整",
                            "胎圈代码：" + beadCode + "，" + classNum + "班计划量" + planQty
                                    + "超过定额" + quota + "，截断为" + quota
                                    + (classNum < 6 ? "，超出" + overflow + "延后至" + (classNum + 1) + "班" : "，超出" + overflow + "无法延后"));
                }
            }
        }
    }

    // ==================== 均衡D日计划 ====================

    /**
     * 均衡D日计划（1班=D日中班）。
     *
     * <p>基于交接班库存平衡基准值，调整1班的计划量。</p>
     */
    private void equilibriumDay1(List<TqScheduleResultVo> scheduleList, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleParams params) {
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        double classStockReference = params.getClassStockReference();
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();

        // 计算D日结束时的库存结余（1班结束后的交接班库存）
        double totalClassStock = scheduleList.stream()
                .mapToDouble(vo -> {
                    // D日结束库存 = 初始库存 + 当天早班计划量 - 成型1班胎圈消耗 - 成型2班胎圈消耗 + 1班产出 - 成型3班胎圈消耗
                    double stock = BigDecimalUtil.add(vo.getStockQty() == null ? 0D : vo.getStockQty(),
                            vo.getTodayMorningPlanQty() == null ? 0D : vo.getTodayMorningPlanQty());
                    stock = BigDecimalUtil.sub(stock, mulCxPlan(vo.getCxClass1Plan(), coefficient));
                    stock = BigDecimalUtil.sub(stock, mulCxPlan(vo.getCxClass2Plan(), coefficient));
                    stock = BigDecimalUtil.add(stock, vo.getClass1PlanQty() == null ? 0D : vo.getClass1PlanQty());
                    stock = BigDecimalUtil.sub(stock, mulCxPlan(vo.getCxClass3Plan(), coefficient));
                    return stock;
                }).sum();

        double difStock = BigDecimalUtil.sub(totalClassStock, classStockReference);

        if (Math.abs(difStock) <= toolCapacity.doubleValue()) {
            return;
        }

        boolean isOverStock = difStock > 0;

        scheduleList = scheduleList.stream().sorted((r1, r2) -> {
            if (isOverStock) {
                return Double.compare(r2.getClass1PlanQty() == null ? 0D : r2.getClass1PlanQty(),
                        r1.getClass1PlanQty() == null ? 0D : r1.getClass1PlanQty());
            } else {
                return Double.compare(r1.getClass1PlanQty() == null ? 0D : r1.getClass1PlanQty(),
                        r2.getClass1PlanQty() == null ? 0D : r2.getClass1PlanQty());
            }
        }).collect(Collectors.toList());

        for (TqScheduleResultVo scheduleVo : scheduleList) {
            double class1Plan = scheduleVo.getClass1PlanQty() == null ? 0D : scheduleVo.getClass1PlanQty();

            if (isOverStock && class1Plan > 0) {
                double decreaseQty = class1Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class1Plan;
                double newClass1Plan = BigDecimalUtil.sub(class1Plan, decreaseQty);
                scheduleVo.setClass1PlanQty(newClass1Plan);
                totalPlanQtyVo.setTotalClass1PlanQty(BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass1PlanQty(), decreaseQty));
                totalClassStock = BigDecimalUtil.sub(totalClassStock, decreaseQty);
            } else if (!isOverStock) {
                double increaseQty = toolCapacity.doubleValue();
                double newClass1Plan = BigDecimalUtil.add(class1Plan, increaseQty);
                scheduleVo.setClass1PlanQty(newClass1Plan);
                totalPlanQtyVo.setTotalClass1PlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalClass1PlanQty(), increaseQty));
                totalClassStock = BigDecimalUtil.add(totalClassStock, increaseQty);
            }

            difStock = BigDecimalUtil.sub(totalClassStock, classStockReference);
            if (Math.abs(difStock) <= toolCapacity.doubleValue()) {
                break;
            }
        }

        autoScheduleLogService.insertTqScheduleLog(scheduleList.get(0).getBatchNo(), "",
                "均衡D日计划(1班)", "均衡后总计划量：" + toJSONString(totalPlanQtyVo));
    }

    // ==================== 均衡D+1日计划 ====================

    /**
     * 均衡D+1日计划（2~4班=D+1日夜早中）。
     *
     * <p>基于差额百分比进行均衡处理，将2班和3班的计划量调整到差额百分比以内。</p>
     */
    private void equilibriumDay2(List<TqScheduleResultVo> scheduleList, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleParams params) {
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        double totalClass2 = totalPlanQtyVo.getTotalClass2PlanQty();
        double totalClass3 = totalPlanQtyVo.getTotalClass3PlanQty();
        double difNum = BigDecimalUtil.sub(totalClass2, totalClass3);
        double totalPlan = BigDecimalUtil.add(totalClass2, totalClass3);
        if (totalPlan == 0) {
            return;
        }
        double actualDifRate = Math.abs(difNum) / totalPlan * 100;

        if (actualDifRate <= BALANCE_DIFF_RATE_THRESHOLD) {
            return;
        }

        boolean isClass2Over = (difNum > 0);
        if (isClass2Over) {
            scheduleList = scheduleList.stream()
                    .sorted(Comparator.comparing(vo -> vo.getClass2PlanQty() == null ? 0D : vo.getClass2PlanQty()))
                    .collect(Collectors.toList());
        } else {
            scheduleList = scheduleList.stream()
                    .sorted(Comparator.comparing(vo -> vo.getClass3PlanQty() == null ? 0D : vo.getClass3PlanQty()))
                    .collect(Collectors.toList());
        }

        double lastDifRate = actualDifRate;
        for (TqScheduleResultVo resultVo : scheduleList) {
            double class2Plan = resultVo.getClass2PlanQty() == null ? 0D : resultVo.getClass2PlanQty();
            double class3Plan = resultVo.getClass3PlanQty() == null ? 0D : resultVo.getClass3PlanQty();

            if (isClass2Over) {
                if (class2Plan == 0) continue;
                double decreasePlanQty = class2Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class2Plan;
                double newTotalClass2 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass2PlanQty(), decreasePlanQty);
                double newTotalClass3 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass3PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass2, newTotalClass3);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass2, newTotalClass3);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) break;
                resultVo.setClass2PlanQty(BigDecimalUtil.sub(class2Plan, decreasePlanQty));
                resultVo.setClass3PlanQty(BigDecimalUtil.add(class3Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass2PlanQty(newTotalClass2);
                totalPlanQtyVo.setTotalClass3PlanQty(newTotalClass3);
                lastDifRate = newDifRate;
            } else {
                if (class3Plan == 0) continue;
                double decreasePlanQty = class3Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class3Plan;
                double newTotalClass2 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass2PlanQty(), decreasePlanQty);
                double newTotalClass3 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass3PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass2, newTotalClass3);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass2, newTotalClass3);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) break;
                resultVo.setClass2PlanQty(BigDecimalUtil.add(class2Plan, decreasePlanQty));
                resultVo.setClass3PlanQty(BigDecimalUtil.sub(class3Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass2PlanQty(newTotalClass2);
                totalPlanQtyVo.setTotalClass3PlanQty(newTotalClass3);
                lastDifRate = newDifRate;
            }
        }

        autoScheduleLogService.insertTqScheduleLog(scheduleList.get(0).getBatchNo(), "",
                "均衡D+1日计划(2~4班)", "均衡后总计划量：" + toJSONString(totalPlanQtyVo));
    }

    // ==================== 均衡D+2日计划 ====================

    /**
     * 均衡D+2日计划（5~6班=D+2日夜早）。
     *
     * <p>基于差额百分比进行均衡处理，将5班和6班的计划量调整到差额百分比以内。</p>
     */
    private void equilibriumDay3(List<TqScheduleResultVo> scheduleList, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleParams params) {
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        double totalClass5 = totalPlanQtyVo.getTotalClass5PlanQty();
        double totalClass6 = totalPlanQtyVo.getTotalClass6PlanQty();
        double difNum = BigDecimalUtil.sub(totalClass5, totalClass6);
        double totalPlan = BigDecimalUtil.add(totalClass5, totalClass6);
        if (totalPlan == 0) {
            return;
        }
        double actualDifRate = Math.abs(difNum) / totalPlan * 100;

        if (actualDifRate <= BALANCE_DIFF_RATE_THRESHOLD) {
            return;
        }

        boolean isClass5Over = (difNum > 0);
        if (isClass5Over) {
            scheduleList = scheduleList.stream()
                    .sorted(Comparator.comparing(vo -> vo.getClass5PlanQty() == null ? 0D : vo.getClass5PlanQty()))
                    .collect(Collectors.toList());
        } else {
            scheduleList = scheduleList.stream()
                    .sorted(Comparator.comparing(vo -> vo.getClass6PlanQty() == null ? 0D : vo.getClass6PlanQty()))
                    .collect(Collectors.toList());
        }

        double lastDifRate = actualDifRate;
        for (TqScheduleResultVo resultVo : scheduleList) {
            double class5Plan = resultVo.getClass5PlanQty() == null ? 0D : resultVo.getClass5PlanQty();
            double class6Plan = resultVo.getClass6PlanQty() == null ? 0D : resultVo.getClass6PlanQty();

            if (isClass5Over) {
                if (class5Plan == 0) continue;
                double decreasePlanQty = class5Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class5Plan;
                double newTotalClass5 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass5PlanQty(), decreasePlanQty);
                double newTotalClass6 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass6PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass5, newTotalClass6);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass5, newTotalClass6);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) break;
                resultVo.setClass5PlanQty(BigDecimalUtil.sub(class5Plan, decreasePlanQty));
                resultVo.setClass6PlanQty(BigDecimalUtil.add(class6Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass5PlanQty(newTotalClass5);
                totalPlanQtyVo.setTotalClass6PlanQty(newTotalClass6);
                lastDifRate = newDifRate;
            } else {
                if (class6Plan == 0) continue;
                double decreasePlanQty = class6Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class6Plan;
                double newTotalClass5 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass5PlanQty(), decreasePlanQty);
                double newTotalClass6 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass6PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass5, newTotalClass6);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass5, newTotalClass6);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) break;
                resultVo.setClass5PlanQty(BigDecimalUtil.add(class5Plan, decreasePlanQty));
                resultVo.setClass6PlanQty(BigDecimalUtil.sub(class6Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass5PlanQty(newTotalClass5);
                totalPlanQtyVo.setTotalClass6PlanQty(newTotalClass6);
                lastDifRate = newDifRate;
            }
        }

        autoScheduleLogService.insertTqScheduleLog(scheduleList.get(0).getBatchNo(), "",
                "均衡D+2日计划(5~6班)", "均衡后总计划量：" + toJSONString(totalPlanQtyVo));
    }

    // ==================== 工具方法 ====================

    /**
     * 成型消耗量乘以需求系数
     */
    private double mulCxPlan(Integer cxPlan, double coefficient) {
        double plan = cxPlan == null ? 0 : cxPlan;
        return BigDecimalUtil.mul(plan, coefficient);
    }

    /**
     * 获取指定班次的计划量
     */
    private double getClassPlanQty(TqScheduleResultVo scheduleVo, int classNum) {
        switch (classNum) {
            case 1: return scheduleVo.getClass1PlanQty() == null ? 0D : scheduleVo.getClass1PlanQty();
            case 2: return scheduleVo.getClass2PlanQty() == null ? 0D : scheduleVo.getClass2PlanQty();
            case 3: return scheduleVo.getClass3PlanQty() == null ? 0D : scheduleVo.getClass3PlanQty();
            case 4: return scheduleVo.getClass4PlanQty() == null ? 0D : scheduleVo.getClass4PlanQty();
            case 5: return scheduleVo.getClass5PlanQty() == null ? 0D : scheduleVo.getClass5PlanQty();
            case 6: return scheduleVo.getClass6PlanQty() == null ? 0D : scheduleVo.getClass6PlanQty();
            default: return 0D;
        }
    }

    /**
     * 设置指定班次的计划量
     */
    private void setClassPlanQty(TqScheduleResultVo scheduleVo, int classNum, double value) {
        switch (classNum) {
            case 1: scheduleVo.setClass1PlanQty(value); break;
            case 2: scheduleVo.setClass2PlanQty(value); break;
            case 3: scheduleVo.setClass3PlanQty(value); break;
            case 4: scheduleVo.setClass4PlanQty(value); break;
            case 5: scheduleVo.setClass5PlanQty(value); break;
            case 6: scheduleVo.setClass6PlanQty(value); break;
        }
    }
}
