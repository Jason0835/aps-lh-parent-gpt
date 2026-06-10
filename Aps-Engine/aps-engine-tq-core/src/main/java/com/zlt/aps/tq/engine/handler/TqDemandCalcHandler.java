package com.zlt.aps.tq.engine.handler;

import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqMonthSurplusVo;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S2: 需求计算与均衡Handler（6班次版本）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>计算库存供应时长（从成型1班开始逐班消耗预计库存，消耗量×需求系数）</li>
 *   <li>计算胎圈6个班次计划量（基于备库班数和需求系数综合计算）</li>
 *   <li>设置收尾提示标识和生产状态</li>
 *   <li>均衡各班计划（交接班库存平衡）</li>
 * </ol>
 *
 * <p>班次与实际时间对应关系（D=排程日期-2，即今天）：</p>
 * <ul>
 *   <li>胎圈1班：D日中班(14:00-22:00)    → 供应成型3班(D+1日夜班)</li>
 *   <li>胎圈2班：D+1日夜班(22:00-6:00)   → 供应成型4班(D+1日早班)</li>
 *   <li>胎圈3班：D+1日早班(6:00-14:00)   → 供应成型5班(D+1日中班)</li>
 *   <li>胎圈4班：D+1日中班(14:00-22:00)  → 供应成型6班(D+2日夜班)</li>
 *   <li>胎圈5班：D+2日夜班(22:00-6:00)   → 供应成型7班(D+2日早班)</li>
 *   <li>胎圈6班：D+2日早班(6:00-14:00)   → 供应成型8班(D+2日中班)，滚动排程</li>
 * </ul>
 *
 * <p>成型8班覆盖：D日早班、D日中班、D+1日夜早中、D+2日夜早中</p>
 * <p>胎圈6班覆盖：D日中班、D+1日夜早中、D+2日夜早</p>
 * <p>映射规律：胎圈N班 → 供应成型(N+2)班</p>
 *
 * <p>需求计算算法：</p>
 * <ul>
 *   <li>初始可用库存 = 当前库存 + 当天早班计划量（昨天已排的、属于今天早班的胎圈计划量）</li>
 *   <li>胎圈消耗量 = 成型需求量 × 需求系数(默认2)</li>
 *   <li>排产量 = 需保证的成型消耗总量(×系数) - 可用库存</li>
 * </ul>
 *
 * <p>该Handler只读取Context中的数据，不依赖外部Service，是纯算法实现。</p>
 */
@Slf4j
@Component
public class TqDemandCalcHandler extends AbsTqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    private static final String DIVISION = "\r\n---------------------------------------------------\r\n";

    @Override
    protected String getStepName() {
        return "S2-需求计算与均衡";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        TqTotalPlanQtyVo totalPlanQtyVo = context.getTotalPlanQtyVo();

        // 1. 遍历排程列表，计算供应时长、计划量、收尾标识
        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            // 计算库存供应时长（用预计库存计算，消耗量乘需求系数）
            computeSupplyTime(scheduleVo, scheduleVo.getPlanStockQty(), params.getDemandCoefficient());

            // 计算胎圈各班计划量（基于备库班数和需求系数）
            computeTqPlanQty(scheduleVo, totalPlanQtyVo, params);

            // 设置收尾提示标识和生产状态
            setStatusAndCloseTip(scheduleVo, context.getMonthSurplusMap().get(scheduleVo.getBeadCode()), params.getCloseOutNum());
        }

        // 2. 均衡D日计划（1班=D日中班）
        equilibriumDay1(context.getScheduleList(), totalPlanQtyVo, params);

        // 3. 均衡D+1日计划（2~4班=D+1日夜早中）
        equilibriumDay2(context.getScheduleList(), totalPlanQtyVo, params);

        // 4. 均衡D+2日计划（5~6班=D+2日夜早）
        equilibriumDay3(context.getScheduleList(), totalPlanQtyVo, params);

        log.info("[S2] 需求计算与均衡完成, 总计划量:{}", toJSONString(totalPlanQtyVo));
    }

    // ==================== 供应时长计算 ====================

    /**
     * 计算并设置供成型库存供应时长（小时）。
     *
     * <p>具体算法：从成型1班开始逐班判断，预计库存-该班消耗量(×需求系数)大于等于0时，供应时长+8小时；
     * 预计库存不足以覆盖该班消耗量时，供应时长加上：(剩余库存/该班消耗×系数)*8小时</p>
     *
     * @param scheduleVo 排程结果VO
     * @param stockQty 预计库存
     * @param demandCoefficient 需求系数（胎圈消耗量=成型需求量×系数）
     */
    private void computeSupplyTime(TqScheduleResultVo scheduleVo, Double stockQty, Double demandCoefficient) {
        double coefficient = demandCoefficient == null ? 2D : demandCoefficient;

        // 成型8个班的消耗量（胎圈消耗量 = 成型需求量 × 系数）
        double cxClass1 = mulCxPlan(scheduleVo.getCxClass1Plan(), coefficient);
        double cxClass2 = mulCxPlan(scheduleVo.getCxClass2Plan(), coefficient);
        double cxClass3 = mulCxPlan(scheduleVo.getCxClass3Plan(), coefficient);
        double cxClass4 = mulCxPlan(scheduleVo.getCxClass4Plan(), coefficient);
        double cxClass5 = mulCxPlan(scheduleVo.getCxClass5Plan(), coefficient);
        double cxClass6 = mulCxPlan(scheduleVo.getCxClass6Plan(), coefficient);
        double cxClass7 = mulCxPlan(scheduleVo.getCxClass7Plan(), coefficient);
        double cxClass8 = mulCxPlan(scheduleVo.getCxClass8Plan(), coefficient);

        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算库存供应时长前数据",
                logSplit("具体算法：从成型1班开始逐班判断，预计库存-该班消耗量(×需求系数" + coefficient + ")大于等于0时，供应时长+8小时；预计库存不足以覆盖该班消耗量时，供应时长加上：(剩余库存/该班消耗×系数)*8小时",
                        "物料编号：" + scheduleVo.getBeadCode() + "，预计库存：" + stockQty,
                        "成型1班消耗(×系数)：" + cxClass1 + "，成型2班消耗(×系数)：" + cxClass2,
                        "成型3班消耗(×系数)：" + cxClass3 + "，成型4班消耗(×系数)：" + cxClass4,
                        "成型5班消耗(×系数)：" + cxClass5 + "，成型6班消耗(×系数)：" + cxClass6,
                        "成型7班消耗(×系数)：" + cxClass7 + "，成型8班消耗(×系数)：" + cxClass8));

        double remnantStock = stockQty == null ? 0D : stockQty;

        // 逐班计算供应时长（8小时/班），覆盖成型8个班
        double[] cxClassPlans = {cxClass1, cxClass2, cxClass3, cxClass4, cxClass5, cxClass6, cxClass7, cxClass8};
        for (double classPlan : cxClassPlans) {
            remnantStock = BigDecimalUtil.sub(remnantStock, classPlan);
            if (remnantStock >= 0) {
                // 剩余库存仍可覆盖该班消耗，供应时长+8小时
                Double supplyTime = scheduleVo.getSupplyTime() == null ? 0D : scheduleVo.getSupplyTime();
                scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 8));
            } else {
                // 剩余库存不足以覆盖该班消耗
                Double supplyTime = scheduleVo.getSupplyTime() == null ? 0D : scheduleVo.getSupplyTime();
                double classSupplyTime = BigDecimalUtil.mul(BigDecimalUtil.div(BigDecimalUtil.add(remnantStock, classPlan), classPlan), 8);
                supplyTime = supplyTime + BigDecimalUtil.roundDown(classSupplyTime, 1);
                scheduleVo.setSupplyTime(supplyTime);
                autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                        "计算库存供应时长结束",
                        "物料编号：" + scheduleVo.getBeadCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
                return;
            }
        }

        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算库存供应时长结束",
                "物料编号：" + scheduleVo.getBeadCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 成型消耗量乘以需求系数
     */
    private double mulCxPlan(Double cxPlan, double coefficient) {
        double plan = cxPlan == null ? 0D : cxPlan;
        return BigDecimalUtil.mul(plan, coefficient);
    }

    // ==================== 计划量计算（6班次，基于备库班数和需求系数） ====================

    /**
     * 计算胎圈6个班次计划量。
     *
     * <p>核心逻辑：基于备库班数和需求系数，逐班滚动计算。</p>
     * <ul>
     *   <li>初始可用库存 = 当前库存 + 当天早班计划量（昨天已排的、属于今天早班的胎圈计划量）</li>
     *   <li>胎圈消耗量 = 成型需求量 × 需求系数(默认2)</li>
     *   <li>胎圈1班 → 供应成型3班(D+1日夜班)</li>
     *   <li>胎圈2班 → 供应成型4班(D+1日早班)</li>
     *   <li>胎圈3班 → 供应成型5班(D+1日中班)</li>
     *   <li>胎圈4班 → 供应成型6班(D+2日夜班)</li>
     *   <li>胎圈5班 → 供应成型7班(D+2日早班)</li>
     *   <li>胎圈6班 → 供应成型8班(D+2日中班)，滚动排程</li>
     * </ul>
     *
     * <p>成型1班(D日早班)和成型2班(D日中班)由库存直接供应：</p>
     * <ul>
     *   <li>成型1班消耗：库存直接供应</li>
     *   <li>成型2班消耗：库存+当天早班产出供应</li>
     * </ul>
     */
    private void computeTqPlanQty(TqScheduleResultVo scheduleVo, TqTotalPlanQtyVo totalPlanQtyVo,
                                  TqScheduleParams params) {
        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 收尾标记默认非收尾

        double productStockDay = params.getProductStockDay();
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        Double totalConsumeQty = scheduleVo.getSurplusQty();
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();

        Double stockQty = scheduleVo.getStockQty() == null ? 0D : scheduleVo.getStockQty();
        Double todayMorningPlanQty = scheduleVo.getTodayMorningPlanQty() == null ? 0D : scheduleVo.getTodayMorningPlanQty();

        // 成型各班消耗量（原始值）
        double cxClass1 = scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan();
        double cxClass2 = scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan();
        double cxClass3 = scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan();
        double cxClass4 = scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan();
        double cxClass5 = scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan();
        double cxClass6 = scheduleVo.getCxClass6Plan() == null ? 0D : scheduleVo.getCxClass6Plan();
        double cxClass7 = scheduleVo.getCxClass7Plan() == null ? 0D : scheduleVo.getCxClass7Plan();
        double cxClass8 = scheduleVo.getCxClass8Plan() == null ? 0D : scheduleVo.getCxClass8Plan();

        // 成型各班消耗量 × 需求系数 = 胎圈消耗量
        double tqConsume1 = BigDecimalUtil.mul(cxClass1, coefficient);
        double tqConsume2 = BigDecimalUtil.mul(cxClass2, coefficient);
        double tqConsume3 = BigDecimalUtil.mul(cxClass3, coefficient);
        double tqConsume4 = BigDecimalUtil.mul(cxClass4, coefficient);
        double tqConsume5 = BigDecimalUtil.mul(cxClass5, coefficient);
        double tqConsume6 = BigDecimalUtil.mul(cxClass6, coefficient);
        double tqConsume7 = BigDecimalUtil.mul(cxClass7, coefficient);
        double tqConsume8 = BigDecimalUtil.mul(cxClass8, coefficient);

        // 初始可用库存 = 当前库存 + 当天早班计划量（昨天已排的、属于今天早班的胎圈计划量）
        double availableStock = BigDecimalUtil.add(stockQty, todayMorningPlanQty);

        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算胎圈6班计划量-初始数据",
                logSplit("物料编号：" + scheduleVo.getBeadCode(),
                        "需求系数：" + coefficient + "，备库班数：" + params.getBackupShiftCount(),
                        "当前库存：" + stockQty + "，当天早班计划量：" + todayMorningPlanQty + "，初始可用库存：" + availableStock,
                        "成型1班消耗：" + cxClass1 + "(胎圈消耗" + tqConsume1 + ")，成型2班消耗：" + cxClass2 + "(胎圈消耗" + tqConsume2 + ")",
                        "成型3班消耗：" + cxClass3 + "(胎圈消耗" + tqConsume3 + ")，成型4班消耗：" + cxClass4 + "(胎圈消耗" + tqConsume4 + ")",
                        "成型5班消耗：" + cxClass5 + "(胎圈消耗" + tqConsume5 + ")，成型6班消耗：" + cxClass6 + "(胎圈消耗" + tqConsume6 + ")",
                        "成型7班消耗：" + cxClass7 + "(胎圈消耗" + tqConsume7 + ")，成型8班消耗：" + cxClass8 + "(胎圈消耗" + tqConsume8 + ")"));

        // 成型1班(D日早班)消耗由库存直接供应
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume1);

        // 成型2班(D日中班)消耗由库存+当天早班产出供应
        // 当天早班产出已经在todayMorningPlanQty中计入初始库存，这里直接扣除消耗
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume2);

        // 胎圈1班(D日中班) → 供应成型3班(D+1日夜班)
        double class1Plan = 0;
        if (availableStock < tqConsume3) {
            class1Plan = BigDecimalUtil.sub(tqConsume3, availableStock);
        }
        class1Plan = planQtyRounding(scheduleVo, class1Plan, toolCapacity, totalConsumeQty);
        scheduleVo.setClass1PlanQty(class1Plan);
        availableStock = BigDecimalUtil.add(availableStock, class1Plan);  // 加上胎圈1班产出
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume3);  // 扣除成型3班胎圈消耗

        // 胎圈2班(D+1日夜班) → 供应成型4班(D+1日早班)
        double class2Plan = 0;
        if (availableStock < tqConsume4) {
            class2Plan = BigDecimalUtil.sub(tqConsume4, availableStock);
        }
        class2Plan = planQtyRounding(scheduleVo, class2Plan, toolCapacity, totalConsumeQty);
        scheduleVo.setClass2PlanQty(class2Plan);
        availableStock = BigDecimalUtil.add(availableStock, class2Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume4);

        // 胎圈3班(D+1日早班) → 供应成型5班(D+1日中班)
        double class3Plan = 0;
        if (availableStock < tqConsume5) {
            class3Plan = BigDecimalUtil.sub(tqConsume5, availableStock);
        }
        class3Plan = planQtyRounding(scheduleVo, class3Plan, toolCapacity, totalConsumeQty);
        scheduleVo.setClass3PlanQty(class3Plan);
        availableStock = BigDecimalUtil.add(availableStock, class3Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume5);

        // 胎圈4班(D+1日中班) → 供应成型6班(D+2日夜班)
        double class4Plan = 0;
        if (availableStock < tqConsume6) {
            class4Plan = BigDecimalUtil.sub(tqConsume6, availableStock);
        }
        class4Plan = planQtyRounding(scheduleVo, class4Plan, toolCapacity, totalConsumeQty);
        scheduleVo.setClass4PlanQty(class4Plan);
        availableStock = BigDecimalUtil.add(availableStock, class4Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume6);

        // 胎圈5班(D+2日夜班) → 供应成型7班(D+2日早班)
        double class5Plan = 0;
        if (availableStock < tqConsume7) {
            class5Plan = BigDecimalUtil.sub(tqConsume7, availableStock);
        }
        class5Plan = planQtyRounding(scheduleVo, class5Plan, toolCapacity, totalConsumeQty);
        scheduleVo.setClass5PlanQty(class5Plan);
        availableStock = BigDecimalUtil.add(availableStock, class5Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume7);

        // 胎圈6班(D+2日早班) → 供应成型8班(D+2日中班)，滚动排程，按预生产库存天数计算目标交接班库存
        double nextDayCxConsume = BigDecimalUtil.add(tqConsume7, tqConsume8); // 预估下一天胎圈消耗
        double targetStock = BigDecimalUtil.roundDown(BigDecimalUtil.mul(nextDayCxConsume, productStockDay), 0);
        double class6Plan = 0;
        if (targetStock > availableStock) {
            class6Plan = BigDecimalUtil.sub(targetStock, availableStock);
        }
        class6Plan = planQtyRounding(scheduleVo, class6Plan, toolCapacity, totalConsumeQty);
        scheduleVo.setClass6PlanQty(class6Plan);

        // 保存交接班库存（6班滚动计算后的最终库存结余）
        scheduleVo.setClassStock(BigDecimalUtil.add(availableStock, class6Plan));

        // 计算供需比率：交接班库存 / 成型一天胎圈消耗量
        double oneDayTqConsume = BigDecimalUtil.add(tqConsume6, BigDecimalUtil.add(tqConsume7, tqConsume8));
        scheduleVo.setSupplyDemandRatio(oneDayTqConsume > 0 ? BigDecimalUtil.div(scheduleVo.getClassStock(), oneDayTqConsume, 4) : 0);

        // 累加总计划量
        totalPlanQtyVo.setTotalClass1PlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalClass1PlanQty(), class1Plan));
        totalPlanQtyVo.setTotalClass2PlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalClass2PlanQty(), class2Plan));
        totalPlanQtyVo.setTotalClass3PlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalClass3PlanQty(), class3Plan));
        totalPlanQtyVo.setTotalClass4PlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalClass4PlanQty(), class4Plan));
        totalPlanQtyVo.setTotalClass5PlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalClass5PlanQty(), class5Plan));
        totalPlanQtyVo.setTotalClass6PlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalClass6PlanQty(), class6Plan));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalPlanQty(),
                BigDecimalUtil.add(BigDecimalUtil.add(class1Plan, class2Plan),
                        BigDecimalUtil.add(BigDecimalUtil.add(class3Plan, class4Plan),
                                BigDecimalUtil.add(class5Plan, class6Plan)))));

        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算胎圈6班计划量",
                logSplit("物料编号：" + scheduleVo.getBeadCode(),
                        "1班计划量：" + class1Plan + "，2班计划量：" + class2Plan + "，3班计划量：" + class3Plan,
                        "4班计划量：" + class4Plan + "，5班计划量：" + class5Plan + "，6班计划量：" + class6Plan,
                        "交接班库存：" + scheduleVo.getClassStock() + "，供需比率：" + scheduleVo.getSupplyDemandRatio()));
    }

    /**
     * 计划量整车取整。
     *
     * <p>规则：按工装容量向上取整。如果超过月度剩余量，则截断到月度剩余量。</p>
     */
    private double planQtyRounding(TqScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
                                   Double totalConsumeQty) {
        if (planQty <= 0) {
            return 0D;
        }
        // 整车取整：向上取整到工装容量的整数倍
        if (toolCapacity.doubleValue() > 0 && planQty > 0) {
            planQty = Math.ceil(planQty / toolCapacity.doubleValue()) * toolCapacity.doubleValue();
        }

        // 如果超过月度剩余量，截断
        if (totalConsumeQty != null && totalConsumeQty > 0 && planQty > totalConsumeQty) {
            planQty = totalConsumeQty;
        }

        return planQty;
    }

    // ==================== 收尾标识与生产状态 ====================

    /**
     * 设置收尾提示标识和生产状态字段。
     */
    private void setStatusAndCloseTip(TqScheduleResultVo scheduleVo, TqMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if (monthSurplusVo == null) {
            scheduleVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
            log.error("月计划汇总数据为空，物料编号为：{}", scheduleVo.getBeadCode());
            return;
        }

        Double monthRemainQty = monthSurplusVo.getMonthRemainQty();
        if (monthRemainQty < closeOutNum) {
            scheduleVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NEED);
        } else {
            scheduleVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
        }

        Double monthFinishQty = monthSurplusVo.getMonthFinishQty();
        if (monthFinishQty == 0D) {
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
        } else if (monthFinishQty > 0D && monthRemainQty > 0) {
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_ING);
        } else if (monthRemainQty <= 0) {
            scheduleVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_FINISH);
        }
    }

    // ==================== 均衡处理 ====================

    /**
     * 均衡D日计划（1班=D日中班）。
     *
     * <p>基于交接班库存平衡基准值，调整1班的计划量。</p>
     */
    private void equilibriumDay1(List<TqScheduleResultVo> scheduleList, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleParams params) {
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        double classStockReference = params.getClassStockReference();
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();

        // 计算D日结束时的库存结余（1班结束后的库存）
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
            // 差异少于一车，不需要处理
            return;
        }

        boolean isOverStock = difStock > 0; // 库存是否超量

        // 排序：库存超量时按1班计划量倒序（优先减少1班），库存不足时按1班计划量顺序（优先增加1班）
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
                // 库存超量，减少1班计划量
                double decreaseQty = class1Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class1Plan;
                double newClass1Plan = BigDecimalUtil.sub(class1Plan, decreaseQty);
                scheduleVo.setClass1PlanQty(newClass1Plan);
                totalPlanQtyVo.setTotalClass1PlanQty(BigDecimalUtil.sub(totalPlanQtyVo.getTotalClass1PlanQty(), decreaseQty));
                totalClassStock = BigDecimalUtil.sub(totalClassStock, decreaseQty);
            } else if (!isOverStock) {
                // 库存不足，增加1班计划量
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

        // 差额百分比超过5%才需要均衡
        if (actualDifRate <= 5) {
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
                if (class2Plan == 0) {
                    continue;
                }
                double decreasePlanQty = class2Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class2Plan;
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
                double decreasePlanQty = class3Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class3Plan;
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

        autoScheduleLogService.insertTqScheduleLog(scheduleList.get(0).getBatchNo(), "",
                "均衡D+1日计划(2~4班)", "均衡后总计划量：" + toJSONString(totalPlanQtyVo));
    }

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

        // 差额百分比超过5%才需要均衡
        if (actualDifRate <= 5) {
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
                if (class5Plan == 0) {
                    continue;
                }
                double decreasePlanQty = class5Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class5Plan;
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
                double decreasePlanQty = class6Plan > toolCapacity.doubleValue() ? toolCapacity.doubleValue() : class6Plan;
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

        autoScheduleLogService.insertTqScheduleLog(scheduleList.get(0).getBatchNo(), "",
                "均衡D+2日计划(5~6班)", "均衡后总计划量：" + toJSONString(totalPlanQtyVo));
    }

    // ==================== 工具方法 ====================

    private String logSplit(String... messages) {
        StringBuilder sb = new StringBuilder();
        for (String msg : messages) {
            sb.append(msg).append(DIVISION);
        }
        return sb.toString();
    }
}
