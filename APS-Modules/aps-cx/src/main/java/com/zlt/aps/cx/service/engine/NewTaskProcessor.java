package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.BalancingResult;
import com.zlt.aps.cx.vo.DailyEmbryoTask;
import com.zlt.aps.cx.vo.EmbryoAssignment;
import com.zlt.aps.cx.vo.MachineAllocationResult;
import com.zlt.aps.cx.vo.MachineAssignment;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.TaskAllocation;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * S5.3.3 新增任务处理器 — 按结构合并任务并调用 {@link BalancingService} 做 DFS 均衡分配。
 *
 * <h3>流水线位置</h3>
 * <pre>
 * CoreScheduleAlgorithmServiceImpl.executeShiftSchedule
 *   → 5.3.1 ContinueTaskProcessor   产出 continueAllocations（可选保底预留）+ 扣减续作 vulcanizeMachineCount
 *   → 5.3.2 TrialTaskProcessor      产出 trialAllocations
 *   → 5.3.3 NewTaskProcessor（本类） 产出 newAllocations
 *   → 5.3.4 合并 continue + new + trial → ShiftScheduleService 精排
 * </pre>
 *
 * <h3>本类职责（与 ContinueTaskProcessor / TrialTaskProcessor 的分工）</h3>
 * <ul>
 *   <li><b>不</b>处理试制任务的机台选择（已由 TrialTaskProcessor 完成）。</li>
 *   <li><b>不</b>在 DFS 内做续作历史保底（SYS04070003=Y 时保底已在 ContinueTaskProcessor 完成；
 *       本类传入 {@code forceKeepHistory=false}，仅通过 {@code continueLoadMap} 继承预扣）。</li>
 *   <li><b>负责</b>：将「新增任务」与「续作剩余 demand&gt;0 任务」按结构合并 → 构建预扣/量试约束 →
 *       调用 DFS → 将 {@link BalancingResult} 转为 {@link MachineAllocationResult}。</li>
 * </ul>
 *
 * <h3>单位约定</h3>
 * <table>
 *   <tr><th>阶段</th><th>字段</th><th>单位</th></tr>
 *   <tr><td>DFS 输入/输出</td><td>{@code DailyEmbryoTask.vulcanizeMachineCount}</td><td>硫化机台数（负荷）</td></tr>
 *   <tr><td>DFS 过滤</td><td>{@code endingExtraInventory}</td><td>条；≤0 不进均衡</td></tr>
 *   <tr><td>结果转换</td><td>{@code EmbryoAssignment.assignedQty}</td><td>硫化机台数 → TaskAllocation.vulcanizeMachineCount</td></tr>
 *   <tr><td>结果转换</td><td>{@code TaskAllocation.quantity}</td><td>条 ← endingExtraInventory（非 assignedQty）</td></tr>
 * </table>
 *
 * <h3>主流程（{@link #processNewTasks}）</h3>
 * <ol>
 *   <li>5.3.3.1 — 按结构分组：newTasks（endingExtraInventory&gt;0）+ 续作剩余（demand&gt;0 且有余量）</li>
 *   <li>5.3.3.2 - 逐结构：候选机台 -> 续作预扣映射 -> 量试约束 -> 机台上限映射 -> DFS -> 结果转换</li>
 * </ol>
 *
 * <h3>续作负荷预/后均衡</h3>
 * <ul>
 *   <li>{@link #preBalanceContinueLoad}：DFS 前调整续作分配，使机台间负荷差和种类差在阈值内。
 *       Phase 1 修正负荷差（移动同胎胚续作任务），Phase 2 修正种类差（交换/移动/三方轮换）。</li>
 *   <li>{@link #postBalanceContinueLoad}：DFS 后基于总负荷（续作+新增）再次均衡。
 *       逻辑同 preBalance，但 loadMap 为 totalLoadMap（续作+新增），continueLoadMap 独立维护。</li>
 *   <li>共享方法：{@link #executeTypeSwap}（两机台间任务交换）、{@link #executeTypeMove}（单向移动）、
 *       {@link #tryThreeWayRotation}（三方轮换）供 pre/post 复用。</li>
 * </ul>
 *
 * @author APS Team
 * @see ContinueTaskProcessor
 * @see BalancingService#balanceEmbryosToMachinesWithMachineCapacity
 * @see DailyEmbryoTask
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewTaskProcessor {

    private final BalancingService balancingService;

    /** 续作均衡最大迭代次数 */
    private static final int MAX_BALANCE_ITERATIONS = 50;

    // ==================== 5.3.3 新增均衡入口 ====================

    /**
     * 处理本班次新增任务及续作剩余需求，按结构调用 DFS 均衡。
     *
     * <p><b>输入合并规则</b>：
     * <ul>
     *   <li>{@code newTasks}：TaskGroupService 的 newTasks 队列，要求 {@code endingExtraInventory &gt; 0}。</li>
     *   <li>{@code continueTasks}：续作列表；仅当 {@code vulcanizeMachineCount &gt; 0} 且 {@code endingExtraInventory &gt; 0}
     *       时并入同结构（保底预留后仍有剩余硫化机 demand 的场景）。</li>
     *   <li>{@code existAllocations}：ContinueTaskProcessor 的 continueAllocations，用于构建 DFS 预扣，<b>不</b>写入本方法返回值。</li>
     *   <li>{@code trialAllocations}：用于量试锁定机台（同胎胚已有试制 → 量试只能上试制机台）。</li>
     * </ul>
     *
     * <p><b>输出</b>：仅包含 DFS 新分配结果的 {@code newAllocations}；续作保底预留仍在 {@code existAllocations} 中，
     * 由 CoreScheduleAlgorithmServiceImpl 5.3.4 与本品输出合并。
     *
     * <p><b>注意</b>：5.3.3.2.4 会对结构内全部任务执行 {@code setIsContinueTask(false)}，续作任务在本路径以
     * 「剩余 demand 的普通均衡任务」身份参与 DFS（isContinue 标志在 TaskAllocation 转换时取自 task 对象当前值）。
     *
     * @param newTasks           TaskGroupService 新增任务列表
     * @param context            排程上下文
     * @param scheduleDate       当前排程日
     * @param dayShifts          当天班次配置（签名保留，当前逻辑未使用）
     * @param continueTasks      续作任务列表（可与 newTasks 合并进均衡）
     * @param existAllocations   续作保底预留结果（continueAllocations）
     * @param trialAllocations   试制分配结果（量试约束来源）
     * @return DFS 均衡产生的新机台分配列表
     */
    public List<MachineAllocationResult> processNewTasks(
            List<DailyEmbryoTask> newTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            List<DailyEmbryoTask> continueTasks,
            List<MachineAllocationResult> existAllocations,
            List<MachineAllocationResult> trialAllocations) {

        List<MachineAllocationResult> allResults = new ArrayList<>();

        log.info("========== 开始处理新增任务，新增={}，续作={} ==========",
                CollectionUtils.isEmpty(newTasks) ? 0 : newTasks.size(),
                CollectionUtils.isEmpty(continueTasks) ? 0 : continueTasks.size());

        // --- 5.3.3.1 按结构分组：收集参与 DFS 的任务 ---
        Map<String, List<DailyEmbryoTask>> structureTaskMap = new LinkedHashMap<>();

        // 5.3.3.1.a 新增任务：无实际条数则不占 DFS 搜索空间与机台种类槽
        if (!CollectionUtils.isEmpty(newTasks)) {
            for (DailyEmbryoTask task : newTasks) {
                Integer endingExtraInventory = task.getEndingExtraInventory();
                if (endingExtraInventory == null || endingExtraInventory <= 0) {
                    log.info("跳过计划量为0的新增任务: 胎胚={}, 物料={}", task.getEmbryoCode(), task.getMaterialCode());
                    continue;
                }
                structureTaskMap.computeIfAbsent(task.getStructureName(), k -> new ArrayList<>()).add(task);
            }
        }

        // 5.3.3.1.b 续作剩余：保底预留后 vulcanizeMachineCount 仍 >0 的任务与新增一并均衡
        if (!CollectionUtils.isEmpty(continueTasks)) {
            for (DailyEmbryoTask task : continueTasks) {
                int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                Integer endingExtraInventory = task.getEndingExtraInventory();
                if (demand > 0 && endingExtraInventory != null && endingExtraInventory > 0) {
                    structureTaskMap.computeIfAbsent(task.getStructureName(), k -> new ArrayList<>()).add(task);
                }
            }
        }

        if (structureTaskMap.isEmpty()) {
            log.info("无新增和续作剩余任务，说明续作任务全部已保底为1且无新增胎胚，跳过均衡");
            return allResults;
        }

        // 续作保底已在 ContinueTaskProcessor 完成；DFS 内 forceKeepHistory 关闭，避免双重预留
        boolean forceKeepHistoryForBalancing = false;

        // --- 5.3.3.2 按结构独立执行一轮「预扣 → 约束 → DFS → 转换」---
        for (Map.Entry<String, List<DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<DailyEmbryoTask> newTasksForStructure = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个参与均衡任务 ---", structureName, newTasksForStructure.size());

            // 5.3.3.2.1 结构候选机台（当日排产配置 ∩ 可用机台，或提前生产回退）
            String productionVersion = newTasksForStructure.get(0).getProductionVersion();
            List<MpCxCapacityConfiguration> availableMachines =
                    context.getAvailableMachinesForStructure(structureName, scheduleDate, productionVersion);
            if (availableMachines.isEmpty()) {
                log.warn("结构 {} 没有可安排的机台，跳过", structureName);
                continue;
            }

            // 5.3.3.2.2 从 continueAllocations 提取本结构的续作预扣（仅同 structureName 的 TaskAllocation）
            Map<String, Set<String>> machineHistoryMap = new HashMap<>();
            Map<String, Integer> continueLoadMap = new HashMap<>();
            Map<String, Set<String>> continueTypeMap = new HashMap<>();
            Map<String, Set<String>> continueLhMachineCodeMap = new HashMap<>();

            if (existAllocations != null) {
                for (MachineAllocationResult allocation : existAllocations) {
                    String machineCode = allocation.getMachineCode();
                    Set<String> embryos = new HashSet<>();
                    Set<String> types = new HashSet<>();
                    Set<String> lhMachineCodes = new HashSet<>();
                    Set<String> countedLoadKeys = new HashSet<>();
                    int load = 0;
                    for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                        if (structureName.equals(taskAlloc.getStructureName())) {
                            embryos.add(taskAlloc.getEmbryoCode());
                            types.add(taskAlloc.getEmbryoCode());
                            // 按 lhMachineCode 去重计算负荷（同一台硫化机L+R只算1台）
                            String lhMc = taskAlloc.getLhMachineCode();
                            String loadKey = (lhMc != null && !lhMc.isEmpty()) ? lhMc : "lhId_" + taskAlloc.getLhId();
                            if (!countedLoadKeys.contains(loadKey)) {
                                countedLoadKeys.add(loadKey);
                                load += taskAlloc.getVulcanizeMachineCount() != null ? taskAlloc.getVulcanizeMachineCount() : 0;
                            }
                            if (lhMc != null && !lhMc.isEmpty()) {
                                lhMachineCodes.add(lhMc);
                            }
                        }
                    }
                    if (!embryos.isEmpty()) {
                        machineHistoryMap.put(machineCode, embryos);
                        continueLoadMap.put(machineCode, load);
                        continueTypeMap.put(machineCode, types);
                        continueLhMachineCodeMap.put(machineCode, lhMachineCodes);
                    }
                }
            }

            // 5.3.3.2.3 量试约束：胎胚 → 试制所在机台（trial / continue 分配 / continueTasks 兜底）
            Map<String, String> trialMachineMap = buildTrialMachineMap(
                    trialAllocations, existAllocations, continueTasks, structureName);

            // 5.3.3.2.4 量试任务打 constrainedMachineCode；全部任务仍进入 balancedTasks 参与 DFS
            List<DailyEmbryoTask> constrainedTrials = new ArrayList<>();
            List<DailyEmbryoTask> balancedTasks = new ArrayList<>();

            for (DailyEmbryoTask task : newTasksForStructure) {
                task.setIsContinueTask(false);
                if (isVolumeTrialConstrained(task, trialMachineMap)) {
                    task.setConstrainedMachineCode(trialMachineMap.get(task.getEmbryoCode()));
                    constrainedTrials.add(task);
                }
                balancedTasks.add(task);
            }

            // 约束量试：将锁定胎胚写入 machineHistoryMap，DFS 候选机台筛选时保留该种类
            for (DailyEmbryoTask constrainedTask : constrainedTrials) {
                String targetMachine = constrainedTask.getConstrainedMachineCode();
                machineHistoryMap.computeIfAbsent(targetMachine, k -> new HashSet<>())
                        .add(constrainedTask.getEmbryoCode());
            }

            List<DailyEmbryoTask> allTasksForStructure = new ArrayList<>(balancedTasks);
            int continueRemaining = countContinueRemaining(continueTasks, structureName);

            log.info("结构 {}：续作预扣机台={}, 续作剩余任务={}, 约束量试={}, 参与DFS总任务={}",
                    structureName, continueLoadMap.size(), continueRemaining,
                    constrainedTrials.size(), allTasksForStructure.size());

            // 5.3.3.2.5 机台维度上限：最大硫化机数、最大胎胚种类数
            Map<String, Integer> machineMaxLhMap = buildMachineMaxLhMap(
                    availableMachines, structureName, context);
            Map<String, Integer> machineMaxEmbryoTypesMap = buildMachineMaxEmbryoTypesMap(
                    availableMachines, structureName, context);

            // 5.3.3.2.5.1 续作负荷预均衡（在 BalancingService 之前调整续作分配，使机台间负荷差 ≤ 阈值）
            int loadDiffThreshold = balancingService.getLoadDiffThreshold(context);
            int typeDiffThreshold = balancingService.getTypeDiffThreshold(context);
            preBalanceContinueLoad(existAllocations, continueLoadMap, continueTypeMap,
                    continueLhMachineCodeMap, loadDiffThreshold, typeDiffThreshold);

            // 5.3.3.2.6 DFS 均衡（10 参数重载：含 continueLoadMap / continueTypeMap / continueLhMachineCodeMap 预扣）
            BalancingResult balancingResult =
                    balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                            allTasksForStructure,
                            availableMachines,
                            machineHistoryMap,
                            machineMaxLhMap,
                            machineMaxEmbryoTypesMap,
                            forceKeepHistoryForBalancing,
                            context,
                            continueLoadMap,
                            continueTypeMap,
                            continueLhMachineCodeMap);

            if (balancingResult == null
                    || CollectionUtils.isEmpty(balancingResult.getAssignments())) {
                log.warn("结构 {} 均衡分配失败，跳过", structureName);
                continue;
            }

            // 5.3.3.2.6.1 后置续作重分配：若均衡后机台间负荷差或种类差仍超阈值，
            // 尝试通过移动或交换续作任务来修正（交换可同时修负荷差和种类差）。
            postBalanceContinueLoad(balancingResult, existAllocations,
                    continueLoadMap, continueTypeMap, continueLhMachineCodeMap,
                    loadDiffThreshold, typeDiffThreshold);

            logFinalBalancingSummary(structureName, balancingResult, existAllocations,
                    continueLoadMap, continueTypeMap, availableMachines,
                    loadDiffThreshold, typeDiffThreshold);

            // 5.3.3.2.7 BalancingResult → MachineAllocationResult（按 EmbryoAssignment 逐条转换，禁止按胎胚合并）
            for (MachineAssignment assignment : balancingResult.getAssignments()) {
                MachineAllocationResult result =
                        new MachineAllocationResult();
                result.setMachineCode(assignment.getMachineCode());
                result.setTaskAllocations(new ArrayList<>());

                int usedCapacity = 0;

                for (EmbryoAssignment embryoAssignment
                        : assignment.getEmbryoAssignments()) {
                    DailyEmbryoTask task = embryoAssignment.getTask();

                    // 续作预扣占位：task=null 表示该机台负荷已在 ContinueTaskProcessor 分配，此处只跳过不写 TaskAllocation
                    if (task == null) {
                        continue;
                    }

                    int assignedQty = embryoAssignment.getAssignedQty();
                    usedCapacity += assignedQty;

                    int taskPlannedQty = task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0
                            ? task.getEndingExtraInventory() : task.getDemandQuantity();
                    TaskAllocation taskAlloc = task.toTaskAllocation(taskPlannedQty, assignedQty);

                    result.getTaskAllocations().add(taskAlloc);
                }

                result.setUsedCapacity(usedCapacity);
                allResults.add(result);
            }

            // 5.3.3.2.8 约束量试分配校验日志（仅当存在约束量试时输出明细）
            if (!constrainedTrials.isEmpty()) {
                for (DailyEmbryoTask ct : constrainedTrials) {
                    boolean trialAssigned = false;
                    for (MachineAllocationResult mr : allResults) {
                        if (mr.getMachineCode().equals(ct.getConstrainedMachineCode())) {
                            for (TaskAllocation ta : mr.getTaskAllocations()) {
                                if (ta.getEmbryoCode().equals(ct.getEmbryoCode())) {
                                    trialAssigned = true;
                                    break;
                                }
                            }
                        }
                        if (trialAssigned) {
                            break;
                        }
                    }
                    if (trialAssigned) {
                        log.info("约束量试任务 {} → 机台 {} (已分配)", ct.getEmbryoCode(), ct.getConstrainedMachineCode());
                    } else {
                        log.warn("约束量试任务 {} → 机台 {} (未分配，约束冲突)", ct.getEmbryoCode(), ct.getConstrainedMachineCode());
                    }
                }
                log.info("均衡后机台分配结果：");
                for (MachineAllocationResult mr : allResults) {
                    List<String> taskDetails = new ArrayList<>();
                    for (TaskAllocation ta : mr.getTaskAllocations()) {
                        String detail = ta.getEmbryoCode() + "[" + ta.getMaterialCode() + "]"
                                + "(" + ta.getVulcanizeMachineCount() + ")";
                        taskDetails.add(detail);
                    }
                    log.info("  机台 {}: {}", mr.getMachineCode(), taskDetails);
                }
            }
        }

        log.info("========== 新增任务处理完成，共 {} 个机台分配 ==========", allResults.size());
        return allResults;
    }

    // ==================== 机台上限映射（传入 BalancingService MachineState） ====================

    /**
     * 统计本结构续作剩余任务数（demand>0 的续作任务个数）。
     */
    private int countContinueRemaining(List<DailyEmbryoTask> continueTasks, String structureName) {
        int count = 0;
        if (!CollectionUtils.isEmpty(continueTasks)) {
            for (DailyEmbryoTask task : continueTasks) {
                if (structureName.equals(task.getStructureName())) {
                    int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                    if (demand > 0) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * 构建「机台编码 -> 机型编码」映射。
     */
    private Map<String, String> buildMachineTypeMap(ScheduleContextVo context) {
        Map<String, String> machineTypeMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }
        return machineTypeMap;
    }

    /**
     * 构建「机型_结构 -> 配比值」映射。
     *
     * @param valueExtractor 从 MdmStructureLhRatio 中提取目标字段（如 lhMachineMaxQty / maxEmbryoQty）
     */
    private Map<String, Integer> buildTypeStructureRatioMap(
            ScheduleContextVo context, Function<MdmStructureLhRatio, Integer> valueExtractor) {
        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                Integer value = valueExtractor.apply(ratio);
                if (value != null) {
                    typeStructureMap.put(key, value);
                }
            }
        }
        return typeStructureMap;
    }

    /**
     * 构建「机台编码 -> 最大硫化机数」映射。
     *
     * <p>查找顺序：机型+结构（{@code MdmStructureLhRatio}）-> 仅结构 -> {@link ScheduleConstants#DEFAULT_MAX_LH_MACHINE_QTY}。
     */
    private Map<String, Integer> buildMachineMaxLhMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        Map<String, String> machineTypeMap = buildMachineTypeMap(context);
        Map<String, Integer> typeStructureMap = buildTypeStructureRatioMap(
                context, MdmStructureLhRatio::getLhMachineMaxQty);

        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);
            String key = machineType + "_" + structureName;
            Integer maxLh = typeStructureMap.get(key);

            if (maxLh == null && context.getStructureLhRatioMap() != null) {
                MdmStructureLhRatio ratioConfig = context.getStructureLhRatioMap().get(structureName);
                if (ratioConfig != null && ratioConfig.getLhMachineMaxQty() != null) {
                    maxLh = ratioConfig.getLhMachineMaxQty();
                }
            }

            if (maxLh == null) {
                maxLh = ScheduleConstants.DEFAULT_MAX_LH_MACHINE_QTY;
            }
            result.put(machineCode, maxLh);
        }

        return result;
    }

    /**
     * 构建「机台编码 → 最大胎胚种类数」映射。
     *
     * <p>查找顺序：机台前缀参数（SYS04040001）→ 机型+结构 → {@code context.maxTypesPerMachine} /
     * {@link ScheduleConstants#DEFAULT_MAX_TYPES_PER_MACHINE}。
     */
    private Map<String, Integer> buildMachineMaxEmbryoTypesMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        Map<String, String> machineTypeMap = buildMachineTypeMap(context);
        Map<String, Integer> typeStructureMap = buildTypeStructureRatioMap(
                context, MdmStructureLhRatio::getMaxEmbryoQty);

        Map<String, Integer> machineMaxEmbryoTypes = context.getMachineMaxEmbryoTypes();

        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);

            Integer maxTypes = null;
            if (machineMaxEmbryoTypes != null && machineCode != null) {
                for (Map.Entry<String, Integer> entry : machineMaxEmbryoTypes.entrySet()) {
                    if (machineCode.startsWith(entry.getKey())) {
                        maxTypes = entry.getValue();
                        log.info("  机台 {} (机型={}): 使用{}专用最大胎胚种类数={}", machineCode, machineType, entry.getKey(), maxTypes);
                        break;
                    }
                }
            }
            if (maxTypes != null) {
                result.put(machineCode, maxTypes);
                continue;
            }

            String key = machineType + "_" + structureName;
            maxTypes = typeStructureMap.get(key);

            if (maxTypes == null && context.getStructureLhRatioMap() != null) {
                MdmStructureLhRatio ratioConfig = context.getStructureLhRatioMap().get(structureName);
                if (ratioConfig != null && ratioConfig.getMaxEmbryoQty() != null) {
                    maxTypes = ratioConfig.getMaxEmbryoQty();
                }
            }

            if (maxTypes == null) {
                maxTypes = context.getMaxTypesPerMachine() != null
                        ? context.getMaxTypesPerMachine() : ScheduleConstants.DEFAULT_MAX_TYPES_PER_MACHINE;
            }
            log.info("  机台 {} (机型={}): 最大胎胚种类数={}", machineCode, machineType, maxTypes);
            result.put(machineCode, maxTypes);
        }

        return result;
    }

    // ==================== 量试约束 ====================

    /**
     * 构建「胎胚编码 → 试制所在机台」映射（仅当前结构）。
     *
     * <p><b>数据来源优先级</b>：
     * <ol>
     *   <li>{@code trialAllocations} — 本班 TrialTaskProcessor 已锁定的试制机台</li>
     *   <li>{@code existAllocations} — 续作保底中的试制任务（putIfAbsent，不覆盖 trial）</li>
     *   <li>{@code continueTasks} — 续作任务上的 continueMachineCodes 兜底（保底预留可能跳过 plannedProduction=0）</li>
     * </ol>
     *
     * <p>供 {@link #isVolumeTrialConstrained} 判定量试是否必须上试制机台。
     */
    private Map<String, String> buildTrialMachineMap(
            List<MachineAllocationResult> trialAllocations,
            List<MachineAllocationResult> existAllocations,
            List<DailyEmbryoTask> continueTasks,
            String structureName) {
        Map<String, String> trialMachineMap = new HashMap<>();
        if (trialAllocations != null) {
            for (MachineAllocationResult allocation : trialAllocations) {
                String machineCode = allocation.getMachineCode();
                for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                    if (structureName.equals(taskAlloc.getStructureName())
                            && taskAlloc.getEmbryoCode() != null
                            && Boolean.TRUE.equals(taskAlloc.getIsTrialTask())) {
                        trialMachineMap.put(taskAlloc.getEmbryoCode(), machineCode);
                    }
                }
            }
        }
        if (existAllocations != null) {
            for (MachineAllocationResult allocation : existAllocations) {
                String machineCode = allocation.getMachineCode();
                for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                    if (structureName.equals(taskAlloc.getStructureName())
                            && taskAlloc.getEmbryoCode() != null
                            && Boolean.TRUE.equals(taskAlloc.getIsTrialTask())) {
                        trialMachineMap.putIfAbsent(taskAlloc.getEmbryoCode(), machineCode);
                    }
                }
            }
        }
        if (continueTasks != null) {
            for (DailyEmbryoTask task : continueTasks) {
                if (structureName.equals(task.getStructureName())
                        && task.getEmbryoCode() != null
                        && Boolean.TRUE.equals(task.getIsTrialTask())
                        && task.getContinueMachineCodes() != null && !task.getContinueMachineCodes().isEmpty()
                        && !trialMachineMap.containsKey(task.getEmbryoCode())) {
                    String preferredMachine = task.getContinueMachineCodes().get(0);
                    trialMachineMap.put(task.getEmbryoCode(), preferredMachine);
                    log.info("buildTrialMachineMap[{}]: 从continueTasks兜底添加 embryoCode={} -> machineCode={}",
                            structureName, task.getEmbryoCode(), preferredMachine);
                }
            }
        }
        if (!trialMachineMap.isEmpty()) {
            log.info("buildTrialMachineMap[{}]: 最终结果 trialMachineMap={}", structureName, trialMachineMap);
        }
        return trialMachineMap;
    }

    /**
     * 量试是否受试制机台约束。
     *
     * <p>条件：{@code isProductionTrial=true} 且同胎胚在 {@code trialMachineMap} 中已有试制机台。
     * 满足时 DFS 仅允许分配到 {@code constrainedMachineCode}。
     */
    private boolean isVolumeTrialConstrained(
            DailyEmbryoTask task,
            Map<String, String> trialMachineMap) {
        if (!Boolean.TRUE.equals(task.getIsProductionTrial())) {
            return false;
        }
        return trialMachineMap.containsKey(task.getEmbryoCode());
    }

    // ==================== 续作负荷预均衡 ====================

    /**
     * 续作负荷预均衡：在调用 BalancingService 之前，检查各机台续作负荷是否均衡，
     * 若超过阈值则将高负荷机台上的续作任务移到低负荷机台（仅移动双方都有的同胎胚）。
     *
     * <p>原理：ContinueTaskProcessor 按历史分配续作任务，可能导致机台间负荷不均
     * （如 H1302=7台、H1403=8台）。若不预均衡，后续新任务分配时会因高负荷机台
     * 容量不足而无法分配，或分配后 gap 超阈值。
     *
     * <p>移动条件：
     * <ul>
     *   <li>高负荷与低负荷机台差值 > loadDiffThreshold</li>
     *   <li>高负荷机台上有续作任务，其胎胚在低负荷机台上也存在（不占新种类槽）</li>
     * </ul>
     *
     * @param existAllocations      续作分配结果（可修改 machineCode 实现重分配）
     * @param continueLoadMap       机台 -> 续作负荷（可变，会被更新）
     * @param continueTypeMap       机台 -> 续作种类集合（可变，会被更新）
     * @param continueLhMachineCodeMap 机台 -> 续作硫化机编号集合（可变，会被更新）
     * @param loadDiffThreshold     负荷差额阈值
     * @param typeDiffThreshold     种类差额阈值
     */
    private void preBalanceContinueLoad(
            List<MachineAllocationResult> existAllocations,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            Map<String, Set<String>> continueLhMachineCodeMap,
            int loadDiffThreshold,
            int typeDiffThreshold) {

        if (existAllocations == null || existAllocations.size() < 2) {
            return;
        }

        for (int iter = 0; iter < MAX_BALANCE_ITERATIONS; iter++) {
            // 找最高和最低负荷机台
            String highMachine = null;
            String lowMachine = null;
            int highLoad = -1;
            int lowLoad = Integer.MAX_VALUE;

            for (Map.Entry<String, Integer> entry : continueLoadMap.entrySet()) {
                String mc = entry.getKey();
                int load = entry.getValue();
                if (load > highLoad) {
                    highLoad = load;
                    highMachine = mc;
                }
                if (load < lowLoad) {
                    lowLoad = load;
                    lowMachine = mc;
                }
            }

            if (highMachine == null || lowMachine == null || highMachine.equals(lowMachine)) {
                break;
            }
            if (highLoad - lowLoad <= loadDiffThreshold) {
                break;
            }

            // 在高负荷机台上找一个续作任务，其胎胚在低负荷机台上也存在
            Set<String> lowTypes = continueTypeMap.getOrDefault(lowMachine, Collections.emptySet());
            if (lowTypes.isEmpty()) {
                break;
            }

            TaskAllocation taskToMove = null;
            MachineAllocationResult highAlloc = null;

            outer:
            for (MachineAllocationResult alloc : existAllocations) {
                if (!highMachine.equals(alloc.getMachineCode())) {
                    continue;
                }
                for (TaskAllocation ta : alloc.getTaskAllocations()) {
                    if (lowTypes.contains(ta.getEmbryoCode())) {
                        // 胎胚在低负荷机台已有，移动不占新种类槽
                        taskToMove = ta;
                        highAlloc = alloc;
                        break outer;
                    }
                }
            }

            if (taskToMove == null) {
                log.info("续作预均衡: {} -> {} 无可移动的同胎胚续作任务，终止", highMachine, lowMachine);
                break;
            }

            // 执行移动：从 highMachine 的 allocation 中移除 taskToMove，添加到 lowMachine 的 allocation
            String embryoCode = taskToMove.getEmbryoCode();
            String lhMc = taskToMove.getLhMachineCode();
            int vulcCount = taskToMove.getVulcanizeMachineCount() != null ? taskToMove.getVulcanizeMachineCount() : 0;

            // 从高负荷机台移除
            highAlloc.getTaskAllocations().remove(taskToMove);

            // 更新高负荷机台的 loadMap
            int highNewLoad = continueLoadMap.getOrDefault(highMachine, 0) - vulcCount;
            continueLoadMap.put(highMachine, Math.max(0, highNewLoad));

            // 更新高负荷机台的 lhMachineCodeMap
            if (lhMc != null && !lhMc.isEmpty()) {
                // 检查是否还有其他任务使用这个 lhMachineCode
                boolean stillUsed = highAlloc.getTaskAllocations().stream()
                        .anyMatch(ta -> lhMc.equals(ta.getLhMachineCode()));
                if (!stillUsed) {
                    Set<String> highLhCodes = continueLhMachineCodeMap.get(highMachine);
                    if (highLhCodes != null) {
                        highLhCodes.remove(lhMc);
                    }
                }
            }

            // 检查高负荷机台是否还保留该胎胚
            boolean highStillHasEmbryo = highAlloc.getTaskAllocations().stream()
                    .anyMatch(ta -> embryoCode.equals(ta.getEmbryoCode()));
            if (!highStillHasEmbryo) {
                Set<String> highTypes = continueTypeMap.get(highMachine);
                if (highTypes != null) {
                    highTypes.remove(embryoCode);
                }
            }

            // 添加到低负荷机台
            final String lowMachineCode = lowMachine;
            MachineAllocationResult lowAlloc = existAllocations.stream()
                    .filter(a -> lowMachineCode.equals(a.getMachineCode()))
                    .findFirst().orElse(null);
            if (lowAlloc == null) {
                lowAlloc = new MachineAllocationResult();
                lowAlloc.setMachineCode(lowMachine);
                lowAlloc.setTaskAllocations(new ArrayList<>());
                existAllocations.add(lowAlloc);
            }
            lowAlloc.getTaskAllocations().add(taskToMove);

            // 更新低负荷机台的 loadMap
            int lowNewLoad = continueLoadMap.getOrDefault(lowMachine, 0) + vulcCount;
            continueLoadMap.put(lowMachine, lowNewLoad);

            // 更新低负荷机台的 typeMap
            Set<String> lowTypeSet = continueTypeMap.computeIfAbsent(lowMachine, k -> new HashSet<>());
            lowTypeSet.add(embryoCode);

            // 更新低负荷机台的 lhMachineCodeMap
            if (lhMc != null && !lhMc.isEmpty()) {
                Set<String> lowLhCodes = continueLhMachineCodeMap.computeIfAbsent(lowMachine, k -> new HashSet<>());
                lowLhCodes.add(lhMc);
            }

            log.info("续作预均衡(负荷): {} 的 {}({}台) -> {}, 负荷: {}->{}, {}->{}",
                    highMachine, embryoCode, vulcCount, lowMachine,
                    highLoad, highNewLoad, lowLoad, lowNewLoad);
        }

        // ---- Phase 2: 种类差修正（通过交换续作任务，保持负荷不变）----
        for (int iter = 0; iter < MAX_BALANCE_ITERATIONS; iter++) {
            // 找种类最多的机台，和所有种类较少的候选机台（按种类数升序）
            String highTypeMachine = null;
            int highTypeCount = -1;
            List<Map.Entry<String, Integer>> lowCandidates = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : continueTypeMap.entrySet()) {
                int count = entry.getValue().size();
                if (count > highTypeCount) {
                    // 之前的最高变成候选（用新高减旧高判断差值）
                    if (highTypeMachine != null && count - highTypeCount > typeDiffThreshold) {
                        lowCandidates.add(new AbstractMap.SimpleEntry<>(highTypeMachine, highTypeCount));
                    }
                    highTypeCount = count;
                    highTypeMachine = entry.getKey();
                } else if (highTypeCount - count > typeDiffThreshold) {
                    lowCandidates.add(new AbstractMap.SimpleEntry<>(entry.getKey(), count));
                }
            }
            lowCandidates.sort(Comparator.comparingInt(Map.Entry::getValue));

            if (highTypeMachine == null || lowCandidates.isEmpty()) {
                break;
            }

            boolean swapped = false;
            Set<String> highEmbryos = continueTypeMap.getOrDefault(highTypeMachine, Collections.emptySet());

            for (Map.Entry<String, Integer> lowCandidate : lowCandidates) {
                String lowTypeMachine = lowCandidate.getKey();
                int lowTypeCount = lowCandidate.getValue();
                Set<String> lowEmbryos = continueTypeMap.getOrDefault(lowTypeMachine, Collections.emptySet());

                // 在 highTypeMachine 上找一个续作任务，其胎胚在 lowTypeMachine 上不存在（独占胎胚）
                TaskAllocation highTaskToSwap = null;
                MachineAllocationResult highSwapAlloc = null;

                outerHigh:
                for (MachineAllocationResult alloc : existAllocations) {
                    if (!highTypeMachine.equals(alloc.getMachineCode())) continue;
                    for (TaskAllocation ta : alloc.getTaskAllocations()) {
                        if (!lowEmbryos.contains(ta.getEmbryoCode())) {
                            highTaskToSwap = ta;
                            highSwapAlloc = alloc;
                            break outerHigh;
                        }
                    }
                }

                if (highTaskToSwap == null) continue;

                // 在 lowTypeMachine 上找一个续作任务，其胎胚在 highTypeMachine 上已存在（交换后不增种类）
                TaskAllocation lowTaskToSwap = null;
                MachineAllocationResult lowSwapAlloc = null;

                final Set<String> highEmbryoSet = highEmbryos;
                for (MachineAllocationResult alloc : existAllocations) {
                    if (!lowTypeMachine.equals(alloc.getMachineCode())) continue;
                    for (TaskAllocation ta : alloc.getTaskAllocations()) {
                        if (highEmbryoSet.contains(ta.getEmbryoCode())) {
                            lowTaskToSwap = ta;
                            lowSwapAlloc = alloc;
                            break;
                        }
                    }
                    if (lowTaskToSwap != null) break;
                }

                if (lowTaskToSwap == null) {
                    // 交换失败（无共有胎胚），尝试直接移动：从高种类机台移独占胎胚到低种类机台
                    if (executeTypeMove(highTaskToSwap, highSwapAlloc,
                            highTypeMachine, lowTypeMachine, existAllocations,
                            continueLoadMap, continueLoadMap,
                            continueTypeMap, continueLhMachineCodeMap,
                            highTypeCount, lowTypeCount, loadDiffThreshold, "续作预均衡")) {
                        swapped = true;
                        break;
                    }
                    // MOVE 也失败，尝试三方轮换
                    if (tryThreeWayRotation(existAllocations, highTaskToSwap, highSwapAlloc,
                            highTypeMachine, lowTypeMachine, highEmbryos,
                            continueTypeMap, continueLoadMap, continueLoadMap,
                            continueTypeMap, continueLhMachineCodeMap,
                            loadDiffThreshold, "续作预均衡", false)) {
                        swapped = true;
                        break;
                    }
                    continue;
                }

                // 检查交换后负荷差是否在阈值内
                int highVulc = highTaskToSwap.getVulcanizeMachineCount() != null ? highTaskToSwap.getVulcanizeMachineCount() : 0;
                int lowVulc = lowTaskToSwap.getVulcanizeMachineCount() != null ? lowTaskToSwap.getVulcanizeMachineCount() : 0;
                int highNewLoad = continueLoadMap.getOrDefault(highTypeMachine, 0) - highVulc + lowVulc;
                int lowNewLoad = continueLoadMap.getOrDefault(lowTypeMachine, 0) - lowVulc + highVulc;
                if (Math.abs(highNewLoad - lowNewLoad) > loadDiffThreshold) continue;

                // 执行交换
                executeTypeSwap(highTaskToSwap, lowTaskToSwap, highSwapAlloc, lowSwapAlloc,
                        highTypeMachine, lowTypeMachine,
                        continueLoadMap, continueLoadMap,
                        continueTypeMap, continueLhMachineCodeMap,
                        highTypeCount, lowTypeCount, "续作预均衡");
                swapped = true;
                break;
            }

            if (!swapped) break;
        }
    }

    /**
     * 后置续作重分配：BalancingService 返回后，若机台间总负荷差（续作+新增）仍超阈值，
     * 将高负荷机台上的续作任务移到低负荷机台（仅移动双方都有的同胎胚，不占新种类槽）。
     *
     * <p>与 {@link #preBalanceContinueLoad} 的区别：
     * <ul>
     *   <li>preBalance 在贪心前运行，只看续作负荷</li>
     *   <li>postBalance 在贪心后运行，看续作+新增的总负荷，能处理贪心分配后产生的不均衡</li>
     * </ul>
     *
     * <p>典型场景：H1302 续作7台/4种(满)，H1403 续作8台+新增1台=9台，gap=2。
     * postBalance 会把 H1403 的续作（如215101877，H1302也有）移到 H1302，
     * 使 H1302=8、H1403=8，gap=0。
     *
     * @param balancingResult        BalancingService 返回的新任务分配结果
     * @param existAllocations       续作分配结果（可修改，移动续作 TaskAllocation）
     * @param continueLoadMap        机台 -> 续作负荷（可变，会被更新）
     * @param continueTypeMap        机台 -> 续作种类集合（可变，会被更新）
     * @param continueLhMachineCodeMap 机台 -> 续作硫化机编号集合（可变，会被更新）
     * @param loadDiffThreshold      负荷差额阈值
     * @param typeDiffThreshold      种类差额阈值
     */
    private void postBalanceContinueLoad(
            BalancingResult balancingResult,
            List<MachineAllocationResult> existAllocations,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            Map<String, Set<String>> continueLhMachineCodeMap,
            int loadDiffThreshold,
            int typeDiffThreshold) {

        if (existAllocations == null || existAllocations.size() < 2 || balancingResult == null) {
            return;
        }

        // 构建总负荷映射 = 续作负荷 + 新增负荷
        Map<String, Integer> totalLoadMap = new HashMap<>(continueLoadMap);
        // 构建各机台全部胎胚集合 = 续作胎胚 + 新增胎胚（用于判断移动续作后是否占新种类槽）
        Map<String, Set<String>> allEmbryosMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : continueTypeMap.entrySet()) {
            allEmbryosMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        for (MachineAssignment ma : balancingResult.getAssignments()) {
            String mc = ma.getMachineCode();
            int newLoad = ma.getEmbryoAssignments().stream()
                    .filter(ea -> ea.getTask() != null)
                    .mapToInt(EmbryoAssignment::getAssignedQty)
                    .sum();
            totalLoadMap.merge(mc, newLoad, Integer::sum);
            Set<String> embryos = allEmbryosMap.computeIfAbsent(mc, k -> new HashSet<>());
            for (EmbryoAssignment ea : ma.getEmbryoAssignments()) {
                if (ea.getTask() != null) {
                    embryos.add(ea.getEmbryoCode());
                }
            }
        }

        for (int iter = 0; iter < MAX_BALANCE_ITERATIONS; iter++) {
            // 找最高和最低总负荷机台
            String highMachine = null;
            String lowMachine = null;
            int highLoad = -1;
            int lowLoad = Integer.MAX_VALUE;

            for (Map.Entry<String, Integer> entry : totalLoadMap.entrySet()) {
                String mc = entry.getKey();
                int load = entry.getValue();
                if (load > highLoad) {
                    highLoad = load;
                    highMachine = mc;
                }
                if (load < lowLoad) {
                    lowLoad = load;
                    lowMachine = mc;
                }
            }

            if (highMachine == null || lowMachine == null || highMachine.equals(lowMachine)) {
                break;
            }
            if (highLoad - lowLoad <= loadDiffThreshold) {
                break;
            }

            // 在高负荷机台上找一个续作任务，其胎胚在低负荷机台上也存在（续作或新增均可）
            Set<String> lowTypes = allEmbryosMap.getOrDefault(lowMachine, Collections.emptySet());
            if (lowTypes.isEmpty()) {
                break;
            }

            TaskAllocation taskToMove = null;
            MachineAllocationResult highAlloc = null;

            outer:
            for (MachineAllocationResult alloc : existAllocations) {
                if (!highMachine.equals(alloc.getMachineCode())) {
                    continue;
                }
                for (TaskAllocation ta : alloc.getTaskAllocations()) {
                    if (lowTypes.contains(ta.getEmbryoCode())) {
                        taskToMove = ta;
                        highAlloc = alloc;
                        break outer;
                    }
                }
            }

            if (taskToMove == null) {
                log.info("后置续作重分配: {} -> {} 无可移动的同胎胚续作任务，终止", highMachine, lowMachine);
                break;
            }

            // 执行移动（同 preBalanceContinueLoad 逻辑）
            String embryoCode = taskToMove.getEmbryoCode();
            String lhMc = taskToMove.getLhMachineCode();
            int vulcCount = taskToMove.getVulcanizeMachineCount() != null ? taskToMove.getVulcanizeMachineCount() : 0;

            // 从高负荷机台移除
            highAlloc.getTaskAllocations().remove(taskToMove);

            int highNewContinueLoad = continueLoadMap.getOrDefault(highMachine, 0) - vulcCount;
            continueLoadMap.put(highMachine, Math.max(0, highNewContinueLoad));
            totalLoadMap.put(highMachine, highLoad - vulcCount);

            if (lhMc != null && !lhMc.isEmpty()) {
                boolean stillUsed = highAlloc.getTaskAllocations().stream()
                        .anyMatch(ta -> lhMc.equals(ta.getLhMachineCode()));
                if (!stillUsed) {
                    Set<String> highLhCodes = continueLhMachineCodeMap.get(highMachine);
                    if (highLhCodes != null) {
                        highLhCodes.remove(lhMc);
                    }
                }
            }

            boolean highStillHasEmbryo = highAlloc.getTaskAllocations().stream()
                    .anyMatch(ta -> embryoCode.equals(ta.getEmbryoCode()));
            if (!highStillHasEmbryo) {
                Set<String> highTypes = continueTypeMap.get(highMachine);
                if (highTypes != null) {
                    highTypes.remove(embryoCode);
                }
            }

            // 添加到低负荷机台
            final String lowMachineCode = lowMachine;
            MachineAllocationResult lowAlloc = existAllocations.stream()
                    .filter(a -> lowMachineCode.equals(a.getMachineCode()))
                    .findFirst().orElse(null);
            if (lowAlloc == null) {
                lowAlloc = new MachineAllocationResult();
                lowAlloc.setMachineCode(lowMachine);
                lowAlloc.setTaskAllocations(new ArrayList<>());
                existAllocations.add(lowAlloc);
            }
            lowAlloc.getTaskAllocations().add(taskToMove);

            int lowNewContinueLoad = continueLoadMap.getOrDefault(lowMachine, 0) + vulcCount;
            continueLoadMap.put(lowMachine, lowNewContinueLoad);
            totalLoadMap.put(lowMachine, lowLoad + vulcCount);

            Set<String> lowTypeSet = continueTypeMap.computeIfAbsent(lowMachine, k -> new HashSet<>());
            lowTypeSet.add(embryoCode);

            if (lhMc != null && !lhMc.isEmpty()) {
                Set<String> lowLhCodes = continueLhMachineCodeMap.computeIfAbsent(lowMachine, k -> new HashSet<>());
                lowLhCodes.add(lhMc);
            }

            log.info("后置续作重分配(负荷): {} 的 {}({}台) -> {}, 总负荷: {}->{}, {}->{}",
                    highMachine, embryoCode, vulcCount, lowMachine,
                    highLoad, highLoad - vulcCount, lowLoad, lowLoad + vulcCount);
        }

        // ---- Phase 2: 种类差修正（通过交换续作任务，保持负荷不变）----
        // 循环外预计算新增任务的胎胚集合（静态，不随迭代变化）
        Map<String, Set<String>> newTaskEmbryosMap = new HashMap<>();
        for (MachineAssignment ma : balancingResult.getAssignments()) {
            Set<String> embryos = newTaskEmbryosMap.computeIfAbsent(ma.getMachineCode(), k -> new HashSet<>());
            for (EmbryoAssignment ea : ma.getEmbryoAssignments()) {
                if (ea.getTask() != null) {
                    embryos.add(ea.getEmbryoCode());
                }
            }
        }

        for (int iter = 0; iter < MAX_BALANCE_ITERATIONS; iter++) {
            // 仅合并 continueTypeMap（变化部分）+ newTaskEmbryosMap（静态部分）
            Map<String, Set<String>> currentAllEmbryos = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : continueTypeMap.entrySet()) {
                Set<String> merged = new HashSet<>(entry.getValue());
                Set<String> newTaskEmbryos = newTaskEmbryosMap.get(entry.getKey());
                if (newTaskEmbryos != null) {
                    merged.addAll(newTaskEmbryos);
                }
                currentAllEmbryos.put(entry.getKey(), merged);
            }
            // 也处理只在 newTaskEmbryosMap 中的机台
            for (Map.Entry<String, Set<String>> entry : newTaskEmbryosMap.entrySet()) {
                if (!currentAllEmbryos.containsKey(entry.getKey())) {
                    currentAllEmbryos.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
            }

            // 找种类最多的机台，和所有种类较少的候选机台（按种类数升序）
            String highTypeMachine = null;
            int highTypeCount = -1;
            List<Map.Entry<String, Integer>> lowCandidates = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : currentAllEmbryos.entrySet()) {
                int count = entry.getValue().size();
                if (count > highTypeCount) {
                    // 之前的最高变成候选（用新高减旧高判断差值）
                    if (highTypeMachine != null && count - highTypeCount > typeDiffThreshold) {
                        lowCandidates.add(new AbstractMap.SimpleEntry<>(highTypeMachine, highTypeCount));
                    }
                    highTypeCount = count;
                    highTypeMachine = entry.getKey();
                } else if (highTypeCount - count > typeDiffThreshold) {
                    lowCandidates.add(new AbstractMap.SimpleEntry<>(entry.getKey(), count));
                }
            }
            // 按种类数升序排列候选（优先和种类最少的交换）
            lowCandidates.sort(Comparator.comparingInt(Map.Entry::getValue));

            if (highTypeMachine == null || lowCandidates.isEmpty()) {
                break;
            }

            // 尝试与每个低种类机台交换，直到成功
            boolean swapped = false;
            Set<String> highEmbryos = currentAllEmbryos.getOrDefault(highTypeMachine, Collections.emptySet());

            for (Map.Entry<String, Integer> lowCandidate : lowCandidates) {
                String lowTypeMachine = lowCandidate.getKey();
                int lowTypeCount = lowCandidate.getValue();

                // 在 highTypeMachine 上找一个续作任务，其胎胚在 lowTypeMachine 上不存在（独占胎胚）
                Set<String> lowEmbryos = currentAllEmbryos.getOrDefault(lowTypeMachine, Collections.emptySet());

                TaskAllocation highTaskToSwap = null;
                MachineAllocationResult highSwapAlloc = null;

                outerHigh:
                for (MachineAllocationResult alloc : existAllocations) {
                    if (!highTypeMachine.equals(alloc.getMachineCode())) {
                        continue;
                    }
                    for (TaskAllocation ta : alloc.getTaskAllocations()) {
                        if (!lowEmbryos.contains(ta.getEmbryoCode())) {
                            highTaskToSwap = ta;
                            highSwapAlloc = alloc;
                            break outerHigh;
                        }
                    }
                }

                if (highTaskToSwap == null) {
                    log.info("后置续作重分配(种类): {} -> {} 无可交换的独占续作胎胚，尝试下一个候选",
                            highTypeMachine, lowTypeMachine);
                    continue;
                }

                // 在 lowTypeMachine 上找一个续作任务，其胎胚在 highTypeMachine 上已存在（交换后不增种类）
                // 且 vulcCount 与 highTaskToSwap 相同（保持负荷不变）
                int targetVulcCount = highTaskToSwap.getVulcanizeMachineCount() != null
                        ? highTaskToSwap.getVulcanizeMachineCount() : 0;

                TaskAllocation lowTaskToSwap = null;
                MachineAllocationResult lowSwapAlloc = null;

                final Set<String> highEmbryoSet = highEmbryos;
                outerLow:
                for (MachineAllocationResult alloc : existAllocations) {
                    if (!lowTypeMachine.equals(alloc.getMachineCode())) {
                        continue;
                    }
                    for (TaskAllocation ta : alloc.getTaskAllocations()) {
                        if (highEmbryoSet.contains(ta.getEmbryoCode())) {
                            int vc = ta.getVulcanizeMachineCount() != null ? ta.getVulcanizeMachineCount() : 0;
                            if (vc == targetVulcCount) {
                                lowTaskToSwap = ta;
                                lowSwapAlloc = alloc;
                                break outerLow;
                            }
                        }
                    }
                }

                // 如果没找到相同 vulcCount 的，找任意 vulcCount 的（允许负荷微调）
                if (lowTaskToSwap == null) {
                    for (MachineAllocationResult alloc : existAllocations) {
                        if (!lowTypeMachine.equals(alloc.getMachineCode())) {
                            continue;
                        }
                        for (TaskAllocation ta : alloc.getTaskAllocations()) {
                            if (highEmbryoSet.contains(ta.getEmbryoCode())) {
                                lowTaskToSwap = ta;
                                lowSwapAlloc = alloc;
                                break;
                            }
                        }
                        if (lowTaskToSwap != null) break;
                    }
                }

                if (lowTaskToSwap == null) {
                    // 交换失败（无共有胎胚），尝试直接移动：从高种类机台移独占胎胚到低种类机台
                    if (executeTypeMove(highTaskToSwap, highSwapAlloc,
                            highTypeMachine, lowTypeMachine, existAllocations,
                            totalLoadMap, continueLoadMap,
                            continueTypeMap, continueLhMachineCodeMap,
                            highTypeCount, lowTypeCount, loadDiffThreshold, "后置续作重分配")) {
                        swapped = true;
                        break;
                    }
                    // MOVE 也失败，尝试三方轮换
                    if (tryThreeWayRotation(existAllocations, highTaskToSwap, highSwapAlloc,
                            highTypeMachine, lowTypeMachine, highEmbryos,
                            currentAllEmbryos, totalLoadMap, continueLoadMap,
                            continueTypeMap, continueLhMachineCodeMap,
                            loadDiffThreshold, "后置续作重分配", true)) {
                        swapped = true;
                        break;
                    }
                    log.info("后置续作重分配(种类): {} -> {} 交换/移动/轮换均失败，尝试下一个候选",
                            highTypeMachine, lowTypeMachine);
                    continue;
                }

                // 检查交换后负荷差是否在阈值内
                int highVulc = highTaskToSwap.getVulcanizeMachineCount() != null ? highTaskToSwap.getVulcanizeMachineCount() : 0;
                int lowVulc = lowTaskToSwap.getVulcanizeMachineCount() != null ? lowTaskToSwap.getVulcanizeMachineCount() : 0;
                int highNewLoad = totalLoadMap.getOrDefault(highTypeMachine, 0) - highVulc + lowVulc;
                int lowNewLoad = totalLoadMap.getOrDefault(lowTypeMachine, 0) - lowVulc + highVulc;
                if (Math.abs(highNewLoad - lowNewLoad) > loadDiffThreshold) {
                    log.info("后置续作重分配(种类): {} <-> {} 交换后负荷差={}超阈值，尝试下一个候选",
                            highTypeMachine, lowTypeMachine, Math.abs(highNewLoad - lowNewLoad));
                    continue;
                }

                // 执行交换
                executeTypeSwap(highTaskToSwap, lowTaskToSwap, highSwapAlloc, lowSwapAlloc,
                        highTypeMachine, lowTypeMachine,
                        totalLoadMap, continueLoadMap,
                        continueTypeMap, continueLhMachineCodeMap,
                        highTypeCount, lowTypeCount, "后置续作重分配");
                swapped = true;
                break;
            } // end for lowCandidates

            if (!swapped) {
                break;
            }
        }
    }

    // ==================== 续作均衡共享方法 ====================

    /**
     * 执行两机台间的续作任务交换，同步更新负荷/种类/lhMachineCode 映射。
     *
     * @param highTaskToSwap      高种类机台上要移走的任务
     * @param lowTaskToSwap       低种类机台上要移走的任务
     * @param highSwapAlloc       高种类机台的分配对象
     * @param lowSwapAlloc        低种类机台的分配对象
     * @param highTypeMachine     高种类机台编码
     * @param lowTypeMachine      低种类机台编码
     * @param loadMap             主负荷映射（会被更新）
     * @param continueLoadMap     续作负荷映射（会被更新）
     * @param continueTypeMap     续作种类集合映射（会被更新）
     * @param continueLhMachineCodeMap 续作硫化机编号集合映射（会被更新）
     * @param highTypeCount       交换前高种类机台种类数
     * @param lowTypeCount        交换前低种类机台种类数
     * @param logPrefix           日志前缀
     */
    private void executeTypeSwap(
            TaskAllocation highTaskToSwap,
            TaskAllocation lowTaskToSwap,
            MachineAllocationResult highSwapAlloc,
            MachineAllocationResult lowSwapAlloc,
            String highTypeMachine,
            String lowTypeMachine,
            Map<String, Integer> loadMap,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            Map<String, Set<String>> continueLhMachineCodeMap,
            int highTypeCount,
            int lowTypeCount,
            String logPrefix) {

        int highVulc = highTaskToSwap.getVulcanizeMachineCount() != null ? highTaskToSwap.getVulcanizeMachineCount() : 0;
        int lowVulc = lowTaskToSwap.getVulcanizeMachineCount() != null ? lowTaskToSwap.getVulcanizeMachineCount() : 0;
        int highNewLoad = loadMap.getOrDefault(highTypeMachine, 0) - highVulc + lowVulc;
        int lowNewLoad = loadMap.getOrDefault(lowTypeMachine, 0) - lowVulc + highVulc;

        String highEmbryo = highTaskToSwap.getEmbryoCode();
        String lowEmbryo = lowTaskToSwap.getEmbryoCode();
        String highLhMc = highTaskToSwap.getLhMachineCode();
        String lowLhMc = lowTaskToSwap.getLhMachineCode();

        // 从各自机台移除
        highSwapAlloc.getTaskAllocations().remove(highTaskToSwap);
        lowSwapAlloc.getTaskAllocations().remove(lowTaskToSwap);

        // 添加到对方机台
        highSwapAlloc.getTaskAllocations().add(lowTaskToSwap);
        lowSwapAlloc.getTaskAllocations().add(highTaskToSwap);

        // 更新负荷
        loadMap.put(highTypeMachine, highNewLoad);
        loadMap.put(lowTypeMachine, lowNewLoad);
        if (loadMap != continueLoadMap) {
            continueLoadMap.merge(highTypeMachine, -highVulc + lowVulc, Integer::sum);
            continueLoadMap.merge(lowTypeMachine, -lowVulc + highVulc, Integer::sum);
        }

        // 更新种类集合
        boolean highStillHas = highSwapAlloc.getTaskAllocations().stream()
                .anyMatch(ta -> highEmbryo.equals(ta.getEmbryoCode()));
        if (!highStillHas) {
            Set<String> highTypes = continueTypeMap.get(highTypeMachine);
            if (highTypes != null) highTypes.remove(highEmbryo);
        }
        boolean lowStillHas = lowSwapAlloc.getTaskAllocations().stream()
                .anyMatch(ta -> lowEmbryo.equals(ta.getEmbryoCode()));
        if (!lowStillHas) {
            Set<String> lowTypes = continueTypeMap.get(lowTypeMachine);
            if (lowTypes != null) lowTypes.remove(lowEmbryo);
        }
        continueTypeMap.computeIfAbsent(lowTypeMachine, k -> new HashSet<>()).add(highEmbryo);

        // 更新 lhMachineCode 集合
        if (highLhMc != null && !highLhMc.isEmpty()) {
            boolean highStillUsesLh = highSwapAlloc.getTaskAllocations().stream()
                    .anyMatch(ta -> highLhMc.equals(ta.getLhMachineCode()));
            if (!highStillUsesLh) {
                Set<String> highLhCodes = continueLhMachineCodeMap.get(highTypeMachine);
                if (highLhCodes != null) highLhCodes.remove(highLhMc);
            }
            continueLhMachineCodeMap.computeIfAbsent(lowTypeMachine, k -> new HashSet<>()).add(highLhMc);
        }
        if (lowLhMc != null && !lowLhMc.isEmpty()) {
            boolean lowStillUsesLh = lowSwapAlloc.getTaskAllocations().stream()
                    .anyMatch(ta -> lowLhMc.equals(ta.getLhMachineCode()));
            if (!lowStillUsesLh) {
                Set<String> lowLhCodes = continueLhMachineCodeMap.get(lowTypeMachine);
                if (lowLhCodes != null) lowLhCodes.remove(lowLhMc);
            }
            continueLhMachineCodeMap.computeIfAbsent(highTypeMachine, k -> new HashSet<>()).add(lowLhMc);
        }

        log.info("{}(种类交换): {} 的 {}({}台) <-> {} 的 {}({}台), 种类: {}/{} -> {}/{}, 负荷: {}/{}",
                logPrefix, highTypeMachine, highEmbryo, highVulc, lowTypeMachine, lowEmbryo, lowVulc,
                highTypeCount, lowTypeCount, highTypeCount - 1, lowTypeCount + 1,
                highNewLoad, lowNewLoad);
    }

    /**
     * 执行续作任务从高种类机台到低种类机台的单向移动，同步更新负荷/种类/lhMachineCode 映射。
     *
     * @param highTaskToSwap      高种类机台上要移走的任务
     * @param highSwapAlloc       高种类机台的分配对象
     * @param highTypeMachine     高种类机台编码
     * @param lowTypeMachine      低种类机台编码
     * @param existAllocations    所有机台分配结果（用于查找/创建低种类机台分配对象）
     * @param loadMap             主负荷映射（会被更新）
     * @param continueLoadMap     续作负荷映射（会被更新）
     * @param continueTypeMap     续作种类集合映射（会被更新）
     * @param continueLhMachineCodeMap 续作硫化机编号集合映射（会被更新）
     * @param highTypeCount       移动前高种类机台种类数
     * @param lowTypeCount        移动前低种类机台种类数
     * @param loadDiffThreshold   负荷差额阈值
     * @param logPrefix           日志前缀
     * @return true 如果移动成功
     */
    private boolean executeTypeMove(
            TaskAllocation highTaskToSwap,
            MachineAllocationResult highSwapAlloc,
            String highTypeMachine,
            String lowTypeMachine,
            List<MachineAllocationResult> existAllocations,
            Map<String, Integer> loadMap,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            Map<String, Set<String>> continueLhMachineCodeMap,
            int highTypeCount,
            int lowTypeCount,
            int loadDiffThreshold,
            String logPrefix) {

        int moveVulc = highTaskToSwap.getVulcanizeMachineCount() != null ? highTaskToSwap.getVulcanizeMachineCount() : 0;
        int highMoveLoad = loadMap.getOrDefault(highTypeMachine, 0) - moveVulc;
        int lowMoveLoad = loadMap.getOrDefault(lowTypeMachine, 0) + moveVulc;
        if (Math.abs(highMoveLoad - lowMoveLoad) > loadDiffThreshold) {
            return false;
        }

        String moveEmbryo = highTaskToSwap.getEmbryoCode();
        String moveLhMc = highTaskToSwap.getLhMachineCode();

        highSwapAlloc.getTaskAllocations().remove(highTaskToSwap);
        MachineAllocationResult lowMoveAlloc = existAllocations.stream()
                .filter(a -> lowTypeMachine.equals(a.getMachineCode()))
                .findFirst().orElse(null);
        if (lowMoveAlloc == null) {
            lowMoveAlloc = new MachineAllocationResult();
            lowMoveAlloc.setMachineCode(lowTypeMachine);
            lowMoveAlloc.setTaskAllocations(new ArrayList<>());
            existAllocations.add(lowMoveAlloc);
        }
        lowMoveAlloc.getTaskAllocations().add(highTaskToSwap);

        loadMap.put(highTypeMachine, highMoveLoad);
        loadMap.put(lowTypeMachine, lowMoveLoad);
        if (loadMap != continueLoadMap) {
            continueLoadMap.merge(highTypeMachine, -moveVulc, Integer::sum);
            continueLoadMap.merge(lowTypeMachine, moveVulc, Integer::sum);
        }

        // 更新种类集合
        boolean highStillHasMove = highSwapAlloc.getTaskAllocations().stream()
                .anyMatch(ta -> moveEmbryo.equals(ta.getEmbryoCode()));
        if (!highStillHasMove) {
            Set<String> highTypes = continueTypeMap.get(highTypeMachine);
            if (highTypes != null) highTypes.remove(moveEmbryo);
        }
        continueTypeMap.computeIfAbsent(lowTypeMachine, k -> new HashSet<>()).add(moveEmbryo);

        // 更新 lhMachineCode 集合
        if (moveLhMc != null && !moveLhMc.isEmpty()) {
            boolean highStillUsesLh = highSwapAlloc.getTaskAllocations().stream()
                    .anyMatch(ta -> moveLhMc.equals(ta.getLhMachineCode()));
            if (!highStillUsesLh) {
                Set<String> highLhCodes = continueLhMachineCodeMap.get(highTypeMachine);
                if (highLhCodes != null) highLhCodes.remove(moveLhMc);
            }
            continueLhMachineCodeMap.computeIfAbsent(lowTypeMachine, k -> new HashSet<>()).add(moveLhMc);
        }

        log.info("{}(种类移动): {} 的 {}({}台) -> {}, 种类: {}/{} -> {}/{}, 负荷: {}/{}",
                logPrefix, highTypeMachine, moveEmbryo, moveVulc, lowTypeMachine,
                highTypeCount, lowTypeCount, highTypeCount - 1, lowTypeCount + 1,
                highMoveLoad, lowMoveLoad);
        return true;
    }

    /**
     * 尝试三方轮换：A(高种类)给B(低种类)独占胎胚X，B给C独占胎胚Y，C给A共有胎胚Z。
     * 净效果：A种类-1，B和C种类不变，负荷可能微调（需在阈值内）。
     *
     * @param existAllocations       所有机台分配结果
     * @param highTaskToSwap         高种类机台上要移走的任务（胎胚X）
     * @param highSwapAlloc          高种类机台的分配对象
     * @param highTypeMachine        高种类机台编码
     * @param lowTypeMachine         低种类机台编码
     * @param highEmbryos            高种类机台的胎胚集合
     * @param typeMapForLookup       用于查找C机台胎胚集合的映射（pre: continueTypeMap, post: currentAllEmbryos）
     * @param loadMap                主负荷映射（pre: continueLoadMap, post: totalLoadMap）
     * @param continueLoadMap        续作负荷映射（pre: 同loadMap, post: 独立映射）
     * @param continueTypeMap        续作种类集合映射（用于更新种类）
     * @param continueLhMachineCodeMap 续作硫化机编号集合映射
     * @param loadDiffThreshold      负荷差额阈值
     * @param logPrefix              日志前缀
     * @param updateContinueLoadMap  是否同步更新 continueLoadMap（pre: false, post: true）
     * @return true if rotation succeeded
     */
    private boolean tryThreeWayRotation(
            List<MachineAllocationResult> existAllocations,
            TaskAllocation highTaskToSwap,
            MachineAllocationResult highSwapAlloc,
            String highTypeMachine,
            String lowTypeMachine,
            Set<String> highEmbryos,
            Map<String, Set<String>> typeMapForLookup,
            Map<String, Integer> loadMap,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            Map<String, Set<String>> continueLhMachineCodeMap,
            int loadDiffThreshold,
            String logPrefix,
            boolean updateContinueLoadMap) {

        String embryoX = highTaskToSwap.getEmbryoCode();
        int vulcX = highTaskToSwap.getVulcanizeMachineCount() != null ? highTaskToSwap.getVulcanizeMachineCount() : 0;

        MachineAllocationResult allocB = existAllocations.stream()
                .filter(a -> lowTypeMachine.equals(a.getMachineCode()))
                .findFirst().orElse(null);
        if (allocB == null) return false;

        for (MachineAllocationResult allocC : existAllocations) {
            String machineC = allocC.getMachineCode();
            if (machineC.equals(highTypeMachine) || machineC.equals(lowTypeMachine)) continue;

            Set<String> embryosC = typeMapForLookup.getOrDefault(machineC, Collections.emptySet());

            for (TaskAllocation taskZ : allocC.getTaskAllocations()) {
                String embryoZ = taskZ.getEmbryoCode();
                if (!highEmbryos.contains(embryoZ)) continue;

                int vulcZ = taskZ.getVulcanizeMachineCount() != null ? taskZ.getVulcanizeMachineCount() : 0;

                for (TaskAllocation taskY : allocB.getTaskAllocations()) {
                    String embryoY = taskY.getEmbryoCode();
                    if (embryosC.contains(embryoY) || embryoY.equals(embryoX)) continue;

                    int vulcY = taskY.getVulcanizeMachineCount() != null ? taskY.getVulcanizeMachineCount() : 0;

                    int loadA = loadMap.getOrDefault(highTypeMachine, 0) - vulcX + vulcZ;
                    int loadB = loadMap.getOrDefault(lowTypeMachine, 0) + vulcX - vulcY;
                    int loadC = loadMap.getOrDefault(machineC, 0) + vulcY - vulcZ;
                    int newMax = Math.max(Math.max(loadA, loadB), loadC);
                    int newMin = Math.min(Math.min(loadA, loadB), loadC);
                    for (Map.Entry<String, Integer> e : loadMap.entrySet()) {
                        if (e.getKey().equals(highTypeMachine) || e.getKey().equals(lowTypeMachine) || e.getKey().equals(machineC)) continue;
                        newMax = Math.max(newMax, e.getValue());
                        newMin = Math.min(newMin, e.getValue());
                    }
                    if (newMax - newMin > loadDiffThreshold) continue;

                    // 执行三方轮换
                    highSwapAlloc.getTaskAllocations().remove(highTaskToSwap);
                    allocB.getTaskAllocations().remove(taskY);
                    allocC.getTaskAllocations().remove(taskZ);
                    highSwapAlloc.getTaskAllocations().add(taskZ);
                    allocB.getTaskAllocations().add(highTaskToSwap);
                    allocC.getTaskAllocations().add(taskY);

                    loadMap.put(highTypeMachine, loadA);
                    loadMap.put(lowTypeMachine, loadB);
                    loadMap.put(machineC, loadC);
                    if (updateContinueLoadMap) {
                        continueLoadMap.merge(highTypeMachine, -vulcX + vulcZ, Integer::sum);
                        continueLoadMap.merge(lowTypeMachine, vulcX - vulcY, Integer::sum);
                        continueLoadMap.merge(machineC, vulcY - vulcZ, Integer::sum);
                    }

                    // 更新种类集合
                    Set<String> aTypes = continueTypeMap.get(highTypeMachine);
                    boolean aStillHasX = highSwapAlloc.getTaskAllocations().stream()
                            .anyMatch(ta -> embryoX.equals(ta.getEmbryoCode()));
                    if (!aStillHasX) {
                        if (aTypes != null) aTypes.remove(embryoX);
                    }
                    continueTypeMap.computeIfAbsent(lowTypeMachine, k -> new HashSet<>()).add(embryoX);
                    boolean bStillHasY = allocB.getTaskAllocations().stream()
                            .anyMatch(ta -> embryoY.equals(ta.getEmbryoCode()));
                    if (!bStillHasY) {
                        Set<String> bTypes = continueTypeMap.get(lowTypeMachine);
                        if (bTypes != null) bTypes.remove(embryoY);
                    }
                    continueTypeMap.computeIfAbsent(machineC, k -> new HashSet<>()).add(embryoY);
                    boolean cStillHasZ = allocC.getTaskAllocations().stream()
                            .anyMatch(ta -> embryoZ.equals(ta.getEmbryoCode()));
                    if (!cStillHasZ) {
                        Set<String> cTypes = continueTypeMap.get(machineC);
                        if (cTypes != null) cTypes.remove(embryoZ);
                    }

                    // 更新 lhMachineCode 集合
                    updateLhAfterRotation(continueLhMachineCodeMap, highSwapAlloc, highTypeMachine, highTaskToSwap.getLhMachineCode(), lowTypeMachine);
                    updateLhAfterRotation(continueLhMachineCodeMap, allocB, lowTypeMachine, taskY.getLhMachineCode(), machineC);
                    updateLhAfterRotation(continueLhMachineCodeMap, allocC, machineC, taskZ.getLhMachineCode(), highTypeMachine);

                    log.info("{}(三方轮换): {}的{}({}台)->{}, {}的{}({}台)->{}, {}的{}({}台)->{}, 负荷: {}/{}/{}",
                            logPrefix, highTypeMachine, embryoX, vulcX, lowTypeMachine,
                            lowTypeMachine, embryoY, vulcY, machineC,
                            machineC, embryoZ, vulcZ, highTypeMachine,
                            loadA, loadB, loadC);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 三方轮换后更新 lhMachineCode 集合。
     * 从 fromMachine 移走任务后，若该 lhMachineCode 不再被使用则移除；添加到 toMachine。
     */
    private void updateLhAfterRotation(
            Map<String, Set<String>> continueLhMachineCodeMap,
            MachineAllocationResult fromAlloc, String fromMachine,
            String movedLhMc, String toMachine) {
        if (movedLhMc == null || movedLhMc.isEmpty()) {
            return;
        }
        // 检查 fromMachine 是否还有其他任务使用这个 lhMachineCode
        boolean stillUsed = fromAlloc.getTaskAllocations().stream()
                .anyMatch(ta -> movedLhMc.equals(ta.getLhMachineCode()));
        if (!stillUsed) {
            Set<String> fromLhCodes = continueLhMachineCodeMap.get(fromMachine);
            if (fromLhCodes != null) {
                fromLhCodes.remove(movedLhMc);
            }
        }
        // 添加到 toMachine
        continueLhMachineCodeMap.computeIfAbsent(toMachine, k -> new HashSet<>()).add(movedLhMc);
    }

    /**
     * 打印结构级均衡最终结果：算法路径、新增分配阶段 P1、后置续作调整后的各机台续作+新增明细及最终负荷/种类差。
     */
    private void logFinalBalancingSummary(
            String structureName,
            BalancingResult balancingResult,
            List<MachineAllocationResult> existAllocations,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            List<MpCxCapacityConfiguration> availableMachines,
            int loadDiffThreshold,
            int typeDiffThreshold) {

        log.info("====== 【均衡最终结果】结构 {} ======", structureName);
        log.info("  算法路径: {}", describeAlgorithmPath(balancingResult));
        log.info("  新增任务分配阶段: 全部分配={}, 负荷差={}, 种类差={}, P1={}",
                balancingResult.getAllAssigned(),
                balancingResult.getLoadGap(),
                balancingResult.getTypeGap(),
                Boolean.TRUE.equals(balancingResult.getP1Satisfied()) ? "满足" : "未满足");

        Map<String, Map<String, Integer>> newTaskByMachine = new LinkedHashMap<>();
        Map<String, Integer> newLoadByMachine = new HashMap<>();
        if (balancingResult.getAssignments() != null) {
            for (MachineAssignment ma : balancingResult.getAssignments()) {
                String machineCode = ma.getMachineCode();
                Map<String, Integer> embryoCounts = newTaskByMachine.computeIfAbsent(machineCode, k -> new LinkedHashMap<>());
                int newLoad = 0;
                if (ma.getEmbryoAssignments() != null) {
                    for (EmbryoAssignment ea : ma.getEmbryoAssignments()) {
                        embryoCounts.merge(ea.getEmbryoCode(), ea.getAssignedQty(), Integer::sum);
                        newLoad += ea.getAssignedQty();
                    }
                }
                newLoadByMachine.put(machineCode, newLoad);
            }
        }

        Set<String> allMachines = new TreeSet<>();
        if (availableMachines != null) {
            for (MpCxCapacityConfiguration config : availableMachines) {
                if (config.getCxMachineCode() != null) {
                    allMachines.add(config.getCxMachineCode());
                }
            }
        }
        allMachines.addAll(continueLoadMap.keySet());
        allMachines.addAll(newTaskByMachine.keySet());

        int minLoad = Integer.MAX_VALUE;
        int maxLoad = 0;
        int minTypes = Integer.MAX_VALUE;
        int maxTypes = 0;

        for (String machine : allMachines) {
            int continueLoad = continueLoadMap.getOrDefault(machine, 0);
            Map<String, Integer> continueEmbryoCounts = summarizeContinueEmbryos(existAllocations, machine);
            Map<String, Integer> newCounts = newTaskByMachine.getOrDefault(machine, Collections.emptyMap());
            int newLoad = newLoadByMachine.getOrDefault(machine, 0);
            int totalLoad = continueLoad + newLoad;

            Set<String> allTypes = new HashSet<>(continueTypeMap.getOrDefault(machine, Collections.emptySet()));
            allTypes.addAll(newCounts.keySet());

            minLoad = Math.min(minLoad, totalLoad);
            maxLoad = Math.max(maxLoad, totalLoad);
            minTypes = Math.min(minTypes, allTypes.size());
            maxTypes = Math.max(maxTypes, allTypes.size());

            String continueDetail = formatEmbryoCounts(continueEmbryoCounts);
            String newDetail = formatEmbryoCounts(newCounts);
            String typeList = allTypes.isEmpty() ? "无" : String.join(",", new TreeSet<>(allTypes));

            log.info("  机台 {}: 续作{}台{} + 新增{}台{} => 合计{}台/{}种 [{}]",
                    machine, continueLoad,
                    continueDetail.isEmpty() ? "" : continueDetail,
                    newLoad,
                    newDetail.isEmpty() ? "" : newDetail,
                    totalLoad, allTypes.size(), typeList);
        }

        int finalLoadGap = allMachines.isEmpty() ? 0 : maxLoad - minLoad;
        int finalTypeGap = allMachines.isEmpty() ? 0 : maxTypes - minTypes;
        boolean finalP1 = finalLoadGap <= loadDiffThreshold
                && finalTypeGap <= typeDiffThreshold
                && Boolean.TRUE.equals(balancingResult.getAllAssigned());

        log.info("  后置续作调整后: 负荷差={}(阈值{}), 种类差={}(阈值{}), 最终P1={}",
                finalLoadGap, loadDiffThreshold, finalTypeGap, typeDiffThreshold,
                finalP1 ? "满足" : "未满足");
        log.info("====== 【均衡最终结果】结构 {} 结束 ======", structureName);
    }

    private String describeAlgorithmPath(BalancingResult balancingResult) {
        if (balancingResult == null || balancingResult.getAlgorithmPath() == null) {
            return "未知";
        }
        switch (balancingResult.getAlgorithmPath()) {
            case "GREEDY_R1":
                return "贪心R1（一次满足P1）";
            case "GREEDY_R2":
                return "贪心R1未满足 → 贪心R2（突破容量上限+局部搜索）";
            default:
                return balancingResult.getAlgorithmPath();
        }
    }

    private Map<String, Integer> summarizeContinueEmbryos(List<MachineAllocationResult> existAllocations, String machineCode) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (MachineAllocationResult alloc : existAllocations) {
            if (!machineCode.equals(alloc.getMachineCode())) {
                continue;
            }
            for (TaskAllocation taskAllocation : alloc.getTaskAllocations()) {
                int vulcCount = taskAllocation.getVulcanizeMachineCount() != null
                        ? taskAllocation.getVulcanizeMachineCount() : 0;
                counts.merge(taskAllocation.getEmbryoCode(), vulcCount, Integer::sum);
            }
        }
        return counts;
    }

    private String formatEmbryoCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "";
        }
        return "[" + counts.entrySet().stream()
                .map(entry -> entry.getKey() + "×" + entry.getValue())
                .collect(Collectors.joining(", ")) + "]";
    }
}
