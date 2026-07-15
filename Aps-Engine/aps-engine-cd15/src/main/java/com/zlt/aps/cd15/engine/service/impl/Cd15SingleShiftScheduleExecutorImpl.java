package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.algorithm.Cd15BigRollMeterCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15DemandCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15MachineCandidateResolver;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15NaturalDemand;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleRequest;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
import com.zlt.aps.cd15.engine.service.Cd15SingleShiftScheduleExecutor;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CD15 单规格单班排程执行器实现。
 */
@Service
@RequiredArgsConstructor
public class Cd15SingleShiftScheduleExecutorImpl implements Cd15SingleShiftScheduleExecutor {

    private static final String DATA_MISSING = "DATA_MISSING";
    private static final String NO_BIG_ROLL_STOCK = "NO_BIG_ROLL_STOCK";

    private final Cd15DemandCalculator demandCalculator;
    private final Cd15BigRollMeterCalculator bigRollMeterCalculator;
    private final Cd15MachineCandidateResolver machineCandidateResolver;

    @Override
    public Cd15SingleShiftScheduleResult execute(Cd15SingleShiftScheduleRequest request) {
        String validationError = this.validateRequired(request);
        if (validationError != null) {
            return Cd15SingleShiftScheduleResult.unscheduled(DATA_MISSING, validationError);
        }
        Cd15ConstructionMaterial material = request.getMaterial();
        Cd15NaturalDemand demand = request.getDemand();
        Cd15MachineInfo machine = request.getMachine();
        BigDecimal effectiveWidth = material.getCraftWidth();
        if (!machineCandidateResolver.supports(machine, effectiveWidth)) {
            return Cd15SingleShiftScheduleResult.unscheduled(
                    machineCandidateResolver.resolveFailureReason(machine, effectiveWidth),
                    "机台宽度或启用状态不满足当前斜裁规格");
        }
        BigDecimal pieceCount = demandCalculator.calculatePieceCount(demand.getNaturalDemandQty(),
                material.getUnitConsumeMillimeter(), material.getCurlLength());
        BigDecimal netDemandMeters = demandCalculator.calculateNetDemandMeters(pieceCount,
                material.getCraftWidth(), request.getStockMetersAtSix());
        BigDecimal bigRollConsumeMeters = bigRollMeterCalculator.calculateBigRollConsumeMeters(pieceCount,
                material.getUnitConsumeMillimeter(), material.getCraftWidth(),
                request.getCordWidthMillimeter(), netDemandMeters);
        if (this.stockMeters(request.getGdyyStock()).compareTo(bigRollConsumeMeters) < 0) {
            return Cd15SingleShiftScheduleResult.unscheduled(NO_BIG_ROLL_STOCK,
                    "GDYY大卷库存不足，无法满足当前单班试排");
        }
        return Cd15SingleShiftScheduleResult.scheduled(this.toDraft(request, pieceCount,
                netDemandMeters, bigRollConsumeMeters));
    }

    private Cd15ScheduleResultDraft toDraft(Cd15SingleShiftScheduleRequest request,
                                            BigDecimal pieceCount,
                                            BigDecimal netDemandMeters,
                                            BigDecimal bigRollConsumeMeters) {
        Cd15ConstructionMaterial material = request.getMaterial();
        Cd15NaturalDemand demand = request.getDemand();
        Cd15MachineInfo machine = request.getMachine();
        GdyyStock gdyyStock = request.getGdyyStock();
        return Cd15ScheduleResultDraft.builder()
                .orderNo(request.getOrderNo())
                .groupNo(request.getGroupNo())
                .factoryCode(demand.getFactoryCode())
                .scheduleDate(demand.getScheduleDate())
                .cxBatchNo(demand.getCxBatchNo())
                .bigRollCode(material.getBigRollCode())
                .bigRollBarcode(gdyyStock.getBigRollBarcode())
                .steelStripCode(material.getSteelStripCode())
                .cuttingAngle(material.getCuttingAngle())
                .machineCode(machine.getMachineCode())
                .machineName(machine.getMachineName())
                .classField(demand.getClassField())
                .classIndex(demand.getClassIndex())
                .cxPlanQty(demand.getNaturalDemandQty())
                .planQty(netDemandMeters)
                .produceOrder(request.getProduceOrder())
                .pieceCount(pieceCount)
                .netDemandMeters(netDemandMeters)
                .bigRollConsumeMeters(bigRollConsumeMeters)
                .vehiclePlanQuantity(this.vehiclePlanQuantity(material))
                .stockMetersAtSix(request.getStockMetersAtSix())
                .cutMode("SINGLE")
                .sourceType("AUTO_SCHEDULE")
                .analysis("单规格单班试排")
                .build();
    }


    private BigDecimal vehiclePlanQuantity(Cd15ConstructionMaterial material) {
        BigDecimal unitConsumeMeters = material.getUnitConsumeMillimeter().divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        if (unitConsumeMeters.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal piecePerVehicle = material.getCurlLength().divide(unitConsumeMeters, 0, RoundingMode.FLOOR);
        BigDecimal craftWidthMeters = material.getCraftWidth().divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        return piecePerVehicle.multiply(craftWidthMeters);
    }
    private String validateRequired(Cd15SingleShiftScheduleRequest request) {
        if (request == null || request.getMaterial() == null || request.getDemand() == null
                || request.getMachine() == null || request.getGdyyStock() == null) {
            return "单规格单班试排参数不完整";
        }
        if (request.getMaterial().getCraftWidth() == null
                || request.getMaterial().getUnitConsumeMillimeter() == null
                || request.getMaterial().getCurlLength() == null) {
            return "施工宽度、单耗或卷曲长度缺失";
        }
        return null;
    }

    private BigDecimal stockMeters(GdyyStock gdyyStock) {
        return gdyyStock.getStockMeters() == null ? BigDecimal.ZERO : gdyyStock.getStockMeters();
    }
}