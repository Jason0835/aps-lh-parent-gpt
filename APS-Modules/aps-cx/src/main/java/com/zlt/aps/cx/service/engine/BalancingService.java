package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
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
 * S5.3.3.2.5 DFS 均衡分配引擎 — 将结构内胎胚任务按「硫化机台数」均衡分配到多台成型机。
 *
 * <h3>流水线位置</h3>
 * <pre>
 * CoreScheduleAlgorithmServiceImpl.executeShiftSchedule
 *   → NewTaskProcessor.processNewTasks（按结构分组）
 *       → BalancingService.balanceEmbryosToMachinesWithMachineCapacity（本类）
 *           → 输出 MachineAllocationResult → ShiftScheduleService 班次精排
 * </pre>
 *
 * <h3>单位与输入约定</h3>
 * <ul>
 *   <li><b>分配单位</b>：硫化机台数（{@code vulcanizeMachineCount}），不是条数；条数由 TaskGroupService 写入
 *       {@code endingExtraInventory}，精排阶段才换算。</li>
 *   <li><b>下量过滤</b>：仅 {@code endingExtraInventory &gt; 0} 的任务进入均衡（与 Processor 预过滤一致）。</li>
 *   <li><b>续作预扣</b>：{@code continueLoadMap} / {@code continueTypeMap} 由 NewTaskProcessor 传入，
 *       初始化机台时扣减剩余容量与种类槽，DFS 新分配数 = 总负荷 − 预扣负荷。</li>
 *   <li><b>机台上限</b>：每台机 {@code maxCapacity}=最大硫化机数、{@code maxTypes}=最大胎胚种类数，
 *       均由 NewTaskProcessor 按机型+结构+参数前缀解析后传入。</li>
 * </ul>
 *
 * <h3>主流程（{@link #balanceEmbryosToMachinesWithMachineCapacity} 8 参数重载）</h3>
 * <ol>
 *   <li>5.3.3.2.5.1 — 输入过滤：收尾舍弃 / endingExtraInventory≤0</li>
 *   <li>5.3.3.2.5.2 — 供需预估：Σ需求、Σ产能、distinct 胎胚种类数</li>
 *   <li>5.3.3.2.5.3 — 任务排序：量试约束机台 → 续作 → 非零净需求 → 大任务 → 候选机台少者优先</li>
 *   <li>5.3.3.2.5.4 — 机台状态初始化 + 续作预扣写入 MachineState</li>
 *   <li>5.3.3.2.5.5 — 保底预留（SYS04070003=Y 时在 DFS 前于历史机台各留 1 台硫化机）</li>
 *   <li>5.3.3.2.5.6 — DFS+剪枝（{@link #dfsAssign}）：单胎胚可拆分到多机台，搜索上限 100 万次</li>
 *   <li>5.3.3.2.5.7 — 结果转换：最优解 → {@link BalancingResult}；无解时保底预留兜底或空结果</li>
 * </ol>
 *
 * <h3>DFS 解的质量优先级</h3>
 * <ol>
 *   <li>完整解（已分配数 = 总需求）优于部分解</li>
 *   <li>完整解中：满足均衡阈值（{@link #isBalanced}）优于不满足</li>
 *   <li>同等级下：{@link #calculateBalancingScore} 分数更低者更优（负荷差×10 + 种类差×100）</li>
 * </ol>
 *
 * <h3>约束参数（T_CX_PARAM_CONFIG / context）</h3>
 * <table>
 *   <tr><th>编码</th><th>含义</th><th>默认</th></tr>
 *   <tr><td>SYS04070001</td><td>机台间胎胚种类数最大允许差额</td><td>1</td></tr>
 *   <tr><td>SYS04070002</td><td>机台间硫化机台数最大允许差额</td><td>3</td></tr>
 *   <tr><td>SYS04070003</td><td>强制保留历史任务（Y=DFS 前保底预留）</td><td>N</td></tr>
 * </table>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalancingService {

    /** 机台最大胎胚种类数上限（默认4种） */
    public static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /** 机台默认最大硫化机数（配比配置缺失时的兜底值） */
    public static final int DEFAULT_MAX_LH_MACHINE_QTY = 10;

    /** 参数编码：机台最大硫化机数 */
    private static final String PARAM_MAX_LH_MACHINE_QTY = "SYS04020003";

    /** 参数编码：强制保留历史任务  */
    private static final String PARAM_FORCE_KEEP_HISTORY = "SYS04070003";

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
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
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
     * 均衡分配主入口（10 参数）— 含续作预扣与 DFS 全流程。
     *
     * <p>调用方：{@link NewTaskProcessor#processNewTasks} 对每个结构调用一次。
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
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
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
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> filteredTasks = tasks.stream()
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
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            String taskType = Boolean.TRUE.equals(task.getIsTrialTask()) ? "试制"
                    : Boolean.TRUE.equals(task.getIsContinueTask()) ? "续作" : "新增";
            String zeroDemandFlag = task.getDeferredRemainingDemand() != null ? "(零净需求)" : "";
            log.info("  胎胚任务: embryoCode={}, materialCode={}, vulcanizeMachineCount={}, 类型={}{}",
                    task.getEmbryoCode(), task.getMaterialCode(),
                    task.getVulcanizeMachineCount(),
                    taskType, zeroDemandFlag);
        }
        Map<String, Map<String, Integer>> embryoMaterialStats = new java.util.LinkedHashMap<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            String embryoCode = task.getEmbryoCode();
            String materialCode = task.getMaterialCode() != null ? task.getMaterialCode() : "未知";
            int count = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            embryoMaterialStats.computeIfAbsent(embryoCode, k -> new java.util.HashMap<>())
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
            totalCapacity += (maxLh != null ? maxLh : DEFAULT_MAX_LH_MACHINE_QTY);
        }

        // 计算种类数（去重）
        long totalTypes = tasks.stream()
                .map(CoreScheduleAlgorithmService.DailyEmbryoTask::getEmbryoCode)
                .distinct()
                .count();

        log.info("均衡分配计算：总需求（硫化机台数）={}, 种类数={}, 总产能（各机台最大硫化机数之和）={}, 机台数={}",
                totalDemand, totalTypes, totalCapacity, availableMachines.size());

        // 打印任务需求明细（合并同胚子代码）
        Map<String, Integer> taskDetailMap = new LinkedHashMap<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
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
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            embryoTotalDemand.merge(task.getEmbryoCode(),
                    task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0,
                    Integer::sum);
        }

        final Set<String> availableMachineCodes = availableMachines.stream()
                .map(MpCxCapacityConfiguration::getCxMachineCode)
                .collect(Collectors.toSet());

        List<CoreScheduleAlgorithmService.DailyEmbryoTask> sortedTasks = tasks.stream()
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

                    MachineState tmpA = createTempMachineState(availableMachineCodes);
                    MachineState tmpB = createTempMachineState(availableMachineCodes);
                    int candA = countCandidatesForStaticSort(a.getEmbryoCode(), tmpA);
                    int candB = countCandidatesForStaticSort(b.getEmbryoCode(), tmpB);
                    return Integer.compare(candA, candB);
                })
                .collect(Collectors.toList());

        // ========================================================================
        // 两轮均衡编排：第一轮严格均衡 → 完整性检测 → 第二轮宽松均衡（如需）
        //
        // 触发条件：第一轮未获得完整解（保底预留已满足负荷 + dfsAssignedCount < totalDemand）
        // 第二轮差异：解除容量上限 + 解除保底预留 + 解除续作预扣 + 历史优先/同胎胚集中/均衡例外排序
        // 异常处理：第二轮仍失败时取两轮较优者
        // ========================================================================

        // ---- 需求快照（供第二轮恢复原始 vulcanizeMachineCount） ----
        // 第一轮保底预留会扣减 vulcanizeMachineCount，第二轮需恢复到原始值
        Map<Long, Integer> demandSnapshot = snapshotDemands(tasks);

        // ---- 第一轮：严格均衡（容量上限 + 保底预留，与原单轮逻辑完全一致） ----
        BalancingRoundConfig round1Config = createRound1Config(forceKeepHistory);
        RoundExecutionResult round1Result = executeBalancingRound(
                round1Config, sortedTasks, availableMachines, machineHistoryMap,
                machineMaxLhMap, machineMaxEmbryoTypesMap, forceKeepHistory, context,
                continueLoadMap, continueTypeMap, continueLhMachineCodeMap,
                typeDiffThreshold, loadDiffThreshold, totalDemand, totalDemand);

        // ---- 完整性检测 ----
        // 完整解判定：保底预留已满足负荷 + DFS分配数 >= 原始总需求
        // 注意：续作预扣不计入已满足负荷（续作任务 demand 已在 ContinueTaskProcessor 扣减）
        if (round1Result.isComplete()) {
            log.info("第一轮均衡获得完整解，无需第二轮");
            return round1Result.getResult();
        }

        // ---- 第二轮：宽松均衡（解除容量上限 + 解除保底预留 + 解除续作预扣 + 历史优先/同胎胚集中/均衡例外排序） ----
        log.warn("第一轮均衡未获得完整解（已分配={}/{}），启动第二轮宽松均衡",
                round1Result.getPreOccupiedLoad() + round1Result.getDfsAssignedCount(), totalDemand);

        // 恢复原始需求（撤销第一轮保底预留的扣减）
        restoreDemands(sortedTasks, demandSnapshot);

        BalancingRoundConfig round2Config = createRound2Config();
        RoundExecutionResult round2Result = executeBalancingRound(
                round2Config, sortedTasks, availableMachines, machineHistoryMap,
                machineMaxLhMap, machineMaxEmbryoTypesMap, forceKeepHistory, context,
                continueLoadMap, continueTypeMap, continueLhMachineCodeMap,
                typeDiffThreshold, loadDiffThreshold, totalDemand, totalDemand);

        // ---- 异常处理：第二轮仍无法完成分配时的应对方案 ----
        int round1Total = round1Result.getPreOccupiedLoad() + round1Result.getDfsAssignedCount();
        int round2Total = round2Result.getPreOccupiedLoad() + round2Result.getDfsAssignedCount();

        if (round2Result.isComplete()) {
            log.info("第二轮均衡获得完整解，已分配={}/{}", round2Total, totalDemand);
            return round2Result.getResult();
        } else if (round2Total >= round1Total) {
            log.warn("第二轮仍未获得完整解（已分配={}/{}），返回第二轮最优部分解",
                    round2Total, totalDemand);
            return round2Result.getResult();
        } else {
            // 回退策略：第二轮因 maxTypes 约束反而分配更少时，返回第一轮结果
            log.warn("第二轮分配数({}) < 第一轮({})，回退第一轮结果", round2Total, round1Total);
            return round1Result.getResult();
        }
    }


    // ==================== 两轮均衡编排（executeBalancingRound + 辅助） ====================

    /**
     * 单轮均衡执行 — 封装步骤4-7（机台初始化→保底预留→DFS→结果转换），由两轮编排调用。
     *
     * <p><b>与第一轮原逻辑的差异点</b>（由 {@link BalancingRoundConfig} 驱动）：
     * <ul>
     *   <li>保底预留：第一轮 {@code config.enforceReservedHistory=forceKeepHistory}（按参数）；
     *       第二轮 {@code false}（不强制预留）</li>
     *   <li>容量上限：第一轮 {@code config.enforceCapacityLimit=true}（maxCapacity 管控）；
     *       第二轮 {@code false}（解除容量上限，允许超载）</li>
     *   <li>候选排序：第一轮历史偏好为④级；第二轮 {@code config.boostHistoryPreference=true}
     *       启用三项原则（历史优先+同胎胚集中+均衡例外）</li>
     *   <li>capacitySufficient：第一轮按实际供需计算；第二轮强制 {@code true}（产能无限）</li>
     *   <li>续作预扣：第一轮 {@code config.enforceContinuePreload=true}（预扣种类槽+负荷）；
     *       第二轮 {@code false}（不预扣，释放种类槽供新胎胚使用）</li>
     * </ul>
     *
     * @param config                     轮次配置（控制行为分支）
     * @param sortedTasks                已排序任务列表（两轮共用，第二轮前需恢复需求快照）
     * @param availableMachines          可用机台列表
     * @param machineHistoryMap          机台→历史胎胚集合
     * @param machineMaxLhMap            机台→最大硫化机数
     * @param machineMaxEmbryoTypesMap   机台→最大胎胚种类数
     * @param forceKeepHistory           SYS04070003 参数值（仅第一轮保底预留用）
     * @param context                    排程上下文
     * @param continueLoadMap            续作预扣负荷
     * @param continueTypeMap            续作预扣种类
     * @param continueLhMachineCodeMap   续作预扣硫化机台号
     * @param typeDiffThreshold          种类差额阈值
     * @param loadDiffThreshold          负荷差额阈值
     * @param totalDemand                本轮总需求（快照值，用于日志和完整性比较）
     * @param totalOriginalDemand        原始总需求（用于 RoundExecutionResult.isComplete 判定）
     * @return 单轮执行结果（含 BalancingResult + 完整度信息）
     */
    private RoundExecutionResult executeBalancingRound(
            BalancingRoundConfig config,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> sortedTasks,
            List<MpCxCapacityConfiguration> availableMachines,
            Map<String, Set<String>> machineHistoryMap,
            Map<String, Integer> machineMaxLhMap,
            Map<String, Integer> machineMaxEmbryoTypesMap,
            boolean forceKeepHistory,
            ScheduleContextVo context,
            Map<String, Integer> continueLoadMap,
            Map<String, Set<String>> continueTypeMap,
            Map<String, Set<String>> continueLhMachineCodeMap,
            int typeDiffThreshold,
            int loadDiffThreshold,
            int totalDemand,
            int totalOriginalDemand) {

        log.info("====== {} 开始 ======", config.getRoundName());

        // ---- 步骤4: 机台状态初始化（含续作预扣） ----
        // MachineState 字段：maxCapacity/maxTypes=上限，currentLoad/currentTypes=已占用，
        // assignedEmbryos=分配明细，historyEmbryos=在产胎胚（影响 DFS 候选排序）
        List<MachineState> machineStates = new ArrayList<>();
        for (MpCxCapacityConfiguration machineConfig : availableMachines) {
            MachineState state = new MachineState();
            state.setMachineCode(machineConfig.getCxMachineCode());

            Integer maxLh = machineMaxLhMap.get(machineConfig.getCxMachineCode());
            state.setMaxCapacity(maxLh != null ? maxLh : DEFAULT_MAX_LH_MACHINE_QTY);

            Integer maxTypes = machineMaxEmbryoTypesMap.get(machineConfig.getCxMachineCode());
            state.setMaxTypes(maxTypes != null ? maxTypes : DEFAULT_MAX_TYPES_PER_MACHINE);

            state.setCurrentLoad(0);
            state.setCurrentTypes(0);
            state.setAssignedEmbryos(new ArrayList<>());
            state.setAssignedLhMachineCodes(new HashSet<>());

            // 防御性拷贝：避免 addAll(preTypes) 污染原始 machineHistoryMap（两轮共用）
            Set<String> historyEmbryos = machineHistoryMap.get(machineConfig.getCxMachineCode());
            state.setHistoryEmbryos(historyEmbryos != null ? new HashSet<>(historyEmbryos) : new HashSet<>());

            // 预扣续作已占的容量和种类槽
            // 【第二轮差异】config.enforceContinuePreload=false 时跳过续作预扣
            // 第二轮不预扣：释放种类槽供新胎胚使用，避免 maxTypes 约束阻止分配
            if (config.isEnforceContinuePreload()) {
                int preLoad = (continueLoadMap != null) ? continueLoadMap.getOrDefault(machineConfig.getCxMachineCode(), 0) : 0;
                Set<String> preTypes = (continueTypeMap != null) ? continueTypeMap.getOrDefault(machineConfig.getCxMachineCode(), Collections.emptySet()) : Collections.emptySet();
                Set<String> preLhMachineCodes = (continueLhMachineCodeMap != null)
                        ? continueLhMachineCodeMap.getOrDefault(machineConfig.getCxMachineCode(), Collections.emptySet())
                        : Collections.emptySet();
                if (preLoad > 0 || !preTypes.isEmpty()) {
                    state.setCurrentLoad(preLoad);
                    state.setCurrentTypes(preTypes.size());
                    state.getAssignedLhMachineCodes().addAll(preLhMachineCodes);
                    state.getHistoryEmbryos().addAll(preTypes);
                    for (String embryoCode : preTypes) {
                        state.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, null, 1));
                    }
                    log.info("  【初始化机台】{}: 续作预扣={}台/{}, 剩余容量={}台, 剩余种类={}种, 历史胎胚={}",
                            machineConfig.getCxMachineCode(), preLoad, preTypes,
                            state.getMaxCapacity() - state.getCurrentLoad(),
                            state.getMaxTypes() - state.getCurrentTypes(),
                            state.getHistoryEmbryos());
                } else {
                    log.info("  【初始化机台】{}: 剩余容量={}台, 剩余种类={}种, 历史胎胚={}",
                            machineConfig.getCxMachineCode(),
                            state.getMaxCapacity() - state.getCurrentLoad(),
                            state.getMaxTypes() - state.getCurrentTypes(),
                            state.getHistoryEmbryos());
                }
            } else {
                // 第二轮：不预扣，机台从空状态开始（currentLoad=0, currentTypes=0）
                log.info("  【初始化机台】{}: [无续作预扣] 剩余种类={}种, 历史胎胚={}",
                        machineConfig.getCxMachineCode(),
                        state.getMaxTypes() - state.getCurrentTypes(),
                        state.getHistoryEmbryos());
            }

            machineStates.add(state);
        }

        // ---- 捕获续作预扣负荷（保底预留前） ----
        // 续作预扣（continuePreloadLoad）不代表已满足的需求——续作任务的 vulcanizeMachineCount
        // 已在 ContinueTaskProcessor 中扣减保底预留量，传入本方法的 totalDemand 已是扣减后的剩余需求。
        // 而保底预留（reservedHistoryTasks）会从任务 vulcanizeMachineCount 扣减，代表已满足的需求，
        // 应计入 isComplete() 判定。因此需将两者分开计量。
        int continuePreloadLoad = 0;
        for (MachineState s : machineStates) {
            continuePreloadLoad += s.getCurrentLoad();
        }

        // ---- 步骤5: 保底预留 ----
        // 【第二轮差异】config.enforceReservedHistory=false 时跳过保底预留
        // 第一轮：enforceReservedHistory=forceKeepHistory，与原逻辑一致
        // 第二轮：enforceReservedHistory=false，不强制预留
        if (config.isEnforceReservedHistory() && forceKeepHistory) {
            reservedHistoryTasks(sortedTasks, machineStates, context);
        }

        // ---- capacitySufficient 计算 ----
        // 【第二轮差异】capacitySufficientOverride 非 null 时强制使用该值（第二轮=true，产能无限）
        int totalCapacity = 0;
        for (MpCxCapacityConfiguration mc : availableMachines) {
            Integer maxLh = machineMaxLhMap.get(mc.getCxMachineCode());
            totalCapacity += (maxLh != null ? maxLh : DEFAULT_MAX_LH_MACHINE_QTY);
        }
        boolean capacitySufficient = config.getCapacitySufficientOverride() != null
                ? config.getCapacitySufficientOverride()
                : (totalCapacity >= totalDemand);

        if (!capacitySufficient && config.isEnforceCapacityLimit()) {
            log.warn("产能不足（产能={}, 需求={}），DFS将尝试最优部分解", totalCapacity, totalDemand);
        }

        // ---- 步骤6: DFS 搜索 ----
        // preOccupiedLoad = 续作预扣 + 保底预留已占负荷（第二轮仅续作预扣）
        int preOccupiedLoad = 0;
        for (MachineState s : machineStates) {
            preOccupiedLoad += s.getCurrentLoad();
        }
        // reservedHistoryLoad = 保底预留已占负荷（代表已满足的需求，计入 isComplete）
        // 续作预扣（continuePreloadLoad）不代表已满足的需求，不计入 isComplete
        int reservedHistoryLoad = preOccupiedLoad - continuePreloadLoad;
        log.info("{} 续作预扣总负荷={}, 保底预留负荷={}, DFS剩余任务总需求={}",
                config.getRoundName(), preOccupiedLoad, reservedHistoryLoad, totalDemand);

        DfsSearchResult searchResult = new DfsSearchResult();
        searchResult.bestScore = Integer.MAX_VALUE;
        searchResult.bestAssignedCount = 0;
        searchResult.bestIsBalanced = false;
        searchResult.bestAssignments = null;
        searchResult.bestMachineCodes = null;
        searchResult.searchCount = 0;
        searchResult.pruneCount = 0;
        searchResult.dfsAssignedQty = 0;
        searchResult.config = config;  // 传递轮次配置，DFS 内部通过 searchResult.config 读取策略

        List<CoreScheduleAlgorithmService.DailyEmbryoTask> remainingTasks = getRemainingTasks(sortedTasks);

        // 【修复】DFS totalDemand：第一轮用原始值（保持不变），第二轮用剩余需求（启用B2剪枝+正确日志）
        // 原始 totalDemand 含续作预扣/保底预留，bestAssignedCount 不含，等式永不成立；
        // 第二轮使用 remainingTasks 的 vulcanizeMachineCount 之和，使 B2 剪枝和结果判定正确生效。
        int dfsTotalDemand = totalDemand;
        if (!config.isEnforceCapacityLimit()) {
            // 第二轮：使用剩余任务的实际需求之和
            dfsTotalDemand = remainingTasks.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();
            log.info("{} DFS总需求(剩余)={}，预扣负荷={}", config.getRoundName(), dfsTotalDemand, preOccupiedLoad);
        }

        if (!remainingTasks.isEmpty()) {
            List<String> taskList = remainingTasks.stream()
                    .map(t -> t.getEmbryoCode() + "(" + (t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0) + ")")
                    .collect(Collectors.toList());
            log.info("{} DFS任务列表（排序后，共{}个）：{}", config.getRoundName(), remainingTasks.size(), taskList);
        }

        int initialRemainingCount = remainingTasks.isEmpty() ? 0
                : (remainingTasks.get(0).getVulcanizeMachineCount() != null
                ? remainingTasks.get(0).getVulcanizeMachineCount() : 0);

        dfsAssign(remainingTasks, 0, initialRemainingCount, machineStates, forceKeepHistory,
                typeDiffThreshold, loadDiffThreshold, dfsTotalDemand, searchResult, capacitySufficient, preOccupiedLoad);

        log.info("{} DFS搜索统计：总搜索次数={}, 剪枝次数={}, 最优分数={}, 均衡={}, 最优已分配={}/{}",
                config.getRoundName(),
                searchResult.searchCount, searchResult.pruneCount, searchResult.bestScore,
                searchResult.bestIsBalanced ? "满足" : "不满足",
                searchResult.bestAssignedCount, dfsTotalDemand);

        // 分配不足时输出缺口明细（诊断用）
        if (searchResult.bestAssignedCount < dfsTotalDemand && searchResult.bestAssignments != null) {
            log.info("{} 检测到分配不足：已分配={}/总需求={}",
                    config.getRoundName(), searchResult.bestAssignedCount, dfsTotalDemand);
            Map<String, Integer> assignedQtyMap = new java.util.HashMap<>();
            for (List<EmbryoAssignment> assignments : searchResult.bestAssignments) {
                for (EmbryoAssignment ea : assignments) {
                    assignedQtyMap.merge(ea.getEmbryoCode(), ea.getAssignedQty(), Integer::sum);
                }
            }
            Map<String, Integer> demandQtyMap = new java.util.LinkedHashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask t : remainingTasks) {
                String key = t.getEmbryoCode()
                        + (t.getIsProductionTrial() != null && t.getIsProductionTrial() ? "(量试)" : "(正式)")
                        + (t.getConstrainedMachineCode() != null ? "[约束:" + t.getConstrainedMachineCode() + "]" : "");
                demandQtyMap.merge(key, t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 1, Integer::sum);
            }
            Map<String, Integer> demandByEmbryo = new java.util.HashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask t : remainingTasks) {
                demandByEmbryo.merge(t.getEmbryoCode(), t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 1, Integer::sum);
            }
            List<String> shortageItems = new java.util.ArrayList<>();
            for (Map.Entry<String, Integer> e : demandByEmbryo.entrySet()) {
                int assigned = assignedQtyMap.getOrDefault(e.getKey(), 0);
                if (assigned < e.getValue()) {
                    shortageItems.add(e.getKey() + "(" + assigned + "/" + e.getValue() + ")");
                }
            }
            if (!shortageItems.isEmpty()) {
                log.warn("{} 分配不足的胎胚：{}，各任务明细：{}", config.getRoundName(), shortageItems, demandQtyMap);
            }
        }

        // ---- 步骤7: 结果转换 ----
        BalancingResult result;
        if (searchResult.bestAssignments != null) {
            if (searchResult.bestAssignedCount == dfsTotalDemand) {
                result = convertDfsResultToBalancingResult(searchResult.bestAssignments, searchResult.bestMachineCodes, machineStates, sortedTasks);
                log.info("{} 找到满足均衡条件的完整方案，已分配 {} 台硫化机", config.getRoundName(), searchResult.bestAssignedCount);
            } else {
                log.warn("{} DFS搜索完成：找到最优但不完备的解（已分配 {}/{}），直接使用此结果",
                        config.getRoundName(), searchResult.bestAssignedCount, dfsTotalDemand);
                result = convertDfsResultToBalancingResult(searchResult.bestAssignments, searchResult.bestMachineCodes, machineStates, sortedTasks);
            }
        } else {
            boolean hasReservedAssignments = machineStates.stream()
                    .anyMatch(s -> !s.getAssignedEmbryos().isEmpty());
            if (hasReservedAssignments) {
                log.info("{} DFS无剩余任务，但存在保底预留分配，收集预留结果", config.getRoundName());
                result = buildResultFromMachineStates(machineStates);
            } else {
                log.warn("{} DFS未找到任何方案，返回空结果", config.getRoundName());
                BalancingResult emptyResult = new BalancingResult();
                emptyResult.setAssignments(new ArrayList<>());
                result = emptyResult;
            }
        }

        logAllocationResult(result, machineStates, remainingTasks);

        // ---- 构建 RoundExecutionResult ----
        RoundExecutionResult roundResult = new RoundExecutionResult();
        roundResult.setResult(result);
        roundResult.setDfsAssignedCount(searchResult.bestAssignedCount);
        roundResult.setPreOccupiedLoad(reservedHistoryLoad);
        roundResult.setTotalOriginalDemand(totalOriginalDemand);
        roundResult.setSearchCount(searchResult.searchCount);
        roundResult.setPruneCount(searchResult.pruneCount);

        log.info("====== {} 结束，完整={}，已分配={}/{}（保底预留+DFS） ======",
                config.getRoundName(), roundResult.isComplete(),
                reservedHistoryLoad + searchResult.bestAssignedCount, totalOriginalDemand);

        return roundResult;
    }

    /**
     * 创建第一轮配置（严格均衡：容量管控 + 按参数保底预留）。
     * <p>行为与原单轮逻辑完全一致，确保第一轮保持现有逻辑不变。
     *
     * @param forceKeepHistory SYS04070003 参数值（Y=true 时 DFS 前保底预留）
     * @return 第一轮配置实例
     */
    private BalancingRoundConfig createRound1Config(boolean forceKeepHistory) {
        BalancingRoundConfig config = new BalancingRoundConfig();
        config.setRoundName("第一轮-严格均衡");
        config.setEnforceCapacityLimit(true);       // 强制 maxCapacity 管控
        config.setEnforceReservedHistory(forceKeepHistory);  // 按参数决定是否保底预留
        config.setBoostHistoryPreference(false);     // 历史偏好为排序④级（原逻辑）
        config.setCapacitySufficientOverride(null);  // 按实际供需计算
        config.setEnforceContinuePreload(true);      // 续作预扣（与原逻辑一致）
        return config;
    }

    /**
     * 创建第二轮配置（宽松均衡：解除容量上限 + 解除保底预留 + 历史优先/同胎胚集中/均衡例外排序）。
     *
     * <p><b>触发条件</b>：第一轮未获得完整解（{@code RoundExecutionResult.isComplete()=false}），
     * 即 存在未分配的任务（预扣负荷 + DFS分配数 < 原始总需求）。
     *
     * <p><b>与第一轮的差异</b>：
     * <ul>
     *   <li>enforceCapacityLimit=false：解除容量上限，允许超载分配</li>
     *   <li>enforceReservedHistory=false：不强制保底预留</li>
     *   <li>boostHistoryPreference=true：启用第二轮排序策略（历史优先+同胎胚集中+均衡例外）</li>
     *   <li>capacitySufficientOverride=true：强制产能充足策略（均衡优先排序）</li>
     *   <li>enforceContinuePreload=false：不预扣续作，释放种类槽供新胎胚使用</li>
     * </ul>
     *
     * @return 第二轮配置实例
     */
    private BalancingRoundConfig createRound2Config() {
        BalancingRoundConfig config = new BalancingRoundConfig();
        config.setRoundName("第二轮-宽松均衡");
        config.setEnforceCapacityLimit(false);      // 解除容量上限
        config.setEnforceReservedHistory(false);     // 不强制保底预留
        config.setBoostHistoryPreference(true);      // 启用第二轮排序策略（历史优先+同胎胚集中+均衡例外）
        config.setCapacitySufficientOverride(Boolean.TRUE);  // 强制产能充足
        config.setEnforceContinuePreload(false);     // 不预扣续作，释放种类槽供新胎胚使用
        return config;
    }

    /**
     * 快照所有任务的 vulcanizeMachineCount — 供第二轮恢复原始需求。
     *
     * <p>第一轮保底预留会从 vulcanizeMachineCount 扣减已预留台数，
     * 第二轮需要恢复到原始值才能重新分配全部需求。
     *
     * @param tasks 待快照任务列表
     * @return lhId → 原始 vulcanizeMachineCount 映射
     */
    private Map<Long, Integer> snapshotDemands(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        Map<Long, Integer> snapshot = new HashMap<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
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
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> sortedTasks,
            Map<Long, Integer> snapshot) {
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : sortedTasks) {
            Integer original = snapshot.get(task.getLhId());
            if (original != null) {
                task.setVulcanizeMachineCount(original);
            }
        }
        log.info("第二轮需求已从快照恢复，共恢复 {} 个任务的 vulcanizeMachineCount", sortedTasks.size());
    }


    // ==================== DFS 核心（dfsAssign 及辅助） ====================

    /**
     * 5.3.3.2.5.5 子步骤 — 保底预留历史胎胚（SYS04070003=Y）。
     *
     * <p>对每个机台 historyEmbryos 中的胎胚：若任务仍有 vulcanizeMachineCount&gt;0，
     * 在该机台预留 1 台硫化机，并从任务需求量扣减 1。
     *
     * <p>与 {@link ContinueTaskProcessor} 的区别：此处写入 {@link MachineState} 供 DFS 继承，
     * ContinueTaskProcessor 则产出 continueAllocations 供 NewTaskProcessor 构建预扣 Map。
     */
    private void reservedHistoryTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MachineState> machineStates,
            ScheduleContextVo context) {

        log.info("开始保底预留历史任务...");

        int totalReserved = 0;

        for (MachineState state : machineStates) {
            Set<String> historyEmbryos = state.getHistoryEmbryos();
            if (historyEmbryos == null || historyEmbryos.isEmpty()) {
                continue;
            }

            for (String embryoCode : historyEmbryos) {
                // 直接遍历 tasks 列表按 embryoCode 匹配，避免 Map 去2重丢失多机台同胚胎的预留
                CoreScheduleAlgorithmService.DailyEmbryoTask matchedTask = null;
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                    if (embryoCode.equals(task.getEmbryoCode())) {
                        int remainingDemand = task.getVulcanizeMachineCount() != null
                                ? task.getVulcanizeMachineCount() : 0;
                        if (remainingDemand > 0) {
                            matchedTask = task;
                            break;
                        }
                    }
                }

                if (matchedTask == null) {
                    continue;
                }

                // matchedTask 仅在 vulcanizeMachineCount>0 时被选中，此处无需再判 null
                int remainingDemand = matchedTask.getVulcanizeMachineCount();

                // 按 lhMachineCode 去重判断是否为新硫化机
                String lhMc = matchedTask.getLhMachineCode();
                String resMachineKey = lhMc != null ? lhMc : "task_" + matchedTask.getLhId();
                boolean resIsNewLhMachine = !state.getAssignedLhMachineCodes().contains(resMachineKey);

                // 新硫化机才检查容量上限；已有硫化机不占负荷，不受限
                if (resIsNewLhMachine && state.getCurrentLoad() >= state.getMaxCapacity()) {
                    log.warn("机台 {} 容量已满，无法保底预留胎胚 {}",
                            state.getMachineCode(), embryoCode);
                    continue;
                }

                // 检查胎胚种类数是否已达上限
                boolean isNewTypeForHistory = state.getAssignedEmbryos().stream()
                        .noneMatch(e -> e.getEmbryoCode().equals(embryoCode));
                if (isNewTypeForHistory && state.getCurrentTypes() >= state.getMaxTypes()) {
                    log.warn("机台 {} 胎胚种类已达上限 ({}/{})，无法保底预留胎胚 {}",
                            state.getMachineCode(), state.getCurrentTypes(), state.getMaxTypes(), embryoCode);
                    continue;
                }

                // 保底预留1个硫化机台数
                int reservedCount = 1;

                // 按 lhMachineCode 去重：已有硫化机不增负荷（resIsNewLhMachine 已在上方计算）
                int loadInc = resIsNewLhMachine ? reservedCount : 0;

                state.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, matchedTask, reservedCount));
                state.setCurrentLoad(state.getCurrentLoad() + loadInc);
                if (resIsNewLhMachine) {
                    state.getAssignedLhMachineCodes().add(resMachineKey);
                }
                state.setCurrentTypes(state.getCurrentTypes() + 1);

                matchedTask.setVulcanizeMachineCount(remainingDemand - reservedCount);

                totalReserved++;
                log.info("机台 {} 保底预留胎胚 {} 共 {} 个硫化机",
                        state.getMachineCode(), embryoCode, reservedCount);
            }
        }

        log.info("保底预留完成，共预留 {} 个任务", totalReserved);
    }

    /**
     * 提取 vulcanizeMachineCount&gt;0 的任务列表，作为 DFS 输入。
     *
     * <p>保底预留 / 续作预扣已在之前步骤从 vulcanizeMachineCount 扣减，此处只保留仍有需求的任务。
     */
    private List<CoreScheduleAlgorithmService.DailyEmbryoTask> getRemainingTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        return tasks.stream()
                .filter(t -> t.getVulcanizeMachineCount() != null && t.getVulcanizeMachineCount() > 0)
                .collect(Collectors.toList());
    }

    /**
     * DFS 深度优先搜索 + 剪枝 — 核心分配算法。
     *
     * <p><b>递归模型</b>：按 sortedTasks 顺序处理；每个任务可拆分到多机台。
     * {@code remainingCount} 表示当前任务（taskIndex）尚未分配的硫化机台数。
     *
     * <p><b>递归参数</b>
     * <ul>
     *   <li>{@code taskIndex} — 当前任务在 sortedTasks 中的下标</li>
     *   <li>{@code remainingCount} — 当前任务剩余待分配硫化机数；0 表示该任务已完成，推进到下一任务</li>
     *   <li>{@code preOccupiedLoad} — 续作预扣+保底预留已占负荷，评估新分配数时需扣除</li>
     * </ul>
     *
     * <p><b>执行阶段</b>
     * <ol>
     *   <li>A. 防护 — searchCount&gt;100万 强制返回</li>
     *   <li>B. 剪枝 — 剩余产能=0 时记录部分/完整解后返回；已找到均衡完整解后启用贪心上界剪枝</li>
     *   <li>C. 终止 — taskIndex≥tasks.size() 时评估完整解或部分解，更新 searchResult</li>
     *   <li>D. 分配 — remainingCount&gt;0：找候选机台 → 尝试 assignQty(大到小) → 递归 → 回溯</li>
     *   <li>E. 推进 — remainingCount=0：启动下一 taskIndex 或跳过零需求任务</li>
     * </ol>
     *
     * <p><b>解的择优</b>（见 {@link DfsSearchResult}）：完整度 &gt; 是否均衡 &gt; calculateBalancingScore。
     */
    private void dfsAssign(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            int taskIndex,
            int remainingCount,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            int typeDiffThreshold,
            int loadDiffThreshold,
            int totalDemand,
            DfsSearchResult searchResult,
            boolean capacitySufficient,
            int preOccupiedLoad) {

        searchResult.searchCount++;
        searchResult.callCount++;

        // --- A. 搜索次数上限（硬终止，防极端结构卡死） ---
        // 第一轮：100万次（与原始逻辑一致）；第二轮：500万次（无容量限制，搜索空间更大）
        int searchLimit = searchResult.config.isEnforceCapacityLimit() ? 1000000 : 5000000;
        if (searchResult.searchCount > searchLimit) {
            return;
        }

        // --- B1. 剩余产能为零剪枝 ---
        //
        // 触发条件：ΣcurrentLoad == ΣmaxCapacity，后续分支无法再增加任何分配，直接剪枝返回。
        // 注意：产能不足（总产能<总需求）时通常不会在此处触发，而是在 C 终止层记录部分解。
        //
        // 变量：
        //   totalAssignedNow  = ΣcurrentLoad − preOccupiedLoad   （本次 DFS 新分配的硫化机数）
        //   totalRequiredAll  = Σtask.vulcanizeMachineCount      （剩余任务总需求）
        //
        // 伪代码（与 C 终止层择优规则一致）：
        // <pre>
        // if (remainingCapacity &lt;= 0) {
        //     assigned = totalAssignedNow
        //     required = totalRequiredAll
        //
        //     if (assigned &lt; required) {
        //         // 【部分解】任务未分完但机台已满，记录当前最优部分解
        //         if (best 仍是部分解) {
        //             if (assigned &gt; bestAssignedCount)           → 替换（多分配优先）
        //             else if (assigned == bestAssignedCount
        //                      &amp;&amp; score &lt; bestScore)            → 替换（同完整度比均衡分）
        //         } else {
        //             // best 已是完整解，部分解不覆盖完整解
        //             不替换
        //         }
        //     } else {
        //         // 【完整解】assigned &gt;= required，所有任务需求已满足（可能仍有未遍历的 taskIndex）
        //         if (best 仍是部分解)                              → 替换
        //         else if (当前均衡 &amp;&amp; best 不均衡)               → 替换
        //         else if (均衡等级相同 &amp;&amp; score &lt; bestScore)    → 替换
        //     }
        //     pruneCount++; return;
        // }
        // </pre>
        int allCurrentLoad = 0;
        int allCapacity = 0;
        for (MachineState s : machineStates) {
            allCurrentLoad += s.getCurrentLoad();
            allCapacity += s.getMaxCapacity();
        }
        int remainingCapacity = allCapacity - allCurrentLoad;
        // 【第二轮差异】enforceCapacityLimit=false 时跳过容量剪枝
        // 第二轮允许超载分配，remainingCapacity 可能为负但不应终止搜索；
        // 仅 maxTypes 约束（findCandidateMachinesForSplit 中保留）可阻止分配，
        // 无候选时由 D1/D4 层记录部分解，所有任务完成时由 C 层记录完整解。
        if (searchResult.config.isEnforceCapacityLimit() && remainingCapacity <= 0) {
            int totalAssignedNow = searchResult.dfsAssignedQty;
            int totalRequiredAll = tasks.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();
            if (totalAssignedNow < totalRequiredAll) {
                // 部分解分支（见上方伪代码 if assigned < required）
                int partialScore = calculateBalancingScore(machineStates);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequiredAll);
                if (!currentBestIsComplete &&
                        (totalAssignedNow > searchResult.bestAssignedCount ||
                                (totalAssignedNow == searchResult.bestAssignedCount && partialScore < searchResult.bestScore))) {
                    searchResult.bestScore = partialScore;
                    searchResult.bestAssignedCount = totalAssignedNow;
                    searchResult.bestIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            } else {
                // 完整解分支（见上方伪代码 else assigned >= required）
                int score = calculateBalancingScore(machineStates);
                boolean currentIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequiredAll);
                boolean shouldReplace = false;
                if (!currentBestIsComplete) {
                    shouldReplace = true;
                } else if (currentIsBalanced && !searchResult.bestIsBalanced) {
                    shouldReplace = true;
                } else if (currentIsBalanced == searchResult.bestIsBalanced && score < searchResult.bestScore) {
                    shouldReplace = true;
                }
                if (shouldReplace) {
                    searchResult.bestScore = score;
                    searchResult.bestAssignedCount = totalAssignedNow;
                    searchResult.bestIsBalanced = currentIsBalanced;
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            }
            searchResult.pruneCount++;
            return;
        }

        // --- B2. 贪心上界剪枝（仅已存在「均衡完整解」时启用，加速收敛） ---
        if (searchResult.bestAssignedCount >= totalDemand && searchResult.bestIsBalanced) {
            int curMaxLoad = 0;
            for (MachineState s : machineStates) {
                if (s.getCurrentLoad() > curMaxLoad) {
                    curMaxLoad = s.getCurrentLoad();
                }
            }
            // 贪心解的负荷下界：总已分配 / 机台数
            int greedyLoadLowerBound = allCurrentLoad / machineStates.size();
            if (curMaxLoad - greedyLoadLowerBound > loadDiffThreshold) {
                searchResult.pruneCount++;
                return;
            }
        }

        // --- C. 终止：所有任务索引已遍历 ---
        if (taskIndex >= tasks.size()) {
            int totalAssigned = searchResult.dfsAssignedQty;
            int totalRequired = tasks.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();

            if (totalAssigned == totalRequired) {
                // C1. 完整解择优
                int score = calculateBalancingScore(machineStates);
                boolean currentIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequired);

                boolean shouldReplace = false;
                if (!currentBestIsComplete) {
                    // 之前是部分解，当前完整解更优
                    shouldReplace = true;
                } else if (currentIsBalanced && !searchResult.bestIsBalanced) {
                    // 之前是不均衡完整解，当前是均衡完整解 → 替换
                    shouldReplace = true;
                } else if (currentIsBalanced == searchResult.bestIsBalanced && score < searchResult.bestScore) {
                    // 同等均衡等级，分数更优 → 替换
                    shouldReplace = true;
                }

                if (shouldReplace) {
                    searchResult.bestScore = score;
                    searchResult.bestAssignedCount = totalAssigned;
                    searchResult.bestIsBalanced = currentIsBalanced;
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            } else {
                // C2. 部分解择优（完整度优先于均衡分数）
                int partialScore = calculateBalancingScore(machineStates);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequired);
                if (!currentBestIsComplete &&
                        (totalAssigned > searchResult.bestAssignedCount ||
                                (totalAssigned == searchResult.bestAssignedCount && partialScore < searchResult.bestScore))) {
                    searchResult.bestScore = partialScore;
                    searchResult.bestAssignedCount = totalAssigned;
                    searchResult.bestIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            }
            return;
        }

        CoreScheduleAlgorithmService.DailyEmbryoTask task = tasks.get(taskIndex);
        String embryoCode = task.getEmbryoCode();

        // --- D. 当前任务仍有 remainingCount 台待分配 ---
        if (remainingCount > 0) {
            List<MachineState> candidates = findCandidateMachinesForSplit(
                    embryoCode, task.getLhMachineCode(), machineStates, forceKeepHistory, searchResult.searchCount == 1,
                    task.getConstrainedMachineCode(), searchResult.config);

            if (candidates.isEmpty()) {
                // D1. 无候选：记录部分解，跳过本任务继续后续任务
                int totalAssignedNow = searchResult.dfsAssignedQty;
                int totalRequiredAll = tasks.stream()
                        .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                        .sum();
                int partialScore = calculateBalancingScore(machineStates);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequiredAll);
                if (!currentBestIsComplete &&
                        (totalAssignedNow > searchResult.bestAssignedCount ||
                                (totalAssignedNow == searchResult.bestAssignedCount && partialScore < searchResult.bestScore))) {
                    searchResult.bestScore = partialScore;
                    searchResult.bestAssignedCount = totalAssignedNow;
                    searchResult.bestIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
                // 跳过当前任务，递归处理下一个
                dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient, preOccupiedLoad);
                return;
            }

            sortCandidatesForDfs(candidates, embryoCode, task.getLhMachineCode(), forceKeepHistory, capacitySufficient, loadDiffThreshold, searchResult.config);

            // D2. 对每个候选机台尝试 assignQty（从大到小，优先填满单机）
            boolean anyRecursed = false;
            String lhMachineCode = task.getLhMachineCode();
            String machineKey = (lhMachineCode != null && !lhMachineCode.isEmpty()) ? lhMachineCode : "task_" + task.getLhId();
            for (MachineState candidate : candidates) {
                // 判断是否为新硫化机（已有硫化机不占负荷，不受 maxCapacity 限制）
                boolean isNewLhMachine = !candidate.getAssignedLhMachineCodes().contains(machineKey);
                int maxCanAssign;
                if (isNewLhMachine) {
                    // 【第二轮差异】enforceCapacityLimit=false 时不受 maxCapacity 限制，可分配全部剩余需求
                    if (searchResult.config.isEnforceCapacityLimit()) {
                        maxCanAssign = Math.min(remainingCount,
                                candidate.getMaxCapacity() - candidate.getCurrentLoad());
                    } else {
                        maxCanAssign = remainingCount;
                    }
                } else {
                    maxCanAssign = remainingCount; // 不占负荷
                }

                if (maxCanAssign <= 0) {
                    continue;
                }

                // 尝试分配不同的数量（从大到小，优先填满一台机台）
                for (int assignQty = maxCanAssign; assignQty >= 1; assignQty--) {
                    int newTypes = candidate.getCurrentTypes();
                    boolean isNewType = !candidate.getAssignedEmbryos().stream()
                            .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                    if (isNewType) {
                        newTypes++;
                    }

                    // 硬约束剪枝：新机台种类数超过 maxTypes
                    if (isNewType && newTypes > candidate.getMaxTypes()) {
                        searchResult.pruneCount++;
                        continue;
                    }

                    // 软剪枝：剩余种类槽不足以容纳后续未分配胎胚种类（仅已有完整解时启用）
                    int remainingTypeCapacity = 0;
                    for (MachineState state : machineStates) {
                        remainingTypeCapacity += state.getMaxTypes() - state.getCurrentTypes();
                        if (state == candidate && isNewType) {
                            remainingTypeCapacity--; // 当前分配已占1个
                        }
                    }
                    // 统计剩余未分配的胎胚种类数（当前任务之后的）
                    int remainingDistinctTypes = 0;
                    Set<String> assignedEmbryoSet = new HashSet<>();
                    for (MachineState state : machineStates) {
                        for (EmbryoAssignment ea : state.getAssignedEmbryos()) {
                            assignedEmbryoSet.add(ea.getEmbryoCode());
                        }
                    }
                    if (isNewType) {
                        assignedEmbryoSet.add(embryoCode);
                    }
                    for (int i = taskIndex; i < tasks.size(); i++) {
                        String nextEmbryo = tasks.get(i).getEmbryoCode();
                        if (!assignedEmbryoSet.contains(nextEmbryo)) {
                            remainingDistinctTypes++;
                            assignedEmbryoSet.add(nextEmbryo);
                        }
                    }
                    // 仅在已找到完整解后启用种类可行性剪枝
                    if (remainingDistinctTypes > remainingTypeCapacity
                            && searchResult.bestAssignedCount >= totalDemand) {
                        searchResult.pruneCount++;
                        continue;
                    }

                    // D3. 试探分配 → 递归 → 回溯
                    // currentLoad 增量按 lhMachineCode 去重（新硫化机才增负荷）
                    int loadInc = isNewLhMachine ? assignQty : 0;
                    int newLoad = candidate.getCurrentLoad() + loadInc;
                    candidate.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, assignQty));
                    candidate.setCurrentLoad(newLoad);
                    if (isNewLhMachine) {
                        candidate.getAssignedLhMachineCodes().add(machineKey);
                    }
                    if (isNewType) {
                        candidate.setCurrentTypes(candidate.getCurrentTypes() + 1);
                    }

                    // 维护 dfsAssignedQty（DFS 新分配的 assignedQty 之和）
                    searchResult.dfsAssignedQty += assignQty;

                    int newRemainingCount = remainingCount - assignQty;

                    // 如果当前胎胚还有剩余，继续分配；否则处理下一个任务
                    anyRecursed = true;
                    if (newRemainingCount > 0) {
                        dfsAssign(tasks, taskIndex, newRemainingCount, machineStates, forceKeepHistory,
                                typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient, preOccupiedLoad);
                    } else {
                        dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                                typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient, preOccupiedLoad);
                    }

                    // 回溯
                    searchResult.dfsAssignedQty -= assignQty;
                    candidate.getAssignedEmbryos().remove(candidate.getAssignedEmbryos().size() - 1);
                    candidate.setCurrentLoad(candidate.getCurrentLoad() - loadInc);
                    if (isNewLhMachine) {
                        candidate.getAssignedLhMachineCodes().remove(machineKey);
                    }
                    if (isNewType) {
                        candidate.setCurrentTypes(candidate.getCurrentTypes() - 1);
                    }
                }
            }

            // D4. 所有候选均被剪枝：跳过本任务，继续后续
            if (!anyRecursed) {
                int totalAssignedNow = searchResult.dfsAssignedQty;
                int totalRequiredAll = tasks.stream()
                        .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                        .sum();
                int partialScore = calculateBalancingScore(machineStates);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequiredAll);
                if (!currentBestIsComplete &&
                        (totalAssignedNow > searchResult.bestAssignedCount ||
                                (totalAssignedNow == searchResult.bestAssignedCount && partialScore < searchResult.bestScore))) {
                    searchResult.bestScore = partialScore;
                    searchResult.bestAssignedCount = totalAssignedNow;
                    searchResult.bestIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
                // 跳过当前任务，递归处理下一个
                dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient, preOccupiedLoad);
            }
        } else {
            // --- E. remainingCount=0：推进 taskIndex ---
            // C 层已保证 taskIndex < tasks.size()；task 已在方法前部绑定为 tasks.get(taskIndex)
            int currentLhCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;

            if (currentLhCount <= 0) {
                // 当前任务不需要分配，跳到下一个
                dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient, preOccupiedLoad);
            } else {
                // 开始处理当前任务（传入其完整需求量）
                dfsAssign(tasks, taskIndex, currentLhCount, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient, preOccupiedLoad);
            }
        }
    }

    /**
     * 静态排序辅助 — 创建容量充足的临时 MachineState（仅用于 countCandidatesForStaticSort）。
     */
    private MachineState createTempMachineState(Set<String> availableMachineCodes) {
        MachineState state = new MachineState();
        // 临时设置一个很大的容量，使其在初始时总是可用
        state.setCurrentLoad(0);
        state.setCurrentTypes(0);
        state.setMaxCapacity(100);
        state.setMaxTypes(DEFAULT_MAX_TYPES_PER_MACHINE);
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
        // 这里做一个粗略估算：
        // 如果数字在 215104000-215104600 范围内（22个胚子），后几个候选更少
        if (num >= 215103130) {
            // 肽子 21-22：候选极少
            return 2;
        } else if (num >= 215103000) {
            // 肽子 11-20：候选较少
            return 3;
        } else {
            return 3;
        }
    }

    /**
     * 查找可接收当前胎胚的候选机台（DFS 分配层 D2 的前置步骤）。
     *
     * <p>候选条件（全部满足）：
     * <ol>
     *   <li>量试约束：{@code constrainedMachineCode} 非空时仅该机台可接收</li>
     *   <li>容量：{@code currentLoad &lt; maxCapacity}</li>
     *   <li>种类：新胎胚时 {@code currentTypes &lt; maxTypes}；已有胎胚不占新种类槽</li>
     * </ol>
     *
     * <p>单胎胚的硫化机数可在多个候选机台间拆分（由 dfsAssign 的 assignQty 循环实现）。
     */
    private List<MachineState> findCandidateMachinesForSplit(
            String embryoCode,
            String lhMachineCode,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            boolean isFirstCall,
            String constrainedMachineCode,
            BalancingRoundConfig config) {

        // lhMachineCode 为空时 machineKey=null，isNewLhMachine 恒为 true（降级为每条记录各算1台）
        String machineKey = (lhMachineCode != null && !lhMachineCode.isEmpty()) ? lhMachineCode : null;
        List<MachineState> candidates = new ArrayList<>();

        for (MachineState state : machineStates) {
            // 约束机台过滤：如果该胎胚有约束机台，只能分配到指定机台
            if (constrainedMachineCode != null && !constrainedMachineCode.isEmpty()
                    && !state.getMachineCode().equals(constrainedMachineCode)) {
                continue;
            }
            // 新硫化机：容量满才跳过；已有硫化机：不跳过（不占负荷）
            // 【第二轮差异】config.enforceCapacityLimit=false 时跳过容量检查，允许超载分配
            boolean isNewLhMachine = machineKey == null || !state.getAssignedLhMachineCodes().contains(machineKey);
            if (isNewLhMachine && config.isEnforceCapacityLimit()
                    && state.getCurrentLoad() >= state.getMaxCapacity()) {
                log.trace("  [-满载] 机台 {}", state.getMachineCode());
                continue;
            }
            // 胎胚种类数已达上限，且当前胎胚是新种类，跳过
            boolean isNewType = state.getAssignedEmbryos().stream()
                    .noneMatch(e -> e.getEmbryoCode().equals(embryoCode));
            if (isNewType && state.getCurrentTypes() >= state.getMaxTypes()) {
                log.trace("  [-种类满] 机台 {}", state.getMachineCode());
                continue;
            }
            candidates.add(state);
        }

        // 仅在候选为空或首次搜索时打印，避免重复噪音
        if (candidates.isEmpty() || isFirstCall) {
            StringBuilder skipInfo = new StringBuilder();
            for (MachineState s : machineStates) {
                if (!candidates.contains(s)) {
                    if (s.getCurrentLoad() >= s.getMaxCapacity()) {
                        skipInfo.append("满载(")
                                .append(s.getCurrentLoad())
                                .append("/")
                                .append(s.getMaxCapacity())
                                .append(")/");
                    } else {
                        boolean isNew = s.getAssignedEmbryos().stream()
                                .noneMatch(e -> e.getEmbryoCode().equals(embryoCode));
                        if (isNew) {
                            skipInfo.append("种类满(")
                                    .append(s.getCurrentTypes())
                                    .append("/")
                                    .append(s.getMaxTypes())
                                    .append(")/");
                        }
                    }
                }
            }
            if (!candidates.isEmpty()) {
                log.info("胎胚 {} 候选机台({}): [{}] | 跳过: {}",
                        embryoCode, candidates.size(),
                        candidates.stream().map(MachineState::getMachineCode).collect(Collectors.joining(",")),
                        skipInfo);
            }
        }

        return candidates;
    }

    /**
     * DFS 候选机台排序 — 策略随产能是否充足分支。
     *
     * <p><b>产能充足</b>（totalCapacity≥totalDemand）：优先均衡负荷，其次节省种类槽、偏好历史胎胚。
     * <p><b>产能不足</b>：优先节省种类槽（已在机台的胎胚绝对优先），尽量多排，负荷均衡降为次要。
     */
    private void sortCandidatesForDfs(
            List<MachineState> candidates,
            String embryoCode,
            String lhMachineCode,
            boolean forceKeepHistory,
            boolean capacitySufficient,
            int loadDiffThreshold,
            BalancingRoundConfig config) {

        // lhMachineCode 为空时 machineKey=null，不触发"已有硫化机"优先级（降级行为）
        String machineKey = (lhMachineCode != null && !lhMachineCode.isEmpty()) ? lhMachineCode : null;

        // 【第二轮差异】boostHistoryPreference=true 时启用第二轮排序策略
        // 排序原则：①历史胎胚优先 → ②同胎胚集中(受均衡例外约束) → ③均衡性 → ④已有硫化机 → ⑤种类槽 → ⑥种类少
        if (config.isBoostHistoryPreference()) {
            // 计算候选机台中的最小负荷（用于均衡性例外判断）
            int minLoadAmongCandidates = Integer.MAX_VALUE;
            for (MachineState c : candidates) {
                minLoadAmongCandidates = Math.min(minLoadAmongCandidates, c.getCurrentLoad());
            }
            final int minLoad = minLoadAmongCandidates;

            candidates.sort((a, b) -> {
                // ① 历史胎胚优先原则：历史机台绝对优先，保障生产连贯性
                boolean aHasHistory = a.getHistoryEmbryos().contains(embryoCode);
                boolean bHasHistory = b.getHistoryEmbryos().contains(embryoCode);
                if (aHasHistory && !bHasHistory) {
                    return -1;
                }
                if (!aHasHistory && bHasHistory) {
                    return 1;
                }

                // ② 同胎胚任务集中原则（受均衡性优先例外约束）
                // 已有该胎胚的机台优先集中，但仅当其负荷未超出均衡阈值时生效；
                // 若负荷已超阈值（currentLoad - minLoad > loadDiffThreshold），
                // 则不优先集中，由③均衡性优先接管，将任务拆分到负荷更低的机台。
                boolean aAlreadyHas = a.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                boolean bAlreadyHas = b.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                boolean aLoadBalanced = (a.getCurrentLoad() - minLoad) <= loadDiffThreshold;
                boolean bLoadBalanced = (b.getCurrentLoad() - minLoad) <= loadDiffThreshold;
                // 集中有效 = 已有胎胚 且 负荷未超阈值
                boolean aConcentrationOk = aAlreadyHas && aLoadBalanced;
                boolean bConcentrationOk = bAlreadyHas && bLoadBalanced;
                if (aConcentrationOk && !bConcentrationOk) {
                    return -1;
                }
                if (!aConcentrationOk && bConcentrationOk) {
                    return 1;
                }

                // ③ 均衡性优先：负荷少的优先（当集中会导致不均衡时，按负荷拆分到不同机台）
                int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
                if (loadCompare != 0) {
                    return loadCompare;
                }

                // ④ 已有相同硫化机的机台优先（不增加负荷）
                boolean aHasLhMc = machineKey != null && a.getAssignedLhMachineCodes().contains(machineKey);
                boolean bHasLhMc = machineKey != null && b.getAssignedLhMachineCodes().contains(machineKey);
                if (aHasLhMc && !bHasLhMc) {
                    return -1;
                }
                if (!aHasLhMc && bHasLhMc) {
                    return 1;
                }

                // ⑤ 剩余种类容量大的优先
                if (!aAlreadyHas) {
                    int aRemainingTypes = a.getMaxTypes() - a.getCurrentTypes();
                    int bRemainingTypes = b.getMaxTypes() - b.getCurrentTypes();
                    int remainingCompare = Integer.compare(bRemainingTypes, aRemainingTypes);
                    if (remainingCompare != 0) {
                        return remainingCompare;
                    }
                }

                // ⑥ 种类少的优先
                return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
            });
            return;
        }

        // === 以下为第一轮逻辑（保持不变） ===
        if (capacitySufficient) {
            // 产能充足：负荷均衡 → 已有硫化机 → 已有胎胚 → 历史胎胚 → 剩余种类槽 → 种类少
            candidates.sort((a, b) -> {
                // 优先级1：负荷少的优先（均衡分配，避免集中）
                int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
                if (loadCompare != 0) {
                    return loadCompare;
                }

                // 优先级2：已有相同硫化机的机台优先（不增加负荷）
                boolean aHasLhMc = machineKey != null && a.getAssignedLhMachineCodes().contains(machineKey);
                boolean bHasLhMc = machineKey != null && b.getAssignedLhMachineCodes().contains(machineKey);
                if (aHasLhMc && !bHasLhMc) {
                    return -1;
                }
                if (!aHasLhMc && bHasLhMc) {
                    return 1;
                }

                // 优先级3：负荷相同时，已有胎胚优先（节省种类槽）
                boolean aAlreadyHas = a.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                boolean bAlreadyHas = b.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                if (aAlreadyHas && !bAlreadyHas) {
                    return -1;
                }
                if (!aAlreadyHas && bAlreadyHas) {
                    return 1;
                }

                // 优先级4：历史胎胚优先
                boolean aHasHistory = a.getHistoryEmbryos().contains(embryoCode);
                boolean bHasHistory = b.getHistoryEmbryos().contains(embryoCode);
                if (aHasHistory && !bHasHistory) {
                    return -1;
                }
                if (!aHasHistory && bHasHistory) {
                    return 1;
                }

                // 优先级5：同为未有时，剩余种类容量大的优先（保留稀缺种类槽）
                if (!aAlreadyHas) {
                    int aRemainingTypes = a.getMaxTypes() - a.getCurrentTypes();
                    int bRemainingTypes = b.getMaxTypes() - b.getCurrentTypes();
                    int remainingCompare = Integer.compare(bRemainingTypes, aRemainingTypes);
                    if (remainingCompare != 0) {
                        return remainingCompare;
                    }
                }

                // 优先级6：种类少的优先
                return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
            });
        } else {
            // 产能不足：已在机台 → 已有硫化机 → 历史胎胚 → 剩余种类槽 → 负荷少 → 种类少
            candidates.sort((a, b) -> {
                // 优先级1：胎胚已在机台上绝对优先（节省种类槽，多排任务）
                boolean aAlreadyHas = a.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                boolean bAlreadyHas = b.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                if (aAlreadyHas && !bAlreadyHas) {
                    return -1;
                }
                if (!aAlreadyHas && bAlreadyHas) {
                    return 1;
                }

                // 优先级2：已有相同硫化机的机台优先（不增加负荷）
                boolean aHasLhMc = machineKey != null && a.getAssignedLhMachineCodes().contains(machineKey);
                boolean bHasLhMc = machineKey != null && b.getAssignedLhMachineCodes().contains(machineKey);
                if (aHasLhMc && !bHasLhMc) {
                    return -1;
                }
                if (!aHasLhMc && bHasLhMc) {
                    return 1;
                }

                // 优先级3：历史胎胚优先
                boolean aHasHistory = a.getHistoryEmbryos().contains(embryoCode);
                boolean bHasHistory = b.getHistoryEmbryos().contains(embryoCode);
                if (aHasHistory && !bHasHistory) {
                    return -1;
                }
                if (!aHasHistory && bHasHistory) {
                    return 1;
                }

                // 优先级4：剩余种类容量大的优先（保留灵活性）
                int aRemainingTypes = a.getMaxTypes() - a.getCurrentTypes();
                int bRemainingTypes = b.getMaxTypes() - b.getCurrentTypes();
                int remainingCompare = Integer.compare(bRemainingTypes, aRemainingTypes);
                if (remainingCompare != 0) {
                    return remainingCompare;
                }

                // 优先级5：负荷少的优先
                int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
                if (loadCompare != 0) {
                    return loadCompare;
                }

                // 优先级6：种类少的优先
                return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
            });
        }
    }

    /**
     * 深拷贝各机台 assignedEmbryos 列表（保存 DFS 最优解快照，按机台索引存储避免同 embryoCode 覆盖）。
     */
    private List<List<EmbryoAssignment>> copyAssignments(List<MachineState> machineStates) {
        List<List<EmbryoAssignment>> copy = new ArrayList<>();
        for (MachineState state : machineStates) {
            copy.add(new ArrayList<>(state.getAssignedEmbryos()));
        }
        return copy;
    }

    /**
     * 提取机台编码列表
     */
    private List<String> copyMachineCodes(List<MachineState> machineStates) {
        List<String> codes = new ArrayList<>();
        for (MachineState state : machineStates) {
            codes.add(state.getMachineCode());
        }
        return codes;
    }

    /**
     * 判断当前机台负荷/种类分布是否满足均衡阈值（SYS04070001/002）。
     *
     * @return true 当 maxLoad−minLoad ≤ loadDiffThreshold 且 maxTypes−minTypes ≤ typeDiffThreshold
     */
    private boolean isBalanced(List<MachineState> machineStates, int typeDiffThreshold, int loadDiffThreshold) {
        if (machineStates.isEmpty()) {
            return true;
        }
        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;
        for (MachineState state : machineStates) {
            maxLoad = Math.max(maxLoad, state.getCurrentLoad());
            minLoad = Math.min(minLoad, state.getCurrentLoad());
            maxTypes = Math.max(maxTypes, state.getCurrentTypes());
            minTypes = Math.min(minTypes, state.getCurrentTypes());
        }
        int loadGap = maxLoad - minLoad;
        int typeGap = maxTypes - minTypes;
        return loadGap <= loadDiffThreshold && typeGap <= typeDiffThreshold;
    }

    /**
     * 均衡分数 — 仅在同等完整度/均衡等级内比较优劣。
     *
     * <p>公式：{@code loadGap × 10 + typeGap × 100}，越小越优。
     */
    private int calculateBalancingScore(List<MachineState> machineStates) {
        if (machineStates.isEmpty()) {
            return 0;
        }

        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;

        for (MachineState state : machineStates) {
            maxLoad = Math.max(maxLoad, state.getCurrentLoad());
            minLoad = Math.min(minLoad, state.getCurrentLoad());
            maxTypes = Math.max(maxTypes, state.getCurrentTypes());
            minTypes = Math.min(minTypes, state.getCurrentTypes());
        }

        int loadGap = maxLoad - minLoad;
        int typeGap = maxTypes - minTypes;

        return loadGap * 10 + typeGap * 100;
    }

    /**
     * 将 DFS 最优解（按机台索引的 EmbryoAssignment 列表）转换为 {@link BalancingResult}。
     */
    private BalancingResult convertDfsResultToBalancingResult(
            List<List<EmbryoAssignment>> assignments,
            List<String> machineCodes,
            List<MachineState> machineStates,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {

        BalancingResult result = new BalancingResult();
        result.setAssignments(new ArrayList<>());

        for (int i = 0; i < assignments.size(); i++) {
            List<EmbryoAssignment> embryoAssignments = assignments.get(i);
            if (embryoAssignments.isEmpty()) {
                continue;
            }

            MachineAssignment assignment = new MachineAssignment();
            assignment.setMachineCode(machineCodes.get(i));
            assignment.setEmbryoAssignments(embryoAssignments);
            result.getAssignments().add(assignment);
        }

        return result;
    }

    /**
     * 从 MachineState 直接构建结果 — DFS 无剩余任务但存在保底/续作预扣时使用。
     */
    private BalancingResult buildResultFromMachineStates(List<MachineState> machineStates) {
        BalancingResult result = new BalancingResult();
        result.setAssignments(new ArrayList<>());

        for (MachineState state : machineStates) {
            if (state.getAssignedEmbryos().isEmpty()) {
                continue;
            }
            MachineAssignment assignment = new MachineAssignment();
            assignment.setMachineCode(state.getMachineCode());
            assignment.setEmbryoAssignments(new ArrayList<>(state.getAssignedEmbryos()));
            result.getAssignments().add(assignment);
        }
        return result;
    }

    /**
     * 5.3.3.2.5.7 子步骤 — 输出分配明细、均衡指标（负荷差/种类差）、未排上胎胚告警。
     */
    private void logAllocationResult(BalancingResult result, List<MachineState> machineStates,
                                     List<CoreScheduleAlgorithmService.DailyEmbryoTask> originalTasks) {
        log.info("【均衡分配结果】");

        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;

        // 统计已分配的胎胚数量
        Map<String, Integer> assignedQtyMap = new LinkedHashMap<>();

        // 构建 machineStates 的预扣映射（机台编码 → 预扣信息）
        Map<String, MachineState> stateMap = new LinkedHashMap<>();
        for (MachineState state : machineStates) {
            stateMap.put(state.getMachineCode(), state);
        }

        for (MachineAssignment assignment : result.getAssignments()) {
            String machineCode = assignment.getMachineCode();

            // 合并相同胚子代码的条目（DFS分配的）
            Map<String, Integer> embryoQtyMap = new LinkedHashMap<>();
            for (EmbryoAssignment e : assignment.getEmbryoAssignments()) {
                embryoQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
                assignedQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
            }

            // 加入续作预扣信息
            MachineState state = stateMap.get(machineCode);
            int preLoad = 0;
            if (state != null && state.getCurrentLoad() > 0) {
                // 预扣的种类可能和DFS分配的种类有重叠，需要区分
                Set<String> dfsEmbryos = new HashSet<>(embryoQtyMap.keySet());
                for (EmbryoAssignment e : state.getAssignedEmbryos()) {
                    if (!dfsEmbryos.contains(e.getEmbryoCode())) {
                        embryoQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
                    }
                    assignedQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
                    preLoad += e.getAssignedQty();
                }
            }

            List<String> embryos = embryoQtyMap.entrySet().stream()
                    .map(e -> e.getKey() + "(" + e.getValue() + ")")
                    .collect(Collectors.toList());

            if (preLoad > 0) {
                log.info("    分配->机台{}: {} (含续作预留{})", machineCode, embryos, preLoad);
            } else {
                log.info("    分配->机台{}: {}", machineCode, embryos);
            }

            // 从 result 中计算均衡指标
            int load = assignment.getEmbryoAssignments().stream()
                    .mapToInt(EmbryoAssignment::getAssignedQty).sum();
            int types = (int) assignment.getEmbryoAssignments().stream()
                    .map(EmbryoAssignment::getEmbryoCode).distinct().count();

            maxLoad = Math.max(maxLoad, load);
            minLoad = Math.min(minLoad, load);
            maxTypes = Math.max(maxTypes, types);
            minTypes = Math.min(minTypes, types);
        }

        // 也打印 machineStates 中有预扣但没有 DFS 分配的机台
        for (MachineState state : machineStates) {
            if (!state.getAssignedEmbryos().isEmpty() &&
                    result.getAssignments().stream().noneMatch(a -> a.getMachineCode().equals(state.getMachineCode()))) {
                Map<String, Integer> preEmbryoMap = new LinkedHashMap<>();
                for (EmbryoAssignment e : state.getAssignedEmbryos()) {
                    preEmbryoMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
                    assignedQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
                }
                List<String> embryos = preEmbryoMap.entrySet().stream()
                        .map(e -> e.getKey() + "(" + e.getValue() + ")")
                        .collect(Collectors.toList());
                log.info("    续作->机台{}: {} (仅续作预留)", state.getMachineCode(), embryos);
            }
        }

        // 如果没有分配结果，避免打印错误指标
        if (minLoad == Integer.MAX_VALUE) {
            log.info("均衡指标：无有效分配");
        } else {
            log.info("【均衡指标】机台负荷差距={}台, 胎胚种类差距={}种",
                    maxLoad - minLoad, maxTypes - minTypes);
        }

        // 打印未排上的胎胚
        if (originalTasks != null && !originalTasks.isEmpty()) {
            Map<String, Integer> demandByEmbryo = new LinkedHashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask t : originalTasks) {
                demandByEmbryo.merge(t.getEmbryoCode(),
                        t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 1, Integer::sum);
            }
            List<String> unassignedItems = new ArrayList<>();
            for (Map.Entry<String, Integer> e : demandByEmbryo.entrySet()) {
                int assigned = assignedQtyMap.getOrDefault(e.getKey(), 0);
                if (assigned < e.getValue()) {
                    unassignedItems.add(e.getKey() + "(" + (e.getValue() - assigned) + ")");
                }
            }
            if (!unassignedItems.isEmpty()) {
                log.warn("【未排上】胎胚: {}", unassignedItems);
            }
        }
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

    /** 读取 SYS04070003 是否强制保留历史任务（Y/true=DFS 前保底预留）。 */
    public boolean getForceKeepHistoryConfig(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                return "Y".equalsIgnoreCase(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false;
    }


    // ==================== 数据结构 ====================

    /**
     * DFS 搜索过程中的单机台快照。
     *
     * <ul>
     *   <li>{@code maxCapacity} — 最大可分配硫化机台数</li>
     *   <li>{@code maxTypes} — 最大胎胚种类数</li>
     *   <li>{@code currentLoad} / {@code currentTypes} — 已占用（含续作预扣+保底预留+DFS 分配）</li>
     *   <li>{@code assignedEmbryos} — 分配明细（embryoCode + assignedQty + 源 task）</li>
     *   <li>{@code historyEmbryos} — 在产/历史胎胚，影响 sortCandidatesForDfs 优先级</li>
     * </ul>
     */
    @lombok.Data
    public static class MachineState {
        private String machineCode;
        private int maxCapacity;
        private int maxTypes;
        private int currentLoad;
        private int currentTypes;
        private List<EmbryoAssignment> assignedEmbryos;
        private Set<String> historyEmbryos;
        /** 已分配的硫化机台号集合（按 lhMachineCode 去重负荷） */
        private Set<String> assignedLhMachineCodes;
    }

    /**
     * DFS 搜索过程的全局最优解记录（单次 balance 调用内共享，由 dfsAssign 读写）。
     */
    @lombok.Data
    private static class DfsSearchResult {
        /** 最优解均衡分数（越小越优，仅同完整度/均衡等级内比较） */
        int bestScore;
        /** 最优解已分配硫化机台数（完整度第一优先） */
        int bestAssignedCount;
        /** 最优解是否满足 isBalanced 阈值 */
        boolean bestIsBalanced;
        /** 最优解各机台分配明细（按机台索引，与 bestMachineCodes 对齐） */
        List<List<EmbryoAssignment>> bestAssignments;
        /** 与 bestAssignments 对应的机台编码列表 */
        List<String> bestMachineCodes;
        int searchCount;
        int pruneCount;
        int callCount;
        /** DFS 新分配的 assignedQty 之和（不含续作预扣/保底预留），替代 currentLoad 计算 totalAssigned */
        int dfsAssignedQty;
        /**
         * 当前轮次配置 — DFS 内部通过此字段读取轮次策略（容量管控/历史偏好等），
         * 避免在 dfsAssign 递归签名中增加参数，6处递归调用无需改动。
         */
        BalancingRoundConfig config;
    }

    /** 均衡分配对外返回结构：机台 → 多条 EmbryoAssignment。 */
    @lombok.Data
    public static class BalancingResult {
        private List<MachineAssignment> assignments;
    }

    /** 单机台的分配汇总。 */
    @lombok.Data
    public static class MachineAssignment {
        private String machineCode;
        private List<EmbryoAssignment> embryoAssignments;
    }

    /**
     * 单条分配记录：胎胚编码 + 源任务 + 分配到该机台的硫化机台数。
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class EmbryoAssignment {
        private String embryoCode;
        private CoreScheduleAlgorithmService.DailyEmbryoTask task;
        /** 分配到该机台的硫化机台数（非条数） */
        private int assignedQty;
    }

    /**
     * 均衡轮次配置 — 封装不同轮次之间的行为差异，实现配置驱动的策略分支。
     *
     * <p><b>设计目标</b>：通过配置对象控制 DFS 及辅助方法的行为分支，
     * 避免在 DFS 递归签名中堆积布尔参数，同时支持未来扩展更多轮次。
     *
     * <p><b>轮次差异</b>：
     * <table>
     *   <tr><th>配置项</th><th>第一轮（严格）</th><th>第二轮（宽松）</th></tr>
     *   <tr><td>enforceCapacityLimit</td><td>true（强制maxCapacity管控）</td><td>false（解除容量上限）</td></tr>
     *   <tr><td>enforceReservedHistory</td><td>=forceKeepHistory（按参数）</td><td>false（不强制保底预留）</td></tr>
     *   <tr><td>boostHistoryPreference</td><td>false（历史偏好为排序④级）</td><td>true（启用第二轮排序策略：历史优先+同胎胚集中+均衡例外）</td></tr>
     *   <tr><td>capacitySufficientOverride</td><td>null（按实际供需计算）</td><td>Boolean.TRUE（强制产能充足策略）</td></tr>
     *   <tr><td>enforceContinuePreload</td><td>true（续作预扣占用种类槽+负荷）</td><td>false（不预扣，释放种类槽供新胎胚使用）</td></tr>
     * </table>
     */
    @lombok.Data
    public static class BalancingRoundConfig {
        /** 轮次名称（日志标识，如"第一轮-严格均衡"/"第二轮-宽松均衡"） */
        private String roundName;
        /**
         * 是否强制容量上限管控（maxCapacity）。
         * <p>第一轮=true：机台 currentLoad 超过 maxCapacity 时拒绝分配；
         * 第二轮=false：解除容量限制，允许超载分配，确保所有任务都能上机。
         */
        private boolean enforceCapacityLimit;
        /**
         * 是否执行保底预留（reservedHistoryTasks，SYS04070003=Y 时的 DFS 前预留）。
         * <p>第一轮=forceKeepHistory 参数值；第二轮=false：不强制预留。
         */
        private boolean enforceReservedHistory;
        /**
         * 是否启用第二轮排序策略（历史胎胚优先 + 同胎胚任务集中 + 均衡性优先例外）。
         * <p>第一轮=false：历史偏好为排序第④优先级；
         * 第二轮=true：启用三项排序原则——
         * <ul>
         *   <li>① 历史胎胚优先：历史机台绝对优先，保障生产连贯性</li>
         *   <li>② 同胎胚任务集中：已有该胎胚的机台优先集中，提高处理效率</li>
         *   <li>③ 均衡性优先例外：当集中会导致负荷差超阈值时，不集中而按均衡拆分</li>
         * </ul>
         */
        private boolean boostHistoryPreference;
        /**
         * 产能充足标志覆盖值（null=按实际供需计算，非null=强制使用该值）。
         * <p>第二轮强制为 true（容量无限），使用均衡优先的排序策略。
         */
        private Boolean capacitySufficientOverride;
        /**
         * 是否执行续作预扣（continueLoadMap/continueTypeMap/continueLhMachineCodeMap）。
         * <p>第一轮=true：机台初始化时预扣续作已占的容量和种类槽，反映实际机台占用状态；
         * 第二轮=false：不预扣，释放种类槽供新胎胚使用，避免 maxTypes 约束阻止分配。
         * <p><b>原因</b>：续作预扣设置的 currentTypes 在第二轮仍受 maxTypes 约束，
         * 若机台续作已占满 maxTypes 个种类槽，即使解除容量上限，新胎胚仍无法上机，
         * 违背"确保所有任务都能被成功分配"的核心目标。
         */
        private boolean enforceContinuePreload;
    }

    /**
     * 单轮均衡执行结果 — 包装 BalancingResult 与完整度信息，供两轮编排决策。
     */
    @lombok.Data
    public static class RoundExecutionResult {
        /** 均衡分配结果 */
        private BalancingResult result;
        /** DFS 实际分配的硫化机台数（不含续作预扣/保底预留） */
        private int dfsAssignedCount;
        /** 已满足的预占负荷（仅保底预留部分；续作预扣不代表已满足需求，不计入） */
        private int preOccupiedLoad;
        /** 原始总需求（所有任务 vulcanizeMachineCount 快照之和） */
        private int totalOriginalDemand;
        /** DFS 搜索次数 */
        private int searchCount;
        /** DFS 剪枝次数 */
        private int pruneCount;

        /**
         * 判断本轮是否获得完整解：保底预留已满足负荷 + DFS分配数 >= 原始总需求。
         * <p>完整解 = 所有任务的硫化机台数都已分配到机台（含保底预留/DFS分配）。
         * <p>注意：续作预扣（continuePreloadLoad）不计入已满足负荷，因为续作任务的
         * vulcanizeMachineCount 已在 ContinueTaskProcessor 中扣减保底预留量，
         * 传入 BalancingService 的 totalDemand 已是扣减后的剩余需求。
         */
        public boolean isComplete() {
            return (preOccupiedLoad + dfsAssignedCount) >= totalOriginalDemand;
        }
    }
}
