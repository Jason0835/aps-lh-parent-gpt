package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.domain.TqMachineCandidate;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum;
import com.zlt.aps.tq.engine.service.impl.TqTaskChainScheduleService;
import com.zlt.aps.tq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.tq.engine.strategy.TqDemandCalcHelper;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTaskNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S3: 班次排产分配Handler（6班次版本）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>按优先级排序排程记录（定点机台优先 → 已排产规格优先 → 计划量从大到小）</li>
 *   <li>通过策略链过滤候选机台（定点/口型板/寸口/维修）</li>
 *   <li>机台分配与切换：当前机台定额满则切换其他可用机台，仍无可用则延后到下一班</li>
 *   <li>机台定额约束：单机台单班产量不超过该机台定额</li>
 *   <li>设置6个班次的生产顺序</li>
 *   <li>构建任务链</li>
 * </ol>
 */
@Slf4j
@Component
public class TqMachineAssignHandler extends AbsTqScheduleStepHandler {

    /**
     * 排程6班次与 ClassNumThreePlanEnums.classIndex（即 CLASS_NUM_THREE 字典值）的映射。
     * <p>排程以三班制为基础（班制固定为 中班→夜班→早班 循环）：</p>
     * <ul>
     *   <li>1班 = D日中班   → "03"（中班）</li>
     *   <li>2班 = D+1日夜班 → "01"（夜班）</li>
     *   <li>3班 = D+1日早班 → "02"（早班）</li>
     *   <li>4班 = D+1日中班 → "03"（中班）</li>
     *   <li>5班 = D+2日夜班 → "01"（夜班）</li>
     *   <li>6班 = D+2日早班 → "02"（早班）</li>
     * </ul>
     * 注意：此处使用与 CLASS_NUM_THREE 字典值一致的两位字符串（"01"/"02"/"03"），
     * 与机台 T_TQ_MACHINE_INFO.OPEN_MACHINE_CLASS 字段存储格式一致，
     * 不再使用旧的 OpenMachineClassEnums 枚举值（1/2/3/4/5）。
     */
    private static final String[] SHIFT_CLASS_MAP = {"03", "01", "02", "03", "01", "02"};

    @Resource
    private List<IMachineFilterStrategy> filterStrategies;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /**
     * 任务链排程服务（Phase 5 重构新增）。
     *
     * <p>用于将 {@link #buildTaskChain} 构建的旧 {@code taskChainMap} 同步到结构化
     * {@link MachineShiftTaskChain}，支持人工插单、转机台、调量等结构化操作。</p>
     */
    @Resource
    private TqTaskChainScheduleService taskChainScheduleService;

    private static final String DIVISION = "\r\n---------------------------------------------------\r\n";

    @Override
    protected String getStepName() {
        return "S3-班次排产分配";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        // 1. 按策略优先级排序
        List<IMachineFilterStrategy> sortedStrategies = filterStrategies.stream()
                .sorted(Comparator.comparingInt(IMachineFilterStrategy::getOrder))
                .collect(Collectors.toList());

        // 2. 机台分配
        chooseMachine(context, sortedStrategies);

        // 诊断日志：检查S3结束后是否存在"有计划量但未分配机台"的记录
        logUnassignedSchedule(context);

        // 3. 构建任务链（生产顺序设置移到S3.5之后执行，确保所有计划量修改完成后再设置顺序值）
        buildTaskChain(context);

        log.info("[S3] 班次排产分配完成");
    }

    /**
     * 生产线挑选（6班次版本，3步排产策略）。
     *
     * <p>分配逻辑：</p>
     * <ol>
     *   <li>先按优先级排序排程记录</li>
     *   <li>遍历6个班次，逐班为排程记录分配机台</li>
     *   <li>3步排产策略：①当前班当前机台 → ②当前班切换机台 → ③延至下一班次</li>
     * </ol>
     *
     * <p>定额约束：单机台单班产量不超过该机台定额(quota)，超出部分延后而非前移</p>
     */
    private void chooseMachine(TqScheduleContext context, List<IMachineFilterStrategy> sortedStrategies) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        List<TqMachineInfo> allMachineList = context.getAllMachineList();

        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }

        // ========== S3.1 机台分配阶段：为每个规格分配机台，构建真实 machineSpecCountMap ==========
        // 核心思路：先完成机台分配，再判断单/多规格，打破"判断依赖分配结果、分配又依赖判断"的循环依赖。
        // 两层判断逻辑：
        //   第1层（权威）：specifyMachineSpecCountMap — 基于限制作业配置反向统计，有限制作业的机台100%精准
        //   第2层（兜底）：machineSpecCountMap — 基于S3.1实际机台分配结果统计，无限制作业的机台用此判断
        double defaultQuota = context.getParams().getMaxClassOutput() == null ? 3000D
                : context.getParams().getMaxClassOutput();

        // --- 构建第1层：specifyMachineSpecCountMap（限制作业反向映射） ---
        // 从 specifyCanMachineMap(beadCode→canMachineCodes) 反向构建 machineCode→specCount
        // 仅统计 scheduleList 中存在的规格，避免无关规格干扰
        Map<String, String> specifyCanMachineMap = context.getSpecifyCanMachineMap();
        Map<String, Integer> specifyMachineSpecCountMap = new HashMap<>();
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            String canMachineCodes = specifyCanMachineMap.get(scheduleVo.getBeadCode());
            if (canMachineCodes != null && !canMachineCodes.isEmpty()) {
                for (String machineCode : canMachineCodes.split(",")) {
                    specifyMachineSpecCountMap.merge(machineCode.trim(), 1, Integer::sum);
                }
            }
        }

        // --- 构建第2层：S3.1 机台分配阶段 ---
        // 模拟第一班次的机台选择，维护 simulateCapacityMap 和 plannedMachineMap，
        // 使后续规格能感知前置规格的机台占用，避免"空状态下所有规格扎堆选同一机台"的误判
        Map<String, BigDecimal> simulateCapacityMap = new HashMap<>();
        Map<String, String> plannedMachineMap = new HashMap<>();
        String firstClassCode = SHIFT_CLASS_MAP[0];

        // 按优先级排序：定点机台优先 → 计划量从大到小
        List<TqScheduleResultVo> sortedScheduleList = scheduleList.stream().sorted((o1, o2) -> {
            Integer flag1 = context.getSpecifyCanMachineMap().containsKey(o1.getBeadCode()) ? 1 : 2;
            Integer flag2 = context.getSpecifyCanMachineMap().containsKey(o2.getBeadCode()) ? 1 : 2;
            int result = flag1.compareTo(flag2);
            if (result != 0) {
                return result;
            }
            double totalPlan1 = getTotalPlanQty(o1);
            double totalPlan2 = getTotalPlanQty(o2);
            return Double.compare(totalPlan2, totalPlan1);
        }).collect(Collectors.toList());

        for (TqScheduleResultVo scheduleVo : sortedScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineCode())) {
                // 已有定点机台（前置处理已分配），直接记录
                plannedMachineMap.put(scheduleVo.getBeadCode(), scheduleVo.getMachineCode());
                continue;
            }
            // 模拟第一班次的机台选择
            List<TqMachineInfo> candidates = searchOptionalMachineList(
                    scheduleVo, firstClassCode, simulateCapacityMap, allMachineList,
                    context, sortedStrategies, plannedMachineMap);
            if (!CollectionUtil.isEmpty(candidates)) {
                TqMachineInfo machine = candidates.get(0);
                String machineCode = machine.getMachineCode();
                scheduleVo.setMachineCode(machineCode);
                scheduleVo.setUnscheduledFlag("0");
                plannedMachineMap.put(scheduleVo.getBeadCode(), machineCode);
                // 模拟产能占用：用一个班次的定额占位，使后续规格按"剩余产能"排序时能看到占用
                double quota = getMachineQuota(machine, defaultQuota);
                simulateCapacityMap.merge(machineCode, BigDecimal.valueOf(quota), BigDecimal::add);
            }
        }

        // 基于 S3.1 真实分配结果构建 machineSpecCountMap（第2层兜底判断）
        Map<String, Integer> machineSpecCountMap = new HashMap<>();
        for (Map.Entry<String, String> entry : plannedMachineMap.entrySet()) {
            machineSpecCountMap.merge(entry.getValue(), 1, Integer::sum);
        }

        log.info("[S3.1] 机台分配完成，specifyMachineSpecCountMap={}，machineSpecCountMap={}",
                specifyMachineSpecCountMap, machineSpecCountMap);

        // ========== S3.2 计划量分配阶段：6班次循环分配 ==========
        // 机台产能占用追踪（6个班次，清空模拟数据，使用真实产能追踪）
        Map<String, BigDecimal> class1CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class2CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class3CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class4CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class5CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class6CapacityMap = new HashMap<>();
        Map<String, List<String>> glueMap = new HashMap<>();
        Map<String, List<String>> mouthPlatMap = new HashMap<>();

        // S3.1阶段已分配的机台，补充胶料/口模映射（S3.2中会用到）
        for (TqScheduleResultVo scheduleVo : sortedScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineCode())) {
                putMachineCode(scheduleVo.getGlueCode(), scheduleVo.getMachineCode(), glueMap);
                putMachineCode(scheduleVo.getMouthPlateCode(), scheduleVo.getMachineCode(), mouthPlatMap);
            }
        }

        // 逐班分配机台（1班→2班→3班→4班→5班→6班）
        // 班次对应的 CLASS_NUM_THREE 字典值（三班制 中班/夜班/早班 循环），见 SHIFT_CLASS_MAP 注释
        // 1班=D日中班→"03", 2班=D+1日夜班→"01", 3班=D+1日早班→"02",
        // 4班=D+1日中班→"03", 5班=D+2日夜班→"01", 6班=D+2日早班→"02"

        Map<String, BigDecimal>[] capacityMaps = new Map[]{
                class1CapacityMap, class2CapacityMap, class3CapacityMap,
                class4CapacityMap, class5CapacityMap, class6CapacityMap
        };

        for (int classIdx = 0; classIdx < 6; classIdx++) {
            String classCode = SHIFT_CLASS_MAP[classIdx];

            // 阈值切换生产：供应时长未达阈值的规格优先排产，已达阈值的规格排后面
            double supplyTimeThreshold = context.getParams().getSupplyTimeThreshold() == null ? 24D
                    : context.getParams().getSupplyTimeThreshold();

            // ========== 三级优先级排序（新规则） ==========
            // Priority-1: 当前班次新触发备库的规格（最高优先级）
            // Priority-2: 前序班次已触发备库的规格，同组内按剩余需求缺口从大到小排序（缺口越大越优先）
            // Priority-3: 非备库规格，按供应时长升序
            final Integer currentClassNum = classIdx + 1;
            List<TqScheduleResultVo> classSortedScheduleList = sortedScheduleList.stream()
                    .sorted((o1, o2) -> {
                        // P-0: 当前班次新触发备库规格最高优先级
                        // 典型场景：TQM03机台2班，211100022在2班触发备库，但1班未排产未分配机台，
                        // 如果排在已分配机台规格之后，等排到它时TQM03已满排0延后到3班
                        boolean currentTrigger1 = o1.getBackupTriggerClass() != null
                                && o1.getBackupTriggerClass() > 0
                                && o1.getBackupTriggerClass().equals(currentClassNum);
                        boolean currentTrigger2 = o2.getBackupTriggerClass() != null
                                && o2.getBackupTriggerClass() > 0
                                && o2.getBackupTriggerClass().equals(currentClassNum);
                        if (currentTrigger1 != currentTrigger2) {
                            return currentTrigger1 ? -1 : 1;
                        }

                        // P-1: 已分配机台的规格优先处理（避免机台分配不稳定）
                        boolean planned1 = plannedMachineMap.containsKey(o1.getBeadCode());
                        boolean planned2 = plannedMachineMap.containsKey(o2.getBeadCode());
                        if (planned1 != planned2) {
                            return planned1 ? -1 : 1;
                        }

                        boolean backup1 = o1.getBackupTriggerClass() != null && o1.getBackupTriggerClass() > 0;
                        boolean backup2 = o2.getBackupTriggerClass() != null && o2.getBackupTriggerClass() > 0;

                        // P-1: 备库规格优先于非备库规格（不区分当前触发/前序触发）
                        // 原逻辑将"当前班次新触发"优先于"前序班次已触发"，但多规格机台场景下
                        // 应统一按备库缺口大小排序，缺口越大越优先，避免小缺口新触发规格抢占大缺口产能
                        if (backup1 != backup2) {
                            return backup1 ? -1 : 1;
                        }

                        // P-2: 备库规格与非备库规格均按供应时长升序排序（供应时长短=紧急=优先排）
                        double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
                        double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
                        return Double.compare(st1, st2);
                    })
                    .collect(Collectors.toList());

            for (TqScheduleResultVo scheduleVo : classSortedScheduleList) {
                double planQty = getClassPlanQty(scheduleVo, classIdx + 1);
                if (planQty <= 0) {
                    continue;
                }

                // 获取机台定额：优先使用机台自身的quota，否则使用全局maxClassOutput
                defaultQuota = context.getParams().getMaxClassOutput() == null ? 3000D
                        : context.getParams().getMaxClassOutput();

                // ========== 单机台满排+延后策略（业务需求） ==========
                // 业务规则：一个规格正常占用一个机台；当计划量超过机台定额（阈值）时，
                // 当班满排机台定额的量，剩余量延后到下一班；下一班继续满排，仍排不完则继续延后。

                // 步骤1：当前班次，当前已分配机台
                if (StringUtils.isNotEmpty(scheduleVo.getMachineCode())) {
                    String machineCode = scheduleVo.getMachineCode();
                    TqMachineInfo existingMachine = allMachineList.stream()
                            .filter(m -> m.getMachineCode().equals(machineCode))
                            .findFirst().orElse(null);

                    // 机台不支持当前班次，延后到下一班
                    if (existingMachine != null && !existingMachine.getOpenMachineClass().contains(classCode)) {
                        setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                        deferToNextClass(scheduleVo, classIdx + 1, planQty);
                        continue;
                    }

                    // 获取机台定额（作为当班满排阈值）
                    double machineQuota = getMachineQuota(existingMachine, defaultQuota);

                    // 定额检查：已排产能 + 当前计划量不能超过定额
                    BigDecimal currentCapacity = capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO);
                    double remainingCapacity = BigDecimalUtil.sub(machineQuota, currentCapacity.doubleValue());

                    // 备库胎圈多规格阈值限制：
                    // 单一规格机台 → 只受 quota 限制（可满排）
                    // 多规格机台   → 备库胎圈初始排产不超过 SYS1101029 阈值，剩余产能由 S3.5 回填
                    double backupThreshold = context.getParams().getBackupShiftThreshold() == null ? 1000D
                            : context.getParams().getBackupShiftThreshold();
                    double initAssignLimit = getBackupInitAssignLimit(scheduleVo, machineCode,
                            machineSpecCountMap, specifyMachineSpecCountMap, machineQuota, backupThreshold, classIdx + 1);
                    // 当班实际可排上限 = min(机台剩余产能, 备库初始排产上限)
                    // 超排容忍（SYS1101031）：计划量超出机台剩余产能，但超出部分≤容忍阈值时，允许当班超排，不延后到下一班
                    double effectiveCapacity = calcEffectiveCapacityWithTolerance(
                            planQty, remainingCapacity, initAssignLimit,
                            context.getParams().getMachineOverAssignTolerance(),
                            scheduleVo, machineCode, classIdx + 1, context);

                    // 判断是否为备库胎圈多规格机台场景
                    boolean isBackupSpec = scheduleVo.getBackupTriggerClass() != null
                            && scheduleVo.getBackupTriggerClass() > 0;
                    boolean isMultiSpecMachine = !isSingleSpecMachine(machineCode, machineSpecCountMap, specifyMachineSpecCountMap);

                    if (isBackupSpec && isMultiSpecMachine) {
                        // 备库胎圈多规格机台：满排到阈值(有效产能)，超出部分延后到下一班继续满排
                        double overAssignTolerance = context.getParams().getMachineOverAssignTolerance();
                        if (effectiveCapacity <= 0) {
                            // 机台本班已排满，全部计划量延后到下一班
                            setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                            deferToNextClass(scheduleVo, classIdx + 1, planQty);
                        } else if (planQty > effectiveCapacity) {
                            double overflowQty = BigDecimalUtil.sub(planQty, effectiveCapacity);
                            if (TqDemandCalcHelper.canOverAssignInCurrentClass(overflowQty, overAssignTolerance)) {
                                // 超排容忍（SYS1101031）：尾数≤容忍阈值，当班超排完，不延后
                                setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                                capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                        .add(BigDecimalUtils.valueOf(planQty)));
                                autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                        "备库胎圈满排-超排容忍", "胎圈代码：" + scheduleVo.getBeadCode()
                                                + "，" + (classIdx + 1) + "班计划量" + planQty
                                                + "，有效产能" + effectiveCapacity + "，超出" + overflowQty
                                                + " ≤ 超排容忍阈值(" + overAssignTolerance + ")，当班超排" + planQty);
                                // 扣减备库剩余量
                                double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                                scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, planQty));
                            } else {
                                // 尾数超出容忍范围，当班排满阈值产能，超出部分延后到下一班
                                setClassPlanQty(scheduleVo, classIdx + 1, effectiveCapacity);
                                capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                        .add(BigDecimalUtils.valueOf(effectiveCapacity)));
                                deferToNextClass(scheduleVo, classIdx + 1, overflowQty);
                                autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                        "备库胎圈满排阈值-剩余延后", "胎圈代码：" + scheduleVo.getBeadCode()
                                                + "，" + (classIdx + 1) + "班排满" + effectiveCapacity
                                                + "，剩余" + overflowQty + "延后至下一班");
                                recordQuotaExceed(context, scheduleVo, machineCode, classIdx + 1,
                                        effectiveCapacity, overflowQty, true);
                                // 扣减备库剩余量
                                double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                                scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, effectiveCapacity));
                            }
                        } else {
                            // 全部可排
                            // 备库胎圈多规格机台：剩余量 < 整车容量(SYS1101004)时向上取整到整车容量，避免零散排产
                            double actualPlanQty = applyToolCapacityRounding(planQty, effectiveCapacity,
                                    isBackupSpec, isMultiSpecMachine, scheduleVo, classIdx + 1, context);
                            setClassPlanQty(scheduleVo, classIdx + 1, actualPlanQty);
                            capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .add(BigDecimalUtils.valueOf(actualPlanQty)));
                            // 扣减备库剩余量（取整后可能超排，backupRemainingQty 置0）
                            double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                            double newRemaining = BigDecimalUtil.sub(remaining, actualPlanQty);
                            scheduleVo.setBackupRemainingQty(newRemaining < 0 ? 0D : newRemaining);
                        }
                    } else if (effectiveCapacity <= 0) {
                        // 非备库或单规格机台：当前机台本班已排满，全部计划量延后到下一班
                        setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                        deferToNextClass(scheduleVo, classIdx + 1, planQty);
                        recordQuotaExceed(context, scheduleVo, machineCode, classIdx + 1,
                                0D, planQty, false);
                    } else if (planQty > effectiveCapacity) {
                        double overflowQty = BigDecimalUtil.sub(planQty, effectiveCapacity);
                        double overAssignTolerance = context.getParams().getMachineOverAssignTolerance();
                        if (TqDemandCalcHelper.canOverAssignInCurrentClass(overflowQty, overAssignTolerance)) {
                            // 超排容忍（SYS1101031）：尾数≤容忍阈值，当班超排完，不延后
                            setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                            capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .add(BigDecimalUtils.valueOf(planQty)));
                            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                    "当班满排-超排容忍", "胎圈代码：" + scheduleVo.getBeadCode()
                                            + "，" + (classIdx + 1) + "班计划量" + planQty
                                            + "，有效产能" + effectiveCapacity + "，超出" + overflowQty
                                            + " ≤ 超排容忍阈值(" + overAssignTolerance + ")，当班超排" + planQty);
                            // 备库胎圈扣减备库剩余量
                            if (isBackupSpec) {
                                double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                                scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, planQty));
                            }
                        } else {
                            // 尾数超出容忍范围：当班先排满有效产能，超出部分延后到下一班
                            setClassPlanQty(scheduleVo, classIdx + 1, effectiveCapacity);
                            capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .add(BigDecimalUtils.valueOf(effectiveCapacity)));
                            deferToNextClass(scheduleVo, classIdx + 1, overflowQty);
                            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                    "当班满排-剩余延后", "胎圈代码：" + scheduleVo.getBeadCode()
                                            + "，" + (classIdx + 1) + "班排满" + effectiveCapacity
                                            + "，剩余" + overflowQty + "延后至下一班");
                            recordQuotaExceed(context, scheduleVo, machineCode, classIdx + 1,
                                    effectiveCapacity, overflowQty, false);
                            // 备库胎圈扣减备库剩余量（仅扣减当班排产部分，延后部分在下一班扣减）
                            if (isBackupSpec) {
                                double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                                scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, effectiveCapacity));
                            }
                        }
                    } else {
                        // 全部可排
                        setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(planQty)));
                        // 备库胎圈扣减备库剩余量
                        if (isBackupSpec) {
                            double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                            scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, planQty));
                        }
                    }
                    chooseMachineLog(scheduleVo, context);
                    continue;
                }

                // 未分配机台：搜索一个可用机台分配
                List<TqMachineInfo> optionalMachineList = searchOptionalMachineList(
                        scheduleVo, classCode, capacityMaps[classIdx], allMachineList, context, sortedStrategies, plannedMachineMap);

                if (CollectionUtil.isEmpty(optionalMachineList)) {
                    // 无可用机台，延后至下一班次
                    setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                    deferToNextClass(scheduleVo, classIdx + 1, planQty);
                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "无可用机台-延后至下一班", "胎圈代码：" + scheduleVo.getBeadCode()
                                    + "，" + (classIdx + 1) + "班计划量" + planQty + "延后");
                    continue;
                }

                // 选择第一台可用机台（一个规格一个机台）
                TqMachineInfo machine = optionalMachineList.get(0);
                String machineCode = machine.getMachineCode();
                double machineQuota = getMachineQuota(machine, defaultQuota);
                BigDecimal currentCapacity = capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO);
                double remainingCapacity = BigDecimalUtil.sub(machineQuota, currentCapacity.doubleValue());

                // 首次分配机台，设置排程记录的机台编号
                scheduleVo.setMachineCode(machineCode);
                scheduleVo.setUnscheduledFlag("0");
                plannedMachineMap.put(scheduleVo.getBeadCode(), machineCode);
                putMachineCode(scheduleVo.getGlueCode(), machineCode, glueMap);
                putMachineCode(scheduleVo.getMouthPlateCode(), machineCode, mouthPlatMap);

                // 不预占其他班次产能：预占会导致S2计划量与S3实际排产量双重计算，
                // 使机台在后续班次"假满"（capacityMap包含S2预占值+实际排产值），无法正常排产。
                // 每个班次独立按实际已排产量计算剩余产能即可。

                // 备库胎圈多规格阈值限制（同已分配机台分支逻辑）
                double backupThreshold = context.getParams().getBackupShiftThreshold() == null ? 1000D
                        : context.getParams().getBackupShiftThreshold();
                double initAssignLimit = getBackupInitAssignLimit(scheduleVo, machineCode,
                        machineSpecCountMap, specifyMachineSpecCountMap, machineQuota, backupThreshold, classIdx + 1);
                // 超排容忍（SYS1101031）：计划量超出机台剩余产能，但超出部分≤容忍阈值时，允许当班超排，不延后到下一班
                double effectiveCapacity = calcEffectiveCapacityWithTolerance(
                        planQty, remainingCapacity, initAssignLimit,
                        context.getParams().getMachineOverAssignTolerance(),
                        scheduleVo, machineCode, classIdx + 1, context);

                // 判断是否为备库胎圈多规格机台场景（同已分配机台分支）
                boolean isBackupSpec = scheduleVo.getBackupTriggerClass() != null
                        && scheduleVo.getBackupTriggerClass() > 0;
                boolean isMultiSpecMachine = !isSingleSpecMachine(machineCode, machineSpecCountMap, specifyMachineSpecCountMap);

                if (isBackupSpec && isMultiSpecMachine) {
                    // 备库胎圈多规格机台：满排到阈值(有效产能)，超出部分延后到下一班继续满排
                    double overAssignTolerance = context.getParams().getMachineOverAssignTolerance();
                    if (effectiveCapacity <= 0) {
                        // 机台本班已排满，全部计划量延后到下一班
                        setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                        deferToNextClass(scheduleVo, classIdx + 1, planQty);
                        recordQuotaExceed(context, scheduleVo, machineCode, classIdx + 1,
                                0D, planQty, true);
                    } else if (planQty > effectiveCapacity) {
                        double overflowQty = BigDecimalUtil.sub(planQty, effectiveCapacity);
                        if (TqDemandCalcHelper.canOverAssignInCurrentClass(overflowQty, overAssignTolerance)) {
                            // 超排容忍（SYS1101031）：尾数≤容忍阈值，当班超排完，不延后
                            setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                            capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .add(BigDecimalUtils.valueOf(planQty)));
                            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                    "备库胎圈满排-超排容忍", "胎圈代码：" + scheduleVo.getBeadCode()
                                            + "，" + (classIdx + 1) + "班计划量" + planQty
                                            + "，有效产能" + effectiveCapacity + "，超出" + overflowQty
                                            + " ≤ 超排容忍阈值(" + overAssignTolerance + ")，当班超排" + planQty);
                            // 扣减备库剩余量
                            double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                            scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, planQty));
                        } else {
                            // 尾数超出容忍范围，当班排满阈值产能，超出部分延后到下一班
                            setClassPlanQty(scheduleVo, classIdx + 1, effectiveCapacity);
                            capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .add(BigDecimalUtils.valueOf(effectiveCapacity)));
                            deferToNextClass(scheduleVo, classIdx + 1, overflowQty);
                            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                    "备库胎圈满排阈值-剩余延后", "胎圈代码：" + scheduleVo.getBeadCode()
                                            + "，" + (classIdx + 1) + "班排满" + effectiveCapacity
                                            + "，剩余" + overflowQty + "延后至下一班");
                            recordQuotaExceed(context, scheduleVo, machineCode, classIdx + 1,
                                    effectiveCapacity, overflowQty, true);
                            // 扣减备库剩余量
                            double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                            scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, effectiveCapacity));
                        }
                    } else {
                        // 全部可排
                        // 备库胎圈多规格机台：剩余量 < 整车容量(SYS1101004)时向上取整到整车容量，避免零散排产
                        double actualPlanQty = applyToolCapacityRounding(planQty, effectiveCapacity,
                                isBackupSpec, isMultiSpecMachine, scheduleVo, classIdx + 1, context);
                        setClassPlanQty(scheduleVo, classIdx + 1, actualPlanQty);
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(actualPlanQty)));
                        // 扣减备库剩余量（取整后可能超排，backupRemainingQty 置0）
                        double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                        double newRemaining = BigDecimalUtil.sub(remaining, actualPlanQty);
                        scheduleVo.setBackupRemainingQty(newRemaining < 0 ? 0D : newRemaining);
                    }
                } else if (effectiveCapacity <= 0) {
                    // 非备库或单规格机台：机台本班已排满，全部计划量延后到下一班
                    setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                    deferToNextClass(scheduleVo, classIdx + 1, planQty);
                    recordQuotaExceed(context, scheduleVo, machineCode, classIdx + 1,
                            0D, planQty, false);
                } else if (planQty > effectiveCapacity) {
                    double overflowQty = BigDecimalUtil.sub(planQty, effectiveCapacity);
                    double overAssignTolerance = context.getParams().getMachineOverAssignTolerance();
                    if (TqDemandCalcHelper.canOverAssignInCurrentClass(overflowQty, overAssignTolerance)) {
                        // 超排容忍（SYS1101031）：尾数≤容忍阈值，当班超排完，不延后
                        setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(planQty)));
                        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                "当班满排-超排容忍", "胎圈代码：" + scheduleVo.getBeadCode()
                                        + "，" + (classIdx + 1) + "班计划量" + planQty
                                        + "，有效产能" + effectiveCapacity + "，超出" + overflowQty
                                        + " ≤ 超排容忍阈值(" + overAssignTolerance + ")，当班超排" + planQty);
                        // 备库胎圈扣减备库剩余量
                        if (isBackupSpec) {
                            double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                            scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, planQty));
                        }
                    } else {
                        // 尾数超出容忍范围：当班先排满有效产能，超出部分延后到下一班
                        setClassPlanQty(scheduleVo, classIdx + 1, effectiveCapacity);
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(effectiveCapacity)));
                        deferToNextClass(scheduleVo, classIdx + 1, overflowQty);
                        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                "当班满排-剩余延后", "胎圈代码：" + scheduleVo.getBeadCode()
                                        + "，" + (classIdx + 1) + "班排满" + effectiveCapacity
                                        + "，剩余" + overflowQty + "延后至下一班");
                        recordQuotaExceed(context, scheduleVo, machineCode, classIdx + 1,
                                effectiveCapacity, overflowQty, false);
                        // 备库胎圈扣减备库剩余量（仅扣减当班排产部分，延后部分在下一班扣减）
                        if (isBackupSpec) {
                            double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                            scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, effectiveCapacity));
                        }
                    }
                } else {
                    // 全部可排
                    setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                    capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                            .add(BigDecimalUtils.valueOf(planQty)));
                    // 备库胎圈扣减备库剩余量
                    if (isBackupSpec) {
                        double remaining = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                        scheduleVo.setBackupRemainingQty(BigDecimalUtil.sub(remaining, planQty));
                    }
                }

                chooseMachineLog(scheduleVo, context);
            }

            // S3.2 剩余产能二次分配（集中排产）：当班逐规格阈值排产后，
            // 回收机台剩余产能，按缺口从大到小回填给备库胎圈，避免缺口最大的规格被分散到多班排产
            redistributeResidualCapacity(classIdx, capacityMaps, sortedScheduleList,
                    machineSpecCountMap, specifyMachineSpecCountMap, allMachineList, plannedMachineMap, context);
        }
    }

    /**
     * 备库胎圈多规格机台：剩余量 < 整车容量(SYS1101004)时向上取整到整车容量。
     *
     * <p>当备库胎圈的剩余排产量小于整车容量时，向上取整到整车容量（不超过有效产能上限），
     * 避免零散排产导致后续班次产生整车不齐的尾数。多排部分视为整车取整超排，
     * backupRemainingQty 在调用方置0处理。</p>
     *
     * @param planQty            原始计划量（剩余排产量）
     * @param effectiveCapacity  有效产能上限（机台剩余产能与备库阈值的较小值）
     * @param isBackupSpec       是否备库胎圈
     * @param isMultiSpecMachine 是否多规格机台
     * @param scheduleVo         排程记录（用于日志）
     * @param classNum           班次序号（1-6）
     * @param context            排程上下文
     * @return 取整后的计划量；非备库/单规格/已超整车容量时返回原值
     */
    private double applyToolCapacityRounding(double planQty, double effectiveCapacity,
                                             boolean isBackupSpec, boolean isMultiSpecMachine,
                                             TqScheduleResultVo scheduleVo, int classNum,
                                             TqScheduleContext context) {
        if (!isBackupSpec || !isMultiSpecMachine) {
            return planQty;
        }
        Double toolCapacity = context.getParams().getToolCapacity();
        if (toolCapacity == null || toolCapacity <= 0 || planQty >= toolCapacity) {
            return planQty;
        }
        // 剩余量 < 整车容量，向上取整到整车容量（不超过有效产能上限）
        double roundedQty = Math.min(toolCapacity, effectiveCapacity);
        if (roundedQty > planQty) {
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "备库胎圈整车取整", "胎圈代码：" + scheduleVo.getBeadCode()
                            + "，" + classNum + "班剩余量" + planQty + " < 整车容量" + toolCapacity
                            + "，向上取整到" + roundedQty);
        }
        return roundedQty;
    }

    /**
     * S3.2 剩余产能二次分配（供应时长优先排产）。
     *
     * <p>每个班次逐规格阈值排产后，回收机台剩余产能，回填给备库胎圈。
     * 排序规则：供应时长升序（短=紧急=优先排），与 S3.1 初始排产、S5.6 兜底回填口径一致。</p>
     *
     * <p>处理流程：
     * <ol>
     *   <li>按机台分组当班有排产且仍有备库剩余量的胎圈规格</li>
     *   <li>仅处理多规格机台（单规格机台S3已按定额满排）</li>
     *   <li>按供应时长(supplyTime)升序排序（短=紧急=优先回填）</li>
     *   <li>逐规格回填剩余产能：回填量 = min(机台剩余产能, 备库剩余量)</li>
     *   <li>更新当班计划量、机台产能占用、备库剩余量</li>
     * </ol></p>
     *
     * @param classIdx                    班次索引（0-5）
     * @param capacityMaps                6个班次的机台产能占用Map
     * @param scheduleList                排程列表
     * @param machineSpecCountMap         机台规格数映射
     * @param specifyMachineSpecCountMap  限制作业机台规格数映射
     * @param allMachineList              所有机台列表
     * @param plannedMachineMap           已分配机台映射（胎圈代码→机台编码）
     * @param context                     排程上下文
     */
    private void redistributeResidualCapacity(int classIdx, Map<String, BigDecimal>[] capacityMaps,
                                              List<TqScheduleResultVo> scheduleList,
                                              Map<String, Integer> machineSpecCountMap,
                                              Map<String, Integer> specifyMachineSpecCountMap,
                                              List<TqMachineInfo> allMachineList,
                                              Map<String, String> plannedMachineMap,
                                              TqScheduleContext context) {
        int classNum = classIdx + 1;
        double defaultQuota = context.getParams().getMaxClassOutput() == null ? 3000D
                : context.getParams().getMaxClassOutput();

        // 构建机台信息Map（机台编码 -> 机台信息）
        Map<String, TqMachineInfo> machineInfoMap = allMachineList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineCode, m -> m, (a, b) -> a));

        // ========== 第一步：非备库/未触发规格产能让渡 ==========
        // 多规格机台上，如果某规格在当前班次"不应排产"（非备库规格，或备库触发班次 > 当前班次），
        // 但S2给了计划量导致S3排产占用了机台产能，则将其当班计划量让渡出来，
        // 延后到备库触发班次，释放的产能用于回填给备库缺口更大的规格。
        // 典型场景：023规格3班才触发备库，但1/2班被S2非备库常规计算给了112计划量，
        // 导致1/2班机台产能被023占用，001（备库缺口最大）拿不到足够的剩余产能。

        // 先收集需要产能让渡的机台（有多规格在当班排产的机台）
        Map<String, List<TqScheduleResultVo>> machineSpecMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .filter(s -> getClassPlanQty(s, classNum) > 0)
                .collect(Collectors.groupingBy(TqScheduleResultVo::getMachineCode));

        for (Map.Entry<String, List<TqScheduleResultVo>> entry : machineSpecMap.entrySet()) {
            String machineCode = entry.getKey();
            List<TqScheduleResultVo> specs = entry.getValue();

            // 仅处理多规格机台
            if (isSingleSpecMachine(machineCode, machineSpecCountMap, specifyMachineSpecCountMap)) {
                continue;
            }

            // 检查该机台上是否有备库规格仍有剩余量（存在需要回填的规格）
            boolean hasBackupWithRemaining = specs.stream()
                    .anyMatch(s -> s.getBackupTriggerClass() != null && s.getBackupTriggerClass() > 0
                            && s.getBackupRemainingQty() != null && s.getBackupRemainingQty() > 0);
            if (!hasBackupWithRemaining) {
                continue;
            }

            // 遍历该机台上的规格，让渡不应排产的规格
            for (TqScheduleResultVo spec : specs) {
                double currentPlan = getClassPlanQty(spec, classNum);
                if (currentPlan <= 0) {
                    continue;
                }

                // 判断规格在当前班次是否"不应排产"
                boolean shouldDefer = false;
                String deferReason = "";
                Integer backupTriggerClass = spec.getBackupTriggerClass();
                if (backupTriggerClass == null || backupTriggerClass <= 0) {
                    // 非备库规格：当前班次不是该规格的"需求班次"，不应占用多规格机台产能
                    shouldDefer = true;
                    deferReason = "非备库规格在多规格机台上让渡产能";
                } else if (backupTriggerClass > classNum) {
                    // 备库触发班次 > 当前班次：当前班次库存充足不需要排产，不应占用机台产能
                    shouldDefer = true;
                    deferReason = "备库触发班次" + backupTriggerClass + " > 当前班次" + classNum + "，让渡产能";
                }

                if (!shouldDefer) {
                    continue;
                }

                // 让渡：扣减当班计划量，释放机台产能
                setClassPlanQty(spec, classNum, 0D);
                capacityMaps[classIdx].put(machineCode,
                        capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .subtract(BigDecimalUtils.valueOf(currentPlan)));

                // 将让渡量累加到备库触发班次（有备库触发）或下一班次（无备库触发）
                int targetClassNum;
                if (backupTriggerClass != null && backupTriggerClass > 0) {
                    targetClassNum = backupTriggerClass;
                } else {
                    targetClassNum = Math.min(classNum + 1, 6);
                }
                double targetPlan = getClassPlanQty(spec, targetClassNum);
                setClassPlanQty(spec, targetClassNum, BigDecimalUtil.add(targetPlan, currentPlan));

                autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S3.2-产能让渡", "胎圈代码：" + spec.getBeadCode()
                                + "，" + classNum + "班让渡" + currentPlan + "→" + targetClassNum + "班"
                                + "，原因：" + deferReason
                                + "，释放机台" + machineCode + "产能" + currentPlan);
            }
        }

        // ========== 第1.5步：备库触发班次产能保障 ==========
        // 多规格机台上，如果某规格在当前班次是备库触发班次（backupTriggerClass == classNum），
        // 但因S3.1排产时机台满排导致planQty=0（延后到后续班次），需要从其他规格让渡产能，
        // 确保备库触发班次有足够的排产量。
        // 典型场景：TQM03机台2班，211100022在2班触发备库，但3个1班触发的规格已占满1500定额，
        // 导致211100022排0延后到3班。此时应从其他1班触发规格（如211100024）让渡500产能给211100022。
        guaranteeBackupTriggerClassCapacity(scheduleList, classNum, classIdx, capacityMaps,
                machineSpecCountMap, specifyMachineSpecCountMap, allMachineList, machineInfoMap,
                defaultQuota, plannedMachineMap, context);

        // ========== 第二步：备库胎圈剩余产能回填 ==========
        // 按机台分组当班有排产且仍有备库剩余量的胎圈规格
        Map<String, List<TqScheduleResultVo>> machineBackupSpecMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .filter(s -> s.getBackupTriggerClass() != null && s.getBackupTriggerClass() > 0)
                .filter(s -> s.getBackupRemainingQty() != null && s.getBackupRemainingQty() > 0)
                .collect(Collectors.groupingBy(TqScheduleResultVo::getMachineCode));

        for (Map.Entry<String, List<TqScheduleResultVo>> entry : machineBackupSpecMap.entrySet()) {
            String machineCode = entry.getKey();
            List<TqScheduleResultVo> backupSpecs = entry.getValue();

            // 仅处理多规格机台（单规格机台S3已按定额满排，无剩余产能可回填）
            if (isSingleSpecMachine(machineCode, machineSpecCountMap, specifyMachineSpecCountMap)) {
                continue;
            }

            TqMachineInfo machine = machineInfoMap.get(machineCode);
            double machineQuota = getMachineQuota(machine, defaultQuota);

            // 按供应时长升序排序（短=紧急=优先回填），与S3.1/S5.6口径一致
            List<TqScheduleResultVo> sortedSpecs = backupSpecs.stream()
                    .sorted((o1, o2) -> {
                        double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
                        double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
                        return Double.compare(st1, st2);
                    })
                    .collect(Collectors.toList());

            // 诊断日志：输出S3.2排序详情（供应时长+备库剩余缺口），用于排查剩余产能分配方向
            log.info("[S3.2-DIAG] 机台:{} {}班 剩余产能二次分配排序结果(按supplyTime升序):",
                    machineCode, classIdx + 1);
            for (TqScheduleResultVo spec : sortedSpecs) {
                log.info("[S3.2-DIAG]   beadCode={} supplyTime={} backupRemainingQty={} triggerClass={}",
                        spec.getBeadCode(),
                        spec.getSupplyTime(),
                        spec.getBackupRemainingQty(),
                        spec.getBackupTriggerClass());
            }

            // 逐规格回填剩余产能
            for (TqScheduleResultVo spec : sortedSpecs) {
                // 实时计算机台剩余产能（每次回填后更新）
                BigDecimal usedCapacity = capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO);
                double remainingCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity.doubleValue());
                if (remainingCapacity <= 0) {
                    break;
                }

                double backupRemaining = spec.getBackupRemainingQty() == null ? 0D : spec.getBackupRemainingQty();
                if (backupRemaining <= 0) {
                    continue;
                }

                // 回填量 = min(机台剩余产能, 备库剩余量)
                double assignQty = Math.min(remainingCapacity, backupRemaining);

                // 累加到当班计划量
                double currentPlan = getClassPlanQty(spec, classNum);
                setClassPlanQty(spec, classNum, BigDecimalUtil.add(currentPlan, assignQty));
                capacityMaps[classIdx].put(machineCode,
                        capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(assignQty)));

                // 前移消化：从后续班次planQty中等额扣减回填量，防止总量超排
                // 回填本质是"把后续班次的延后量前移到当班"，需要同步减少后续班次的planQty
                forwardDigestRemainingQty(spec, classNum, assignQty);

                // 扣减备库剩余量
                double newRemaining = BigDecimalUtil.sub(backupRemaining, assignQty);
                spec.setBackupRemainingQty(newRemaining < 0 ? 0D : newRemaining);

                autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S3.2-剩余产能二次分配", "胎圈代码：" + spec.getBeadCode()
                                + "，机台：" + machineCode
                                + "，" + classNum + "班回填" + assignQty
                                + "，当班累计" + BigDecimalUtil.add(currentPlan, assignQty)
                                + "，备库剩余" + spec.getBackupRemainingQty());
            }
        }
    }

    /**
     * 备库触发班次产能保障：确保备库触发班次（backupTriggerClass == classNum）的规格
     * 在当前班次有足够的排产量。
     *
     * <p>背景：多规格机台上，某规格在当前班次是备库触发班次（backupTriggerClass == classNum），
     * 但S3.1排产时机台已被其他规格占满，导致该规格排0延后到后续班次。
     * 此时需要从同机台其他规格让渡产能，确保备库触发班次有排产量。</p>
     *
     * <p>让渡策略：</p>
     * <ol>
     *   <li>找出当前班次是备库触发班次但planQty=0的规格（"需要保障的规格"）</li>
     *   <li>按机台分组，在同机台其他规格中寻找可让渡的产能</li>
     *   <li>让渡优先级：非备库规格 > 备库触发班次 != 当前班次的规格 > 备库触发班次 == 当前班次的规格</li>
     *   <li>同优先级内按backupRemainingQty升序（缺口小的先让渡）</li>
     *   <li>让渡量上限 = 该规格当班planQty - 阈值（保障每个规格至少排到阈值量）</li>
     *   <li>让渡后，从被让渡规格的当班planQty扣减，加到需要保障规格的当班planQty</li>
     *   <li>被让渡规格的扣减量累加到其后续班次（与第一步让渡逻辑一致）</li>
     * </ol>
     *
     * @param scheduleList 排程列表
     * @param classNum 当前班次号（1~6）
     * @param classIdx 当前班次索引（classNum-1）
     * @param capacityMaps 产能追踪Map
     * @param machineSpecCountMap 机台规格数映射
     * @param specifyMachineSpecCountMap 指定机台规格数映射
     * @param allMachineList 所有机台列表
     * @param machineInfoMap 机台信息映射
     * @param defaultQuota 默认机台定额
     * @param plannedMachineMap 已分配机台映射（胎圈代码→机台编码）
     * @param context 排程上下文
     */
    private void guaranteeBackupTriggerClassCapacity(List<TqScheduleResultVo> scheduleList, int classNum,
                                                      int classIdx, Map<String, BigDecimal>[] capacityMaps,
                                                      Map<String, Integer> machineSpecCountMap,
                                                      Map<String, Integer> specifyMachineSpecCountMap,
                                                      List<TqMachineInfo> allMachineList,
                                                      Map<String, TqMachineInfo> machineInfoMap,
                                                      double defaultQuota,
                                                      Map<String, String> plannedMachineMap,
                                                      TqScheduleContext context) {
        double backupThreshold = context.getParams().getBackupShiftThreshold() == null ? 1000D
                : context.getParams().getBackupShiftThreshold();

        // 1. 找出当前班次是备库触发班次但planQty=0的规格（需要保障的规格）
        // 注意：此时规格可能尚未分配机台（1班未排产导致machineCode为空），
        // 需通过plannedMachineMap查找其他同组规格的机台来推断应分配的机台
        List<TqScheduleResultVo> needGuaranteeSpecs = scheduleList.stream()
                .filter(s -> s.getBackupTriggerClass() != null && s.getBackupTriggerClass() == classNum)
                .filter(s -> getClassPlanQty(s, classNum) <= 0)
                .collect(Collectors.toList());

        log.info("[S3.2-触发班次产能保障] 班次:{}, 需保障规格数:{}", classNum, needGuaranteeSpecs.size());
        for (TqScheduleResultVo s : needGuaranteeSpecs) {
            log.info("[S3.2-触发班次产能保障] 需保障规格:{}, 当班planQty:{}, backupTriggerClass:{}, backupRemainingQty:{}, machineCode:{}",
                    s.getBeadCode(), getClassPlanQty(s, classNum), s.getBackupTriggerClass(), s.getBackupRemainingQty(), s.getMachineCode());
        }

        if (needGuaranteeSpecs.isEmpty()) {
            return;
        }

        // 2. 按机台分组需要保障的规格（兼容machineCode为空的情况，通过plannedMachineMap推断）
        Map<String, List<TqScheduleResultVo>> machineGuaranteeMap = new HashMap<>();
        for (TqScheduleResultVo spec : needGuaranteeSpecs) {
            String machineCode = spec.getMachineCode();
            if (StringUtils.isEmpty(machineCode)) {
                // 通过plannedMachineMap查找：找到同组规格已分配的机台
                machineCode = plannedMachineMap.get(spec.getBeadCode());
            }
            if (StringUtils.isEmpty(machineCode)) {
                // 仍未找到机台：遍历其他备库规格的机台，找同机台定额最匹配的
                // 取第一个可用的机台（简化处理，实际场景中同一组规格通常在同一机台）
                for (TqScheduleResultVo otherSpec : scheduleList) {
                    if (!otherSpec.getBeadCode().equals(spec.getBeadCode())
                            && StringUtils.isNotEmpty(otherSpec.getMachineCode())) {
                        machineCode = otherSpec.getMachineCode();
                        break;
                    }
                }
            }
            if (StringUtils.isNotEmpty(machineCode)) {
                machineGuaranteeMap.computeIfAbsent(machineCode, k -> new ArrayList<>()).add(spec);
                // 如果规格尚未分配机台，先设置机台
                if (StringUtils.isEmpty(spec.getMachineCode())) {
                    spec.setMachineCode(machineCode);
                }
            }
        }

        for (Map.Entry<String, List<TqScheduleResultVo>> entry : machineGuaranteeMap.entrySet()) {
            String machineCode = entry.getKey();
            List<TqScheduleResultVo> guaranteeSpecs = entry.getValue();

            // 仅处理多规格机台
            if (isSingleSpecMachine(machineCode, machineSpecCountMap, specifyMachineSpecCountMap)) {
                continue;
            }

            TqMachineInfo machine = machineInfoMap.get(machineCode);
            double machineQuota = getMachineQuota(machine, defaultQuota);

            // 3. 找出同机台上可以让渡产能的规格
            List<TqScheduleResultVo> allSpecsOnMachine = scheduleList.stream()
                    .filter(s -> machineCode.equals(s.getMachineCode()))
                    .filter(s -> getClassPlanQty(s, classNum) > 0)
                    .collect(Collectors.toList());

            for (TqScheduleResultVo guaranteeSpec : guaranteeSpecs) {
                // 需要保障的排产量 = 阈值（与S3.1初始排产上限一致）
                double neededQty = Math.min(backupThreshold,
                        guaranteeSpec.getBackupRemainingQty() == null ? 0D : guaranteeSpec.getBackupRemainingQty());
                if (neededQty <= 0) {
                    continue;
                }

                // 按让渡优先级排序可让渡规格：
                // 优先让渡非备库规格，其次让渡触发班次 != 当前班次的备库规格
                // 同优先级内按backupRemainingQty升序（缺口小的先让渡）
                List<TqScheduleResultVo> donorCandidates = allSpecsOnMachine.stream()
                        .filter(s -> !s.getBeadCode().equals(guaranteeSpec.getBeadCode()))
                        .filter(s -> getClassPlanQty(s, classNum) > 0)
                        .sorted((o1, o2) -> {
                            // 让渡优先级：非备库(0) > 备库触发班次!=当前(1) > 备库触发班次==当前(2)
                            int priority1 = getDonorPriority(o1, classNum);
                            int priority2 = getDonorPriority(o2, classNum);
                            if (priority1 != priority2) {
                                return Integer.compare(priority1, priority2);
                            }
                            // 同优先级按backupRemainingQty升序（缺口小的先让渡）
                            double rem1 = o1.getBackupRemainingQty() == null ? 0D : o1.getBackupRemainingQty();
                            double rem2 = o2.getBackupRemainingQty() == null ? 0D : o2.getBackupRemainingQty();
                            return Double.compare(rem1, rem2);
                        })
                        .collect(Collectors.toList());

                double remainingNeeded = neededQty;
                for (TqScheduleResultVo donorSpec : donorCandidates) {
                    if (remainingNeeded <= 0) {
                        break;
                    }

                    double donorPlanQty = getClassPlanQty(donorSpec, classNum);
                    if (donorPlanQty <= 0) {
                        continue;
                    }

                    // 让渡量上限：该规格当班planQty中超出阈值的剩余部分
                    // 非备库规格可以全部让渡；备库规格保留阈值量以确保本班最低排产
                    // 但如果让渡后该规格本班仍有排产（planQty > 0），即使不足阈值也可接受，
                    // 因为产能保障的目标是确保备库触发班次有排产量，优先级高于非触发班次的满排
                    Integer donorTriggerClass = donorSpec.getBackupTriggerClass();
                    double maxDonation;
                    if (donorTriggerClass == null || donorTriggerClass <= 0) {
                        // 非备库规格：可以全部让渡
                        maxDonation = donorPlanQty;
                    } else if (donorTriggerClass != classNum) {
                        // 备库但触发班次≠当前班次：可以全部让渡，本班不是它的关键班次
                        maxDonation = donorPlanQty;
                    } else {
                        // 备库且触发班次==当前班次：优先保障自身，不让渡
                        maxDonation = 0D;
                    }
                    if (maxDonation <= 0) {
                        continue;
                    }

                    double donateQty = Math.min(remainingNeeded, maxDonation);

                    // 让渡：扣减被让渡规格当班计划量
                    double newDonorPlan = BigDecimalUtil.sub(donorPlanQty, donateQty);
                    setClassPlanQty(donorSpec, classNum, newDonorPlan);
                    capacityMaps[classIdx].put(machineCode,
                            capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .subtract(BigDecimalUtils.valueOf(donateQty)));

                    // 被让渡规格的扣减量累加到后续班次
                    int targetClassNum;
                    if (donorTriggerClass != null && donorTriggerClass > 0 && donorTriggerClass > classNum) {
                        targetClassNum = donorTriggerClass;
                    } else {
                        targetClassNum = Math.min(classNum + 1, 6);
                    }
                    double targetPlan = getClassPlanQty(donorSpec, targetClassNum);
                    setClassPlanQty(donorSpec, targetClassNum, BigDecimalUtil.add(targetPlan, donateQty));

                    // 保障规格获得产能：设置当班计划量
                    setClassPlanQty(guaranteeSpec, classNum, donateQty);
                    capacityMaps[classIdx].put(machineCode,
                            capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .add(BigDecimalUtils.valueOf(donateQty)));

                    // 从后续班次扣减保障规格等额的延后量（避免总量超排）
                    forwardDigestRemainingQty(guaranteeSpec, classNum, donateQty);

                    remainingNeeded = BigDecimalUtil.sub(remainingNeeded, donateQty);

                    autoScheduleLogService.insertTqScheduleLog(donorSpec.getBatchNo(), donorSpec.getOrderNo(),
                            "S3.2-触发班次产能保障-让渡", "胎圈代码：" + donorSpec.getBeadCode()
                                    + "，" + classNum + "班让渡" + donateQty + "→" + guaranteeSpec.getBeadCode()
                                    + "，扣减量延后至" + targetClassNum + "班"
                                    + "，释放机台" + machineCode + "产能" + donateQty);
                    autoScheduleLogService.insertTqScheduleLog(guaranteeSpec.getBatchNo(), guaranteeSpec.getOrderNo(),
                            "S3.2-触发班次产能保障-获得", "胎圈代码：" + guaranteeSpec.getBeadCode()
                                    + "，" + classNum + "班获得" + donateQty + "产能（备库触发班次保障）");
                }
            }
        }
    }

    /**
     * 获取规格的让渡优先级（数值越小越优先让渡）。
     *
     * @param spec 排程记录
     * @param classNum 当前班次号
     * @return 优先级：0=非备库，1=备库触发班次!=当前班次，2=备库触发班次==当前班次
     */
    private int getDonorPriority(TqScheduleResultVo spec, int classNum) {
        Integer triggerClass = spec.getBackupTriggerClass();
        if (triggerClass == null || triggerClass <= 0) {
            return 0;  // 非备库规格，最优先让渡
        }
        if (triggerClass != classNum) {
            return 1;  // 备库但触发班次不是当前班次
        }
        return 2;  // 备库且触发班次是当前班次，最低优先级让渡
    }

    /**
     * 前移消化：从后续班次planQty中等额扣减回填量，防止总量超排。
     *
     * <p>S3.2 剩余产能二次分配的本质是"把后续班次的延后量前移到当班排产"，
     * 需要同步减少后续班次的planQty，否则当班增加了回填量、后续班次延后量不变，
     * 导致总排产量大于备库总量。</p>
     *
     * <p>处理逻辑：从下一班开始逐班扣减planQty，直到扣减总量=回填量。
     * 如果后续班次planQty不足，则只扣减到0为止（不超扣）。</p>
     *
     * @param spec       排程记录
     * @param classNum   当班班次号（1-6）
     * @param assignQty  回填量（需要从前移消化中扣减的总量）
     */
    private void forwardDigestRemainingQty(TqScheduleResultVo spec, int classNum, double assignQty) {
        double remaining = assignQty;
        for (int nextClass = classNum + 1; nextClass <= 6 && remaining > 0; nextClass++) {
            double nextPlan = getClassPlanQty(spec, nextClass);
            if (nextPlan <= 0) {
                continue;
            }
            double deduct = Math.min(nextPlan, remaining);
            setClassPlanQty(spec, nextClass, BigDecimalUtil.sub(nextPlan, deduct));
            remaining = BigDecimalUtil.sub(remaining, deduct);
        }
    }

    /**
     * 诊断日志：检查S3结束后是否存在"有计划量但未分配机台"的记录。
     *
     * <p>用于排查机台编码为空的问题：如果S3阶段就有空机台记录，
     * 说明问题出在S3本身（计划量为0被跳过）；如果S3阶段没有空机台记录，
     * 说明问题出在后续阶段（S5均衡调整等注入了计划量）。</p>
     *
     * @param context 排程上下文
     */
    private void logUnassignedSchedule(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }
        List<TqScheduleResultVo> unassignedList = scheduleList.stream()
                .filter(s -> StringUtils.isEmpty(s.getMachineCode()) && getTotalPlanQty(s) > 0)
                .collect(Collectors.toList());
        if (!unassignedList.isEmpty()) {
            log.warn("[S3] 存在{}条有计划量但未分配机台的记录", unassignedList.size());
            for (TqScheduleResultVo vo : unassignedList) {
                autoScheduleLogService.insertTqScheduleLog(vo.getBatchNo(), vo.getOrderNo(),
                        "S3诊断-未分配机台但有计划量",
                        "胎圈代码：" + vo.getBeadCode()
                                + "，6班计划量：" + getTotalPlanQty(vo)
                                + "，1~6班分别为：" + vo.getClass1PlanQty() + "/" + vo.getClass2PlanQty()
                                + "/" + vo.getClass3PlanQty() + "/" + vo.getClass4PlanQty()
                                + "/" + vo.getClass5PlanQty() + "/" + vo.getClass6PlanQty());
            }
        }
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

    /**
     * 判断机台是否为单一规格机台（只生产1个规格）。
     *
     * <p>分层判断逻辑：</p>
     * <ol>
     *   <li>第1层（权威）：specifyMachineSpecCountMap — 基于限制作业配置反向统计，有限制作业的机台100%精准</li>
     *   <li>第2层（兜底）：machineSpecCountMap — 基于S3.1实际机台分配结果统计，无限制作业的机台用此判断</li>
     * </ol>
     *
     * @param machineCode 机台编码
     * @param machineSpecCountMap S3.1实际分配的机台→规格数映射
     * @param specifyMachineSpecCountMap 限制作业配置的机台→规格数映射
     * @return true=单一规格机台，false=多规格机台
     */
    private boolean isSingleSpecMachine(String machineCode,
                                        Map<String, Integer> machineSpecCountMap,
                                        Map<String, Integer> specifyMachineSpecCountMap) {
        // 第1层：限制作业配置（权威判断）
        Integer specifyCount = specifyMachineSpecCountMap.get(machineCode);
        if (specifyCount != null) {
            return specifyCount == 1;
        }
        // 第2层：S3.1实际分配结果（兜底判断）
        Integer count = machineSpecCountMap.get(machineCode);
        return count != null && count == 1;
    }

    /**
     * 计算备库胎圈当班初始排产上限。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>非备库胎圈：返回 quota（不受阈值限制，走现有逻辑）</li>
     *   <li>备库胎圈 + 单一规格机台：返回 quota（单一规格只受机台定额限制）</li>
     *   <li>备库胎圈 + 多规格机台：返回 min(quota, threshold)（受SYS1101029阈值限制）</li>
     * </ul>
     *
     * @param scheduleVo 排程结果VO
     * @param machineCode 机台编码
     * @param machineSpecCountMap S3.1实际分配的机台→规格数映射
     * @param specifyMachineSpecCountMap 限制作业配置的机台→规格数映射
     * @param machineQuota 机台定额
     * @param backupShiftThreshold 备库班次阈值（SYS1101029）
     * @param classNum 当前班次号（1-6）
     * @return 当班初始排产上限
     */
    private double getBackupInitAssignLimit(TqScheduleResultVo scheduleVo, String machineCode,
                                            Map<String, Integer> machineSpecCountMap,
                                            Map<String, Integer> specifyMachineSpecCountMap,
                                            double machineQuota, double backupShiftThreshold, int classNum) {
        boolean isBackupSpec = scheduleVo.getBackupTriggerClass() != null
                && scheduleVo.getBackupTriggerClass() > 0;
        if (!isBackupSpec) {
            // 非备库胎圈：不受阈值限制
            return machineQuota;
        }
        if (isSingleSpecMachine(machineCode, machineSpecCountMap, specifyMachineSpecCountMap)) {
            // 单一规格机台：备库胎圈只受机台定额限制，可满排
            return machineQuota;
        }
        // 多规格机台：判断当前班次是否为尾数合并班次
        Integer mergedTailClass = scheduleVo.getMergedTailClass();
        if (mergedTailClass != null && mergedTailClass == classNum) {
            // 尾数合并班次：S2阶段已将尾数合并到本班（如500+9.6=509.6→510），
            // 放宽阈值限制为机台定额，避免本班计划量被SYS1101029阈值再次截断延后到下一班
            return machineQuota;
        }
        // 多规格机台：备库胎圈受阈值限制，初始排产不超过 threshold
        return Math.min(machineQuota, backupShiftThreshold);
    }

    /**
     * 将超出机台定额的计划量延后至下一班次累加。
     *
     * <p>业务规则：当班满排机台定额（阈值）后，剩余量延后到下一班；
     * 下一班继续满排，仍排不完则继续延后，直到6班排完或排完所有计划量。</p>
     *
     * @param scheduleVo 排程记录
     * @param currentClass 当前班次号（1~6）
     * @param overflowQty 溢出量
     */
    private void deferToNextClass(TqScheduleResultVo scheduleVo, int currentClass, double overflowQty) {
        if (currentClass >= 6) {
            // 已是最后一班，无法延后，记录溢出
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "延后失败(已是最后一班)", "胎圈代码：" + scheduleVo.getBeadCode()
                            + "，" + currentClass + "班溢出量" + overflowQty + "无法延后");
            log.warn("[S3] 胎圈{}的6班溢出量{}无法延后", scheduleVo.getBeadCode(), overflowQty);
            return;
        }
        // 延后至下一班次累加（当前班次的值已由调用方正确设置，此处不再扣减）
        double nextClassQty = getClassPlanQty(scheduleVo, currentClass + 1);
        setClassPlanQty(scheduleVo, currentClass + 1, BigDecimalUtil.add(nextClassQty, overflowQty));

        // 延后量不再扣减 backupRemainingQty：
        // 延后量仍属"待排"（只是换了班次排），不是"已排产量"，不应从备库剩余量中扣减。
        // 否则 S3.2 redistributeResidualCapacity 回填时 backupRemaining 已归零，无法回填剩余产能。
        // backupRemaining 仅在"实际排产到机台"时扣减（调用方已处理），延后量不扣减。
        // S5.6 recalcBackupRemainingQty 会做最终重算兜底。

        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "延后至下一班", "胎圈代码：" + scheduleVo.getBeadCode()
                        + "，" + currentClass + "班溢出量" + overflowQty + "延后至" + (currentClass + 1) + "班");
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

    /**
     * 获取总计划量
     */
    private double getTotalPlanQty(TqScheduleResultVo scheduleVo) {
        double total = 0D;
        for (int i = 1; i <= 6; i++) {
            total = BigDecimalUtil.add(total, getClassPlanQty(scheduleVo, i));
        }
        return total;
    }

    /**
     * 检索符合条件的可选机台列表。
     *
     * <p>Phase 3 重构增强：在 Phase 2 基础上新增候选机台追踪，记录每个机台的过滤状态、过滤原因、
     * 剩余产能和选中评分，写入 {@code context.candidateTraceMap} 供 Phase 4 解释快照使用。</p>
     *
     * <p>追踪逻辑：</p>
     * <ul>
     *   <li>初始化全部机台为候选对象（filtered=false）</li>
     *   <li>策略链过滤时，被过滤的机台标记 filtered=true 并记录过滤策略名</li>
     *   <li>班次过滤时，被过滤的机台标记 filtered=true 并记录原因"OPEN_MACHINE_CLASS_NOT_MATCH"</li>
     *   <li>通过过滤的机台按排序优先级评分，选中机台在调用方标记 selected=true</li>
     *   <li>候选列表（含被过滤机台）写入 {@code context.candidateTraceMap}</li>
     * </ul>
     */
    private List<TqMachineInfo> searchOptionalMachineList(TqScheduleResultVo scheduleVo, String classCode,
                                                          Map<String, BigDecimal> capacityMap,
                                                          List<TqMachineInfo> allMachineList,
                                                          TqScheduleContext context,
                                                          List<IMachineFilterStrategy> sortedStrategies,
                                                          Map<String, String> plannedMachineMap) {
        // 1. 设置当前班次编码到上下文，供策略链中的MaintenanceFilter按班次精确过滤
        context.setCurrentClassCode(classCode);

        // Phase 3 重构新增：初始化候选机台追踪列表（含全部机台，被过滤的机台标记 filtered=true）
        String beadCode = scheduleVo.getBeadCode();
        Map<String, TqMachineCandidate> candidateMap = new LinkedHashMap<>();
        // Phase 5 重构新增：读取当前班次索引，用于评分时判断是否跨班次切换
        int currentClassIdx = resolveClassIndex(classCode);
        for (TqMachineInfo machine : allMachineList) {
            TqMachineCandidate candidate = new TqMachineCandidate();
            candidate.setMachineCode(machine.getMachineCode());
            candidate.setMachineName(machine.getMachineName());
            candidate.setFiltered(false);
            // 剩余产能 = 机台定额 - 已排产量
            BigDecimal usedCapacity = capacityMap.getOrDefault(machine.getMachineCode(), BigDecimal.ZERO);
            BigDecimal quota = machine.getQuota() == null ? BigDecimal.ZERO : BigDecimal.valueOf(machine.getQuota());
            candidate.setRemainCapacity(quota.subtract(usedCapacity));
            candidate.setRemainQuota(candidate.getRemainCapacity());
            // Phase 5 重构新增：填充任务链末尾节点信息，供评分策略使用
            fillLastChainInfo(candidate, machine.getMachineCode(), beadCode, currentClassIdx, context);
            candidateMap.put(machine.getMachineCode(), candidate);
        }

        // 2. 通过策略链过滤
        List<TqMachineInfo> filtered = new ArrayList<>(allMachineList);
        List<String> hitStrategies = new ArrayList<>();
        for (IMachineFilterStrategy strategy : sortedStrategies) {
            int beforeSize = filtered.size();
            List<TqMachineInfo> afterFilter = strategy.filter(filtered, scheduleVo, context);
            // Phase 3 重构新增：记录被该策略过滤掉的机台
            if (beforeSize > afterFilter.size()) {
                Set<String> afterCodes = afterFilter.stream()
                        .map(TqMachineInfo::getMachineCode)
                        .collect(Collectors.toSet());
                for (TqMachineInfo machine : filtered) {
                    if (!afterCodes.contains(machine.getMachineCode())) {
                        TqMachineCandidate candidate = candidateMap.get(machine.getMachineCode());
                        if (candidate != null && !candidate.isFiltered()) {
                            candidate.setFiltered(true);
                            candidate.setFilterStrategy(strategy.getStrategyName());
                            candidate.setFilterReasonCode("STRATEGY_FILTER");
                            candidate.setFilterReasonDesc("被" + strategy.getStrategyName() + "过滤");
                        }
                    }
                }
                hitStrategies.add(strategy.getClass().getSimpleName());
            }
            filtered = afterFilter;
            if (filtered.isEmpty()) {
                break;
            }
        }

        // 3. 过滤对应班次可用的机台
        int beforeClassFilter = filtered.size();
        filtered = filtered.stream()
                .filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode))
                .collect(Collectors.toList());
        // Phase 3 重构新增：记录被班次过滤的机台
        if (beforeClassFilter > filtered.size()) {
            Set<String> afterCodes = filtered.stream()
                    .map(TqMachineInfo::getMachineCode)
                    .collect(Collectors.toSet());
            for (TqMachineInfo machine : allMachineList) {
                if (!afterCodes.contains(machine.getMachineCode())) {
                    TqMachineCandidate candidate = candidateMap.get(machine.getMachineCode());
                    if (candidate != null && !candidate.isFiltered()) {
                        candidate.setFiltered(true);
                        candidate.setFilterStrategy("OpenMachineClassFilter");
                        candidate.setFilterReasonCode("OPEN_MACHINE_CLASS_NOT_MATCH");
                        candidate.setFilterReasonDesc("机台开机班次" + machine.getOpenMachineClass()
                                + "不包含当前班次" + classCode);
                    }
                }
            }
        }

        // Phase 2 重构新增：埋点机台过滤策略证据，便于排程结果可解释性回溯
        if (!hitStrategies.isEmpty() || beforeClassFilter > filtered.size()) {
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("classCode", classCode);
            evidence.put("allMachineCount", allMachineList.size());
            evidence.put("hitStrategies", hitStrategies);
            evidence.put("finalCandidateCount", filtered.size());
            TqDemandCalcHelper.addRuleTrace(context, scheduleVo.getBeadCode(),
                    TqScheduleRuleCodeEnum.MACHINE_FILTER,
                    filtered.isEmpty() ? TqScheduleRuleResultEnum.MISS : TqScheduleRuleResultEnum.HIT,
                    evidence);
        }

        // 4. 按优先级排序（排序即评分，rank=1 的为首选机台）
        filtered = filtered.stream().sorted((m1, m2) -> {
            // 同一个规格优先排在已排过相同规格的机台上
            String scheduleMachineCode = plannedMachineMap.getOrDefault(beadCode, "");
            Integer hasMachine1 = m1.getMachineCode().equals(scheduleMachineCode) ? 0 : 1;
            Integer hasMachine2 = m2.getMachineCode().equals(scheduleMachineCode) ? 0 : 1;
            int result = hasMachine1.compareTo(hasMachine2);
            if (result != 0) {
                return result;
            }
            // 按剩余产能升序排序
            BigDecimal capacity1 = capacityMap.getOrDefault(m1.getMachineCode(), BigDecimal.ZERO);
            BigDecimal capacity2 = capacityMap.getOrDefault(m2.getMachineCode(), BigDecimal.ZERO);
            result = capacity1.compareTo(capacity2);
            if (result != 0) {
                return result;
            }
            return m1.getMachineCode().compareTo(m2.getMachineCode());
        }).collect(Collectors.toList());

        // Phase 3 重构新增：为通过过滤的机台设置排名和评分描述，写入 candidateTraceMap
        List<TqMachineCandidate> candidateTraceList = new ArrayList<>(candidateMap.values());
        String scheduleMachineCode = plannedMachineMap.getOrDefault(beadCode, "");
        int rank = 0;
        for (TqMachineInfo machine : filtered) {
            rank++;
            TqMachineCandidate candidate = candidateMap.get(machine.getMachineCode());
            if (candidate != null) {
                candidate.setRank(rank);
                // 评分：按排名反序（rank=1 得分最高），便于后续排序
                candidate.setScore(BigDecimal.valueOf(filtered.size() - rank + 1));
                // 评分结果描述
                StringBuilder scoreResult = new StringBuilder();
                if (machine.getMachineCode().equals(scheduleMachineCode)) {
                    scoreResult.append("已排过相同规格的机台优先；");
                }
                // Phase 5 重构新增：补充任务链连续性信息
                if (candidate.getContinuityScore() != null) {
                    scoreResult.append("连续性=").append(candidate.getContinuityScore());
                    if (candidate.getSwitchTime() > 0) {
                        scoreResult.append("(切换").append(candidate.getSwitchTime()).append("h)");
                    }
                    scoreResult.append("；");
                }
                BigDecimal usedCapacity = capacityMap.getOrDefault(machine.getMachineCode(), BigDecimal.ZERO);
                BigDecimal quota = machine.getQuota() == null ? BigDecimal.ZERO : BigDecimal.valueOf(machine.getQuota());
                scoreResult.append("剩余产能=").append(quota.subtract(usedCapacity))
                        .append("，排名=").append(rank);
                candidate.setScoreResult(scoreResult.toString());
                // 标记首选机台（rank=1）为选中状态，表示"应选机台"
                // 注意：调用方可能因机台已满等原因不使用此机台，最终落库机台以 scheduleVo.machineCode 为准
                if (rank == 1) {
                    candidate.setSelected(true);
                }
            }
        }
        // 被过滤的机台排名设为 -1（不参与排序）
        for (TqMachineCandidate candidate : candidateTraceList) {
            if (candidate.isFiltered()) {
                candidate.setRank(-1);
            }
        }
        context.getCandidateTraceMap().put(beadCode, candidateTraceList);

        return filtered;
    }

    /**
     * 设置6个班次的生产顺序。
     *
     * <p>排序规则：1.相同英寸连续生产 2.同英寸内按库存供应时长升序排序</p>
     */
    public void setProduceOrder(List<TqScheduleResultVo> scheduleList) {
        // 按机台分组：顺序值应按机台独立编号（同一机台同一班次内的规格顺序1,2,3...）
        // 而非全局编号，避免"机台只有2个规格但顺序值=4"的问题
        Map<String, List<TqScheduleResultVo>> machineGroupMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .collect(Collectors.groupingBy(TqScheduleResultVo::getMachineCode));

        for (int classIdx = 0; classIdx < 6; classIdx++) {
            final int ci = classIdx;
            // 每个机台内独立设置顺序值
            for (Map.Entry<String, List<TqScheduleResultVo>> entry : machineGroupMap.entrySet()) {
                String machineCode = entry.getKey();
                List<TqScheduleResultVo> machineSpecs = entry.getValue();
                // 排序：1.相同英寸连续 2.同英寸内按供应时长升序
                List<TqScheduleResultVo> sortedList = machineSpecs.stream()
                        .filter(s -> getClassPlanQty(s, ci + 1) > 0)
                        .sorted(Comparator
                                .comparing((TqScheduleResultVo s) -> s.getDimension() == null ? BigDecimal.ZERO : s.getDimension())
                                .thenComparing(s -> s.getSupplyTime() == null ? 0D : s.getSupplyTime()))
                        .collect(Collectors.toList());

                int order = 1;
                for (TqScheduleResultVo scheduleVo : sortedList) {
                    setClassProduceOrder(scheduleVo, ci + 1, order++);

                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "设置" + (ci + 1) + "班生产顺序",
                            "机台" + machineCode + "内相同英寸连续生产规则，寸口=" + scheduleVo.getDimension()
                                    + "，供应时长=" + scheduleVo.getSupplyTime()
                                    + "，生产顺序=" + (order - 1));
                }
            }
        }
    }

    /**
     * 设置指定班次的生产顺序
     */
    private void setClassProduceOrder(TqScheduleResultVo scheduleVo, int classNum, int order) {
        switch (classNum) {
            case 1: scheduleVo.setClass1ProduceOrder(order); break;
            case 2: scheduleVo.setClass2ProduceOrder(order); break;
            case 3: scheduleVo.setClass3ProduceOrder(order); break;
            case 4: scheduleVo.setClass4ProduceOrder(order); break;
            case 5: scheduleVo.setClass5ProduceOrder(order); break;
            case 6: scheduleVo.setClass6ProduceOrder(order); break;
        }
    }

    /**
     * 计算当班实际可排上限（支持机台定额超排容忍 SYS1101031）
     * <p>默认规则：min(机台剩余产能, 备库初始排产上限)</p>
     * <p>超排容忍规则：当计划量超出机台剩余产能，且超出部分 ≤ 超排容忍阈值时，允许当班超排到计划量（不超过备库初始排产上限），不延后到下一班</p>
     * <p>避免尾数被延后到下一班单独排产，降低生产效率</p>
     *
     * @param planQty              S2.2 计算出的当班计划量
     * @param remainingCapacity    机台剩余产能（机台定额 - 已排产能）
     * @param initAssignLimit      备库初始排产上限（getBackupInitAssignLimit 计算结果）
     * @param overAssignTolerance  超排容忍阈值（SYS1101031，null 或 ≤0 表示不启用）
     * @param scheduleVo           排程记录（用于日志）
     * @param machineCode          机台编号（用于日志）
     * @param classNum             班次号 1-6
     * @param context              排程上下文
     * @return 当班实际可排上限
     */
    private double calcEffectiveCapacityWithTolerance(double planQty, double remainingCapacity, double initAssignLimit,
                                                      Double overAssignTolerance, TqScheduleResultVo scheduleVo,
                                                      String machineCode, int classNum, TqScheduleContext context) {
        double tolerance = overAssignTolerance == null ? 0D : overAssignTolerance;
        // 计划量超出机台剩余产能的部分
        double overflow = BigDecimalUtil.sub(planQty, remainingCapacity);
        // 调试日志：打印关键参数值，便于排查超排容忍是否生效
        log.info("[S3-超排容忍] 胎圈={}，{}班，机台={}，planQty={}，remainingCapacity={}，initAssignLimit={}，tolerance={}，overflow={}",
                scheduleVo.getBeadCode(), classNum, machineCode, planQty, remainingCapacity, initAssignLimit, tolerance, overflow);
        double effectiveCapacity;
        if (tolerance > 0 && overflow > 0 && overflow <= tolerance) {
            // 超出部分在容忍范围内：允许超排到计划量（不超过备库初始排产上限）
            effectiveCapacity = Math.min(planQty, initAssignLimit);
            // 记录超排日志，便于排查
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    classNum + "班机台定额超排容忍",
                    "胎圈代码：" + scheduleVo.getBeadCode() + "，机台" + machineCode
                            + "，计划量" + planQty + "，机台剩余产能" + remainingCapacity
                            + "，超出" + overflow + " ≤ 超排容忍阈值(" + tolerance + ")"
                            + "，当班超排" + effectiveCapacity);
        } else {
            // 未超排或未启用超排容忍：保持原有限制
            effectiveCapacity = Math.min(remainingCapacity, initAssignLimit);
        }
        log.info("[S3-超排容忍] 胎圈={}，{}班，最终effectiveCapacity={}，是否超排={}",
                scheduleVo.getBeadCode(), classNum, effectiveCapacity,
                tolerance > 0 && overflow > 0 && overflow <= tolerance);
        return effectiveCapacity;
    }

    /**
     * 将指定key分配给特定机台
     */
    private void putMachineCode(String key, String machineCode, Map<String, List<String>> machineMap) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        List<String> machineList = machineMap.get(key);
        if (machineList == null) {
            machineList = new ArrayList<>();
            machineMap.put(key, machineList);
        }
        if (!machineList.contains(machineCode)) {
            machineList.add(machineCode);
        }
    }

    /**
     * 机台分配日志
     */
    private void chooseMachineLog(TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        StringBuilder logDetail = new StringBuilder();
        logDetail.append("①优先选择定点机台中限制作业集合匹配上的机台;②如果没有，在选择口型板与机台对应关系集合的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(DIVISION);
        logDetail.append("定点机台中限制作业集合：").append(toJSONString(context.getSpecifyCanMachineMap())).append(DIVISION);
        logDetail.append("定点机台中不可作业集合：").append(toJSONString(context.getSpecifyNotMachineMap())).append(DIVISION);
        logDetail.append("口型板与机台对应关系集合：").append(toJSONString(context.getMouthPlateMachineMap())).append(DIVISION);
        logDetail.append("结果数据：").append(toJSONString(scheduleVo)).append(DIVISION);
        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "设置生产线（机台）", logDetail.toString());
    }

    /**
     * 记录机台定额约束证据（当班满排-剩余延后场景）。
     *
     * <p>Phase 2 重构新增：当计划量超出机台定额或备库阈值导致部分延后时，
     * 记录 MACHINE_QUOTA_LIMIT 证据，便于排程结果可解释性回溯。</p>
     *
     * @param context           排程上下文
     * @param scheduleVo        排程结果 VO
     * @param machineCode       命中机台编码
     * @param classNum          班次号（1-6）
     * @param assignedQty       当班实际排产量
     * @param overflowQty       延后至下一班的溢出量
     * @param isBackupThreshold 是否为备库胎圈多规格机台阈值限制（true=备库阈值，false=机台定额）
     */
    private void recordQuotaExceed(TqScheduleContext context, TqScheduleResultVo scheduleVo,
                                   String machineCode, int classNum,
                                   double assignedQty, double overflowQty, boolean isBackupThreshold) {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("machineCode", machineCode);
        evidence.put("classNum", classNum);
        evidence.put("assignedQty", assignedQty);
        evidence.put("overflowQty", overflowQty);
        evidence.put("limitType", isBackupThreshold ? "BACKUP_THRESHOLD" : "MACHINE_QUOTA");
        TqDemandCalcHelper.addRuleTrace(context, scheduleVo.getBeadCode(),
                TqScheduleRuleCodeEnum.MACHINE_QUOTA_LIMIT,
                TqScheduleRuleResultEnum.ADJUST,
                evidence);
    }

    private String logSplit(String... messages) {
        StringBuilder sb = new StringBuilder();
        for (String msg : messages) {
            sb.append(msg).append(DIVISION);
        }
        return sb.toString();
    }

    /**
     * 构建未排原因描述
     */
    private String buildUnscheduledReason(TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        List<String> reasons = new ArrayList<>();
        if (context.getSpecifyCanMachineMap().containsKey(scheduleVo.getBeadCode())) {
            reasons.add("定点机台限制作业范围内无可用机台");
        }
        if (StringUtils.isNotEmpty(scheduleVo.getMouthPlateCode())) {
            reasons.add("口型板[" + scheduleVo.getMouthPlateCode() + "]无对应可用机台");
        }
        if (scheduleVo.getDimension() != null) {
            reasons.add("寸口[" + scheduleVo.getDimension() + "]无对应可用机台");
        }
        if (reasons.isEmpty()) {
            reasons.add("所有机台均不满足条件");
        }
        return String.join("；", reasons);
    }

    // ==================== 任务链构建 ====================

    /**
     * 构建任务链（按优先级排序后串联）。
     *
     * <p>按机台维度，将6个班次的生产任务串联成LinkedList。</p>
     * <p>每个节点包含：班次、机台、胎圈编码、计划量、预计库存等信息。</p>
     * <p>构建顺序：先收集所有节点，再按 classIndex升序 → produceOrder升序 排序后串联。</p>
     * <p>切换班次时，重新计算本班开始预计库存和库存保证班数。</p>
     */
    private void buildTaskChain(TqScheduleContext context) {
        Map<String, LinkedList<TqTaskNode>> taskChainMap = new HashMap<>();
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        double coefficient = context.getParams().getDemandCoefficient() == null ? 2D : context.getParams().getDemandCoefficient();

        // 第一步：收集所有节点（不排序）
        Map<String, List<TqTaskNode>> machineNodeMap = new HashMap<>();
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            if (StringUtils.isEmpty(scheduleVo.getMachineCode())) {
                continue;
            }
            String machineCode = scheduleVo.getMachineCode();
            List<TqTaskNode> nodeList = machineNodeMap.computeIfAbsent(machineCode, k -> new ArrayList<>());

            for (int classIdx = 1; classIdx <= 6; classIdx++) {
                double planQty = getClassPlanQty(scheduleVo, classIdx);
                if (planQty <= 0) {
                    continue;
                }

                TqTaskNode node = new TqTaskNode();
                node.setClassIndex(classIdx);
                node.setMachineCode(machineCode);
                node.setBeadCode(scheduleVo.getBeadCode());
                node.setPlanQty(planQty);
                node.setProduceOrder(getClassProduceOrder(scheduleVo, classIdx));
                node.setScheduleId(scheduleVo.getId());

                nodeList.add(node);
            }
        }

        // 第二步：按机台分组，组内按 classIndex升序 → produceOrder升序 排序后构建链
        for (Map.Entry<String, List<TqTaskNode>> entry : machineNodeMap.entrySet()) {
            String machineCode = entry.getKey();
            List<TqTaskNode> nodeList = entry.getValue();

            // 排序：先按班次，同班次内按生产顺序
            nodeList.sort(Comparator
                    .comparingInt(TqTaskNode::getClassIndex)
                    .thenComparingInt(TqTaskNode::getProduceOrder));

            // 构建有序链
            LinkedList<TqTaskNode> chain = new LinkedList<>();
            for (TqTaskNode node : nodeList) {
                // 计算本班开始预计库存
                double startStock = computeStartStock(chain, node, context);
                node.setStartStockQty(startStock);

                // 计算本班成型消耗量
                double cxConsume = getCxConsumeByNode(node, coefficient, context);
                node.setCxConsumeQty(cxConsume);

                // 计算本班结束预计库存 = 开始库存 + 本班产出 - 本班消耗
                double endStock = BigDecimalUtil.add(BigDecimalUtil.sub(startStock, cxConsume), node.getPlanQty());
                node.setEndStockQty(endStock);

                // 计算库存保证班数
                double guaranteeShifts = endStock > 0 && cxConsume > 0
                        ? BigDecimalUtil.div(endStock, cxConsume, 1) : 999;
                node.setGuaranteeShifts(guaranteeShifts);

                // 规格切换时长：与前一个节点规格不同时需要切换
                if (!chain.isEmpty()) {
                    TqTaskNode lastNode = chain.getLast();
                    if (!lastNode.getBeadCode().equals(node.getBeadCode())) {
                        double specSwitchTime = context.getParams().getSpecSwitchTime() == null ? 0.5D
                                : context.getParams().getSpecSwitchTime();
                        double inchSwitchTime = context.getParams().getInchSwitchTime() == null ? 1D
                                : context.getParams().getInchSwitchTime();
                        // 同班次内切换用规格切换时长，跨班次切换用英寸切换时长
                        boolean sameClass = lastNode.getClassIndex() == node.getClassIndex();
                        node.setSwitchTime(sameClass ? specSwitchTime : inchSwitchTime);
                    } else {
                        node.setSwitchTime(0);
                    }
                }

                // 计算有效生产时长（班次时长8小时 - 切换时长）
                double shiftHours = 8D;
                node.setEffectiveHours(BigDecimalUtil.sub(shiftHours, node.getSwitchTime()));

                chain.addLast(node);
            }

            taskChainMap.put(machineCode, chain);
        }

        context.setTaskChainMap(taskChainMap);
        log.info("[S3] 任务链构建完成, 机台数:{}", taskChainMap.size());

        // Phase 5：同步构建结构化任务链 taskChainGroup（保留旧 taskChainMap 兼容）
        syncToTaskChainGroup(context, taskChainMap);
    }

    /**
     * 将旧 {@code taskChainMap} 同步到结构化 {@code taskChainGroup}。
     *
     * <p>Phase 5 重构新增：遍历已计算好的旧任务链，按机台分组逐节点追加到
     * {@link MachineShiftTaskChain}，并注册到 {@code taskNodeIndex} 索引。
     * 同步前清空旧数据，避免重复加载导致节点重复。</p>
     *
     * <p>注意：同步只复制引用，不重新计算附加属性（startStockQty/endStockQty/switchTime 等），
     * 因为 {@link ScheduleTaskNode#getTask()} 直接持有原 {@link TqTaskNode} 对象，
     * 后续修改会双向生效。</p>
     *
     * @param context      排程上下文
     * @param taskChainMap 旧任务链 Map（key=机台编号，value=该机台的任务链）
     */
    private void syncToTaskChainGroup(TqScheduleContext context, Map<String, LinkedList<TqTaskNode>> taskChainMap) {
        // 清空旧结构化链表和节点索引，避免重复加载
        MachineShiftTaskChain<TqTaskNode> taskChainGroup = context.getTaskChainGroup();
        if (taskChainGroup == null) {
            log.warn("[S3] taskChainGroup 未初始化，跳过同步");
            return;
        }
        taskChainGroup.clear();
        context.getTaskNodeIndex().clear();

        if (taskChainMap == null || taskChainMap.isEmpty()) {
            return;
        }

        int syncCount = 0;
        for (Map.Entry<String, LinkedList<TqTaskNode>> entry : taskChainMap.entrySet()) {
            String machineCode = entry.getKey();
            LinkedList<TqTaskNode> chain = entry.getValue();
            if (chain == null || chain.isEmpty()) {
                continue;
            }
            for (TqTaskNode taskNode : chain) {
                // 直接调用任务链服务追加，复用其 businessKey 构建和节点索引注册逻辑
                ScheduleChainChangeResult<TqTaskNode> result = taskChainScheduleService.appendAutoTask(
                        taskNode, machineCode, context);
                syncCount++;
                if (log.isDebugEnabled() && result != null) {
                    log.debug("[S3] 任务链同步: machineCode={}, taskId={}, affectedNodes={}",
                            machineCode, result.getOperationType(), result.getAffectedNodes() == null
                                    ? 0 : result.getAffectedNodes().size());
                }
            }
        }
        log.info("[S3] 任务链结构化同步完成, 机台数:{}, 节点数:{}",
                taskChainGroup.values().size(), syncCount);
    }

    /**
     * 计算本班开始预计库存（基于任务链前序节点）。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>链条第一个节点：使用14点预计库存（planStockQty）</li>
     *   <li>同班次内后续节点：使用同班次前一个节点的结束库存</li>
     *   <li>跨班次节点：使用上一个班次最后一个节点的结束库存</li>
     * </ul>
     */
    private double computeStartStock(LinkedList<TqTaskNode> chain, TqTaskNode currentNode,
                                     TqScheduleContext context) {
        if (chain.isEmpty()) {
            // 链条第一个节点：从排产记录取14点预计库存
            TqScheduleResultVo scheduleVo = findScheduleVo(currentNode, context);
            return scheduleVo == null ? 0D : (scheduleVo.getPlanStockQty() == null ? 0D : scheduleVo.getPlanStockQty());
        }
        // 后续节点：使用上一个节点的结束库存
        TqTaskNode lastNode = chain.getLast();
        return lastNode.getEndStockQty();
    }

    // ==================== Phase 5 重构新增：任务链评分上下文 ====================

    /**
     * 将班次编码（两位字符串）解析为班次索引（1~6）。
     *
     * <p>班次编码与班次索引的映射参考 {@link #SHIFT_CLASS_MAP}：
     * 班次索引 1=D日中班("03")，2=D+1日夜班("01")，3=D+1日早班("02")，
     * 4=D+1日中班("03")，5=D+2日夜班("01")，6=D+2日早班("02")。</p>
     *
     * <p>该方法为反向查找：从编码找到第一个匹配的索引位置。无法匹配时返回 -1。</p>
     *
     * @param classCode 班次编码（"01"/"02"/"03"）
     * @return 班次索引（1~6）；未匹配时返回 -1
     */
    private int resolveClassIndex(String classCode) {
        if (StringUtils.isEmpty(classCode)) {
            return -1;
        }
        for (int i = 0; i < SHIFT_CLASS_MAP.length; i++) {
            if (SHIFT_CLASS_MAP[i].equals(classCode)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * 填充候选机台的任务链末尾节点信息（前置规格/班次/生产顺序/切换时长/连续性得分）。
     *
     * <p>Phase 5 重构新增：从 {@link TqScheduleContext#getTaskChainGroup()} 读取指定机台的链表末尾节点，
     * 提取前置规格（beadCode）、班次顺序（classIndex）、生产顺序（produceOrder），
     * 并基于当前待排规格和班次计算切换时长和连续性得分。</p>
     *
     * <p>计算规则：</p>
     * <ul>
     *   <li>机台无任务（首班次）：lastBeadCode=null，switchTime=0，continuityScore=90</li>
     *   <li>规格一致：switchTime=0，continuityScore=100（无切换损耗）</li>
     *   <li>同班次不同规格：switchTime=规格切换时长，continuityScore=60</li>
     *   <li>跨班次不同规格：switchTime=英寸切换时长，continuityScore=80</li>
     * </ul>
     *
     * @param candidate     待填充的候选机台对象
     * @param machineCode   机台编码
     * @param currentBead   当前待排胎圈规格
     * @param currentClassIdx 当前待排班次索引（1~6）
     * @param context       排程上下文
     */
    private void fillLastChainInfo(TqMachineCandidate candidate, String machineCode, String currentBead,
                                   int currentClassIdx, TqScheduleContext context) {
        if (candidate == null || StringUtils.isEmpty(machineCode) || context == null
                || context.getTaskChainGroup() == null) {
            return;
        }
        // 读取机台任务链末尾节点
        java.time.LocalDate scheduleDate = parseLocalDate(context.getScheduleDate());
        com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList<TqTaskNode> chain =
                context.getTaskChainGroup().get(machineCode, scheduleDate, null);
        if (chain == null || chain.getSize() == 0) {
            // 机台无任务：首班次，无需切换，但缺乏连续性优势
            candidate.setLastBeadCode(null);
            candidate.setLastClassIndex(null);
            candidate.setLastProduceOrder(null);
            candidate.setLastChainNode(null);
            candidate.setSwitchTime(0D);
            candidate.setContinuityScore(new BigDecimal("90"));
            return;
        }

        // 取末尾节点
        ScheduleTaskNode<TqTaskNode> lastNode = chain.toList().get(chain.getSize() - 1);
        TqTaskNode lastTask = lastNode.getTask();
        candidate.setLastChainNode(lastNode);
        if (lastTask != null) {
            candidate.setLastBeadCode(lastTask.getBeadCode());
            candidate.setLastClassIndex(lastTask.getClassIndex());
            candidate.setLastProduceOrder(lastTask.getProduceOrder());
        }

        // 计算切换时长和连续性得分
        double specSwitchTime = context.getParams().getSpecSwitchTime() == null ? 0.5D
                : context.getParams().getSpecSwitchTime();
        double inchSwitchTime = context.getParams().getInchSwitchTime() == null ? 1D
                : context.getParams().getInchSwitchTime();

        if (lastTask == null || StringUtils.isEmpty(lastTask.getBeadCode())
                || lastTask.getBeadCode().equals(currentBead)) {
            // 前置规格为空或与当前规格一致：无切换损耗
            candidate.setSwitchTime(0D);
            candidate.setContinuityScore(new BigDecimal("100"));
        } else if (lastTask.getClassIndex() == currentClassIdx) {
            // 同班次不同规格：规格切换时长，连续性一般
            candidate.setSwitchTime(specSwitchTime);
            candidate.setContinuityScore(new BigDecimal("60"));
        } else {
            // 跨班次不同规格：英寸切换时长，连续性较好
            candidate.setSwitchTime(inchSwitchTime);
            candidate.setContinuityScore(new BigDecimal("80"));
        }
    }

    /**
     * 将 {@code yyyy-MM-dd} 格式的排程日期字符串解析为 {@link java.time.LocalDate}。
     *
     * @param scheduleDate 排程日期字符串
     * @return {@link java.time.LocalDate}；空值或格式异常时返回当前日期
     */
    private java.time.LocalDate parseLocalDate(String scheduleDate) {
        if (StringUtils.isEmpty(scheduleDate)) {
            return java.time.LocalDate.now();
        }
        try {
            return java.time.LocalDate.parse(scheduleDate);
        } catch (Exception e) {
            log.warn("[S3] 排程日期格式异常，回退到当前日期: scheduleDate={}", scheduleDate, e);
            return java.time.LocalDate.now();
        }
    }

    /**
     * 通过任务链节点反查排产记录（用于获取14点预计库存等原始数据）。
     */
    private TqScheduleResultVo findScheduleVo(TqTaskNode node, TqScheduleContext context) {
        if (node.getScheduleId() != null) {
            return context.getScheduleList().stream()
                    .filter(s -> s.getId() != null && s.getId().equals(node.getScheduleId()))
                    .findFirst().orElse(null);
        }
        // 兜底：按beadCode+machineCode匹配
        return context.getScheduleList().stream()
                .filter(s -> s.getBeadCode().equals(node.getBeadCode())
                        && node.getMachineCode().equals(s.getMachineCode()))
                .findFirst().orElse(null);
    }

    /**
     * 获取成型消耗量（基于任务链节点反查排产记录）。
     */
    private double getCxConsumeByNode(TqTaskNode node, double coefficient, TqScheduleContext context) {
        TqScheduleResultVo scheduleVo = findScheduleVo(node, context);
        if (scheduleVo == null) {
            return 0D;
        }
        return getCxConsume(scheduleVo, node.getClassIndex(), coefficient);
    }

    /**
     * 刷新任务链（S4/S5调整计划量后调用）。
     *
     * <p>从指定机台指定班次开始，重新计算库存、消耗量、保证班数等。</p>
     *
     * @param context 排程上下文
     * @param machineCode 需要刷新的机台编号，null表示刷新所有机台
     * @param fromClassIdx 起始班次索引（1~6），从该班次开始重算
     */
    public void refreshTaskChain(TqScheduleContext context, String machineCode, int fromClassIdx) {
        Map<String, LinkedList<TqTaskNode>> taskChainMap = context.getTaskChainMap();
        if (taskChainMap == null || taskChainMap.isEmpty()) {
            return;
        }

        double coefficient = context.getParams().getDemandCoefficient() == null ? 2D : context.getParams().getDemandCoefficient();

        // 确定需要刷新的机台列表
        List<String> machineCodes = machineCode != null
                ? Collections.singletonList(machineCode)
                : new ArrayList<>(taskChainMap.keySet());

        for (String mid : machineCodes) {
            LinkedList<TqTaskNode> chain = taskChainMap.get(mid);
            if (chain == null || chain.isEmpty()) {
                continue;
            }

            boolean needRecalc = false;
            for (TqTaskNode node : chain) {
                if (node.getClassIndex() >= fromClassIdx) {
                    needRecalc = true;
                }
                if (!needRecalc) {
                    continue;
                }

                // 重新从排产记录同步planQty（S4/S5可能修改了）
                TqScheduleResultVo scheduleVo = findScheduleVo(node, context);
                if (scheduleVo != null) {
                    node.setPlanQty(getClassPlanQty(scheduleVo, node.getClassIndex()));
                }

                // 重算库存
                int nodeIdx = chain.indexOf(node);
                if (nodeIdx == 0) {
                    double startStock = scheduleVo == null ? 0D
                            : (scheduleVo.getPlanStockQty() == null ? 0D : scheduleVo.getPlanStockQty());
                    node.setStartStockQty(startStock);
                } else {
                    node.setStartStockQty(chain.get(nodeIdx - 1).getEndStockQty());
                }

                // 重算消耗量和结束库存
                double cxConsume = getCxConsumeByNode(node, coefficient, context);
                node.setCxConsumeQty(cxConsume);
                double endStock = BigDecimalUtil.add(BigDecimalUtil.sub(node.getStartStockQty(), cxConsume), node.getPlanQty());
                node.setEndStockQty(endStock);

                // 重算保证班数
                double guaranteeShifts = endStock > 0 && cxConsume > 0
                        ? BigDecimalUtil.div(endStock, cxConsume, 1) : 999;
                node.setGuaranteeShifts(guaranteeShifts);

                // 重算切换时长
                if (nodeIdx > 0) {
                    TqTaskNode prevNode = chain.get(nodeIdx - 1);
                    if (!prevNode.getBeadCode().equals(node.getBeadCode())) {
                        double specSwitchTime = context.getParams().getSpecSwitchTime() == null ? 0.5D
                                : context.getParams().getSpecSwitchTime();
                        double inchSwitchTime = context.getParams().getInchSwitchTime() == null ? 1D
                                : context.getParams().getInchSwitchTime();
                        boolean sameClass = prevNode.getClassIndex() == node.getClassIndex();
                        node.setSwitchTime(sameClass ? specSwitchTime : inchSwitchTime);
                    } else {
                        node.setSwitchTime(0);
                    }
                }

                // 重算有效生产时长
                double shiftHours = 8D;
                node.setEffectiveHours(BigDecimalUtil.sub(shiftHours, node.getSwitchTime()));
            }
        }

        log.info("[任务链刷新] 机台:{}, 起始班次:{}", machineCode, fromClassIdx);
    }

    /**
     * 获取成型消耗量（胎圈消耗 = 成型计划 × 系数）
     */
    private double getCxConsume(TqScheduleResultVo scheduleVo, int classIdx, double coefficient) {
        double cxPlan;
        switch (classIdx) {
            case 1: cxPlan = scheduleVo.getCxClass3Plan() == null ? 0 : scheduleVo.getCxClass3Plan(); break;
            case 2: cxPlan = scheduleVo.getCxClass4Plan() == null ? 0 : scheduleVo.getCxClass4Plan(); break;
            case 3: cxPlan = scheduleVo.getCxClass5Plan() == null ? 0 : scheduleVo.getCxClass5Plan(); break;
            case 4: cxPlan = scheduleVo.getCxClass6Plan() == null ? 0 : scheduleVo.getCxClass6Plan(); break;
            case 5: cxPlan = scheduleVo.getCxClass7Plan() == null ? 0 : scheduleVo.getCxClass7Plan(); break;
            case 6: cxPlan = scheduleVo.getCxClass8Plan() == null ? 0 : scheduleVo.getCxClass8Plan(); break;
            default: cxPlan = 0D;
        }
        return BigDecimalUtil.mul(cxPlan, coefficient);
    }

    /**
     * 获取指定班次的生产顺序
     */
    private int getClassProduceOrder(TqScheduleResultVo scheduleVo, int classNum) {
        switch (classNum) {
            case 1: return scheduleVo.getClass1ProduceOrder() == null ? 0 : scheduleVo.getClass1ProduceOrder();
            case 2: return scheduleVo.getClass2ProduceOrder() == null ? 0 : scheduleVo.getClass2ProduceOrder();
            case 3: return scheduleVo.getClass3ProduceOrder() == null ? 0 : scheduleVo.getClass3ProduceOrder();
            case 4: return scheduleVo.getClass4ProduceOrder() == null ? 0 : scheduleVo.getClass4ProduceOrder();
            case 5: return scheduleVo.getClass5ProduceOrder() == null ? 0 : scheduleVo.getClass5ProduceOrder();
            case 6: return scheduleVo.getClass6ProduceOrder() == null ? 0 : scheduleVo.getClass6ProduceOrder();
            default: return 0;
        }
    }
}
