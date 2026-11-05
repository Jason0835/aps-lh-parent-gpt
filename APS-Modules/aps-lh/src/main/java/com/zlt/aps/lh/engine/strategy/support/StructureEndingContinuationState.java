package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 结构收尾续作的冻结资格及三步执行事实，不持有独立数量账本。
 *
 * @author Codex
 * @date 2026/10/29
 */
@Data
public class StructureEndingContinuationState {
    /**
     * 原需求身份，新增副本仍消费同一物料状态账本。
     */
    private SkuScheduleDTO sourceSku;
    /**
     * 前结构在有效批次内的最晚收尾时间。
     */
    private Date endingTime;
    /**
     * 正式分量前冻结的合法总目标，后续候选不得重新初始化或扩大。
     */
    private Integer legalTargetQty;
    /**
     * 原机第一步生产截止，中班为十九点，其他班为班末。
     */
    private Date firstDeadline;
    /**
     * 新增及原机回补的次日对应班次截止。
     */
    private Date finalDeadline;
    /**
     * 按下机优先级由高到低保存的原结果身份，零量清理后仍可定位原机。
     */
    private List<LhScheduleResult> originalResults = new ArrayList<>();
    /**
     * 原机进入本规则前的只读生产参数，不能用后续机台状态反推模具和产能。
     */
    private List<LhScheduleResult> originalSeeds = new ArrayList<>();
    /**
     * 第一步冻结的物理机台集合，新增阶段不得使用这些机台。
     */
    private Set<String> originalPhysicalMachines = new LinkedHashSet<>();
    /**
     * 普通增机最早生效日，空值表示没有普通增机资格。
     */
    private LocalDate firstExpansionDate;
    /**
     * 截短原机前确认的普通增机台数上限。
     */
    private int ordinaryAdditionalMachines;
    /**
     * 截短前按业务日冻结的普通增机上限，原机技术释放不能增加该上限。
     */
    private Map<LocalDate, Integer> ordinaryAdditionalByDate = new LinkedHashMap<>();
    /**
     * 截短原机前的原机数量，不能随第一步技术释放减少。
     */
    private int originalMachineCount;
    /**
     * 既有延期增机资格，仅用于保持其首个实际增机日限制。
     */
    private boolean deferredExpansion;
    /**
     * 有效历史指定关系，按原计划身份保存；不向普通机台传播资格。
     */
    private Set<Long> historicalPlanIds = new LinkedHashSet<>();
    /**
     * 已正量提交的历史关系，保护门禁直接读取，避免候选循环扫描全量结果。
     */
    private Set<Long> committedHistoricalPlanIds = new LinkedHashSet<>();
    /**
     * 原特殊历史增机资格的只读预判结果，普通目标已满足时仍保留精确指定关系。
     */
    private Map<Long, PreviousAlternateAdditionalMachineDemand> historicalAdditionalDemands = new LinkedHashMap<>();
    /**
     * 原dayN降模已经确定的班次释放，区分业务退出与第一步新增的技术释放投影。
     */
    private Map<String, Integer> originalReleaseShiftBounds = new LinkedHashMap<>();
    /**
     * 第一步原机累计实排量，最终排序同时检查回补增量。
     */
    private Map<String, Integer> originalCommittedQuantities = new LinkedHashMap<>();
    /**
     * 已提交的新增物理机台，历史指定与普通增机共用去重集合。
     */
    private Set<String> addedPhysicalMachines = new LinkedHashSet<>();
    /**
     * 已提交运行侧，精确历史单控补齐另一侧不能被物理机台去重误挡。
     */
    private Set<String> addedRuntimeMachines = new LinkedHashSet<>();
    /**
     * 接管前已经属于原余量收尾，不能把普通dayN降模改成历史余量交接身份。
     */
    private boolean originalRemainderEnding;
    /**
     * 第一步已完成，重复入口不得重新分配原机前缀。
     */
    private boolean firstAllocated;
    /**
     * 第二、三步已完成，后续普通入口只保留未排量。
     */
    private boolean completed;

    /**
     * 复制可变阶段状态，原需求、结果和只读种子继续保持身份。
     *
     * @return 可供候选或整阶段回滚的独立容器副本
     */
    public StructureEndingContinuationState copy() {
        StructureEndingContinuationState copy = new StructureEndingContinuationState();
        copy.sourceSku = sourceSku;
        copy.endingTime = endingTime;
        copy.legalTargetQty = legalTargetQty;
        copy.firstDeadline = firstDeadline;
        copy.finalDeadline = finalDeadline;
        copy.originalResults = new ArrayList<>(originalResults);
        copy.originalSeeds = new ArrayList<>(originalSeeds);
        copy.originalPhysicalMachines = new LinkedHashSet<>(originalPhysicalMachines);
        copy.firstExpansionDate = firstExpansionDate;
        copy.ordinaryAdditionalMachines = ordinaryAdditionalMachines;
        copy.ordinaryAdditionalByDate = new LinkedHashMap<>(ordinaryAdditionalByDate);
        copy.originalMachineCount = originalMachineCount;
        copy.deferredExpansion = deferredExpansion;
        copy.historicalPlanIds = new LinkedHashSet<>(historicalPlanIds);
        copy.committedHistoricalPlanIds = new LinkedHashSet<>(committedHistoricalPlanIds);
        copy.historicalAdditionalDemands = new LinkedHashMap<>(historicalAdditionalDemands);
        copy.originalReleaseShiftBounds = new LinkedHashMap<>(originalReleaseShiftBounds);
        copy.originalCommittedQuantities = new LinkedHashMap<>(originalCommittedQuantities);
        copy.addedPhysicalMachines = new LinkedHashSet<>(addedPhysicalMachines);
        copy.addedRuntimeMachines = new LinkedHashSet<>(addedRuntimeMachines);
        copy.originalRemainderEnding = originalRemainderEnding;
        copy.firstAllocated = firstAllocated;
        copy.completed = completed;
        return copy;
    }
}
