package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
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
 *       调用 DFS → 将 {@link BalancingService.BalancingResult} 转为 {@link CoreScheduleAlgorithmService.MachineAllocationResult}。</li>
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
 *   <li>5.3.3.2 — 逐结构：候选机台 → 续作预扣映射 → 量试约束 → 机台上限映射 → DFS → 结果转换</li>
 * </ol>
 *
 * @author APS Team
 * @see ContinueTaskProcessor
 * @see BalancingService#balanceEmbryosToMachinesWithMachineCapacity
 * @see CoreScheduleAlgorithmService.DailyEmbryoTask
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewTaskProcessor {

    private final BalancingService balancingService;

    /** 配比缺失时单台默认最大硫化机数（传入 BalancingService 机台 maxCapacity） */
    private static final int DEFAULT_MAX_LH_MACHINE_COUNT = 10;

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
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> trialAllocations) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> allResults = new ArrayList<>();

        log.info("========== 开始处理新增任务，新增={}，续作={} ==========",
                CollectionUtils.isEmpty(newTasks) ? 0 : newTasks.size(),
                CollectionUtils.isEmpty(continueTasks) ? 0 : continueTasks.size());

        // --- 5.3.3.1 按结构分组：收集参与 DFS 的任务 ---
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap = new LinkedHashMap<>();

        // 5.3.3.1.a 新增任务：无实际条数则不占 DFS 搜索空间与机台种类槽
        if (!CollectionUtils.isEmpty(newTasks)) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : newTasks) {
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
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
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
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasksForStructure = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个参与均衡任务 ---", structureName, newTasksForStructure.size());

            // 5.3.3.2.1 结构候选机台（当日排产配置 ∩ 可用机台，或提前生产回退）
            String productionVersion = newTasksForStructure.get(0).getProductionVersion();
            List<MpCxCapacityConfiguration> availableMachines =
                    getAvailableMachinesForStructure(structureName, scheduleDate, context, productionVersion);
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
                for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
                    String machineCode = allocation.getMachineCode();
                    Set<String> embryos = new HashSet<>();
                    Set<String> types = new HashSet<>();
                    Set<String> lhMachineCodes = new HashSet<>();
                    Set<String> countedLoadKeys = new HashSet<>();
                    int load = 0;
                    for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
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
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> constrainedTrials = new ArrayList<>();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> balancedTasks = new ArrayList<>();

            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : newTasksForStructure) {
                task.setIsContinueTask(false);
                if (isVolumeTrialConstrained(task, trialMachineMap)) {
                    task.setConstrainedMachineCode(trialMachineMap.get(task.getEmbryoCode()));
                    constrainedTrials.add(task);
                }
                balancedTasks.add(task);
            }

            // 约束量试：将锁定胎胚写入 machineHistoryMap，DFS 候选机台筛选时保留该种类
            for (CoreScheduleAlgorithmService.DailyEmbryoTask constrainedTask : constrainedTrials) {
                String targetMachine = constrainedTask.getConstrainedMachineCode();
                machineHistoryMap.computeIfAbsent(targetMachine, k -> new HashSet<>())
                        .add(constrainedTask.getEmbryoCode());
            }

            List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForStructure = new ArrayList<>(balancedTasks);

            // 日志：统计本结构续作剩余条数（demand>0 的续作任务个数）
            int continueRemaining = 0;
            if (!CollectionUtils.isEmpty(continueTasks)) {
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
                    if (structureName.equals(task.getStructureName())) {
                        int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                        if (demand > 0) {
                            continueRemaining++;
                        }
                    }
                }
            }

            log.info("结构 {}：续作预扣机台={}, 续作剩余任务={}, 约束量试={}, 参与DFS总任务={}",
                    structureName, continueLoadMap.size(), continueRemaining,
                    constrainedTrials.size(), allTasksForStructure.size());

            // 5.3.3.2.5 机台维度上限：最大硫化机数、最大胎胚种类数 → 传入 BalancingService
            Map<String, Integer> machineMaxLhMap = buildMachineMaxLhMap(
                    availableMachines, structureName, context);
            Map<String, Integer> machineMaxEmbryoTypesMap = buildMachineMaxEmbryoTypesMap(
                    availableMachines, structureName, context);

            // 5.3.3.2.6 DFS 均衡（10 参数重载：含 continueLoadMap / continueTypeMap / continueLhMachineCodeMap 预扣）
            BalancingService.BalancingResult balancingResult =
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

            log.info("结构 {} 均衡分配完成", structureName);

            // 5.3.3.2.7 BalancingResult → MachineAllocationResult（按 EmbryoAssignment 逐条转换，禁止按胎胚合并）
            for (BalancingService.MachineAssignment assignment : balancingResult.getAssignments()) {
                CoreScheduleAlgorithmService.MachineAllocationResult result =
                        new CoreScheduleAlgorithmService.MachineAllocationResult();
                result.setMachineCode(assignment.getMachineCode());
                result.setTaskAllocations(new ArrayList<>());

                int usedCapacity = 0;

                for (BalancingService.EmbryoAssignment embryoAssignment
                        : assignment.getEmbryoAssignments()) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = embryoAssignment.getTask();

                    // 续作预扣占位：task=null 表示该机台负荷已在 ContinueTaskProcessor 分配，此处只跳过不写 TaskAllocation
                    if (task == null) {
                        continue;
                    }

                    int assignedQty = embryoAssignment.getAssignedQty();
                    usedCapacity += assignedQty;

                    CoreScheduleAlgorithmService.TaskAllocation taskAlloc =
                            new CoreScheduleAlgorithmService.TaskAllocation();
                    taskAlloc.setEmbryoCode(task.getEmbryoCode());
                    taskAlloc.setMaterialCode(task.getMaterialCode());
                    taskAlloc.setMaterialDesc(task.getMaterialDesc());
                    taskAlloc.setMainMaterialDesc(task.getMainMaterialDesc());
                    taskAlloc.setStructureName(task.getStructureName());
                    int taskPlannedQty = task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0
                            ? task.getEndingExtraInventory() : task.getDemandQuantity();
                    taskAlloc.setQuantity(taskPlannedQty);
                    taskAlloc.setVulcanizeMachineCount(assignedQty);
                    taskAlloc.setEndingExtraInventory(task.getEndingExtraInventory());
                    taskAlloc.setPriority(task.getPriority());
                    taskAlloc.setStockHours(task.getStockHours());
                    taskAlloc.setIsTrialTask(task.getIsTrialTask());
                    taskAlloc.setIsProductionTrial(task.getIsProductionTrial());
                    taskAlloc.setIsContinueTask(task.getIsContinueTask());
                    taskAlloc.setIsEndingTask(task.getIsEndingTask());
                    taskAlloc.setEndingSurplusQty(task.getEndingSurplusQty());
                    taskAlloc.setIsMainProduct(task.getIsMainProduct());
                    taskAlloc.setLhId(task.getLhId());
                    taskAlloc.setIsLastEndingBatch(task.getIsLastEndingBatch());
                    taskAlloc.setIsEndProduction(task.getIsEndProduction());
                    taskAlloc.setEndingAbandoned(task.getEndingAbandoned());
                    taskAlloc.setIsOpeningDayTask(task.getIsOpeningDayTask());
                    taskAlloc.setIsClosingDayTask(task.getIsClosingDayTask());
                    taskAlloc.setConstructionStage(task.getConstructionStage());
                    taskAlloc.setIsFirstTask(task.getIsFirstTask());
                    taskAlloc.setIsUrgentEnding(task.getIsUrgentEnding());
                    taskAlloc.setIsNearEnding(task.getIsNearEnding());

                    result.getTaskAllocations().add(taskAlloc);
                }

                result.setUsedCapacity(usedCapacity);
                allResults.add(result);
            }

            // 5.3.3.2.8 约束量试分配校验日志（仅当存在约束量试时输出明细）
            if (!constrainedTrials.isEmpty()) {
                for (CoreScheduleAlgorithmService.DailyEmbryoTask ct : constrainedTrials) {
                    boolean trialAssigned = false;
                    for (CoreScheduleAlgorithmService.MachineAllocationResult mr : allResults) {
                        if (mr.getMachineCode().equals(ct.getConstrainedMachineCode())) {
                            for (CoreScheduleAlgorithmService.TaskAllocation ta : mr.getTaskAllocations()) {
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
                for (CoreScheduleAlgorithmService.MachineAllocationResult mr : allResults) {
                    List<String> taskDetails = new ArrayList<>();
                    for (CoreScheduleAlgorithmService.TaskAllocation ta : mr.getTaskAllocations()) {
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

    // ==================== 结构可用机台解析（与 ContinueTaskProcessor 同构，多 availableMachines 交集） ====================

    /**
     * 获取某结构在排程日可参与 DFS 的成型机台列表。
     *
     * <p><b>过滤链</b>：
     * <ol>
     *   <li>{@code structureAllocationMap}：年月 + 月内日区间（beginDay~endDay）</li>
     *   <li>机台编码必须存在于 {@code context.availableMachines}（主数据启用机台）</li>
     *   <li>按机台编码去重</li>
     *   <li>无结果时回退 {@link #getAdvanceProductionMachines}</li>
     * </ol>
     *
     * <p>排产版本已在 context 加载阶段按各月配置过滤，此处不再按 productionVersion 二次过滤。
     */
    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextVo context,
            String productionVersion) {
        Set<String> availableMachineCodes = new HashSet<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                availableMachineCodes.add(machine.getCxMachineCode());
            }
        }

        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs =
                    context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                int dayOfMonth = scheduleDate.getDayOfMonth();
                int dateYear = scheduleDate.getYear();
                int dateMonth = scheduleDate.getMonthValue();

                List<MpCxCapacityConfiguration> result = configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= dayOfMonth && c.getEndDay() >= dayOfMonth)
                        .filter(c -> c.getYear() != null && c.getYear() == dateYear
                                && c.getMonth() != null && c.getMonth() == dateMonth)
                        .filter(c -> availableMachineCodes.contains(c.getCxMachineCode()))
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(MpCxCapacityConfiguration::getCxMachineCode, c -> c, (a, b) -> a, LinkedHashMap::new),
                                m -> new ArrayList<>(m.values())));
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return getAdvanceProductionMachines(structureName, context, productionVersion);
    }

    /**
     * 提前生产机台回退（当日 structureAllocationMap 无机台时）。
     *
     * <p>数据由 TaskGroupService 写入 {@code advanceProductionMachineMap}，已做版本与占用冲突处理；
     * {@code productionVersion} 参数保留签名兼容，本方法内不再过滤。
     */
    private List<MpCxCapacityConfiguration> getAdvanceProductionMachines(
            String structureName, ScheduleContextVo context, String productionVersion) {
        if (context.getAdvanceProductionMachineMap() != null) {
            List<MpCxCapacityConfiguration> advanceMachines =
                    context.getAdvanceProductionMachineMap().get(structureName);
            if (advanceMachines != null && !advanceMachines.isEmpty()) {
                log.info("【提前生产】NewTaskProcessor 结构={} 使用提前生产机台={}",
                        structureName,
                        advanceMachines.stream()
                                .map(MpCxCapacityConfiguration::getCxMachineCode)
                                .collect(Collectors.toList()));
                return new ArrayList<>(advanceMachines);
            }
        }
        return new ArrayList<>();
    }

    // ==================== 机台上限映射（传入 BalancingService MachineState） ====================

    /**
     * 构建「机台编码 → 最大硫化机数」映射。
     *
     * <p>查找顺序：机型+结构（{@code MdmStructureLhRatio}）→ 仅结构 → {@link #DEFAULT_MAX_LH_MACHINE_COUNT}。
     */
    private Map<String, Integer> buildMachineMaxLhMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        Map<String, String> machineTypeMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }

        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getLhMachineMaxQty() != null) {
                    typeStructureMap.put(key, ratio.getLhMachineMaxQty());
                }
            }
        }

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
                maxLh = DEFAULT_MAX_LH_MACHINE_COUNT;
            }
            result.put(machineCode, maxLh);
        }

        return result;
    }

    /**
     * 构建「机台编码 → 最大胎胚种类数」映射。
     *
     * <p>查找顺序：机台前缀参数（SYS04040001）→ 机型+结构 → {@code context.maxTypesPerMachine} /
     * {@link BalancingService#DEFAULT_MAX_TYPES_PER_MACHINE}。
     */
    private Map<String, Integer> buildMachineMaxEmbryoTypesMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        Map<String, String> machineTypeMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }

        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getMaxEmbryoQty() != null) {
                    typeStructureMap.put(key, ratio.getMaxEmbryoQty());
                }
            }
        }

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
                        ? context.getMaxTypesPerMachine() : BalancingService.DEFAULT_MAX_TYPES_PER_MACHINE;
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
            List<CoreScheduleAlgorithmService.MachineAllocationResult> trialAllocations,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            String structureName) {
        Map<String, String> trialMachineMap = new HashMap<>();
        if (trialAllocations != null) {
            for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : trialAllocations) {
                String machineCode = allocation.getMachineCode();
                for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                    if (structureName.equals(taskAlloc.getStructureName())
                            && taskAlloc.getEmbryoCode() != null
                            && Boolean.TRUE.equals(taskAlloc.getIsTrialTask())) {
                        trialMachineMap.put(taskAlloc.getEmbryoCode(), machineCode);
                    }
                }
            }
        }
        if (existAllocations != null) {
            for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
                String machineCode = allocation.getMachineCode();
                for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                    if (structureName.equals(taskAlloc.getStructureName())
                            && taskAlloc.getEmbryoCode() != null
                            && Boolean.TRUE.equals(taskAlloc.getIsTrialTask())) {
                        trialMachineMap.putIfAbsent(taskAlloc.getEmbryoCode(), machineCode);
                    }
                }
            }
        }
        if (continueTasks != null) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
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
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            Map<String, String> trialMachineMap) {
        if (!Boolean.TRUE.equals(task.getIsProductionTrial())) {
            return false;
        }
        return trialMachineMap.containsKey(task.getEmbryoCode());
    }
}
