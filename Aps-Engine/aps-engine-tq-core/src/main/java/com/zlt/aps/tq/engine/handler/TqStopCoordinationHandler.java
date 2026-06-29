package com.zlt.aps.tq.engine.handler;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * S4: 成型/胎圈停产协调Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>场景1-成型停产：胎圈需在成型停产前提前备货生产</li>
 *   <li>场景2-胎圈停产成型不停产：胎圈停产班次计划量前移至停产前最后一个可用班次</li>
 *   <li>停产交集日达阈值触发开产逻辑</li>
 * </ol>
 *
 * <p>成型停产场景细分：</p>
 * <ul>
 *   <li>成型停产1天：胎圈按预排班数(moldingStopPreShiftCount)排产，保证成型恢复后供应</li>
 *   <li>成型停产≥2天：触发开产逻辑，按开产库存补量预值(reopenStockThreshold)控制排产量</li>
 * </ul>
 *
 * <p>胎圈停产成型不停产场景：</p>
 * <ul>
 *   <li>胎圈停产班次的计划量前移至停产前最后一个可用班次</li>
 *   <li>收尾规格的计划量也需要前移</li>
 * </ul>
 *
 * <p>停产交集日逻辑：</p>
 * <ul>
 *   <li>当成型停产和胎圈停产在同一班次有交集时，需提前备货</li>
 *   <li>交集天数≥stopIntersectionDays时，触发开产逻辑</li>
 *   <li>开产后计划量达到reopenStockThreshold时切换下一个优先级规格（雨露均沾）</li>
 * </ul>
 */
@Slf4j
@Component
public class TqStopCoordinationHandler extends AbsTqScheduleStepHandler {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Resource
    private TqMachineAssignHandler machineAssignHandler;

    @Override
    protected String getStepName() {
        return "S4-成型/胎圈停产协调";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        Map<String, Boolean> cxStopShiftMap = context.getCxStopShiftMap();
        Map<String, Boolean> tqStopShiftMap = context.getTqStopShiftMap();

        // 1. 处理成型停产场景
        handleMoldingStop(context, cxStopShiftMap);

        // 2. 处理胎圈停产成型不停产场景
        handleTqStopOnly(context, cxStopShiftMap, tqStopShiftMap);

        // 3. 处理停产交集日开产逻辑
        handleStopIntersection(context, cxStopShiftMap, tqStopShiftMap);

        // 4. 刷新任务链（S4调整了计划量，需同步更新链条节点的库存和保证班数）
        machineAssignHandler.refreshTaskChain(context, null, 1);

        log.info("[S4] 成型/胎圈停产协调完成");
    }

    // ==================== 场景1：成型停产 ====================

    /**
     * 处理成型停产场景。
     *
     * <p>成型停产时，胎圈仍需排产以保证成型恢复后的供应：</p>
     * <ul>
     *   <li>成型停产1天：胎圈按预排班数排产</li>
     *   <li>成型停产≥2天(可配置)：触发开产逻辑</li>
     * </ul>
     *
     * @param context 排程上下文
     * @param cxStopShiftMap 成型停产班次
     */
    private void handleMoldingStop(TqScheduleContext context, Map<String, Boolean> cxStopShiftMap) {
        if (cxStopShiftMap == null || cxStopShiftMap.isEmpty()) {
            return;
        }

        String scheduleDate = context.getScheduleDate();
        String[] shiftKeys = buildShiftKeys(scheduleDate);
        TqScheduleParams params = context.getParams();
        double preShiftCount = params.getMoldingStopPreShiftCount() == null ? 2D : params.getMoldingStopPreShiftCount();

        // 统计成型停产班次数
        long cxStopCount = 0;
        for (int i = 0; i < 6; i++) {
            if (shiftKeys[i] != null && cxStopShiftMap.containsKey(shiftKeys[i])) {
                cxStopCount++;
            }
        }

        if (cxStopCount == 0) {
            return;
        }

        // 成型停产天数（3个班次=1天）
        double stopDays = BigDecimalUtil.div(cxStopCount, 3, 1);
        double stopIntersectionDays = params.getStopIntersectionDays() == null ? 2D : params.getStopIntersectionDays();

        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            for (int classIdx = 0; classIdx < 6; classIdx++) {
                String shiftKey = shiftKeys[classIdx];
                if (shiftKey == null || !cxStopShiftMap.containsKey(shiftKey)) {
                    continue;
                }

                double planQty = getClassPlanQty(scheduleVo, classIdx + 1);

                if (stopDays < stopIntersectionDays) {
                    // 成型停产1天：胎圈按预排班数排产，保持计划量不变
                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "成型停产-按预排班数排产",
                            "胎圈代码：" + scheduleVo.getBeadCode()
                                    + "，" + (classIdx + 1) + "班成型停产，胎圈保持排产，计划量=" + planQty
                                    + "，预排班数=" + preShiftCount);
                } else {
                    // 成型停产≥2天：触发开产逻辑
                    handleReopenProduction(scheduleVo, classIdx + 1, planQty, context);
                }
            }
        }
    }

    /**
     * 开产逻辑：成型停产后胎圈开产排产。
     *
     * <p>开产后计划量达到开产库存补量预值时，切换下一个优先级规格（雨露均沾）。</p>
     */
    private void handleReopenProduction(TqScheduleResultVo scheduleVo, int classNum,
                                        double planQty, TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        double reopenThreshold = params.getReopenStockThreshold() == null ? 0D : params.getReopenStockThreshold();

        if (reopenThreshold <= 0) {
            // 未配置开产阈值，保持原计划量
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "成型停产-开产(无阈值)", "胎圈代码：" + scheduleVo.getBeadCode()
                            + "，" + classNum + "班开产，计划量=" + planQty);
            return;
        }

        // 如果计划量已达到开产库存补量预值，标记为可切换规格
        if (planQty >= reopenThreshold) {
            scheduleVo.setCloseOutSpecFlag("1"); // 标记为非收尾，允许切换到下一规格
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "成型停产-开产(达到阈值切换)", "胎圈代码：" + scheduleVo.getBeadCode()
                            + "，" + classNum + "班开产，计划量=" + planQty
                            + "，达到开产阈值=" + reopenThreshold + "，切换下一优先级规格");
        } else {
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                    "成型停产-开产(未达阈值)", "胎圈代码：" + scheduleVo.getBeadCode()
                            + "，" + classNum + "班开产，计划量=" + planQty
                            + "，开产阈值=" + reopenThreshold);
        }
    }

    // ==================== 场景2：胎圈停产成型不停产 ====================

    /**
     * 处理胎圈停产成型不停产场景。
     *
     * <p>胎圈停产但成型不停产时，胎圈停产班次的计划量需要前移到停产前最后一个可用班次。</p>
     * <p>只处理胎圈停产但成型未停产的班次。</p>
     */
    private void handleTqStopOnly(TqScheduleContext context, Map<String, Boolean> cxStopShiftMap,
                                  Map<String, Boolean> tqStopShiftMap) {
        if (tqStopShiftMap == null || tqStopShiftMap.isEmpty()) {
            return;
        }

        String scheduleDate = context.getScheduleDate();
        String[] shiftKeys = buildShiftKeys(scheduleDate);

        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            // 从后往前检查胎圈停产班次
            for (int classIdx = 5; classIdx >= 0; classIdx--) {
                String shiftKey = shiftKeys[classIdx];
                if (shiftKey == null) {
                    continue;
                }

                // 只处理胎圈停产但成型不停产的班次
                boolean tqStop = tqStopShiftMap.containsKey(shiftKey);
                boolean cxStop = cxStopShiftMap != null && cxStopShiftMap.containsKey(shiftKey);
                if (!tqStop || cxStop) {
                    continue; // 非胎圈停产 或 成型也停产（场景1已处理）
                }

                double planQty = getClassPlanQty(scheduleVo, classIdx + 1);
                if (planQty <= 0) {
                    continue;
                }

                // 前移到停产前最后一个可用班次
                int targetClassIdx = findPrevAvailableClass(shiftKeys, classIdx, tqStopShiftMap, cxStopShiftMap);
                if (targetClassIdx >= 0) {
                    double targetQty = getClassPlanQty(scheduleVo, targetClassIdx + 1);
                    setClassPlanQty(scheduleVo, targetClassIdx + 1, BigDecimalUtil.add(targetQty, planQty));
                    setClassPlanQty(scheduleVo, classIdx + 1, 0D);

                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "胎圈停产成型不停产-前移",
                            "胎圈代码：" + scheduleVo.getBeadCode()
                                    + "，" + (classIdx + 1) + "班胎圈停产，计划量" + planQty
                                    + "前移到" + (targetClassIdx + 1) + "班");
                } else {
                    autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                            "胎圈停产成型不停产-无可用班次",
                            "胎圈代码：" + scheduleVo.getBeadCode()
                                    + "，" + (classIdx + 1) + "班胎圈停产，计划量" + planQty
                                    + "无可用前序班次前移");
                }
            }
        }
    }

    /**
     * 从指定班次往前查找最后一个可用班次（既非胎圈停产也非成型停产）
     */
    private int findPrevAvailableClass(String[] shiftKeys, int currentIdx,
                                       Map<String, Boolean> tqStopShiftMap,
                                       Map<String, Boolean> cxStopShiftMap) {
        for (int i = currentIdx - 1; i >= 0; i--) {
            String shiftKey = shiftKeys[i];
            if (shiftKey == null) {
                continue;
            }
            boolean tqStop = tqStopShiftMap.containsKey(shiftKey);
            boolean cxStop = cxStopShiftMap != null && cxStopShiftMap.containsKey(shiftKey);
            if (!tqStop && !cxStop) {
                return i;
            }
        }
        return -1;
    }

    // ==================== 停产交集日开产逻辑 ====================

    /**
     * 处理成型停产和胎圈停产交集日的开产逻辑。
     *
     * <p>当成型停产和胎圈停产在同一班次有交集时：</p>
     * <ul>
     *   <li>交集班次需要提前备货生产（在交集前生产足够量）</li>
     *   <li>交集天数≥stopIntersectionDays时，触发开产逻辑</li>
     * </ul>
     */
    private void handleStopIntersection(TqScheduleContext context, Map<String, Boolean> cxStopShiftMap,
                                        Map<String, Boolean> tqStopShiftMap) {
        if (cxStopShiftMap == null || tqStopShiftMap == null) {
            return;
        }

        String scheduleDate = context.getScheduleDate();
        String[] shiftKeys = buildShiftKeys(scheduleDate);
        TqScheduleParams params = context.getParams();
        double stopIntersectionDays = params.getStopIntersectionDays() == null ? 2D : params.getStopIntersectionDays();

        // 统计交集班次
        int intersectionCount = 0;
        for (int i = 0; i < 6; i++) {
            if (shiftKeys[i] != null && cxStopShiftMap.containsKey(shiftKeys[i]) && tqStopShiftMap.containsKey(shiftKeys[i])) {
                intersectionCount++;
            }
        }

        if (intersectionCount == 0) {
            return;
        }

        // 交集天数（3个班次=1天）
        double intersectionDays = BigDecimalUtil.div(intersectionCount, 3, 1);

        if (intersectionDays < stopIntersectionDays) {
            // 交集天数未达阈值，仅提前备货
            handleAdvanceStock(context, shiftKeys, cxStopShiftMap, tqStopShiftMap);
        } else {
            // 交集天数达阈值，触发开产逻辑
            handleAdvanceStock(context, shiftKeys, cxStopShiftMap, tqStopShiftMap);
            handleReopenForIntersection(context, intersectionDays);
        }
    }

    /**
     * 提前备货：在交集班次前生产足够量
     */
    private void handleAdvanceStock(TqScheduleContext context, String[] shiftKeys,
                                    Map<String, Boolean> cxStopShiftMap, Map<String, Boolean> tqStopShiftMap) {
        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            for (int classIdx = 0; classIdx < 6; classIdx++) {
                String shiftKey = shiftKeys[classIdx];
                if (shiftKey == null) {
                    continue;
                }
                // 成型有需求的班次（非成型停产），但胎圈停产，需提前备货
                boolean cxStop = cxStopShiftMap.containsKey(shiftKey);
                boolean tqStop = tqStopShiftMap.containsKey(shiftKey);
                if (!cxStop && tqStop) {
                    // 成型不停产但胎圈停产，成型需求量需在前面班次提前生产
                    double cxDemand = getCxDemand(scheduleVo, classIdx + 1, context);
                    if (cxDemand > 0) {
                        // 找到前面最近的可用班次
                        int targetIdx = findPrevAvailableClass(shiftKeys, classIdx, tqStopShiftMap, cxStopShiftMap);
                        if (targetIdx >= 0) {
                            double targetQty = getClassPlanQty(scheduleVo, targetIdx + 1);
                            setClassPlanQty(scheduleVo, targetIdx + 1, BigDecimalUtil.add(targetQty, cxDemand));

                            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                                    "停产交集-提前备货",
                                    "胎圈代码：" + scheduleVo.getBeadCode()
                                            + "，" + (classIdx + 1) + "班胎圈停产成型不停产"
                                            + "，成型需求量" + cxDemand + "提前到" + (targetIdx + 1) + "班生产");
                        }
                    }
                }
            }
        }
    }

    /**
     * 交集天数达阈值时的开产逻辑
     */
    private void handleReopenForIntersection(TqScheduleContext context, double intersectionDays) {
        TqScheduleParams params = context.getParams();
        double reopenThreshold = params.getReopenStockThreshold() == null ? 0D : params.getReopenStockThreshold();

        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            double totalPlan = 0D;
            for (int i = 1; i <= 6; i++) {
                totalPlan = BigDecimalUtil.add(totalPlan, getClassPlanQty(scheduleVo, i));
            }

            // 计划量达到开产库存补量预值时，切换下一个优先级规格
            if (reopenThreshold > 0 && totalPlan >= reopenThreshold) {
                scheduleVo.setCloseOutSpecFlag("1"); // 标记为非收尾，允许切换
                autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(),
                        "停产交集-开产(达到阈值切换)",
                        "胎圈代码：" + scheduleVo.getBeadCode()
                                + "，交集天数=" + intersectionDays
                                + "，总计划量=" + totalPlan
                                + "，达到开产阈值=" + reopenThreshold
                                + "，切换下一优先级规格");
            }
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 构建6个班次对应的日期班次key
     * 班次映射：1班=D日中班, 2班=D+1日夜班, 3班=D+1日早班, 4班=D+1日中班, 5班=D+2日夜班, 6班=D+2日早班
     */
    private String[] buildShiftKeys(String scheduleDate) {
        String[] keys = new String[6];
        try {
            java.time.LocalDate baseDate = java.time.LocalDate.parse(scheduleDate);
            keys[0] = baseDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + "|中班";
            keys[1] = baseDate.plusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + "|夜班";
            keys[2] = baseDate.plusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + "|早班";
            keys[3] = baseDate.plusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + "|中班";
            keys[4] = baseDate.plusDays(2).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + "|夜班";
            keys[5] = baseDate.plusDays(2).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + "|早班";
        } catch (Exception e) {
            log.error("[S4] 解析排程日期失败: {}", scheduleDate, e);
        }
        return keys;
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
     * 获取成型需求量（胎圈N班供应成型(N+2)班）
     */
    private double getCxDemand(TqScheduleResultVo scheduleVo, int classNum, TqScheduleContext context) {
        double coefficient = context.getParams().getDemandCoefficient() == null ? 2D : context.getParams().getDemandCoefficient();
        double cxPlan;
        switch (classNum) {
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
}
