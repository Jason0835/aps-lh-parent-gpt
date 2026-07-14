package com.zlt.aps.tq.engine.handler;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S5.5: 定额校验与顺序重置Handler。
 *
 * <p>职责：在S4(停产协调)和S5(班次均衡)修改计划量之后，对所有机台所有班次进行定额校验，
 * 确保单机台单班次排产量不超过机台定额（quota），超出部分按优先级延后到下一班次；
 * 最后统一重置6个班次的生产顺序值。</p>
 *
 * <p>背景：S3阶段使用capacityMaps追踪机台产能，但S4/S5修改计划量时不检查定额，
 * 可能导致单机台单班次总排产量超过quota。本Handler在所有计划量修改完成后统一校验修正。</p>
 *
 * <p>处理流程：</p>
 * <ol>
 *   <li>按机台分组所有已分配机台的排程记录</li>
 *   <li>逐机台逐班次（1→6）校验：Σ(同机台同班次计划量) ≤ quota</li>
 *   <li>若超量：按供应时长降序排序规格（供应时长大的先延后），将超出部分延后到下一班</li>
 *   <li>第6班超量无法延后，仅记录日志</li>
 *   <li>调用TqMachineAssignHandler.setProduceOrder()重置所有生产顺序值</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqQuotaValidateHandler extends AbsTqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Resource
    private TqMachineAssignHandler machineAssignHandler;

    @Override
    protected String getStepName() {
        return "S5.5-定额校验与顺序重置";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }

        // 1. 按机台分组，得到 machineCode → 该机台上所有规格的排程记录
        Map<String, List<TqScheduleResultVo>> machineSpecMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()))
                .collect(Collectors.groupingBy(TqScheduleResultVo::getMachineCode));

        if (machineSpecMap.isEmpty()) {
            log.info("[S5.5] 无已分配机台的规格，跳过定额校验");
            return;
        }

        // 2. 构建机台信息Map，便于取quota
        Map<String, TqMachineInfo> machineInfoMap = context.getAllMachineList().stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineCode, m -> m, (a, b) -> a));

        // 3. 默认机台定额（兜底，优先使用机台自身quota）
        double defaultQuota = context.getParams().getMaxClassOutput() == null ? 3000D
                : context.getParams().getMaxClassOutput();

        // 4. 逐机台处理定额校验
        int totalOverflowCount = 0;
        for (Map.Entry<String, List<TqScheduleResultVo>> entry : machineSpecMap.entrySet()) {
            String machineCode = entry.getKey();
            List<TqScheduleResultVo> specList = entry.getValue();
            TqMachineInfo machine = machineInfoMap.get(machineCode);
            double machineQuota = getMachineQuota(machine, defaultQuota);

            totalOverflowCount += validateAndDeferOverflow(machineCode, specList, machineQuota);
        }

        log.info("[S5.5] 定额校验完成, 机台数:{}, 累计超量延后次数:{}", machineSpecMap.size(), totalOverflowCount);

        // 5. 所有计划量修改完成后，统一重置6个班次的生产顺序
        machineAssignHandler.setProduceOrder(scheduleList);
        log.info("[S5.5] 生产顺序重置完成");
    }

    /**
     * 校验指定机台所有班次的排产量是否超过定额，超出部分延后到下一班。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>逐班次（1→6）统计该机台该班次已排产量</li>
     *   <li>若超过定额：按供应时长降序排序规格（供应时长大的先延后），逐规格扣减并延后到下一班</li>
     *   <li>第6班超量无法延后，仅记录日志</li>
     * </ol>
     *
     * @param machineCode 机台编码
     * @param specList 该机台上所有规格的排程记录
     * @param machineQuota 机台定额
     * @return 累计超量延后次数
     */
    private int validateAndDeferOverflow(String machineCode, List<TqScheduleResultVo> specList,
                                         double machineQuota) {
        int overflowCount = 0;

        // 逐班次校验（1→6），延后时会累加到下一班，所以需要顺序处理
        for (int classNum = 1; classNum <= 6; classNum++) {
            // 统计该机台该班次已排产量
            double usedCapacity = 0D;
            for (TqScheduleResultVo spec : specList) {
                usedCapacity = BigDecimalUtil.add(usedCapacity, getClassPlanQty(spec, classNum));
            }

            double overflow = BigDecimalUtil.sub(usedCapacity, machineQuota);
            if (overflow <= 0) {
                // 未超量，跳过
                continue;
            }

            // 超量：按供应时长降序排序规格（供应时长大的先延后，保留供应时长小的优先排产）
            int finalClassNum = classNum;
            List<TqScheduleResultVo> sortedSpecs = specList.stream()
                    .filter(s -> getClassPlanQty(s, finalClassNum) > 0)
                    .sorted((o1, o2) -> {
                        double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
                        double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
                        return Double.compare(st2, st1);
                    })
                    .collect(Collectors.toList());

            // 逐规格扣减并延后，直到超量清零
            for (TqScheduleResultVo spec : sortedSpecs) {
                if (overflow <= 0) {
                    break;
                }
                double currentPlan = getClassPlanQty(spec, classNum);
                if (currentPlan <= 0) {
                    continue;
                }

                // 该规格可扣减的量 = min(当前班次计划量, 剩余超量)
                double deductQty = Math.min(currentPlan, overflow);
                double newPlan = BigDecimalUtil.sub(currentPlan, deductQty);
                setClassPlanQty(spec, classNum, newPlan);

                // 延后到下一班
                if (classNum < 6) {
                    deferToNextClass(spec, classNum, deductQty);
                    overflowCount++;

                    autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                            "S5.5-定额校验超量延后",
                            "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                                    + "，" + classNum + "班超定额，扣减" + deductQty
                                    + "延后至" + (classNum + 1) + "班");
                } else {
                    // 第6班无法延后，记录日志
                    autoScheduleLogService.insertTqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                            "S5.5-第6班超定额无法延后",
                            "胎圈代码：" + spec.getBeadCode() + "，机台：" + machineCode
                                    + "，第6班超定额" + deductQty + "无法延后");
                }

                overflow = BigDecimalUtil.sub(overflow, deductQty);
            }
        }

        return overflowCount;
    }

    /**
     * 将超出量延后到下一班次累加。
     *
     * @param scheduleVo 排程记录
     * @param currentClass 当前班次（1~5）
     * @param overflowQty 延后量
     */
    private void deferToNextClass(TqScheduleResultVo scheduleVo, int currentClass, double overflowQty) {
        double nextClassQty = getClassPlanQty(scheduleVo, currentClass + 1);
        setClassPlanQty(scheduleVo, currentClass + 1, BigDecimalUtil.add(nextClassQty, overflowQty));
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
     * 获取机台定额：优先使用机台自身的quota字段，否则使用全局默认值
     */
    private double getMachineQuota(TqMachineInfo machine, double defaultQuota) {
        if (machine != null && machine.getQuota() != null && machine.getQuota() > 0) {
            return machine.getQuota();
        }
        return defaultQuota;
    }
}
