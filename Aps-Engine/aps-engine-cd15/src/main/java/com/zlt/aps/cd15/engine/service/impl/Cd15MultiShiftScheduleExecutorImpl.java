package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.algorithm.Cd15DemandCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15MachineCandidateResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15ResourceSnapshotBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateSorter;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleRequest;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
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

    private final Cd15ResourceSnapshotBuilder resourceSnapshotBuilder;
    private final Cd15ScheduleCandidateBuilder candidateBuilder;
    private final Cd15ScheduleCandidateSorter candidateSorter;
    private final Cd15MachineCandidateResolver machineCandidateResolver;
    private final Cd15DemandCalculator demandCalculator;
    private final Cd15SingleShiftScheduleExecutor singleShiftScheduleExecutor;

    @Override
    public Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input) {
        Cd15RollingResourceSnapshot snapshot = resourceSnapshotBuilder.build(input);
        List<Cd15ScheduleCandidate> candidates = candidateSorter.sort(candidateBuilder.build(input, snapshot));
        List<Cd15ScheduleResultDraft> scheduledDrafts = new ArrayList<>();
        List<Cd15SingleShiftScheduleResult> unscheduledResults = new ArrayList<>();
        AtomicInteger produceOrder = new AtomicInteger(1);
        candidates.forEach(candidate -> this.scheduleCandidate(input, snapshot, candidate,
                produceOrder, scheduledDrafts, unscheduledResults));
        return Cd15MultiShiftScheduleResult.builder()
                .scheduledDrafts(scheduledDrafts)
                .unscheduledResults(unscheduledResults)
                .build();
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
        BigDecimal stockMetersAtSix = this.stockMeters(snapshot, candidate.getSteelStripCode());
        Cd15SingleShiftScheduleResult result = singleShiftScheduleExecutor.execute(
                Cd15SingleShiftScheduleRequest.builder()
                        .material(candidate.getMaterial())
                        .demand(candidate.getDemand())
                        .machine(machine.get())
                        .gdyyStock(gdyyStock)
                        .stockMetersAtSix(stockMetersAtSix)
                        .cordWidthMillimeter(candidate.getMaterial().getCordWidth())
                        .orderNo(this.orderNo(candidate, produceOrder.get()))
                        .groupNo(this.orderNo(candidate, produceOrder.get()))
                        .produceOrder(produceOrder.getAndIncrement())
                        .build());
        if (result.isScheduled()) {
            Cd15ScheduleResultDraft draft = result.getDraft();
            scheduledDrafts.add(draft);
            BigDecimal rawDemandMeters = demandCalculator.calculateRawDemandMeters(
                    draft.getPieceCount(), candidate.getMaterial().getCraftWidth());
            this.deductStock(snapshot, candidate.getSteelStripCode(), rawDemandMeters);
            this.deductBigRollStock(gdyyStock, draft.getBigRollConsumeMeters());
        } else {
            unscheduledResults.add(result);
        }
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

    private String orderNo(Cd15ScheduleCandidate candidate, int sequence) {
        return String.format("CD15-%s-%02d-%03d", candidate.getDemand().getCxBatchNo(),
                candidate.getClassIndex(), sequence);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}