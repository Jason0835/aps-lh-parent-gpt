package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.gsq.api.domain.entity.GsqStockShiftConfig;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleResultEnum;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
 *   <li>备库班数 N = 按「供胎圈机台数」区间配置匹配（T_GSQ_STOCK_SHIFT_CONFIG，DepthConfig 方式），
 *       无配置时回退到参数 SYS1601003 备库班数</li>
 *   <li>触发条件：可用库存 &lt; 胎圈下个班次消耗量（钢丝圈N班 → 供应胎圈N+1班）</li>
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

    /** 默认尾数合并阈值 */
    private static final double DEFAULT_MERGE_THRESHOLD = 100D;

    @Override
    protected String getStepName() {
        return "S2.3-计划量计算";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        // 1. 逐规格应用备库模型（基于胎圈消耗）
        applyBackupModel(context);

        // 2. 聚合6班次总计划量统计
        aggregateTotalPlanQty(context);

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
        List<GsqStockShiftConfig> configList = context.getStockShiftConfigList();
        double coefficient = params.getDemandCoefficient() == null ? 1D : params.getDemandCoefficient();

        // 备库班数匹配维度（GSQ无成型cx）：使用供胎圈机台数（当前以包布机总台数为匹配基数）
        Integer machineCount = params.getWrappingMachineCount();

        for (GsqScheduleResultVo vo : context.getScheduleList()) {
            // 1. 确定备库班数 N
            Integer backupShiftCount = matchBackupShiftCount(machineCount, configList);
            boolean hasConfig = backupShiftCount != null && backupShiftCount > 0;
            if (!hasConfig && params.getStockShiftCount() != null && params.getStockShiftCount() > 0) {
                // 无区间配置时回退到参数 SYS1601003 备库班数
                backupShiftCount = params.getStockShiftCount().intValue();
                hasConfig = true;
            }

            vo.setBackupShiftCount(backupShiftCount);
            vo.setHasBackupConfig(hasConfig);

            if (!hasConfig) {
                vo.setBackupTriggerClass(0);
                vo.setBackupTotalQty(0D);
                continue;
            }

            // 2. 滚动判断触发班次
            double availableStock = vo.getPlanStockQty() == null ? 0D : vo.getPlanStockQty();
            int backupTriggerClass = 0;
            double backupTotalQty = 0;

            for (int classNum = 1; classNum <= 6 && backupTriggerClass == 0; classNum++) {
                // 钢丝圈N班供应胎圈N+1班
                double tqConsume = getTqPlan(vo, classNum + 1);
                if (availableStock < tqConsume) {
                    // 触发备库
                    backupTriggerClass = classNum;
                    backupTotalQty = calculateBackupTotalQty(backupTriggerClass, backupShiftCount, vo, coefficient);
                    double actualTotalPlan = triggerBackupAndAllocate(
                            vo, backupTriggerClass, backupTotalQty, availableStock, context);
                    availableStock = BigDecimalUtil.add(availableStock, actualTotalPlan);

                    autoScheduleLogService.insertGsqScheduleLog(vo.getBatchNo(), vo.getOrderNo(),
                            "备库触发", "钢丝圈：" + vo.getSteelRingCode()
                                    + "，" + backupTriggerClass + "班触发备库，备库班数N=" + backupShiftCount
                                    + "，备库总量=" + backupTotalQty + "，分摊计划量=" + actualTotalPlan);
                    break;
                }
                availableStock = BigDecimalUtil.add(
                        BigDecimalUtil.add(availableStock, getClassPlanQty(vo, classNum)), -tqConsume);
            }

            vo.setBackupTriggerClass(backupTriggerClass);
            vo.setBackupTotalQty(backupTotalQty);

            if (backupTriggerClass > 0) {
                recordBackupTriggerEvidence(context, vo, backupTriggerClass, machineCount,
                        backupShiftCount, backupTotalQty, availableStock);
            }
        }
    }

    /**
     * 根据供胎圈机台数匹配备库班数配置，得到需备库班数 N。
     *
     * <p>对齐胎圈 {@code matchBackupShiftCount}：遍历按 MIN_MACHINE_QTY 升序的配置列表，
     * 命中第一个满足 {@code minMachineQty ≤ machineCount ≤ maxMachineQty} 的行；
     * {@code maxMachineQty} 为 null 表示无上限（仅末行允许）。</p>
     *
     * @param machineCount 供胎圈机台数（null 则不匹配）
     * @param configList   备库班数配置列表
     * @return 命中的备库班数；无配置或机台数为空则返回 null
     */
    private Integer matchBackupShiftCount(Integer machineCount, List<GsqStockShiftConfig> configList) {
        if (machineCount == null || machineCount <= 0 || CollectionUtils.isEmpty(configList)) {
            return null;
        }
        for (GsqStockShiftConfig config : configList) {
            Integer minQty = config.getMinMachineQty();
            if (minQty == null) {
                continue;
            }
            Integer maxQty = config.getMaxMachineQty();
            if (machineCount >= minQty && (maxQty == null || machineCount <= maxQty)) {
                return config.getDepthClassQty() == null ? null : config.getDepthClassQty().intValue();
            }
        }
        return null;
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
     *   <li>尾数合并：当某班次排产后剩余量 ≤ mergeThreshold（SYS1601006）时，将剩余量全部合并到当前班次</li>
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
        // 损耗率乘数（GSQ 损耗率以小数存储，如0.02表示2%）
        double lossRate = context.getParams().getLossRate() == null ? 0D : context.getParams().getLossRate();
        double lossRateMultiplier = BigDecimalUtil.add(1, lossRate);
        double totalPlanQty = BigDecimalUtil.mul(pureDemand, lossRateMultiplier);
        // 备库总量做小数向上取整（如509.04 → 510）
        totalPlanQty = Math.ceil(totalPlanQty);

        GsqScheduleParams params = context.getParams();
        double threshold = params.getBackupShiftThreshold() == null
                ? DEFAULT_BACKUP_SHIFT_THRESHOLD : params.getBackupShiftThreshold();
        double mergeThreshold = params.getMergeThreshold() == null
                ? DEFAULT_MERGE_THRESHOLD : params.getMergeThreshold();

        double remainingQty = totalPlanQty;
        for (int classNum = triggerClass; classNum <= 6 && remainingQty > 0; classNum++) {
            double classPlan;
            if (classNum == 6) {
                // 最后一班直接全排
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
        for (int classNum = triggerClass; classNum <= 6; classNum++) {
            actualTotalPlan = BigDecimalUtil.add(actualTotalPlan, getClassPlanQty(vo, classNum));
        }
        return actualTotalPlan;
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
        switch (tqClassNum) {
            case 1: return vo.getTqClass1Plan() == null ? 0D : vo.getTqClass1Plan();
            case 2: return vo.getTqClass2Plan() == null ? 0D : vo.getTqClass2Plan();
            case 3: return vo.getTqClass3Plan() == null ? 0D : vo.getTqClass3Plan();
            case 4: return vo.getTqClass4Plan() == null ? 0D : vo.getTqClass4Plan();
            case 5: return vo.getTqClass5Plan() == null ? 0D : vo.getTqClass5Plan();
            case 6: return vo.getTqClass6Plan() == null ? 0D : vo.getTqClass6Plan();
            case 7: return vo.getTqClass7Plan() == null ? 0D : vo.getTqClass7Plan();
            default: return 0D;
        }
    }

    /**
     * 获取指定班次的计划量。
     */
    private double getClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum) {
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
     * 设置指定班次的计划量。
     */
    private void setClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum, double value) {
        switch (classNum) {
            case 1: scheduleVo.setClass1PlanQty(value); break;
            case 2: scheduleVo.setClass2PlanQty(value); break;
            case 3: scheduleVo.setClass3PlanQty(value); break;
            case 4: scheduleVo.setClass4PlanQty(value); break;
            case 5: scheduleVo.setClass5PlanQty(value); break;
            case 6: scheduleVo.setClass6PlanQty(value); break;
            default: break;
        }
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