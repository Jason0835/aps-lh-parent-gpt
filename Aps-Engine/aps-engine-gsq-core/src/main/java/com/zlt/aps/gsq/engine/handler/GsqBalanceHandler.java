package com.zlt.aps.gsq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleResultEnum;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S5: 钢丝圈班次均衡调整Handler（无备库版）。
 *
 * <p>职责（对齐胎圈TQ的差额百分比均衡）：</p>
 * <ol>
 *   <li>均衡D+1日计划（2~4班）：基于2班与3班差额百分比调整</li>
 *   <li>均衡D+2日计划（5~6班）：基于5班与6班差额百分比调整</li>
 * </ol>
 *
 * <p>均衡策略：</p>
 * <ul>
 *   <li>各规格各班次产量受同机台定额约束（由S5.5定额校验统一兜底）</li>
 *   <li>同日内各班次产量差额百分比控制在{@link #BALANCE_DIFF_RATE_THRESHOLD}%以内</li>
 *   <li>按差额百分比调整：超额班次的多余量以"工装车整车容量"为步长转移给不足班次</li>
 * </ul>
 *
 * <p>无备库版：钢丝圈当前无备库字段与概念，故不包含胎圈"S5均衡跳过备库规格"的逻辑。</p>
 *
 * <p>入参口径说明：钢丝圈无成型cx数据，均衡仅使用各班次总计划量
 * （{@link GsqTotalPlanQtyVo}）与各规格分班次计划量，不依赖胎圈消耗量。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqBalanceHandler extends AbsGsqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /** 均衡差额百分比阈值 */
    private static final double BALANCE_DIFF_RATE_THRESHOLD = 5.0;

    @Override
    protected String getStepName() {
        return "S5-班次均衡调整";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        GsqTotalPlanQtyVo totalPlanQtyVo = context.getTotalPlanQtyVo();
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return;
        }

        // 1. 均衡D+1日计划（2~4班=D+1日夜早中）
        equilibriumDay2(scheduleList, totalPlanQtyVo, context);

        // 2. 均衡D+2日计划（5~6班=D+2日夜早）
        equilibriumDay3(scheduleList, totalPlanQtyVo, context);

        log.info("[S5] 班次均衡调整完成, 总计划量:{}", toJSONString(totalPlanQtyVo));

        // 埋点按日均衡调整证据（每条排程记录都受影响）
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("totalPlanQtyVo", totalPlanQtyVo);
            context.getRuleTrace(scheduleVo.getSteelRingCode()).addRuleHit(
                    GsqScheduleRuleCodeEnum.DAILY_BALANCE_ADJUST,
                    GsqScheduleRuleResultEnum.ADJUST, evidence);
        }
    }

    // ==================== 均衡D+1日计划 ====================

    /**
     * 均衡D+1日计划（2~4班=D+1日夜早中）。
     *
     * <p>基于差额百分比进行均衡处理，将2班和3班的计划量调整到差额百分比以内。</p>
     *
     * @param scheduleList     排程列表
     * @param totalPlanQtyVo   各班次总计划量
     * @param context          排程上下文
     */
    private void equilibriumDay2(List<GsqScheduleResultVo> scheduleList, GsqTotalPlanQtyVo totalPlanQtyVo,
                                 GsqScheduleContext context) {
        double toolCapacity = getToolCapacity(context);
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
        log.info("[S5] equilibriumDay2 触发! totalClass2={} totalClass3={} difRate={}% isClass2Over={}",
                totalClass2, totalClass3, actualDifRate, isClass2Over);

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
        for (GsqScheduleResultVo resultVo : scheduleList) {
            // 跳过未分配机台的记录，避免给空机台记录注入计划量
            if (StringUtils.isEmpty(resultVo.getMachineCode())) {
                continue;
            }
            // 对齐胎圈：S5均衡跳过备库触发规格，避免削减备库触发班次计划量
            if (resultVo.getBackupTriggerClass() != null && resultVo.getBackupTriggerClass() > 0) {
                continue;
            }
            double class2Plan = resultVo.getClass2PlanQty() == null ? 0D : resultVo.getClass2PlanQty();
            double class3Plan = resultVo.getClass3PlanQty() == null ? 0D : resultVo.getClass3PlanQty();

            if (isClass2Over) {
                if (class2Plan == 0) {
                    continue;
                }
                double decreasePlanQty = class2Plan > toolCapacity ? toolCapacity : class2Plan;
                double newTotalClass2 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass2PlanQty(), decreasePlanQty);
                double newTotalClass3 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass3PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass2, newTotalClass3);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass2, newTotalClass3);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) {
                    break;
                }
                resultVo.setClass2PlanQty(BigDecimalUtil.sub(class2Plan, decreasePlanQty));
                resultVo.setClass3PlanQty(BigDecimalUtil.add(class3Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass2PlanQty(newTotalClass2);
                totalPlanQtyVo.setTotalClass3PlanQty(newTotalClass3);
                lastDifRate = newDifRate;
            } else {
                if (class3Plan == 0) {
                    continue;
                }
                double decreasePlanQty = class3Plan > toolCapacity ? toolCapacity : class3Plan;
                double newTotalClass2 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass2PlanQty(), decreasePlanQty);
                double newTotalClass3 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass3PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass2, newTotalClass3);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass2, newTotalClass3);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) {
                    break;
                }
                resultVo.setClass2PlanQty(BigDecimalUtil.add(class2Plan, decreasePlanQty));
                resultVo.setClass3PlanQty(BigDecimalUtil.sub(class3Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass2PlanQty(newTotalClass2);
                totalPlanQtyVo.setTotalClass3PlanQty(newTotalClass3);
                lastDifRate = newDifRate;
            }
        }

        autoScheduleLogService.insertGsqScheduleLog(scheduleList.get(0).getBatchNo(), "",
                "均衡D+1日计划(2~4班)", "均衡后总计划量：" + toJSONString(totalPlanQtyVo));
    }

    // ==================== 均衡D+2日计划 ====================

    /**
     * 均衡D+2日计划（5~6班=D+2日夜早）。
     *
     * <p>基于差额百分比进行均衡处理，将5班和6班的计划量调整到差额百分比以内。</p>
     *
     * @param scheduleList     排程列表
     * @param totalPlanQtyVo   各班次总计划量
     * @param context          排程上下文
     */
    private void equilibriumDay3(List<GsqScheduleResultVo> scheduleList, GsqTotalPlanQtyVo totalPlanQtyVo,
                                 GsqScheduleContext context) {
        double toolCapacity = getToolCapacity(context);
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
        log.info("[S5] equilibriumDay3 触发! totalClass5={} totalClass6={} difRate={}% isClass5Over={}",
                totalClass5, totalClass6, actualDifRate, isClass5Over);

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
        for (GsqScheduleResultVo resultVo : scheduleList) {
            // 跳过未分配机台的记录，避免给空机台记录注入计划量
            if (StringUtils.isEmpty(resultVo.getMachineCode())) {
                continue;
            }
            // 对齐胎圈：S5均衡跳过备库触发规格，避免削减备库触发班次计划量
            if (resultVo.getBackupTriggerClass() != null && resultVo.getBackupTriggerClass() > 0) {
                continue;
            }
            double class5Plan = resultVo.getClass5PlanQty() == null ? 0D : resultVo.getClass5PlanQty();
            double class6Plan = resultVo.getClass6PlanQty() == null ? 0D : resultVo.getClass6PlanQty();

            if (isClass5Over) {
                if (class5Plan == 0) {
                    continue;
                }
                double decreasePlanQty = class5Plan > toolCapacity ? toolCapacity : class5Plan;
                double newTotalClass5 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass5PlanQty(), decreasePlanQty);
                double newTotalClass6 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass6PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass5, newTotalClass6);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass5, newTotalClass6);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) {
                    break;
                }
                resultVo.setClass5PlanQty(BigDecimalUtil.sub(class5Plan, decreasePlanQty));
                resultVo.setClass6PlanQty(BigDecimalUtil.add(class6Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass5PlanQty(newTotalClass5);
                totalPlanQtyVo.setTotalClass6PlanQty(newTotalClass6);
                lastDifRate = newDifRate;
            } else {
                if (class6Plan == 0) {
                    continue;
                }
                double decreasePlanQty = class6Plan > toolCapacity ? toolCapacity : class6Plan;
                double newTotalClass5 = BigDecimalUtil.add(totalPlanQtyVo.getTotalClass5PlanQty(), decreasePlanQty);
                double newTotalClass6 = BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass6PlanQty(), decreasePlanQty);
                double newDifNum = BigDecimalUtil.sub(newTotalClass5, newTotalClass6);
                double newTotalPlan = BigDecimalUtil.add(newTotalClass5, newTotalClass6);
                double newDifRate = newTotalPlan > 0 ? Math.abs(newDifNum) / newTotalPlan * 100 : 0;
                if (newDifRate >= lastDifRate) {
                    break;
                }
                resultVo.setClass5PlanQty(BigDecimalUtil.add(class5Plan, decreasePlanQty));
                resultVo.setClass6PlanQty(BigDecimalUtil.sub(class6Plan, decreasePlanQty));
                totalPlanQtyVo.setTotalClass5PlanQty(newTotalClass5);
                totalPlanQtyVo.setTotalClass6PlanQty(newTotalClass6);
                lastDifRate = newDifRate;
            }
        }

        autoScheduleLogService.insertGsqScheduleLog(scheduleList.get(0).getBatchNo(), "",
                "均衡D+2日计划(5~6班)", "均衡后总计划量：" + toJSONString(totalPlanQtyVo));
    }

    // ==================== 工具方法 ====================

    /**
     * 获取工装车整车容量（作为均衡步长），默认120。
     */
    private double getToolCapacity(GsqScheduleContext context) {
        Double toolCapacity = context.getParams().getToolCapacity();
        return toolCapacity == null || toolCapacity <= 0 ? 120D : toolCapacity;
    }
}