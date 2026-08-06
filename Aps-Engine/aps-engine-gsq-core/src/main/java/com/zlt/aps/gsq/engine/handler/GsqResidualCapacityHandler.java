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
 * （机台定额 - 已排产量），按供应时长升序回填到该机台上已排产的规格，避免产能浪费。</p>
 *
 * <p>排产优先级：同机台同班次内，供应时长越短（越紧急）的规格优先回填。</p>
 *
 * <p>第6班特殊处理：机台第6班仍有剩余产能时，对第6班已排产的规格补充剩余产能。</p>
 *
 * <p>回填规则（无备库版）：仅回填该班次已有排产量的规格（不新增班次排产），
 * 避免随意扩大排产范围；单规格机台 S3 阶段已按机台定额满排，无剩余产能，本Handler不触发。</p>
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
    }

    /**
     * 回填指定机台指定班次的剩余产能到该机台上已排产的规格。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>统计该机台该班次已排产量（所有规格之和）</li>
     *   <li>计算剩余产能 = 机台定额 - 已排产量</li>
     *   <li>剩余产能 &gt; 0 时，按供应时长升序排序候选规格</li>
     *   <li>逐规格回填：仅回填该班次已有排产量的规格（不新增班次排产）</li>
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

        // 2. 候选规格 = 该班次已有排产量的规格，按供应时长升序排序（越紧急越优先补充）
        List<GsqScheduleResultVo> candidates = specList.stream()
                .filter(s -> getClassPlanQty(s, classNum) > 0)
                .sorted(Comparator.comparingDouble(s -> s.getSupplyTime() == null ? 0D : s.getSupplyTime()))
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

            double assignQty = applyRoundingIfNeeded(residualCapacity);
            addClassPlanQty(spec, classNum, assignQty);

            autoScheduleLogService.insertGsqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                    "S5.6-剩余产能回填",
                    "钢丝圈代码：" + spec.getSteelRingCode() + "，机台：" + machineCode
                            + "，" + classNum + "班回填" + assignQty);

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
     * 第6班特殊处理：把每个规格第6班的剩余产能补充完整。
     *
     * <p>业务规则：仅回填第6班已有排产量的规格，回填量不超过第6班剩余产能。</p>
     *
     * @param machineCode 机台编码
     * @param specList    该机台上所有规格的排程记录
     * @param machineQuota 机台定额
     * @param context     排程上下文
     */
    private void fillLastClassWithAllRemaining(String machineCode, List<GsqScheduleResultVo> specList,
                                               double machineQuota, GsqScheduleContext context) {
        int classNum = 6;

        // 候选规格 = 第6班已有排产量的规格，按供应时长升序排序
        List<GsqScheduleResultVo> candidates = specList.stream()
                .filter(s -> getClassPlanQty(s, classNum) > 0)
                .sorted(Comparator.comparingDouble(s -> s.getSupplyTime() == null ? 0D : s.getSupplyTime()))
                .collect(Collectors.toList());

        for (GsqScheduleResultVo spec : candidates) {
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

            double assignQty = applyRoundingIfNeeded(residualCapacity);
            addClassPlanQty(spec, classNum, assignQty);

            autoScheduleLogService.insertGsqScheduleLog(spec.getBatchNo(), spec.getOrderNo(),
                    "S5.6-第6班回填剩余产能",
                    "钢丝圈代码：" + spec.getSteelRingCode() + "，机台：" + machineCode
                            + "，第6班回填" + assignQty);

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
            switch (classNum) {
                case 1: scheduleVo.setClass1PlanQty(null); break;
                case 2: scheduleVo.setClass2PlanQty(null); break;
                case 3: scheduleVo.setClass3PlanQty(null); break;
                case 4: scheduleVo.setClass4PlanQty(null); break;
                case 5: scheduleVo.setClass5PlanQty(null); break;
                case 6: scheduleVo.setClass6PlanQty(null); break;
                default: break;
            }
        } else {
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
}