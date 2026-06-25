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
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S2: 需求计算Handler（6班次版本）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>计算库存供应时长（从成型1班开始逐班消耗交接班库存，消耗量×需求系数）</li>
 *   <li>计算胎圈6个班次计划量（基于备库班数、需求系数和机台定额约束综合计算）</li>
 *   <li>设置收尾提示标识和生产状态（基于胎胚关联汇总判断）</li>
 * </ol>
 *
 * <p>注意：均衡逻辑已移至S5(TqBalanceHandler)，停产协调已移至S4(TqStopCoordinationHandler)</p>
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
 * <p>映射规律：胎圈N班 → 供应成型(N+2)班</p>
 *
 * <p>需求计算算法：</p>
 * <ul>
 *   <li>初始交接班库存 = 当前库存 + 当天早班计划量 - 成型1班消耗 - 成型2班消耗</li>
 *   <li>胎圈消耗量 = 成型需求量 × 需求系数(默认2)</li>
 *   <li>排产量 = min(需保证的成型消耗总量(×系数) - 交接班库存, 机台定额总产能)</li>
 *   <li>超出机台定额的需求量延至下一班次累加</li>
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
        return "S2-需求计算";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        TqTotalPlanQtyVo totalPlanQtyVo = context.getTotalPlanQtyVo();

        // 1. 遍历排程列表，计算供应时长、计划量、收尾标识
        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            // 计算库存供应时长（根据算法模式切换计算方式）
            computeSupplyTime(scheduleVo, scheduleVo.getPlanStockQty(), params);

            // 计算胎圈各班计划量（基于备库班数、需求系数和机台定额约束）
            computeTqPlanQty(scheduleVo, totalPlanQtyVo, params, context);

            // 设置收尾提示标识和生产状态（基于胎胚关联汇总判断）
            setStatusAndCloseTip(scheduleVo, context);
        }

        // 1.5 算法1模式下，筛选库存保证班数不足备库班数的规格（算法2不需要筛选）
        if (params.getDemandCalcMode() != null && params.getDemandCalcMode() == 1) {
            double backupShiftCount = params.getBackupShiftCount() == null ? 5D : params.getBackupShiftCount();
            List<TqScheduleResultVo> filteredList = context.getScheduleList().stream()
                    .filter(s -> {
                        double guaranteeShifts = s.getSupplyTime() == null ? 0D : s.getSupplyTime() / 8;
                        return guaranteeShifts < backupShiftCount;
                    })
                    .collect(Collectors.toList());
            context.setScheduleList(filteredList);
            log.info("[S2] 算法1模式筛选：库存保证班数不足{}班的规格数={}", (int) backupShiftCount, filteredList.size());
        }

        // 注意：均衡逻辑已移至S5(TqBalanceHandler)
        // 注意：停产收尾处理已移至S4(TqStopCoordinationHandler)

        log.info("[S2] 需求计算完成, 总计划量:{}", toJSONString(totalPlanQtyVo));
    }

    // ==================== 供应时长计算 ====================

    /**
     * 计算并设置库存供应时长（小时）。
     *
     * <p>根据算法模式切换计算方式：</p>
     * <ul>
     *   <li>算法1（线下手工排产）：库存保证班数 = 14点预计库存 / 胎圈每班需求量（简单除法）</li>
     *   <li>算法2（系统算法）：14点预计库存逐班递减每个班的胎圈需求量直到不够，得出保证班次</li>
     * </ul>
     *
     * @param scheduleVo 排程结果VO
     * @param stockQty 预计库存
     * @param params 排程参数
     */
    private void computeSupplyTime(TqScheduleResultVo scheduleVo, Double stockQty, TqScheduleParams params) {
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();
        Integer demandCalcMode = params.getDemandCalcMode();

        if (demandCalcMode != null && demandCalcMode == 1) {
            // 算法1：库存保证班数 = 14点预计库存 / 胎圈每班需求量（简单除法）
            // 胎圈每班需求量 = 成型三班最大计划量 × 系数
            double cxClass1 = scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan();
            double cxClass2 = scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan();
            double cxClass3 = scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan();
            double maxCxPlan = Math.max(Math.max(cxClass1, cxClass2), cxClass3);
            double tqPerClassDemand = BigDecimalUtil.mul(maxCxPlan, coefficient);

            double stock = stockQty == null ? 0D : stockQty;
            double guaranteeShifts = tqPerClassDemand > 0
                    ? BigDecimalUtil.div(stock, tqPerClassDemand, 1) : 999;
            double supplyTime = BigDecimalUtil.mul(guaranteeShifts, 8);

            scheduleVo.setSupplyTime(supplyTime);
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "计算库存供应时长（算法1-简单除法）",
                    "物料编号：" + scheduleVo.getBeadCode() + "，预计库存：" + stock
                            + "，成型三班最大值：" + maxCxPlan + "，胎圈每班需求量：" + tqPerClassDemand
                            + "，保证班数：" + guaranteeShifts + "，供应时长：" + supplyTime);
        } else {
            // 算法2：逐班递减直到库存不够（原有逻辑）
            computeSupplyTimeByDeduction(scheduleVo, stockQty, coefficient);
        }
    }

    /**
     * 算法2：逐班递减计算库存供应时长。
     *
     * <p>从成型1班开始逐班判断，预计库存-该班消耗量(×需求系数)大于等于0时，供应时长+8小时；
     * 预计库存不足以覆盖该班消耗量时，供应时长加上：(剩余库存/该班消耗×系数)*8小时</p>
     */
    private void computeSupplyTimeByDeduction(TqScheduleResultVo scheduleVo, Double stockQty, double coefficient) {

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

    /**
     * 收尾判断（基于胎胚关联汇总）。
     *
     * <p>一个胎圈可能对应多个胎胚，需考虑所有关联胎胚是否都收尾：</p>
     * <ol>
     *   <li>通过胎圈编码从beadEmbryoMap获取所有关联胎胚编码</li>
     *   <li>按胎胚号group汇总所有胎胚的月计划余量</li>
     *   <li>收尾条件：该胎圈所有关联胎胚的月计划余量都 &lt;= 6班总需排产量</li>
     *   <li>无关联胎胚时走原逻辑（按胎圈编码查月计划余量）</li>
     * </ol>
     *
     * @param scheduleVo 排程结果VO
     * @param context 排程上下文
     * @param totalTqDemand 6班总需排产量
     * @return true=收尾规格，false=非收尾规格
     */
    private boolean checkCloseOutByEmbryo(TqScheduleResultVo scheduleVo, TqScheduleContext context, double totalTqDemand) {
        String beadCode = scheduleVo.getBeadCode();
        List<String> embryoList = context.getBeadEmbryoMap().get(beadCode);

        if (embryoList == null || embryoList.isEmpty()) {
            // 无关联胎胚，走原逻辑：按胎圈编码查月计划余量
            TqMonthSurplusVo monthSurplusVo = context.getMonthSurplusMap().get(beadCode);
            double monthRemainQty = monthSurplusVo == null ? 0D : monthSurplusVo.getMonthRemainQty();
            return monthRemainQty > 0 && monthRemainQty <= totalTqDemand;
        }

        // 按胎胚号group汇总，所有胎胚都满足收尾条件才算收尾
        for (String embryoCode : embryoList) {
            TqMonthSurplusVo embryoSurplus = context.getMonthSurplusMap().get(embryoCode);
            double embryoRemainQty = embryoSurplus == null ? 0D : embryoSurplus.getMonthRemainQty();
            if (embryoRemainQty > totalTqDemand) {
                // 任一胎胚不满足收尾条件，则该胎圈非收尾
                return false;
            }
        }
        return true;
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
                                  TqScheduleParams params, TqScheduleContext context) {
        // 收尾判断（基于胎胚关联汇总）：月计划余量 <= 6班总需排产量 → 收尾规格
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();
        double totalCxConsume = BigDecimalUtil.add(
                scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan(),
                BigDecimalUtil.add(
                        scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan(),
                        BigDecimalUtil.add(
                                scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan(),
                                BigDecimalUtil.add(
                                        scheduleVo.getCxClass6Plan() == null ? 0D : scheduleVo.getCxClass6Plan(),
                                        BigDecimalUtil.add(
                                                scheduleVo.getCxClass7Plan() == null ? 0D : scheduleVo.getCxClass7Plan(),
                                                scheduleVo.getCxClass8Plan() == null ? 0D : scheduleVo.getCxClass8Plan())))));
        double totalTqDemand = BigDecimalUtil.mul(totalCxConsume, coefficient);
        boolean isCloseOutSpec = checkCloseOutByEmbryo(scheduleVo, context, totalTqDemand);
        scheduleVo.setCloseOutSpecFlag(isCloseOutSpec ? "0" : "1"); // 0=收尾，1=非收尾

        double productStockDay = params.getProductStockDay();
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        Double totalConsumeQty = scheduleVo.getSurplusQty();

        // 损耗率乘数：(100% + 损耗率) / 100
        double lossRate = params.getLossRate() == null ? 0D : params.getLossRate();
        double lossRateMultiplier = BigDecimalUtil.div(BigDecimalUtil.add(100, lossRate), 100);

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
                        "需求系数：" + coefficient + "，备库班数：" + params.getBackupShiftCount() + "，损耗率：" + lossRate + "%，损耗率乘数：" + lossRateMultiplier,
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
        // 公式：胎圈N班 = max(0, 成型(N+2)班消耗×系数 - 可用库存) × (100% + 损耗率)
        double class1Plan = 0;
        double class1PureDemand = 0; // 纯需求量（不含损耗），用于滚动库存计算
        if (availableStock < tqConsume3) {
            class1PureDemand = BigDecimalUtil.sub(tqConsume3, availableStock);
            class1Plan = BigDecimalUtil.mul(class1PureDemand, lossRateMultiplier);
        }
        class1Plan = planQtyRounding(scheduleVo, class1Plan, toolCapacity, totalConsumeQty, context);
        scheduleVo.setClass1PlanQty(class1Plan);
        availableStock = BigDecimalUtil.add(availableStock, class1Plan);  // 加上胎圈1班产出
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume3);  // 扣除成型3班胎圈消耗

        // 胎圈2班(D+1日夜班) → 供应成型4班(D+1日早班)
        double class2Plan = 0;
        double class2PureDemand = 0;
        if (availableStock < tqConsume4) {
            class2PureDemand = BigDecimalUtil.sub(tqConsume4, availableStock);
            class2Plan = BigDecimalUtil.mul(class2PureDemand, lossRateMultiplier);
        }
        class2Plan = planQtyRounding(scheduleVo, class2Plan, toolCapacity, totalConsumeQty, context);
        scheduleVo.setClass2PlanQty(class2Plan);
        availableStock = BigDecimalUtil.add(availableStock, class2Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume4);

        // 胎圈3班(D+1日早班) → 供应成型5班(D+1日中班)
        double class3Plan = 0;
        double class3PureDemand = 0;
        if (availableStock < tqConsume5) {
            class3PureDemand = BigDecimalUtil.sub(tqConsume5, availableStock);
            class3Plan = BigDecimalUtil.mul(class3PureDemand, lossRateMultiplier);
        }
        class3Plan = planQtyRounding(scheduleVo, class3Plan, toolCapacity, totalConsumeQty, context);
        scheduleVo.setClass3PlanQty(class3Plan);
        availableStock = BigDecimalUtil.add(availableStock, class3Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume5);

        // 胎圈4班(D+1日中班) → 供应成型6班(D+2日夜班)
        double class4Plan = 0;
        double class4PureDemand = 0;
        if (availableStock < tqConsume6) {
            class4PureDemand = BigDecimalUtil.sub(tqConsume6, availableStock);
            class4Plan = BigDecimalUtil.mul(class4PureDemand, lossRateMultiplier);
        }
        class4Plan = planQtyRounding(scheduleVo, class4Plan, toolCapacity, totalConsumeQty, context);
        scheduleVo.setClass4PlanQty(class4Plan);
        availableStock = BigDecimalUtil.add(availableStock, class4Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume6);

        // 胎圈5班(D+2日夜班) → 供应成型7班(D+2日早班)
        double class5Plan = 0;
        double class5PureDemand = 0;
        if (availableStock < tqConsume7) {
            class5PureDemand = BigDecimalUtil.sub(tqConsume7, availableStock);
            class5Plan = BigDecimalUtil.mul(class5PureDemand, lossRateMultiplier);
        }
        class5Plan = planQtyRounding(scheduleVo, class5Plan, toolCapacity, totalConsumeQty, context);
        scheduleVo.setClass5PlanQty(class5Plan);
        availableStock = BigDecimalUtil.add(availableStock, class5Plan);
        availableStock = BigDecimalUtil.sub(availableStock, tqConsume7);

        // 胎圈6班(D+2日早班) → 供应成型8班(D+2日中班)，滚动排程，按预生产库存天数计算目标交接班库存
        double nextDayCxConsume = BigDecimalUtil.add(tqConsume7, tqConsume8); // 预估下一天胎圈消耗
        double targetStock = BigDecimalUtil.roundDown(BigDecimalUtil.mul(nextDayCxConsume, productStockDay), 0);
        double class6Plan = 0;
        double class6PureDemand = 0;
        if (targetStock > availableStock) {
            class6PureDemand = BigDecimalUtil.sub(targetStock, availableStock);
            class6Plan = BigDecimalUtil.mul(class6PureDemand, lossRateMultiplier);
        }
        class6Plan = planQtyRounding(scheduleVo, class6Plan, toolCapacity, totalConsumeQty, context);
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
     * 计划量取整 + 工装限制。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>非收尾：按工装容量向上取整</li>
     *   <li>收尾：min(需排产量, 月计划余量) × (100% + 损耗率)</li>
     *   <li>工装限制：可用工装数量 = 工装总数 - 库存/整车个数；需排产量 = min(可用工装数量 × 整车个数, 需排产量)</li>
     *   <li>如果超过月度剩余量，则截断到月度剩余量</li>
     * </ul>
     */
    private double planQtyRounding(TqScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
                                   Double totalConsumeQty, TqScheduleContext context) {
        if (planQty <= 0) {
            return 0D;
        }

        boolean isCloseOutSpec = "0".equals(scheduleVo.getCloseOutSpecFlag());
        double lossRate = context.getParams().getLossRate() == null ? 0D : context.getParams().getLossRate();

        if (isCloseOutSpec) {
            // 收尾规格：min(需排产量, 月计划余量) × (100% + 损耗率)
            if (totalConsumeQty != null && totalConsumeQty > 0 && planQty > totalConsumeQty) {
                planQty = totalConsumeQty;
            }
            double rate = BigDecimalUtil.div(BigDecimalUtil.add(100, lossRate), 100);
            planQty = BigDecimalUtil.mul(planQty, rate);
        } else {
            // 非收尾规格：按工装容量向上取整
            if (toolCapacity.doubleValue() > 0 && planQty > 0) {
                planQty = Math.ceil(planQty / toolCapacity.doubleValue()) * toolCapacity.doubleValue();
            }
            // 超过月度剩余量则截断
            if (totalConsumeQty != null && totalConsumeQty > 0 && planQty > totalConsumeQty) {
                planQty = totalConsumeQty;
            }
        }

        // 工装限制：工装车总数从参数配置获取（全局统一值），整车容量按胎圈编码从容量表获取
        // 可用工装数量 = 工装车总数 - 库存/整车个数；需排产量 = min(可用工装数量 × 整车个数, 需排产量)
        String beadCode = scheduleVo.getBeadCode();
        Integer toolingTotal = context.getParams().getToolingTotal();
        Integer cartCapacity = context.getCartCapacityMap().get(beadCode);
        if (toolingTotal != null && toolingTotal > 0 && cartCapacity != null && cartCapacity > 0) {
            double stockQty = scheduleVo.getStockQty() == null ? 0D : scheduleVo.getStockQty();
            double usedTooling = Math.ceil(stockQty / cartCapacity);
            double availableTooling = Math.max(0, toolingTotal - usedTooling);
            double maxPlanByTooling = availableTooling * cartCapacity;
            if (planQty > maxPlanByTooling) {
                planQty = maxPlanByTooling;
            }
        }

        return planQty;
    }

    // ==================== 收尾标识与生产状态 ====================

    /**
     * 设置收尾提示标识和生产状态字段（基于胎胚关联汇总判断）。
     */
    private void setStatusAndCloseTip(TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        TqMonthSurplusVo monthSurplusVo = context.getMonthSurplusMap().get(scheduleVo.getBeadCode());
        Double closeOutNum = context.getParams().getCloseOutNum();

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

    // ==================== 工具方法 ====================

    private String logSplit(String... messages) {
        StringBuilder sb = new StringBuilder();
        for (String msg : messages) {
            sb.append(msg).append(DIVISION);
        }
        return sb.toString();
    }
}
