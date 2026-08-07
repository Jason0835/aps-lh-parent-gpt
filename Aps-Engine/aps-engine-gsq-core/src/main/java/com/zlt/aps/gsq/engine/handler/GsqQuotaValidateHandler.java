package com.zlt.aps.gsq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
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
 * S5.5: 钢丝圈定额校验与顺序重置Handler。
 *
 * <p>职责（对齐胎圈TQ）：在S4(停产协调)和S5(班次均衡)修改计划量之后，对所有机台所有班次进行定额校验，
 * 确保单机台单班次排产量不超过机台定额（quota），超出部分按优先级延后到下一班次；
 * 最后统一重置6个班次的生产顺序值。</p>
 *
 * <p>背景：S3阶段分配机台时按定额控制排产，但S4/S5修改计划量时不检查定额，
 * 可能导致单机台单班次总排产量超过quota。本Handler在所有计划量修改完成后统一校验修正。</p>
 *
 * <p>处理流程：</p>
 * <ol>
 *   <li>按机台分组所有已分配机台的排程记录</li>
 *   <li>逐机台逐班次（1→6）校验：Σ(同机台同班次计划量) ≤ quota</li>
 *   <li>若超量：按供应时长降序排序规格（供应时长大的先延后），将超出部分延后到下一班</li>
 *   <li>第6班超量无法延后，仅记录日志</li>
 *   <li>调用{@code GsqMachineAssignHandler.setProduceOrder()}重置所有班次生产顺序</li>
 * </ol>
 *
 * <p>无备库版：钢丝圈当前无备库字段与概念，故不包含胎圈"S5.5跳过备库触发班次延后"的逻辑。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqQuotaValidateHandler extends AbsGsqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Resource
    private GsqMachineAssignHandler machineAssignHandler;

    @Override
    protected String getStepName() {
        return "S5.5-定额校验与顺序重置";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }

        // 1. 按机台分组，得到 machineCode → 该机台上所有规格的排程记录
        Map<String, List<GsqScheduleResultVo>> machineSpecMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .collect(Collectors.groupingBy(GsqScheduleResultVo::getMachineCode));

        if (machineSpecMap.isEmpty()) {
            log.info("[S5.5] 无已分配机台的规格，跳过定额校验");
            return;
        }

        // 2. 构建机台信息Map，便于取quota
        Map<String, GsqMachineInfo> machineInfoMap = context.getAllMachineList().stream()
                .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, m -> m, (a, b) -> a));

        // 3. 默认机台定额（兜底，优先使用机台自身quata）
        double defaultQuota = context.getParams().getWrappingMachineQuota() == null ? 1500D
                : context.getParams().getWrappingMachineQuota();

        // 4. 逐机台处理定额校验
        int totalOverflowCount = 0;
        for (Map.Entry<String, List<GsqScheduleResultVo>> entry : machineSpecMap.entrySet()) {
            String machineCode = entry.getKey();
            List<GsqScheduleResultVo> specList = entry.getValue();
            GsqMachineInfo machine = machineInfoMap.get(machineCode);
            double machineQuota = getMachineQuota(machine, defaultQuota);

            totalOverflowCount += validateAndDeferOverflow(context, machineCode, specList, machineQuota);
        }

        log.info("[S5.5] 定额校验完成, 机台数:{}, 累计超量延后次数:{}", machineSpecMap.size(), totalOverflowCount);

        // 5. 所有计划量修改完成后，统一重置6个班次的生产顺序
        machineAssignHandler.setProduceOrder(scheduleList);
        log.info("[S5.5] 生产顺序重置完成");

        // 埋点生产顺序重置证据（每条已分配机台的排程记录都受影响）
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineCode())) {
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("totalOverflowCount", totalOverflowCount);
                context.getRuleTrace(scheduleVo.getSteelRingCode()).addRuleHit(
                        GsqScheduleRuleCodeEnum.PRODUCE_ORDER_RESET,
                        GsqScheduleRuleResultEnum.ADJUST, evidence);
            }
        }
    }

    /**
     * 校验指定机台所有班次的排产量是否超过定额，超出部分延后到下一班。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>逐班次（1→6）按业务优先级顺序填产能</li>
     *   <li>高优先级规格优先占用机台产能，超出定额部分延后到下一班，当班严格不超机台定额</li>
     *   <li>第6班超量无法延后，仅记录日志</li>
     * </ol>
     *
     * @param context               排程上下文（用于规则证据埋点）
     * @param machineCode           机台编码
     * @param specList              该机台上所有规格的排程记录
     * @param machineQuota          机台定额
     * @return 累计超量延后次数
     */
    private int validateAndDeferOverflow(GsqScheduleContext context, String machineCode,
                                         List<GsqScheduleResultVo> specList, double machineQuota) {
        int overflowCount = 0;

        // 有效定额：严格按机台定额封顶（不叠加超排容忍阈值，满足按机台定额排满剩余产能的需求）
        double effectiveQuota = machineQuota;

        // 尾量合并阈值：单班剩余很小且下一班该规格无排产时，合并到当前班排完，避免单独开一只剩尾量的小班次
        double tailThreshold = getTailThreshold(context);

        // 逐班次处理（1→6），延后时会累加到下一班，所以需要顺序处理
        for (int classNum = 1; classNum <= 6; classNum++) {
            // 按业务优先级排序该机台规格：①胎圈不消耗备库 → ②当班触发备库 → ③备库(供应时长升序) → ④非备库(供应时长升序)
            // 高优先级规格优先占用机台产能，低优先级规格的超出部分顺延到下一班
            int finalClassNum = classNum;
            List<GsqScheduleResultVo> sortedSpecs = specList.stream()
                    .filter(s -> getClassPlanQty(s, finalClassNum) > 0)
                    .sorted(buildCapacityPriorityComparator(finalClassNum))
                    .collect(Collectors.toList());

            // 逐班次按优先级顺序填产能：availableCapacity 从机台有效定额开始逐规格扣减
            double availableCapacity = effectiveQuota;
            for (GsqScheduleResultVo spec : sortedSpecs) {
                double plan = getClassPlanQty(spec, classNum);
                if (plan <= 0) {
                    continue;
                }

                // 产能已耗尽：当前班级该规格计划量清零，全部延后到下一班，确保当班严格不超机台定额
                if (availableCapacity <= 0) {
                    setClassPlanQty(spec, classNum, 0D);
                    overflowCount += this.deferQty(context, spec, machineCode, classNum, plan, machineQuota);
                    continue;
                }

                // 实际可排量 = min(该规格当前班计划量, 机台剩余产能)
                double assignQty = Math.min(plan, availableCapacity);
                double remainder = BigDecimalUtil.sub(plan, assignQty);

                // 尾量合并：剩余量很小、下一班该规格无排产且合并后不超机台定额时，合并到当前班排完，
                // 避免单独开一只剩尾量的小班次；合并会超定额则放弃合并，改为延后到下一班
                if (remainder > 0 && this.shouldMergeTail(spec, classNum, plan, remainder, availableCapacity, tailThreshold)) {
                    assignQty = plan;
                    remainder = 0D;
                }

                setClassPlanQty(spec, classNum, assignQty);
                availableCapacity = BigDecimalUtil.sub(availableCapacity, assignQty);

                // 仍有剩余量：延后到下一班（或第6班无法延后仅记录日志）
                if (remainder > 0) {
                    overflowCount += this.deferQty(context, spec, machineCode, classNum, remainder, machineQuota);
                }
            }
        }

        return overflowCount;
    }

    /**
     * 将超出量延后到下一班次累加。
     *
     * @param scheduleVo    排程记录
     * @param currentClass  当前班次（1~5）
     * @param overflowQty   延后量
     */
    private void deferToNextClass(GsqScheduleResultVo scheduleVo, int currentClass, double overflowQty) {
        double nextClassQty = getClassPlanQty(scheduleVo, currentClass + 1);
        setClassPlanQty(scheduleVo, currentClass + 1, BigDecimalUtil.add(nextClassQty, overflowQty));
    }

    /**
     * 将指定规格当前班次的超出/剩余计划量延后到下一班次累加（第6班无法延后仅记录日志）。
     *
     * @param context       排程上下文（用于日志与规则证据埋点）
     * @param spec          待延后规格
     * @param machineCode   机台编码
     * @param classNum      当前班次（1~6）
     * @param qty           延后量
     * @param machineQuota  机台定额（证据埋点用）
     * @return 1 表示实际延后到下一班；0 表示第6班无法延后仅记录日志
     */
    private int deferQty(GsqScheduleContext context, GsqScheduleResultVo spec,
                         String machineCode, int classNum, double qty, double machineQuota) {
        if (qty <= 0) {
            return 0;
        }

        if (classNum < 6) {
            // 延后到下一班
            deferToNextClass(spec, classNum, qty);

            autoScheduleLogService.insertGsqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                    "S5.5-定额校验超量延后",
                    "钢丝圈代码：" + spec.getSteelRingCode() + "，机台：" + machineCode
                            + "，" + classNum + "班超定额，扣减" + qty
                            + "延后至" + (classNum + 1) + "班");

            // 埋点定额超出延后证据
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("machineCode", machineCode);
            evidence.put("classNum", classNum);
            evidence.put("deductQty", qty);
            evidence.put("machineQuota", machineQuota);
            context.getRuleTrace(spec.getSteelRingCode()).addRuleHit(
                    GsqScheduleRuleCodeEnum.QUOTA_EXCEED_DEFER,
                    GsqScheduleRuleResultEnum.ADJUST, evidence);
            return 1;
        }

        // 第6班无法延后，记录日志
        autoScheduleLogService.insertGsqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                "S5.5-第6班超定额无法延后",
                "钢丝圈代码：" + spec.getSteelRingCode() + "，机台：" + machineCode
                        + "，第6班超定额" + qty + "无法延后");

        // 埋点第6班超定额无法延后（SKIP）
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("machineCode", machineCode);
        evidence.put("classNum", 6);
        evidence.put("deductQty", qty);
        evidence.put("machineQuota", machineQuota);
        context.getRuleTrace(spec.getSteelRingCode()).addRuleHit(
                GsqScheduleRuleCodeEnum.QUOTA_EXCEED_DEFER,
                GsqScheduleRuleResultEnum.SKIP, evidence);
        return 0;
    }

    /**
     * 判断剩余量是否应合并到当前班次排完（尾量合并）。
     *
     * <p>规则：剩余量 ≤ 尾量合并阈值，且（第6班 或 下一班该规格无排产）时可考虑合并，
     * 但必须满足「合并后不超出机台定额」（当前剩余产能 ≥ 完整计划量），否则放弃合并改为延后。</p>
     *
     * @param spec             待判断规格
     * @param classNum         当前班次（1~6）
     * @param plan             该规格当前班次完整计划量
     * @param remainder        剩余量
     * @param availableCapacity 当前班次剩余可用产能
     * @param tailThreshold    尾量合并阈值
     * @return true 表示合并到当前班排完
     */
    private boolean shouldMergeTail(GsqScheduleResultVo spec, int classNum, double plan,
                                    double remainder, double availableCapacity, double tailThreshold) {
        if (remainder <= 0 || remainder > tailThreshold) {
            return false;
        }
        // 合并后不得导致当班超出机台定额：当前剩余产能需足够容纳完整计划量
        if (availableCapacity < plan) {
            return false;
        }
        if (classNum >= 6) {
            // 第6班无后续班次，小尾量直接合并排完，避免丢弃
            return true;
        }
        // 中间班次：仅当下一班该规格无排产时才合并，避免把可并入下一班正常生产的量强行叠加
        double nextClassPlan = getClassPlanQty(spec, classNum + 1);
        return nextClassPlan <= 0;
    }

    /**
     * 获取尾量合并阈值（复用工装车整车容量参数，默认120）。
     *
     * @param context 排程上下文
     * @return 尾量合并阈值
     */
    private double getTailThreshold(GsqScheduleContext context) {
        Double toolCapacity = context.getParams().getToolCapacity();
        return toolCapacity == null || toolCapacity <= 0 ? 120D : toolCapacity;
    }

    /**
     * 构建该机台规格的产能分配优先级排序器（顺序填产能使用）。
     *
     * <p>排序规则：</p>
     * <ol>
     *   <li>P-0: 胎圈不消耗但触发备库的规格最高优先（库存完全不足、纯靠备库，最紧急）</li>
     *   <li>P-1: 当班触发备库（backupTriggerClass == 当前班次）</li>
     *   <li>P-2: 备库规格优先于非备库规格</li>
     *   <li>P-3: 组内按供应时长升序（供应时长短的先排，越高优先占用产能）</li>
     * </ol>
     *
     * @param classNum 当前班次
     * @return 产能分配优先级排序器
     */
    private Comparator<GsqScheduleResultVo> buildCapacityPriorityComparator(int classNum) {
        return (o1, o2) -> {
            // P-0: 胎圈每班消耗为0（supplyTime=MAX）但触发备库的规格最高优先，
            //      此类规格库存完全不足、纯靠备库，业务上最紧急，不受除法口径影响
            boolean special1 = isTireBeadNotConsumedBackup(o1);
            boolean special2 = isTireBeadNotConsumedBackup(o2);
            if (special1 != special2) {
                return special1 ? -1 : 1;
            }

            // P-1: 当班触发备库最高优先
            boolean currentTrigger1 = isBackupTriggeredClass(o1, classNum);
            boolean currentTrigger2 = isBackupTriggeredClass(o2, classNum);
            if (currentTrigger1 != currentTrigger2) {
                return currentTrigger1 ? -1 : 1;
            }

            // P-2: 备库规格优先于非备库规格
            boolean backup1 = isBackupSpec(o1);
            boolean backup2 = isBackupSpec(o2);
            if (backup1 != backup2) {
                return backup1 ? -1 : 1;
            }

            // P-3: 组内按供应时长升序（供应时长短的先排）
            double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
            double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
            return Double.compare(st1, st2);
        };
    }

    /**
     * 判断是否为「胎圈每班消耗为0但触发备库」的规格（最高优先）。
     *
     * <p>胎圈每班消耗≤0 时供应时长被计为 Double.MAX_VALUE（见 GsqStockPredictHandler），
     * 但该规格仍触发备库说明库存完全不足、纯靠备库，业务上最紧急。</p>
     *
     * @param vo 规格
     * @return true 表示胎圈不消耗但触发备库
     */
    private boolean isTireBeadNotConsumedBackup(GsqScheduleResultVo vo) {
        return isBackupSpec(vo)
                && vo.getSupplyTime() != null
                && vo.getSupplyTime() == Double.MAX_VALUE;
    }

    /**
     * 判断是否为备库规格（backupTriggerClass > 0）。
     *
     * @param vo 规格
     * @return true 表示备库规格
     */
    private boolean isBackupSpec(GsqScheduleResultVo vo) {
        return vo.getBackupTriggerClass() != null && vo.getBackupTriggerClass() > 0;
    }

    /**
     * 获取指定班次的计划量
     */
    private double getClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum) {
        Object value = scheduleVo.getFieldValueByFieldName("class" + classNum + "PlanQty");
        return value == null ? 0D : ((Number) value).doubleValue();
    }

    /**
     * 设置指定班次的计划量
     */
    private void setClassPlanQty(GsqScheduleResultVo scheduleVo, int classNum, double value) {
        scheduleVo.setFieldValueByFieldName("class" + classNum + "PlanQty", value);
    }

    /**
     * 判断指定班次是否为该规格的备库触发班次（对齐胎圈：备库触发班次不削减计划量）。
     */
    private boolean isBackupTriggeredClass(GsqScheduleResultVo spec, int classNum) {
        return spec.getBackupTriggerClass() != null && spec.getBackupTriggerClass() == classNum;
    }

    /**
     * 获取机台定额：优先使用机台自身的quata字段，否则使用全局默认值
     */
    private double getMachineQuota(GsqMachineInfo machine, double defaultQuota) {
        if (machine != null && machine.getQuata() != null && machine.getQuata().doubleValue() > 0) {
            return machine.getQuata().doubleValue();
        }
        return defaultQuota;
    }
}