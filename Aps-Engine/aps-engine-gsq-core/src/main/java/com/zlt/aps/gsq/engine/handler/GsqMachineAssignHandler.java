package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.service.IGsqMachineFilterChainService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTaskNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3: 钢丝圈班次排产分配Handler（核心）。
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>按"库存供应时长"对排程记录排序（供应时长越短，优先级越高）</li>
 *   <li>遍历6个班次（1→6）</li>
 *   <li>对每个班次，按规格分组后，调用策略链过滤可用机台</li>
 *   <li>选择最优机台分配计划量（单机台定额）</li>
 *   <li>更新任务链和库存供应时长</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqMachineAssignHandler extends AbsGsqScheduleStepHandler {

    @Resource
    private IGsqMachineFilterChainService machineFilterChainService;

    @Override
    protected String getStepName() {
        return "S3-班次排产分配";
    }

    @Override
    protected void doHandle(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList.isEmpty()) {
            log.warn("[S3] 排程记录为空, 跳过机台分配");
            return;
        }

        // 1. 按库存供应时长排序（供应时长短 = 紧急 = 优先排）
        List<GsqScheduleResultVo> sortedList = sortBySupplyDuration(scheduleList, context);

        // 2. 6班次顺序排产
        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            context.setCurrentClassIndex(classIndex);
            // 设置班次编码（01=夜班, 02=早班, 03=中班）
            context.setCurrentClassCode(getClassCode(classIndex));
            log.info("[S3] 开始处理班次 classIndex={}, classCode={}", classIndex, context.getCurrentClassCode());

            for (GsqScheduleResultVo scheduleVo : sortedList) {
                // 跳过该班次计划量为0的规格
                if (!hasShiftPlan(scheduleVo, classIndex)) {
                    continue;
                }

                // 调用策略链过滤可用机台
                List<GsqMachineInfo> availableMachines = machineFilterChainService.filter(
                        context.getAllMachineList(), scheduleVo, context);

                if (availableMachines.isEmpty()) {
                    log.warn("[S3] 规格[{}]在班次[{}]无可用机台, 标记未排", scheduleVo.getSteelRingCode(), classIndex);
                    markUnscheduled(scheduleVo, classIndex);
                    continue;
                }

                // 分配机台并更新任务链
                assignMachine(scheduleVo, availableMachines, classIndex, context);
            }
        }
    }

    /**
     * 按库存供应时长升序排序，供应时长短=紧急=优先排产。
     */
    private List<GsqScheduleResultVo> sortBySupplyDuration(List<GsqScheduleResultVo> list,
                                                            GsqScheduleContext context) {
        return list.stream()
                .sorted(Comparator.comparingDouble(vo -> calcSupplyDuration(vo, context)))
                .collect(Collectors.toList());
    }

    /**
     * 计算库存供应时长：当前预计库存 / 胎圈每班消耗量。
     */
    private double calcSupplyDuration(GsqScheduleResultVo vo, GsqScheduleContext context) {
        double planStock = vo.getPlanStockQty() == null ? 0 : vo.getPlanStockQty();
        double tqPerShift = (vo.getTqClass2Plan() == null ? 0 : vo.getTqClass2Plan());
        if (tqPerShift <= 0) {
            // 胎圈无消耗则供应时长无穷大
            return Double.MAX_VALUE;
        }
        return planStock / tqPerShift;
    }

    /**
     * 判断指定班次是否有计划量。
     */
    private boolean hasShiftPlan(GsqScheduleResultVo vo, int classIndex) {
        Double planQty = getShiftPlan(vo, classIndex);
        return planQty != null && planQty > 0;
    }

    /**
     * 获取指定班次的计划量。
     */
    private Double getShiftPlan(GsqScheduleResultVo vo, int classIndex) {
        switch (classIndex) {
            case 1: return vo.getClass1PlanQty();
            case 2: return vo.getClass2PlanQty();
            case 3: return vo.getClass3PlanQty();
            case 4: return vo.getClass4PlanQty();
            case 5: return vo.getClass5PlanQty();
            case 6: return vo.getClass6PlanQty();
            default: return null;
        }
    }

    /**
     * 设置指定班次的已分配机台编号。
     */
    private void setShiftMachine(GsqScheduleResultVo vo, int classIndex, String machineCode) {
        switch (classIndex) {
            case 1: vo.setClass1MachineCode(machineCode); break;
            case 2: vo.setClass2MachineCode(machineCode); break;
            case 3: vo.setClass3MachineCode(machineCode); break;
            case 4: vo.setClass4MachineCode(machineCode); break;
            case 5: vo.setClass5MachineCode(machineCode); break;
            case 6: vo.setClass6MachineCode(machineCode); break;
            default: break;
        }
    }

    /**
     * 设置指定班次的机台定额。
     */
    private void setShiftQuota(GsqScheduleResultVo vo, int classIndex, Double quota) {
        switch (classIndex) {
            case 1: vo.setClass1MachineQuota(quota); break;
            case 2: vo.setClass2MachineQuota(quota); break;
            case 3: vo.setClass3MachineQuota(quota); break;
            case 4: vo.setClass4MachineQuota(quota); break;
            case 5: vo.setClass5MachineQuota(quota); break;
            case 6: vo.setClass6MachineQuota(quota); break;
            default: break;
        }
    }

    /**
     * 班次索引转班次编码：1/4=03中班, 2/5=01夜班, 3/6=02早班。
     */
    private String getClassCode(int classIndex) {
        switch (classIndex) {
            case 1: case 4: return "03"; // 中班
            case 2: case 5: return "01"; // 夜班
            case 3: case 6: return "02"; // 早班
            default: return "";
        }
    }

    /**
     * 标记为未排产。
     */
    private void markUnscheduled(GsqScheduleResultVo vo, int classIndex) {
        vo.setUnscheduledFlag("1");
        // 分析信息写入未排原因
        String analysis = "班次" + classIndex + "无可用机台";
        appendAnalysis(vo, classIndex, analysis);
    }

    /**
     * 追加班次分析信息。
     */
    private void appendAnalysis(GsqScheduleResultVo vo, int classIndex, String analysis) {
        String existing = getShiftAnalysis(vo, classIndex);
        String newAnalysis = existing == null || existing.isEmpty() ? analysis : existing + ";" + analysis;
        setShiftAnalysis(vo, classIndex, newAnalysis);
    }

    private String getShiftAnalysis(GsqScheduleResultVo vo, int classIndex) {
        switch (classIndex) {
            case 1: return vo.getClass1Analysis();
            case 2: return vo.getClass2Analysis();
            case 3: return vo.getClass3Analysis();
            case 4: return vo.getClass4Analysis();
            case 5: return vo.getClass5Analysis();
            case 6: return vo.getClass6Analysis();
            default: return "";
        }
    }

    private void setShiftAnalysis(GsqScheduleResultVo vo, int classIndex, String analysis) {
        switch (classIndex) {
            case 1: vo.setClass1Analysis(analysis); break;
            case 2: vo.setClass2Analysis(analysis); break;
            case 3: vo.setClass3Analysis(analysis); break;
            case 4: vo.setClass4Analysis(analysis); break;
            case 5: vo.setClass5Analysis(analysis); break;
            case 6: vo.setClass6Analysis(analysis); break;
            default: break;
        }
    }

    /**
     * 分配机台并更新任务链。
     *
     * <p>策略：选择任务链最短的机台作为目标机台，避免机台负载不均。</p>
     */
    private void assignMachine(GsqScheduleResultVo scheduleVo, List<GsqMachineInfo> machines,
                                int classIndex, GsqScheduleContext context) {
        // 选择任务链最短（即负载最轻）的机台
        GsqMachineInfo targetMachine = machines.stream()
                .min(Comparator.comparingInt(m -> getTaskChainSize(context, m.getMachineCode())))
                .orElse(machines.get(0));

        String machineCode = targetMachine.getMachineCode();
        // 机台定额来自 QUATA 字段（BigDecimal类型）
        Double quota = targetMachine.getQuata() == null ? 0D : targetMachine.getQuata().doubleValue();

        // 更新排程记录
        setShiftMachine(scheduleVo, classIndex, machineCode);
        setShiftQuota(scheduleVo, classIndex, quota);

        // 创建任务链节点
        GsqTaskNode node = new GsqTaskNode();
        node.setClassIndex(classIndex);
        node.setMachineCode(machineCode);
        node.setPlanQty(getShiftPlan(scheduleVo, classIndex));
        node.setStartStockQty(scheduleVo.getPlanStockQty() == null ? 0 : scheduleVo.getPlanStockQty());
        double endStock = node.getStartStockQty() + node.getPlanQty() - getTqConsume(scheduleVo, classIndex);
        node.setEndStockQty(endStock);

        // 加入任务链
        Map<String, LinkedList<GsqTaskNode>> taskChainMap = context.getTaskChainMap();
        LinkedList<GsqTaskNode> chain = taskChainMap.computeIfAbsent(machineCode, k -> new LinkedList<>());
        chain.addLast(node);

        log.info("[S3] 规格[{}] 班次[{}] 分配机台[{}] 定额[{}] 计划量[{}]",
                scheduleVo.getSteelRingCode(), classIndex, machineCode, quota, node.getPlanQty());
    }

    /**
     * 获取任务链长度。
     */
    private int getTaskChainSize(GsqScheduleContext context, String machineCode) {
        LinkedList<GsqTaskNode> chain = context.getTaskChainMap().get(machineCode);
        return chain == null ? 0 : chain.size();
    }

    /**
     * 获取对应胎圈班次的消耗量。
     */
    private double getTqConsume(GsqScheduleResultVo vo, int classIndex) {
        // 钢丝圈N班供应胎圈N+1班
        int tqClassIndex = classIndex + 1;
        switch (tqClassIndex) {
            case 2: return vo.getTqClass2Plan() == null ? 0 : vo.getTqClass2Plan();
            case 3: return vo.getTqClass3Plan() == null ? 0 : vo.getTqClass3Plan();
            case 4: return vo.getTqClass4Plan() == null ? 0 : vo.getTqClass4Plan();
            case 5: return vo.getTqClass5Plan() == null ? 0 : vo.getTqClass5Plan();
            case 6: return vo.getTqClass6Plan() == null ? 0 : vo.getTqClass6Plan();
            case 7: return vo.getTqClass7Plan() == null ? 0 : vo.getTqClass7Plan();
            default: return 0;
        }
    }
}
