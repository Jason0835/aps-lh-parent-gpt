package com.zlt.aps.cx.vo;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * groupTasks 单次调用的共享工作状态（生命周期 = 一次
 * {@link com.zlt.aps.cx.service.engine.TaskGroupService#groupTasks} 调用）。
 *
 * <p>由 {@link com.zlt.aps.cx.service.engine.TaskGroupService} 在 R1/R2/R3 私有方法间传递，
 * 承载跨结构累计状态、每结构队列及只读入参快照。勿提升为 Spring Bean 或 ThreadLocal。
 *
 * <p>字段 intentionally public：与提取前内部类一致，供 engine 层直接读写，避免大量 getter 样板。
 *
 * @author APS Team
 */
public class StructureProcessStateVo {

    // ---- output ----
    public TaskGroupResultVo result;

    // ---- readonly inputs ----
    public ScheduleContextVo context;
    public LocalDate scheduleDate;
    public List<CxShiftConfig> dayShifts;
    public int currentClassIndex;
    public Map<String, MdmMaterialInfo> materialMap;
    public Map<String, CxStock> stockMap;
    public Map<String, Set<String>> machineOnlineEmbryoMap;
    public boolean isOpeningShift;
    public int stockHoursCap;
    public int stockHoursSoftTrigger;
    public boolean stockHoursCapEnabled;
    public Map<LhScheduleResult, String> priorityDescMap;
    public Set<String> allKeyProductStructures;
    public Map<String, Set<String>> machineToStructuresMap;
    public Map<String, String> machineCurrentEmbryoMap;

    // ---- material / structure accounts (cross-structure) ----
    public Map<String, Integer> materialUsedFormingRemainder;
    public Map<String, List<DailyEmbryoTask>> materialTasksMap;
    public Map<String, List<MpCxCapacityConfiguration>> structureRecommendedMachinesCache;
    public Map<String, Integer> structureTotalMaxLhCache;
    public Map<String, BigDecimal> structureAvgRatioCache;
    public Map<String, Integer> structureTaskCountMap;
    public Map<String, Set<String>> structureCountedMachineCodesMap;
    public Map<String, BigDecimal> structureCumulativeTimeMap;
    public Map<String, BigDecimal> structureAdvanceAvailableCapacityMap;

    // ---- warehouse ----
    public WarehouseControlStateVo whState;
    public int warehouseCapacity;
    public double warehouseCapacityRatio;
    public int warehouseThreshold;
    public Map<String, Integer> embryoTotalStockMap;
    public Map<String, Integer> shiftFormingOutputMap;
    public Map<String, Integer> shiftVulcanizingConsumptionMap;
    public int runningTotalProjectedStock;
    public Map<String, Integer> embryoTotalMoldMap;
    public Map<Long, Integer> taskVulcConsumptionMap;

    // ---- advance production ----
    public Map<String, Long> machineOccupiedTimeMap;
    public Map<String, Boolean> structureFullyEndedMap;
    public Set<String> advanceUsedMachineCodes;
    public Map<String, Long> machineSwitchRemainingMap;

    // ---- per-structure queues ----
    public List<DailyEmbryoTask> deferredTasks;
    public List<DailyEmbryoTask> firstRoundCompletedTasks;
    public List<DailyEmbryoTask> r2ExitedTasks;

    // ---- globals / counters ----
    public Set<DailyEmbryoTask> r2AddedToResultGlobal;
    public Set<DailyEmbryoTask> r3AddedToResultGlobal;
    public int skippedNullEmbryo;
    public int skippedNullTask;
    public int skippedVulcanizeSurplusZero;
    public int skippedFormingRemainderZero;
    public int skippedCapacityExceeded;
    public int skippedWarehouseFull;
}
