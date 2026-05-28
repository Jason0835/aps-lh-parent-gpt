package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
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
 * 续作任务处理器
 * 
 * <p>负责续作任务的保底预留处理：
 * <ol>
 *   <li>标记所有续作任务 isContinueTask=true</li>
 *   <li>根据参数 SYS04070003 判断是否开启保底预留</li>
 *   <li>开启时：每个机台的每个历史胎胚保底预留1个硫化机在原机台</li>
 *   <li>剩余需求统一交给 NewTaskProcessor 做均衡分配</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContinueTaskProcessor {

    /** 默认日产能（条/天），机台未配置时使用 */
    private static final int DEFAULT_DAILY_CAPACITY = 1200;

    /** 参数编码：强制保留历史任务 */
    private static final String PARAM_FORCE_KEEP_HISTORY = "SYS04070003";

    /**
     * 处理续作任务
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

        // 标记续作任务
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
            task.setIsContinueTask(true);
        }

        // 检查是否强制保留历史任务
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);
        log.info("强制保留历史任务配置: {}", forceKeepHistory);

        if (!forceKeepHistory) {
            // 不做保底预留，续作任务全部由 NewTaskProcessor 统一均衡
            log.info("强制保留历史任务未开启，续作任务不保底预留，全部交给新增均衡处理");
            log.info("========== 续作任务处理完成（仅标记），共 {} 个任务 ==========", continueTasks.size());
            return results;
        }

        // 构建历史任务映射
        Map<String, Set<String>> machineHistoryMap = buildMachineHistoryMap(context);
        log.info("构建历史任务映射完成，共 {} 台机台有历史记录", machineHistoryMap.size());

        // 保底预留：每个机台的每个历史胎胚至少预留1个在原机台
        // 使用 Map<机台编码, MachineAllocationResult> 收集预留结果
        Map<String, CoreScheduleAlgorithmService.MachineAllocationResult> allocationMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> historyEntry : machineHistoryMap.entrySet()) {
            String machineCode = historyEntry.getKey();
            Set<String> historyEmbryos = historyEntry.getValue();

            for (String embryoCode : historyEmbryos) {
                // 在续作任务列表中找到有排产需求的任务（vulcanizeMachineCount>0 且 plannedProduction>0）
                CoreScheduleAlgorithmService.DailyEmbryoTask matchedTask = null;
                boolean foundAbandoned = false;
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
                    if (embryoCode.equals(task.getEmbryoCode())) {
                        int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                        Integer plannedProd = task.getPlannedProduction();
                        if (demand > 0 && (plannedProd != null && plannedProd > 0)) {
                            matchedTask = task;
                            break;
                        }
                        if (plannedProd != null && plannedProd <= 0) {
                            foundAbandoned = true;
                        }
                    }
                }

                if (matchedTask == null) {
                    if (foundAbandoned) {
                        log.info("机台 {} 的历史胎胚 {} 计划产量为0（已舍弃），跳过保底预留", machineCode, embryoCode);
                    } else {
                        log.debug("机台 {} 的历史胎胚 {} 在续作任务中无剩余需求，跳过保底预留", machineCode, embryoCode);
                    }
                    continue;
                }

                // 检查该机台是否支持当前结构+PRODUCTION_VERSION
                List<MpCxCapacityConfiguration> availableForTask = getAvailableMachinesForStructure(
                        matchedTask.getStructureName(), scheduleDate, context, matchedTask.getProductionVersion());
                boolean machineAvailable = availableForTask.stream()
                        .anyMatch(c -> machineCode.equals(c.getCxMachineCode()));
                if (!machineAvailable) {
                    log.info("机台 {} 不支持结构 {} 的生产版本 {}，跳过保底预留胎胚 {}",
                            machineCode, matchedTask.getStructureName(), matchedTask.getProductionVersion(), embryoCode);
                    continue;
                }

                int demand = matchedTask.getVulcanizeMachineCount() != null ? matchedTask.getVulcanizeMachineCount() : 0;

                // 保底预留1个硫化机
                int reservedCount = 1;
                matchedTask.setVulcanizeMachineCount(demand - reservedCount);

                // 构建分配结果
                CoreScheduleAlgorithmService.MachineAllocationResult allocation =
                        allocationMap.computeIfAbsent(machineCode, code -> createMachineAllocation(code, context));

                allocateContinueReservation(allocation, matchedTask, reservedCount, context);

                log.info("机台 {} 保底预留胎胚 {} 共 {} 个硫化机", machineCode, embryoCode, reservedCount);
            }
        }

        results.addAll(allocationMap.values());
        log.info("========== 续作任务保底预留完成，共 {} 台机台预留任务 ==========", results.size());
        return results;
    }

    /**
     * 保底预留分配到机台（续作保底预留场景，只分配预留的硫化机数，不是全量）
     */
    private void allocateContinueReservation(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            int reservedVulcanizeCount,
            ScheduleContextVo context) {

        // quantity 应该始终是胎胚数量，不是硫化机数
        // 使用 endingExtraInventory（最终需要生产的量，经过收尾处理）
        int quantity = task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0
                ? task.getEndingExtraInventory() : task.getDemandQuantity();

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation = new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setEmbryoCode(task.getEmbryoCode());
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialDesc(task.getMaterialDesc());
        taskAllocation.setMainMaterialDesc(task.getMainMaterialDesc());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(quantity);  // 设置为胎胚数量
        taskAllocation.setVulcanizeMachineCount(reservedVulcanizeCount);  // 硫化机数单独存储
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsProductionTrial(task.getIsProductionTrial());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setEndingExtraInventory(task.getEndingExtraInventory());  // 设置收尾额外库存
        taskAllocation.setIsLastEndingBatch(task.getIsLastEndingBatch());  // 设置是否收尾最后一批
        taskAllocation.setIsEndProduction(task.getIsEndProduction());  // 设置是否结束生产
        taskAllocation.setEndingAbandoned(task.getEndingAbandoned());  // 设置收尾是否被舍弃
        taskAllocation.setIsOpeningDayTask(task.getIsOpeningDayTask());  // 设置是否开产日任务
        taskAllocation.setIsClosingDayTask(task.getIsClosingDayTask());  // 设置是否停产日任务
        taskAllocation.setIsMainProduct(task.getIsMainProduct());
        taskAllocation.setIsContinueTask(true);  // 标记为续作预留
        taskAllocation.setLhId(task.getLhId());
        taskAllocation.setConstructionStage(task.getConstructionStage());
        taskAllocation.setIsFirstTask(task.getIsFirstTask());  // 传递是否首任务（新开规格）
        taskAllocation.setIsUrgentEnding(task.getIsUrgentEnding());  // 传递是否紧急收尾
        taskAllocation.setIsNearEnding(task.getIsNearEnding());  // 传递是否临近收尾

        allocation.getTaskAllocations().add(taskAllocation);
        // 注意：这里占用的是硫化机数，不是胎胚数量
        allocation.setUsedCapacity(allocation.getUsedCapacity() + reservedVulcanizeCount);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - reservedVulcanizeCount);
    }

    // ==================== 辅助方法 ====================

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

    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextVo context,
            String productionVersion) {
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                int day = scheduleDate.getDayOfMonth();
                // 过滤日期范围 + PRODUCTION_VERSION
                return configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= day && c.getEndDay() >= day)
                        .filter(c -> productionVersion == null
                                || productionVersion.equals(c.getProductionVersion()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    // ==================== 机台分配方法 ====================

    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextVo context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

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
