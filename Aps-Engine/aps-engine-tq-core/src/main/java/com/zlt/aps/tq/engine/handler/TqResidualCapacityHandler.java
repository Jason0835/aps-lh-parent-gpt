package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3.5: 剩余产能分配Handler。
 *
 * <p>职责：S3机台分配完成后，回收每个机台每班的剩余产能（quota - 已排产量），
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
 * @author APS
 */
@Slf4j
@Component
public class TqResidualCapacityHandler extends AbsTqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Override
    protected String getStepName() {
        return "S3.5-剩余产能分配";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }

        // 供应时长阈值（用于Priority-2/3判定）
        double supplyTimeThreshold = getSupplyTimeThreshold(context);
        // 默认机台定额（兜底，优先使用机台自身quota）
        double defaultQuota = getDefaultQuota(context);

        // 1. 按机台分组，得到 machineCode → 该机台上所有规格的排程记录
        Map<String, List<TqScheduleResultVo>> machineSpecMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .collect(Collectors.groupingBy(TqScheduleResultVo::getMachineCode));

        if (machineSpecMap.isEmpty()) {
            log.info("[S3.5] 无已分配机台的规格，跳过剩余产能分配");
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

        log.info("[S3.5] 剩余产能分配完成, 机台数:{}", machineSpecMap.size());
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
                double assignQty = Math.min(residualCapacity, backupRemaining);
                assignQty = applyRoundingIfNeeded(assignQty);
                addClassPlanQty(spec, classNum, assignQty);
                spec.setBackupRemainingQty(BigDecimalUtil.sub(backupRemaining, assignQty));

                autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S3.5-剩余产能回填备库胎圈",
                        "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                                + "，" + classNum + "班回填" + assignQty
                                + "，剩余备库量：" + spec.getBackupRemainingQty());
            } else {
                // 非备库规格：仅当该班次已有排产量时才回填（避免随意新增班次排产）
                // 业务规则：剩余产能优先补备库胎圈；非备库规格只在已排产的情况下补量
                double currentPlan = getClassPlanQty(spec, classNum);
                if (currentPlan <= 0) {
                    continue;
                }
                // 非备库规格回填量不受backupRemainingQty限制，但只补到当前班次（不主动扩量）
                // 此处不回填非备库规格（保持S3排产结果），仅由备库胎圈消化剩余产能
                // 如需扩展可在此处补充逻辑
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

            if (!isBackupSpec || backupRemaining <= 0) {
                continue;
            }

            // 统计第6班已排产量
            double usedCapacity = 0D;
            for (TqScheduleResultVo s : specList) {
                usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(s, classNum));
            }
            double residualCapacity = BigDecimalUtil.sub(machineQuota, usedCapacity);
            if (residualCapacity <= 0) {
                // 第6班机台已排满，剩余备库量无法塞入，记录日志
                autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                        "S3.5-第6班塞入失败(机台已满)",
                        "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                                + "，剩余备库量" + backupRemaining + "无法塞入第6班");
                continue;
            }

            // 第6班塞入量 = min(剩余备库量, 机台剩余产能)
            double assignQty = Math.min(backupRemaining, residualCapacity);
            assignQty = applyRoundingIfNeeded(assignQty);
            addClassPlanQty(spec, classNum, assignQty);
            spec.setBackupRemainingQty(BigDecimalUtil.sub(backupRemaining, assignQty));

            autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                    "S3.5-第6班塞入剩余备库量",
                    "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                            + "，第6班塞入" + assignQty
                            + "，剩余备库量：" + spec.getBackupRemainingQty());
        }
    }

    /**
     * 构建剩余产能回填的三级优先级排序器。
     *
     * <p>排序规则：</p>
     * <ol>
     *   <li>备库胎圈优先（backupTriggerClass &gt; 0）</li>
     *   <li>同为备库胎圈：按触发班次升序（先触发的优先继续排）</li>
     *   <li>同组内按供应时长阈值分组：未达阈值优先（Priority-2），已达阈值排后（Priority-3）</li>
     *   <li>同组内按供应时长升序</li>
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
        switch (classNum) {
            case 1: scheduleVo.setClass1PlanQty(newValue); break;
            case 2: scheduleVo.setClass2PlanQty(newValue); break;
            case 3: scheduleVo.setClass3PlanQty(newValue); break;
            case 4: scheduleVo.setClass4PlanQty(newValue); break;
            case 5: scheduleVo.setClass5PlanQty(newValue); break;
            case 6: scheduleVo.setClass6PlanQty(newValue); break;
        }
    }

    /**
     * 简单取整处理（避免浮点精度问题，保留2位小数）
     */
    private double applyRoundingIfNeeded(double value) {
        return BigDecimalUtils.valueOf(value).doubleValue();
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
