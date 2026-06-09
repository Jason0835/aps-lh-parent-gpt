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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S3: 机台分配与排序Handler（6班次版本）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>按优先级排序排程记录（定点机台优先 → 已排产规格优先 → 计划量从大到小）</li>
 *   <li>通过策略链过滤候选机台（定点/口型板/寸口/维修）</li>
 *   <li>为每条排程记录分配机台（优先选择已分配产能较低的机台）</li>
 *   <li>处理机台班次不匹配时的计划量转移</li>
 *   <li>设置6个班次的生产顺序</li>
 * </ol>
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
        return "S3-机台分配与排序";
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

        log.info("[S3] 机台分配与排序完成");
    }

    /**
     * 生产线挑选（6班次版本）。
     *
     * <p>分配逻辑：</p>
     * <ol>
     *   <li>先按优先级排序排程记录</li>
     *   <li>遍历6个班次，逐班为排程记录分配机台</li>
     * </ol>
     */
    private void chooseMachine(TqScheduleContext context, List<IMachineFilterStrategy> sortedStrategies) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        List<TqMachineInfo> allMachineList = context.getAllMachineList();

        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }

        // 机台产能占用追踪（6个班次）
        Map<Long, BigDecimal> class1CapacityMap = new HashMap<>();
        Map<Long, BigDecimal> class2CapacityMap = new HashMap<>();
        Map<Long, BigDecimal> class3CapacityMap = new HashMap<>();
        Map<Long, BigDecimal> class4CapacityMap = new HashMap<>();
        Map<Long, BigDecimal> class5CapacityMap = new HashMap<>();
        Map<Long, BigDecimal> class6CapacityMap = new HashMap<>();
        Map<String, List<Long>> glueMap = new HashMap<>();
        Map<String, List<Long>> mouthPlatMap = new HashMap<>();
        Map<String, Long> plannedMachineMap = new HashMap<>();

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

        Map<Long, BigDecimal>[] capacityMaps = new Map[]{
                class1CapacityMap, class2CapacityMap, class3CapacityMap,
                class4CapacityMap, class5CapacityMap, class6CapacityMap
        };

        for (int classIdx = 0; classIdx < 6; classIdx++) {
            String classCode = String.valueOf(classIndexes[classIdx]);

            for (TqScheduleResultVo scheduleVo : sortedScheduleList) {
                double planQty = getClassPlanQty(scheduleVo, classIdx + 1);
                if (planQty <= 0) {
                    continue;
                }

                // 如果已分配机台，检查该机台是否支持当前班次
                if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                    Long machineId = Long.valueOf(scheduleVo.getMachineId());
                    TqMachineInfo existingMachine = allMachineList.stream()
                            .filter(m -> m.getId().equals(machineId))
                            .findFirst().orElse(null);
                    if (existingMachine != null && !existingMachine.getOpenMachineClass().contains(classCode)) {
                        // 机台不支持当前班次，将计划量转移到前一个班次
                        transferPlanToPrevClass(scheduleVo, classIdx + 1);
                    }
                    // 占用机台产能
                    capacityMaps[classIdx].put(machineId, capacityMaps[classIdx].getOrDefault(machineId, BigDecimal.ZERO)
                            .add(BigDecimalUtils.valueOf(planQty)));
                    continue;
                }

                // 未分配机台，搜索可用机台
                List<TqMachineInfo> optionalMachineList = searchOptionalMachineList(
                        scheduleVo, classCode, capacityMaps[classIdx], allMachineList, context, sortedStrategies, plannedMachineMap);
                if (CollectionUtil.isEmpty(optionalMachineList)) {
                    continue;
                }

                TqMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
                Long machineId = machine.getId();
                scheduleVo.setMachineId(String.valueOf(machineId));

                // 检查机台各班次支持情况，不支持的计划量转移
                checkAndTransferPlan(scheduleVo, machine, classIdx + 1);

                // 占用机台各班产能
                for (int i = 0; i < 6; i++) {
                    double classPlan = getClassPlanQty(scheduleVo, i + 1);
                    if (classPlan > 0) {
                        capacityMaps[i].put(machineId, capacityMaps[i].getOrDefault(machineId, BigDecimal.ZERO)
                                .add(BigDecimalUtils.valueOf(classPlan)));
                    }
                }
                plannedMachineMap.put(scheduleVo.getBeadCode(), machineId);
                putMachineId(scheduleVo.getGlueCode(), machineId, glueMap);
                putMachineId(scheduleVo.getMouthPlateCode(), machineId, mouthPlatMap);

                chooseMachineLog(scheduleVo, context);
            }
        }
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
     * 将不支持班次的计划量转移到前一个班次
     */
    private void transferPlanToPrevClass(TqScheduleResultVo scheduleVo, int classNum) {
        if (classNum <= 1) {
            return; // 1班没有前序班次可转移
        }
        double planQty = getClassPlanQty(scheduleVo, classNum);
        if (planQty > 0) {
            double prevPlanQty = getClassPlanQty(scheduleVo, classNum - 1);
            setClassPlanQty(scheduleVo, classNum - 1, BigDecimalUtil.add(prevPlanQty, planQty));
            setClassPlanQty(scheduleVo, classNum, 0D);
        }
    }

    /**
     * 检查机台各班次支持情况，不支持的计划量转移
     */
    private void checkAndTransferPlan(TqScheduleResultVo scheduleVo, TqMachineInfo machine, int currentClass) {
        int[] classIndexes = {
                OpenMachineClassEnums.CLASS_FOUR.getClassIndex(),   // 1班=D日中班
                OpenMachineClassEnums.CLASS_TWO.getClassIndex(),    // 2班=D+1日夜班
                OpenMachineClassEnums.CLASS_THREE.getClassIndex(),  // 3班=D+1日早班
                OpenMachineClassEnums.CLASS_FOUR.getClassIndex(),   // 4班=D+1日中班
                OpenMachineClassEnums.CLASS_TWO.getClassIndex(),    // 5班=D+2日夜班
                OpenMachineClassEnums.CLASS_THREE.getClassIndex()   // 6班=D+2日早班
        };

        for (int i = 1; i <= 6; i++) {
            double planQty = getClassPlanQty(scheduleVo, i);
            if (planQty <= 0) {
                continue;
            }
            String classCode = String.valueOf(classIndexes[i - 1]);
            if (!machine.getOpenMachineClass().contains(classCode)) {
                // 机台不支持该班次，将计划量转移到前一个班次
                transferPlanToPrevClass(scheduleVo, i);
            }
        }
    }

    /**
     * 检索符合条件的可选机台列表。
     */
    private List<TqMachineInfo> searchOptionalMachineList(TqScheduleResultVo scheduleVo, String classCode,
                                                          Map<Long, BigDecimal> capacityMap,
                                                          List<TqMachineInfo> allMachineList,
                                                          TqScheduleContext context,
                                                          List<IMachineFilterStrategy> sortedStrategies,
                                                          Map<String, Long> plannedMachineMap) {
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
            Long scheduleMachineId = plannedMachineMap.getOrDefault(beadCode, 0L);
            Integer hasMachine1 = m1.getId().equals(scheduleMachineId) ? 0 : 1;
            Integer hasMachine2 = m2.getId().equals(scheduleMachineId) ? 0 : 1;
            int result = hasMachine1.compareTo(hasMachine2);
            if (result != 0) {
                return result;
            }
            // 按剩余产能升序排序
            BigDecimal capacity1 = capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO);
            BigDecimal capacity2 = capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO);
            result = capacity1.compareTo(capacity2);
            if (result != 0) {
                return result;
            }
            return m1.getId().compareTo(m2.getId());
        }).collect(Collectors.toList());

        return filtered;
    }

    /**
     * 设置6个班次的生产顺序（根据库存供应时长升序排序，有计划量的才设置顺序）。
     */
    private void setProduceOrder(List<TqScheduleResultVo> scheduleList) {
        int[] produceOrders = new int[6]; // 6个班次各自的生产顺序计数器
        Arrays.fill(produceOrders, 1);

        // 根据库存供应时长升序排序
        List<TqScheduleResultVo> sortedList = scheduleList.stream()
                .sorted(Comparator.comparing(TqScheduleResultVo::getSupplyTime))
                .collect(Collectors.toList());

        for (TqScheduleResultVo scheduleVo : sortedList) {
            if (getClassPlanQty(scheduleVo, 1) > 0) {
                scheduleVo.setClass1ProduceOrder(produceOrders[0]++);
            }
            if (getClassPlanQty(scheduleVo, 2) > 0) {
                scheduleVo.setClass2ProduceOrder(produceOrders[1]++);
            }
            if (getClassPlanQty(scheduleVo, 3) > 0) {
                scheduleVo.setClass3ProduceOrder(produceOrders[2]++);
            }
            if (getClassPlanQty(scheduleVo, 4) > 0) {
                scheduleVo.setClass4ProduceOrder(produceOrders[3]++);
            }
            if (getClassPlanQty(scheduleVo, 5) > 0) {
                scheduleVo.setClass5ProduceOrder(produceOrders[4]++);
            }
            if (getClassPlanQty(scheduleVo, 6) > 0) {
                scheduleVo.setClass6ProduceOrder(produceOrders[5]++);
            }

            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "设置6班次生产顺序",
                    logSplit("根据库存供应时长(从小到大)，设置6个班次的生产顺序（有计划量的才设置生产顺序）",
                            "设置后的排程数据：" + toJSONString(scheduleVo)));
        }
    }

    /**
     * 将指定key分配给特定机台
     */
    private void putMachineId(String key, Long machineId, Map<String, List<Long>> machineMap) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        List<Long> machineList = machineMap.get(key);
        if (machineList == null) {
            machineList = new ArrayList<>();
            machineMap.put(key, machineList);
        }
        if (!machineList.contains(machineId)) {
            machineList.add(machineId);
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
}
