package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleResultEnum;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S2.3: 钢丝圈计划量计算Handler。
 *
 * <p>Phase 1 重构：从原 S2 GsqDemandCalcHandler 中拆分出来，对齐胎圈 TqPlanQtyCalcHandler。</p>
 *
 * <p>Phase 2 重构：实现钢丝圈「完整备库模型」（基于胎圈消耗 tqClassN，因钢丝圈无成型cx数据）。</p>
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>逐规格应用备库模型：基于可用库存与后续胎圈消耗判断是否触发备库，计算备库总量并分摊到触发班次及后续班次</li>
 *   <li>聚合6班次总计划量统计</li>
 * </ol>
 *
 * <p>备库模型说明（对齐胎圈 TqDefaultPlanQtyStrategy，但数据源改为胎圈消耗）：</p>
 * <ul>
 *   <li>备库班数 N = 钢丝圈参数配置固定（SYS1601003），不使用独立的备库班数配置表</li>
 *   <li>触发条件：扣掉当前胎圈班消耗后的剩余可用库存 &lt; 触发阈值（默认0.7，SYS1601003组可配置） × 下一胎圈班消耗（不加入钢丝圈未来生产）</li>
 *   <li>备库总量 = 未来 N 个胎圈班次消耗 × 需求系数，超出胎圈7班的班次用「最后3个非停产胎圈班次均值」估算</li>
 *   <li>分摊规则：备库总量按单班阈值（SYS1603004，默认1000）分摊到触发班次及后续班次，末班全排</li>
 *   <li>写入 backupTriggerClass/backupTotalQty/backupShiftCount/hasBackupConfig 供 S5/S5.5 跳过备库触发班次使用</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqPlanQtyCalcHandler extends AbsGsqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /** 默认备库班次单班排产阈值 */
    private static final double DEFAULT_BACKUP_SHIFT_THRESHOLD = 1000D;

    /** 默认备库触发阈值（班）：剩余库存低于 0.7 × 下一胎圈班消耗时触发备库 */
    private static final double DEFAULT_BACKUP_TRIGGER_THRESHOLD_CLASS = 0.7D;

    /** 默认取整合并阈值（SYS1603006，备库分摊时剩余量≤此值合并到当前班次排完，默认0不启用） */
    private static final double DEFAULT_ROUNDING_MERGE_THRESHOLD = 0D;

    @Override
    protected String getStepName() {
        return "S2.3-计划量计算";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        // 1. 逐规格应用备库模型（基于胎圈消耗）
        applyBackupModel(context);

        // 1.1 常规排产（备库窗口外）班次计入损耗率
        applyLossRateToRegularSupply(context);

        // 2. 聚合6班次总计划量统计
        aggregateTotalPlanQty(context);

        // 诊断日志：打印每个规格的分摊明细（定位备库量是否被库存抵扣，确认后可移除）
        for (GsqScheduleResultVo vo : context.getScheduleList()) {
            log.info("[S2.3-DIAG] 规格:{} 触发库存planStock={} backupTriggerClass={} backupTotalQty={} "
                            + "分摊各班=[1:{} 2:{} 3:{} 4:{} 5:{} 6:{}]",
                    vo.getSteelRingCode(), vo.getPlanStockQty(),
                    vo.getBackupTriggerClass(), vo.getBackupTotalQty(),
                    getClassPlanQty(vo, 1), getClassPlanQty(vo, 2), getClassPlanQty(vo, 3),
                    getClassPlanQty(vo, 4), getClassPlanQty(vo, 5), getClassPlanQty(vo, 6));
        }

        log.info("[S2.3] 计划量计算完成, 排程记录数: {}", context.getScheduleList().size());
    }

    // ==================== 备库模型 ====================

    /**
     * 逐规格应用备库模型，写入备库触发班次、备库总量等字段。
     *
     * @param context 排程上下文
     */
    private void applyBackupModel(GsqScheduleContext context) {
        GsqScheduleParams params = context.getParams();
        double coefficient = params.getDemandCoefficient() == null ? 1D : params.getDemandCoefficient();

        // 备库班数 N 由钢丝圈参数配置固定（SYS1601003），不使用独立的备库班数配置表
        int backupShiftCount = params.getStockShiftCount() == null ? 0 : params.getStockShiftCount().intValue();
        boolean hasBackup = backupShiftCount > 0;

        // 备库触发阈值（班）：剩余库存低于 阈值 × 下一胎圈班消耗时触发，默认0.7
        double triggerThreshold = params.getBackupTriggerThresholdClass() == null
                ? DEFAULT_BACKUP_TRIGGER_THRESHOLD_CLASS : params.getBackupTriggerThresholdClass();

        for (GsqScheduleResultVo vo : context.getScheduleList()) {
            vo.setBackupShiftCount(backupShiftCount);
            vo.setHasBackupConfig(hasBackup);

            if (!hasBackup) {
                vo.setBackupTriggerClass(0);
                vo.setBackupTotalQty(0D);
                continue;
            }

            // 2. 滚动判断触发班次（纯库存判断，不加入钢丝圈未来生产）
            // 触发条件：扣掉当前胎圈班消耗后的剩余库存 < 0.7 × 下一胎圈班消耗
            double availableStock = vo.getPlanStockQty() == null ? 0D : vo.getPlanStockQty();
            int backupTriggerClass = 0;
            double backupTotalQty = 0;
            // 触发备库时的已扣库存（含当前班消耗），供回写 S3 重算使用
            double stockAtTrigger = 0D;

            for (int classNum = 1; classNum <= 6 && backupTriggerClass == 0; classNum++) {
                // 扣掉当前胎圈班消耗后的剩余库存
                // 注意：planStockQty 在 S1 阶段已扣减胎圈1班消耗
                // （planStock = stockQty + lastMidPlan - tqClass1，见 GsqPreValidationHandler.initScheduleFields），
                // classNum=1 时不重复扣减 tqClass1，避免 gap fill 缺口虚增（如 350 被误算为 850）
                double stockAfterCurrent;
                if (classNum == 1) {
                    stockAfterCurrent = availableStock;
                } else {
                    stockAfterCurrent = BigDecimalUtil.sub(availableStock, getTqPlan(vo, classNum));
                }
                // 下一胎圈班消耗（钢丝圈N班供应胎圈N+1班）
                double nextTqConsume = getTqPlan(vo, classNum + 1);
                // 胎圈X+1班没排产（消耗<=0）时，本班不触发备库，仅处理库存缺口
                if (nextTqConsume <= 0) {
                    // 库存不足（扣完当前班消耗后为负）时，当前班常规供应补足缺口
                    if (stockAfterCurrent < 0) {
                        double gap = Math.ceil(BigDecimalUtil.sub(0D, stockAfterCurrent));
                        setClassPlanQty(vo, classNum, gap);
                        availableStock = 0D;
                    } else {
                        availableStock = stockAfterCurrent;
                    }
                    continue;
                }
                if (stockAfterCurrent < BigDecimalUtil.mul(nextTqConsume, triggerThreshold)) {
                    // 触发备库：传 stockAfterCurrent（已扣当前班消耗），pureDemand 含缺口
                    backupTriggerClass = classNum;
                    stockAtTrigger = stockAfterCurrent;
                    backupTotalQty = calculateBackupTotalQty(backupTriggerClass, backupShiftCount, vo, coefficient);
                    double actualTotalPlan = triggerBackupAndAllocate(
                            vo, backupTriggerClass, backupTotalQty, stockAfterCurrent, context);
                    availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);

                    autoScheduleLogService.insertGsqScheduleLog(vo.getBatchNo(), vo.getOrderNo(),
                            "备库触发", "钢丝圈：" + vo.getSteelRingCode()
                                    + "，" + backupTriggerClass + "班触发备库，备库班数N=" + backupShiftCount
                                    + "，备库总量=" + backupTotalQty + "，分摊计划量=" + actualTotalPlan);
                    break;
                }
                availableStock = stockAfterCurrent;
            }

            vo.setBackupTriggerClass(backupTriggerClass);
            vo.setBackupTotalQty(backupTotalQty);
            // 回写触发时的已扣库存（stockAfterCurrent，不含排产量），供 S3 recalcPlanQtyByMachineLossRate 使用。
            // 若回写 availableStock+actualTotalPlan 则 S3 pureDemand 恒为0（备库量被清零）；
            // 若回写原始 planStockQty 则 S3 pureDemand 不含当前班缺口（备库量偏小）。
            if (backupTriggerClass > 0) {
                vo.setPlanStockQty(stockAtTrigger);
            } else {
                vo.setPlanStockQty(availableStock);
            }

            // 备库未触发且库存充足（有剩余）时，库存已覆盖需求，无需排产，清零初始计划量
            if (backupTriggerClass == 0 && availableStock > 0) {
                for (int classNum = 1; classNum <= 6; classNum++) {
                    setClassPlanQty(vo, classNum, 0D);
                }
            }

            if (backupTriggerClass > 0) {
                recordBackupTriggerEvidence(context, vo, backupTriggerClass, null,
                        backupShiftCount, backupTotalQty, availableStock);
            }
        }
    }

    /**
     * 常规排产（备库窗口外）班次计入损耗率。
     *
     * <p>背景：备库窗口内计划量已在 {@link #triggerBackupAndAllocate} 中计入损耗率，
     * 但窗口外的常规续供班次（基于胎圈消耗的 base plan）未乘损耗率，会导致钢丝圈对胎圈
     * 常规消耗的排产量偏小。本方法对窗口外所有有排产的班次统一乘损耗率（向上取整）。</p>
     *
     * @param context 排程上下文
     */
    private void applyLossRateToRegularSupply(GsqScheduleContext context) {
        double paramLossRate = context.getParams().getLossRate() == null ? 0D : context.getParams().getLossRate();
        for (GsqScheduleResultVo vo : context.getScheduleList()) {
            // 按钢丝圈代码取损耗率，未配置时回退全局参数损耗率（与备库分摊口径一致）
            Double specLossRate = context.getLossRateMap().get(vo.getSteelRingCode());
            double multiplier = specLossRate != null
                    ? BigDecimalUtil.add(1D, specLossRate)
                    : BigDecimalUtil.add(1D, paramLossRate);
            if (multiplier <= 1D) {
                continue;
            }

            // 计算备库窗口 [triggerClass, windowEnd]，窗口内班次已含损耗率，跳过
            Integer triggerClass = vo.getBackupTriggerClass();
            Integer backupShiftCount = vo.getBackupShiftCount();
            int windowStart = triggerClass != null ? triggerClass : 0;
            int windowEnd = 6;
            if (triggerClass != null && triggerClass > 0 && backupShiftCount != null && backupShiftCount > 0) {
                windowEnd = Math.min(6, triggerClass + backupShiftCount - 1);
            } else {
                // 非备库触发规格：无备库窗口，全部班次均视为常规排产
                windowStart = 0;
                windowEnd = 0;
            }

            for (int classNum = 1; classNum <= 6; classNum++) {
                if (windowStart > 0 && classNum >= windowStart && classNum <= windowEnd) {
                    continue;
                }
                double plan = getClassPlanQty(vo, classNum);
                if (plan > 0) {
                    double inflated = Math.ceil(BigDecimalUtil.mul(plan, multiplier));
                    setClassPlanQty(vo, classNum, inflated);
                }
            }
        }
    }

    /**
     * 计算备库 N 个胎圈班次的总消耗量 × 需求系数。
     *
     * <p>钢丝圈N班供应胎圈N+1班，因此触发钢丝圈X班时，备库从胎圈 (X+1) 班开始连续 N 个班。
     * 若备库班数超出胎圈计划（胎圈只有7班），剩余班次使用「最后3个非停产胎圈班次均值」估算。</p>
     *
     * @param triggerClass     触发备库的钢丝圈班次（1-6）
     * @param backupShiftCount 需备库的班数 N
     * @param vo               排程结果 VO（含胎圈1~7班消耗量）
     * @param coefficient      需求系数
     * @return 备库 N 个班的总消耗量
     */
    private double calculateBackupTotalQty(int triggerClass, int backupShiftCount,
                                           GsqScheduleResultVo vo, double coefficient) {
        int startTqClass = triggerClass + 1;
        double totalQty = 0;
        int coveredShifts = 0;

        // 阶段1：从胎圈计划中取消耗量（计划内的班次，胎圈最多7班）
        while (coveredShifts < backupShiftCount && startTqClass <= 7) {
            totalQty = BigDecimalUtil.add(totalQty, BigDecimalUtil.mul(getTqPlan(vo, startTqClass), coefficient));
            startTqClass++;
            coveredShifts++;
        }

        // 阶段2：超出胎圈计划的班次，使用"最后3个非停产胎圈班次"的平均消耗量 × 系数
        if (coveredShifts < backupShiftCount) {
            double avgConsume = calculateAvgLast3NonStopConsume(vo);
            while (coveredShifts < backupShiftCount) {
                totalQty = BigDecimalUtil.add(totalQty, BigDecimalUtil.mul(avgConsume, coefficient));
                coveredShifts++;
            }
        }

        return totalQty;
    }

    /**
     * 计算胎圈最后 3 个非停产班次的平均消耗量。
     *
     * <p>停产班次特征：消耗量为 0。从胎圈7班倒序遍历，收集非 0 消耗的班次，取最后 3 个的平均值。</p>
     *
     * @param vo 排程结果 VO
     * @return 最后 3 个非停产班次的平均消耗量
     */
    private double calculateAvgLast3NonStopConsume(GsqScheduleResultVo vo) {
        double sum = 0;
        int count = 0;
        for (int tqClass = 7; tqClass >= 1 && count < 3; tqClass--) {
            double consume = getTqPlan(vo, tqClass);
            if (consume > 0) {
                sum = BigDecimalUtil.add(sum, consume);
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return BigDecimalUtil.div(sum, count, 4);
    }

    /**
     * 触发备库后，按阈值分摊备库总量到触发班次及后续班次。
     *
     * <p>分摊规则（对齐胎圈 triggerBackupAndAllocate）：</p>
     * <ul>
     *   <li>每个班次初始排产上限为 threshold（SYS1603004，默认1000）</li>
     *   <li>尾数合并：当某班次排产后剩余量 ≤ mergeThreshold（SYS1603006，默认0不启用）时，将剩余量全部合并到当前班次</li>
     *   <li>第6班直接全排剩余量</li>
     * </ul>
     *
     * @param vo            排程结果 VO
     * @param triggerClass  触发班次
     * @param backupTotalQty 备库总量
     * @param availableStock 触发时可用库存
     * @param context        排程上下文
     * @return 实际分摊后的总计划量（触发班次及后续班次之和）
     */
    private double triggerBackupAndAllocate(GsqScheduleResultVo vo, int triggerClass, double backupTotalQty,
                                            double availableStock, GsqScheduleContext context) {
        double pureDemand = Math.max(0, BigDecimalUtil.sub(backupTotalQty, availableStock));
        // 按钢丝圈代码查询损耗率管理表（LOSS_RATE字段小数原值，如0.02表示2%）
        // S2阶段机台尚未分配，无法按 机台#钢丝圈 精确查询，使用按钢丝圈代码聚合的lossRateMap
        // 未配置时回退到全局参数损耗率（兼容旧逻辑）
        String steelRingCode = vo.getSteelRingCode();
        Double specLossRate = context.getLossRateMap().get(steelRingCode);
        double paramLossRate = context.getParams().getLossRate() == null ? 0D : context.getParams().getLossRate();
        double specLossRateMultiplier = specLossRate != null
                ? BigDecimalUtil.add(1D, specLossRate)
                : BigDecimalUtil.add(1D, paramLossRate);
        double totalPlanQty = Math.ceil(BigDecimalUtil.mul(pureDemand, specLossRateMultiplier));

        double actualTotalPlan = allocatePlanQty(vo, triggerClass, totalPlanQty, context);
        // 记录分摊后的实际总计划量，供 S5.6 总量截断上限使用（而非理论 backupTotalQty）
        vo.setBackupAllocatedQty(actualTotalPlan);
        return actualTotalPlan;
    }

    /**
     * 将备库总量按阈值分摊到触发班次及后续班次。
     *
     * <p>分摊规则（对齐胎圈 triggerBackupAndAllocate）：</p>
     * <ul>
     *   <li>每个班次初始排产上限为 threshold（SYS1603004，默认1000）</li>
     *   <li>尾数合并：当某班次排产后剩余量 ≤ mergeThreshold（SYS1603006，默认0不启用）时，将剩余量全部合并到当前班次</li>
     *   <li>第6班直接全排剩余量</li>
     * </ul>
     *
     * @param vo           排程结果 VO
     * @param triggerClass 触发班次
     * @param totalPlanQty 备库分摊总量（已含损耗率乘数）
     * @param context      排程上下文
     * @return 实际分摊后的总计划量（触发班次及后续班次之和）
     */
    private double allocatePlanQty(GsqScheduleResultVo vo, int triggerClass, double totalPlanQty,
                                   GsqScheduleContext context) {
        GsqScheduleParams params = context.getParams();
        double threshold = params.getBackupShiftThreshold() == null
                ? DEFAULT_BACKUP_SHIFT_THRESHOLD : params.getBackupShiftThreshold();
        // 取整合并阈值（SYS1603006）：0表示不启用合并，保持向上取整；>0时剩余量≤此值则合并到当前班次
        double mergeThreshold = params.getRoundingMergeThreshold() == null
                ? DEFAULT_ROUNDING_MERGE_THRESHOLD : params.getRoundingMergeThreshold();

        // 备库窗口末班 = 触发班次 + 备库班数 - 1。
        // 备库量只分配到窗口内班次，不向窗口外班次溢出，避免把胎圈未排/常规续供班次误写入备库排量；
        // 窗口外班次保持其初始值（胎圈已排则为常规续供量，未排则为0）。
        int windowEnd = 6;
        Integer backupShiftCount = vo.getBackupShiftCount();
        if (backupShiftCount != null && backupShiftCount > 0) {
            windowEnd = Math.min(6, triggerClass + backupShiftCount - 1);
        }

        double remainingQty = totalPlanQty;
        for (int classNum = triggerClass; classNum <= windowEnd && remainingQty > 0; classNum++) {
            double classPlan;
            if (classNum == windowEnd) {
                // 窗口末班直接全排
                classPlan = remainingQty;
            } else {
                double planForThisClass = Math.min(remainingQty, threshold);
                double afterThisClass = BigDecimalUtil.sub(remainingQty, planForThisClass);
                if (afterThisClass > 0 && afterThisClass <= mergeThreshold) {
                    // 尾数合并到当前班次，避免下一班被取整放大
                    classPlan = remainingQty;
                } else {
                    classPlan = planForThisClass;
                }
            }
            classPlan = Math.ceil(classPlan);
            setClassPlanQty(vo, classNum, classPlan);
            remainingQty = BigDecimalUtil.sub(remainingQty, classPlan);
        }

        double actualTotalPlan = 0;
        for (int classNum = triggerClass; classNum <= windowEnd; classNum++) {
            actualTotalPlan = BigDecimalUtil.add(actualTotalPlan, getClassPlanQty(vo, classNum));
        }
        return actualTotalPlan;
    }

    /**
     * S3 机台分配确定后，按该规格实际机台精确取损耗率，重算备库总计划量并重新分摊。
     *
     * <p>背景：S2.3 阶段机台尚未分配，只能按钢丝圈代码聚合损耗率（多机台不一致时取平均，可能失真）。
     * S3 分配确定 machineCode 后，用 {@code machineCode#steelRingCode} 精确取损耗率重算；
     * 机台未配置损耗率时回退到全局参数损耗率（兼容旧逻辑）。</p>
     *
     * @param vo      排程结果 VO（需已设置 machineCode）
     * @param context 排程上下文
     */
    public void recalcPlanQtyByMachineLossRate(GsqScheduleResultVo vo, GsqScheduleContext context) {
        Integer triggerClass = vo.getBackupTriggerClass();
        if (triggerClass == null || triggerClass <= 0) {
            // 非备库触发规格不参与备库损耗率重算
            return;
        }
        double backupTotalQty = vo.getBackupTotalQty() == null ? 0D : vo.getBackupTotalQty();
        // 取 S2.3 回写后的触发时已扣库存（stockAfterCurrent，不含排产量）
        double availableStock = vo.getPlanStockQty() == null ? 0D : vo.getPlanStockQty();
        double pureDemand = Math.max(0, BigDecimalUtil.sub(backupTotalQty, availableStock));

        // 按实际机台精确取损耗率，未配置时回退全局参数损耗率
        String machineCode = vo.getMachineCode();
        Double machineLossRate = null;
        if (machineCode != null) {
            machineLossRate = context.getMachineLossRateMap().get(machineCode + "#" + vo.getSteelRingCode());
        }
        double paramLossRate = context.getParams().getLossRate() == null ? 0D : context.getParams().getLossRate();
        double multiplier = machineLossRate != null
                ? BigDecimalUtil.add(1D, machineLossRate)
                : BigDecimalUtil.add(1D, paramLossRate);

        double totalPlanQty = Math.ceil(BigDecimalUtil.mul(pureDemand, multiplier));
        double actualTotalPlan = allocatePlanQty(vo, triggerClass, totalPlanQty, context);
        vo.setBackupAllocatedQty(actualTotalPlan);
        log.info("[S3] 规格[{}] 机台[{}] 损耗率[{}] 重算分摊后总量[{}]（备库总量{} 库存{}）",
                vo.getSteelRingCode(), machineCode,
                machineLossRate != null ? machineLossRate : ("全局:" + paramLossRate),
                actualTotalPlan, backupTotalQty, availableStock);
    }

    /**
     * 记录备库触发证据。
     */
    private void recordBackupTriggerEvidence(GsqScheduleContext context, GsqScheduleResultVo vo, int triggerClass,
                                             Integer machineCount, Integer backupShiftCount,
                                             double backupTotalQty, double availableStock) {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("triggerClass", triggerClass);
        evidence.put("machineCount", machineCount);
        evidence.put("backupShiftCount", backupShiftCount);
        evidence.put("backupTotalQty", backupTotalQty);
        evidence.put("availableStockAfterTrigger", availableStock);
        context.getRuleTrace(vo.getSteelRingCode()).addRuleHit(
                GsqScheduleRuleCodeEnum.BACKUP_TRIGGER, GsqScheduleRuleResultEnum.TRIGGER, evidence);
    }

    // ==================== 班次计划量读写 ====================

    /**
     * 获取胎圈指定班次的消耗量（tqClass1~7）。
     *
     * @param vo          排程结果 VO
     * @param tqClassNum  胎圈班次（1~7）
     * @return 消耗量（null 视为 0）
     */
    private double getTqPlan(GsqScheduleResultVo vo, int tqClassNum) {
        Object value = vo.getFieldValueByFieldName("tqClass" + tqClassNum + "Plan");
        return value == null ? 0D : ((Number) value).doubleValue();
    }

    /**
     * 获取指定班次的计划量。
     */
    private double getClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum) {
        Object value = scheduleVo.getFieldValueByFieldName("class" + classNum + "PlanQty");
        return value == null ? 0D : ((Number) value).doubleValue();
    }

    /**
     * 设置指定班次的计划量。
     */
    private void setClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum, double value) {
        scheduleVo.setFieldValueByFieldName("class" + classNum + "PlanQty", value);
    }

    /**
     * 聚合6班次总计划量统计。
     *
     * @param context 排程上下文
     */
    private void aggregateTotalPlanQty(GsqScheduleContext context) {
        GsqTotalPlanQtyVo total = new GsqTotalPlanQtyVo();
        List<GsqScheduleResultVo> list = context.getScheduleList();

        for (GsqScheduleResultVo vo : list) {
            double c1 = vo.getClass1PlanQty() == null ? 0 : vo.getClass1PlanQty();
            double c2 = vo.getClass2PlanQty() == null ? 0 : vo.getClass2PlanQty();
            double c3 = vo.getClass3PlanQty() == null ? 0 : vo.getClass3PlanQty();
            double c4 = vo.getClass4PlanQty() == null ? 0 : vo.getClass4PlanQty();
            double c5 = vo.getClass5PlanQty() == null ? 0 : vo.getClass5PlanQty();
            double c6 = vo.getClass6PlanQty() == null ? 0 : vo.getClass6PlanQty();

            total.setTotalClass1PlanQty(total.getTotalClass1PlanQty() + c1);
            total.setTotalClass2PlanQty(total.getTotalClass2PlanQty() + c2);
            total.setTotalClass3PlanQty(total.getTotalClass3PlanQty() + c3);
            total.setTotalClass4PlanQty(total.getTotalClass4PlanQty() + c4);
            total.setTotalClass5PlanQty(total.getTotalClass5PlanQty() + c5);
            total.setTotalClass6PlanQty(total.getTotalClass6PlanQty() + c6);
        }

        double grandTotal = total.getTotalClass1PlanQty() + total.getTotalClass2PlanQty()
                + total.getTotalClass3PlanQty() + total.getTotalClass4PlanQty()
                + total.getTotalClass5PlanQty() + total.getTotalClass6PlanQty();
        total.setTotalPlanQty(grandTotal);

        context.setTotalPlanQtyVo(total);
        log.info("[S2.3] 6班次总计划量统计完成, 总量: {}", grandTotal);
    }
}