package com.zlt.aps.gsq.engine.handler;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.service.IGsqMachineFilterChainService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqTaskNode;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
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
 *   <li>按优先级排序排程记录（P-0当前班次触发备库 → P-1已分配机台 → P-2备库缺口 → P-3供应时长，对齐胎圈TQ）</li>
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

    /** 默认备库规格班次最大班产阈值（多规格机台上备库规格当班初始排产上限，SYS1603005） */
    private static final double DEFAULT_BACKUP_MULTI_SPEC_THRESHOLD = 1000D;

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

        // 1. 按优先级排序（对齐胎圈TQ P-0~P-3）：
        //    P-0 当前班次新触发备库规格 > P-1 已分配机台规格 > P-2 备库规格(缺口大优先) > P-3 非备库按供应时长升序
        List<GsqScheduleResultVo> sortedList = sortByPriority(scheduleList, context);

        // 2. 规格级机台分配（对齐胎圈TQ）：为每个规格选定一台机台，6班次沿用该机台
        //    使用第一班次上下文执行策略链过滤，选择机台时：
        //    ① 缠绕盘连续优先（机台末规格缠绕盘与当前规格相同者优先，减少换盘）
        //    ② 剩余产能较小优先（剩余产能=QUATA×班次数-已分配量，先把一台机台排满）
        //    确保 machineCode（数据库/页面展示）及 class1~6MachineCode（任务链/质量等消费）均有值
        Map<String, String> machineLastTwiningDisc = new HashMap<>();
        Map<String, Double> machineAssignedQty = new HashMap<>();
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

            final String currentTwiningDisc = scheduleVo.getTwiningDiscCode();
            // 选择机台：①缠绕盘连续优先 ②剩余产能较小优先（优先排满一台）
            GsqMachineInfo targetMachine = availableMachines.stream()
                    .min((m1, m2) -> {
                        // ① 缠绕盘连续优先：机台末规格缠绕盘与当前规格相同者优先
                        String disc1 = machineLastTwiningDisc.get(m1.getMachineCode());
                        String disc2 = machineLastTwiningDisc.get(m2.getMachineCode());
                        boolean same1 = currentTwiningDisc != null && currentTwiningDisc.equals(disc1);
                        boolean same2 = currentTwiningDisc != null && currentTwiningDisc.equals(disc2);
                        if (same1 != same2) {
                            return same1 ? -1 : 1;
                        }
                        // ② 剩余产能较小优先：剩余产能=QUATA×班次数-已分配量，越小越接近排满=优先
                        double remain1 = calcRemainCapacity(m1, machineAssignedQty.getOrDefault(m1.getMachineCode(), 0D));
                        double remain2 = calcRemainCapacity(m2, machineAssignedQty.getOrDefault(m2.getMachineCode(), 0D));
                        return Double.compare(remain1, remain2);
                    })
                    .orElse(availableMachines.get(0));

            String machineCode = targetMachine.getMachineCode();
            // 机台定额来自 QUATA 字段（BigDecimal类型）
            Double quota = targetMachine.getQuata() == null ? 0D : targetMachine.getQuata().doubleValue();
            // 记录该机台末规格缠绕盘及已分配量（供后续规格按缠绕盘连续优先选择）
            if (currentTwiningDisc != null) {
                machineLastTwiningDisc.put(machineCode, currentTwiningDisc);
            }
            machineAssignedQty.merge(machineCode, getTotalPlanQty(scheduleVo), Double::sum);

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

        // 2.5 备库多规格机台当班初始排产限制（对齐胎圈 TQ SYS1101029）
        // 需在所有规格机台分配完成后，统计各机台规格数，才能判断单/多规格机台
        applyBackupMultiSpecLimit(context);

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
     * 按优先级排序（对齐胎圈TQ P-0~P-3）。
     *
     * <p>排序规则：</p>
     * <ul>
     *   <li>P-0: 当前班次（第一班次）新触发备库的规格最高优先级</li>
     *   <li>P-1: 已分配机台的规格优先（保证机台分配稳定）</li>
     *   <li>P-2: 备库规格优先于非备库规格；同为备库规格按剩余需求缺口从大到小（缺口越大越紧急）</li>
     *   <li>P-3: 非备库规格按库存供应时长升序（供应时长短=紧急=优先排）</li>
     * </ul>
     */
    private List<GsqScheduleResultVo> sortByPriority(List<GsqScheduleResultVo> list,
                                                      GsqScheduleContext context) {
        return list.stream()
                .sorted((o1, o2) -> {
                    // P-0: 当前班次（第一班次）新触发备库规格最高优先级
                    boolean currentTrigger1 = o1.getBackupTriggerClass() != null
                            && o1.getBackupTriggerClass() > 0
                            && o1.getBackupTriggerClass() == 1;
                    boolean currentTrigger2 = o2.getBackupTriggerClass() != null
                            && o2.getBackupTriggerClass() > 0
                            && o2.getBackupTriggerClass() == 1;
                    if (currentTrigger1 != currentTrigger2) {
                        return currentTrigger1 ? -1 : 1;
                    }

                    // P-1: 已分配机台的规格优先（避免机台分配不稳定）
                    boolean planned1 = o1.getMachineCode() != null && !o1.getMachineCode().isEmpty();
                    boolean planned2 = o2.getMachineCode() != null && !o2.getMachineCode().isEmpty();
                    if (planned1 != planned2) {
                        return planned1 ? -1 : 1;
                    }

                    boolean backup1 = o1.getBackupTriggerClass() != null && o1.getBackupTriggerClass() > 0;
                    boolean backup2 = o2.getBackupTriggerClass() != null && o2.getBackupTriggerClass() > 0;

                    // P-2: 备库规格优先于非备库规格
                    if (backup1 != backup2) {
                        return backup1 ? -1 : 1;
                    }

                    // P-2: 同为备库规格：按剩余需求缺口从大到小（缺口越大越优先）
                    if (backup1 && backup2) {
                        double rem1 = o1.getBackupRemainingQty() == null ? 0D : o1.getBackupRemainingQty();
                        double rem2 = o2.getBackupRemainingQty() == null ? 0D : o2.getBackupRemainingQty();
                        if (rem1 != rem2) {
                            return Double.compare(rem2, rem1);  // 降序
                        }
                    }

                    // P-3: 非备库规格按供应时长升序
                    double st1 = calcSupplyDuration(o1, context);
                    double st2 = calcSupplyDuration(o2, context);
                    return Double.compare(st1, st2);
                })
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
        Object value = vo.getFieldValueByFieldName("class" + classIndex + "PlanQty");
        return value == null ? null : ((Number) value).doubleValue();
    }

    /**
     * 计算机台剩余产能：QUATA × 班次数 - 已分配量。
     *
     * @param machine     机台
     * @param assignedQty 已分配计划量
     * @return 剩余产能
     */
    private double calcRemainCapacity(GsqMachineInfo machine, double assignedQty) {
        Double quota = machine.getQuata() == null ? 0D : machine.getQuata().doubleValue();
        double totalCapacity = quota * getMachineShiftCount(machine);
        return totalCapacity - assignedQty;
    }

    /**
     * 计算机台在6班次窗口内的运行班次数（按班制折算）。
     *
     * <p>6班次窗口=2天×3班。按 CLASS_SHIFT 班制折算每日班次数再×2天：
     * 三班制=3班/日→6班，两班制=2班/日→4班，其他/为空默认按三班制=6班。</p>
     */
    private int getMachineShiftCount(GsqMachineInfo machine) {
        String classShift = machine.getClassShift();
        if (classShift != null && classShift.contains("两")) {
            return 4;
        }
        return 6;
    }

    /**
     * 获取规格6班次总计划量（用于机台已分配量累计，判断剩余产能）。
     */
    private double getTotalPlanQty(GsqScheduleResultVo vo) {
        double total = 0D;
        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            Double plan = getShiftPlan(vo, classIndex);
            if (plan != null) {
                total += plan;
            }
        }
        return total;
    }

    /**
     * 备库多规格机台当班初始排产限制（对齐胎圈 TQ SYS1101029）。
     *
     * <p>业务规则：</p>
     * <ul>
     *   <li>单一规格机台：备库钢丝圈只受机台定额(quota)限制，可满排，不受本阈值限制</li>
     *   <li>多规格机台：备库钢丝圈当班初始排产不超过 min(机台定额, SYS1603005阈值)，
     *       超出部分延后到下一班次，避免单个备库规格占满机台而挤占同机台其他规格的排产</li>
     * </ul>
     *
     * <p>说明：需在规格级机台分配完成后执行，此时才能统计各机台的规格数，判断单/多规格机台。</p>
     *
     * @param context 排程上下文
     */
    private void applyBackupMultiSpecLimit(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();

        // 1. 机台→规格数统计，用于判断多规格机台
        Map<String, Long> machineSpecCountMap = scheduleList.stream()
                .filter(vo -> vo.getMachineCode() != null && !vo.getMachineCode().isEmpty())
                .collect(Collectors.groupingBy(GsqScheduleResultVo::getMachineCode, Collectors.counting()));

        // 2. 机台信息Map，便于取机台定额
        Map<String, GsqMachineInfo> machineInfoMap = context.getAllMachineList().stream()
                .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, m -> m, (a, b) -> a));

        // 3. 备库规格班次最大班产阈值（SYS1603005）
        GsqScheduleParams params = context.getParams();
        double threshold = params.getBackupMultiSpecThreshold() == null
                ? DEFAULT_BACKUP_MULTI_SPEC_THRESHOLD : params.getBackupMultiSpecThreshold();

        int limitedCount = 0;
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            // 仅处理备库规格
            if (scheduleVo.getBackupTriggerClass() == null || scheduleVo.getBackupTriggerClass() <= 0) {
                continue;
            }
            String machineCode = scheduleVo.getMachineCode();
            if (machineCode == null || machineCode.isEmpty()) {
                continue;
            }
            // 单一规格机台不限制（只受机台定额限制）
            if (machineSpecCountMap.getOrDefault(machineCode, 0L) <= 1) {
                continue;
            }
            // 多规格机台：当班初始排产上限 = min(机台定额, 阈值)
            GsqMachineInfo machine = machineInfoMap.get(machineCode);
            double quota = (machine != null && machine.getQuata() != null) ? machine.getQuata().doubleValue() : 0D;
            double limit = quota > 0 ? Math.min(quota, threshold) : threshold;

            // 逐班次限制当班初始排产，超出部分延后到下一班
            for (int classIndex = 1; classIndex <= 6; classIndex++) {
                double plan = getShiftPlan(scheduleVo, classIndex);
                if (plan <= 0) {
                    continue;
                }
                if (plan <= limit) {
                    continue;
                }
                double overflow = BigDecimalUtil.sub(plan, limit);
                setShiftPlan(scheduleVo, classIndex, limit);
                if (classIndex < 6) {
                    double next = getShiftPlan(scheduleVo, classIndex + 1);
                    setShiftPlan(scheduleVo, classIndex + 1, BigDecimalUtil.add(next, overflow));
                }
                limitedCount++;
                log.info("[S3-备库多规格限制] 规格[{}]机台[{}]{}班 初始排产超阈值, 限[{}]延后[{}]至{}班",
                        scheduleVo.getSteelRingCode(), machineCode, classIndex, limit, overflow, classIndex + 1);
            }
        }
        if (limitedCount > 0) {
            log.info("[S3] 备库多规格机台初始排产限制完成, 限制延后次数:{}", limitedCount);
        }
    }

    /**
     * 设置指定班次的已分配机台编号。
     */
    private void setShiftMachine(GsqScheduleResultVo vo, int classIndex, String machineCode) {
        vo.setFieldValueByFieldName("class" + classIndex + "MachineCode", machineCode);
    }

    /**
     * 设置指定班次的机台定额。
     */
    private void setShiftQuota(GsqScheduleResultVo vo, int classIndex, Double quota) {
        vo.setFieldValueByFieldName("class" + classIndex + "MachineQuota", quota);
    }

    /**
     * 设置指定班次的计划量。
     */
    private void setShiftPlan(GsqScheduleResultVo vo, int classIndex, double value) {
        vo.setFieldValueByFieldName("class" + classIndex + "PlanQty", value);
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
        Object value = vo.getFieldValueByFieldName("class" + classIndex + "Analysis");
        return value == null ? "" : value.toString();
    }

    private void setShiftAnalysis(GsqScheduleResultVo vo, int classIndex, String analysis) {
        vo.setFieldValueByFieldName("class" + classIndex + "Analysis", analysis);
    }

    /**
     * 获取对应胎圈班次的消耗量。
     */
    private double getTqConsume(GsqScheduleResultVo vo, int classIndex) {
        // 钢丝圈N班供应胎圈N+1班
        int tqClassIndex = classIndex + 1;
        Object value = vo.getFieldValueByFieldName("tqClass" + tqClassIndex + "Plan");
        return value == null ? 0 : ((Number) value).doubleValue();
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
        vo.setFieldValueByFieldName("class" + classIndex + "Sequence", sequence);
    }
}
