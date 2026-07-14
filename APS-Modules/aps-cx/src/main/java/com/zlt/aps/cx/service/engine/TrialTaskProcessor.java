package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.DailyEmbryoTask;
import com.zlt.aps.cx.vo.MachineAllocationResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.TaskAllocation;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * S5.3.2 试制任务处理器 — 为试制/量试任务直接选定成型机台，<b>不</b>经过 {@link BalancingService} DFS 均衡。
 *
 * <h3>流水线位置</h3>
 * <pre>
 * CoreScheduleAlgorithmServiceImpl.executeShiftSchedule
 *   → 5.2  TaskGroupService.groupTasks          trialTasks（constructionStage=01，约束已在分组阶段校验）
 *   → 5.3.1 ContinueTaskProcessor
 *   → 5.3.2 TrialTaskProcessor（本类）         产出 trialAllocations
 *   → 5.3.3 NewTaskProcessor                    从 trialAllocations 构建量试机台约束
 *   → 5.3.4 合并分配 → ShiftScheduleService     试制：早/中班、双数、不补整车
 * </pre>
 *
 * <h3>与 TaskGroupService / NewTaskProcessor 的分工</h3>
 * <table>
 *   <tr><th>环节</th><th>负责模块</th><th>内容</th></tr>
 *   <tr><td>试制约束</td><td>TaskGroupService + 上下文参数</td><td>日 SKU 上限、周日是否允许、双数产量、早/中班等<b>在入队前</b>已处理</td></tr>
 *   <tr><td>机台选择</td><td>本类</td><td>空机台优先 → 负载最不均衡机台；校验机台固定禁用结构</td></tr>
 *   <tr><td>量试锁定</td><td>NewTaskProcessor</td><td>同胎胚已有试制分配 → 量试 {@code constrainedMachineCode} 锁定试制机台</td></tr>
 * </table>
 *
 * <h3>单位约定（与 DFS 路径不同）</h3>
 * <ul>
 *   <li>试制分配中 {@code usedCapacity} / {@code remainingCapacity} 累加的是<b>条数</b>
 *       （{@code plannedProduction}），用于本处理器内负载不均衡度计算，<b>不是</b>硫化机台数。</li>
 *   <li>{@code TaskAllocation.quantity} / {@code endingExtraInventory} 为条数；
 *       {@code vulcanizeMachineCount} 取自任务，缺省为 1。</li>
 *   <li>精排阶段 {@link ShiftScheduleService} 仍以 {@code endingExtraInventory} 为下量依据。</li>
 * </ul>
 *
 * <h3>主流程（{@link #processTrialTasks}）</h3>
 * <ol>
 *   <li>5.3.2.1 — 按结构分组，过滤 {@code endingExtraInventory≤0}</li>
 *   <li>5.3.2.2 — 跨结构共享 {@code machineAllocationMap}（负载统计全局一致）</li>
 *   <li>5.3.2.3 — 逐结构：候选机台 → 优先级排序 → {@link #allocateTrialTask}</li>
 * </ol>
 *
 * @author APS Team
 * @see NewTaskProcessor#processNewTasks
 * @see ShiftScheduleService#scheduleTaskToShifts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialTaskProcessor {

    // ==================== 5.3.2 试制处理入口 ====================

    /**
     * 处理本班次全部试制任务，输出试制机台分配列表。
     *
     * <p><b>输入</b>：{@code trialTasks} 来自 TaskGroupService（{@code isTrialTask=true} 或试制施工阶段）；
     * 无任务或过滤后为空则返回空列表。
     *
     * <p><b>输出</b>：{@code trialAllocations}，与 continue/new 分配在 5.3.4 合并；
     * NewTaskProcessor 据此构建「胎胚→试制机台」映射，约束后续量试 DFS。
     *
     * @param trialTasks        试制任务列表
     * @param context           排程上下文
     * @param scheduleDate      当前排程日
     * @param dayShifts         当天班次配置（签名保留，机台选择逻辑未使用）
     * @param availableMachines 可用机台列表（签名保留，实际从 structureAllocationMap 解析）
     * @return 试制任务机台分配结果
     */
    public List<MachineAllocationResult> processTrialTasks(
            List<DailyEmbryoTask> trialTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            List<MdmMoldingMachine> availableMachines) {

        List<MachineAllocationResult> results = new ArrayList<>();

        if (CollectionUtils.isEmpty(trialTasks)) {
            return results;
        }

        log.info("========== 开始处理试制任务，共 {} 个任务 ==========", trialTasks.size());

        // 5.3.2.1 按结构分组；无结构名或无待排条数的任务跳过
        Map<String, List<DailyEmbryoTask>> structureTaskMap =
                trialTasks.stream()
                        .filter(t -> t.getStructureName() != null)
                        .filter(t -> t.getEndingExtraInventory() != null && t.getEndingExtraInventory() > 0)
                        .collect(Collectors.groupingBy(
                                DailyEmbryoTask::getStructureName,
                                LinkedHashMap::new,
                                Collectors.toList()));

        // 5.3.2.2 机台 → 分配结果（跨结构累积，供 selectMachineForTrial 计算全局负载）
        Map<String, MachineAllocationResult> machineAllocationMap =
                new HashMap<>();

        // 5.3.2.3 逐结构处理
        for (Map.Entry<String, List<DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<DailyEmbryoTask> tasks = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个试制胎胚 ---", structureName, tasks.size());

            // 5.3.2.3.1 结构候选机台（当日排产配置或提前生产回退）
            String productionVersion = tasks.get(0).getProductionVersion();
            List<MpCxCapacityConfiguration> structMachines = context.getAvailableMachinesForStructure(
                    structureName, scheduleDate, productionVersion);
            if (structMachines.isEmpty()) {
                log.warn("结构 {} 没有可安排的机台，跳过", structureName);
                continue;
            }

            // 5.3.2.3.2 同结构内按 priority 升序（分值小的先占机台；null 视为最低优先级）
            tasks.sort((a, b) -> {
                Integer priA = a.getPriority();
                Integer priB = b.getPriority();
                return Integer.compare(
                        priA != null ? priA : Integer.MAX_VALUE,
                        priB != null ? priB : Integer.MAX_VALUE);
            });

            // 5.3.2.3.3 逐任务选机并写入 machineAllocationMap
            for (DailyEmbryoTask task : tasks) {
                allocateTrialTask(task, structMachines, machineAllocationMap, context);
            }
        }

        results.addAll(machineAllocationMap.values());
        log.info("========== 试制任务处理完成，共 {} 台机台分配任务 ==========", results.size());
        return results;
    }

    // ==================== 单任务分配 ====================

    /**
     * 将单个试制任务写入选定机台的 {@link MachineAllocationResult}。
     *
     * <p>选机委托 {@link #selectMachineForTrial}；失败时打 warn 并跳过（任务本班不落机台）。
     */
    private void allocateTrialTask(
            DailyEmbryoTask task,
            List<MpCxCapacityConfiguration> structMachines,
            Map<String, MachineAllocationResult> machineAllocationMap,
            ScheduleContextVo context) {

        String structureName = task.getStructureName();
        String embryoCode = task.getEmbryoCode();

        MdmMoldingMachine selectedMachine = selectMachineForTrial(
                embryoCode, structureName, structMachines, machineAllocationMap, context);
        if (selectedMachine == null) {
            log.warn("试制任务 {} 无法找到合适的机台，跳过", embryoCode);
            return;
        }

        String machineCode = selectedMachine.getCxMachineCode();

        MachineAllocationResult allocation =
                machineAllocationMap.computeIfAbsent(machineCode, k -> createMachineAllocation(k, context));

        allocateTaskToMachine(allocation, task);
        log.debug("试制任务 {} 分配到机台 {}，计划量={}", embryoCode, machineCode, task.getPlannedProduction());
    }

    /**
     * 试制选机策略（结构候选集内）。
     *
     * <p><b>过滤</b>：机台须在 {@code structMachines} 中且通过 {@link #checkStructureConstraint}（固定禁用结构）。
     *
     * <p><b>优先级</b>：
     * <ol>
     *   <li>空机台 — {@code machineAllocationMap} 中无记录或 taskAllocations 为空，取第一个空机台</li>
     *   <li>无空机台 — 选 {@link #calculateImbalance} 最大者（与当前非空机台平均负载偏差最大）</li>
     * </ol>
     *
     * @param embryoCode 胎胚编码
     */
    private MdmMoldingMachine selectMachineForTrial(
            String embryoCode,
            String structureName,
            List<MpCxCapacityConfiguration> structMachines,
            Map<String, MachineAllocationResult> machineAllocationMap,
            ScheduleContextVo context) {

        MdmMoldingMachine emptyMachine = null;
        MdmMoldingMachine bestImbalancedMachine = null;
        int maxImbalance = -1;

        for (MpCxCapacityConfiguration config : structMachines) {
            String machineCode = config.getCxMachineCode();
            MdmMoldingMachine machine = findMachine(machineCode, context.getAvailableMachines());
            if (machine == null) {
                continue;
            }

            if (!checkStructureConstraint(machine, structureName, context)) {
                continue;
            }

            MachineAllocationResult allocation = machineAllocationMap.get(machineCode);

            if (allocation == null || allocation.getTaskAllocations().isEmpty()) {
                if (emptyMachine == null) {
                    emptyMachine = machine;
                }
            } else {
                int imbalance = calculateImbalance(machineCode, machineAllocationMap);
                if (imbalance > maxImbalance) {
                    maxImbalance = imbalance;
                    bestImbalancedMachine = machine;
                }
            }
        }

        if (emptyMachine != null) {
            return emptyMachine;
        }
        return bestImbalancedMachine;
    }

    /**
     * 机台负载不均衡度 — 用于无空机台时的择优。
     *
     * <p>公式：{@code |本机 usedCapacity − 所有非空机台 usedCapacity 均值|}。
     * 此处 {@code usedCapacity} 为已分配试制<b>条数</b>累计（见 {@link #allocateTaskToMachine}）。
     */
    private int calculateImbalance(
            String machineCode,
            Map<String, MachineAllocationResult> machineAllocationMap) {
        int totalUsed = 0;
        int count = 0;
        for (MachineAllocationResult alloc : machineAllocationMap.values()) {
            if (!alloc.getTaskAllocations().isEmpty()) {
                totalUsed += alloc.getUsedCapacity();
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        int avgUsed = totalUsed / count;
        MachineAllocationResult current = machineAllocationMap.get(machineCode);
        int currentUsed = current != null ? current.getUsedCapacity() : 0;
        return Math.abs(currentUsed - avgUsed);
    }

    /** 在机台主数据列表中按编码查找 */
    private MdmMoldingMachine findMachine(String machineCode, List<MdmMoldingMachine> machines) {
        if (machines == null) {
            return null;
        }
        for (MdmMoldingMachine m : machines) {
            if (m.getCxMachineCode().equals(machineCode)) {
                return m;
            }
        }
        return null;
    }

    /**
     * 机台固定配置约束 — {@code MdmCxMachineFixed.disableStructure} 包含当前结构则不可选。
     *
     * @return true 表示允许在该机台生产此结构
     */
    private boolean checkStructureConstraint(
            MdmMoldingMachine machine, String structureName, ScheduleContextVo context) {
        if (structureName == null || context.getMachineFixedConfigs() == null) {
            return true;
        }
        for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
            if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                if (fixed.getDisableStructure() != null &&
                        fixed.getDisableStructure().contains(structureName)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ==================== 机台分配壳与任务写入 ====================

    /** 创建空机台分配，remainingCapacity 取机台日产能（条/天） */
    private MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextVo context) {
        MachineAllocationResult allocation =
                new MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    private int getMachineDailyCapacity(String machineCode, ScheduleContextVo context) {
        MdmMoldingMachine machine = findMachine(machineCode, context.getAvailableMachines());
        if (machine != null) {
            return machine.getMaxDayCapacity() != null ? machine.getMaxDayCapacity() : ScheduleConstants.DEFAULT_DAILY_CAPACITY;
        }
        return ScheduleConstants.DEFAULT_DAILY_CAPACITY;
    }

    /**
     * 将试制任务写入 TaskAllocation 并更新机台容量计数。
     *
     * <p>条数：{@code quantity} ← {@code plannedProduction}；
     * {@code endingExtraInventory} 优先任务字段，否则回退 plannedProduction。
     * 容量：{@code usedCapacity} / {@code remainingCapacity} 按<b>条数</b>增减（试制路径特有，区别于 DFS 硫化机台数）。
     *
     * <p>试制不在此做收尾补整车、库存扣减；标志位原样透传供 ShiftScheduleService 试制精排。
     */
    private void allocateTaskToMachine(
            MachineAllocationResult allocation,
            DailyEmbryoTask task) {

        TaskAllocation taskAllocation = task.toTaskAllocation(
                task.getPlannedProduction(),
                task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 1);
        // TrialTaskProcessor 特有 fallback：未设置 endingExtraInventory 时用 plannedProduction
        if (taskAllocation.getEndingExtraInventory() == null) {
            taskAllocation.setEndingExtraInventory(task.getPlannedProduction());
        }

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + task.getPlannedProduction());
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - task.getPlannedProduction());
    }
}
