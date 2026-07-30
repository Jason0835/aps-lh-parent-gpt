package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum;
import com.zlt.aps.tq.engine.strategy.ITqPlanQtyStrategy;
import com.zlt.aps.tq.engine.strategy.TqDemandCalcHelper;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈计划量默认策略（6 班滚动计算）。
 *
 * <p>对应原 {@code TqDemandCalcHandler.computeTqPlanQty} 主体逻辑（closeOutSpecFlag 设置之后的部分）：</p>
 * <ul>
 *   <li>初始可用库存 = 当前库存 - 成型 2 班消耗量（×系数）</li>
 *   <li>胎圈 N 班 → 供应成型 (N+2) 班，逐班滚动</li>
 *   <li>每班排产前判断常规需求：若可用库存 &lt; 成型下个班消耗，则排产差额 × 损耗率乘数</li>
 *   <li>每班排产后判断被动备库触发：可用库存 &lt; SYS1101001 阈值 → 触发备库，撤销常规计算并按阈值分摊到触发班及后续班次</li>
 *   <li>试制/量试规格主动备库：第 1 班直接触发备库</li>
 *   <li>未触发备库：第 6 班按 productStockDay 计算目标库存</li>
 *   <li>每班计划量经 planQtyRounding 做取整和工装限制</li>
 * </ul>
 *
 * <p>本策略读取 S2.2 产出的 {@code closeOutSpecFlag}，写入 6 班计划量、备库标记和交接班库存。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqDefaultPlanQtyStrategy implements ITqPlanQtyStrategy {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /** 策略编码：DEFAULT */
    private static final String STRATEGY_CODE = "DEFAULT";

    @Override
    public String getStrategyCode() {
        return STRATEGY_CODE;
    }

    @Override
    public void calcPlanQty(TqScheduleResultVo scheduleVo, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        double coefficient = params.getDemandCoefficient() == null ? 2D : params.getDemandCoefficient();
        double productStockDay = params.getProductStockDay();
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        Double totalConsumeQty = scheduleVo.getSurplusQty();

        // 损耗率乘数：(100% + 损耗率) / 100
        double lossRate = params.getLossRate() == null ? 0D : params.getLossRate();
        double lossRateMultiplier = BigDecimalUtil.div(BigDecimalUtil.add(100, lossRate), 100);

        Double stockQty = scheduleVo.getStockQty() == null ? 0D : scheduleVo.getStockQty();
        Double todayMorningPlanQty = scheduleVo.getTodayMorningPlanQty() == null ? 0D : scheduleVo.getTodayMorningPlanQty();

        // 成型各班消耗量（原始值）
        double cxClass1 = scheduleVo.getCxClass1Plan() == null ? 0 : scheduleVo.getCxClass1Plan();
        double cxClass2 = scheduleVo.getCxClass2Plan() == null ? 0 : scheduleVo.getCxClass2Plan();
        double cxClass3 = scheduleVo.getCxClass3Plan() == null ? 0 : scheduleVo.getCxClass3Plan();
        double cxClass4 = scheduleVo.getCxClass4Plan() == null ? 0 : scheduleVo.getCxClass4Plan();
        double cxClass5 = scheduleVo.getCxClass5Plan() == null ? 0 : scheduleVo.getCxClass5Plan();
        double cxClass6 = scheduleVo.getCxClass6Plan() == null ? 0 : scheduleVo.getCxClass6Plan();
        double cxClass7 = scheduleVo.getCxClass7Plan() == null ? 0 : scheduleVo.getCxClass7Plan();
        double cxClass8 = scheduleVo.getCxClass8Plan() == null ? 0 : scheduleVo.getCxClass8Plan();

        // 成型各班消耗量 × 需求系数 = 胎圈消耗量
        double tqConsume1 = BigDecimalUtil.mul(cxClass1, coefficient);
        double tqConsume2 = BigDecimalUtil.mul(cxClass2, coefficient);
        double tqConsume3 = BigDecimalUtil.mul(cxClass3, coefficient);
        double tqConsume4 = BigDecimalUtil.mul(cxClass4, coefficient);
        double tqConsume5 = BigDecimalUtil.mul(cxClass5, coefficient);
        double tqConsume6 = BigDecimalUtil.mul(cxClass6, coefficient);
        double tqConsume7 = BigDecimalUtil.mul(cxClass7, coefficient);
        double tqConsume8 = BigDecimalUtil.mul(cxClass8, coefficient);

        // 初始可用库存 = 当前库存 - 成型 2 班 (D 日中班) 消耗量
        double availableStock = BigDecimalUtil.sub(stockQty, tqConsume2);

        TqDemandCalcHelper.logSchedule(autoScheduleLogService, scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算胎圈6班计划量-初始数据",
                TqDemandCalcHelper.logSplit("物料编号：" + scheduleVo.getBeadCode(),
                        "需求系数：" + coefficient + "，备库班数：" + params.getBackupShiftCount() + "，损耗率：" + lossRate + "%，损耗率乘数：" + lossRateMultiplier,
                        "当前库存：" + stockQty + "，当天早班计划量：" + todayMorningPlanQty + "，初始可用库存：" + availableStock,
                        "成型1班消耗：" + cxClass1 + "(胎圈消耗" + tqConsume1 + ")，成型2班消耗：" + cxClass2 + "(胎圈消耗" + tqConsume2 + ")",
                        "成型3班消耗：" + cxClass3 + "(胎圈消耗" + tqConsume3 + ")，成型4班消耗：" + cxClass4 + "(胎圈消耗" + tqConsume4 + ")",
                        "成型5班消耗：" + cxClass5 + "(胎圈消耗" + tqConsume5 + ")，成型6班消耗：" + cxClass6 + "(胎圈消耗" + tqConsume6 + ")",
                        "成型7班消耗：" + cxClass7 + "(胎圈消耗" + tqConsume7 + ")，成型8班消耗：" + cxClass8 + "(胎圈消耗" + tqConsume8 + ")"));

        // === 备库班数配置预查询 ===
        String beadCode = scheduleVo.getBeadCode();
        Integer machineCount = context.getBeadMachineCountMap().get(beadCode);
        Integer backupShiftCount = matchBackupShiftCount(machineCount, context.getStockShiftConfigList());
        boolean hasBackupConfig = backupShiftCount != null && backupShiftCount > 0;
        int backupTriggerClass = 0;
        double backupTotalQty = 0;

        // 备库配置命中证据
        Map<String, Object> backupConfigEvidence = new HashMap<>();
        backupConfigEvidence.put("machineCount", machineCount);
        backupConfigEvidence.put("hasBackupConfig", hasBackupConfig);
        backupConfigEvidence.put("backupShiftCount", backupShiftCount);
        TqDemandCalcHelper.addRuleTrace(context, beadCode,
                TqScheduleRuleCodeEnum.BACKUP_SHIFT_CONFIG_MATCH,
                hasBackupConfig ? TqScheduleRuleResultEnum.HIT : TqScheduleRuleResultEnum.MISS,
                backupConfigEvidence);

        // ==================== 胎圈 1 班（D 日中班 → 供应成型 3 班 D+1 日夜班） ====================
        double class1Plan = 0;
        double class1PureDemand = 0;

        boolean isTrialSpec = scheduleVo.getEmbryoTypeFlag() != null
                && scheduleVo.getEmbryoTypeFlag() == 1;
        if (isTrialSpec && hasBackupConfig && backupTriggerClass == 0) {
            // 试制/量试规格主动备库
            backupTriggerClass = 1;
            backupTotalQty = TqDemandCalcHelper.calculateBackupTotalQty(backupTriggerClass, backupShiftCount, scheduleVo, coefficient);
            double actualTotalPlan = triggerBackupAndAllocate(scheduleVo, backupTriggerClass, backupTotalQty,
                    availableStock, lossRateMultiplier, toolCapacity, totalConsumeQty, context);
            availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);
            class1Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 1);
            logBackupTrigger(scheduleVo, 1, machineCount, backupShiftCount, backupTotalQty, availableStock);
            recordBackupTriggerEvidence(context, beadCode, 1, machineCount, backupShiftCount, backupTotalQty, availableStock, true);
        } else {
            if (availableStock < tqConsume3) {
                class1PureDemand = BigDecimalUtil.sub(tqConsume3, availableStock);
                class1Plan = BigDecimalUtil.mul(class1PureDemand, lossRateMultiplier);
            }
            class1Plan = TqDemandCalcHelper.planQtyRounding(scheduleVo, class1Plan, toolCapacity, totalConsumeQty, context);
            scheduleVo.setClass1PlanQty(class1Plan);
            availableStock = BigDecimalUtil.add(availableStock, class1Plan);
            availableStock = BigDecimalUtil.sub(availableStock, tqConsume3);

            if (hasBackupConfig && TqDemandCalcHelper.shouldTriggerBackup(availableStock, params)) {
                backupTriggerClass = 1;
                availableStock = BigDecimalUtil.add(availableStock, tqConsume3);
                availableStock = BigDecimalUtil.sub(availableStock, class1Plan);
                backupTotalQty = TqDemandCalcHelper.calculateBackupTotalQty(backupTriggerClass, backupShiftCount, scheduleVo, coefficient);
                double actualTotalPlan = triggerBackupAndAllocate(scheduleVo, backupTriggerClass, backupTotalQty,
                        availableStock, lossRateMultiplier, toolCapacity, totalConsumeQty, context);
                availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);
                class1Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 1);
                logBackupTrigger(scheduleVo, 1, machineCount, backupShiftCount, backupTotalQty, availableStock);
                recordBackupTriggerEvidence(context, beadCode, 1, machineCount, backupShiftCount, backupTotalQty, availableStock, false);
            }
        }

        // ==================== 胎圈 2 班（D+1 日夜班 → 供应成型 4 班 D+1 日早班） ====================
        double class2Plan = 0;
        double class2PureDemand = 0;
        if (backupTriggerClass == 0) {
            if (availableStock < tqConsume4) {
                class2PureDemand = BigDecimalUtil.sub(tqConsume4, availableStock);
                class2Plan = BigDecimalUtil.mul(class2PureDemand, lossRateMultiplier);
            }
            class2Plan = TqDemandCalcHelper.planQtyRounding(scheduleVo, class2Plan, toolCapacity, totalConsumeQty, context);
            scheduleVo.setClass2PlanQty(class2Plan);
            availableStock = BigDecimalUtil.add(availableStock, class2Plan);
            availableStock = BigDecimalUtil.sub(availableStock, tqConsume4);

            if (hasBackupConfig && TqDemandCalcHelper.shouldTriggerBackup(availableStock, params)) {
                backupTriggerClass = 2;
                availableStock = BigDecimalUtil.add(availableStock, tqConsume4);
                availableStock = BigDecimalUtil.sub(availableStock, class2Plan);
                backupTotalQty = TqDemandCalcHelper.calculateBackupTotalQty(backupTriggerClass, backupShiftCount, scheduleVo, coefficient);
                double actualTotalPlan = triggerBackupAndAllocate(scheduleVo, backupTriggerClass, backupTotalQty,
                        availableStock, lossRateMultiplier, toolCapacity, totalConsumeQty, context);
                availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);
                class2Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 2);
                logBackupTrigger(scheduleVo, 2, machineCount, backupShiftCount, backupTotalQty, availableStock);
                recordBackupTriggerEvidence(context, beadCode, 2, machineCount, backupShiftCount, backupTotalQty, availableStock, false);
            }
        }

        // ==================== 胎圈 3 班（D+1 日早班 → 供应成型 5 班 D+1 日中班） ====================
        double class3Plan = 0;
        double class3PureDemand = 0;
        if (backupTriggerClass == 0) {
            if (availableStock < tqConsume5) {
                class3PureDemand = BigDecimalUtil.sub(tqConsume5, availableStock);
                class3Plan = BigDecimalUtil.mul(class3PureDemand, lossRateMultiplier);
            }
            class3Plan = TqDemandCalcHelper.planQtyRounding(scheduleVo, class3Plan, toolCapacity, totalConsumeQty, context);
            scheduleVo.setClass3PlanQty(class3Plan);
            availableStock = BigDecimalUtil.add(availableStock, class3Plan);
            availableStock = BigDecimalUtil.sub(availableStock, tqConsume5);

            if (hasBackupConfig && TqDemandCalcHelper.shouldTriggerBackup(availableStock, params)) {
                backupTriggerClass = 3;
                availableStock = BigDecimalUtil.add(availableStock, tqConsume5);
                availableStock = BigDecimalUtil.sub(availableStock, class3Plan);
                backupTotalQty = TqDemandCalcHelper.calculateBackupTotalQty(backupTriggerClass, backupShiftCount, scheduleVo, coefficient);
                double actualTotalPlan = triggerBackupAndAllocate(scheduleVo, backupTriggerClass, backupTotalQty,
                        availableStock, lossRateMultiplier, toolCapacity, totalConsumeQty, context);
                availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);
                class3Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 3);
                logBackupTrigger(scheduleVo, 3, machineCount, backupShiftCount, backupTotalQty, availableStock);
                recordBackupTriggerEvidence(context, beadCode, 3, machineCount, backupShiftCount, backupTotalQty, availableStock, false);
            }
        }

        // ==================== 胎圈 4 班（D+1 日中班 → 供应成型 6 班 D+2 日夜班） ====================
        double class4Plan = 0;
        double class4PureDemand = 0;
        if (backupTriggerClass == 0) {
            if (availableStock < tqConsume6) {
                class4PureDemand = BigDecimalUtil.sub(tqConsume6, availableStock);
                class4Plan = BigDecimalUtil.mul(class4PureDemand, lossRateMultiplier);
            }
            class4Plan = TqDemandCalcHelper.planQtyRounding(scheduleVo, class4Plan, toolCapacity, totalConsumeQty, context);
            scheduleVo.setClass4PlanQty(class4Plan);
            availableStock = BigDecimalUtil.add(availableStock, class4Plan);
            availableStock = BigDecimalUtil.sub(availableStock, tqConsume6);

            if (hasBackupConfig && TqDemandCalcHelper.shouldTriggerBackup(availableStock, params)) {
                backupTriggerClass = 4;
                availableStock = BigDecimalUtil.add(availableStock, tqConsume6);
                availableStock = BigDecimalUtil.sub(availableStock, class4Plan);
                backupTotalQty = TqDemandCalcHelper.calculateBackupTotalQty(backupTriggerClass, backupShiftCount, scheduleVo, coefficient);
                double actualTotalPlan = triggerBackupAndAllocate(scheduleVo, backupTriggerClass, backupTotalQty,
                        availableStock, lossRateMultiplier, toolCapacity, totalConsumeQty, context);
                availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);
                class4Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 4);
                logBackupTrigger(scheduleVo, 4, machineCount, backupShiftCount, backupTotalQty, availableStock);
                recordBackupTriggerEvidence(context, beadCode, 4, machineCount, backupShiftCount, backupTotalQty, availableStock, false);
            }
        }

        // ==================== 胎圈 5 班（D+2 日夜班 → 供应成型 7 班 D+2 日早班） ====================
        double class5Plan = 0;
        double class5PureDemand = 0;
        if (backupTriggerClass == 0) {
            if (availableStock < tqConsume7) {
                class5PureDemand = BigDecimalUtil.sub(tqConsume7, availableStock);
                class5Plan = BigDecimalUtil.mul(class5PureDemand, lossRateMultiplier);
            }
            class5Plan = TqDemandCalcHelper.planQtyRounding(scheduleVo, class5Plan, toolCapacity, totalConsumeQty, context);
            scheduleVo.setClass5PlanQty(class5Plan);
            availableStock = BigDecimalUtil.add(availableStock, class5Plan);
            availableStock = BigDecimalUtil.sub(availableStock, tqConsume7);

            if (hasBackupConfig && TqDemandCalcHelper.shouldTriggerBackup(availableStock, params)) {
                backupTriggerClass = 5;
                availableStock = BigDecimalUtil.add(availableStock, tqConsume7);
                availableStock = BigDecimalUtil.sub(availableStock, class5Plan);
                backupTotalQty = TqDemandCalcHelper.calculateBackupTotalQty(backupTriggerClass, backupShiftCount, scheduleVo, coefficient);
                double actualTotalPlan = triggerBackupAndAllocate(scheduleVo, backupTriggerClass, backupTotalQty,
                        availableStock, lossRateMultiplier, toolCapacity, totalConsumeQty, context);
                availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);
                class5Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 5);
                logBackupTrigger(scheduleVo, 5, machineCount, backupShiftCount, backupTotalQty, availableStock);
                recordBackupTriggerEvidence(context, beadCode, 5, machineCount, backupShiftCount, backupTotalQty, availableStock, false);
            }
        }

        // ==================== 胎圈 6 班（D+2 日早班 → 供应成型 8 班 D+2 日中班，滚动排程） ====================
        double class6Plan = 0;
        if (backupTriggerClass > 0) {
            scheduleVo.setBackupShiftCount(backupShiftCount);
            class6Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 6);
            scheduleVo.setClassStock(availableStock);
        } else {
            scheduleVo.setUseBackupConfigFlag("0");
            double nextDayCxConsume = BigDecimalUtil.add(tqConsume7, tqConsume8);
            double targetStock = BigDecimalUtil.roundDown(BigDecimalUtil.mul(nextDayCxConsume, productStockDay), 0);
            double class6PureDemand = 0;
            if (targetStock > availableStock) {
                class6PureDemand = BigDecimalUtil.sub(targetStock, availableStock);
                class6Plan = BigDecimalUtil.mul(class6PureDemand, lossRateMultiplier);
            }
            class6Plan = TqDemandCalcHelper.planQtyRounding(scheduleVo, class6Plan, toolCapacity, totalConsumeQty, context);
            scheduleVo.setClass6PlanQty(class6Plan);
            scheduleVo.setClassStock(BigDecimalUtil.add(availableStock, class6Plan));
        }

        // 计算供需比率
        double oneDayTqConsume = BigDecimalUtil.add(tqConsume6, BigDecimalUtil.add(tqConsume7, tqConsume8));
        scheduleVo.setSupplyDemandRatio(oneDayTqConsume > 0
                ? BigDecimalUtil.div(scheduleVo.getClassStock(), oneDayTqConsume, 4) : 0);

        // 累加总计划量（统一从 scheduleVo 重新读取，避免分摊后本地变量未同步）
        class1Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 1);
        class2Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 2);
        class3Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 3);
        class4Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 4);
        class5Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 5);
        class6Plan = TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, 6);
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

        TqDemandCalcHelper.logSchedule(autoScheduleLogService, scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "计算胎圈6班计划量",
                TqDemandCalcHelper.logSplit("物料编号：" + scheduleVo.getBeadCode(),
                        "1班计划量：" + class1Plan + "，2班计划量：" + class2Plan + "，3班计划量：" + class3Plan,
                        "4班计划量：" + class4Plan + "，5班计划量：" + class5Plan + "，6班计划量：" + class6Plan,
                        "交接班库存：" + scheduleVo.getClassStock() + "，供需比率：" + scheduleVo.getSupplyDemandRatio()));
    }

    // ==================== 备库分摊 ====================

    /**
     * 触发备库后，按阈值分摊备库总量到触发班及后续班次。
     *
     * <p>保持与原 {@code TqDemandCalcHandler.triggerBackupAndAllocate} 算法等价。</p>
     */
    private double triggerBackupAndAllocate(TqScheduleResultVo scheduleVo, int triggerClass,
                                            double backupTotalQty, double availableStock,
                                            double lossRateMultiplier, BigDecimal toolCapacity,
                                            Double totalConsumeQty, TqScheduleContext context) {
        double pureDemand = Math.max(0, BigDecimalUtil.sub(backupTotalQty, availableStock));
        double totalPlanQty = BigDecimalUtil.mul(pureDemand, lossRateMultiplier);

        TqScheduleParams params = context.getParams();
        double threshold = params.getBackupShiftThreshold() == null ? 1000D : params.getBackupShiftThreshold();

        double remainingQty = totalPlanQty;
        for (int classNum = triggerClass; classNum <= 6 && remainingQty > 0; classNum++) {
            double classPlan;
            if (classNum == 6) {
                classPlan = remainingQty;
            } else {
                classPlan = Math.min(remainingQty, threshold);
            }
            classPlan = TqDemandCalcHelper.planQtyRounding(scheduleVo, classPlan, toolCapacity, totalConsumeQty, context);
            TqDemandCalcHelper.setClassPlanQtyByIndex(scheduleVo, classNum, classPlan);
            remainingQty = BigDecimalUtil.sub(remainingQty, classPlan);
        }

        double actualTotalPlan = 0;
        for (int classNum = triggerClass; classNum <= 6; classNum++) {
            actualTotalPlan = BigDecimalUtil.add(actualTotalPlan, TqDemandCalcHelper.getClassPlanQtyByIndex(scheduleVo, classNum));
        }

        scheduleVo.setUseBackupConfigFlag("1");
        scheduleVo.setBackupTriggerClass(triggerClass);
        scheduleVo.setBackupRemainingQty(totalPlanQty);
        // Phase 2 重构新增：保存初始备库总需求量，供 S5.6 重算 backupRemainingQty 使用
        scheduleVo.setBackupTotalPlanQty(totalPlanQty);

        return actualTotalPlan;
    }

    // ==================== 备库班数配置匹配 ====================

    /**
     * 根据成型机台数匹配胎圈备库班数配置规则，得到需备库班数 N。
     * 包装 Helper 调用，保持与原 Handler 方法签名一致。
     */
    private Integer matchBackupShiftCount(Integer machineCount, List<TqStockShiftConfig> configList) {
        return TqDemandCalcHelper.matchBackupShiftCount(machineCount, configList);
    }

    // ==================== 日志与证据 ====================

    /**
     * 记录备库触发日志。
     */
    private void logBackupTrigger(TqScheduleResultVo scheduleVo, int triggerClass, Integer machineCount,
                                  Integer backupShiftCount, double backupTotalQty, double availableStock) {
        TqDemandCalcHelper.logSchedule(autoScheduleLogService, scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "触发胎圈备库班数配置逻辑",
                TqDemandCalcHelper.logSplit("物料编号：" + scheduleVo.getBeadCode(),
                        "触发班次：胎圈" + triggerClass + "班",
                        "成型机台数：" + (machineCount == null ? "无" : machineCount),
                        "命中备库班数N：" + backupShiftCount,
                        "备库N班总量：" + backupTotalQty,
                        "触发后可用库存：" + availableStock));
    }

    /**
     * 记录备库触发证据。
     *
     * @param context       排程上下文
     * @param beadCode       胎圈编码
     * @param triggerClass   触发班次
     * @param machineCount   成型机台数
     * @param backupShiftCount 命中的备库班数
     * @param backupTotalQty 备库 N 班总量
     * @param availableStock 触发后可用库存
     * @param activeTrigger  true=主动触发（试制规格），false=被动触发
     */
    private void recordBackupTriggerEvidence(TqScheduleContext context, String beadCode, int triggerClass,
                                              Integer machineCount, Integer backupShiftCount,
                                              double backupTotalQty, double availableStock, boolean activeTrigger) {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("triggerClass", triggerClass);
        evidence.put("machineCount", machineCount);
        evidence.put("backupShiftCount", backupShiftCount);
        evidence.put("backupTotalQty", backupTotalQty);
        evidence.put("availableStockAfterTrigger", availableStock);
        evidence.put("triggerType", activeTrigger ? "ACTIVE_TRIAL" : "PASSIVE_LOW_STOCK");
        TqDemandCalcHelper.addRuleTrace(context, beadCode,
                TqScheduleRuleCodeEnum.BACKUP_TRIGGER, TqScheduleRuleResultEnum.TRIGGER, evidence);
    }
}
