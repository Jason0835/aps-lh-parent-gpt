package com.zlt.aps.gsq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleResultEnum;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S5.6: 最终剩余产能回填Handler（原 S3.5）。
 *
 * <p>职责：在 S5.5 定额校验完成、所有计划量修改结束后，回收每个机台每班的剩余产能
 * （机台定额 - 已排产量），按四级优先级回填到该机台上的规格，避免产能浪费（对齐胎圈 TQ S5.6）。</p>
 *
 * <p>排产优先级：</p>
 * <ol>
 *   <li>P-1: 当前班次新触发备库（backupTriggerClass == 当前班次）</li>
 *   <li>P-2: 前序班次已触发备库（同为备库按 backupRemainingQty 降序）</li>
 *   <li>P-3: 非备库规格按供应时长升序</li>
 * </ol>
 *
 * <p>回填规则：备库规格受 backupRemainingQty 限制并扣减（触发班次≤当前班、总量不超 backupTotalQty）；
 * 非备库规格仅当该班次已有排产量时才回填（不新增班次排产）。</p>
 *
 * <p>第6班特殊处理：把剩余产能塞入第6班已排产的规格，备库规格优先塞入剩余备库量。</p>
 *
 * <p>前后置保护：回填前重算 backupRemainingQty（= backupTotalQty - 6班已排），
 * 回填后做总量截断保护（6班总排产 ≤ backupTotalQty）。</p>
 *
 * <p>执行顺序说明（对齐胎圈 TQ）：原 S3.5 位于 S3 之后，但 S4/S5/S5.5 会修改计划量导致回填结果被覆盖。
 * 现移至 S5.5 之后执行（即 S5.6），确保回填结果为最终结果且不被覆盖。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqResidualCapacityHandler extends AbsGsqScheduleStepHandler {

    @Resource
    private com.zlt.aps.common.engine.service.AutoScheduleLogService autoScheduleLogService;

    @Override
    protected String getStepName() {
        return "S5.6-最终剩余产能回填";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }

        // Phase 对齐胎圈：回填前重新计算各备库规格 backupRemainingQty
        // 原因：S2 阶段分摊备份量后，S3/S5/S5.5 会修改计划量，且 S3 延后时未扣减剩余量，
        // 需基于最终实际已排产量修正：backupRemainingQty = backupTotalQty - 6班实际已排产量合计
        recalcBackupRemainingQty(scheduleList);

        // 1. 按机台分组，得到 machineCode → 该机台上所有规格的排程记录
        Map<String, List<GsqScheduleResultVo>> machineSpecMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .collect(Collectors.groupingBy(GsqScheduleResultVo::getMachineCode));

        if (machineSpecMap.isEmpty()) {
            log.info("[S5.6] 无已分配机台的规格，跳过剩余产能回填");
            return;
        }

        // 2. 构建机台信息Map，便于取机台定额
        Map<String, GsqMachineInfo> machineInfoMap = context.getAllMachineList().stream()
                .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, m -> m, (a, b) -> a));

        // 3. 逐机台处理剩余产能回填
        for (Map.Entry<String, List<GsqScheduleResultVo>> entry : machineSpecMap.entrySet()) {
            String machineCode = entry.getKey();
            List<GsqScheduleResultVo> specList = entry.getValue();
            GsqMachineInfo machine = machineInfoMap.get(machineCode);
            double machineQuota = getMachineQuota(machine);

            // 按班次顺序处理 1~5 班的剩余产能回填
            for (int classNum = 1; classNum <= 5; classNum++) {
                fillResidualCapacity(machineCode, specList, classNum, machineQuota, context);
            }

            // 第6班特殊处理：把剩余产能塞入第6班已排产的规格
            fillLastClassWithAllRemaining(machineCode, specList, machineQuota, context);
        }

        log.info("[S5.6] 最终剩余产能回填完成, 机台数:{}", machineSpecMap.size());

        // 总量截断保护：确保每个备库规格6班总排产不超过 backupTotalQty
        // 各阶段（S3延后不完全消化、S5均衡调整、S5.5定额延后等）可能导致总排产量超上限，此处从最后一班逐班削减
        capBackupTotalQty(scheduleList);
    }

    /**
     * 回填指定机台指定班次的剩余产能到该机台上的规格。
     *
     * <p>处理流程（对齐胎圈 TQ S5.6）：</p>
     * <ol>
     *   <li>统计该机台该班次已排产量（所有规格之和）</li>
     *   <li>计算剩余产能 = 机台定额 - 已排产量</li>
     *   <li>剩余产能 &gt; 0 时，按四级优先级排序候选规格（P-1当前新触发备库 → P-2前序备库按剩余量降序 → P-3非备库按供应时长升序）</li>
     *   <li>备库规格：受 backupRemainingQty 限制并扣减，需备份触发班次≤当前班且总量不超</li>
     *   <li>非备库规格：仅当该班次已有排产量时才回填（不新增班次排产）</li>
     * </ol>
     *
     * @param machineCode 机台编码
     * @param specList    该机台上所有规格的排程记录
     * @param classNum    班次号（1~5）
     * @param machineQuota 机台定额
     * @param context     排程上下文
     */
    private void fillResidualCapacity(String machineCode, List<GsqScheduleResultVo> specList,
                                      int classNum, double machineQuota, GsqScheduleContext context) {
        // 1. 统计该机台该班次已排产量
        double usedCapacity = 0D;
        for (GsqScheduleResultVo spec : specList) {
            usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(spec, classNum));
        }
        double residualCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity);
        if (residualCapacity <= 0) {
            // 无剩余产能，跳过
            return;
        }

        // 2. 候选规格 = 全部规格按四级优先级排序（备库优先于非备库，具体准入条件在循环内判断）
        List<GsqScheduleResultVo> candidates = specList.stream()
                .sorted(buildResidualPriorityComparator(classNum))
                .collect(Collectors.toList());

        // 3. 逐规格回填剩余产能（每次迭代前重新计算剩余产能，确保不超定额）
        for (GsqScheduleResultVo spec : candidates) {
            usedCapacity = 0D;
            for (GsqScheduleResultVo s : specList) {
                usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(s, classNum));
            }
            residualCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity);
            if (residualCapacity <= 0) {
                break;
            }

            boolean isBackupSpec = spec.getBackupTriggerClass() != null && spec.getBackupTriggerClass() > 0;
            double backupRemaining = spec.getBackupRemainingQty() == null ? 0D : spec.getBackupRemainingQty();
            // 回填量在分支内赋值，此处统一声明，供分支外埋点使用
            double assignQty = 0D;

            if (isBackupSpec) {
                // 备库规格：受 backupRemainingQty 限制（剩余备库量才能回填）
                if (backupRemaining <= 0) {
                    continue;
                }
                // 总量上限保护：6班已排产量已达到备库总量时不再回填
                double totalScheduled = sumClassPlanQty(spec);
                double backupTotal = spec.getBackupTotalQty() == null ? 0D : spec.getBackupTotalQty();
                if (totalScheduled >= backupTotal) {
                    continue;
                }
                // 备库触发班次 > 当前班次时，当前班次库存充足不需要排产，不回填
                Integer backupTriggerClass = spec.getBackupTriggerClass();
                if (backupTriggerClass != null && backupTriggerClass > classNum) {
                    continue;
                }
                // 回填量不超过备库总量与已排产量的差值，防止超排
                double maxAssignable = BigDecimalUtil.sub(backupTotal, totalScheduled);
                assignQty = Math.min(Math.min(residualCapacity, backupRemaining), maxAssignable);
                assignQty = applyRoundingIfNeeded(assignQty);
                if (assignQty <= 0) {
                    continue;
                }
                addClassPlanQty(spec, classNum, assignQty);
                spec.setBackupRemainingQty(BigDecimalUtil.sub(backupRemaining, assignQty));

                autoScheduleLogService.insertGsqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S5.6-剩余产能回填备库钢丝圈",
                        "钢丝圈代码：" + spec.getSteelRingCode() + "，机台：" + machineCode
                                + "，" + classNum + "班回填" + assignQty
                                + "，剩余备库量：" + spec.getBackupRemainingQty());
            } else {
                // 非备库规格：仅当该班次已有排产量时才回填（避免随意新增班次排产）
                // 业务规则：剩余产能优先补备库钢丝圈；非备库规格只在已排产的情况下补量
                double currentPlan = getClassPlanQty(spec, classNum);
                if (currentPlan <= 0) {
                    continue;
                }
                assignQty = applyRoundingIfNeeded(residualCapacity);
                if (assignQty <= 0) {
                    continue;
                }
                addClassPlanQty(spec, classNum, assignQty);

                autoScheduleLogService.insertGsqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S5.6-剩余产能回填非备库规格",
                        "钢丝圈代码：" + spec.getSteelRingCode() + "，机台：" + machineCode
                                + "，" + classNum + "班回填" + assignQty);
            }

            // 埋点剩余产能回填证据
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("machineCode", machineCode);
            evidence.put("classNum", classNum);
            evidence.put("assignQty", assignQty);
            context.getRuleTrace(spec.getSteelRingCode()).addRuleHit(
                    GsqScheduleRuleCodeEnum.RESIDUAL_CAPACITY_FILL,
                    GsqScheduleRuleResultEnum.ADJUST, evidence);
        }
    }

    /**
     * 第6班特殊处理：把剩余产能塞入第6班已排产的规格。
     *
     * <p>业务规则（对齐胎圈 TQ S5.6）：第6班是最后一个班次，无法再延后。
     * 备库规格优先塞入剩余备库量（不超过机台剩余产能）；非备库规格仅第6班已有排产才回填。</p>
     *
     * @param machineCode 机台编码
     * @param specList    该机台上所有规格的排程记录
     * @param machineQuota 机台定额
     * @param context     排程上下文
     */
    private void fillLastClassWithAllRemaining(String machineCode, List<GsqScheduleResultVo> specList,
                                               double machineQuota, GsqScheduleContext context) {
        int classNum = 6;

        // 候选规格 = 全部规格按四级优先级排序（备库优先，准入条件在循环内判断）
        List<GsqScheduleResultVo> candidates = specList.stream()
                .sorted(buildResidualPriorityComparator(classNum))
                .collect(Collectors.toList());

        for (GsqScheduleResultVo spec : candidates) {
            boolean isBackupSpec = spec.getBackupTriggerClass() != null && spec.getBackupTriggerClass() > 0;
            double backupRemaining = spec.getBackupRemainingQty() == null ? 0D : spec.getBackupRemainingQty();

            // 备库规格：剩余备库量为0则跳过；非备库规格：第6班已有排产量时才允许回填
            if (isBackupSpec && backupRemaining <= 0) {
                continue;
            }
            if (!isBackupSpec && getClassPlanQty(spec, classNum) <= 0) {
                continue;
            }

            // 统计第6班已排产量
            double usedCapacity = 0D;
            for (GsqScheduleResultVo s : specList) {
                usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(s, classNum));
            }
            double residualCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity);
            if (residualCapacity <= 0) {
                // 第6班机台已排满，无法塞入
                continue;
            }

            // 第6班塞入量 = min(剩余备库量, 机台剩余产能, 备库总量与已排产量差值)  [备库规格]
            //           或 = 机台剩余产能                                       [非备库规格]
            double assignQty;
            if (isBackupSpec) {
                double totalScheduled = sumClassPlanQty(spec);
                double backupTotal = spec.getBackupTotalQty() == null ? 0D : spec.getBackupTotalQty();
                double maxAssignable = BigDecimalUtil.sub(backupTotal, totalScheduled);
                if (maxAssignable <= 0) {
                    continue;
                }
                assignQty = Math.min(Math.min(backupRemaining, residualCapacity), maxAssignable);
            } else {
                assignQty = residualCapacity;
            }
            assignQty = applyRoundingIfNeeded(assignQty);
            if (assignQty <= 0) {
                continue;
            }
            addClassPlanQty(spec, classNum, assignQty);
            if (isBackupSpec) {
                spec.setBackupRemainingQty(BigDecimalUtil.sub(backupRemaining, assignQty));
            }

            autoScheduleLogService.insertGsqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                    isBackupSpec ? "S5.6-第6班塞入剩余备库量" : "S5.6-第6班回填非备库规格",
                    "钢丝圈代码：" + spec.getSteelRingCode() + "，机台：" + machineCode
                            + "，第6班塞入" + assignQty
                            + (isBackupSpec ? "，剩余备库量：" + spec.getBackupRemainingQty() : ""));

            // 埋点第6班回填证据
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("machineCode", machineCode);
            evidence.put("classNum", classNum);
            evidence.put("assignQty", assignQty);
            context.getRuleTrace(spec.getSteelRingCode()).addRuleHit(
                    GsqScheduleRuleCodeEnum.RESIDUAL_CAPACITY_FILL,
                    GsqScheduleRuleResultEnum.ADJUST, evidence);
        }
    }

    /**
     * 获取指定班次的计划量。
     */
    private double getClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum) {
        Object value = scheduleVo.getFieldValueByFieldName("class" + classNum + "PlanQty");
        return value == null ? 0D : ((Number) value).doubleValue();
    }

    /**
     * 累加指定班次的计划量。
     */
    private void addClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum, double addQty) {
        double current = getClassPlanQty(scheduleVo, classNum);
        double newValue = BigDecimalUtil.add(current, addQty);
        setClassPlanQtyDirect(scheduleVo, classNum, newValue);
    }

    /**
     * 直接设置指定班次的计划量（非累加）。
     * 当计划量为0或负数时，清空计划量字段，保持数据干净。
     */
    private void setClassPlanQtyDirect(GsqScheduleResultVo scheduleVo, int classNum, double value) {
        if (value <= 0) {
            scheduleVo.setFieldValueByFieldName("class" + classNum + "PlanQty", null);
        } else {
            scheduleVo.setFieldValueByFieldName("class" + classNum + "PlanQty", value);
        }
    }

    /**
     * 简单取整处理（避免浮点精度问题，保留2位小数）。
     */
    private double applyRoundingIfNeeded(double value) {
        return BigDecimalUtils.valueOf(value).doubleValue();
    }

    /**
     * 获取机台定额：优先使用机台自身的quata字段，否则使用兜底默认值。
     */
    private double getMachineQuota(GsqMachineInfo machine) {
        if (machine != null && machine.getQuata() != null && machine.getQuata().doubleValue() > 0) {
            return machine.getQuata().doubleValue();
        }
        return 1500D;
    }

    /**
     * 构建剩余产能回填的四级优先级排序器（对齐胎圈 TQ S5.6）。
     *
     * <p>排序规则：</p>
     * <ol>
     *   <li>P-1: 当前班次新触发备库（backupTriggerClass == 当前班次），最高优先</li>
     *   <li>P-2: 前序班次已触发备库（备库且非当前班次触发）</li>
     *   <li>同为备库：按 backupRemainingQty 降序（剩余缺口大的优先）</li>
     *   <li>P-3: 非备库规格按供应时长升序（越紧急越优先）</li>
     * </ol>
     *
     * @param currentClassNum 当前班次（用于判定 P-1 新触发备库）
     * @return 排序器
     */
    private Comparator<GsqScheduleResultVo> buildResidualPriorityComparator(int currentClassNum) {
        return (o1, o2) -> {
            boolean backup1 = o1.getBackupTriggerClass() != null && o1.getBackupTriggerClass() > 0;
            boolean backup2 = o2.getBackupTriggerClass() != null && o2.getBackupTriggerClass() > 0;

            // P-1: 当前班次新触发备库（最高优先级）
            boolean currentTrigger1 = backup1 && o1.getBackupTriggerClass() == currentClassNum;
            boolean currentTrigger2 = backup2 && o2.getBackupTriggerClass() == currentClassNum;
            if (currentTrigger1 != currentTrigger2) {
                return currentTrigger1 ? -1 : 1;
            }

            // P-2: 前序班次已触发备库
            boolean prevTrigger1 = backup1 && !currentTrigger1;
            boolean prevTrigger2 = backup2 && !currentTrigger2;
            if (prevTrigger1 != prevTrigger2) {
                return prevTrigger1 ? -1 : 1;
            }

            // 同为备库触发：按剩余需求缺口从大到小排序（缺口越大越优先）
            if (backup1 && backup2) {
                double rem1 = o1.getBackupRemainingQty() == null ? 0D : o1.getBackupRemainingQty();
                double rem2 = o2.getBackupRemainingQty() == null ? 0D : o2.getBackupRemainingQty();
                if (rem1 != rem2) {
                    return Double.compare(rem2, rem1); // 降序
                }
            }

            // P-3: 非备库规格按供应时长升序
            double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
            double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
            return Double.compare(st1, st2);
        };
    }

    /**
     * 重新计算所有备库规格的 backupRemainingQty。
     *
     * <p>背景（对齐胎圈 TQ S5.6）：S2 阶段分摊备份量后，S3 延后、S5 均衡、S5.5 定额校验都会修改计划量，
     * 且 S3 延后时未扣减 backupRemainingQty，导致该值与实际已排产量不一致。
     * 本方法在 S5.6 回填前统一修正：backupRemainingQty = backupTotalQty - 6班实际已排产量合计。</p>
     *
     * @param scheduleList 排程列表
     */
    private void recalcBackupRemainingQty(List<GsqScheduleResultVo> scheduleList) {
        int recalcCount = 0;
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            // 仅处理备库规格
            if (scheduleVo.getBackupTriggerClass() == null || scheduleVo.getBackupTriggerClass() <= 0) {
                continue;
            }
            // 若未保存初始备库总量（兼容旧数据），跳过重算
            if (scheduleVo.getBackupTotalQty() == null) {
                continue;
            }

            // 计算6班实际已排产量合计
            double actualTotalPlanQty = sumClassPlanQty(scheduleVo);

            // 重算剩余备库量 = 初始备库总量 - 已排产量
            double newRemaining = BigDecimalUtil.sub(scheduleVo.getBackupTotalQty(), actualTotalPlanQty);
            if (newRemaining < 0) {
                // 已超排，剩余量置0
                newRemaining = 0D;
            }
            scheduleVo.setBackupRemainingQty(newRemaining);
            recalcCount++;
        }
        if (recalcCount > 0) {
            log.info("[S5.6] 备库剩余量重算完成, 重算规格数:{}", recalcCount);
        }
    }

    /**
     * 总量截断保护：确保每个备库规格6班总排产不超过 backupTotalQty。
     *
     * <p>背景（对齐胎圈 TQ S5.6）：各阶段（S3延后未完全消化、S5均衡调整、S5.5定额延后等）
     * 可能导致6班总排产量超过备库总量上限。本方法从最后一班开始逐班削减超排量，
     * 保持前序班次排产稳定。</p>
     *
     * @param scheduleList 排程列表
     */
    private void capBackupTotalQty(List<GsqScheduleResultVo> scheduleList) {
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            // 仅处理备库规格
            if (scheduleVo.getBackupTriggerClass() == null || scheduleVo.getBackupTriggerClass() <= 0) {
                continue;
            }
            Double backupTotalQty = scheduleVo.getBackupTotalQty();
            if (backupTotalQty == null || backupTotalQty <= 0) {
                continue;
            }

            double totalScheduled = sumClassPlanQty(scheduleVo);
            double overflow = BigDecimalUtil.sub(totalScheduled, backupTotalQty);
            if (overflow <= 0) {
                continue;
            }

            // 从最后一班开始削减超排量
            for (int classNum = 6; classNum >= 1 && overflow > 0; classNum--) {
                double classPlan = getClassPlanQty(scheduleVo, classNum);
                if (classPlan <= 0) {
                    continue;
                }
                double deduct = Math.min(classPlan, overflow);
                double newPlan = BigDecimalUtil.sub(classPlan, deduct);
                setClassPlanQtyDirect(scheduleVo, classNum, newPlan);
                overflow = BigDecimalUtil.sub(overflow, deduct);
            }

            if (overflow > 0) {
                log.warn("[S5.6] 总量截断后仍有超排, 钢丝圈:{}, 剩余超排:{}",
                        scheduleVo.getSteelRingCode(), overflow);
            }

            // 截断后剩余备库量清零
            scheduleVo.setBackupRemainingQty(0D);
        }
    }

    /**
     * 计算规格6班已排产量合计。
     *
     * @param spec 排程记录
     * @return 6班已排产量合计
     */
    private double sumClassPlanQty(GsqScheduleResultVo spec) {
        double total = 0D;
        for (int i = 1; i <= 6; i++) {
            total = BigDecimalUtil.add(total, getClassPlanQty(spec, i));
        }
        return total;
    }
}