package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.algorithm.Cd15BigRollAgingAllocator;
import com.zlt.aps.cd15.engine.algorithm.Cd15DemandCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15MachineCandidateResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15ResourceSnapshotBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15RollingPrefixResourceDeductor;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateSorter;
import com.zlt.aps.cd15.engine.algorithm.Cd15ShiftDisplayHelper;
import com.zlt.aps.cd15.engine.algorithm.Cd15SplitCutGroupBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15StorageLaneAllocator;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocationItem;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleRequest;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15SplitCutGroup;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocationResult;
import com.zlt.aps.cd15.engine.service.Cd15MultiShiftScheduleExecutor;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleProgressListener;
import com.zlt.aps.cd15.engine.service.Cd15SingleShiftScheduleExecutor;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** CD15 多班滚动试排执行器。 */
@Service
@RequiredArgsConstructor
public class Cd15MultiShiftScheduleExecutorImpl implements Cd15MultiShiftScheduleExecutor {

    private static final String DATA_MISSING = "DATA_MISSING";
    private static final String SPLIT = "SPLIT";

    private final Cd15ResourceSnapshotBuilder resourceSnapshotBuilder;
    private final Cd15RollingPrefixResourceDeductor prefixResourceDeductor;
    private final Cd15ScheduleCandidateBuilder candidateBuilder;
    private final Cd15ScheduleCandidateSorter candidateSorter;
    private final Cd15SplitCutGroupBuilder splitCutGroupBuilder;
    private final Cd15MachineCandidateResolver machineCandidateResolver;
    private final Cd15BigRollAgingAllocator bigRollAgingAllocator;
    private final Cd15StorageLaneAllocator storageLaneAllocator;
    private final Cd15DemandCalculator demandCalculator;
    private final Cd15SingleShiftScheduleExecutor singleShiftScheduleExecutor;

    @Override
    public Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input) {
        return this.execute(input, Cd15ScheduleProgressListener.NO_OP);
    }

    @Override
    public Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input, Cd15ScheduleProgressListener listener) {
        return this.execute(input, 1, listener);
    }

    @Override
    public Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input, int startClassIndex) {
        return this.execute(input, startClassIndex, Cd15ScheduleProgressListener.NO_OP);
    }

    @Override
    public Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input, int startClassIndex,
                                                Cd15ScheduleProgressListener listener) {
        Cd15ScheduleProgressListener progressListener = listener == null
                ? Cd15ScheduleProgressListener.NO_OP : listener;
        Cd15RollingResourceSnapshot snapshot = resourceSnapshotBuilder.build(input);
        prefixResourceDeductor.deductPrefixResources(input, snapshot);
        List<Cd15ScheduleCandidate> candidates = new ArrayList<>(
                candidateSorter.sort(candidateBuilder.build(input, snapshot)).stream()
                        .filter(candidate -> candidate.getClassIndex() >= startClassIndex)
                        .collect(Collectors.toList()));
        List<Cd15ScheduleResultDraft> scheduledDrafts = new ArrayList<>();
        List<Cd15SingleShiftScheduleResult> unscheduledResults = new ArrayList<>();
        AtomicInteger produceOrder = new AtomicInteger(1);
        int currentClassIndex = -1;
        while (!candidates.isEmpty()) {
            Cd15ScheduleCandidate candidate = candidates.remove(0);
            if (candidate.getClassIndex() != currentClassIndex) {
                currentClassIndex = candidate.getClassIndex();
                progressListener.onProgress(40, "SCHEDULE_TRIAL", this.shiftStageName(candidate, "班次开始"));
            }
            Optional<Cd15SplitCutGroup> splitGroup = splitCutGroupBuilder.findSplitGroup(
                    candidate, candidates, input == null ? Collections.emptyMap() : input.getAngleWidthMaxByAngle());
            if (splitGroup.isPresent()) {
                candidates.remove(splitGroup.get().getSecondCandidate());
                this.scheduleSplitGroup(input, snapshot, splitGroup.get(), produceOrder,
                        scheduledDrafts, unscheduledResults);
            } else {
                this.scheduleCandidate(input, snapshot, candidate, produceOrder,
                        scheduledDrafts, unscheduledResults);
            }
        }
        return Cd15MultiShiftScheduleResult.builder()
                .scheduledDrafts(scheduledDrafts)
                .unscheduledResults(unscheduledResults)
                .build();
    }

    private void scheduleSplitGroup(Cd15AutoScheduleInput input,
                                    Cd15RollingResourceSnapshot snapshot,
                                    Cd15SplitCutGroup splitGroup,
                                    AtomicInteger produceOrder,
                                    List<Cd15ScheduleResultDraft> scheduledDrafts,
                                    List<Cd15SingleShiftScheduleResult> unscheduledResults) {
        Optional<Cd15MachineInfo> machine = machineCandidateResolver.resolve(input, splitGroup);
        if (!machine.isPresent()) {
            this.addSplitUnscheduled(unscheduledResults, splitGroup,
                    machineCandidateResolver.resolveFailureReason(input, splitGroup), "未找到满足分裁组合硬约束的可用机台");
            return;
        }
        if (this.hasAgingDataMissing(snapshot, splitGroup.getFirstCandidate().getBigRollCode())) {
            this.addSplitUnscheduled(unscheduledResults, splitGroup, DATA_MISSING, "GDYY大卷入库时间或单卷米数缺失");
            return;
        }
        int sameProduceOrder = produceOrder.getAndIncrement();
        String sameOrderNo = this.orderNo(splitGroup.getFirstCandidate(), sameProduceOrder);
        String sameGroupNo = sameOrderNo;
        GdyyStock trialStock = this.trialStock(splitGroup.getFirstCandidate().getBigRollCode());
        Cd15SingleShiftScheduleResult firstResult = this.executeCandidate(snapshot, splitGroup.getFirstCandidate(), machine.get(),
                trialStock, sameOrderNo, sameGroupNo, sameProduceOrder);
        Cd15SingleShiftScheduleResult secondResult = this.executeCandidate(snapshot, splitGroup.getSecondCandidate(), machine.get(),
                trialStock, sameOrderNo, sameGroupNo, sameProduceOrder);
        if (!firstResult.isScheduled() || !secondResult.isScheduled()) {
            unscheduledResults.add(firstResult.isScheduled() ? secondResult : firstResult);
            return;
        }
        Cd15StorageLaneAllocationResult firstLaneAllocation = this.allocateStorageLane(snapshot, firstResult.getDraft());
        if (!firstLaneAllocation.isSuccess()) {
            this.addSplitUnscheduled(unscheduledResults, splitGroup, firstLaneAllocation.getFailureReason(),
                    "STORAGE_LANE_LIMIT for first split steel strip");
            return;
        }
        Cd15StorageLaneAllocationResult secondLaneAllocation = this.allocateStorageLane(
                firstLaneAllocation.getLanes(), secondResult.getDraft());
        if (!secondLaneAllocation.isSuccess()) {
            this.addSplitUnscheduled(unscheduledResults, splitGroup, secondLaneAllocation.getFailureReason(),
                    "STORAGE_LANE_LIMIT for second split steel strip");
            return;
        }
        BigDecimal totalBigRollConsume = this.value(firstResult.getDraft().getBigRollConsumeMeters())
                .add(this.value(secondResult.getDraft().getBigRollConsumeMeters()));
        Cd15BigRollAgingAllocation allocation = this.allocateBigRoll(snapshot,
                splitGroup.getFirstCandidate().getBigRollCode(), totalBigRollConsume,
                this.shiftStartTime(input, splitGroup.getFirstCandidate().getClassIndex()));
        if (!allocation.isSuccess()) {
            this.addSplitUnscheduled(unscheduledResults, splitGroup, allocation.getFailureReason(),
                    "GDYY大卷成熟库存不足，无法满足分裁组合");
            return;
        }
        String bigRollBarcode = this.firstBarcode(allocation);
        this.markSplitDraft(firstResult.getDraft(), sameOrderNo, sameGroupNo, sameProduceOrder, bigRollBarcode);
        this.markSplitDraft(secondResult.getDraft(), sameOrderNo, sameGroupNo, sameProduceOrder, bigRollBarcode);
        this.applyLaneAllocation(firstResult.getDraft(), firstLaneAllocation);
        this.applyLaneAllocation(secondResult.getDraft(), secondLaneAllocation);
        snapshot.setStorageLanes(secondLaneAllocation.getLanes());
        this.applySourceTrace(input, firstResult.getDraft());
        this.applySourceTrace(input, secondResult.getDraft());
        scheduledDrafts.add(firstResult.getDraft());
        scheduledDrafts.add(secondResult.getDraft());
        this.deductCandidateStock(snapshot, splitGroup.getFirstCandidate(), firstResult.getDraft());
        this.deductCandidateStock(snapshot, splitGroup.getSecondCandidate(), secondResult.getDraft());
    }

    private void scheduleCandidate(Cd15AutoScheduleInput input,
                                   Cd15RollingResourceSnapshot snapshot,
                                   Cd15ScheduleCandidate candidate,
                                   AtomicInteger produceOrder,
                                   List<Cd15ScheduleResultDraft> scheduledDrafts,
                                   List<Cd15SingleShiftScheduleResult> unscheduledResults) {
        Optional<Cd15MachineInfo> machine = machineCandidateResolver.resolve(input, candidate,
                candidate.getMaterial().getCraftWidth());
        if (!machine.isPresent()) {
            unscheduledResults.add(this.unscheduled(candidate,
                    machineCandidateResolver.resolveFailureReason(input, candidate, candidate.getMaterial().getCraftWidth()),
                    "未找到满足机台硬约束的可用机台"));
            return;
        }
        if (this.hasAgingDataMissing(snapshot, candidate.getBigRollCode())) {
            unscheduledResults.add(this.unscheduled(candidate, DATA_MISSING, "GDYY大卷入库时间或单卷米数缺失"));
            return;
        }
        int sequence = produceOrder.getAndIncrement();
        Cd15SingleShiftScheduleResult result = this.executeCandidate(snapshot, candidate, machine.get(),
                this.trialStock(candidate.getBigRollCode()), this.orderNo(candidate, sequence),
                this.orderNo(candidate, sequence), sequence);
        if (result.isScheduled()) {
            Cd15ScheduleResultDraft draft = result.getDraft();
            Cd15StorageLaneAllocationResult laneAllocation = this.allocateStorageLane(snapshot, draft);
            if (!laneAllocation.isSuccess()) {
                unscheduledResults.add(this.unscheduled(candidate, laneAllocation.getFailureReason(), "STORAGE_LANE_LIMIT"));
                return;
            }
            Cd15BigRollAgingAllocation allocation = this.allocateBigRoll(snapshot, candidate.getBigRollCode(),
                    draft.getBigRollConsumeMeters(), this.shiftStartTime(input, candidate.getClassIndex()));
            if (!allocation.isSuccess()) {
                unscheduledResults.add(this.unscheduled(candidate, allocation.getFailureReason(), "GDYY大卷成熟库存不足"));
                return;
            }
            draft.setBigRollBarcode(this.firstBarcode(allocation));
            this.applyLaneAllocation(draft, laneAllocation);
            snapshot.setStorageLanes(laneAllocation.getLanes());
            this.applySourceTrace(input, draft);
            scheduledDrafts.add(draft);
            this.deductCandidateStock(snapshot, candidate, draft);
        } else {
            unscheduledResults.add(result);
        }
    }

    private Cd15SingleShiftScheduleResult executeCandidate(Cd15RollingResourceSnapshot snapshot,
                                                           Cd15ScheduleCandidate candidate,
                                                           Cd15MachineInfo machine,
                                                           GdyyStock gdyyStock,
                                                           String sameOrderNo,
                                                           String sameGroupNo,
                                                           int sameProduceOrder) {
        Cd15SingleShiftScheduleResult result = singleShiftScheduleExecutor.execute(Cd15SingleShiftScheduleRequest.builder()
                .material(candidate.getMaterial())
                .demand(candidate.getDemand())
                .machine(machine)
                .gdyyStock(gdyyStock)
                .stockMetersAtSix(this.stockMeters(snapshot, candidate.getSteelStripCode()))
                .cordWidthMillimeter(candidate.getMaterial().getCordWidth())
                .orderNo(sameOrderNo)
                .groupNo(sameGroupNo)
                .produceOrder(sameProduceOrder)
                .build());
        return this.enrichShiftInfo(candidate, result);
    }

    private void markSplitDraft(Cd15ScheduleResultDraft draft,
                                String sameOrderNo,
                                String sameGroupNo,
                                int sameProduceOrder,
                                String bigRollBarcode) {
        draft.setOrderNo(sameOrderNo);
        draft.setGroupNo(sameGroupNo);
        draft.setProduceOrder(sameProduceOrder);
        draft.setBigRollBarcode(bigRollBarcode);
        draft.setCutMode(SPLIT);
    }

    /** 将钢带维度来源追溯信息写入每条排程草稿。 */
    private void applySourceTrace(Cd15AutoScheduleInput input,
                                  Cd15ScheduleResultDraft draft) {
        if (input == null || draft == null || !StringUtils.hasText(draft.getSteelStripCode())
                || input.getSteelStripSourceTraceBySteelStrip() == null) {
            return;
        }
        Cd15SteelStripSourceTrace sourceTrace = input.getSteelStripSourceTraceBySteelStrip()
                .get(draft.getSteelStripCode());
        if (sourceTrace == null) {
            return;
        }
        draft.setCxBatchNo(sourceTrace.getCxBatchNo());
        draft.setCxMachineCodes(sourceTrace.getCxMachineCodes());
        draft.setPlanSurplusQty(sourceTrace.getPlanSurplusQty());
    }

    private void addSplitUnscheduled(List<Cd15SingleShiftScheduleResult> unscheduledResults,
                                     Cd15SplitCutGroup splitGroup,
                                     String reasonCode,
                                     String reason) {
        unscheduledResults.add(this.unscheduled(splitGroup.getFirstCandidate(), reasonCode, reason));
        unscheduledResults.add(this.unscheduled(splitGroup.getSecondCandidate(), reasonCode, reason));
    }

    private Cd15SingleShiftScheduleResult unscheduled(Cd15ScheduleCandidate candidate,
                                                      String reasonCode,
                                                      String reason) {
        return Cd15SingleShiftScheduleResult.builder()
                .scheduled(false)
                .unscheduledReasonCode(reasonCode)
                .unscheduledReason(this.prefixedAnalysis(this.shiftDisplayName(candidate), reason))
                .factoryCode(candidate.getDemand().getFactoryCode())
                .scheduleDate(this.shiftDate(candidate))
                .steelStripCode(candidate.getSteelStripCode())
                .bigRollCode(candidate.getBigRollCode())
                .cuttingAngle(candidate.getCuttingAngle())
                .classField(candidate.getDemand().getClassField())
                .shiftDisplayName(this.shiftDisplayName(candidate))
                .demandQty(candidate.getDemand().getNaturalDemandQty())
                .scheduledQty(BigDecimal.ZERO)
                .unscheduledQty(candidate.getDemand().getNaturalDemandQty())
                .build();
    }


    private Cd15StorageLaneAllocationResult allocateStorageLane(Cd15RollingResourceSnapshot snapshot,
                                                                Cd15ScheduleResultDraft draft) {
        return this.allocateStorageLane(snapshot.getStorageLanes(), draft);
    }

    private Cd15StorageLaneAllocationResult allocateStorageLane(List<com.zlt.aps.cd15.engine.model.Cd15StorageLaneState> storageLanes,
                                                                Cd15ScheduleResultDraft draft) {
        return storageLaneAllocator.allocate(draft.getSteelStripCode(), draft.getPlanQty(),
                draft.getVehiclePlanQuantity(), storageLanes);
    }

    private void applyLaneAllocation(Cd15ScheduleResultDraft draft,
                                     Cd15StorageLaneAllocationResult allocation) {
        List<Cd15StorageLaneAllocation> allocations = allocation.getAllocations() == null
                ? Collections.emptyList() : allocation.getAllocations();
        int totalVehicles = allocations.stream().mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        BigDecimal remainingQuantity = this.value(draft.getPlanQty());
        List<Cd15LaneAllocationDraft> laneDrafts = new ArrayList<>();
        for (int index = 0; index < allocations.size(); index++) {
            Cd15StorageLaneAllocation source = allocations.get(index);
            BigDecimal quantity = index == allocations.size() - 1
                    ? remainingQuantity
                    : this.value(draft.getPlanQty()).multiply(BigDecimal.valueOf(source.getVehicleCount()))
                            .divide(BigDecimal.valueOf(Math.max(1, totalVehicles)), 4, RoundingMode.HALF_UP);
            remainingQuantity = remainingQuantity.subtract(quantity);
            laneDrafts.add(Cd15LaneAllocationDraft.builder()
                    .classField(draft.getClassField())
                    .laneCode(source.getLaneCode())
                    .allocationQuantity(quantity)
                    .vehicleCount(source.getVehicleCount())
                    .build());
        }
        draft.setLaneAllocations(laneDrafts);
        draft.setStorageLaneCode(laneDrafts.stream()
                .map(Cd15LaneAllocationDraft::getLaneCode)
                .collect(Collectors.joining(",")));
    }

    private void deductCandidateStock(Cd15RollingResourceSnapshot snapshot,
                                      Cd15ScheduleCandidate candidate,
                                      Cd15ScheduleResultDraft draft) {
        BigDecimal rawDemandMeters = demandCalculator.calculateRawDemandMeters(
                draft.getPieceCount(), candidate.getMaterial().getCraftWidth());
        this.deductStock(snapshot, candidate.getSteelStripCode(), rawDemandMeters);
    }

    private Cd15BigRollAgingAllocation allocateBigRoll(Cd15RollingResourceSnapshot snapshot,
                                                       String bigRollCode,
                                                       BigDecimal consumeMeters,
                                                       LocalDateTime startTime) {
        Map<String, List<Cd15BigRollAgingStock>> agingStocksByBigRoll = snapshot.getGdyyAgingStocksByBigRoll() == null
                ? Collections.emptyMap() : snapshot.getGdyyAgingStocksByBigRoll();
        return bigRollAgingAllocator.allocate(agingStocksByBigRoll.getOrDefault(bigRollCode, Collections.emptyList()),
                bigRollCode, consumeMeters, startTime);
    }

    private boolean hasAgingDataMissing(Cd15RollingResourceSnapshot snapshot, String bigRollCode) {
        return snapshot.getDataMissingBigRollCodes() != null
                && snapshot.getDataMissingBigRollCodes().contains(bigRollCode);
    }

    private GdyyStock trialStock(String bigRollCode) {
        GdyyStock stock = new GdyyStock();
        stock.setBigRollCode(bigRollCode);
        stock.setStockMeters(new BigDecimal("999999999"));
        return stock;
    }

    private String firstBarcode(Cd15BigRollAgingAllocation allocation) {
        return allocation.getItems() == null ? null : allocation.getItems().stream()
                .map(Cd15BigRollAgingAllocationItem::getStock)
                .filter(item -> item != null && item.getBigRollBarcode() != null)
                .map(Cd15BigRollAgingStock::getBigRollBarcode)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal stockMeters(Cd15RollingResourceSnapshot snapshot, String steelStripCode) {
        Map<String, BigDecimal> stockMetersBySteelStrip = snapshot.getStockMetersBySteelStrip() == null
                ? Collections.emptyMap() : snapshot.getStockMetersBySteelStrip();
        return stockMetersBySteelStrip.getOrDefault(steelStripCode, BigDecimal.ZERO);
    }

    private void deductStock(Cd15RollingResourceSnapshot snapshot, String steelStripCode, BigDecimal consumeMeters) {
        if (snapshot.getStockMetersBySteelStrip() == null) {
            return;
        }
        snapshot.getStockMetersBySteelStrip().compute(steelStripCode,
                (key, oldValue) -> this.value(oldValue).subtract(this.value(consumeMeters)).max(BigDecimal.ZERO));
    }

    private Cd15SingleShiftScheduleResult enrichShiftInfo(Cd15ScheduleCandidate candidate,
                                                           Cd15SingleShiftScheduleResult result) {
        if (result == null) {
            return null;
        }
        String shiftDisplayName = this.shiftDisplayName(candidate);
        Date shiftDate = this.shiftDate(candidate);
        result.setShiftDisplayName(shiftDisplayName);
        result.setScheduleDate(shiftDate);
        result.setUnscheduledReason(this.prefixedAnalysis(shiftDisplayName, result.getUnscheduledReason()));
        Cd15ScheduleResultDraft draft = result.getDraft();
        if (draft != null) {
            draft.setShiftDisplayName(shiftDisplayName);
            draft.setScheduleDate(shiftDate);
            draft.setAnalysis(this.prefixedAnalysis(shiftDisplayName, draft.getAnalysis()));
        }
        return result;
    }

    private String shiftStageName(Cd15ScheduleCandidate candidate, String suffix) {
        return this.shiftDisplayName(candidate) + suffix;
    }

    private String shiftDisplayName(Cd15ScheduleCandidate candidate) {
        if (candidate == null || candidate.getDemand() == null) {
            return "CLASS1";
        }
        return Cd15ShiftDisplayHelper.shiftDisplayName(candidate.getDemand().getScheduleDate(), candidate.getClassIndex());
    }

    private Date shiftDate(Cd15ScheduleCandidate candidate) {
        if (candidate == null || candidate.getDemand() == null) {
            return null;
        }
        LocalDate displayDate = Cd15ShiftDisplayHelper.displayDate(
                Cd15ShiftDisplayHelper.toLocalDate(candidate.getDemand().getScheduleDate()), candidate.getClassIndex());
        return Cd15ShiftDisplayHelper.toDate(displayDate);
    }

    private String prefixedAnalysis(String shiftDisplayName, String analysis) {
        if (!StringUtils.hasText(shiftDisplayName)) {
            return analysis;
        }
        if (!StringUtils.hasText(analysis)) {
            return shiftDisplayName;
        }
        String prefix = shiftDisplayName + "：";
        return analysis.startsWith(prefix) ? analysis : prefix + analysis;
    }

    private LocalDateTime shiftStartTime(Cd15AutoScheduleInput input, int classIndex) {
        return Cd15ShiftDisplayHelper.shiftStartTime(input == null ? null : input.getScheduleDate(), classIndex);
    }
    private String orderNo(Cd15ScheduleCandidate candidate, int sequence) {
        return String.format("CD15-%s-%02d-%03d", candidate.getDemand().getCxBatchNo(),
                candidate.getClassIndex(), sequence);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}