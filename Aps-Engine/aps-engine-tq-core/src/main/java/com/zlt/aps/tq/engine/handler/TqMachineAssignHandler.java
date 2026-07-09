package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.IMachineFilterStrategy;
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

        // 3. 设置6个班次的生产顺序
        setProduceOrder(context.getScheduleList());

        // 4. 构建任务链
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

        // ========== 预扫描：建立机台→规格数映射 ==========
        // 业务规则：一个规格只在指定机台生产，但一个机台可生产多个规格
        // 通过预扫描每个规格的候选机台，统计每台机台对应多少个规格
        // 用于 S3 分配时判断"单一规格机台"（只受quota限制）vs"多规格机台"（备库胎圈受阈值限制）
        Map<String, Integer> machineSpecCountMap = preScanMachineSpecCount(scheduleList, allMachineList,
                context, sortedStrategies);

        // 机台产能占用追踪（6个班次）
        Map<String, BigDecimal> class1CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class2CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class3CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class4CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class5CapacityMap = new HashMap<>();
        Map<String, BigDecimal> class6CapacityMap = new HashMap<>();
        Map<String, List<String>> glueMap = new HashMap<>();
        Map<String, List<String>> mouthPlatMap = new HashMap<>();
        Map<String, String> plannedMachineMap = new HashMap<>();

        // 按优先级排序：定点机台优先 → 计划量从大到小
        // 注：plannedMachineMap 在此处为空（尚未分配），原"已排产规格优先"条件失效，
        //     已移至班次内三级优先级排序中动态判断（见 classSortedScheduleList）
        List<TqScheduleResultVo> sortedScheduleList = scheduleList.stream().sorted((o1, o2) -> {
            Integer flag1 = context.getSpecifyCanMachineMap().containsKey(o1.getBeadCode()) ? 1 : 2;
            Integer flag2 = context.getSpecifyCanMachineMap().containsKey(o2.getBeadCode()) ? 1 : 2;
            int result = flag1.compareTo(flag2);
            if (result != 0) {
                return result;
            }
            // 按1~6班总计划量从大到小排序
            double totalPlan1 = getTotalPlanQty(o1);
            double totalPlan2 = getTotalPlanQty(o2);
            return Double.compare(totalPlan2, totalPlan1);
        }).collect(Collectors.toList());

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
                        // 已分配机台的规格优先处理（避免机台分配不稳定）
                        boolean planned1 = plannedMachineMap.containsKey(o1.getBeadCode());
                        boolean planned2 = plannedMachineMap.containsKey(o2.getBeadCode());
                        if (planned1 != planned2) {
                            return planned1 ? -1 : 1;
                        }

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

                        // 同为备库触发：按剩余需求缺口从大到小排序（缺口越大越优先）
                        if (backup1 && backup2) {
                            double rem1 = o1.getBackupRemainingQty() == null ? 0D : o1.getBackupRemainingQty();
                            double rem2 = o2.getBackupRemainingQty() == null ? 0D : o2.getBackupRemainingQty();
                            if (rem1 != rem2) {
                                return Double.compare(rem2, rem1);  // 降序
                            }
                        }

                        // P-3: 非备库规格按供应时长升序
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
                double defaultQuota = context.getParams().getMaxClassOutput() == null ? 3000D
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
                            machineSpecCountMap, machineQuota, backupThreshold);
                    // 当班实际可排上限 = min(机台剩余产能, 备库初始排产上限)
                    double effectiveCapacity = Math.min(remainingCapacity, initAssignLimit);

                    // 判断是否为备库胎圈多规格机台场景
                    boolean isBackupSpec = scheduleVo.getBackupTriggerClass() != null
                            && scheduleVo.getBackupTriggerClass() > 0;
                    boolean isMultiSpecMachine = !isSingleSpecMachine(machineCode, machineSpecCountMap);

                    if (isBackupSpec && isMultiSpecMachine) {
                        // 备库胎圈多规格机台：只排 min(计划量, 阈值, 剩余产能)，不延后
                        // 未排量累加到 backupRemainingQty，由 S3.5 按优先级回填
                        double assignQty = Math.min(planQty, effectiveCapacity);
                        if (assignQty < 0) assignQty = 0;
                        setClassPlanQty(scheduleVo, classIdx + 1, assignQty);
                        if (assignQty > 0) {
                            capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                    .add(BigDecimalUtils.valueOf(assignQty)));
                        }
                        // 未排量累加到 backupRemainingQty（供 S3.5 回填使用）
                        double unplanQty = BigDecimalUtil.sub(planQty, assignQty);
                        if (unplanQty > 0) {
                            double current = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                            scheduleVo.setBackupRemainingQty(BigDecimalUtil.add(current, unplanQty));
                            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                    "备库胎圈阈值限制-未排量累计", "胎圈代码：" + scheduleVo.getBeadCode()
                                            + "，" + (classIdx + 1) + "班排" + assignQty
                                            + "，未排量" + unplanQty + "累计到backupRemainingQty");
                        }
                    } else if (effectiveCapacity <= 0) {
                        // 非备库或单规格机台：当前机台本班已排满，全部计划量延后到下一班
                        setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                        deferToNextClass(scheduleVo, classIdx + 1, planQty);
                    } else if (planQty > effectiveCapacity) {
                        // 部分可排：当班先排满有效产能，超出部分延后到下一班
                        setClassPlanQty(scheduleVo, classIdx + 1, effectiveCapacity);
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(effectiveCapacity)));

                        double overflowQty = BigDecimalUtil.sub(planQty, effectiveCapacity);
                        deferToNextClass(scheduleVo, classIdx + 1, overflowQty);

                        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                "当班满排-剩余延后", "胎圈代码：" + scheduleVo.getBeadCode()
                                        + "，" + (classIdx + 1) + "班排满" + effectiveCapacity
                                        + "，剩余" + overflowQty + "延后至下一班");
                    } else {
                        // 全部可排
                        setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(planQty)));
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

                // 占用机台其他班产能（避免其他规格在同一机台其他班次超定额）
                for (int i = 0; i < 6; i++) {
                    if (i == classIdx) continue;
                    double classPlan = getClassPlanQty(scheduleVo, i + 1);
                    if (classPlan > 0) {
                        capacityMaps[i].put(machineCode, capacityMaps[i].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(classPlan)));
                    }
                }

                // 备库胎圈多规格阈值限制（同已分配机台分支逻辑）
                double backupThreshold = context.getParams().getBackupShiftThreshold() == null ? 1000D
                        : context.getParams().getBackupShiftThreshold();
                double initAssignLimit = getBackupInitAssignLimit(scheduleVo, machineCode,
                        machineSpecCountMap, machineQuota, backupThreshold);
                double effectiveCapacity = Math.min(remainingCapacity, initAssignLimit);

                // 判断是否为备库胎圈多规格机台场景（同已分配机台分支）
                boolean isBackupSpec = scheduleVo.getBackupTriggerClass() != null
                        && scheduleVo.getBackupTriggerClass() > 0;
                boolean isMultiSpecMachine = !isSingleSpecMachine(machineCode, machineSpecCountMap);

                if (isBackupSpec && isMultiSpecMachine) {
                    // 备库胎圈多规格机台：只排 min(计划量, 阈值, 剩余产能)，不延后
                    double assignQty = Math.min(planQty, effectiveCapacity);
                    if (assignQty < 0) assignQty = 0;
                    setClassPlanQty(scheduleVo, classIdx + 1, assignQty);
                    if (assignQty > 0) {
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(assignQty)));
                    }
                    // 未排量累加到 backupRemainingQty（供 S3.5 回填使用）
                    double unplanQty = BigDecimalUtil.sub(planQty, assignQty);
                    if (unplanQty > 0) {
                        double current = scheduleVo.getBackupRemainingQty() == null ? 0D : scheduleVo.getBackupRemainingQty();
                        scheduleVo.setBackupRemainingQty(BigDecimalUtil.add(current, unplanQty));
                        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                "备库胎圈阈值限制-未排量累计", "胎圈代码：" + scheduleVo.getBeadCode()
                                        + "，" + (classIdx + 1) + "班排" + assignQty
                                        + "，未排量" + unplanQty + "累计到backupRemainingQty");
                    }
                } else if (effectiveCapacity <= 0) {
                    // 非备库或单规格机台：机台本班已排满，全部计划量延后到下一班
                    setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                    deferToNextClass(scheduleVo, classIdx + 1, planQty);
                } else if (planQty > effectiveCapacity) {
                    // 部分可排：当班先排满有效产能，超出部分延后到下一班
                    setClassPlanQty(scheduleVo, classIdx + 1, effectiveCapacity);
                    capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                            .add(BigDecimalUtils.valueOf(effectiveCapacity)));

                    double overflowQty = BigDecimalUtil.sub(planQty, effectiveCapacity);
                    deferToNextClass(scheduleVo, classIdx + 1, overflowQty);

                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "当班满排-剩余延后", "胎圈代码：" + scheduleVo.getBeadCode()
                                    + "，" + (classIdx + 1) + "班排满" + effectiveCapacity
                                    + "，剩余" + overflowQty + "延后至下一班");
                } else {
                    // 全部可排
                    setClassPlanQty(scheduleVo, classIdx + 1, planQty);
                    capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                            .add(BigDecimalUtils.valueOf(planQty)));
                }

                chooseMachineLog(scheduleVo, context);
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
     * 预扫描所有规格的候选机台，统计每台机台对应多少个规格。
     *
     * <p>业务规则：一个规格只在指定机台生产，但一个机台可生产多个规格。
     * 通过预扫描建立 machineCode → specCount 映射，用于判断"单一规格机台"vs"多规格机台"。</p>
     *
     * @param scheduleList 排程结果列表
     * @param allMachineList 所有机台列表
     * @param context 排程上下文
     * @param sortedStrategies 排序后的机台过滤策略链
     * @return machineCode → specCount 映射
     */
    private Map<String, Integer> preScanMachineSpecCount(List<TqScheduleResultVo> scheduleList,
                                                         List<TqMachineInfo> allMachineList,
                                                         TqScheduleContext context,
                                                         List<IMachineFilterStrategy> sortedStrategies) {
        Map<String, Integer> machineSpecCountMap = new HashMap<>();
        // 使用第一班次作为预扫描班次，空产能Map
        String firstClassCode = SHIFT_CLASS_MAP[0];
        Map<String, BigDecimal> emptyCapacityMap = new HashMap<>();
        Map<String, String> emptyPlannedMap = new HashMap<>();

        for (TqScheduleResultVo scheduleVo : scheduleList) {
            List<TqMachineInfo> candidates = searchOptionalMachineList(
                    scheduleVo, firstClassCode, emptyCapacityMap, allMachineList, context, sortedStrategies, emptyPlannedMap);
            if (!CollectionUtil.isEmpty(candidates)) {
                String machineCode = candidates.get(0).getMachineCode();
                machineSpecCountMap.merge(machineCode, 1, Integer::sum);
            }
        }
        return machineSpecCountMap;
    }

    /**
     * 判断机台是否为单一规格机台（只生产1个规格）。
     *
     * @param machineCode 机台编码
     * @param machineSpecCountMap 机台→规格数映射
     * @return true=单一规格机台，false=多规格机台
     */
    private boolean isSingleSpecMachine(String machineCode, Map<String, Integer> machineSpecCountMap) {
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
     * @param machineSpecCountMap 机台→规格数映射
     * @param machineQuota 机台定额
     * @param backupShiftThreshold 备库班次阈值（SYS1101029）
     * @return 当班初始排产上限
     */
    private double getBackupInitAssignLimit(TqScheduleResultVo scheduleVo, String machineCode,
                                            Map<String, Integer> machineSpecCountMap,
                                            double machineQuota, double backupShiftThreshold) {
        boolean isBackupSpec = scheduleVo.getBackupTriggerClass() != null
                && scheduleVo.getBackupTriggerClass() > 0;
        if (!isBackupSpec) {
            // 非备库胎圈：不受阈值限制
            return machineQuota;
        }
        if (isSingleSpecMachine(machineCode, machineSpecCountMap)) {
            // 单一规格机台：备库胎圈只受机台定额限制，可满排
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
     */
    private List<TqMachineInfo> searchOptionalMachineList(TqScheduleResultVo scheduleVo, String classCode,
                                                          Map<String, BigDecimal> capacityMap,
                                                          List<TqMachineInfo> allMachineList,
                                                          TqScheduleContext context,
                                                          List<IMachineFilterStrategy> sortedStrategies,
                                                          Map<String, String> plannedMachineMap) {
        // 1. 通过策略链过滤
        List<TqMachineInfo> filtered = new ArrayList<>(allMachineList);
        for (IMachineFilterStrategy strategy : sortedStrategies) {
            filtered = strategy.filter(filtered, scheduleVo, context);
            if (filtered.isEmpty()) {
                break;
            }
        }

        // 2. 过滤对应班次可用的机台
        filtered = filtered.stream()
                .filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode))
                .collect(Collectors.toList());

        // 3. 按优先级排序
        String beadCode = scheduleVo.getBeadCode();
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

        return filtered;
    }

    /**
     * 设置6个班次的生产顺序。
     *
     * <p>排序规则：1.相同英寸连续生产 2.同英寸内按库存供应时长升序排序</p>
     */
    private void setProduceOrder(List<TqScheduleResultVo> scheduleList) {
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
