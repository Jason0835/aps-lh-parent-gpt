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
 * S5.3.1 续作任务处理器 — 在「历史在产胎胚」与「本班次均衡分配」之间做保底预留或纯标记。
 *
 * <h3>流水线位置</h3>
 * <pre>
 * CoreScheduleAlgorithmServiceImpl.executeShiftSchedule
 *   → 5.2  TaskGroupService.groupTasks          分出 continueTasks（已含计划量/收尾/硫化机台数）
 *   → 5.3.1 ContinueTaskProcessor（本类）
 *   → 5.3.2 TrialTaskProcessor
 *   → 5.3.3 NewTaskProcessor                    消费 continueTasks 剩余 demand + continueAllocations 预扣
 *   → 5.3.4 合并 continue + new + trial 分配
 *   → 5.3.7 ShiftScheduleService 班次精排
 * </pre>
 *
 * <h3>续作任务来源（上游判定，本类不重复识别）</h3>
 * <ul>
 *   <li>{@link TaskGroupService} 根据 {@code context.machineOnlineEmbryoMap}（胎胚→在产机台）与当日硫化需求，
 *       将「机台当前在产且本班仍有排产需求」的任务归入 {@code continueTasks}。</li>
 *   <li>输入任务已走完 R1 计划量、收尾属性、立库封顶；{@code vulcanizeMachineCount} 表示本结构待均衡的
 *       <b>硫化机台数</b>，{@code endingExtraInventory} 表示精排用的<b>条数</b>。</li>
 * </ul>
 *
 * <h3>与 NewTaskProcessor / BalancingService 的分工</h3>
 * <table>
 *   <tr><th>SYS04070003</th><th>本类行为</th><th>下游行为</th></tr>
 *   <tr>
 *     <td>N（默认）</td>
 *     <td>仅 {@code isContinueTask=true}，返回空列表</td>
 *     <td>续作与新增一并进入 DFS，由 {@link BalancingService} 统一均衡（无历史保底）</td>
 *   </tr>
 *   <tr>
 *     <td>Y</td>
 *     <td>每台历史机台、每个在产胎胚各<b>保底预留 1 台硫化机</b>，写入 {@code continueAllocations}</td>
 *     <td>{@link NewTaskProcessor} 从 {@code continueAllocations} 构建 {@code continueLoadMap} /
 *         {@code continueTypeMap}，DFS 在<b>扣减预扣后</b>的容量上分配剩余机台数；
 *         {@code forceKeepHistory=false} 传入 BalancingService，避免 DFS 内二次保底</td>
 *   </tr>
 * </table>
 *
 * <h3>单位约定</h3>
 * <ul>
 *   <li><b>保底预留粒度</b>：1 台硫化机（{@code reservedVulcanizeCount=1}），不是 1 车或 1 条胎胚。</li>
 *   <li><b>机台容量占用</b>：{@link CoreScheduleAlgorithmService.MachineAllocationResult#setUsedCapacity}
 *       累加的是硫化机台数；条数写在 {@link CoreScheduleAlgorithmService.TaskAllocation#setQuantity}。</li>
 *   <li><b>任务 demand 扣减</b>：每成功预留 1 台，对应 {@code DailyEmbryoTask.vulcanizeMachineCount -= 1}，
 *       剩余 demand 由 NewTaskProcessor 并入同结构任务列表继续 DFS。</li>
 * </ul>
 *
 * <h3>主流程（{@link #processContinueTasks}）</h3>
 * <ol>
 *   <li>5.3.1.1 — 全量标记 {@code isContinueTask=true}</li>
 *   <li>5.3.1.2 — 读取 SYS04070003；N 则提前返回空分配</li>
 *   <li>5.3.1.3 — 反转 {@code machineOnlineEmbryoMap} → {@code machineHistoryMap}（机台→在产胎胚集合）</li>
 *   <li>5.3.1.4 — 双重循环（机台 × 历史胎胚）：匹配续作任务 → 版本校验 → 预留 1 台 → 写入 {@code allocationMap}</li>
 * </ol>
 *
 * <h3>参数</h3>
 * <table>
 *   <tr><th>编码</th><th>含义</th><th>默认</th></tr>
 *   <tr><td>SYS04070003</td><td>强制保留历史任务（Y=本类保底预留）</td><td>N</td></tr>
 * </table>
 *
 * @author APS Team
 * @see NewTaskProcessor#processNewTasks
 * @see BalancingService#balanceEmbryosToMachinesWithMachineCapacity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContinueTaskProcessor {

    /** 配比缺失时单台默认最大硫化机数（与 NewTaskProcessor 兜底一致） */
    private static final int DEFAULT_MAX_LH_MACHINE_COUNT = 10;

    /** 机台主数据未配置日产能时的默认值（条/天，仅用于 {@link #getMachineDailyCapacity}） */
    private static final int DEFAULT_DAILY_CAPACITY = 1200;

    /** 参数编码：强制保留历史任务 — Y/true 启用保底预留，N/false 仅标记续作 */
    private static final String PARAM_FORCE_KEEP_HISTORY = "SYS04070003";

    // ==================== 5.3.1 续作处理入口 ====================

    /**
     * 处理本班次全部续作任务。
     *
     * <p><b>输出语义</b>：返回的 {@code MachineAllocationResult} 列表仅包含「已发生保底预留」的机台；
     * SYS04070003=N 时恒为空列表，但 {@code continueTasks} 内任务仍带 {@code isContinueTask=true} 供下游识别。
     *
     * <p><b>匹配与跳过规则</b>（5.3.1.4 内层循环）：
     * <ul>
     *   <li>历史胎胚在 {@code continueTasks} 中找不到 → 跳过（debug）</li>
     *   <li>找到但 {@code plannedProduction≤0}（收尾舍弃等）→ 跳过（info，视为已放弃）</li>
     *   <li>找到且 {@code demand>0 && plannedProduction>0} → 候选匹配任务</li>
     *   <li>机台不在该结构当日（或提前生产）可用列表 → 跳过（PRODUCTION_VERSION/排产配置不匹配）</li>
     * </ul>
     *
     * @param continueTasks  TaskGroupService 输出的续作任务（本方法会修改其中任务的 vulcanizeMachineCount）
     * @param context        排程上下文（含 machineOnlineEmbryoMap、structureAllocationMap 等）
     * @param scheduleDate   当前排程日（用于结构机台日期/年月过滤）
     * @param dayShifts      当天班次配置（签名保留，当前逻辑未使用）
     * @param day            排程天序号（签名保留，当前逻辑未使用）
     * @return 保底预留产生的机台分配；无预留时为空列表
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processContinueTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        if (CollectionUtils.isEmpty(continueTasks)) {
            return results;
        }

        log.info("========== 开始处理续作任务，共 {} 个任务 ==========", continueTasks.size());

        // --- 5.3.1.1 续作身份标记（无论是否保底，下游精排日志与 TaskAllocation 均依赖此标志）---
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
            task.setIsContinueTask(true);
        }

        // --- 5.3.1.2 读取 SYS04070003：N 则不做机台级预留，续作剩余 demand 全部交给 NewTaskProcessor DFS ---
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);
        log.info("强制保留历史任务配置: {}", forceKeepHistory);

        if (!forceKeepHistory) {
            log.info("强制保留历史任务未开启，续作任务不保底预留，全部交给新增均衡处理");
            log.info("========== 续作任务处理完成（仅标记），共 {} 个任务 ==========", continueTasks.size());
            return results;
        }

        // --- 5.3.1.3 机台→在产胎胚：由「胎胚→机台」在线映射反转，驱动「在哪台机继续产」---
        Map<String, Set<String>> machineHistoryMap = buildMachineHistoryMap(context);
        log.info("构建历史任务映射完成，共 {} 台机台有历史记录", machineHistoryMap.size());

        // --- 5.3.1.4 保底预留：外层按机台、内层按该机台历史胎胚，每台每胚最多预留 1 硫化机 ---
        Map<String, CoreScheduleAlgorithmService.MachineAllocationResult> allocationMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> historyEntry : machineHistoryMap.entrySet()) {
            String machineCode = historyEntry.getKey();
            Set<String> historyEmbryos = historyEntry.getValue();

            for (String embryoCode : historyEmbryos) {

                // 5.3.1.4.1 收集该机台该胎胚的所有匹配任务（有正 demand 与正计划量）
                List<CoreScheduleAlgorithmService.DailyEmbryoTask> matchedTasks = new ArrayList<>();
                boolean foundAbandoned = false;
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
                    if (embryoCode.equals(task.getEmbryoCode())) {
                        int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                        Integer plannedProd = task.getPlannedProduction();
                        if (demand > 0 && (plannedProd != null && plannedProd > 0)) {
                            matchedTasks.add(task);
                        }
                        if (plannedProd != null && plannedProd <= 0) {
                            foundAbandoned = true;
                        }
                    }
                }

                // 5.3.1.4.2 无匹配：区分「计划量已舍弃」与「无剩余需求」
                if (matchedTasks.isEmpty()) {
                    if (foundAbandoned) {
                        log.info("机台 {} 的历史胎胚 {} 计划产量为0（已舍弃），跳过保底预留", machineCode, embryoCode);
                    } else {
                        log.debug("机台 {} 的历史胎胚 {} 在续作任务中无剩余需求，跳过保底预留", machineCode, embryoCode);
                    }
                    continue;
                }

                // 5.3.1.4.3 取第一个任务检查机台可用性（同结构的任务机台配置一致）
                CoreScheduleAlgorithmService.DailyEmbryoTask firstTask = matchedTasks.get(0);
                List<MpCxCapacityConfiguration> availableForTask = getAvailableMachinesForStructure(
                        firstTask.getStructureName(), scheduleDate, context, firstTask.getProductionVersion());
                boolean machineAvailable = availableForTask.stream()
                        .anyMatch(c -> machineCode.equals(c.getCxMachineCode()));
                if (!machineAvailable) {
                    log.info("机台 {} 不支持结构 {} 的生产版本 {}，跳过保底预留胎胚 {}",
                            machineCode, firstTask.getStructureName(), firstTask.getProductionVersion(), embryoCode);
                    continue;
                }

                // 5.3.1.4.4 动态保底预留：以前序班次该机台该胎胚的硫化机台数作为预留目标，无前序数据时兜底1
                int prevCount = getDynamicReservedCount(context, machineCode, embryoCode);
                int remainingToReserve = prevCount;
                int totalReservedThisEmbryo = 0;

                for (CoreScheduleAlgorithmService.DailyEmbryoTask matchedTask : matchedTasks) {
                    if (remainingToReserve <= 0) break;

                    int demand = matchedTask.getVulcanizeMachineCount() != null ? matchedTask.getVulcanizeMachineCount() : 0;
                    int reservedForThisTask = Math.min(demand, remainingToReserve);
                    matchedTask.setVulcanizeMachineCount(demand - reservedForThisTask);

                    CoreScheduleAlgorithmService.MachineAllocationResult allocation =
                            allocationMap.computeIfAbsent(machineCode, code -> createMachineAllocation(code, context));
                    allocateContinueReservation(allocation, matchedTask, reservedForThisTask, context);

                    remainingToReserve -= reservedForThisTask;
                    totalReservedThisEmbryo += reservedForThisTask;
                }

                log.info("机台 {} 保底预留胎胚 {} 共 {} 个硫化机（目标={}, 已满足={}）",
                        machineCode, embryoCode, totalReservedThisEmbryo, prevCount, totalReservedThisEmbryo);
            }
        }

        results.addAll(allocationMap.values());
        log.info("========== 续作任务保底预留完成，共 {} 台机台预留任务 ==========", results.size());
        return results;
    }

    /**
     * 将单次保底预留写入机台分配结果。
     *
     * <p><b>条数 vs 机台数</b>：
     * <ul>
     *   <li>{@code TaskAllocation.quantity} — 胎胚条数，优先 {@code endingExtraInventory}（收尾后实际待产量），
     *       否则 {@code demandQuantity}；供 ShiftScheduleService 精排。</li>
     *   <li>{@code TaskAllocation.vulcanizeMachineCount} - 本次预留的硫化机台数（动态：前序班次台数或兜底1）。</li>
     *   <li>{@code allocation.usedCapacity / remainingCapacity} — 按硫化机台数增减，<b>不得</b>使用 quantity。</li>
     * </ul>
     *
     * <p>收尾/开停产/首任务/紧急收尾等标志原样透传，保证精排与日志与 TaskGroupService 一致。
     */
    private void allocateContinueReservation(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            int reservedVulcanizeCount,
            ScheduleContextVo context) {

        int quantity = task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0
                ? task.getEndingExtraInventory() : task.getDemandQuantity();

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation = new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setEmbryoCode(task.getEmbryoCode());
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialDesc(task.getMaterialDesc());
        taskAllocation.setMainMaterialDesc(task.getMainMaterialDesc());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(quantity);
        taskAllocation.setVulcanizeMachineCount(reservedVulcanizeCount);
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsProductionTrial(task.getIsProductionTrial());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setEndingExtraInventory(task.getEndingExtraInventory());
        taskAllocation.setIsLastEndingBatch(task.getIsLastEndingBatch());
        taskAllocation.setIsEndProduction(task.getIsEndProduction());
        taskAllocation.setEndingAbandoned(task.getEndingAbandoned());
        taskAllocation.setIsOpeningDayTask(task.getIsOpeningDayTask());
        taskAllocation.setIsClosingDayTask(task.getIsClosingDayTask());
        taskAllocation.setIsMainProduct(task.getIsMainProduct());
        taskAllocation.setIsContinueTask(true);
        taskAllocation.setLhId(task.getLhId());
        taskAllocation.setLhMachineCode(task.getLhMachineCode());
        taskAllocation.setConstructionStage(task.getConstructionStage());
        taskAllocation.setIsFirstTask(task.getIsFirstTask());
        taskAllocation.setIsUrgentEnding(task.getIsUrgentEnding());
        taskAllocation.setIsNearEnding(task.getIsNearEnding());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + reservedVulcanizeCount);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - reservedVulcanizeCount);
    }

    // ==================== 参数与历史映射 ====================

    /**
     * 获取动态保底预留目标值：以前序班次该机台该胎胚的硫化机台数作为预留目标。
     *
     * <p>数据来源：{@code context.previousShiftMachineEmbryoLoadMap}（机台 -> 胎胚 -> 硫化机台数）。
     * 第一个班次从 T_CX_SHIFT_MACHINE_LOAD 加载前日最后班次数据，后续班次由
     * CoreScheduleAlgorithmServiceImpl 用前一个班次的分配结果更新。
     *
     * <p>调1用方负责跨多个任务累计预留（每天任务的 {@code vulcanizeMachineCount} 为1），
     * 直至达到本方法返回的目标值或所有任务耗尽。
     *
     * <p>兜底策略：无前序班次数据或查不到对应记录时，返回1（与原固定预留逻辑一致）。
     *
     * @param context     排程上下文
     * @param machineCode 成型机台编码
     * @param embryoCode  胎胚编码
     * @return 预留目标值，默认1
     */
    private int getDynamicReservedCount(ScheduleContextVo context,
                                        String machineCode, String embryoCode) {
        Map<String, Map<String, Integer>> prevLoadMap = context.getPreviousShiftMachineEmbryoLoadMap();
        if (prevLoadMap != null) {
            Map<String, Integer> embryoLoadMap = prevLoadMap.get(machineCode);
            if (embryoLoadMap != null) {
                Integer prevCount = embryoLoadMap.get(embryoCode);
                if (prevCount != null && prevCount > 0) {
                    log.info("动态保底预留目标: 机台={}, 胎胚={}, 前序班次台数={}", machineCode, embryoCode, prevCount);
                    return prevCount;
                }
            }
        }
        log.info("动态保底预留(兜底): 机台={}, 胎胚={}, 无前序数据, 目标=1", machineCode, embryoCode);
        return 1;
    }

    /**
     * 解析 SYS04070003「强制保留历史任务」。
     *
     * <p>取值 Y/true（忽略大小写）为开启；缺省或空值视为 N（与参数表默认一致）。
     */
    private boolean getForceKeepHistoryConfig(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                boolean result = "Y".equalsIgnoreCase(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
                log.info("SYS04070003 数据库配置值: {}, 解析结果: {}", config.getParamValue(), result);
                return result;
            }
        }
        return false;
    }

    /**
     * 构建「机台编码 → 该机台当前在产胎胚集合」映射。
     *
     * <p>数据来源：{@code context.machineOnlineEmbryoMap} 为胎胚→机台集合，此处反转为机台→胎胚，
     * 用于 5.3.1.4「在历史机台上为在产胎胚做保底」。
     */
    private Map<String, Set<String>> buildMachineHistoryMap(ScheduleContextVo context) {
        Map<String, Set<String>> historyMap = new HashMap<>();
        Map<String, Set<String>> onlineEmbryoMap = context.getMachineOnlineEmbryoMap();
        if (onlineEmbryoMap != null) {
            for (Map.Entry<String, Set<String>> entry : onlineEmbryoMap.entrySet()) {
                String embryoCode = entry.getKey();
                for (String cxCode : entry.getValue()) {
                    historyMap.computeIfAbsent(cxCode, k -> new HashSet<>()).add(embryoCode);
                }
            }
        }
        return historyMap;
    }

    // ==================== 结构可用机台解析（与 NewTaskProcessor 同构） ====================

    /**
     * 获取某结构在排程日可用的成型机台配置列表。
     *
     * <p><b>优先级</b>：
     * <ol>
     *   <li>{@code structureAllocationMap} 中按年月 + 当月日区间（beginDay~endDay）过滤，机台编码去重</li>
     *   <li>当日无配置时回退 {@link #getAdvanceProductionMachines}（TaskGroupService 已写入
     *       {@code advanceProductionMachineMap} 并完成版本/冲突处理）</li>
     * </ol>
     *
     * <p>保底预留前必须确认目标机台出现在此列表中，避免给不可排产机台预留负荷。
     */
    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextVo context,
            String productionVersion) {
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs =
                    context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                int day = scheduleDate.getDayOfMonth();
                int dateYear = scheduleDate.getYear();
                int dateMonth = scheduleDate.getMonthValue();
                List<MpCxCapacityConfiguration> result = configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= day && c.getEndDay() >= day)
                        .filter(c -> c.getYear() != null && c.getYear() == dateYear
                                && c.getMonth() != null && c.getMonth() == dateMonth)
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
     * <p>{@code advanceProductionMachineMap} 在 TaskGroupService 按结构解析未来排产配置时已做：
     * 版本过滤、机台占用三态判定、跨结构冲突剔除。此处直接取用，<b>不再</b>按 productionVersion 二次过滤，
     * 避免跨月机台因版本字段与当月不一致被误删。
     *
     * @param productionVersion 保留参数以与 NewTaskProcessor 签名一致，本方法内未使用
     */
    private List<MpCxCapacityConfiguration> getAdvanceProductionMachines(
            String structureName, ScheduleContextVo context, String productionVersion) {
        if (context.getAdvanceProductionMachineMap() != null) {
            List<MpCxCapacityConfiguration> advanceMachines =
                    context.getAdvanceProductionMachineMap().get(structureName);
            if (advanceMachines != null && !advanceMachines.isEmpty()) {
                log.info("【提前生产】ContinueTaskProcessor 结构={} 使用提前生产机台={}",
                        structureName,
                        advanceMachines.stream()
                                .map(MpCxCapacityConfiguration::getCxMachineCode)
                                .collect(Collectors.toList()));
                return new ArrayList<>(advanceMachines);
            }
        }
        return new ArrayList<>();
    }

    // ==================== 机台分配壳对象 ====================

    /**
     * 创建空的机台分配结果，{@code remainingCapacity} 取自机台主数据日产能（条/天语义，与 Processor 层约定一致）。
     */
    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextVo context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    /** 从 {@code context.availableMachines} 读取机台 {@code maxDayCapacity}，缺失时用 {@link #DEFAULT_DAILY_CAPACITY}。 */
    private int getMachineDailyCapacity(String machineCode, ScheduleContextVo context) {
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                if (machine.getCxMachineCode().equals(machineCode)) {
                    return machine.getMaxDayCapacity() != null ? machine.getMaxDayCapacity() : DEFAULT_DAILY_CAPACITY;
                }
            }
        }
        return DEFAULT_DAILY_CAPACITY;
    }
}
