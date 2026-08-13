package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.vo.BalancingResult;
import com.zlt.aps.cx.vo.DailyEmbryoTask;
import com.zlt.aps.cx.vo.EmbryoAssignment;
import com.zlt.aps.cx.vo.GreedyContext;
import com.zlt.aps.cx.vo.GreedyParams;
import com.zlt.aps.cx.vo.MachineAssignment;
import com.zlt.aps.cx.vo.MachineState;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * S5.3.3.2.5 贪心均衡分配引擎 - 将结构内胎胚任务按「硫化机台数」均衡分配到多台成型机。
 *
 * <p>采用<b>贪心两轮 + 局部搜索</b>策略替代早期 DFS 搜索，在保证均衡质量的同时显著降低计算开销。
 *
 * <h3>流水线位置</h3>
 * <pre>
 * CoreScheduleAlgorithmServiceImpl.executeShiftSchedule
 *   -> NewTaskProcessor.processNewTasks（按结构分组）
 *       -> preBalanceContinueLoad（续作负荷预均衡，NewTaskProcessor 内部）
 *       -> BalancingService.balanceEmbryosToMachinesWithMachineCapacity（本类）
 *           -> greedyAssign（R1：严格容量 + 局部搜索）
 *               -> R1 满足 P1 则返回
 *               -> R1 不满足则回退 greedyAssignRound2（R2：放宽容量 + 局部搜索）
 *       -> postBalanceContinueLoad（续作负荷后置重分配，NewTaskProcessor 内部）
 *       -> 输出 BalancingResult -> ShiftScheduleService 班次精排
 * </pre>
 *
 * <h3>单位与输入约定</h3>
 * <ul>
 *   <li><b>分配单位</b>：硫化机台数（{@code vulcanizeMachineCount}），不是条数；条数由 TaskGroupService 写入
 *       {@code endingExtraInventory}，精排阶段才换算。</li>
 *   <li><b>下量过滤</b>：仅 {@code endingExtraInventory &gt; 0} 的任务进入均衡（与 Processor 预过滤一致）。</li>
 *   <li><b>续作预扣</b>：{@code continueLoadMap} / {@code continueTypeMap} / {@code continueLhMachineCodeMap}
 *       由 NewTaskProcessor 传入，初始化机台时扣减剩余容量与种类槽，贪心新分配数 = 总负荷 − 预扣负荷。</li>
 *   <li><b>机台上限</b>：每台机 {@code maxCapacity}=最大硫化机数、{@code maxTypes}=最大胎胚种类数，
 *       均由 NewTaskProcessor 按机型+结构+参数前缀解析后传入。</li>
 * </ul>
 *
 * <h3>主流程（{@link #balanceEmbryosToMachinesWithMachineCapacity} 10 参数重载）</h3>
 * <ol>
 *   <li>5.3.3.2.5.1 — 输入过滤：收尾舍弃 / endingExtraInventory≤0</li>
 *   <li>5.3.3.2.5.2 - 供需预估：Σ需求、Σ产能、distinct 胎胚种类数、均衡阈值读取</li>
 *   <li>5.3.3.2.5.3 — 任务排序：量试约束机台 → 续作 → 非零净需求 → 大任务 → 候选机台少者优先</li>
 *   <li>5.3.3.2.5.4 - 贪心R1（{@link #greedyAssign}）：严格容量约束下的聚集分配 + 局部搜索</li>
 *   <li>5.3.3.2.5.5 - 若 R1 满足 P1（完整+均衡）则直接返回；否则恢复需求快照</li>
 *   <li>5.3.3.2.5.6 - 贪心R2（{@link #greedyAssignRound2}）：放宽容量上限 + 局部搜索，返回最优部分解</li>
 *   <li>5.3.3.2.5.7 - 结果转换：MachineState 列表 -> {@link BalancingResult}</li>
 * </ol>
 *
 * <h3>贪心解的质量优先级（三级 P1/P2/P3）</h3>
 * <ul>
 *   <li><b>P1（必须满足）</b>：全部分配 + 负荷差≤阈值 + 种类差≤阈值</li>
 *   <li><b>P2（尽量满足）</b>：历史连续性（同胎胚优先分到历史机台）+ 胎胚聚集（同胎胚尽量少拆分）</li>
 *   <li><b>P3（尽量满足）</b>：各机台条数均匀</li>
 * </ul>
 * <p>R1 要求 P1 满足才返回；R2 无论 P1 是否满足都返回最优部分解（不返回 null）。
 *
 * <h3>贪心分配策略</h3>
 * <ol>
 *   <li><b>胎胚分组排序</b>：按胎胚总需求升序（小需求优先，因其候选机台少）</li>
 *   <li><b>候选机台排序</b>：历史机台 > 已有该胎胚的机台 > 负荷最低 > 种类槽最多</li>
 *   <li><b>聚集填充</b>：逐机台分配直至达 targetLoad 或容量耗尽，同胎胚尽量集中到少数机台</li>
 *   <li><b>局部搜索</b>：Move（高负荷->低负荷移动1台）+ Swap（两机台交换不同胎胚）+ TypeMove（种类均衡移动）</li>
 * </ol>
 *
 * <h3>R1 与 R2 的差异</h3>
 * <table>
 *   <tr><th>维度</th><th>R1（{@link #greedyAssign}）</th><th>R2（{@link #greedyAssignRound2}）</th></tr>
 *   <tr><td>容量约束</td><td>严格不超过 maxLh</td><td>可突破 maxLh（每次最多1台）</td></tr>
 *   <tr><td>maxTypes 限制</td><td>两阶段：先 target 再原始值</td><td>直接使用原始值</td></tr>
 *   <tr><td>P1 不满足时</td><td>返回 null，触发 R2</td><td>返回最优部分解</td></tr>
 *   <tr><td>续作预均衡</td><td>不依赖</td><td>前置依赖 preBalanceContinueLoad</td></tr>
 * </table>
 *
 * <h3>约束参数（T_CX_PARAM_CONFIG / context）</h3>
 * <table>
 *   <tr><th>编码</th><th>含义</th><th>默认</th></tr>
 *   <tr><td>SYS04070001</td><td>机台间胎胚种类数最大允许差额</td><td>1</td></tr>
 *   <tr><td>SYS04070002</td><td>机台间硫化机台数最大允许差额</td><td>3</td></tr>
 *   <tr><td>SYS04070003</td><td>强制保留历史任务（Y=保底预留，当前由 R2 隐式处理）</td><td>N</td></tr>
 * </table>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalancingService {

    /** 参数编码：胎胚种类数允许差额（均衡阈值） */
    private static final String PARAM_TYPE_DIFF_THRESHOLD = "SYS04070001";

    /** 参数编码：硫化机台数允许差额（均衡阈值） */
    private static final String PARAM_LOAD_DIFF_THRESHOLD = "SYS04070002";

    /** 默认：胎胚种类数允许差额（最多差1种） */
    private static final int DEFAULT_TYPE_DIFF_THRESHOLD = 1;

    /** 默认：硫化机台数允许差额（最多差3台） */
    private static final int DEFAULT_LOAD_DIFF_THRESHOLD = 3;

    // ==================== 5.3.3.2.5 均衡分配入口 ====================

    /**
     * 均衡分配（7 参数重载）— 无续作预扣场景。
     *
     * <p>委托给 10 参数重载，{@code continueLoadMap=null}、{@code continueTypeMap=null}、{@code continueLhMachineCodeMap=null}。
     */
    public BalancingResult balanceEmbryosToMachinesWithMachineCapacity(
            List<DailyEmbryoTask> tasks,
            List<MpCxCapacityConfiguration> availableMachines,
            Map<String, Set<String>> machineHistoryMap,
            Map<String, Integer> machineMaxLhMap,
            Map<String, Integer> machineMaxEmbryoTypesMap,
            boolean forceKeepHistory,
            ScheduleContextVo context) {
        // 续作均衡场景：无续作预扣
        return balanceEmbryosToMachinesWithMachineCapacity(
                tasks, availableMachines, machineHistoryMap,
                machineMaxLhMap, machineMaxEmbryoTypesMap,
                forceKeepHistory, context,
                null, null, null);
    }

    /**
     * 均衡分配主入口（10 参数）- 含续作预扣与贪心两轮全流程。
     *
     * <p>调用方：{@link NewTaskProcessor#processNewTasks} 对每个结构调用一次。
     *
     * <p>执行流程：
     * <ol>
     *   <li>输入过滤（收尾舍弃 / endingExtraInventory≤0）</li>
     *   <li>供需预估 + 均衡阈值读取</li>
     *   <li>任务排序（量试约束 -> 续作 -> 非零净需求 -> 大任务 -> 候选少者优先）</li>
     *   <li>贪心R1（{@link #greedyAssign}）：严格容量约束，满足 P1 则返回</li>
     *   <li>R1 不满足则恢复需求快照，执行贪心R2（{@link #greedyAssignRound2}）</li>
     *   <li>R2 始终返回最优部分解（不返回 null）</li>
     * </ol>
     *
     * @param tasks                    待均衡任务（同结构；vulcanizeMachineCount 为剩余待分配硫化机数）
     * @param availableMachines        该结构当日可用机台（已按 PRODUCTION_VERSION 过滤）
     * @param machineHistoryMap        机台 → 在产/历史胎胚集合（embryoCode 集合，来自在机信息反转）
     * @param machineMaxLhMap          机台 → 最大硫化机数（机型+结构配比）
     * @param machineMaxEmbryoTypesMap 机台 → 最大胎胚种类数（参数前缀 / 配比 / 默认 4）
     * @param forceKeepHistory         SYS04070003：true 时 DFS 前执行 {@link #reservedHistoryTasks}
     * @param context                  排程上下文（读均衡阈值参数）
     * @param continueLoadMap          续作已占硫化机数（机台 → 台数，已按 lhMachineCode 去重），null 表示无预扣
     * @param continueTypeMap          续作已占胎胚种类（机台 → embryoCode 集合），null 表示无预扣
     * @param continueLhMachineCodeMap 续作已占硫化机台号集合（机台 → lhMachineCode 集合），null 表示无预扣
     * @return 机台 → 胎胚 → 分配硫化机数 的 {@link BalancingResult}
     */
    public BalancingResult balanceEmbryosToMachinesWithMachineCapacity(
            List<DailyEmbryoTask> tasks,
            List<MpCxCapacityConfiguration> availableMachines,
            Map<String, Set<String>> machineHistoryMap,
            Map<String, Integer> machineMaxLhMap,
            Map<String, Integer> machineMaxEmbryoTypesMap,
            boolean forceKeepHistory,
            ScheduleContextVo context,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            Map<String, Set<String>> continueLhMachineCodeMap) {

        log.info("====== 均衡分配(含续作预扣)开始 ======");
        log.info("任务数={}, 可用机台数={}", tasks.size(), availableMachines.size());

        // ---- 5.3.3.2.5.1 输入过滤 ----
        // 条件1：收尾最后一批且 endingExtraInventory≤0 → 已舍弃，不参与均衡
        // 条件2：endingExtraInventory 必须 >0（实际排产量，Processor 层亦会预过滤）
        List<DailyEmbryoTask> filteredTasks = tasks.stream()
                .filter(t -> !(Boolean.TRUE.equals(t.getIsLastEndingBatch())
                        && (t.getEndingExtraInventory() == null || t.getEndingExtraInventory() <= 0)))
                .filter(t -> t.getEndingExtraInventory() != null && t.getEndingExtraInventory() > 0)
                .collect(Collectors.toList());
        if (filteredTasks.size() < tasks.size()) {
            log.info("收尾舍弃/零计划量过滤: {} 个任务被移除，剩余 {} 个", tasks.size() - filteredTasks.size(), filteredTasks.size());
            tasks = filteredTasks;
        }

        if (tasks.isEmpty()) {
            log.info("所有任务均被收尾舍弃，返回空结果");
            BalancingResult emptyResult = new BalancingResult();
            emptyResult.setAssignments(new ArrayList<>());
            return emptyResult;
        }

        // ---- 诊断日志：任务明细与共用胎胚物料分布（不影响分配逻辑） ----
        for (DailyEmbryoTask task : tasks) {
            String taskType = Boolean.TRUE.equals(task.getIsTrialTask()) ? "试制"
                    : Boolean.TRUE.equals(task.getIsContinueTask()) ? "续作" : "新增";
            String zeroDemandFlag = task.getDeferredRemainingDemand() != null ? "(零净需求)" : "";
            log.info("  胎胚任务: embryoCode={}, materialCode={}, vulcanizeMachineCount={}, 类型={}{}",
                    task.getEmbryoCode(), task.getMaterialCode(),
                    task.getVulcanizeMachineCount(),
                    taskType, zeroDemandFlag);
        }
        Map<String, Map<String, Integer>> embryoMaterialStats = new java.util.LinkedHashMap<>();
        for (DailyEmbryoTask task : tasks) {
            String embryoCode = task.getEmbryoCode();
            String materialCode = task.getMaterialCode() != null ? task.getMaterialCode() : "未知";
            int count = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            embryoMaterialStats.computeIfAbsent(embryoCode, k -> new HashMap<>())
                    .merge(materialCode, count, Integer::sum);
        }
        log.info("====== 胎胚物料分布统计 ======");
        for (Map.Entry<String, Map<String, Integer>> entry : embryoMaterialStats.entrySet()) {
            String embryoCode = entry.getKey();
            Map<String, Integer> materialCounts = entry.getValue();
            List<String> details = materialCounts.entrySet().stream()
                    .map(e -> e.getKey() + "(" + e.getValue() + "台)")
                    .collect(java.util.stream.Collectors.toList());
            int totalCount = materialCounts.values().stream().mapToInt(Integer::intValue).sum();
            log.info("  【胎胚 {}】共{}台，物料分布：{}", embryoCode, totalCount, details);
        }
        log.info("================================");

        // ---- 5.3.3.2.5.2 供需预估 + 均衡阈值 ----
        int typeDiffThreshold = getTypeDiffThreshold(context);
        int loadDiffThreshold = getLoadDiffThreshold(context);

        log.info("均衡分配参数：种类差额阈值={}, 负荷差额阈值={}",
                typeDiffThreshold, loadDiffThreshold);

        // 总需求 = 结构内所有任务 vulcanizeMachineCount 之和（单位：硫化机台数）
        int totalDemand = tasks.stream()
                .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                .sum();

        // 总产能 = 各机台 maxLh 之和（每台机型+结构可能不同）
        int totalCapacity = 0;
        for (MpCxCapacityConfiguration config : availableMachines) {
            Integer maxLh = machineMaxLhMap.get(config.getCxMachineCode());
            totalCapacity += (maxLh != null ? maxLh : ScheduleConstants.DEFAULT_MAX_LH_MACHINE_QTY);
        }

        // 计算种类数（去重）
        long totalTypes = tasks.stream()
                .map(DailyEmbryoTask::getEmbryoCode)
                .distinct()
                .count();

        log.info("均衡分配计算：总需求（硫化机台数）={}, 种类数={}, 总产能（各机台最大硫化机数之和）={}, 机台数={}",
                totalDemand, totalTypes, totalCapacity, availableMachines.size());

        // 打印任务需求明细（合并同胚子代码）
        Map<String, Integer> taskDetailMap = new LinkedHashMap<>();
        for (DailyEmbryoTask task : tasks) {
            String code = task.getEmbryoCode();
            int cnt = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            taskDetailMap.merge(code, cnt, Integer::sum);
        }
        List<String> taskDetails = taskDetailMap.entrySet().stream()
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.toList());
        log.info("任务需求明细：{}", taskDetails);

        // ---- 5.3.3.2.5.3 任务排序（决定 DFS 探索顺序，影响部分解质量） ----
        // 排序键优先级（高→低）：
        //   ① constrainedMachineCode 非空（量试锁定机台，候选唯一，必须先排）
        //   ② isContinueTask 且有机台列表（续作候选受限）
        //   ③ 非 deferredRemainingDemand（零净需求让位于有实际缺口的任务）
        //   ④ 同 embryoCode 总硫化机数降序（大任务优先占种类槽）
        //   ⑤ 静态候选机台数升序（受限任务优先）
        Map<String, Integer> embryoTotalDemand = new HashMap<>();
        for (DailyEmbryoTask task : tasks) {
            embryoTotalDemand.merge(task.getEmbryoCode(),
                    task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0,
                    Integer::sum);
        }

        List<DailyEmbryoTask> sortedTasks = tasks.stream()
                .sorted((a, b) -> {
                    boolean aConstrained = a.getConstrainedMachineCode() != null && !a.getConstrainedMachineCode().isEmpty();
                    boolean bConstrained = b.getConstrainedMachineCode() != null && !b.getConstrainedMachineCode().isEmpty();
                    if (aConstrained != bConstrained) return aConstrained ? -1 : 1;

                    boolean aContinue = a.getIsContinueTask() != null && a.getIsContinueTask()
                            && a.getContinueMachineCodes() != null && !a.getContinueMachineCodes().isEmpty();
                    boolean bContinue = b.getIsContinueTask() != null && b.getIsContinueTask()
                            && b.getContinueMachineCodes() != null && !b.getContinueMachineCodes().isEmpty();
                    if (aContinue != bContinue) return aContinue ? -1 : 1;

                    boolean aZeroDemand = a.getDeferredRemainingDemand() != null;
                    boolean bZeroDemand = b.getDeferredRemainingDemand() != null;
                    if (aZeroDemand != bZeroDemand) return aZeroDemand ? 1 : -1;

                    int totalA = embryoTotalDemand.getOrDefault(a.getEmbryoCode(), 0);
                    int totalB = embryoTotalDemand.getOrDefault(b.getEmbryoCode(), 0);
                    int demandCompare = Integer.compare(totalB, totalA);
                    if (demandCompare != 0) return demandCompare;

                    MachineState tmpA = createTempMachineState();
                    MachineState tmpB = createTempMachineState();
                    int candA = countCandidatesForStaticSort(a.getEmbryoCode(), tmpA);
                    int candB = countCandidatesForStaticSort(b.getEmbryoCode(), tmpB);
                    return Integer.compare(candA, candB);
                })
                .collect(Collectors.toList());

        // ========================================================================
        // 贪心分配（DFS 前置）：满足 P1（完整+均衡）则直接返回，否则回退 DFS 三轮
        //
        // P1（必须满足）：全部分配 + 负荷差≤阈值 + 种类差≤阈值
        // P2（尽量满足）：历史连续性 + 胎胚聚集
        // P3（尽量满足）：条数均匀
        // ========================================================================

        Map<Long, Integer> demandSnapshot = snapshotDemands(tasks);

        // 构建贪心共享参数对象（消除多方法间 7 参数列表重复）
        GreedyParams params = new GreedyParams();
        params.setAvailableMachines(availableMachines);
        params.setMachineHistoryMap(machineHistoryMap);
        params.setMachineMaxLhMap(machineMaxLhMap);
        params.setMachineMaxEmbryoTypesMap(machineMaxEmbryoTypesMap);
        params.setContinueLoadMap(continueLoadMap);
        params.setContinueTypeMap(continueTypeMap);
        params.setContinueLhMachineCodeMap(continueLhMachineCodeMap);
        params.setLhMachineSupplyMap(context.getLhMachineSupplyMap());

        BalancingResult greedyResult = greedyAssign(
                sortedTasks, params, typeDiffThreshold, loadDiffThreshold, totalDemand);

        if (greedyResult != null) {
            log.info("====== 贪心R1满足 P1（完整+均衡），跳过R2，算法路径=GREEDY_R1 ======");
            return greedyResult;
        }

        log.info("====== 贪心R1未满足P1，启动贪心R2（突破容量上限+局部搜索，非DFS） ======");

        // 恢复需求快照（贪心可能修改了 vulcanizeMachineCount）
        restoreDemands(sortedTasks, demandSnapshot);

        // ---- 贪心R2：突破容量上限分配 + 局部搜索 ----
        return greedyAssignRound2(
                sortedTasks, params, typeDiffThreshold, loadDiffThreshold, totalDemand);

    }

    /**
     * 快照所有任务的 vulcanizeMachineCount - 供后续轮恢复原始需求。
     *
     * <p>第一轮保底预留会从 vulcanizeMachineCount 扣减已预留台数，
     * 第二轮需要恢复到原始值才能重新分配全部需求。
     *
     * @param tasks 待快照任务列表
     * @return lhId → 原始 vulcanizeMachineCount 映射
     */
    private Map<Long, Integer> snapshotDemands(
            List<DailyEmbryoTask> tasks) {
        Map<Long, Integer> snapshot = new HashMap<>();
        for (DailyEmbryoTask task : tasks) {
            Integer count = task.getVulcanizeMachineCount();
            snapshot.put(task.getLhId(), count != null ? count : 0);
        }
        return snapshot;
    }

    /**
     * 从快照恢复任务的 vulcanizeMachineCount — 第二轮执行前调用。
     *
     * @param sortedTasks 已排序任务列表（与快照时相同的任务对象）
     * @param snapshot    snapshotDemands 产生的快照映射
     */
    private void restoreDemands(
            List<DailyEmbryoTask> sortedTasks,
            Map<Long, Integer> snapshot) {
        for (DailyEmbryoTask task : sortedTasks) {
            Integer original = snapshot.get(task.getLhId());
            if (original != null) {
                task.setVulcanizeMachineCount(original);
            }
        }
        log.info("第二轮需求已从快照恢复，共恢复 {} 个任务的 vulcanizeMachineCount", sortedTasks.size());
    }

    /**
     * 静态排序辅助 — 创建容量充足的临时 MachineState（仅用于 countCandidatesForStaticSort）。
     */
    private MachineState createTempMachineState() {
        MachineState state = new MachineState();
        // 临时设置一个很大的容量，使其在初始时总是可用
        state.setCurrentLoad(0);
        state.setCurrentTypes(0);
        state.setMaxCapacity(100);
        state.setMaxTypes(ScheduleConstants.DEFAULT_MAX_TYPES_PER_MACHINE);
        return state;
    }

    /**
     * 静态排序辅助 — 按 embryoCode 数字段粗估候选机台稀缺程度（排序键⑤）。
     *
     * <p>无运行时 MachineState 信息时的启发式：编码数值越大，假定候选越少，返回更小值使其排序靠前。
     */
    private int countCandidatesForStaticSort(String embryoCode, MachineState tmpState) {
        // 提取胚胎编码中的数字部分
        String numPart = embryoCode.replaceAll("[^0-9]", "");
        int num = 0;
        if (!numPart.isEmpty()) {
            try { num = Integer.parseInt(numPart); } catch (NumberFormatException ignored) {}
        }

        // 胚胎编码大的（通常需求大/候选少），候选更少
        if (num >= ScheduleConstants.EMBRYO_CODE_HIGH_THRESHOLD) {
            return 2;
        }
        return 3;
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建硫化机去重 key（统一所有调用点的 fallback 逻辑，确保 key 一致性）。
     *
     * <p>当 {@code lhMachineCode} 非空非空串时直接返回；为空时回退到 {@code "lhId_" + lhId}，
     * 与 {@code NewTaskProcessor} 构建 {@code continueLhMachineCodeMap} 的 key 保持一致。
     *
     * @param lhMachineCode 硫化机台编号（已通过 extractLhMachineKey 去除 L/R 后缀）
     * @param lhId          硫化排程结果 ID（fallback 用）
     * @return 硫化机去重 key
     */
    private static String buildLhMachineKey(String lhMachineCode, Long lhId) {
        if (lhMachineCode != null && !lhMachineCode.isEmpty()) {
            return lhMachineCode;
        }
        return "lhId_" + (lhId != null ? lhId : "null");
    }

    // ==================== 参数读取（SYS04070001~003） ====================

    /** 读取 SYS04070001 机台间胎胚种类数允许差额，默认 {@link #DEFAULT_TYPE_DIFF_THRESHOLD}。 */
    public int getTypeDiffThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_TYPE_DIFF_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析种类差额阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_TYPE_DIFF_THRESHOLD;
    }

    /** 读取 SYS04070002 机台间硫化机台数允许差额，默认 {@link #DEFAULT_LOAD_DIFF_THRESHOLD}。 */
    public int getLoadDiffThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_LOAD_DIFF_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析负荷差额阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_LOAD_DIFF_THRESHOLD;
    }

    /** 读取 SYS04070003 是否强制保留历史任务（Y/true=保底预留）。 */
    public boolean getForceKeepHistoryConfig(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(ScheduleConstants.PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                return "Y".equalsIgnoreCase(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false;
    }

    // ==================== 贪心+局部搜索（DFS 前置） ====================

    /** 局部搜索最大迭代次数，防止极端场景死循环。 */
    private static final int GREEDY_LOCAL_SEARCH_MAX_ITERATIONS = 200;

    /**
     * 贪心+局部搜索均衡分配（DFS 前置）。
     *
     * <p>三级优先级：
     * <ul>
     *   <li><b>P1（必须满足）</b>：全部分配 + 负荷差≤阈值 + 种类差≤阈值</li>
     *   <li><b>P2（尽量满足）</b>：历史连续性 + 胎胚聚集（同胎胚尽量少拆分到不同机台）</li>
     *   <li><b>P3（尽量满足）</b>：各机台条数均匀</li>
     * </ul>
     *
     * <p>满足 P1 则返回 {@link BalancingResult}，否则返回 {@code null} 由 R2（{@link #greedyAssignRound2}）接管。
     *
     * @param sortedTasks       已排序的任务列表（不会修改原始 vulcanizeMachineCount）
     * @param params            贪心共享参数
     * @param typeDiffThreshold 机台间种类差阈值
     * @param loadDiffThreshold 机台间负荷差阈值
     * @param totalDemand       总需求（硫化机台数）
     * @return P1 满足则返回分配结果，否则返回 null
     */
    private BalancingResult greedyAssign(
            List<DailyEmbryoTask> sortedTasks,
            GreedyParams params,
            int typeDiffThreshold,
            int loadDiffThreshold,
            int totalDemand) {

        if (params.getAvailableMachines().isEmpty() || sortedTasks.isEmpty()) {
            return null;
        }

        log.info("====== 贪心分配开始: {} 台机台, {} 个任务, 总需求={} ======",
                params.getAvailableMachines().size(), sortedTasks.size(), totalDemand);

        // ---- 1~3. 初始化机台状态 + 胎胚分组排序 + 计算目标值 ----
        GreedyContext ctx = prepareGreedyContext(sortedTasks, params, totalDemand);
        List<MachineState> machineStates = ctx.getMachineStates();

        log.info("贪心参数: targetTypes={}, targetLoad={}, loadDiffThreshold={}, typeDiffThreshold={}",
                ctx.getTargetTypes(), ctx.getTargetLoad(), loadDiffThreshold, typeDiffThreshold);

        // ---- 4. Phase 1a: 贪心分配（maxTypes=target） ----
        int remainingAfter1a = greedyAssignPhase(machineStates, ctx.getEmbryoGroups(), ctx.getSortedEmbryos(),
                params.getMachineHistoryMap(), ctx.getTargetTypes(), ctx.getTargetLoad(), loadDiffThreshold,
                params.getLhMachineSupplyMap());

        // ---- 5. Phase 1b: 放宽分配（maxTypes=原始值，仅对 Phase 1a 未分配的任务） ----
        int remainingAfter1b = remainingAfter1a;
        if (remainingAfter1a > 0) {
            log.info("Phase 1a 剩余 {} 台未分配，放宽 maxTypes 到原始值重试", remainingAfter1a);
            remainingAfter1b = greedyAssignPhase(machineStates, ctx.getEmbryoGroups(), ctx.getSortedEmbryos(),
                    params.getMachineHistoryMap(), Integer.MAX_VALUE, ctx.getTargetLoad(), loadDiffThreshold,
                    params.getLhMachineSupplyMap());
        }

        boolean allAssigned = (remainingAfter1b == 0);
        log.info("贪心分配完成: allAssigned={}, 各机台负荷={}",
                allAssigned, formatMachineLoads(machineStates));

        if (!allAssigned) {
            log.info("贪心R1未全部分配（剩余 {} 台），交由贪心R2继续", remainingAfter1b);
            return null;
        }

        // ---- 6. Phase 2: 局部搜索（Move + Swap）修正负荷均衡 ----
        int loadGap = calculateLoadGap(machineStates);
        int typeGap = calculateTypeGap(machineStates);
        if (loadGap > loadDiffThreshold || typeGap > typeDiffThreshold) {
            log.info("Phase 2 局部搜索: loadGap={}, typeGap={}, 阈值={}/{}",
                    loadGap, typeGap, loadDiffThreshold, typeDiffThreshold);
            localSearch(machineStates, loadDiffThreshold, typeDiffThreshold, params.getLhMachineSupplyMap());
        }

        // ---- 7. Phase 3: 验证 P1 ----
        // 注意：totalAssigned 是负荷数（按 lhMachineCode 去重），可能小于 totalDemand，
        // 但任务实际已全部分配（allAssigned=true）。因此 P1 完整性检查用 allAssigned 而非 totalAssigned。
        loadGap = calculateLoadGap(machineStates);
        typeGap = calculateTypeGap(machineStates);
        int totalAssigned = machineStates.stream().mapToInt(MachineState::getCurrentLoad).sum();

        if (loadGap <= loadDiffThreshold && typeGap <= typeDiffThreshold) {
            log.info("贪心R1 P1 满足: totalAssigned={}, loadGap={}, typeGap={}",
                    totalAssigned, loadGap, typeGap);
            logBalancingResult(machineStates);
            return buildBalancingResult(machineStates, "GREEDY_R1", true, loadGap, typeGap, true);
        }

        log.info("贪心R1 P1 未满足（负荷/种类差超标）: totalAssigned={}, loadGap={}, typeGap={}, 交由贪心R2继续",
                totalAssigned, loadGap, typeGap);
        return null;
    }

    /**
     * 贪心第二轮（替代 DFS 三轮）：突破容量上限分配 + 局部搜索修正均衡。
     *
     * <p>与 {@link #greedyAssign} 的差异：
     * <ul>
     *   <li>不限制 maxAssignForBalance（可突破 maxLh）</li>
     *   <li>仍尊重 maxTypes（不能超最大种类数）</li>
     *   <li>续作预扣负荷+种类全量跟踪（用于均衡判断）</li>
     *   <li>P1 不满足时返回最优部分解（不返回 null）</li>
     * </ul>
     *
     * <p>前置条件：续作负荷已由 {@code NewTaskProcessor.preBalanceContinueLoad} 预均衡。
     *
     * @param sortedTasks       已排序的任务列表
     * @param params            贪心共享参数
     * @param typeDiffThreshold 机台间种类差阈值
     * @param loadDiffThreshold 机台间负荷差阈值
     * @param totalDemand       总需求（硫化机台数）
     */
    private BalancingResult greedyAssignRound2(
            List<DailyEmbryoTask> sortedTasks,
            GreedyParams params,
            int typeDiffThreshold,
            int loadDiffThreshold,
            int totalDemand) {

        if (params.getAvailableMachines().isEmpty() || sortedTasks.isEmpty()) {
            BalancingResult empty = new BalancingResult();
            empty.setAssignments(new ArrayList<>());
            return empty;
        }

        log.info("====== 贪心R2分配开始: {} 台机台, {} 个任务, 总需求={} ======",
                params.getAvailableMachines().size(), sortedTasks.size(), totalDemand);

        // ---- 1~3. 初始化机台状态 + 胎胚分组排序 + 计算目标值 ----
        GreedyContext ctx = prepareGreedyContext(sortedTasks, params, totalDemand);
        List<MachineState> machineStates = ctx.getMachineStates();

        log.info("贪心R2参数: targetTypes={}, targetLoad={}, loadDiffThreshold={}, typeDiffThreshold={}",
                ctx.getTargetTypes(), ctx.getTargetLoad(), loadDiffThreshold, typeDiffThreshold);

        // ---- 4a. Phase 1a: 按 targetLoad 分配（同R1逻辑，maxTypes=原始值） ----
        int remainingAfter1a = greedyAssignPhase(machineStates, ctx.getEmbryoGroups(), ctx.getSortedEmbryos(),
                params.getMachineHistoryMap(), Integer.MAX_VALUE, ctx.getTargetLoad(), loadDiffThreshold,
                params.getLhMachineSupplyMap());

        // ---- 4b. Phase 1b: 剩余任务放宽容量上限分配（可突破 maxLh） ----
        int remaining = remainingAfter1a;
        if (remainingAfter1a > 0) {
            log.info("贪心R2 Phase 1a 剩余 {} 台，放宽容量上限重试", remainingAfter1a);
            remaining = greedyAssignPhaseRelaxed(machineStates, ctx.getEmbryoGroups(), ctx.getSortedEmbryos(),
                    params.getMachineHistoryMap(), ctx.getTargetLoad(), loadDiffThreshold,
                    params.getLhMachineSupplyMap());
        }

        boolean allAssigned = (remaining == 0);
        log.info("贪心R2分配完成: allAssigned={}, 各机台负荷={}",
                allAssigned, formatMachineLoads(machineStates));

        // ---- 5. Phase 2: 局部搜索修正均衡 ----
        int loadGap = calculateLoadGap(machineStates);
        int typeGap = calculateTypeGap(machineStates);
        if (loadGap > loadDiffThreshold || typeGap > typeDiffThreshold) {
            log.info("贪心R2 Phase 2 局部搜索: loadGap={}, typeGap={}, 阈值={}/{}",
                    loadGap, typeGap, loadDiffThreshold, typeDiffThreshold);
            localSearch(machineStates, loadDiffThreshold, typeDiffThreshold, params.getLhMachineSupplyMap());
        }

        // ---- 6. Phase 3: 验证 P1 ----
        // 注意：totalAssigned 是负荷数（按 lhMachineCode 去重），可能小于 effectiveTotalLoad，
        // 但任务实际已全部分配（allAssigned=true）。因此 P1 完整性检查用 allAssigned 而非 totalAssigned。
        loadGap = calculateLoadGap(machineStates);
        typeGap = calculateTypeGap(machineStates);
        int totalAssigned = machineStates.stream().mapToInt(MachineState::getCurrentLoad).sum();

        if (allAssigned && loadGap <= loadDiffThreshold && typeGap <= typeDiffThreshold) {
            log.info("贪心R2 P1 满足: totalAssigned={}, loadGap={}, typeGap={}",
                    totalAssigned, loadGap, typeGap);
        } else {
            log.info("贪心R2 P1 未完全满足（仍采用当前最优解）: allAssigned={}, totalAssigned={}, loadGap={}, typeGap={}, 阈值={}/{}",
                    allAssigned, totalAssigned, loadGap, typeGap, loadDiffThreshold, typeDiffThreshold);
        }

        logBalancingResult(machineStates);
        boolean p1Satisfied = allAssigned && loadGap <= loadDiffThreshold && typeGap <= typeDiffThreshold;
        return buildBalancingResult(machineStates, "GREEDY_R2", allAssigned, loadGap, typeGap, p1Satisfied);
    }

    /**
     * 放宽容量上限的贪心分配单轮（用于贪心R2）。
     *
     * <p>与 {@link #greedyAssignPhase} 的差异：不限制 maxAssignForBalance（可突破 maxLh）。
     * 仍尊重 maxTypes（不能超最大种类数）。
     *
     * <p>当机台已超 targetLoad 时，每次只分配1台（避免堆积）；未超则正常分配至 targetLoad。
     *
     * @param machineStates    机台状态列表（会被修改）
     * @param embryoGroups     胎胚 -> 任务列表映射
     * @param sortedEmbryos    已排序的胎胚编码列表（小需求优先）
     * @param machineHistoryMap 机台 -> 历史在产胎胚集合
     * @param targetLoad       目标负荷（超过后每次只分配1台）
     * @param loadDiffThreshold 负荷差阈值（预留，当前未使用）
     * @return 未分配的硫化机台数
     */
    private int greedyAssignPhaseRelaxed(
            List<MachineState> machineStates,
            Map<String, List<DailyEmbryoTask>> embryoGroups,
            List<String> sortedEmbryos,
            Map<String, Set<String>> machineHistoryMap,
            int targetLoad,
            int loadDiffThreshold,
            Map<String, Set<String>> lhMachineSupplyMap) {

        int totalRemaining = 0;

        for (String embryoCode : sortedEmbryos) {
            List<DailyEmbryoTask> taskList = embryoGroups.get(embryoCode);
            int remaining = taskList.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0).sum();
            if (remaining <= 0) continue;

            Set<String> dedicatedMachines = collectDedicatedMachines(taskList, lhMachineSupplyMap);
            List<MachineState> candidates = sortGreedyCandidates(
                    machineStates, embryoCode, machineHistoryMap, Integer.MAX_VALUE, dedicatedMachines);

            int taskIndex = 0;
            for (MachineState candidate : candidates) {
                if (remaining <= 0) break;

                boolean alreadyHas = candidate.getAssignedEmbryos().stream()
                        .anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));

                if (!alreadyHas) {
                    if (candidate.getCurrentTypes() >= candidate.getMaxTypes()) {
                        continue;
                    }
                }

                // R2 Phase 1b：机台已超 targetLoad 时，每次只分配1台（避免堆积）
                int assignQty = calcRelaxedAssignQty(candidate, remaining, targetLoad);

                int[] assignResult = assignTasksToCandidate(
                        candidate, taskList, embryoCode, assignQty, remaining, taskIndex, alreadyHas);
                remaining = assignResult[0];
                taskIndex = assignResult[1];
            }

            if (remaining > 0) {
                log.info("  胎胚 {} 在R2(放宽容量)仍剩余 {} 台未分配", embryoCode, remaining);
            }
            totalRemaining += remaining;
        }

        return totalRemaining;
    }

    /**
     * 计算放宽容量模式下的单轮分配量。
     *
     * <p>机台未超 targetLoad 时正常分配至 targetLoad；已超时每次仅分配1台（避免堆积）。
     * 候选排序已按负荷升序，低负荷机台优先。
     *
     * @param candidate  候选机台状态
     * @param remaining  剩余未分配数
     * @param targetLoad 目标负荷
     * @return 本轮可分配的硫化机台数
     */
    private int calcRelaxedAssignQty(MachineState candidate, int remaining, int targetLoad) {
        int capacityAboveTarget = targetLoad - candidate.getCurrentLoad();
        int maxAssignForBalance;
        if (capacityAboveTarget > 0) {
            maxAssignForBalance = capacityAboveTarget; // 未达 targetLoad，正常分配
        } else {
            maxAssignForBalance = Math.min(remaining, 1); // 已超 targetLoad，每次最多1台
        }
        return Math.min(remaining, maxAssignForBalance);
    }

    /**
     * 初始化贪心用机台状态（含续作预扣）。
     *
     * <p>为每台可用机台创建 {@link MachineState}，设置 maxCapacity/maxTypes，
     * 并将续作预扣的负荷、种类、硫化机台号写入初始状态。
     *
     * <p>续作预扣处理：
     * <ul>
     *   <li>continueLoadMap -> currentLoad（初始负荷）</li>
     *   <li>continueTypeMap -> currentTypes + historyEmbryos + 占位 EmbryoAssignment</li>
     *   <li>continueLhMachineCodeMap -> assignedLhMachineCodes（硫化机去重集合）</li>
     * </ul>
     *
     * @param params 贪心共享参数（availableMachines、machineMaxLhMap、machineMaxEmbryoTypesMap、
     *               machineHistoryMap、continueLoadMap、continueTypeMap、continueLhMachineCodeMap）
     * @return 初始化后的机台状态列表
     */
    private List<MachineState> initGreedyMachineStates(GreedyParams params) {

        List<MpCxCapacityConfiguration> availableMachines = params.getAvailableMachines();
        Map<String, Integer> machineMaxLhMap = params.getMachineMaxLhMap();
        Map<String, Integer> machineMaxEmbryoTypesMap = params.getMachineMaxEmbryoTypesMap();
        Map<String, Set<String>> machineHistoryMap = params.getMachineHistoryMap();
        Map<String, Integer> continueLoadMap = params.getContinueLoadMap();
        Map<String, Set<String>> continueTypeMap = params.getContinueTypeMap();
        Map<String, Set<String>> continueLhMachineCodeMap = params.getContinueLhMachineCodeMap();

        List<MachineState> machineStates = new ArrayList<>();
        for (MpCxCapacityConfiguration mc : availableMachines) {
            MachineState state = new MachineState();
            state.setMachineCode(mc.getCxMachineCode());

            Integer maxLh = machineMaxLhMap.get(mc.getCxMachineCode());
            state.setMaxCapacity(maxLh != null ? maxLh : ScheduleConstants.DEFAULT_MAX_LH_MACHINE_QTY);

            Integer maxTypes = machineMaxEmbryoTypesMap.get(mc.getCxMachineCode());
            state.setMaxTypes(maxTypes != null ? maxTypes : ScheduleConstants.DEFAULT_MAX_TYPES_PER_MACHINE);

            state.setCurrentLoad(0);
            state.setCurrentTypes(0);
            state.setAssignedEmbryos(new ArrayList<>());
            state.setAssignedLhMachineCodes(new HashSet<>());

            Set<String> historyEmbryos = machineHistoryMap.get(mc.getCxMachineCode());
            state.setHistoryEmbryos(historyEmbryos != null ? new HashSet<>(historyEmbryos) : new HashSet<>());

            // 续作预扣
            int preLoad = (continueLoadMap != null) ? continueLoadMap.getOrDefault(mc.getCxMachineCode(), 0) : 0;
            Set<String> preTypes = (continueTypeMap != null)
                    ? continueTypeMap.getOrDefault(mc.getCxMachineCode(), Collections.emptySet())
                    : Collections.emptySet();
            Set<String> preLhMachineCodes = (continueLhMachineCodeMap != null)
                    ? continueLhMachineCodeMap.getOrDefault(mc.getCxMachineCode(), Collections.emptySet())
                    : Collections.emptySet();

            if (preLoad > 0 || !preTypes.isEmpty()) {
                state.setCurrentLoad(preLoad);
                state.setCurrentTypes(preTypes.size());
                state.getAssignedLhMachineCodes().addAll(preLhMachineCodes);
                state.getHistoryEmbryos().addAll(preTypes);
                for (String embryo : preTypes) {
                    state.getAssignedEmbryos().add(new EmbryoAssignment(embryo, null, 0));
                }
                log.info("  【贪心初始化】{}: 续作预扣={}台, 种类={}", mc.getCxMachineCode(), preLoad, preTypes);
            }
            machineStates.add(state);
        }
        return machineStates;
    }

    /**
     * 贪心分配公共前置准备：初始化机台状态 + 胎胚分组排序 + 计算目标值。
     *
     * <p>被 {@link #greedyAssign} 和 {@link #greedyAssignRound2} 共用，消除重复代码。
     *
     * @param sortedTasks 已排序的任务列表
     * @param params      贪心共享参数
     * @param totalDemand 总需求（硫化机台数）
     * @return 贪心上下文（机台状态、胎胚分组、排序后胎胚、目标种类、目标负荷）
     */
    private GreedyContext prepareGreedyContext(
            List<DailyEmbryoTask> sortedTasks,
            GreedyParams params,
            int totalDemand) {

        // 1. 初始化机台状态（含续作预扣）
        List<MachineState> machineStates = initGreedyMachineStates(params);

        // 2. 按胎胚分组并按总需求升序排序（小需求优先）
        // 小需求胎胚（如1台）通常只有少数候选机台（历史机台），需优先分配避免被大需求占满。
        // 大需求胎胚（如11台）有更多机台选择，后分配时填充剩余容量，自然实现聚集。
        Map<String, List<DailyEmbryoTask>> embryoGroups = new LinkedHashMap<>();
        for (DailyEmbryoTask task : sortedTasks) {
            embryoGroups.computeIfAbsent(task.getEmbryoCode(), k -> new ArrayList<>()).add(task);
        }
        List<String> sortedEmbryos = embryoGroups.keySet().stream()
                .sorted((a, b) -> {
                    int demandA = embryoGroups.get(a).stream()
                            .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0).sum();
                    int demandB = embryoGroups.get(b).stream()
                            .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0).sum();
                    return Integer.compare(demandA, demandB);
                })
                .collect(Collectors.toList());

        // 3. 计算目标值
        int machineCount = params.getAvailableMachines().size();
        long distinctTypes = embryoGroups.keySet().size();
        int targetTypes = machineCount > 0 && distinctTypes > 0
                ? (int) Math.ceil((double) distinctTypes / machineCount) : 0;
        // targetLoad 需包含续作预扣负荷：总负荷 = DFS需求 + 预扣负荷
        int totalPreloadLoad = machineStates.stream().mapToInt(MachineState::getCurrentLoad).sum();
        int effectiveTotalLoad = totalDemand + totalPreloadLoad;
        int targetLoad = machineCount > 0
                ? (int) Math.ceil((double) effectiveTotalLoad / machineCount) : 0;

        GreedyContext ctx = new GreedyContext();
        ctx.setMachineStates(machineStates);
        ctx.setEmbryoGroups(embryoGroups);
        ctx.setSortedEmbryos(sortedEmbryos);
        ctx.setTargetTypes(targetTypes);
        ctx.setTargetLoad(targetLoad);
        return ctx;
    }

    /**
     * 将 assignQty 台硫化机分配到候选机台（从 taskList 中逐任务扣减）。
     *
     * <p>被 {@link #greedyAssignPhase} 和 {@link #greedyAssignPhaseRelaxed} 共用。
     * 逐任务从 taskList 取出 demand，分配 min(demand, assignQty) 台到候选机台，
     * 更新机台负荷/种类/硫化机号集合，并扣减任务的 vulcanizeMachineCount。
     *
     * @param candidate   候选机台状态（会被修改：负荷、种类、分配记录）
     * @param taskList    胎胚对应的任务列表（task.vulcanizeMachineCount 会被修改）
     * @param embryoCode  当前胎胚编码
     * @param assignQty   本轮可分配的硫化机台数
     * @param remaining   剩余未分配数
     * @param taskIndex   当前任务索引
     * @param alreadyHas  候选机台是否已有该胎胚（true 时不增量种类数）
     * @return int[2]: [0]=更新后的 remaining, [1]=更新后的 taskIndex
     */
    private int[] assignTasksToCandidate(
            MachineState candidate, List<DailyEmbryoTask> taskList,
            String embryoCode, int assignQty, int remaining, int taskIndex, boolean alreadyHas) {

        while (assignQty > 0 && taskIndex < taskList.size()) {
            DailyEmbryoTask task = taskList.get(taskIndex);
            int taskDemand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            if (taskDemand <= 0) {
                taskIndex++;
                continue;
            }

            int actualAssign = Math.min(taskDemand, assignQty);
            String machineKey = buildLhMachineKey(task.getLhMachineCode(), task.getLhId());
            boolean isNewLhMachine = !candidate.getAssignedLhMachineCodes().contains(machineKey);
            int loadInc = isNewLhMachine ? actualAssign : 0;

            candidate.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, actualAssign));
            candidate.setCurrentLoad(candidate.getCurrentLoad() + loadInc);
            if (isNewLhMachine) {
                candidate.getAssignedLhMachineCodes().add(machineKey);
            }
            if (!alreadyHas) {
                candidate.setCurrentTypes(candidate.getCurrentTypes() + 1);
                alreadyHas = true;
            }

            task.setVulcanizeMachineCount(taskDemand - actualAssign);
            remaining -= actualAssign;
            assignQty -= actualAssign;

            if (task.getVulcanizeMachineCount() == 0) {
                taskIndex++;
            }
        }
        return new int[]{remaining, taskIndex};
    }

    /**
     * 贪心分配单轮：逐个胎胚按候选机台优先级分配，聚集填充。
     *
     * <p>对每个胎胚按候选机台排序（历史优先 -> 已有该胎胚 -> 负荷最低 -> 种类槽最多），
     * 逐机台分配直至达到 targetLoad 或容量耗尽。同胎胚尽量集中到少数机台以实现聚集。
     *
     * @param machineStates     机台状态列表（会被修改）
     * @param embryoGroups      胎胚 -> 任务列表映射
     * @param sortedEmbryos     已排序的胎胚编码列表（小需求优先）
     * @param machineHistoryMap 机台 -> 历史在产胎胚集合
     * @param maxTypesLimit     种类上限（target 或 MAX_VALUE 表示不限制）
     * @param targetLoad        目标负荷（分配时不超过此值，留余量给局部搜索微调）
     * @param loadDiffThreshold 负荷差阈值（预留，当前未使用）
     * @return 未分配的硫化机台数
     */
    private int greedyAssignPhase(
            List<MachineState> machineStates,
            Map<String, List<DailyEmbryoTask>> embryoGroups,
            List<String> sortedEmbryos,
            Map<String, Set<String>> machineHistoryMap,
            int maxTypesLimit,
            int targetLoad,
            int loadDiffThreshold,
            Map<String, Set<String>> lhMachineSupplyMap) {

        int totalUnassigned = 0;

        for (String embryoCode : sortedEmbryos) {
            List<DailyEmbryoTask> taskList = embryoGroups.get(embryoCode);
            int remaining = taskList.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();

            if (remaining <= 0) {
                continue;
            }

            Set<String> dedicatedMachines = collectDedicatedMachines(taskList, lhMachineSupplyMap);
            // 候选机台排序: 专供 > 历史 > 已有该胎胚 > 负荷最低 > 种类槽最多
            List<MachineState> candidates = sortGreedyCandidates(
                    machineStates, embryoCode, machineHistoryMap, maxTypesLimit, dedicatedMachines);

            // 逐机台聚集分配
            int taskIndex = 0;
            for (MachineState candidate : candidates) {
                if (remaining <= 0) break;

                int availableLoad = candidate.getMaxCapacity() - candidate.getCurrentLoad();
                if (availableLoad <= 0) continue;

                boolean alreadyHas = candidate.getAssignedEmbryos().stream()
                        .anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));

                // 种类检查
                if (!alreadyHas) {
                    int effectiveMax = (maxTypesLimit == Integer.MAX_VALUE)
                            ? candidate.getMaxTypes()
                            : Math.min(candidate.getMaxTypes(), maxTypesLimit);
                    if (candidate.getCurrentTypes() >= effectiveMax) {
                        continue;
                    }
                }

                // 负荷约束：不超过 targetLoad（留出余量给局部搜索微调）
                int maxAssignForBalance = targetLoad - candidate.getCurrentLoad();
                if (maxAssignForBalance <= 0) {
                    continue;
                }

                int assignQty = Math.min(remaining, Math.min(availableLoad, maxAssignForBalance));

                // 从 taskList 中取任务分配
                int[] assignResult = assignTasksToCandidate(
                        candidate, taskList, embryoCode, assignQty, remaining, taskIndex, alreadyHas);
                remaining = assignResult[0];
                taskIndex = assignResult[1];
            }

            if (remaining > 0) {
                log.info("  胎胚 {} 在本轮(maxTypesLimit={})剩余 {} 台未分配",
                        embryoCode, maxTypesLimit == Integer.MAX_VALUE ? "原始值" : maxTypesLimit, remaining);
            }
            totalUnassigned += remaining;
        }

        return totalUnassigned;
    }

    /**
     * 候选机台排序（P2历史优先 + P1均衡）。
     *
     * <p>排序优先级:
     * <ol>
     *   <li>历史机台（该胎胚上个班次在此机台生产）</li>
     *   <li>已有该胎胚的机台（聚集，不占新种类槽）</li>
     *   <li>当前负荷最低（均衡）</li>
     *   <li>种类槽最充裕（currentTypes 最少）</li>
     * </ol>
     *
     * @param machineStates     机台状态列表
     * @param embryoCode        当前待分配的胎胚编码
     * @param machineHistoryMap 机台 -> 历史在产胎胚集合
     * @param maxTypesLimit     种类上限（用于过滤种类槽已满的候选，MAX_VALUE 表示不过滤）
     * @return 按优先级排序后的候选机台列表
     */
    private List<MachineState> sortGreedyCandidates(
            List<MachineState> machineStates,
            String embryoCode,
            Map<String, Set<String>> machineHistoryMap,
            int maxTypesLimit,
            Set<String> dedicatedMachines) {

        return machineStates.stream()
                .sorted((a, b) -> {
                    // ① 专供机台优先（优先专供可回退：专供机满负荷后再回退到其他机台）
                    boolean aDedicated = dedicatedMachines != null && dedicatedMachines.contains(a.getMachineCode());
                    boolean bDedicated = dedicatedMachines != null && dedicatedMachines.contains(b.getMachineCode());
                    if (aDedicated != bDedicated) return aDedicated ? -1 : 1;

                    // ② 历史机台优先
                    Set<String> histA = machineHistoryMap.get(a.getMachineCode());
                    Set<String> histB = machineHistoryMap.get(b.getMachineCode());
                    boolean aIsHistory = histA != null && histA.contains(embryoCode);
                    boolean bIsHistory = histB != null && histB.contains(embryoCode);
                    if (aIsHistory && !bIsHistory) return -1;
                    if (!aIsHistory && bIsHistory) return 1;

                    // ③ 已有该胎胚的机台优先（聚集）
                    boolean aHas = a.getAssignedEmbryos().stream().anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));
                    boolean bHas = b.getAssignedEmbryos().stream().anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));
                    if (aHas && !bHas) return -1;
                    if (!aHas && bHas) return 1;

                    // ④ 负荷最低优先（均衡）
                    int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
                    if (loadCompare != 0) return loadCompare;

                    // ⑤ 种类槽最多优先
                    return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
                })
                .collect(Collectors.toList());
    }

    /**
     * 收集一组任务（同一胎胚）的专供成型机台号并集。
     *
     * <p>同一胎胚可能有多个任务（不同硫化机），专供约束按任务（lhMachineCode）区分，
     * 此处取并集用于候选机台排序：并集中的机台视为该胎胚的优先机台。
     *
     * @param tasks              同一胎胚的任务列表
     * @param lhMachineSupplyMap 硫化机专供成型机映射（null 表示无专供约束）
     * @return 专供成型机台号并集，无专供配置时返回空集合
     */
    private Set<String> collectDedicatedMachines(
            List<DailyEmbryoTask> tasks,
            Map<String, Set<String>> lhMachineSupplyMap) {
        if (lhMachineSupplyMap == null || lhMachineSupplyMap.isEmpty() || tasks == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (DailyEmbryoTask task : tasks) {
            if (task.getLhMachineCode() == null) {
                continue;
            }
            Set<String> dedicated = lhMachineSupplyMap.get(task.getLhMachineCode());
            if (dedicated != null) {
                result.addAll(dedicated);
            }
        }
        return result;
    }

    /**
     * 判断任务是否允许分配到指定成型机（专供约束）。
     *
     * <p>任务无专供配置、或目标机台在专供集合内时允许；否则不允许（防止局部搜索
     * 把专供任务搬到非专供机台）。
     *
     * @param task              待移动任务
     * @param machineCode       目标成型机台号
     * @param lhMachineSupplyMap 硫化机专供成型机映射
     * @return true 表示允许
     */
    private boolean isDedicatedAllowed(
            DailyEmbryoTask task,
            String machineCode,
            Map<String, Set<String>> lhMachineSupplyMap) {
        if (task == null || task.getLhMachineCode() == null
                || lhMachineSupplyMap == null || lhMachineSupplyMap.isEmpty()) {
            return true;
        }
        Set<String> dedicated = lhMachineSupplyMap.get(task.getLhMachineCode());
        return dedicated == null || dedicated.isEmpty() || dedicated.contains(machineCode);
    }

    /**
     * 局部搜索：通过 Move 和 Swap 操作修正负荷均衡。
     *
     * <p>迭代执行以下操作直到均衡达标或无法改善：
     * <ol>
     *   <li><b>TypeMove</b>：种类差超标时，将高种类机台的独占胎胚移到低种类机台</li>
     *   <li><b>Move</b>：负荷差超标时，将高负荷机台的任务移到低负荷机台</li>
     *   <li><b>Swap</b>：Move 失败时，交换两机台间的不同胎胚任务</li>
     * </ol>
     *
     * <p>最大迭代 {@link #GREEDY_LOCAL_SEARCH_MAX_ITERATIONS} 次，防止极端场景死循环。
     *
     * @param machineStates    机台状态列表（会被修改）
     * @param loadDiffThreshold 负荷差阈值
     * @param typeDiffThreshold 种类差阈值
     * @param lhMachineSupplyMap 硫化机专供成型机映射（null 表示无专供约束）
     */
    private void localSearch(
            List<MachineState> machineStates,
            int loadDiffThreshold,
            int typeDiffThreshold,
            Map<String, Set<String>> lhMachineSupplyMap) {

        for (int iter = 0; iter < GREEDY_LOCAL_SEARCH_MAX_ITERATIONS; iter++) {
            int loadGap = calculateLoadGap(machineStates);
            int typeGap = calculateTypeGap(machineStates);
            if (loadGap <= loadDiffThreshold && typeGap <= typeDiffThreshold) {
                log.info("局部搜索第{}轮: loadGap={} ≤ 阈值{}, typeGap={} ≤ 阈值{}, 完成",
                        iter, loadGap, loadDiffThreshold, typeGap, typeDiffThreshold);
                return;
            }

            // 优先处理种类差：将高种类机台的独占胎胚移到低种类机台
            if (typeGap > typeDiffThreshold) {
                boolean typeMoved = tryTypeBalance(machineStates, typeDiffThreshold, lhMachineSupplyMap);
                if (typeMoved) {
                    continue; // 种类移动后重新评估
                }
            }

            // 处理负荷差：将高负荷机台的任务移到低负荷机台
            if (loadGap > loadDiffThreshold) {
                // 找最高和最低负荷机台
                MachineState highMachine = null;
                MachineState lowMachine = null;
                for (MachineState ms : machineStates) {
                    if (highMachine == null || ms.getCurrentLoad() > highMachine.getCurrentLoad()) {
                        highMachine = ms;
                    }
                    if (lowMachine == null || ms.getCurrentLoad() < lowMachine.getCurrentLoad()) {
                        lowMachine = ms;
                    }
                }

                if (highMachine == lowMachine) {
                    return;
                }

                // 尝试 Move: 从 highMachine 找一个任务移到 lowMachine
                boolean moved = tryMove(highMachine, lowMachine, typeDiffThreshold, lhMachineSupplyMap);
                if (!moved) {
                    // Move 失败，尝试在所有高低机台对中找可移动的
                    moved = tryMoveAnyPair(machineStates, loadDiffThreshold, typeDiffThreshold, lhMachineSupplyMap);
                }
                if (!moved) {
                    // 尝试 Swap
                    moved = trySwap(highMachine, lowMachine, lhMachineSupplyMap);
                }
                if (!moved) {
                    log.info("局部搜索第{}轮: 无可用 Move/Swap，终止", iter);
                    return;
                }
            } else {
                // loadGap 已满足但 typeGap 未满足，且 tryTypeBalance 也失败
                log.info("局部搜索第{}轮: loadGap={}已满足, typeGap={}未满足但无法改善, 终止",
                        iter, loadGap, typeGap);
                return;
            }
        }
        log.info("局部搜索达到最大迭代次数 {}", GREEDY_LOCAL_SEARCH_MAX_ITERATIONS);
    }

    /**
     * 尝试将高负荷机台的一个任务（1台硫化机）移到低负荷机台。
     *
     * <p>移动条件：低负荷机台种类槽未满（或已有该胎胚），移动后减少负荷差。
     * 硫化机去重：同一 lhMachineCode 的任务在低负荷机台已存在时不增加负荷计数。
     *
     * @param highMachine       高负荷机台（移出方）
     * @param lowMachine        低负荷机台（移入方）
     * @param typeDiffThreshold 种类差阈值（预留，当前未使用）
     * @param lhMachineSupplyMap 硫化机专供成型机映射（null 表示无专供约束）
     * @return true 如果成功移动
     */
    private boolean tryMove(MachineState highMachine, MachineState lowMachine, int typeDiffThreshold,
                            Map<String, Set<String>> lhMachineSupplyMap) {
        // 遍历高负荷机台上的分配记录，找可移动的
        for (int i = 0; i < highMachine.getAssignedEmbryos().size(); i++) {
            EmbryoAssignment ea = highMachine.getAssignedEmbryos().get(i);
            if (ea.getTask() == null || ea.getAssignedQty() <= 0) {
                continue;
            }

            String embryoCode = ea.getEmbryoCode();
            // 专供约束：不允许把专供任务搬到非专供机台
            if (!isDedicatedAllowed(ea.getTask(), lowMachine.getMachineCode(), lhMachineSupplyMap)) {
                continue;
            }
            boolean lowHasEmbryo = lowMachine.getAssignedEmbryos().stream()
                    .anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));

            // 检查低负荷机台种类约束
            if (!lowHasEmbryo) {
                if (lowMachine.getCurrentTypes() >= lowMachine.getMaxTypes()) {
                    continue;
                }
            }

            // 执行 Move: 从 highMachine 移 1 台到 lowMachine
            DailyEmbryoTask task = ea.getTask();
            String machineKey = buildLhMachineKey(task.getLhMachineCode(), task.getLhId());
            boolean highHasLhKey = highMachine.getAssignedLhMachineCodes().contains(machineKey);
            boolean lowHasLhKey = lowMachine.getAssignedLhMachineCodes().contains(machineKey);

            // 更新高负荷机台
            ea.setAssignedQty(ea.getAssignedQty() - 1);
            int highLoadDec = highHasLhKey ? 1 : 0;
            highMachine.setCurrentLoad(highMachine.getCurrentLoad() - highLoadDec);
            if (ea.getAssignedQty() == 0) {
                highMachine.getAssignedEmbryos().remove(i);
                // 检查该胎胚是否还有其他分配记录
                boolean stillHas = highMachine.getAssignedEmbryos().stream()
                        .anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));
                if (!stillHas) {
                    highMachine.setCurrentTypes(highMachine.getCurrentTypes() - 1);
                }
                if (!highHasLhKey) {
                    // This shouldn't happen, but defensive
                }
            }

            // 更新低负荷机台
            lowMachine.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, 1));
            int lowLoadInc = lowHasLhKey ? 0 : 1;
            lowMachine.setCurrentLoad(lowMachine.getCurrentLoad() + lowLoadInc);
            if (!lowHasLhKey) {
                lowMachine.getAssignedLhMachineCodes().add(machineKey);
            }
            if (!lowHasEmbryo) {
                lowMachine.setCurrentTypes(lowMachine.getCurrentTypes() + 1);
            }

            // 更新任务的 vulcanizeMachineCount
            task.setVulcanizeMachineCount(task.getVulcanizeMachineCount() + 1);

            log.info("  Move: {} 的 {}(1台) -> {}, 负荷: {}->{}, {}->{}",
                    highMachine.getMachineCode(), embryoCode, lowMachine.getMachineCode(),
                    highMachine.getCurrentLoad() + highLoadDec, highMachine.getCurrentLoad(),
                    lowMachine.getCurrentLoad() - lowLoadInc, lowMachine.getCurrentLoad());
            return true;
        }
        return false;
    }

    /**
     * 在所有高低机台对中尝试 Move（当最优对无法 Move 时的扩展搜索）。
     *
     * <p>按负荷降序遍历所有高低机台对，负荷差≤阈值时终止（已无改善空间）。
     *
     * @param machineStates    机台状态列表
     * @param loadDiffThreshold 负荷差阈值
     * @param typeDiffThreshold 种类差阈值
     * @return true 如果任一对成功 Move
     */
    private boolean tryMoveAnyPair(List<MachineState> machineStates, int loadDiffThreshold, int typeDiffThreshold,
                                   Map<String, Set<String>> lhMachineSupplyMap) {
        // 按负荷降序排列
        List<MachineState> sorted = machineStates.stream()
                .sorted((a, b) -> Integer.compare(b.getCurrentLoad(), a.getCurrentLoad()))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            for (int j = sorted.size() - 1; j > i; j--) {
                MachineState high = sorted.get(i);
                MachineState low = sorted.get(j);
                if (high.getCurrentLoad() - low.getCurrentLoad() <= loadDiffThreshold) {
                    return false; // 已无改善空间
                }
                if (tryMove(high, low, typeDiffThreshold, lhMachineSupplyMap)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 尝试交换两机台间的不同胎胚任务以减少负荷差。
     *
     * <p>从高负荷机台找一个胎胚A、低负荷机台找一个不同胎胚B，
     * 各移1台到对方机台。交换后不新增种类（已有对方胎胚或种类槽未满）。
     *
     * @param highMachine 高负荷机台
     * @param lowMachine  低负荷机台
     * @param lhMachineSupplyMap 硫化机专供成型机映射（null 表示无专供约束）
     * @return true 如果成功交换
     */
    private boolean trySwap(MachineState highMachine, MachineState lowMachine,
                            Map<String, Set<String>> lhMachineSupplyMap) {
        for (EmbryoAssignment highEa : new ArrayList<>(highMachine.getAssignedEmbryos())) {
            if (highEa.getTask() == null || highEa.getAssignedQty() <= 0) continue;

            for (EmbryoAssignment lowEa : new ArrayList<>(lowMachine.getAssignedEmbryos())) {
                if (lowEa.getTask() == null || lowEa.getAssignedQty() <= 0) continue;

                String highEmbryo = highEa.getEmbryoCode();
                String lowEmbryo = lowEa.getEmbryoCode();
                if (highEmbryo.equals(lowEmbryo)) continue;

                // 专供约束：交换后每个任务仍须落在其专供机台（或任务无专供）
                if (!isDedicatedAllowed(highEa.getTask(), lowMachine.getMachineCode(), lhMachineSupplyMap)
                        || !isDedicatedAllowed(lowEa.getTask(), highMachine.getMachineCode(), lhMachineSupplyMap)) {
                    continue;
                }

                // 检查种类约束（交换后不新增种类）
                boolean highHasLowEmbryo = highMachine.getAssignedEmbryos().stream()
                        .anyMatch(e -> lowEmbryo.equals(e.getEmbryoCode()));
                boolean lowHasHighEmbryo = lowMachine.getAssignedEmbryos().stream()
                        .anyMatch(e -> highEmbryo.equals(e.getEmbryoCode()));

                if (!highHasLowEmbryo && highMachine.getCurrentTypes() >= highMachine.getMaxTypes()) {
                    continue;
                }
                if (!lowHasHighEmbryo && lowMachine.getCurrentTypes() >= lowMachine.getMaxTypes()) {
                    continue;
                }

                // 执行 Swap: highEa 的 1 台 <-> lowEa 的 1 台
                // 简化处理：仅当能减少负荷差时执行
                // highMachine 失去1台 highEmbryo, 得到1台 lowEmbryo -> 净负荷变化取决于 lhMachineCode
                // 这里简化为直接交换1台负荷
                highEa.setAssignedQty(highEa.getAssignedQty() - 1);
                lowEa.setAssignedQty(lowEa.getAssignedQty() - 1);

                highMachine.getAssignedEmbryos().add(new EmbryoAssignment(lowEmbryo, lowEa.getTask(), 1));
                lowMachine.getAssignedEmbryos().add(new EmbryoAssignment(highEmbryo, highEa.getTask(), 1));

                if (highEa.getAssignedQty() == 0) {
                    highMachine.getAssignedEmbryos().remove(highEa);
                    boolean stillHas = highMachine.getAssignedEmbryos().stream()
                            .anyMatch(e -> highEmbryo.equals(e.getEmbryoCode()));
                    if (!stillHas) highMachine.setCurrentTypes(highMachine.getCurrentTypes() - 1);
                }
                if (lowEa.getAssignedQty() == 0) {
                    lowMachine.getAssignedEmbryos().remove(lowEa);
                    boolean stillHas = lowMachine.getAssignedEmbryos().stream()
                            .anyMatch(e -> lowEmbryo.equals(e.getEmbryoCode()));
                    if (!stillHas) lowMachine.setCurrentTypes(lowMachine.getCurrentTypes() - 1);
                }

                if (!highHasLowEmbryo) highMachine.setCurrentTypes(highMachine.getCurrentTypes() + 1);
                if (!lowHasHighEmbryo) lowMachine.setCurrentTypes(lowMachine.getCurrentTypes() + 1);

                log.info("  Swap: {} 的 {} <-> {} 的 {}",
                        highMachine.getMachineCode(), highEmbryo,
                        lowMachine.getMachineCode(), lowEmbryo);
                return true;
            }
        }
        return false;
    }

    /**
     * 种类均衡：将高种类机台的独占胎胚（低种类机台没有的）移到低种类机台，减少种类差。
     *
     * <p>选择策略：找出种类最多的机台（highType）和种类最少的机台（lowType），
     * 从 highType 上找一个 lowType 没有的胎胚，将其1台任务移到 lowType。
     * 移动条件：
     * <ul>
     *   <li>lowType 种类槽未满（currentTypes < maxTypes）</li>
     *   <li>移动后 highType 该胎胚若清空则种类减1（独占胎胚）</li>
     *   <li>移动后不导致 loadGap 超过 loadDiffThreshold（通过检查 highType 和 lowType 的负荷差）</li>
     * </ul>
     *
     * @param machineStates    机台状态列表
     * @param typeDiffThreshold 种类差阈值
     * @param lhMachineSupplyMap 硫化机专供成型机映射（null 表示无专供约束）
     * @return true 如果成功移动
     */
    private boolean tryTypeBalance(List<MachineState> machineStates, int typeDiffThreshold,
                                   Map<String, Set<String>> lhMachineSupplyMap) {
        // 找种类最多和最少的机台
        MachineState highTypeMachine = null;
        MachineState lowTypeMachine = null;
        for (MachineState ms : machineStates) {
            if (highTypeMachine == null || ms.getCurrentTypes() > highTypeMachine.getCurrentTypes()) {
                highTypeMachine = ms;
            }
            if (lowTypeMachine == null || ms.getCurrentTypes() < lowTypeMachine.getCurrentTypes()) {
                lowTypeMachine = ms;
            }
        }

        if (highTypeMachine == lowTypeMachine) {
            return false;
        }
        if (highTypeMachine.getCurrentTypes() - lowTypeMachine.getCurrentTypes() <= typeDiffThreshold) {
            return false;
        }
        // 低种类机台种类槽已满，无法接受新种类
        if (lowTypeMachine.getCurrentTypes() >= lowTypeMachine.getMaxTypes()) {
            return false;
        }

        // 在高种类机台上找一个低种类机台没有的独占胎胚
        for (EmbryoAssignment ea : new ArrayList<>(highTypeMachine.getAssignedEmbryos())) {
            if (ea.getTask() == null || ea.getAssignedQty() <= 0) {
                continue;
            }

            String embryoCode = ea.getEmbryoCode();
            // 专供约束：不允许把专供任务搬到非专供机台
            if (!isDedicatedAllowed(ea.getTask(), lowTypeMachine.getMachineCode(), lhMachineSupplyMap)) {
                continue;
            }
            boolean lowHasEmbryo = lowTypeMachine.getAssignedEmbryos().stream()
                    .anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));
            if (lowHasEmbryo) {
                continue; // 低种类机台已有此胎胚，移动不会改善种类差
            }

            // 检查负荷：移动后 highType 负荷-1，lowType 负荷+1（新硫化机情况）
            // 确保移动后 loadGap 不超过阈值（简化：检查两机台负荷差变化）
            DailyEmbryoTask task = ea.getTask();
            String machineKey = buildLhMachineKey(task.getLhMachineCode(), task.getLhId());
            boolean highHasLhKey = highTypeMachine.getAssignedLhMachineCodes().contains(machineKey);
            boolean lowHasLhKey = lowTypeMachine.getAssignedLhMachineCodes().contains(machineKey);
            int highLoadDec = highHasLhKey ? 1 : 0;
            int lowLoadInc = lowHasLhKey ? 0 : 1;

            // 移动后两机台负荷差变化：原本 high-low，移动后 (high-highLoadDec)-(low+lowLoadInc)
            // 只要移动后 loadGap 不恶化即可
            int newHighLoad = highTypeMachine.getCurrentLoad() - highLoadDec;
            int newLowLoad = lowTypeMachine.getCurrentLoad() + lowLoadInc;
            // 检查不会导致新的 loadGap 超阈值（简化检查：两机台间）
            if (Math.abs(newHighLoad - newLowLoad) > Math.abs(
                    highTypeMachine.getCurrentLoad() - lowTypeMachine.getCurrentLoad()) + 1) {
                continue; // 移动会恶化负荷差，跳过
            }

            // 执行 Move
            ea.setAssignedQty(ea.getAssignedQty() - 1);
            highTypeMachine.setCurrentLoad(highTypeMachine.getCurrentLoad() - highLoadDec);
            if (ea.getAssignedQty() == 0) {
                highTypeMachine.getAssignedEmbryos().remove(ea);
                // 检查该胎胚是否还有其他分配记录
                boolean stillHas = highTypeMachine.getAssignedEmbryos().stream()
                        .anyMatch(e -> embryoCode.equals(e.getEmbryoCode()));
                if (!stillHas) {
                    highTypeMachine.setCurrentTypes(highTypeMachine.getCurrentTypes() - 1);
                }
            }

            lowTypeMachine.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, 1));
            lowTypeMachine.setCurrentLoad(lowTypeMachine.getCurrentLoad() + lowLoadInc);
            if (!lowHasLhKey) {
                lowTypeMachine.getAssignedLhMachineCodes().add(machineKey);
            }
            lowTypeMachine.setCurrentTypes(lowTypeMachine.getCurrentTypes() + 1);

            // 更新任务的 vulcanizeMachineCount
            task.setVulcanizeMachineCount(task.getVulcanizeMachineCount() + 1);

            log.info("  TypeMove: {} 的 {}({}种) -> {}, 种类: {}->{}, {}->{}",
                    highTypeMachine.getMachineCode(), embryoCode,
                    highTypeMachine.getCurrentTypes() + 1, lowTypeMachine.getMachineCode(),
                    highTypeMachine.getCurrentTypes() + 1, highTypeMachine.getCurrentTypes(),
                    lowTypeMachine.getCurrentTypes() - 1, lowTypeMachine.getCurrentTypes());
            return true;
        }
        return false;
    }

    // ---- 贪心辅助方法 ----

    /** 计算机台间最大负荷差。 */
    private int calculateLoadGap(List<MachineState> machineStates) {
        int max = 0, min = Integer.MAX_VALUE;
        for (MachineState ms : machineStates) {
            max = Math.max(max, ms.getCurrentLoad());
            min = Math.min(min, ms.getCurrentLoad());
        }
        return max - min;
    }

    /** 计算机台间最大种类差。 */
    private int calculateTypeGap(List<MachineState> machineStates) {
        int max = 0, min = Integer.MAX_VALUE;
        for (MachineState ms : machineStates) {
            max = Math.max(max, ms.getCurrentTypes());
            min = Math.min(min, ms.getCurrentTypes());
        }
        return max - min;
    }

    /** 格式化各机台负荷（日志用）。 */
    private String formatMachineLoads(List<MachineState> machineStates) {
        return machineStates.stream()
                .map(ms -> ms.getMachineCode() + "=" + ms.getCurrentLoad() + "台/" + ms.getCurrentTypes() + "种")
                .collect(Collectors.joining(", "));
    }

    /** 打印贪心分配结果（日志用）。 */
    private void logBalancingResult(List<MachineState> machineStates) {
        for (MachineState ms : machineStates) {
            // 统计续作预留数量（task==null 的占位条目）
            long continueReserved = ms.getAssignedEmbryos().stream()
                    .filter(ea -> ea.getTask() == null)
                    .count();
            // 按胎胚编码聚合 assignedQty，按数量降序排列
            String embryos = ms.getAssignedEmbryos().stream()
                    .filter(ea -> ea.getTask() != null)
                    .collect(Collectors.groupingBy(
                            EmbryoAssignment::getEmbryoCode,
                            LinkedHashMap::new,
                            Collectors.summingInt(EmbryoAssignment::getAssignedQty)))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(e -> e.getKey() + "(" + e.getValue() + ")")
                    .collect(Collectors.joining(", "));
            log.info("    分配->机台{}: [{}] (含续作预留{})", ms.getMachineCode(), embryos, continueReserved);
        }
    }

    /**
     * 将 MachineState 列表转换为 BalancingResult，并写入算法路径与 P1 摘要。
     */
    private BalancingResult buildBalancingResult(List<MachineState> machineStates,
                                                 String algorithmPath,
                                                 boolean allAssigned,
                                                 int loadGap,
                                                 int typeGap,
                                                 boolean p1Satisfied) {
        BalancingResult result = convertMachineStatesToBalancingResult(machineStates);
        result.setAlgorithmPath(algorithmPath);
        result.setAllAssigned(allAssigned);
        result.setLoadGap(loadGap);
        result.setTypeGap(typeGap);
        result.setP1Satisfied(p1Satisfied);
        return result;
    }

    /**
     * 将 MachineState 列表转换为 BalancingResult。
     *
     * <p>跳过 task=null 的 EmbryoAssignment（续作预扣占位条目）和 assignedQty≤0 的条目。
     * 每个 MachineState 生成一个 MachineAssignment，包含该机台的所有有效胎胚分配。
     *
     * @param machineStates 机台状态列表
     * @return 转换后的 BalancingResult
     */
    private BalancingResult convertMachineStatesToBalancingResult(List<MachineState> machineStates) {
        BalancingResult result = new BalancingResult();
        List<MachineAssignment> assignments = new ArrayList<>();

        for (MachineState ms : machineStates) {
            MachineAssignment ma = new MachineAssignment();
            ma.setMachineCode(ms.getMachineCode());
            ma.setEmbryoAssignments(new ArrayList<>());

            for (EmbryoAssignment ea : ms.getAssignedEmbryos()) {
                if (ea.getTask() != null && ea.getAssignedQty() > 0) {
                    ma.getEmbryoAssignments().add(ea);
                }
            }
            assignments.add(ma);
        }

        result.setAssignments(assignments);
        return result;
    }
}