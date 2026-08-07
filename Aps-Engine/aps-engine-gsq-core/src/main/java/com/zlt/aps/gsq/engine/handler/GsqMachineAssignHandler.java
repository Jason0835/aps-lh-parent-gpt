package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.service.IGsqMachineFilterChainService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTaskNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
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

        // 2. 规格级机台分配（对齐胎圈TQ）：为每个规格选定一台机台，6班次沿用该机台
        //    使用第一班次上下文执行策略链过滤，按"已分配规格数"负载均衡选择机台，
        //    确保 machineCode（数据库/页面展示）及 class1~6MachineCode（任务链/质量等消费）均有值
        Map<String, Integer> machineAssignCount = new HashMap<>();
        for (GsqScheduleResultVo scheduleVo : sortedList) {
            if (!hasAnyShiftPlan(scheduleVo)) {
                continue;
            }

            // 使用第一班次上下文过滤可用机台
            context.setCurrentClassIndex(1);
            context.setCurrentClassCode(getClassCode(1));
            List<GsqMachineInfo> availableMachines = machineFilterChainService.filter(
                    context.getAllMachineList(), scheduleVo, context);

            if (availableMachines.isEmpty()) {
                log.warn("[S3] 规格[{}]无可用机台, 标记未排", scheduleVo.getSteelRingCode());
                markUnscheduled(scheduleVo);
                continue;
            }

            // 选择已分配规格数最少（负载最轻）的机台，避免负载不均
            GsqMachineInfo targetMachine = availableMachines.stream()
                    .min(Comparator.comparingInt(m -> machineAssignCount.getOrDefault(m.getMachineCode(), 0)))
                    .orElse(availableMachines.get(0));

            String machineCode = targetMachine.getMachineCode();
            // 机台定额来自 QUATA 字段（BigDecimal类型）
            Double quota = targetMachine.getQuata() == null ? 0D : targetMachine.getQuata().doubleValue();
            machineAssignCount.merge(machineCode, 1, Integer::sum);

            // 设置规格级单机台：machineCode 及各班次机台/定额均填该机台
            scheduleVo.setMachineCode(machineCode);
            for (int classIndex = 1; classIndex <= 6; classIndex++) {
                if (hasShiftPlan(scheduleVo, classIndex)) {
                    setShiftMachine(scheduleVo, classIndex, machineCode);
                    setShiftQuota(scheduleVo, classIndex, quota);
                }
            }

            log.info("[S3] 规格[{}] 规格级分配机台[{}] 定额[{}]", scheduleVo.getSteelRingCode(), machineCode, quota);
        }

        // 3. 6班次顺序构建任务链（沿用规格级已分配机台）
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
                String machineCode = scheduleVo.getMachineCode();
                if (machineCode == null || machineCode.isEmpty()) {
                    continue;
                }

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
     * 判断规格是否在任意班次有计划量。
     */
    private boolean hasAnyShiftPlan(GsqScheduleResultVo vo) {
        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            if (hasShiftPlan(vo, classIndex)) {
                return true;
            }
        }
        return false;
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
     * 标记为未排产（该规格所有有计划量的班次均标记无可用机台）。
     */
    private void markUnscheduled(GsqScheduleResultVo vo) {
        vo.setUnscheduledFlag("1");
        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            if (hasShiftPlan(vo, classIndex)) {
                // 分析信息写入未排原因
                appendAnalysis(vo, classIndex, "班次" + classIndex + "无可用机台");
            }
        }
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

    /**
     * 设置6个班次的生产顺序（对齐胎圈TQ）。
     *
     * <p>由 {@code GsqQuotaValidateHandler} 在 S5.5 定额校验完成后调用，统一重置生产顺序。</p>
     *
     * <p>排序规则：1.相同英寸连续生产 2.同英寸内按库存供应时长升序排序。
     * 顺序值按机台独立编号（同一机台同一班次内的规格顺序1,2,3...），而非全局编号，
     * 避免"机台只有2个规格但顺序值=4"的问题。</p>
     *
     * <p>写入每个班次的 CLASSX_SEQUENCE 字段（class1Sequence~class6Sequence）。</p>
     *
     * @param scheduleList 排程记录列表
     */
    public void setProduceOrder(List<GsqScheduleResultVo> scheduleList) {
        // 按机台分组：顺序值应按机台独立编号
        Map<String, List<GsqScheduleResultVo>> machineGroupMap = scheduleList.stream()
                .filter(vo -> vo.getMachineCode() != null && !vo.getMachineCode().isEmpty())
                .collect(Collectors.groupingBy(GsqScheduleResultVo::getMachineCode));

        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            // 每个机台内独立设置顺序值
            for (Map.Entry<String, List<GsqScheduleResultVo>> entry : machineGroupMap.entrySet()) {
                List<GsqScheduleResultVo> machineSpecs = entry.getValue();
                // 排序：1.相同英寸连续 2.同英寸内按供应时长升序
                int finalClassIndex = classIndex;
                List<GsqScheduleResultVo> sortedList = machineSpecs.stream()
                        .filter(vo -> hasShiftPlan(vo, finalClassIndex))
                        .sorted((o1, o2) -> {
                            java.math.BigDecimal dim1 = o1.getDimension() == null
                                    ? java.math.BigDecimal.ZERO : o1.getDimension();
                            java.math.BigDecimal dim2 = o2.getDimension() == null
                                    ? java.math.BigDecimal.ZERO : o2.getDimension();
                            int cmp = dim1.compareTo(dim2);
                            if (cmp != 0) {
                                return cmp;
                            }
                            double st1 = o1.getSupplyTime() == null ? 0D : o1.getSupplyTime();
                            double st2 = o2.getSupplyTime() == null ? 0D : o2.getSupplyTime();
                            return Double.compare(st1, st2);
                        })
                        .collect(Collectors.toList());

                int order = 1;
                for (GsqScheduleResultVo vo : sortedList) {
                    setClassSequence(vo, classIndex, order++);
                }
            }
        }
    }

    /**
     * 设置指定班次的生产顺序值（classXSequence）。
     */
    private void setClassSequence(GsqScheduleResultVo vo, int classIndex, int sequence) {
        switch (classIndex) {
            case 1: vo.setClass1Sequence(sequence); break;
            case 2: vo.setClass2Sequence(sequence); break;
            case 3: vo.setClass3Sequence(sequence); break;
            case 4: vo.setClass4Sequence(sequence); break;
            case 5: vo.setClass5Sequence(sequence); break;
            case 6: vo.setClass6Sequence(sequence); break;
            default: break;
        }
    }
}
