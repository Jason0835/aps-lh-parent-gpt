package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
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
 *   <li>3步排产策略：①当前班当前机台 → ②当前班切换机台 → ③延至下一班次</li>
 *   <li>机台定额约束：单机台单班产量不超过该机台定额</li>
 *   <li>设置6个班次的生产顺序</li>
 *   <li>构建任务链</li>
 * </ol>
 *
 * <p>3步排产策略说明：</p>
 * <ul>
 *   <li>步骤1：当前班次，当前已分配机台，若定额有余量则排产，超出部分进入步骤2</li>
 *   <li>步骤2：当前班次，搜索其他可用机台切换排产，超出部分进入步骤3</li>
 *   <li>步骤3：超出定额的需求量延后至下一班次累加，不再前移</li>
 * </ul>
 */
@Slf4j
@Component
public class TqMachineAssignHandler extends AbsTqScheduleStepHandler {

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

        // 按优先级排序：定点机台优先 → 已排产规格优先 → 计划量从大到小
        List<TqScheduleResultVo> sortedScheduleList = scheduleList.stream().sorted((o1, o2) -> {
            Integer flag1 = context.getSpecifyCanMachineMap().containsKey(o1.getBeadCode()) ? 1 : 2;
            Integer flag2 = context.getSpecifyCanMachineMap().containsKey(o2.getBeadCode()) ? 1 : 2;
            int result = flag1.compareTo(flag2);
            if (result != 0) {
                return result;
            }
            Integer isPlanned1 = plannedMachineMap.containsKey(o1.getBeadCode()) ? 1 : 2;
            Integer isPlanned2 = plannedMachineMap.containsKey(o2.getBeadCode()) ? 1 : 2;
            result = isPlanned1.compareTo(isPlanned2);
            if (result != 0) {
                return result;
            }
            // 按1~6班总计划量从大到小排序
            double totalPlan1 = getTotalPlanQty(o1);
            double totalPlan2 = getTotalPlanQty(o2);
            return Double.compare(totalPlan2, totalPlan1);
        }).collect(Collectors.toList());

        // 逐班分配机台（1班→2班→3班→4班→5班→6班）
        // 班次对应的OpenMachineClass索引（D=排程日期-2，即今天）：
        // 1班=D日中班→CLASS_FOUR(中班), 2班=D+1日夜班→CLASS_TWO(夜班), 3班=D+1日早班→CLASS_THREE(早班)
        // 4班=D+1日中班→CLASS_FOUR(中班), 5班=D+2日夜班→CLASS_TWO(夜班), 6班=D+2日早班→CLASS_THREE(早班)
        int[] classIndexes = {
                OpenMachineClassEnums.CLASS_FOUR.getClassIndex(),   // 1班=D日中班
                OpenMachineClassEnums.CLASS_TWO.getClassIndex(),    // 2班=D+1日夜班
                OpenMachineClassEnums.CLASS_THREE.getClassIndex(),  // 3班=D+1日早班
                OpenMachineClassEnums.CLASS_FOUR.getClassIndex(),   // 4班=D+1日中班
                OpenMachineClassEnums.CLASS_TWO.getClassIndex(),    // 5班=D+2日夜班
                OpenMachineClassEnums.CLASS_THREE.getClassIndex()   // 6班=D+2日早班
        };

        Map<String, BigDecimal>[] capacityMaps = new Map[]{
                class1CapacityMap, class2CapacityMap, class3CapacityMap,
                class4CapacityMap, class5CapacityMap, class6CapacityMap
        };

        for (int classIdx = 0; classIdx < 6; classIdx++) {
            String classCode = String.valueOf(classIndexes[classIdx]);

            // 阈值切换生产：供应时长未达阈值的规格优先排产，已达阈值的规格排后面
            double supplyTimeThreshold = context.getParams().getSupplyTimeThreshold() == null ? 24D
                    : context.getParams().getSupplyTimeThreshold();

            // 按供应时长阈值排序：未达阈值优先，已达阈值排后
            List<TqScheduleResultVo> classSortedScheduleList = sortedScheduleList.stream()
                    .sorted((o1, o2) -> {
                        double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
                        double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
                        boolean above1 = st1 >= supplyTimeThreshold;
                        boolean above2 = st2 >= supplyTimeThreshold;
                        // 未达阈值排前面
                        if (above1 != above2) {
                            return above1 ? 1 : -1;
                        }
                        // 同组内按供应时长升序
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

                // ========== 3步排产策略 ==========

                // 步骤1：当前班次，当前已分配机台
                if (StringUtils.isNotEmpty(scheduleVo.getMachineCode())) {
                    String machineCode = scheduleVo.getMachineCode();
                    TqMachineInfo existingMachine = allMachineList.stream()
                            .filter(m -> m.getMachineCode().equals(machineCode))
                            .findFirst().orElse(null);

                    // 机台不支持当前班次，延后到下一班
                    if (existingMachine != null && !existingMachine.getOpenMachineClass().contains(classCode)) {
                        deferToNextClass(scheduleVo, classIdx + 1, planQty);
                        continue;
                    }

                    // 获取机台定额
                    double machineQuota = getMachineQuota(existingMachine, defaultQuota);

                    // 定额检查：已排产能 + 当前计划量不能超过定额
                    BigDecimal currentCapacity = capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO);
                    double remainingCapacity = BigDecimalUtil.sub(machineQuota, currentCapacity.doubleValue());

                    if (remainingCapacity <= 0) {
                        // 步骤1失败：已无剩余定额，进入步骤2
                        double overflowQty = planQty;
                        setClassPlanQty(scheduleVo, classIdx + 1, 0D);

                        // 步骤2：当前班次，搜索其他可用机台切换
                        boolean step2Success = trySwitchMachine(scheduleVo, classIdx, classCode, overflowQty,
                                capacityMaps, allMachineList, context, sortedStrategies, plannedMachineMap,
                                glueMap, mouthPlatMap, defaultQuota);

                        if (!step2Success) {
                            // 步骤3：延后至下一班次
                            deferToNextClass(scheduleVo, classIdx + 1, overflowQty);
                        }
                    } else if (planQty > remainingCapacity) {
                        // 部分可排：截断到剩余定额，超出部分进入步骤2/3
                        setClassPlanQty(scheduleVo, classIdx + 1, remainingCapacity);
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(remainingCapacity)));

                        double overflowQty = BigDecimalUtil.sub(planQty, remainingCapacity);

                        // 步骤2：搜索其他可用机台
                        boolean step2Success = trySwitchMachine(scheduleVo, classIdx, classCode, overflowQty,
                                capacityMaps, allMachineList, context, sortedStrategies, plannedMachineMap,
                                glueMap, mouthPlatMap, defaultQuota);

                        if (!step2Success) {
                            // 步骤3：延后至下一班次
                            deferToNextClass(scheduleVo, classIdx + 1, overflowQty);
                        }
                    } else {
                        // 全部可排
                        capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(planQty)));
                    }
                    continue;
                }

                // 未分配机台，搜索可用机台
                List<TqMachineInfo> optionalMachineList = searchOptionalMachineList(
                        scheduleVo, classCode, capacityMaps[classIdx], allMachineList, context, sortedStrategies, plannedMachineMap);
                if (CollectionUtil.isEmpty(optionalMachineList)) {
                    // 无可用机台，延后至下一班次（步骤3）
                    deferToNextClass(scheduleVo, classIdx + 1, planQty);
                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "无可用机台-延后至下一班", "胎圈代码：" + scheduleVo.getBeadCode()
                                    + "，" + (classIdx + 1) + "班计划量" + planQty + "延后");
                    continue;
                }
                scheduleVo.setUnscheduledFlag("0");

                TqMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
                String machineCode = machine.getMachineCode();
                scheduleVo.setMachineCode(machineCode);

                // 获取机台定额
                double machineQuota = getMachineQuota(machine, defaultQuota);

                // 定额约束检查
                BigDecimal currentCapacity = capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO);
                double remainingCapacity = BigDecimalUtil.sub(machineQuota, currentCapacity.doubleValue());

                if (planQty > remainingCapacity && remainingCapacity > 0) {
                    // 部分可排
                    setClassPlanQty(scheduleVo, classIdx + 1, remainingCapacity);
                    capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                            .add(BigDecimalUtils.valueOf(remainingCapacity)));

                    double overflowQty = BigDecimalUtil.sub(planQty, remainingCapacity);
                    // 超出部分延后至下一班次
                    deferToNextClass(scheduleVo, classIdx + 1, overflowQty);
                } else if (remainingCapacity <= 0) {
                    // 无剩余定额，全部延后
                    setClassPlanQty(scheduleVo, classIdx + 1, 0D);
                    deferToNextClass(scheduleVo, classIdx + 1, planQty);
                } else {
                    // 全部可排
                    capacityMaps[classIdx].put(machineCode, capacityMaps[classIdx].getOrDefault(machineCode, BigDecimal.ZERO)
                            .add(BigDecimalUtils.valueOf(planQty)));
                }

                // 占用机台其他班产能
                for (int i = 0; i < 6; i++) {
                    if (i == classIdx) continue; // 当前班已处理
                    double classPlan = getClassPlanQty(scheduleVo, i + 1);
                    if (classPlan > 0) {
                        capacityMaps[i].put(machineCode, capacityMaps[i].getOrDefault(machineCode, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(classPlan)));
                    }
                }
                plannedMachineMap.put(scheduleVo.getBeadCode(), machineCode);
                putMachineCode(scheduleVo.getGlueCode(), machineCode, glueMap);
                putMachineCode(scheduleVo.getMouthPlateCode(), machineCode, mouthPlatMap);

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
     * 步骤2：尝试切换到其他可用机台排产
     *
     * @return true=切换成功，false=无可用机台
     */
    private boolean trySwitchMachine(TqScheduleResultVo scheduleVo, int classIdx, String classCode,
                                     double overflowQty, Map<String, BigDecimal>[] capacityMaps,
                                     List<TqMachineInfo> allMachineList, TqScheduleContext context,
                                     List<IMachineFilterStrategy> sortedStrategies,
                                     Map<String, String> plannedMachineMap,
                                     Map<String, List<String>> glueMap, Map<String, List<String>> mouthPlatMap,
                                     double defaultQuota) {
        // 搜索其他可用机台（排除当前已分配的机台）
        String currentMachineCode = scheduleVo.getMachineCode();
        List<TqMachineInfo> optionalMachineList = searchOptionalMachineList(
                scheduleVo, classCode, capacityMaps[classIdx], allMachineList, context, sortedStrategies, plannedMachineMap);

        // 排除当前机台
        if (StringUtils.isNotEmpty(currentMachineCode)) {
            optionalMachineList = optionalMachineList.stream()
                    .filter(m -> !m.getMachineCode().equals(currentMachineCode))
                    .collect(Collectors.toList());
        }

        if (CollectionUtil.isEmpty(optionalMachineList)) {
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "步骤2-切换机台失败", "胎圈代码：" + scheduleVo.getBeadCode()
                            + "，无其他可用机台，" + overflowQty + "将延后至下一班");
            return false;
        }

        // 选择定额余量最大的机台
        TqMachineInfo switchMachine = optionalMachineList.stream()
                .max(Comparator.comparingDouble(m -> {
                    double quota = getMachineQuota(m, defaultQuota);
                    double used = capacityMaps[classIdx].getOrDefault(m.getMachineCode(), BigDecimal.ZERO).doubleValue();
                    return BigDecimalUtil.sub(quota, used);
                })).orElse(null);

        if (switchMachine == null) {
            return false;
        }

        double switchMachineQuota = getMachineQuota(switchMachine, defaultQuota);
        BigDecimal switchCapacity = capacityMaps[classIdx].getOrDefault(switchMachine.getMachineCode(), BigDecimal.ZERO);
        double switchRemaining = BigDecimalUtil.sub(switchMachineQuota, switchCapacity.doubleValue());

        if (switchRemaining <= 0) {
            return false;
        }

        // 在切换机台上排产
        double assignQty = Math.min(overflowQty, switchRemaining);
        // 将部分计划量分配给切换机台（记录在scheduleVo的辅助字段或日志中）
        capacityMaps[classIdx].put(switchMachine.getMachineCode(), capacityMaps[classIdx].getOrDefault(switchMachine.getMachineCode(), BigDecimal.ZERO)
                .add(BigDecimalUtils.valueOf(assignQty)));

        double stillOverflow = BigDecimalUtil.sub(overflowQty, assignQty);
        if (stillOverflow > 0) {
            // 仍有超出部分，延后至下一班次
            deferToNextClass(scheduleVo, classIdx + 1, stillOverflow);
        }

        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "步骤2-切换机台成功", "胎圈代码：" + scheduleVo.getBeadCode()
                        + "，切换到机台" + switchMachine.getMachineCode() + "，排产量" + assignQty
                        + (stillOverflow > 0 ? "，仍有" + stillOverflow + "延后" : ""));
        return true;
    }

    /**
     * 步骤3：将超出定额的计划量延后至下一班次累加
     *
     * <p>不再前移，而是延后到下一个班次。6班已是最后一班时，记录溢出日志。</p>
     */
    private void deferToNextClass(TqScheduleResultVo scheduleVo, int currentClass, double overflowQty) {
        if (currentClass >= 6) {
            // 已是最后一班，无法延后，记录溢出
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "步骤3-延后失败(已是最后一班)", "胎圈代码：" + scheduleVo.getBeadCode()
                            + "，" + currentClass + "班溢出量" + overflowQty + "无法延后");
            log.warn("[S3] 胎圈{}的6班溢出量{}无法延后", scheduleVo.getBeadCode(), overflowQty);
            return;
        }
        // 延后至下一班次累加
        double nextClassQty = getClassPlanQty(scheduleVo, currentClass + 1);
        setClassPlanQty(scheduleVo, currentClass + 1, BigDecimalUtil.add(nextClassQty, overflowQty));
        // 当前班次减去溢出量
        double currentQty = getClassPlanQty(scheduleVo, currentClass);
        setClassPlanQty(scheduleVo, currentClass, BigDecimalUtil.sub(currentQty, overflowQty));

        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                "步骤3-延后至下一班", "胎圈代码：" + scheduleVo.getBeadCode()
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
        int[] produceOrders = new int[6]; // 6个班次各自的生产顺序计数器
        Arrays.fill(produceOrders, 1);

        for (int classIdx = 0; classIdx < 6; classIdx++) {
            final int ci = classIdx;
            // 排序：1.相同英寸连续 2.同英寸内按供应时长升序
            List<TqScheduleResultVo> sortedList = scheduleList.stream()
                    .filter(s -> getClassPlanQty(s, ci + 1) > 0)
                    .sorted(Comparator
                            .comparing((TqScheduleResultVo s) -> s.getDimension() == null ? BigDecimal.ZERO : s.getDimension())
                            .thenComparing(TqScheduleResultVo::getSupplyTime))
                    .collect(Collectors.toList());

            for (TqScheduleResultVo scheduleVo : sortedList) {
                setClassProduceOrder(scheduleVo, ci + 1, produceOrders[ci]++);

                autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                        "设置" + (ci + 1) + "班生产顺序",
                        "相同英寸连续生产规则，寸口=" + scheduleVo.getDimension()
                                + "，供应时长=" + scheduleVo.getSupplyTime()
                                + "，生产顺序=" + (produceOrders[ci] - 1));
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
            case 1: cxPlan = scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan(); break;
            case 2: cxPlan = scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan(); break;
            case 3: cxPlan = scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan(); break;
            case 4: cxPlan = scheduleVo.getCxClass6Plan() == null ? 0D : scheduleVo.getCxClass6Plan(); break;
            case 5: cxPlan = scheduleVo.getCxClass7Plan() == null ? 0D : scheduleVo.getCxClass7Plan(); break;
            case 6: cxPlan = scheduleVo.getCxClass8Plan() == null ? 0D : scheduleVo.getCxClass8Plan(); break;
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
