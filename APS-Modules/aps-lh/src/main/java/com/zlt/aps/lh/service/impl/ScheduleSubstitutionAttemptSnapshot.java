package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingAllocationSnapshot;
import com.zlt.aps.lh.engine.strategy.support.ContinuationRemainderFinishGroup;
import com.zlt.aps.lh.engine.strategy.support.ContinuationFinishScheduleResult;
import com.zlt.aps.lh.engine.strategy.support.ContinuationMachineFinishPlan;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingMachineProfile;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateAdditionalMachineDemand;
import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.CapsuleReplacementTimeWindowDTO;
import com.zlt.aps.lh.api.domain.dto.CleaningScheduleDateFillItem;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftRuntimeState;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleProcessLog;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.context.EmbryoStockConsumeLedger;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.LhShiftRemarkRuntime;
import com.zlt.aps.lh.component.StructureEarlyProductionAdmission;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.StructureShiftInMachineIndex;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.ActiveMachineBinding;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecision;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecisionLogEntry;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecisionLogCollector;
import com.zlt.aps.lh.engine.strategy.support.HistoricalStructureHandoffState;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.ScheduleSubstitutionDirective;
import com.zlt.aps.lh.engine.strategy.support.DayTypeBlockReverseSelectionDirective;
import com.zlt.aps.lh.engine.strategy.support.SharedMouldSubstitutionRecord;
import com.zlt.aps.lh.engine.strategy.support.SpecialMaterialSubstitutionRecord;
import com.zlt.aps.lh.engine.strategy.support.UnscheduledResultRuntime;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchPlan;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.ContinuationTemporaryFaultTransferEvent;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.ContinuousProductionConsumption;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 排程联动置换单候选的通用内存状态快照。
 *
 * <p>S4.5.1 的特殊材料置换和共用模具联动置换均需要复用现有新增排产主链，而该主链会同时修改排程结果、机台运行态、模具资源、
 * 换模/首检次数、日计划账本和未排结果。候选失败时仅恢复结果列表无法保证无副作用，因此本快照
 * 在真正截断续作结果前保存本次调用可能触达的全部运行态，并在失败时一次性恢复。</p>
 *
 * <p>该对象只在单个候选尝试期间存活，不落库，也不改变最终 S4.6 的数据库事务边界。</p>
 *
 * @author APS
 */
final class ScheduleSubstitutionAttemptSnapshot {

    /** 公共设备计划全部状态，包含0判定缓存、待复评、候选时间计划、安排记录和版本。 */
    private EquipmentPlanRuntimeState equipmentPlanRuntimeState;
    /**
     * 候选可改变的故障迁移进度；固定维修来源和禁产窗口不随候选撤销。
     */
    private Map<String, ContinuationTemporaryFaultTransferEvent> repairTransferEventMap;
    /**
     * 清洗完整状态及约束版本快照。
     */
    private CleaningPlanRuntimeState cleaningPlanRuntimeState;
    /** 不可变实际消费记录随结果及账本一起恢复。 */
    private Map<LhScheduleResult, ContinuousProductionConsumption> continuousProductionConsumptionMap;
    /** 旧公共状态仍需与设备计划一起回滚，避免嵌套预演污染。 */
    private Set<String> maintenanceDeferredPhysicalMachineCodeSet;
    private boolean maintenancePreDecisionCompleted;
    /** 消费记录与是否已最终扣账必须一起恢复。 */
    private boolean continuousDailyQuotaSynced;

    /** 结构班次占用与结果同步回滚，预演机台不得残留并占用正式提交名额。 */
    private StructureShiftInMachineIndex structureShiftInMachineIndex;
    /** 结构切换首班随候选整体回滚。 */
    private Map<String, StructureSwitchRuntimeState> structureSwitchRuntimeMap;
    /** 置换前结果与冻结切换计划的关联。 */
    private Map<LhScheduleResult, StructureSwitchPlan> structureSwitchResultPlanMap;
    /** 首检保护关系随候选结果一起回滚，不能污染后续重试。 */
    private Map<LhScheduleResult, FirstInspectionAllocationPlan> firstInspectionResultPlanMap;
    /**
     * 实际整机成对提交关系与结果身份一起恢复，失败候选及预演不能残留计数合并依据。
     */
    private Map<LhScheduleResult, LhScheduleResult> committedWholeSingleControlPairMap;

    /** 置换前排程结果对象顺序 */
    private List<LhScheduleResult> scheduleResultList;
    /** 置换前每个排程结果的字段快照，使用对象身份作为键 */
    private Map<LhScheduleResult, LhScheduleResult> scheduleResultStateMap;
    /** 置换前结果来源 SKU 映射 */
    private Map<LhScheduleResult, SkuScheduleDTO> scheduleResultSourceSkuMap;
    /**
     * 收尾延期增机首次实际生产日，失败预演或提交必须与结果和资源一起撤销。
     */
    private Map<String, LocalDate> endingDeferredFirstProductionDateMap;
    /** 置换前机台结果分配 */
    private Map<String, List<LhScheduleResult>> machineAssignmentMap;
    /** 置换前全部机台运行态 */
    private Map<String, MachineScheduleDTO> machineScheduleStateMap;
    /** 置换前机台班次剩余产能 */
    private Map<String, int[]> machineShiftCapacityMap;
    /** 候选前班次运行态剩余产能 */
    private Map<Integer, ShiftRuntimeState> shiftRuntimeStateMap;
    /** 置换前未排结果对象顺序 */
    private List<LhUnscheduledResult> unscheduledResultList;
    /** 置换前每条未排结果字段快照 */
    private Map<LhUnscheduledResult, LhUnscheduledResult> unscheduledResultStateMap;
    /** 置换前未排需求快照和原因事件运行态。 */
    private UnscheduledResultRuntime unscheduledResultRuntime;
    /** 置换前 SKU 剩余量账本 */
    private Map<String, Integer> skuProductionRemainingQtyMap;
    /** 置换前 SKU 目标量账本 */
    private Map<String, Integer> skuProductionTargetQtyMap;
    /** 置换前胎胚库存消费账本 */
    private Map<String, EmbryoStockConsumeLedger> embryoStockConsumeLedgerMap;
    /** 置换前各 SKU 胎胚库存配额 */
    private Map<String, Integer> embryoStockSkuQuotaMap;
    /** 置换前按胎胚库存硬收口的 SKU 集合 */
    private Set<String> embryoStockHardTargetMaterialSet;
    /**
     * 候选试排前已成立的共用胎胚收尾身份。
     */
    private Map<String, String> sharedEmbryoStockEndingIdentityMap;
    /**
     * 胎胚实机成员和虚拟硫化余量随试排恢复，避免回滚后重复领取库存或虚拟需求。
     */
    private Set<String> embryoStockRealMachineSkuKeys;
    /**
     * SKU准入期间的库存冻结门禁。
     */
    private boolean embryoStockAllocationDeferred;
    /**
     * 独立虚拟机硫化余量快照。
     */
    private Map<String, Integer> virtualMachineCuringRemainingQtyMap;
    /**
     * 独立虚拟需求的历史合法硫化总目标。
     */
    private Map<String, Integer> virtualMachineCuringTargetQtyMap;
    /**
     * 候选试排前的共用胎胚首次额度。
     */
    private Map<String, Integer> sharedEmbryoStockEndingInitialQuotaMap;
    /**
     * 候选试排前的共用胎胚实际消费量。
     */
    private Map<String, Integer> sharedEmbryoStockEndingConsumedQtyMap;
    /**
     * 试排前独立胎胚收尾的逐机任务量。
     */
    private Map<LhScheduleResult, Integer> sharedEmbryoStockEndingMachineQuotaMap;
    /**
     * 试排前独立胎胚收尾的逐机释放时间。
     */
    private Map<LhScheduleResult, Date> sharedEmbryoStockEndingMachineReleaseMap;
    /** 置换前满足降模条件的 SKU 集合 */
    private Set<String> skuDecrementKeySet;
    /** 置换前已执行降模处理的 SKU 集合 */
    private Set<String> decrementHandledSkuKeySet;
    /** 置换前早中班换模计数 */
    private Map<String, int[]> dailyMouldChangeCountMap;
    /** 置换前早中班首检计数 */
    private Map<String, int[]> dailyFirstInspectionCountMap;
    /** 置换前班次首检顺序计数 */
    private Map<String, Integer> shiftFirstInspectionCountMap;
    /** 置换前同胎胚换模班次占用 */
    private Map<String, Set<Integer>> greenTireChangeoverShiftMap;
    /** 置换前结构已排机台运行态 */
    private Map<LocalDate, Map<String, Set<String>>> structureScheduledMachineCodeMap;
    /** 置换前 SKU 已排机台运行态 */
    private Map<LocalDate, Map<String, Set<String>>> skuScheduledMachineCodeMap;
    /** 置换前每日保养计数 */
    private Map<String, Integer> dailyMaintenanceCountMap;
    /** 置换前每日保养物理机集合 */
    private Map<String, Set<String>> dailyMaintenancePhysicalMachineSetMap;
    /** 置换前保养顺延日志去重键 */
    private Set<String> maintenanceResumeDelayLogKeySet;
    /** 置换前换模上限阻塞原因 */
    private Map<String, String> mouldChangeLimitBlockedReasonMap;
    /** 置换前新增选机日志序号 */
    private Map<String, Integer> newSpecMachineSelectionCountMap;
    /** 新增机台驱动实时顺序和结果快照 */
    private Map<SkuScheduleDTO, Map<Integer, Integer>> newSpecRealtimeSelectionOrderMap;
    private Set<LhScheduleResult> newSpecRealtimeSnapshotResultSet;
    /** 置换前新增待排类型计数 */
    private int pendingFormalNewSpecSkuCount;
    private int pendingTrialNewSpecSkuCount;
    private int pendingMassTrialNewSpecSkuCount;
    private int pendingSmallBatchNewSpecSkuCount;
    /** 置换前新增选机临时判定，均使用 SKU 对象身份作为键 */
    private Map<SkuScheduleDTO, Boolean> newSpecTypeRuleBlockedMap;
    private Map<SkuScheduleDTO, Boolean> newSpecEarlyProductionAllowedMap;
    private Map<SkuScheduleDTO, Boolean> newSpecSingleControlStructureEndingLayerMap;
    /** 置换前满班补齐超排累计账本 */
    private Map<String, Integer> skuShiftFillOverQtyMap;
    /** 置换前定点机台挤量预留信息 */
    private Map<String, String> specifyMachineReservedMaterialMap;
    private Map<String, Date> specifyMachineReservedSwitchStartTimeMap;
    /** 跨日准备换模事件 */
    private Set<String> crossDayPreparationMouldChangeEventKeySet;
    /** 置换前胶囊运行态及换胶囊班次资源 */
    private Map<String, Integer> capsuleRuntimeUsageMap;
    private Set<String> capsuleReplacementShiftKeySet;
    private Set<String> capsuleThresholdHandledMachineSet;
    private Map<String, Integer> capsuleReplacementShiftCapacityLimitMap;
    private Map<String, CapsuleReplacementTimeWindowDTO> capsuleReplacementTimeWindowMap;
    /** 置换前运行态有效胎胚 SKU */
    private Map<String, List<String>> activeEmbryoSkuMap;
    /** 置换前动态单胎胚收尾集合 */
    private Set<String> dynamicSingleEmbryoEndingMaterialSet;
    /** 置换前精确置换成功记录 */
    private List<SpecialMaterialSubstitutionRecord> substitutionRecordList;
    /** 置换前共用模具联动成功记录 */
    private List<SharedMouldSubstitutionRecord> sharedMouldSubstitutionRecordList;
    /** 置换前新增待排 SKU 列表，预演会临时隔离为 A 或 B 单个 SKU。 */
    private List<SkuScheduleDTO> newSpecSkuList;
    /** 班次9候选归属也随失败预演恢复，不能残留未生效的日期池。 */
    private List<SkuScheduleDTO> nextShiftNewPlanCandidateList;
    private Map<String, LocalDate> nextShiftNewPlanPoolDateMap;

    /** 置换前模具交替计划；换活字块或局部策略不得在失败预演中残留。 */
    private List<LhMouldChangePlan> mouldChangePlanList;
    /** 前置组合的绑定及未来模具释放记录随失败尝试一起恢复。 */
    private List<ActiveMachineBinding> preScheduledMachineBindingList;
    private Map<String, Date> preScheduledMouldReleaseTimeMap;
    /** 置换前持久化过程日志；预演日志必须随快照回滚。 */
    private List<LhScheduleProcessLog> scheduleLogList;
    /** 按天换活字块反选及结构待排视图 */
    private List<DayTypeBlockReverseSelectionDirective> dayTypeBlockReverseSelectionDirectiveList;
    private Map<String, String> dayTypeBlockReverseSelectedSkuKeyMap;
    private Map<String, List<SkuScheduleDTO>> structureSkuMap;
    private Map<LocalDate, Map<String, StructureEarlyProductionAdmission>>
            structureEarlyProductionAdmissionMap;
    /** 置换前结果级收尾、共用胎胚错峰和精度前插运行态。 */
    private Map<LhScheduleResult, Integer> sharedEmbryoReleaseShiftIndexMap;
    private Map<LhScheduleResult, Integer> sharedEmbryoReleaseShiftQtyMap;
    private Map<LhScheduleResult, Integer> sharedEmbryoAllowedOverQtyMap;
    private Map<LhScheduleResult, Integer> endingFillAllowedOverQtyMap;
    /** 置换前SKU收尾补满动作基准量，用于组级允许超量重算回滚 */
    private Map<LhScheduleResult, Integer> endingFillBeforeQtyMap;
    private Set<LhScheduleResult> precisionPreInsertResultSet;
    private Map<LhScheduleResult, Date> precisionPreInsertInspectionTimeMap;
    private Map<LhScheduleResult, Date> precisionPreInsertMouldChangeTimeMap;
    private Map<LhScheduleResult, Integer> precisionPreInsertInspectionShiftIndexMap;
    /** 置换前提前生产运行视图，预演结束后不能无条件丢弃调用前状态。 */
    private Map<SkuScheduleDTO, EarlyProductionRuntimePlan> earlyProductionRuntimePlanMap;
    /**
     * 历史承接诊断随候选回滚，负荷预演不能提前解除正式机台的保留。
     */
    private Map<LhMouldChangePlan, HistoricalStructureHandoffState> historicalStructureHandoffStateMap;
    /**
     * 预演前日志采集器身份，保留运行视图对原明细对象的引用。
     */
    private Map<LocalDate, EarlyProductionDecisionLogCollector> earlyProductionDecisionLogCollectorMap;
    /**
     * 每个原采集器的明细顺序，恢复时删除预演新增日志。
     */
    private Map<EarlyProductionDecisionLogCollector, List<EarlyProductionDecisionLogEntry>> earlyProductionLogEntries;
    /**
     * 原明细对象的字段快照，撤回预演对已有日志的成功标记和数量更新。
     */
    private Map<EarlyProductionDecisionLogEntry, EarlyProductionDecisionLogEntry> earlyProductionLogStates;
    /** 置换前当前排程日期 */
    private Date currentScheduleDate;
    /** 置换前临时指定机台与共用模具联动指令，内部候选失败恢复时必须保持调用方原指令。 */
    private String specialMaterialSpecifiedMachineCode;
    private String specialMaterialSpecifiedSkuKey;
    private Date specialMaterialEarliestSwitchTime;
    private ScheduleSubstitutionDirective scheduleSubstitutionDirective;
    /** 置换前所有联动 SKU 字段快照，按对象身份保存 A、B 及必要来源 SKU。 */
    private Map<SkuScheduleDTO, SkuScheduleDTO> skuStateMap;
    /** 置换前所有联动 SKU 日计划账本深拷贝。 */
    private Map<SkuScheduleDTO, Map<LocalDate, SkuDailyPlanQuotaDTO>> skuDailyQuotaMap;
    /** 原始账本引用，用于恢复续作与补偿SKU的共享关系。 */
    private Map<SkuScheduleDTO, Map<LocalDate, SkuDailyPlanQuotaDTO>> skuDailyQuotaReferenceMap;

    /** 历史承载置换新增的退出时刻与物料身份，失败必须同步恢复。 */
    private Map<String, Date> previousAlternateReleasedMachineTimeMap;
    private Map<String, String> previousAlternateReleasedSkuKeyMap;
    private Map<SkuScheduleDTO, Date> previousAlternateCandidateAvailableTimeMap;
    /** 正式交替结果标记，避免失败结果残留影响同料计划生成。 */
    private Map<LhScheduleResult, LhMouldChangePlan> previousAlternateResultPlanMap;
    /**
     * 换块成功残量来源随候选尝试恢复，保留候选对象身份并隔离集合本身。
     */
    private Set<SkuScheduleDTO> typeBlockResidualSceneSkuSet;
    /**
     * 备注的正式关联和精确历史任务待确认附件随业务尝试恢复。
     */
    private LhShiftRemarkRuntime shiftRemarkRuntime;
    /**
     * 成对承接的前料展示来源快照，恢复时保持原结果对象身份且不残留预演关联。
     */
    private Map<LhScheduleResult, LhMouldChangePlan> previousAlternateBeforeMaterialSourceMap;
    /** 历史指定增机值对象不可变，复制映射即可恢复失败尝试及整批预演。 */
    private Map<String, PreviousAlternateAdditionalMachineDemand>
            previousAlternateAdditionalMachineDemandMap;

    /** 收尾只读快照采用写时复制，置换失败恢复原对象映射。 */
    private Map<LhScheduleResult, ContinuationEndingAllocationSnapshot> continuationFinishSnapshots;
    /**
     * 收尾组保留对象身份，另存类型、目标、预算、容量及公共完成输出的内容快照。
     */
    private List<ContinuationRemainderFinishGroup> continuationFinishGroups;
    /**
     * 预演会改变公共完成事实，不能仅复制收尾组列表外壳。
     */
    private Map<ContinuationRemainderFinishGroup, ContinuationRemainderFinishGroup> continuationFinishGroupStates;
    /**
     * 分组时已保护的原机集合与组内容同时回滚。
     */
    private Set<LhScheduleResult> continuationFinishAllocatedResults;

    private ScheduleSubstitutionAttemptSnapshot() {
    }

    /**
     * 捕获单候选置换前状态。
     *
     * @param context 排程上下文
     * @param specialSku 待排特殊材料 SKU
     * @return 可用于失败恢复的状态快照
     */
    static ScheduleSubstitutionAttemptSnapshot capture(
            LhScheduleContext context,
            SkuScheduleDTO specialSku) {
        List<SkuScheduleDTO> skuList = new ArrayList<SkuScheduleDTO>(1);
        if (specialSku != null) {
            skuList.add(specialSku);
        }
        return capture(context, skuList);
    }

    /**
     * 捕获一次联动置换预演或正式提交前的完整运行态。
     *
     * @param context 排程上下文
     * @param skuCollection 本次可能被修改的 A、B 及来源 SKU
     * @return 可用于原子恢复的通用快照
     */
    static ScheduleSubstitutionAttemptSnapshot capture(
            LhScheduleContext context,
            Collection<SkuScheduleDTO> skuCollection) {
        ScheduleSubstitutionAttemptSnapshot snapshot =
                new ScheduleSubstitutionAttemptSnapshot();
        snapshot.previousAlternateReleasedMachineTimeMap = new LinkedHashMap<>(context.getPreviousAlternateReleasedMachineTimeMap());
        snapshot.previousAlternateReleasedSkuKeyMap = new LinkedHashMap<>(context.getPreviousAlternateReleasedSkuKeyMap());
        snapshot.previousAlternateCandidateAvailableTimeMap = new IdentityHashMap<>(context.getPreviousAlternateCandidateAvailableTimeMap());
        snapshot.previousAlternateResultPlanMap = new IdentityHashMap<>(context.getPreviousAlternateResultPlanMap());
        snapshot.typeBlockResidualSceneSkuSet =
                Collections.newSetFromMap(new IdentityHashMap<SkuScheduleDTO, Boolean>(16));
        snapshot.typeBlockResidualSceneSkuSet.addAll(context.getTypeBlockResidualSceneSkuSet());
        snapshot.shiftRemarkRuntime = context.getShiftRemarkRuntime().snapshot();
        snapshot.previousAlternateBeforeMaterialSourceMap =
                new IdentityHashMap<>(context.getPreviousAlternateBeforeMaterialSourceMap());
        snapshot.previousAlternateAdditionalMachineDemandMap =
                new LinkedHashMap<>(context.getPreviousAlternateAdditionalMachineDemandMap());
        snapshot.continuationFinishSnapshots = copyContinuationFinishSnapshots(context.getContinuationSurplusEndingSnapshotMap());
        snapshot.continuationFinishGroups = new ArrayList<>(context.getContinuationRemainderFinishGroups());
        snapshot.continuationFinishGroupStates = new IdentityHashMap<>();
        snapshot.continuationFinishGroups.forEach(group -> snapshot.continuationFinishGroupStates.put(group, copyFinishGroup(group)));
        snapshot.continuationFinishAllocatedResults = copyIdentityResultSet(context.getContinuationSurplusEndingAllocatedResults());
        snapshot.nextShiftNewPlanCandidateList = new ArrayList<>(context.getNextShiftNewPlanCandidateList());
        snapshot.nextShiftNewPlanPoolDateMap = new LinkedHashMap<>(context.getNextShiftNewPlanPoolDateMap());
        snapshot.structureShiftInMachineIndex = Objects.isNull(context.getStructureShiftInMachineIndex())
                ? null : context.getStructureShiftInMachineIndex().copy();
        snapshot.structureSwitchRuntimeMap = new LinkedHashMap<>(context.getStructureSwitchRuntimeMap());
        snapshot.structureSwitchResultPlanMap = new IdentityHashMap<>(context.getStructureSwitchResultPlanMap());
        snapshot.firstInspectionResultPlanMap = new IdentityHashMap<>(context.getFirstInspectionResultPlanMap());
        snapshot.committedWholeSingleControlPairMap = new IdentityHashMap<>(context.getCommittedWholeSingleControlPairMap());
        snapshot.scheduleResultList = new ArrayList<LhScheduleResult>(context.getScheduleResultList());
        snapshot.scheduleResultStateMap = new IdentityHashMap<LhScheduleResult, LhScheduleResult>(
                Math.max(16, context.getScheduleResultList().size() * 2));
        for (LhScheduleResult result : context.getScheduleResultList()) {
            snapshot.scheduleResultStateMap.put(result, copyBean(result, LhScheduleResult.class));
        }
        // 零量行已移除时，公共快照仍引用原对象；预演修改这些字段也必须恢复。
        context.getContinuationSurplusEndingSnapshotMap().keySet().forEach(result ->
                snapshot.scheduleResultStateMap.putIfAbsent(result, copyBean(result, LhScheduleResult.class)));
        snapshot.scheduleResultSourceSkuMap =
                new IdentityHashMap<LhScheduleResult, SkuScheduleDTO>(context.getScheduleResultSourceSkuMap());
        snapshot.endingDeferredFirstProductionDateMap =
                new LinkedHashMap<>(context.getEndingDeferredFirstProductionDateMap());
        snapshot.machineAssignmentMap = copyResultListMap(context.getMachineAssignmentMap());
        snapshot.machineScheduleStateMap = copyMachineScheduleMap(context.getMachineScheduleMap());
        snapshot.machineShiftCapacityMap = copyIntArrayMap(context.getMachineShiftCapacityMap());
        snapshot.shiftRuntimeStateMap = copyShiftRuntimeStateMap(
                context.getShiftRuntimeStateMap());

        snapshot.unscheduledResultList =
                new ArrayList<LhUnscheduledResult>(context.getUnscheduledResultList());
        snapshot.unscheduledResultStateMap =
                new IdentityHashMap<LhUnscheduledResult, LhUnscheduledResult>(
                        Math.max(16, context.getUnscheduledResultList().size() * 2));
        for (LhUnscheduledResult result : context.getUnscheduledResultList()) {
            snapshot.unscheduledResultStateMap.put(result, copyBean(result, LhUnscheduledResult.class));
        }
        snapshot.unscheduledResultRuntime = context.getUnscheduledResultRuntime().copy();

        snapshot.skuProductionRemainingQtyMap =
                new LinkedHashMap<String, Integer>(context.getSkuProductionRemainingQtyMap());
        snapshot.skuProductionTargetQtyMap =
                new LinkedHashMap<String, Integer>(context.getSkuProductionTargetQtyMap());
        snapshot.embryoStockConsumeLedgerMap =
                copyEmbryoLedgerMap(context.getEmbryoStockConsumeLedgerMap());
        /*
         * 特殊材料候选试排会经过目标量、胎胚配额和降模判断，必须同步快照这些数量判定状态。
         * 否则失败候选可能残留硬目标或降模处理标记，影响下一候选以及后续普通排程。
         */
        snapshot.embryoStockSkuQuotaMap =
                new LinkedHashMap<String, Integer>(context.getEmbryoStockSkuQuotaMap());
        snapshot.embryoStockHardTargetMaterialSet =
                new LinkedHashSet<String>(context.getEmbryoStockHardTargetMaterialSet());
        snapshot.sharedEmbryoStockEndingIdentityMap =
                new LinkedHashMap<>(context.getSharedEmbryoStockEndingIdentityMap());
        snapshot.embryoStockRealMachineSkuKeys = Objects.isNull(context.getEmbryoStockRealMachineSkuKeys())
                ? null : new LinkedHashSet<>(context.getEmbryoStockRealMachineSkuKeys());
        snapshot.embryoStockAllocationDeferred = context.isEmbryoStockAllocationDeferred();
        snapshot.virtualMachineCuringRemainingQtyMap =
                new LinkedHashMap<>(context.getVirtualMachineCuringRemainingQtyMap());
        snapshot.virtualMachineCuringTargetQtyMap =
                new LinkedHashMap<>(context.getVirtualMachineCuringTargetQtyMap());
        snapshot.sharedEmbryoStockEndingInitialQuotaMap =
                new LinkedHashMap<>(context.getSharedEmbryoStockEndingInitialQuotaMap());
        snapshot.sharedEmbryoStockEndingConsumedQtyMap =
                new LinkedHashMap<>(context.getSharedEmbryoStockEndingConsumedQtyMap());
        snapshot.sharedEmbryoStockEndingMachineQuotaMap =
                new IdentityHashMap<>(context.getSharedEmbryoStockEndingMachineQuotaMap());
        snapshot.sharedEmbryoStockEndingMachineReleaseMap =
                copyResultDateMap(context.getSharedEmbryoStockEndingMachineReleaseMap());
        snapshot.skuDecrementKeySet =
                new LinkedHashSet<String>(context.getSkuDecrementKeySet());
        snapshot.decrementHandledSkuKeySet =
                new LinkedHashSet<String>(context.getDecrementHandledSkuKeySet());
        snapshot.dailyMouldChangeCountMap = copyIntArrayMap(context.getDailyMouldChangeCountMap());
        snapshot.dailyFirstInspectionCountMap = copyIntArrayMap(context.getDailyFirstInspectionCountMap());
        snapshot.shiftFirstInspectionCountMap =
                new LinkedHashMap<String, Integer>(context.getShiftFirstInspectionCountMap());
        snapshot.greenTireChangeoverShiftMap = copyIntegerSetMap(context.getGreenTireChangeoverShiftMap());
        snapshot.structureScheduledMachineCodeMap =
                copyScheduledMachineMap(context.getStructureScheduledMachineCodeMap());
        snapshot.skuScheduledMachineCodeMap =
                copyScheduledMachineMap(context.getSkuScheduledMachineCodeMap());
        snapshot.dailyMaintenanceCountMap =
                new LinkedHashMap<String, Integer>(context.getDailyMaintenanceCountMap());
        snapshot.equipmentPlanRuntimeState = context.getEquipmentPlanRuntimeState().copy();
        snapshot.repairTransferEventMap = context.getContinuationTemporaryFaultTransferEventMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> RepairPlanRuntimeState.copyEvent(entry.getValue()),
                        (first, duplicate) -> first, LinkedHashMap::new));
        snapshot.cleaningPlanRuntimeState = context.getCleaningPlanRuntimeState().copy();
        snapshot.continuousProductionConsumptionMap = new IdentityHashMap<>(context.getContinuousProductionConsumptionMap());
        snapshot.maintenanceDeferredPhysicalMachineCodeSet =
                new LinkedHashSet<>(context.getMaintenanceDeferredPhysicalMachineCodeSet());
        snapshot.maintenancePreDecisionCompleted = context.isMaintenancePreDecisionCompleted();
        snapshot.continuousDailyQuotaSynced = context.isContinuousDailyQuotaSynced();
        snapshot.dailyMaintenancePhysicalMachineSetMap =
                copyStringSetMap(context.getDailyMaintenancePhysicalMachineSetMap());
        snapshot.maintenanceResumeDelayLogKeySet =
                new LinkedHashSet<String>(context.getMaintenanceResumeDelayLogKeySet());
        snapshot.mouldChangeLimitBlockedReasonMap =
                new LinkedHashMap<String, String>(context.getMouldChangeLimitBlockedReasonMap());
        snapshot.newSpecMachineSelectionCountMap =
                new LinkedHashMap<String, Integer>(context.getNewSpecMachineSelectionCountMap());
        snapshot.newSpecRealtimeSelectionOrderMap =
                copyRealtimeSelectionOrderMap(
                        context.getNewSpecRealtimeSelectionOrderMap());
        snapshot.newSpecRealtimeSnapshotResultSet =
                copyIdentityResultSet(context.getNewSpecRealtimeSnapshotResultSet());
        snapshot.pendingFormalNewSpecSkuCount = context.getPendingFormalNewSpecSkuCount();
        snapshot.pendingTrialNewSpecSkuCount = context.getPendingTrialNewSpecSkuCount();
        snapshot.pendingMassTrialNewSpecSkuCount = context.getPendingMassTrialNewSpecSkuCount();
        snapshot.pendingSmallBatchNewSpecSkuCount = context.getPendingSmallBatchNewSpecSkuCount();
        snapshot.newSpecTypeRuleBlockedMap =
                new IdentityHashMap<SkuScheduleDTO, Boolean>(context.getNewSpecTypeRuleBlockedMap());
        snapshot.newSpecEarlyProductionAllowedMap =
                new IdentityHashMap<SkuScheduleDTO, Boolean>(
                        context.getNewSpecEarlyProductionAllowedMap());
        snapshot.newSpecSingleControlStructureEndingLayerMap =
                new IdentityHashMap<SkuScheduleDTO, Boolean>(
                        context.getNewSpecSingleControlStructureEndingLayerMap());
        snapshot.skuShiftFillOverQtyMap =
                new LinkedHashMap<String, Integer>(context.getSkuShiftFillOverQtyMap());
        snapshot.specifyMachineReservedMaterialMap =
                new LinkedHashMap<String, String>(context.getSpecifyMachineReservedMaterialMap());
        snapshot.specifyMachineReservedSwitchStartTimeMap =
                new LinkedHashMap<String, Date>(
                        context.getSpecifyMachineReservedSwitchStartTimeMap());
        snapshot.crossDayPreparationMouldChangeEventKeySet =
                new LinkedHashSet<String>(
                        context.getCrossDayPreparationMouldChangeEventKeySet());
        snapshot.capsuleRuntimeUsageMap =
                new LinkedHashMap<String, Integer>(context.getCapsuleRuntimeUsageMap());
        snapshot.capsuleReplacementShiftKeySet =
                new LinkedHashSet<String>(context.getCapsuleReplacementShiftKeySet());
        snapshot.capsuleThresholdHandledMachineSet =
                new LinkedHashSet<String>(context.getCapsuleThresholdHandledMachineSet());
        snapshot.capsuleReplacementShiftCapacityLimitMap =
                new LinkedHashMap<String, Integer>(
                        context.getCapsuleReplacementShiftCapacityLimitMap());
        snapshot.capsuleReplacementTimeWindowMap = copyCapsuleReplacementTimeWindowMap(
                context.getCapsuleReplacementTimeWindowMap());
        snapshot.activeEmbryoSkuMap = copyStringListMap(context.getActiveEmbryoSkuMap());
        snapshot.dynamicSingleEmbryoEndingMaterialSet =
                new LinkedHashSet<String>(context.getDynamicSingleEmbryoEndingMaterialSet());
        snapshot.substitutionRecordList =
                new ArrayList<SpecialMaterialSubstitutionRecord>(
                        context.getSpecialMaterialSubstitutionRecordList());
        snapshot.sharedMouldSubstitutionRecordList =
                new ArrayList<SharedMouldSubstitutionRecord>(
                        context.getSharedMouldSubstitutionRecordList());
        snapshot.newSpecSkuList = new ArrayList<SkuScheduleDTO>(context.getNewSpecSkuList());
        snapshot.mouldChangePlanList =
                new ArrayList<LhMouldChangePlan>(context.getMouldChangePlanList());
        snapshot.scheduleLogList =
                new ArrayList<LhScheduleProcessLog>(context.getScheduleLogList());
        snapshot.dayTypeBlockReverseSelectionDirectiveList = copyBeanList(
                context.getDayTypeBlockReverseSelectionDirectiveList(),
                DayTypeBlockReverseSelectionDirective.class);
        snapshot.dayTypeBlockReverseSelectedSkuKeyMap =
                new LinkedHashMap<String, String>(
                        context.getDayTypeBlockReverseSelectedSkuKeyMap());
        snapshot.structureSkuMap = copySkuListMap(context.getStructureSkuMap());
        snapshot.structureEarlyProductionAdmissionMap =
                copyStructureAdmissionMap(
                        context.getStructureEarlyProductionAdmissionMap());
        snapshot.sharedEmbryoReleaseShiftIndexMap =
                new IdentityHashMap<LhScheduleResult, Integer>(
                        context.getSharedEmbryoEndingStaggerReleaseShiftIndexMap());
        snapshot.sharedEmbryoReleaseShiftQtyMap =
                new IdentityHashMap<LhScheduleResult, Integer>(
                        context.getSharedEmbryoEndingStaggerReleaseShiftQtyMap());
        snapshot.sharedEmbryoAllowedOverQtyMap =
                new IdentityHashMap<LhScheduleResult, Integer>(
                        context.getSharedEmbryoEndingStaggerAllowedOverQtyMap());
        snapshot.endingFillAllowedOverQtyMap =
                new IdentityHashMap<LhScheduleResult, Integer>(
                        context.getEndingFillAllowedOverQtyMap());
        snapshot.endingFillBeforeQtyMap =
                new IdentityHashMap<LhScheduleResult, Integer>(
                        context.getEndingFillBeforeQtyMap());
        snapshot.precisionPreInsertResultSet =
                java.util.Collections.newSetFromMap(
                        new IdentityHashMap<LhScheduleResult, Boolean>());
        snapshot.precisionPreInsertResultSet.addAll(
                context.getPrecisionPreInsertResultSet());
        snapshot.precisionPreInsertInspectionTimeMap =
                new IdentityHashMap<LhScheduleResult, Date>(
                        context.getPrecisionPreInsertInspectionTimeMap());
        snapshot.precisionPreInsertMouldChangeTimeMap =
                new IdentityHashMap<LhScheduleResult, Date>(
                        context.getPrecisionPreInsertMouldChangeTimeMap());
        snapshot.precisionPreInsertInspectionShiftIndexMap =
                new IdentityHashMap<LhScheduleResult, Integer>(
                        context.getPrecisionPreInsertInspectionShiftIndexMap());
        snapshot.earlyProductionRuntimePlanMap =
                copyEarlyProductionRuntimePlanMap(
                        context.getEarlyProductionRuntimePlanMap());
        snapshot.historicalStructureHandoffStateMap = new IdentityHashMap<>(context.getHistoricalStructureHandoffStateMap());
        snapshot.earlyProductionDecisionLogCollectorMap = new LinkedHashMap<>(context.getEarlyProductionDecisionLogCollectorMap());
        snapshot.earlyProductionLogEntries = new IdentityHashMap<>();
        snapshot.earlyProductionLogStates = new IdentityHashMap<>();
        snapshot.earlyProductionDecisionLogCollectorMap.values().forEach(collector -> {
            List<EarlyProductionDecisionLogEntry> entries = collector.snapshotEntries();
            snapshot.earlyProductionLogEntries.put(collector, entries);
            entries.forEach(entry -> snapshot.earlyProductionLogStates.put(entry,
                    copyBean(entry, EarlyProductionDecisionLogEntry.class)));
        });
        snapshot.currentScheduleDate = context.getCurrentScheduleDate();
        snapshot.specialMaterialSpecifiedMachineCode =
                context.getSpecialMaterialSpecifiedMachineCode();
        snapshot.specialMaterialSpecifiedSkuKey =
                context.getSpecialMaterialSpecifiedSkuKey();
        snapshot.specialMaterialEarliestSwitchTime =
                context.getSpecialMaterialEarliestSwitchTime();
        snapshot.scheduleSubstitutionDirective = Objects.isNull(
                context.getScheduleSubstitutionDirective())
                ? null : copyBean(context.getScheduleSubstitutionDirective(),
                ScheduleSubstitutionDirective.class);
        snapshot.skuStateMap = new IdentityHashMap<SkuScheduleDTO, SkuScheduleDTO>(4);
        snapshot.skuDailyQuotaMap =
                new IdentityHashMap<SkuScheduleDTO, Map<LocalDate, SkuDailyPlanQuotaDTO>>(4);
        snapshot.skuDailyQuotaReferenceMap =
                new IdentityHashMap<SkuScheduleDTO, Map<LocalDate, SkuDailyPlanQuotaDTO>>(4);
        // 独立调整原对象不一定在普通候选集合中，必须一并恢复其字段和共享日计划账本。
        List<SkuScheduleDTO> capturedSkus = new ArrayList<>(context.getDayPlanAdjustSkuList());
        // 公共组来源可能已退出候选池；嵌套预演修改其目标或消费后仍须恢复DTO与共享dayN。
        context.getContinuationRemainderFinishGroups().stream().map(ContinuationRemainderFinishGroup::getSourceSku)
                .forEach(capturedSkus::add);
        context.getScheduleResultSourceSkuMap().values().stream()
                .filter(sku -> Objects.nonNull(sku) && context.getSharedEmbryoStockEndingIdentityMap().containsKey(
                        MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus())))
                .forEach(capturedSkus::add);
        // 无日计划的指定试验原对象与派生候选共享额度，失败仍需一并恢复。
        capturedSkus.addAll(context.getPreviousAlternateTrialDemandList());
        // 前置结构切换绑定可能已移出待排列表，逐班续排失败仍须恢复其共享数量与日计划账本。
        context.getPreScheduledMachineBindingList().stream().map(ActiveMachineBinding::getSku)
                .forEach(capturedSkus::add);
        if (Objects.nonNull(skuCollection)) {
            capturedSkus.addAll(skuCollection);
        }
        for (SkuScheduleDTO sku : capturedSkus) {
            if (Objects.isNull(sku) || snapshot.skuStateMap.containsKey(sku)) {
                continue;
            }
            snapshot.skuStateMap.put(sku, copyBean(sku, SkuScheduleDTO.class));
            snapshot.skuDailyQuotaMap.put(sku, copyDailyQuotaMap(sku.getDailyPlanQuotaMap()));
            snapshot.skuDailyQuotaReferenceMap.put(sku, sku.getDailyPlanQuotaMap());
        }
        snapshot.preScheduledMachineBindingList = new ArrayList<>(context.getPreScheduledMachineBindingList());
        snapshot.preScheduledMouldReleaseTimeMap = new LinkedHashMap<>(context.getPreScheduledMouldReleaseTimeMap());
        return snapshot;
    }

    /**
     * 恢复候选置换前状态。
     *
     * @param context 排程上下文
     */
    void restore(LhScheduleContext context) {
        this.restore(context, true);
    }

    /**
     * 将资源快照复制到班次9副本，不恢复或改写原结果和原SKU对象。
     * 调用方必须先切断机台Map引用，并在排产前替换副本中的结果及SKU视图。
     *
     * @param context 班次9独立上下文
     */
    void copyResourcesTo(LhScheduleContext context) {
        this.restore(context, false);
    }

    /**
     * 恢复资源容器，原窗口回滚才允许恢复原对象字段。
     *
     * @param context 目标上下文
     * @param restoreSourceObjects 是否恢复原结果和SKU对象
     */
    private void restore(LhScheduleContext context, boolean restoreSourceObjects) {
        // 班次9使用新实例；原窗口回滚保留外层局部收集域，只恢复可发布关联。
        if (restoreSourceObjects) {
            context.getShiftRemarkRuntime().restore(shiftRemarkRuntime);
        } else {
            context.setShiftRemarkRuntime(new LhShiftRemarkRuntime());
        }
        // 每次恢复再次复制，避免重试、嵌套回滚或班次9副本反向修改快照。
        context.setStructureShiftInMachineIndex(Objects.isNull(structureShiftInMachineIndex)
                ? null : structureShiftInMachineIndex.copy());
        context.setStructureSwitchRuntimeMap(new LinkedHashMap<>(structureSwitchRuntimeMap));
        context.setStructureSwitchResultPlanMap(new IdentityHashMap<>(structureSwitchResultPlanMap));
        context.setTypeBlockResidualSceneSkuSet(
                Collections.newSetFromMap(new IdentityHashMap<SkuScheduleDTO, Boolean>(16)));
        context.getTypeBlockResidualSceneSkuSet().addAll(typeBlockResidualSceneSkuSet);
        context.setFirstInspectionResultPlanMap(new IdentityHashMap<>(firstInspectionResultPlanMap));

        context.setPreScheduledMachineBindingList(new ArrayList<>(preScheduledMachineBindingList));
        context.setPreScheduledMouldReleaseTimeMap(new LinkedHashMap<>(preScheduledMouldReleaseTimeMap));
        if (restoreSourceObjects) {
            for (Map.Entry<LhScheduleResult, LhScheduleResult> entry : scheduleResultStateMap.entrySet()) {
                BeanUtil.copyProperties(entry.getValue(), entry.getKey());
            }
        }
        context.setScheduleResultList(new ArrayList<LhScheduleResult>(scheduleResultList));
        if (restoreSourceObjects) {
            continuationFinishGroupStates.forEach((group, state) -> BeanUtil.copyProperties(copyFinishGroup(state), group));
            context.setContinuationRemainderFinishGroups(new ArrayList<>(continuationFinishGroups));
        } else {
            context.setContinuationRemainderFinishGroups(continuationFinishGroups.stream()
                    .map(group -> copyFinishGroup(continuationFinishGroupStates.get(group))).collect(Collectors.toList()));
        }
        context.setContinuationSurplusEndingAllocatedResults(copyIdentityResultSet(continuationFinishAllocatedResults));
        context.setScheduleResultSourceSkuMap(
                new IdentityHashMap<LhScheduleResult, SkuScheduleDTO>(scheduleResultSourceSkuMap));
        context.setCommittedWholeSingleControlPairMap(new IdentityHashMap<>(committedWholeSingleControlPairMap));
        context.setEndingDeferredFirstProductionDateMap(new LinkedHashMap<>(endingDeferredFirstProductionDateMap));
        context.setMachineAssignmentMap(copyResultListMap(machineAssignmentMap));
        restoreMachineScheduleMap(context);
        context.setMachineShiftCapacityMap(copyIntArrayMap(machineShiftCapacityMap));
        context.setShiftRuntimeStateMap(copyShiftRuntimeStateMap(shiftRuntimeStateMap));

        if (restoreSourceObjects) {
            for (Map.Entry<LhUnscheduledResult, LhUnscheduledResult> entry
                    : unscheduledResultStateMap.entrySet()) {
                BeanUtil.copyProperties(entry.getValue(), entry.getKey());
            }
        }
        context.setUnscheduledResultList(new ArrayList<LhUnscheduledResult>(unscheduledResultList));
        context.setUnscheduledResultRuntime(unscheduledResultRuntime.copy());
        context.setSkuProductionRemainingQtyMap(
                new LinkedHashMap<String, Integer>(skuProductionRemainingQtyMap));
        context.setSkuProductionTargetQtyMap(
                new LinkedHashMap<String, Integer>(skuProductionTargetQtyMap));
        context.setEmbryoStockConsumeLedgerMap(copyEmbryoLedgerMap(embryoStockConsumeLedgerMap));
        // 恢复候选试排前的胎胚配额和降模处理标记，确保失败候选对主排程上下文完全无副作用。
        context.setEmbryoStockSkuQuotaMap(
                new LinkedHashMap<String, Integer>(embryoStockSkuQuotaMap));
        context.setEmbryoStockHardTargetMaterialSet(
                new LinkedHashSet<String>(embryoStockHardTargetMaterialSet));
        context.setSharedEmbryoStockEndingIdentityMap(new LinkedHashMap<>(sharedEmbryoStockEndingIdentityMap));
        context.setEmbryoStockRealMachineSkuKeys(Objects.isNull(embryoStockRealMachineSkuKeys)
                ? null : new LinkedHashSet<>(embryoStockRealMachineSkuKeys));
        context.setEmbryoStockAllocationDeferred(embryoStockAllocationDeferred);
        context.setVirtualMachineCuringRemainingQtyMap(new LinkedHashMap<>(virtualMachineCuringRemainingQtyMap));
        context.setVirtualMachineCuringTargetQtyMap(new LinkedHashMap<>(virtualMachineCuringTargetQtyMap));
        context.setSharedEmbryoStockEndingInitialQuotaMap(new LinkedHashMap<>(sharedEmbryoStockEndingInitialQuotaMap));
        context.setSharedEmbryoStockEndingConsumedQtyMap(new LinkedHashMap<>(sharedEmbryoStockEndingConsumedQtyMap));
        context.setSharedEmbryoStockEndingMachineQuotaMap(new IdentityHashMap<>(sharedEmbryoStockEndingMachineQuotaMap));
        context.setSharedEmbryoStockEndingMachineReleaseMap(copyResultDateMap(sharedEmbryoStockEndingMachineReleaseMap));
        context.setSkuDecrementKeySet(new LinkedHashSet<String>(skuDecrementKeySet));
        context.setDecrementHandledSkuKeySet(
                new LinkedHashSet<String>(decrementHandledSkuKeySet));
        context.setDailyMouldChangeCountMap(copyIntArrayMap(dailyMouldChangeCountMap));
        context.setDailyFirstInspectionCountMap(copyIntArrayMap(dailyFirstInspectionCountMap));
        context.setShiftFirstInspectionCountMap(
                new LinkedHashMap<String, Integer>(shiftFirstInspectionCountMap));
        context.setGreenTireChangeoverShiftMap(copyIntegerSetMap(greenTireChangeoverShiftMap));
        context.setStructureScheduledMachineCodeMap(
                copyScheduledMachineMap(structureScheduledMachineCodeMap));
        context.setSkuScheduledMachineCodeMap(copyScheduledMachineMap(skuScheduledMachineCodeMap));
        context.setDailyMaintenanceCountMap(
                new LinkedHashMap<String, Integer>(dailyMaintenanceCountMap));
        context.setEquipmentPlanRuntimeState(equipmentPlanRuntimeState.copy());
        context.getRepairPlanRuntimeState().restoreTransferProgress(repairTransferEventMap);
        context.setCleaningPlanRuntimeState(cleaningPlanRuntimeState.copy());
        context.setContinuousProductionConsumptionMap(new IdentityHashMap<>(continuousProductionConsumptionMap));
        context.setMaintenanceDeferredPhysicalMachineCodeSet(new LinkedHashSet<>(maintenanceDeferredPhysicalMachineCodeSet));
        context.setMaintenancePreDecisionCompleted(maintenancePreDecisionCompleted);
        context.setContinuousDailyQuotaSynced(continuousDailyQuotaSynced);
        context.setDailyMaintenancePhysicalMachineSetMap(
                copyStringSetMap(dailyMaintenancePhysicalMachineSetMap));
        context.setMaintenanceResumeDelayLogKeySet(
                new LinkedHashSet<String>(maintenanceResumeDelayLogKeySet));
        context.setMouldChangeLimitBlockedReasonMap(
                new LinkedHashMap<String, String>(mouldChangeLimitBlockedReasonMap));
        context.setNewSpecMachineSelectionCountMap(
                new LinkedHashMap<String, Integer>(newSpecMachineSelectionCountMap));
        context.setNewSpecRealtimeSelectionOrderMap(
                copyRealtimeSelectionOrderMap(newSpecRealtimeSelectionOrderMap));
        context.setNewSpecRealtimeSnapshotResultSet(
                copyIdentityResultSet(newSpecRealtimeSnapshotResultSet));
        context.setPendingFormalNewSpecSkuCount(pendingFormalNewSpecSkuCount);
        context.setPendingTrialNewSpecSkuCount(pendingTrialNewSpecSkuCount);
        context.setPendingMassTrialNewSpecSkuCount(pendingMassTrialNewSpecSkuCount);
        context.setPendingSmallBatchNewSpecSkuCount(pendingSmallBatchNewSpecSkuCount);
        context.setNewSpecTypeRuleBlockedMap(
                new IdentityHashMap<SkuScheduleDTO, Boolean>(newSpecTypeRuleBlockedMap));
        context.setNewSpecEarlyProductionAllowedMap(
                new IdentityHashMap<SkuScheduleDTO, Boolean>(newSpecEarlyProductionAllowedMap));
        context.setNewSpecSingleControlStructureEndingLayerMap(
                new IdentityHashMap<SkuScheduleDTO, Boolean>(
                        newSpecSingleControlStructureEndingLayerMap));
        context.setSkuShiftFillOverQtyMap(
                new LinkedHashMap<String, Integer>(skuShiftFillOverQtyMap));
        context.setSpecifyMachineReservedMaterialMap(
                new LinkedHashMap<String, String>(specifyMachineReservedMaterialMap));
        context.setSpecifyMachineReservedSwitchStartTimeMap(
                new LinkedHashMap<String, Date>(specifyMachineReservedSwitchStartTimeMap));
        context.setCrossDayPreparationMouldChangeEventKeySet(
                new LinkedHashSet<String>(crossDayPreparationMouldChangeEventKeySet));
        context.setCapsuleRuntimeUsageMap(
                new LinkedHashMap<String, Integer>(capsuleRuntimeUsageMap));
        context.setCapsuleReplacementShiftKeySet(
                new LinkedHashSet<String>(capsuleReplacementShiftKeySet));
        context.setCapsuleThresholdHandledMachineSet(
                new LinkedHashSet<String>(capsuleThresholdHandledMachineSet));
        context.setCapsuleReplacementShiftCapacityLimitMap(
                new LinkedHashMap<String, Integer>(capsuleReplacementShiftCapacityLimitMap));
        context.setCapsuleReplacementTimeWindowMap(copyCapsuleReplacementTimeWindowMap(
                capsuleReplacementTimeWindowMap));
        context.setActiveEmbryoSkuMap(copyStringListMap(activeEmbryoSkuMap));
        context.setDynamicSingleEmbryoEndingMaterialSet(
                new LinkedHashSet<String>(dynamicSingleEmbryoEndingMaterialSet));
        context.setSpecialMaterialSubstitutionRecordList(
                new ArrayList<SpecialMaterialSubstitutionRecord>(substitutionRecordList));
        context.setSharedMouldSubstitutionRecordList(
                new ArrayList<SharedMouldSubstitutionRecord>(sharedMouldSubstitutionRecordList));
        context.setNewSpecSkuList(new ArrayList<SkuScheduleDTO>(newSpecSkuList));
        context.setNextShiftNewPlanCandidateList(new ArrayList<>(nextShiftNewPlanCandidateList));
        context.setNextShiftNewPlanPoolDateMap(new LinkedHashMap<>(nextShiftNewPlanPoolDateMap));
        context.setMouldChangePlanList(new ArrayList<LhMouldChangePlan>(mouldChangePlanList));
        context.setScheduleLogList(new ArrayList<LhScheduleProcessLog>(scheduleLogList));
        context.setDayTypeBlockReverseSelectionDirectiveList(copyBeanList(
                dayTypeBlockReverseSelectionDirectiveList,
                DayTypeBlockReverseSelectionDirective.class));
        context.setDayTypeBlockReverseSelectedSkuKeyMap(
                new LinkedHashMap<String, String>(dayTypeBlockReverseSelectedSkuKeyMap));
        context.setStructureSkuMap(copySkuListMap(structureSkuMap));
        context.setStructureEarlyProductionAdmissionMap(
                copyStructureAdmissionMap(structureEarlyProductionAdmissionMap));
        context.setSharedEmbryoEndingStaggerReleaseShiftIndexMap(
                new IdentityHashMap<LhScheduleResult, Integer>(
                        sharedEmbryoReleaseShiftIndexMap));
        context.setSharedEmbryoEndingStaggerReleaseShiftQtyMap(
                new IdentityHashMap<LhScheduleResult, Integer>(
                        sharedEmbryoReleaseShiftQtyMap));
        context.setSharedEmbryoEndingStaggerAllowedOverQtyMap(
                new IdentityHashMap<LhScheduleResult, Integer>(
                        sharedEmbryoAllowedOverQtyMap));
        context.setEndingFillAllowedOverQtyMap(
                new IdentityHashMap<LhScheduleResult, Integer>(
                        endingFillAllowedOverQtyMap));
        context.setEndingFillBeforeQtyMap(
                new IdentityHashMap<LhScheduleResult, Integer>(
                        endingFillBeforeQtyMap));
        Set<LhScheduleResult> restoredPrecisionResultSet =
                java.util.Collections.newSetFromMap(
                        new IdentityHashMap<LhScheduleResult, Boolean>());
        restoredPrecisionResultSet.addAll(precisionPreInsertResultSet);
        context.setPrecisionPreInsertResultSet(restoredPrecisionResultSet);
        context.setPrecisionPreInsertInspectionTimeMap(
                new IdentityHashMap<LhScheduleResult, Date>(
                        precisionPreInsertInspectionTimeMap));
        context.setPrecisionPreInsertMouldChangeTimeMap(
                new IdentityHashMap<LhScheduleResult, Date>(
                        precisionPreInsertMouldChangeTimeMap));
        context.setPrecisionPreInsertInspectionShiftIndexMap(
                new IdentityHashMap<LhScheduleResult, Integer>(
                        precisionPreInsertInspectionShiftIndexMap));
        context.setHistoricalStructureHandoffStateMap(new IdentityHashMap<>(historicalStructureHandoffStateMap));
        earlyProductionLogStates.forEach((entry, state) -> BeanUtil.copyProperties(state, entry));
        earlyProductionLogEntries.forEach(EarlyProductionDecisionLogCollector::restoreEntries);
        context.setEarlyProductionDecisionLogCollectorMap(new LinkedHashMap<>(earlyProductionDecisionLogCollectorMap));
        context.setEarlyProductionRuntimePlanMap(
                copyEarlyProductionRuntimePlanMap(earlyProductionRuntimePlanMap));
        context.setCurrentScheduleDate(currentScheduleDate);
        context.setSpecialMaterialSpecifiedMachineCode(
                specialMaterialSpecifiedMachineCode);
        context.setSpecialMaterialSpecifiedSkuKey(specialMaterialSpecifiedSkuKey);
        context.setSpecialMaterialEarliestSwitchTime(
                specialMaterialEarliestSwitchTime);
        context.setScheduleSubstitutionDirective(
                Objects.isNull(scheduleSubstitutionDirective)
                        ? null : copyBean(scheduleSubstitutionDirective,
                        ScheduleSubstitutionDirective.class));

        if (restoreSourceObjects) {
            for (Map.Entry<SkuScheduleDTO, SkuScheduleDTO> entry : skuStateMap.entrySet()) {
                BeanUtil.copyProperties(entry.getValue(), entry.getKey());
                // 原位恢复账本，快照外仍引用该账本的续作SKU也能看到回滚后的消费值。
                Map<LocalDate, SkuDailyPlanQuotaDTO> originalQuotaMap =
                        skuDailyQuotaReferenceMap.get(entry.getKey());
                if (Objects.nonNull(originalQuotaMap)) {
                    originalQuotaMap.clear();
                    originalQuotaMap.putAll(copyDailyQuotaMap(skuDailyQuotaMap.get(entry.getKey())));
                }
                entry.getKey().setDailyPlanQuotaMap(originalQuotaMap);
            }
        }

        /*
         * 模具资源上下文没有暴露可变 Map，恢复排程结果和机台状态后按相同基础数据重新构建，
         * 可确保机台旧模具绑定、特殊材料预占模具和全局占用集合同时回到候选前口径。
         */
        context.setPreviousAlternateReleasedMachineTimeMap(new LinkedHashMap<>(previousAlternateReleasedMachineTimeMap));
        context.setPreviousAlternateReleasedSkuKeyMap(new LinkedHashMap<>(previousAlternateReleasedSkuKeyMap));
        context.setPreviousAlternateCandidateAvailableTimeMap(new IdentityHashMap<>(previousAlternateCandidateAvailableTimeMap));
        context.setPreviousAlternateResultPlanMap(new IdentityHashMap<>(previousAlternateResultPlanMap));
        context.setPreviousAlternateBeforeMaterialSourceMap(
                new IdentityHashMap<>(previousAlternateBeforeMaterialSourceMap));
        context.setPreviousAlternateAdditionalMachineDemandMap(new LinkedHashMap<>(previousAlternateAdditionalMachineDemandMap));
        context.setContinuationSurplusEndingSnapshotMap(copyContinuationFinishSnapshots(continuationFinishSnapshots));
        context.setMouldResourceContext(MouldResourceContext.from(context));
    }

    private void restoreMachineScheduleMap(LhScheduleContext context) {
        Map<String, MachineScheduleDTO> currentMap = context.getMachineScheduleMap();
        currentMap.clear();
        for (Map.Entry<String, MachineScheduleDTO> entry : machineScheduleStateMap.entrySet()) {
            currentMap.put(entry.getKey(), copyMachine(entry.getValue()));
        }
    }

    private static Map<String, MachineScheduleDTO> copyMachineScheduleMap(
            Map<String, MachineScheduleDTO> sourceMap) {
        Map<String, MachineScheduleDTO> targetMap =
                new LinkedHashMap<String, MachineScheduleDTO>(Math.max(16, sourceMap.size() * 2));
        for (Map.Entry<String, MachineScheduleDTO> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), copyMachine(entry.getValue()));
        }
        return targetMap;
    }

    private static MachineScheduleDTO copyMachine(MachineScheduleDTO source) {
        MachineScheduleDTO target = copyBean(source, MachineScheduleDTO.class);
        target.setShiftRemainingCapacity(source.getShiftRemainingCapacity() == null
                ? null : source.getShiftRemainingCapacity().clone());
        target.setShiftAvailable(source.getShiftAvailable() == null
                ? null : source.getShiftAvailable().clone());
        target.setCleaningWindowList(CleaningPlanRuntimeState.copyWindows(source.getCleaningWindowList()));
        target.setMaintenanceWindowList(source.getMaintenanceWindowList() == null
                ? new ArrayList<>(0) : source.getMaintenanceWindowList().stream()
                .map(window -> window == null ? null : window.copy()).collect(Collectors.toList()));
        target.setMouldChangeTasks(source.getMouldChangeTasks() == null
                ? new ArrayList<>(0) : new ArrayList<>(source.getMouldChangeTasks()));
        return target;
    }

    /**
     * 复制收尾组的可变输入与输出，结果和来源仍引用由通用快照恢复的原对象。
     *
     * @param source 原收尾组
     * @return 与原组隔离的内容快照
     */
    private static ContinuationRemainderFinishGroup copyFinishGroup(ContinuationRemainderFinishGroup source) {
        ContinuationRemainderFinishGroup copied = copyBean(source, ContinuationRemainderFinishGroup.class);
        copied.setOriginalResults(source.getOriginalResults() == null ? null : new ArrayList<>(source.getOriginalResults()));
        copied.setParticipatingResults(source.getParticipatingResults() == null ? null : new ArrayList<>(source.getParticipatingResults()));
        copied.setStopHoldResults(source.getStopHoldResults() == null ? null : new ArrayList<>(source.getStopHoldResults()));
        if (Objects.nonNull(source.getMachineProfiles())) {
            copied.setMachineProfiles(source.getMachineProfiles().stream()
                    .map(ContinuationEndingMachineProfile::copyForCalculation).collect(Collectors.toList()));
        }
        if (Objects.nonNull(source.getNextActions())) {
            Map<String, Date> actions = new LinkedHashMap<>();
            source.getNextActions().forEach((machine, time) -> actions.put(machine, copyDate(time)));
            copied.setNextActions(actions);
        }
        if (Objects.nonNull(source.getOriginalMachineFinishPlan())) {
            ContinuationFinishScheduleResult plan = copyBean(source.getOriginalMachineFinishPlan(), ContinuationFinishScheduleResult.class);
            if (Objects.nonNull(source.getOriginalMachineFinishPlan().getMachinePlans())) {
                plan.setMachinePlans(source.getOriginalMachineFinishPlan().getMachinePlans().stream().map(original -> {
                    ContinuationMachineFinishPlan item = copyBean(original, ContinuationMachineFinishPlan.class);
                    item.setFinishTime(copyDate(original.getFinishTime()));
                    item.setOfflineTime(copyDate(original.getOfflineTime()));
                    item.setFinishDeadline(copyDate(original.getFinishDeadline()));
                    return item;
                }).collect(Collectors.toList()));
            }
            copied.setOriginalMachineFinishPlan(plan);
        }
        return copied;
    }

    /** 精度强制释放会更新收尾事实，捕获及恢复都复制数组与日期，防止嵌套预演污染原快照。 */
    private static Map<LhScheduleResult, ContinuationEndingAllocationSnapshot> copyContinuationFinishSnapshots(
            Map<LhScheduleResult, ContinuationEndingAllocationSnapshot> sources) {
        Map<LhScheduleResult, ContinuationEndingAllocationSnapshot> copies = new IdentityHashMap<>();
        sources.forEach((result, source) -> {
            ContinuationEndingAllocationSnapshot copy = copyBean(source, ContinuationEndingAllocationSnapshot.class);
            if (Objects.nonNull(copy)) {
                copy.setShiftQuantities(source.getShiftQuantities() == null ? null : source.getShiftQuantities().clone());
                copy.setProductionStartTime(copyDate(source.getProductionStartTime()));
                copy.setProductionEndTime(copyDate(source.getProductionEndTime()));
                copy.setReleaseTime(copyDate(source.getReleaseTime()));
                copy.setFinishDeadline(copyDate(source.getFinishDeadline()));
                copy.setPredictedChangeTime(copyDate(source.getPredictedChangeTime()));
            }
            copies.put(result, copy);
        });
        return copies;
    }

    /**
     * 复制公共胎胚释放投影中的日期内容，避免与节点方案日期共享可变对象。
     *
     * @param source 原结果到释放日期映射
     * @return 保持结果身份且日期隔离的映射
     */
    private static Map<LhScheduleResult, Date> copyResultDateMap(Map<LhScheduleResult, Date> source) {
        Map<LhScheduleResult, Date> copied = new IdentityHashMap<>();
        source.forEach((result, date) -> copied.put(result, copyDate(date)));
        return copied;
    }

    /** @param source 来源日期 @return 独立日期副本 */
    private static Date copyDate(Date source) {
        return Objects.isNull(source) ? null : new Date(source.getTime());
    }

    private static Map<String, List<LhScheduleResult>> copyResultListMap(
            Map<String, List<LhScheduleResult>> sourceMap) {
        Map<String, List<LhScheduleResult>> targetMap =
                new LinkedHashMap<String, List<LhScheduleResult>>(Math.max(16, sourceMap.size() * 2));
        for (Map.Entry<String, List<LhScheduleResult>> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue() == null
                    ? new ArrayList<LhScheduleResult>(0)
                    : new ArrayList<LhScheduleResult>(entry.getValue()));
        }
        return targetMap;
    }

    private static Map<String, int[]> copyIntArrayMap(Map<String, int[]> sourceMap) {
        Map<String, int[]> targetMap =
                new LinkedHashMap<String, int[]>(Math.max(16, sourceMap.size() * 2));
        for (Map.Entry<String, int[]> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
        }
        return targetMap;
    }

    private static Map<Integer, ShiftRuntimeState> copyShiftRuntimeStateMap(
            Map<Integer, ShiftRuntimeState> sourceMap) {
        Map<Integer, ShiftRuntimeState> targetMap =
                new LinkedHashMap<Integer, ShiftRuntimeState>(
                        Math.max(8, sourceMap == null ? 0 : sourceMap.size() * 2));
        if (sourceMap == null || sourceMap.isEmpty()) {
            return targetMap;
        }
        for (Map.Entry<Integer, ShiftRuntimeState> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), copyBean(entry.getValue(), ShiftRuntimeState.class));
        }
        return targetMap;
    }

    private static Map<SkuScheduleDTO, Map<Integer, Integer>>
    copyRealtimeSelectionOrderMap(
            Map<SkuScheduleDTO, Map<Integer, Integer>> sourceMap) {
        Map<SkuScheduleDTO, Map<Integer, Integer>> targetMap =
                new IdentityHashMap<SkuScheduleDTO, Map<Integer, Integer>>(
                        Math.max(4, sourceMap == null ? 0 : sourceMap.size() * 2));
        if (sourceMap == null || sourceMap.isEmpty()) {
            return targetMap;
        }
        for (Map.Entry<SkuScheduleDTO, Map<Integer, Integer>> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue() == null
                    ? new LinkedHashMap<Integer, Integer>(0)
                    : new LinkedHashMap<Integer, Integer>(entry.getValue()));
        }
        return targetMap;
    }

    private static Set<LhScheduleResult> copyIdentityResultSet(
            Set<LhScheduleResult> sourceSet) {
        Set<LhScheduleResult> targetSet = java.util.Collections.newSetFromMap(
                new IdentityHashMap<LhScheduleResult, Boolean>());
        if (sourceSet != null) {
            targetSet.addAll(sourceSet);
        }
        return targetSet;
    }

    private static Map<String, List<SkuScheduleDTO>> copySkuListMap(
            Map<String, List<SkuScheduleDTO>> sourceMap) {
        Map<String, List<SkuScheduleDTO>> targetMap =
                new LinkedHashMap<String, List<SkuScheduleDTO>>(
                        Math.max(8, sourceMap == null ? 0 : sourceMap.size() * 2));
        if (sourceMap == null || sourceMap.isEmpty()) {
            return targetMap;
        }
        for (Map.Entry<String, List<SkuScheduleDTO>> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue() == null
                    ? new ArrayList<SkuScheduleDTO>(0)
                    : new ArrayList<SkuScheduleDTO>(entry.getValue()));
        }
        return targetMap;
    }

    private static <T> List<T> copyBeanList(List<T> sourceList,
                                             Class<T> targetClass) {
        List<T> targetList = new ArrayList<T>(
                sourceList == null ? 0 : sourceList.size());
        if (sourceList == null || sourceList.isEmpty()) {
            return targetList;
        }
        for (T source : sourceList) {
            targetList.add(copyBean(source, targetClass));
        }
        return targetList;
    }

    private static Map<LocalDate, Map<String, StructureEarlyProductionAdmission>>
    copyStructureAdmissionMap(
            Map<LocalDate, Map<String, StructureEarlyProductionAdmission>> sourceMap) {
        Map<LocalDate, Map<String, StructureEarlyProductionAdmission>> targetMap =
                new LinkedHashMap<LocalDate, Map<String, StructureEarlyProductionAdmission>>(4);
        if (sourceMap == null || sourceMap.isEmpty()) {
            return targetMap;
        }
        for (Map.Entry<LocalDate, Map<String, StructureEarlyProductionAdmission>> dateEntry
                : sourceMap.entrySet()) {
            Map<String, StructureEarlyProductionAdmission> structureMap =
                    new LinkedHashMap<String, StructureEarlyProductionAdmission>(8);
            if (dateEntry.getValue() != null) {
                for (Map.Entry<String, StructureEarlyProductionAdmission> structureEntry
                        : dateEntry.getValue().entrySet()) {
                    StructureEarlyProductionAdmission copiedAdmission =
                            copyBean(structureEntry.getValue(),
                                    StructureEarlyProductionAdmission.class);
                    copiedAdmission.setRawScheduledPhysicalMachineCodes(
                            new LinkedHashSet<String>(structureEntry.getValue()
                                    .getRawScheduledPhysicalMachineCodes()));
                    copiedAdmission.setScheduledPhysicalMachineCodes(
                            new LinkedHashSet<String>(structureEntry.getValue()
                                    .getScheduledPhysicalMachineCodes()));
                    structureMap.put(structureEntry.getKey(), copiedAdmission);
                }
            }
            targetMap.put(dateEntry.getKey(), structureMap);
        }
        return targetMap;
    }

    private static Map<String, EmbryoStockConsumeLedger> copyEmbryoLedgerMap(
            Map<String, EmbryoStockConsumeLedger> sourceMap) {
        Map<String, EmbryoStockConsumeLedger> targetMap =
                new LinkedHashMap<String, EmbryoStockConsumeLedger>(
                        Math.max(16, sourceMap.size() * 2));
        for (Map.Entry<String, EmbryoStockConsumeLedger> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), copyBean(entry.getValue(), EmbryoStockConsumeLedger.class));
        }
        return targetMap;
    }

    private static Map<String, Set<Integer>> copyIntegerSetMap(Map<String, Set<Integer>> sourceMap) {
        Map<String, Set<Integer>> targetMap =
                new LinkedHashMap<String, Set<Integer>>(Math.max(16, sourceMap.size() * 2));
        for (Map.Entry<String, Set<Integer>> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue() == null
                    ? new LinkedHashSet<Integer>(0) : new LinkedHashSet<Integer>(entry.getValue()));
        }
        return targetMap;
    }

    private static Map<String, Set<String>> copyStringSetMap(Map<String, Set<String>> sourceMap) {
        Map<String, Set<String>> targetMap =
                new LinkedHashMap<String, Set<String>>(Math.max(16, sourceMap.size() * 2));
        for (Map.Entry<String, Set<String>> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue() == null
                    ? new LinkedHashSet<String>(0) : new LinkedHashSet<String>(entry.getValue()));
        }
        return targetMap;
    }

    private static Map<String, List<String>> copyStringListMap(Map<String, List<String>> sourceMap) {
        Map<String, List<String>> targetMap =
                new LinkedHashMap<String, List<String>>(Math.max(16, sourceMap.size() * 2));
        for (Map.Entry<String, List<String>> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue() == null
                    ? new ArrayList<String>(0) : new ArrayList<String>(entry.getValue()));
        }
        return targetMap;
    }

    private static Map<LocalDate, Map<String, Set<String>>> copyScheduledMachineMap(
            Map<LocalDate, Map<String, Set<String>>> sourceMap) {
        Map<LocalDate, Map<String, Set<String>>> targetMap =
                new LinkedHashMap<LocalDate, Map<String, Set<String>>>(
                        Math.max(8, sourceMap.size() * 2));
        for (Map.Entry<LocalDate, Map<String, Set<String>>> dateEntry : sourceMap.entrySet()) {
            targetMap.put(dateEntry.getKey(), copyStringSetMap(dateEntry.getValue()));
        }
        return targetMap;
    }

    private static Map<LocalDate, SkuDailyPlanQuotaDTO> copyDailyQuotaMap(
            Map<LocalDate, SkuDailyPlanQuotaDTO> sourceMap) {
        if (sourceMap == null) {
            return new LinkedHashMap<LocalDate, SkuDailyPlanQuotaDTO>(0);
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> targetMap =
                new LinkedHashMap<LocalDate, SkuDailyPlanQuotaDTO>(Math.max(8, sourceMap.size() * 2));
        for (Map.Entry<LocalDate, SkuDailyPlanQuotaDTO> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), copyBean(entry.getValue(), SkuDailyPlanQuotaDTO.class));
        }
        return targetMap;
    }

    private static Map<SkuScheduleDTO, EarlyProductionRuntimePlan>
    copyEarlyProductionRuntimePlanMap(
            Map<SkuScheduleDTO, EarlyProductionRuntimePlan> sourceMap) {
        Map<SkuScheduleDTO, EarlyProductionRuntimePlan> targetMap =
                new IdentityHashMap<SkuScheduleDTO, EarlyProductionRuntimePlan>(
                        Math.max(4, sourceMap == null ? 0 : sourceMap.size() * 2));
        if (sourceMap == null || sourceMap.isEmpty()) {
            return targetMap;
        }
        for (Map.Entry<SkuScheduleDTO, EarlyProductionRuntimePlan> entry
                : sourceMap.entrySet()) {
            if (entry.getValue() == null) {
                targetMap.put(entry.getKey(), null);
                continue;
            }
            EarlyProductionRuntimePlan copiedPlan =
                    copyBean(entry.getValue(), EarlyProductionRuntimePlan.class);
            copiedPlan.setShiftedDailyPlanQuotaMap(
                    copyDailyQuotaMap(entry.getValue().getShiftedDailyPlanQuotaMap()));
            if (entry.getValue().getDecision() != null) {
                EarlyProductionDecision copiedDecision =
                        copyBean(entry.getValue().getDecision(), EarlyProductionDecision.class);
                copiedDecision.setStructurePlanMachineCounts(
                        new ArrayList<Integer>(
                                entry.getValue().getDecision().getStructurePlanMachineCounts()));
                copiedPlan.setDecision(copiedDecision);
            }
            if (entry.getValue().getDecisionLogEntry() != null) {
                // 日志字段由身份快照单独恢复；运行视图必须继续指向采集器中的同一明细。
                copiedPlan.setDecisionLogEntry(entry.getValue().getDecisionLogEntry());
            }
            targetMap.put(entry.getKey(), copiedPlan);
        }
        return targetMap;
    }

    /**
     * 复制换胶囊时间窗口，保证候选置换回滚不会残留机台不可生产时间。
     *
     * @param sourceMap 原始时间窗口
     * @return 独立时间窗口副本
     */
    private static Map<String, CapsuleReplacementTimeWindowDTO> copyCapsuleReplacementTimeWindowMap(
            Map<String, CapsuleReplacementTimeWindowDTO> sourceMap) {
        if (sourceMap == null) {
            return new LinkedHashMap<String, CapsuleReplacementTimeWindowDTO>(0);
        }
        Map<String, CapsuleReplacementTimeWindowDTO> targetMap =
                new LinkedHashMap<String, CapsuleReplacementTimeWindowDTO>(Math.max(4, sourceMap.size() * 2));
        for (Map.Entry<String, CapsuleReplacementTimeWindowDTO> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), copyBean(entry.getValue(), CapsuleReplacementTimeWindowDTO.class));
        }
        return targetMap;
    }

    private static <T> T copyBean(T source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T target;
        try {
            target = targetClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException("排程联动置换状态快照创建失败: "
                    + targetClass.getSimpleName(), ex);
        }
        BeanUtil.copyProperties(source, target);
        return target;
    }
}
