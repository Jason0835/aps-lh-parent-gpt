package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.algorithm.Cd15DemandCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15MachineCandidateResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15ResourceSnapshotBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateSorter;
import com.zlt.aps.cd15.engine.algorithm.Cd15SplitCutGroupBuilder;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleRequest;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15SplitCutGroup;
import com.zlt.aps.cd15.engine.service.Cd15MultiShiftScheduleExecutor;
import com.zlt.aps.cd15.engine.service.Cd15SingleShiftScheduleExecutor;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CD15 多班滚动试排执行器实现。
 */
@Service
@RequiredArgsConstructor
public class Cd15MultiShiftScheduleExecutorImpl implements Cd15MultiShiftScheduleExecutor {

    private static final String NO_BIG_ROLL_STOCK = "NO_BIG_ROLL_STOCK";
    private static final String SPLIT = "SPLIT";

    private final Cd15ResourceSnapshotBuilder resourceSnapshotBuilder;
    private final Cd15ScheduleCandidateBuilder candidateBuilder;
    private final Cd15ScheduleCandidateSorter candidateSorter;
    private final Cd15SplitCutGroupBuilder splitCutGroupBuilder;
    private final Cd15MachineCandidateResolver machineCandidateResolver;
    private final Cd15DemandCalculator demandCalculator;
    private final Cd15SingleShiftScheduleExecutor singleShiftScheduleExecutor;

    @Override
    public Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input) {
        Cd15RollingResourceSnapshot snapshot = resourceSnapshotBuilder.build(input);
        List<Cd15ScheduleCandidate> candidates = new ArrayList<>(
                candidateSorter.sort(candidateBuilder.build(input, snapshot)));
        List<Cd15ScheduleResultDraft> scheduledDrafts = new ArrayList<>();
        List<Cd15SingleShiftScheduleResult> unscheduledResults = new ArrayList<>();
        AtomicInteger produceOrder = new AtomicInteger(1);
        while (!candidates.isEmpty()) {
            Cd15ScheduleCandidate candidate = candidates.remove(0);
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
        Optional<Cd15MachineInfo> machine = machineCandidateResolver.resolve(
                input == null ? Collections.emptyList() : input.getMachines(), splitGroup.getCombinedWidth());
        if (!machine.isPresent()) {
            this.addSplitUnscheduled(unscheduledResults, Cd15MachineCandidateResolver.NO_AVAILABLE_MACHINE,
                    "未找到满足分裁组合宽度的可用机台");
            return;
        }
        GdyyStock gdyyStock = this.firstAvailableBigRoll(snapshot,
                splitGroup.getFirstCandidate().getBigRollCode());
        if (gdyyStock == null) {
            this.addSplitUnscheduled(unscheduledResults, NO_BIG_ROLL_STOCK, "GDYY大卷库存不足");
            return;
        }
        int sameProduceOrder = produceOrder.getAndIncrement();
        String sameOrderNo = this.orderNo(splitGroup.getFirstCandidate(), sameProduceOrder);
        String sameGroupNo = sameOrderNo;
        GdyyStock trialStock = this.copyStock(gdyyStock);
        Cd15SingleShiftScheduleResult firstResult = this.executeCandidate(snapshot, splitGroup.getFirstCandidate(), machine.get(),
                trialStock, sameOrderNo, sameGroupNo, sameProduceOrder);
        Cd15SingleShiftScheduleResult secondResult = this.executeCandidate(snapshot, splitGroup.getSecondCandidate(), machine.get(),
                trialStock, sameOrderNo, sameGroupNo, sameProduceOrder);
        if (!firstResult.isScheduled() || !secondResult.isScheduled()) {
            unscheduledResults.add(firstResult.isScheduled() ? secondResult : firstResult);
            return;
        }
        BigDecimal totalBigRollConsume = this.value(firstResult.getDraft().getBigRollConsumeMeters())
                .add(this.value(secondResult.getDraft().getBigRollConsumeMeters()));
        if (this.value(gdyyStock.getStockMeters()).compareTo(totalBigRollConsume) < 0) {
            this.addSplitUnscheduled(unscheduledResults, NO_BIG_ROLL_STOCK, "GDYY大卷库存不足，无法满足分裁组合");
            return;
        }
        this.markSplitDraft(firstResult.getDraft(), sameOrderNo, sameGroupNo, sameProduceOrder);
        this.markSplitDraft(secondResult.getDraft(), sameOrderNo, sameGroupNo, sameProduceOrder);
        scheduledDrafts.add(firstResult.getDraft());
        scheduledDrafts.add(secondResult.getDraft());
        this.deductCandidateStock(snapshot, splitGroup.getFirstCandidate(), firstResult.getDraft());
        this.deductCandidateStock(snapshot, splitGroup.getSecondCandidate(), secondResult.getDraft());
        this.deductBigRollStock(gdyyStock, totalBigRollConsume);
    }

    private void scheduleCandidate(Cd15AutoScheduleInput input,
                                   Cd15RollingResourceSnapshot snapshot,
                                   Cd15ScheduleCandidate candidate,
                                   AtomicInteger produceOrder,
                                   List<Cd15ScheduleResultDraft> scheduledDrafts,
                                   List<Cd15SingleShiftScheduleResult> unscheduledResults) {
        Optional<Cd15MachineInfo> machine = machineCandidateResolver.resolve(
                input == null ? Collections.emptyList() : input.getMachines(),
                candidate.getMaterial().getCraftWidth());
        if (!machine.isPresent()) {
            unscheduledResults.add(Cd15SingleShiftScheduleResult.unscheduled(
                    Cd15MachineCandidateResolver.NO_AVAILABLE_MACHINE, "未找到满足宽度的可用机台"));
            return;
        }
        GdyyStock gdyyStock = this.firstAvailableBigRoll(snapshot, candidate.getBigRollCode());
        if (gdyyStock == null) {
            unscheduledResults.add(Cd15SingleShiftScheduleResult.unscheduled(
                    NO_BIG_ROLL_STOCK, "GDYY大卷库存不足"));
            return;
        }
        int sequence = produceOrder.getAndIncrement();
        Cd15SingleShiftScheduleResult result = this.executeCandidate(snapshot, candidate, machine.get(), gdyyStock,
                this.orderNo(candidate, sequence), this.orderNo(candidate, sequence), sequence);
        if (result.isScheduled()) {
            Cd15ScheduleResultDraft draft = result.getDraft();
            scheduledDrafts.add(draft);
            this.deductCandidateStock(snapshot, candidate, draft);
            this.deductBigRollStock(gdyyStock, draft.getBigRollConsumeMeters());
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
        return singleShiftScheduleExecutor.execute(Cd15SingleShiftScheduleRequest.builder()
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
    }

    private void markSplitDraft(Cd15ScheduleResultDraft draft,
                                String sameOrderNo,
                                String sameGroupNo,
                                int sameProduceOrder) {
        draft.setOrderNo(sameOrderNo);
        draft.setGroupNo(sameGroupNo);
        draft.setProduceOrder(sameProduceOrder);
        draft.setCutMode(SPLIT);
    }

    private void addSplitUnscheduled(List<Cd15SingleShiftScheduleResult> unscheduledResults,
                                     String reasonCode,
                                     String reason) {
        unscheduledResults.add(Cd15SingleShiftScheduleResult.unscheduled(reasonCode, reason));
    }

    private void deductCandidateStock(Cd15RollingResourceSnapshot snapshot,
                                      Cd15ScheduleCandidate candidate,
                                      Cd15ScheduleResultDraft draft) {
        BigDecimal rawDemandMeters = demandCalculator.calculateRawDemandMeters(
                draft.getPieceCount(), candidate.getMaterial().getCraftWidth());
        this.deductStock(snapshot, candidate.getSteelStripCode(), rawDemandMeters);
    }

    private GdyyStock firstAvailableBigRoll(Cd15RollingResourceSnapshot snapshot, String bigRollCode) {
        Map<String, List<GdyyStock>> gdyyStocksByBigRoll = snapshot.getGdyyStocksByBigRoll() == null
                ? Collections.emptyMap() : snapshot.getGdyyStocksByBigRoll();
        return gdyyStocksByBigRoll.getOrDefault(bigRollCode, Collections.emptyList()).stream()
                .filter(item -> this.value(item.getStockMeters()).signum() > 0)
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

    private void deductBigRollStock(GdyyStock gdyyStock, BigDecimal consumeMeters) {
        gdyyStock.setStockMeters(this.value(gdyyStock.getStockMeters())
                .subtract(this.value(consumeMeters)).max(BigDecimal.ZERO));
    }

    private GdyyStock copyStock(GdyyStock source) {
        GdyyStock target = new GdyyStock();
        target.setFactoryCode(source.getFactoryCode());
        target.setBigRollCode(source.getBigRollCode());
        target.setBigRollBarcode(source.getBigRollBarcode());
        target.setStockMeters(source.getStockMeters());
        return target;
    }

    private String orderNo(Cd15ScheduleCandidate candidate, int sequence) {
        return String.format("CD15-%s-%02d-%03d", candidate.getDemand().getCxBatchNo(),
                candidate.getClassIndex(), sequence);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}