package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum;
import com.zlt.aps.tq.engine.strategy.TqDemandCalcHelper;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
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
 * <p>职责：在 S5.5 定额校验完成、所有计划量修改结束后，回收每个机台每班的剩余产能（quota - 已排产量），
 * 按三级优先级回填到该机台上的规格，避免产能浪费。</p>
 *
 * <p>触发场景：多规格机台上，备库胎圈因SYS1101029阈值初始只排了threshold量，
 * 排完其他规格后机台仍有剩余产能，需把剩余产能回填给备库胎圈；
 * 或机台上某些规格因机台定额限制延后到下一班，留下剩余产能可被其他规格补充。</p>
 *
 * <p>排产优先级（三级模型）：</p>
 * <ol>
 *   <li>Priority-1: 触发备库胎圈（backupTriggerClass &gt; 0 且 backupRemainingQty &gt; 0）
 *       <ul>
 *         <li>同组内按触发班次升序（先触发的优先继续排）</li>
 *         <li>同触发班次内按供应时长升序</li>
 *       </ul>
 *   </li>
 *   <li>Priority-2: 未触发备库但供应时长 &lt; 供应时长阈值的规格，按供应时长升序</li>
 *   <li>Priority-3: 未触发备库且供应时长 ≥ 供应时长阈值的规格，按供应时长升序</li>
 * </ol>
 *
 * <p>第6班特殊处理：把所有剩余量（含跨班次备库剩余量）塞入第6班，
 * 避免超出6班范围丢失量。第6班塞入量不超过机台剩余产能。</p>
 *
 * <p>单一规格机台：S3阶段已按机台定额满排，无剩余产能，本Handler不会触发回填。</p>
 *
 * <p>执行顺序说明（Phase 2 重构）：原 S3.5 位于 S3 之后，但 S4/S5/S5.5 会修改计划量导致回填结果被覆盖。
 * 现移至 S5.5 之后执行（即 S5.6），确保回填结果为最终结果且不被覆盖。
 * 执行前会重新计算 backupRemainingQty，基于实际已排产量修正（解决 S3 阶段 deferToNextClass 不扣减的问题）。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqResidualCapacityHandler extends AbsTqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Override
    protected String getStepName() {
        return "S5.6-最终剩余产能回填";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }

        // Phase 2 重构新增：执行回填前，重新计算 backupRemainingQty
        // 原因：S3 阶段 deferToNextClass 延后时不扣减 backupRemainingQty，导致该值偏大；
        //       S4/S5/S5.5 也会修改计划量，需基于最终实际已排产量修正剩余备库量。
        // 重算公式：backupRemainingQty = backupTotalPlanQty - 6班实际已排产量合计
        recalcBackupRemainingQty(scheduleList);

        // 供应时长阈值（用于Priority-2/3判定）
        double supplyTimeThreshold = getSupplyTimeThreshold(context);
        // 默认机台定额（兜底，优先使用机台自身quota）
        double defaultQuota = getDefaultQuota(context);

        // 1. 按机台分组，得到 machineCode → 该机台上所有规格的排程记录
        Map<String, List<TqScheduleResultVo>> machineSpecMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .collect(Collectors.groupingBy(TqScheduleResultVo::getMachineCode));

        if (machineSpecMap.isEmpty()) {
            log.info("[S5.6] 无已分配机台的规格，跳过剩余产能回填");
            return;
        }

        // 2. 构建机台信息Map，便于取quota
        Map<String, TqMachineInfo> machineInfoMap = context.getAllMachineList().stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineCode, m -> m, (a, b) -> a));

        // 3. 逐机台处理剩余产能回填
        for (Map.Entry<String, List<TqScheduleResultVo>> entry : machineSpecMap.entrySet()) {
            String machineCode = entry.getKey();
            List<TqScheduleResultVo> specList = entry.getValue();
            TqMachineInfo machine = machineInfoMap.get(machineCode);
            double machineQuota = getMachineQuota(machine, defaultQuota);

            // 按班次顺序处理 1~5 班的剩余产能回填
            for (int classNum = 1; classNum <= 5; classNum++) {
                fillResidualCapacity(machineCode, specList, classNum, machineQuota,
                        supplyTimeThreshold, context);
            }

            // 第6班特殊处理：把所有备库剩余量塞入第6班
            fillLastClassWithAllRemaining(machineCode, specList, machineQuota, context);
        }

        log.info("[S5.6] 最终剩余产能回填完成, 机台数:{}", machineSpecMap.size());

        // 总量截断保护：确保每个备库规格6班总排产不超过 backupTotalPlanQty
        // 各阶段（S3.2回填+forwardDigest不完全消化、S5均衡调整、S5.5定额延后等）
        // 可能导致总排产量超过备库总量上限，此处从最后一班开始逐班削减超排量
        capBackupTotalPlanQty(scheduleList);
    }

    /**
     * 总量截断保护：确保每个备库规格6班总排产不超过 backupTotalPlanQty。
     *
     * <p>背景：S2阶段 triggerBackupAndAllocate 计算出备库总量（backupTotalPlanQty）并分摊到各班次，
     * 但后续阶段（S3.2回填+forwardDigest不完全消化、S5均衡调整、S5.5定额延后等）
     * 可能导致6班总排产量超过备库总量上限。本方法从最后一班开始逐班削减超排量，
     * 确保总量不超。</p>
     *
     * <p>削减策略：从第6班倒序削减，优先削减尾部班次，保持前序班次排产稳定。</p>
     *
     * @param scheduleList 排程列表
     */
    private void capBackupTotalPlanQty(List<TqScheduleResultVo> scheduleList) {
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            // 仅处理备库胎圈
            if (scheduleVo.getBackupTriggerClass() == null || scheduleVo.getBackupTriggerClass() <= 0) {
                continue;
            }
            Double backupTotalPlanQty = scheduleVo.getBackupTotalPlanQty();
            if (backupTotalPlanQty == null || backupTotalPlanQty <= 0) {
                continue;
            }

            double totalScheduled = sumClassPlanQty(scheduleVo);
            double overflow = BigDecimalUtil.sub(totalScheduled, backupTotalPlanQty);
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

                log.info("[S5.6] 总量截断, 胎圈:{}, {}班削减{}, 剩余超排:{}",
                        scheduleVo.getBeadCode(), classNum, deduct, overflow);
            }

            if (overflow > 0) {
                log.warn("[S5.6] 总量截断后仍有超排, 胎圈:{}, 剩余超排:{}",
                        scheduleVo.getBeadCode(), overflow);
            }

            // 截断后重算backupRemainingQty
            scheduleVo.setBackupRemainingQty(0D);
        }
    }

    /**
     * 重新计算所有备库胎圈的 backupRemainingQty。
     *
     * <p>背景：S3 阶段 deferToNextClass 延后时未扣减 backupRemainingQty，S4/S5/S5.5 又修改了计划量，
     * 导致 backupRemainingQty 与实际已排产量不一致。本方法在 S5.6 回填前统一修正：</p>
     *
     * <p>重算公式：backupRemainingQty = backupTotalPlanQty - 6班实际已排产量合计</p>
     *
     * <p>处理逻辑：</p>
     * <ol>
     *   <li>仅处理备库胎圈（backupTriggerClass > 0）</li>
     *   <li>若 backupTotalPlanQty 为空（旧数据兼容），跳过重算保持原值</li>
     *   <li>计算6班实际已排产量合计</li>
     *   <li>剩余量 = 初始总需求 - 已排产量，若为负则置0（已超排）</li>
     * </ol>
     *
     * @param scheduleList 排程列表
     */
    private void recalcBackupRemainingQty(List<TqScheduleResultVo> scheduleList) {
        int recalcCount = 0;
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            // 仅处理备库胎圈
            if (scheduleVo.getBackupTriggerClass() == null || scheduleVo.getBackupTriggerClass() <= 0) {
                continue;
            }
            // 若未保存初始总需求量（旧数据兼容），跳过重算
            if (scheduleVo.getBackupTotalPlanQty() == null) {
                continue;
            }

            // 计算6班实际已排产量合计
            double actualTotalPlanQty = 0D;
            for (int classNum = 1; classNum <= 6; classNum++) {
                actualTotalPlanQty = BigDecimalUtil.add(actualTotalPlanQty, getClassPlanQty(scheduleVo, classNum));
            }

            // 重算剩余备库量 = 初始总需求 - 已排产量
            double newRemaining = BigDecimalUtil.sub(scheduleVo.getBackupTotalPlanQty(), actualTotalPlanQty);
            if (newRemaining < 0) {
                // 已超排，剩余量置0
                newRemaining = 0D;
            }

            double oldRemaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
            scheduleVo.setBackupRemainingQty(newRemaining);
            recalcCount++;

            log.info("[S5.6] 重算备库剩余量, 胎圈:{}, 初始总需求:{}, 已排产量:{}, 旧剩余量:{}, 新剩余量:{}",
                    scheduleVo.getBeadCode(), scheduleVo.getBackupTotalPlanQty(),
                    actualTotalPlanQty, oldRemaining, newRemaining);
        }
        if (recalcCount > 0) {
            log.info("[S5.6] 备库剩余量重算完成, 重算规格数:{}", recalcCount);
        }
    }

    /**
     * 回填指定机台指定班次的剩余产能到该机台上的规格。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>统计该机台该班次已排产量（所有规格之和）</li>
     *   <li>计算剩余产能 = quota - 已排产量</li>
     *   <li>剩余产能 &gt; 0 时，按三级优先级排序候选规格</li>
     *   <li>逐规格回填：备库胎圈受backupRemainingQty限制，其他规格不受限</li>
     * </ol>
     *
     * @param machineCode 机台编码
     * @param specList 该机台上所有规格的排程记录
     * @param classNum 班次号（1~5）
     * @param machineQuota 机台定额
     * @param supplyTimeThreshold 供应时长阈值
     * @param context 排程上下文
     */
    private void fillResidualCapacity(String machineCode, List<TqScheduleResultVo> specList,
                                      int classNum, double machineQuota, double supplyTimeThreshold,
                                      TqScheduleContext context) {
        // 1. 统计该机台该班次已排产量
        double usedCapacity = 0D;
        for (TqScheduleResultVo spec : specList) {
            usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(spec, classNum));
        }
        double residualCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity);
        if (residualCapacity <= 0) {
            // 无剩余产能，跳过
            return;
        }

        // 2. 按三级优先级排序候选规格（传入当前班次用于判定P-1）
        List<TqScheduleResultVo> candidates = specList.stream()
                .sorted(buildResidualPriorityComparator(supplyTimeThreshold, classNum))
                .collect(Collectors.toList());

        // 诊断日志：输出S5.6排序详情，用于排查兜底回填是否覆盖S3.2结果
        log.info("[S5.6-DIAG] 机台:{} {}班 剩余产能={} 候选规格排序结果:",
                machineCode, classNum, residualCapacity);
        for (TqScheduleResultVo spec : candidates) {
            log.info("[S5.6-DIAG]   beadCode={} supplyTime={} backupRemainingQty={} triggerClass={} currentPlan={}",
                    spec.getBeadCode(),
                    spec.getSupplyTime(),
                    spec.getBackupRemainingQty(),
                    spec.getBackupTriggerClass(),
                    getClassPlanQty(spec, classNum));
        }

        // 3. 逐规格回填剩余产能（每次迭代前重新计算剩余产能，确保不超定额）
        for (TqScheduleResultVo spec : candidates) {
            // 每次迭代前重新计算剩余产能，防止累积超量
            usedCapacity = 0D;
            for (TqScheduleResultVo s : specList) {
                usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(s, classNum));
            }
            residualCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity);

            if (residualCapacity <= 0) {
                break;
            }

            boolean isBackupSpec = spec.getBackupTriggerClass() != null && spec.getBackupTriggerClass() > 0;
            double backupRemaining = spec.getBackupRemainingQty() == null ? 0D : spec.getBackupRemainingQty();

            if (isBackupSpec) {
                // 备库胎圈：受 backupRemainingQty 限制（剩余备库量才能回填）
                if (backupRemaining <= 0) {
                    continue;
                }
                // 总量上限保护：如果6班已排产量已达到备库总量，不再回填
                // 防止S5均衡调整削减计划量后，S5.6 recalcBackupRemainingQty 误判为未排满而重复回填
                double totalScheduled = sumClassPlanQty(spec);
                double backupTotal = spec.getBackupTotalPlanQty() == null ? 0D : spec.getBackupTotalPlanQty();
                if (totalScheduled >= backupTotal) {
                    continue;
                }
                // 备库触发班次 > 当前班次时，当前班次库存充足不需要排产，不回填
                // 典型场景：023规格3班才触发备库，1/2班库存充足不应被回填备库量
                Integer backupTriggerClass = spec.getBackupTriggerClass();
                if (backupTriggerClass != null && backupTriggerClass > classNum) {
                    continue;
                }
                // 回填量不超过备库总量与已排产量的差值，防止超排
                double maxAssignable = BigDecimalUtil.sub(backupTotal, totalScheduled);
                double assignQty = Math.min(Math.min(residualCapacity, backupRemaining), maxAssignable);
                assignQty = applyRoundingIfNeeded(assignQty);
                addClassPlanQty(spec, classNum, assignQty);
                spec.setBackupRemainingQty(BigDecimalUtil.sub(backupRemaining, assignQty));

                autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S5.6-剩余产能回填备库胎圈",
                        "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                                + "，" + classNum + "班回填" + assignQty
                                + "，剩余备库量：" + spec.getBackupRemainingQty());

                // Phase 2 重构新增：埋点剩余产能回填证据
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("machineCode", machineCode);
                evidence.put("classNum", classNum);
                evidence.put("assignQty", assignQty);
                evidence.put("backupRemaining", spec.getBackupRemainingQty());
                TqDemandCalcHelper.addRuleTrace(context, spec.getBeadCode(),
                        TqScheduleRuleCodeEnum.RESIDUAL_CAPACITY_FILL,
                        TqScheduleRuleResultEnum.ADJUST,
                        evidence);
            } else {
                // 非备库规格：仅当该班次已有排产量时才回填（避免随意新增班次排产）
                // 业务规则：剩余产能优先补备库胎圈；非备库规格只在已排产的情况下补量
                double currentPlan = getClassPlanQty(spec, classNum);
                if (currentPlan <= 0) {
                    continue;
                }

                // Phase 2 重构修复（方案C）：非备库规格也回填剩余产能，避免机台产能浪费
                // 原逻辑仅由备库胎圈消化剩余产能，当机台上无备库胎圈或备库剩余量为0时，产能被浪费
                // 现修改为：非备库规格在已有排产的班次上补充剩余产能（不新增班次，仅补量）
                double assignQty = residualCapacity;
                assignQty = applyRoundingIfNeeded(assignQty);
                addClassPlanQty(spec, classNum, assignQty);

                autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S5.6-剩余产能回填非备库规格",
                        "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                                + "，" + classNum + "班回填" + assignQty);

                // 埋点非备库规格回填证据
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("machineCode", machineCode);
                evidence.put("classNum", classNum);
                evidence.put("assignQty", assignQty);
                TqDemandCalcHelper.addRuleTrace(context, spec.getBeadCode(),
                        TqScheduleRuleCodeEnum.RESIDUAL_CAPACITY_FILL,
                        TqScheduleRuleResultEnum.ADJUST,
                        evidence);
            }
        }
    }

    /**
     * 第6班特殊处理：把所有备库剩余量塞入第6班。
     *
     * <p>业务规则：第6班是最后一个班次，无法再延后。为避免备库量丢失，
     * 把每个规格的backupRemainingQty全部塞入第6班（不超过机台剩余产能）。</p>
     *
     * <p>注意：第6班塞入量 = min(剩余备库量, 机台第6班剩余产能)。
     * 若机台第6班剩余产能不足，超出部分记录日志（极少出现，因为S3已按延后策略分配）。</p>
     *
     * @param machineCode 机台编码
     * @param specList 该机台上所有规格的排程记录
     * @param machineQuota 机台定额
     * @param context 排程上下文
     */
    private void fillLastClassWithAllRemaining(String machineCode, List<TqScheduleResultVo> specList,
                                               double machineQuota, TqScheduleContext context) {
        int classNum = 6;

        // 按优先级排序，确保备库胎圈优先塞入第6班（classNum=6）
        double supplyTimeThreshold = getSupplyTimeThreshold(context);
        List<TqScheduleResultVo> candidates = specList.stream()
                .sorted(buildResidualPriorityComparator(supplyTimeThreshold, classNum))
                .collect(Collectors.toList());

        for (TqScheduleResultVo spec : candidates) {
            boolean isBackupSpec = spec.getBackupTriggerClass() != null && spec.getBackupTriggerClass() > 0;
            double backupRemaining = spec.getBackupRemainingQty() == null ? 0D : spec.getBackupRemainingQty();

            // 备库胎圈：剩余备库量为0则跳过
            // 非备库规格：第6班已有排产量时才允许回填（避免新增班次），与1~5班逻辑保持一致
            if (isBackupSpec && backupRemaining <= 0) {
                continue;
            }
            // 总量上限保护：备库胎圈6班已排产量已达到备库总量时，不再回填
            if (isBackupSpec) {
                double totalScheduled = sumClassPlanQty(spec);
                double backupTotal = spec.getBackupTotalPlanQty() == null ? 0D : spec.getBackupTotalPlanQty();
                if (totalScheduled >= backupTotal) {
                    continue;
                }
            }
            if (!isBackupSpec) {
                double currentPlan = getClassPlanQty(spec, classNum);
                if (currentPlan <= 0) {
                    continue;
                }
            }

            // 统计第6班已排产量
            double usedCapacity = 0D;
            for (TqScheduleResultVo s : specList) {
                usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(s, classNum));
            }
            double residualCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity);
            if (residualCapacity <= 0) {
                // 第6班机台已排满，剩余量无法塞入，记录日志
                autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S5.6-第6班塞入失败(机台已满)",
                        "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                                + (isBackupSpec ? "，剩余备库量" + backupRemaining : "")
                                + "无法塞入第6班");
                continue;
            }

            // 第6班塞入量 = min(剩余备库量, 机台剩余产能, 备库总量与已排产量差值)  [备库胎圈]
            //           或 = 机台剩余产能                                            [非备库规格]
            double assignQty;
            if (isBackupSpec) {
                double totalScheduled = sumClassPlanQty(spec);
                double backupTotal = spec.getBackupTotalPlanQty() == null ? 0D : spec.getBackupTotalPlanQty();
                double maxAssignable = BigDecimalUtil.sub(backupTotal, totalScheduled);
                if (maxAssignable <= 0) {
                    continue;
                }
                assignQty = Math.min(Math.min(backupRemaining, residualCapacity), maxAssignable);
            } else {
                assignQty = residualCapacity;
            }
            assignQty = applyRoundingIfNeeded(assignQty);
            addClassPlanQty(spec, classNum, assignQty);
            if (isBackupSpec) {
                spec.setBackupRemainingQty(BigDecimalUtil.sub(backupRemaining, assignQty));
            }

            autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                    isBackupSpec ? "S5.6-第6班塞入剩余备库量" : "S5.6-第6班回填非备库规格",
                    "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                            + "，第6班塞入" + assignQty
                            + (isBackupSpec ? "，剩余备库量：" + spec.getBackupRemainingQty() : ""));

            // Phase 2 重构新增：埋点第6班塞入剩余备库量证据
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("machineCode", machineCode);
            evidence.put("classNum", 6);
            evidence.put("assignQty", assignQty);
            evidence.put("backupRemaining", spec.getBackupRemainingQty());
            TqDemandCalcHelper.addRuleTrace(context, spec.getBeadCode(),
                    TqScheduleRuleCodeEnum.RESIDUAL_CAPACITY_FILL,
                    TqScheduleRuleResultEnum.ADJUST,
                    evidence);
        }
    }

    /**
     * 构建剩余产能回填的三级优先级排序器。
     *
     * <p>排序规则：</p>
     * <ol>
     *   <li>备库胎圈优先（backupTriggerClass &gt; 0）</li>
     *   <li>同为备库胎圈：按触发班次升序（先触发的优先继续排）</li>
     *   <li>同组内按供应时长升序（短=紧急=优先排，与S3.2口径一致）</li>
     * </ol>
     *
     * @param supplyTimeThreshold 供应时长阈值
     * @return 排序器
     */
    private Comparator<TqScheduleResultVo> buildResidualPriorityComparator(
            double supplyTimeThreshold, int currentClassNum) {
        return (o1, o2) -> {
            boolean backup1 = o1.getBackupTriggerClass() != null && o1.getBackupTriggerClass() > 0;
            boolean backup2 = o2.getBackupTriggerClass() != null && o2.getBackupTriggerClass() > 0;

            // P-1: 当前班次新触发备库（最高优先级）
            boolean currentTrigger1 = backup1 && o1.getBackupTriggerClass().equals(currentClassNum);
            boolean currentTrigger2 = backup2 && o2.getBackupTriggerClass().equals(currentClassNum);
            if (currentTrigger1 != currentTrigger2) {
                return currentTrigger1 ? -1 : 1;
            }

            // P-2: 前序班次已触发备库
            boolean prevTrigger1 = backup1 && !currentTrigger1;
            boolean prevTrigger2 = backup2 && !currentTrigger2;
            if (prevTrigger1 != prevTrigger2) {
                return prevTrigger1 ? -1 : 1;
            }

            // 备库与非备库规格均按供应时长升序排序（供应时长短=紧急=优先排）
            double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
            double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
            return Double.compare(st1, st2);
        };
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
     * 累加指定班次的计划量
     */
    private void addClassPlanQty(TqScheduleResultVo scheduleVo, int classNum, double addQty) {
        double current = getClassPlanQty(scheduleVo, classNum);
        double newValue = BigDecimalUtil.add(current, addQty);
        setClassPlanQtyDirect(scheduleVo, classNum, newValue);
    }

    /**
     * 直接设置指定班次的计划量（非累加）。
     * 当计划量为0或负数时，清空计划量、生产顺序和排程分析字段，保持数据干净。
     */
    private void setClassPlanQtyDirect(TqScheduleResultVo scheduleVo, int classNum, double value) {
        if (value <= 0) {
            // 无排产量时清空所有字段，避免入库时留下0值或残留顺序值
            switch (classNum) {
                case 1: scheduleVo.setClass1PlanQty(null); scheduleVo.setClass1ProduceOrder(null); scheduleVo.setClass1SysAnalysis(null); break;
                case 2: scheduleVo.setClass2PlanQty(null); scheduleVo.setClass2ProduceOrder(null); scheduleVo.setClass2SysAnalysis(null); break;
                case 3: scheduleVo.setClass3PlanQty(null); scheduleVo.setClass3ProduceOrder(null); scheduleVo.setClass3SysAnalysis(null); break;
                case 4: scheduleVo.setClass4PlanQty(null); scheduleVo.setClass4ProduceOrder(null); scheduleVo.setClass4SysAnalysis(null); break;
                case 5: scheduleVo.setClass5PlanQty(null); scheduleVo.setClass5ProduceOrder(null); scheduleVo.setClass5SysAnalysis(null); break;
                case 6: scheduleVo.setClass6PlanQty(null); scheduleVo.setClass6ProduceOrder(null); scheduleVo.setClass6SysAnalysis(null); break;
            }
        } else {
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

    /**
     * 简单取整处理（避免浮点精度问题，保留2位小数）
     */
    private double applyRoundingIfNeeded(double value) {
        return BigDecimalUtils.valueOf(value).doubleValue();
    }

    /**
     * 计算规格6班已排产量合计。
     *
     * @param spec 排程记录
     * @return 6班已排产量合计
     */
    private double sumClassPlanQty(TqScheduleResultVo spec) {
        double total = 0D;
        for (int i = 1; i <= 6; i++) {
            total = BigDecimalUtil.add(total, getClassPlanQty(spec, i));
        }
        return total;
    }

    /**
     * 获取供应时长阈值
     */
    private double getSupplyTimeThreshold(TqScheduleContext context) {
        return context.getParams().getSupplyTimeThreshold() == null ? 24D
                : context.getParams().getSupplyTimeThreshold();
    }

    /**
     * 获取默认机台定额（兜底值）
     */
    private double getDefaultQuota(TqScheduleContext context) {
        return context.getParams().getMaxClassOutput() == null ? 3000D
                : context.getParams().getMaxClassOutput();
    }

    /**
     * 获取机台定额：优先使用机台自身的quota字段，否则使用全局默认值
     */
    private double getMachineQuota(TqMachineInfo machine, double defaultQuota) {
        if (machine != null && machine.getQuota() != null && machine.getQuota() > 0) {
            return machine.getQuota();
        }
        return defaultQuota;
    }
}
