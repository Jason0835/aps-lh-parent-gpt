package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15NaturalDemand;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CD15 成型逐班自然需求候选构建器。
 */
@Component
public class Cd15ScheduleCandidateBuilder {

    /**
     * 根据成型结果的 class1~class8 计划量与施工版本，动态展开自然需求。
     *
     * @param input 自动排程输入
     * @return 成型自然需求列表
     */
    public List<Cd15NaturalDemand> buildNaturalDemands(Cd15AutoScheduleInput input) {
        List<CxScheduleResult> formingSchedules = input == null || input.getFormingSchedules() == null
                ? Collections.emptyList() : input.getFormingSchedules();
        List<Cd15ShiftDescriptor> shifts = input == null || input.getShifts() == null
                ? Collections.emptyList() : input.getShifts();
        return formingSchedules.stream()
                .filter(Objects::nonNull)
                .flatMap(schedule -> shifts.stream()
                        .map(Cd15ShiftDescriptor::getClassIndex)
                        .map(classIndex -> this.toNaturalDemand(schedule, classIndex)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将自然需求与施工材料层位合并为待排候选。
     *
     * @param input 自动排程输入
     * @param snapshot 滚动资源快照
     * @return 待排候选
     */
    public List<Cd15ScheduleCandidate> build(Cd15AutoScheduleInput input,
                                             Cd15RollingResourceSnapshot snapshot) {
        List<Cd15ConstructionMaterial> materials = input == null || input.getConstructionMaterials() == null
                ? Collections.emptyList() : input.getConstructionMaterials();
        Map<String, BigDecimal> stockMetersBySteelStrip = snapshot == null
                || snapshot.getStockMetersBySteelStrip() == null
                ? Collections.emptyMap() : snapshot.getStockMetersBySteelStrip();
        Map<Integer, Cd15ShiftDescriptor> shiftByClassIndex = input == null || input.getShifts() == null
                ? Collections.emptyMap() : input.getShifts().stream()
                .collect(Collectors.toMap(Cd15ShiftDescriptor::getClassIndex,
                        Function.identity(), (first, second) -> first));
        List<Cd15ScheduleCandidate> candidates = this.buildNaturalDemands(input).stream()
                .flatMap(demand -> materials.stream()
                        .filter(material -> this.match(demand, material))
                        .map(material -> this.toCandidate(demand, material,
                                shiftByClassIndex.get(demand.getClassIndex()), stockMetersBySteelStrip)))
                .collect(Collectors.toList());
        return this.applyDepthWindow(candidates, input);
    }

    /** 按钢带备库深度截取自然班次窗口，小数部分按比例计入末班。 */
    private List<Cd15ScheduleCandidate> applyDepthWindow(List<Cd15ScheduleCandidate> candidates,
                                                         Cd15AutoScheduleInput input) {
        if (input == null || input.getDepthClassQtyBySteelStrip() == null) {
            throw new IllegalArgumentException("按钢带备库深度不能为空");
        }
        List<Cd15ShiftDescriptor> orderedShifts = input.getShifts() == null
                ? Collections.emptyList() : input.getShifts().stream()
                .sorted(Comparator.comparing(Cd15ShiftDescriptor::getStartTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(Cd15ShiftDescriptor::getClassIndex))
                .collect(Collectors.toList());
        return candidates.stream()
                .collect(Collectors.groupingBy(Cd15ScheduleCandidate::getSteelStripCode,
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .flatMap(entry -> {
                    BigDecimal depth = input.getDepthClassQtyBySteelStrip().get(entry.getKey());
                    Map<Integer, BigDecimal> weights = this.shiftWeights(entry.getKey(), depth, orderedShifts);
                    return entry.getValue().stream()
                            .filter(candidate -> weights.containsKey(candidate.getClassIndex()))
                            .map(candidate -> this.copyWithWeight(candidate,
                                    weights.get(candidate.getClassIndex())));
                })
                .collect(Collectors.toList());
    }

    private Map<Integer, BigDecimal> shiftWeights(String steelStripCode,
                                                   BigDecimal depth,
                                                   List<Cd15ShiftDescriptor> orderedShifts) {
        if (depth == null || depth.signum() <= 0) {
            throw new IllegalArgumentException("钢带未匹配有效备库深度, steelStripCode=" + steelStripCode);
        }
        int requiredShiftCount;
        try {
            requiredShiftCount = depth.setScale(0, RoundingMode.CEILING).intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("钢带备库深度超出支持范围, steelStripCode=" + steelStripCode, exception);
        }
        int fullShiftCount = depth.setScale(0, RoundingMode.FLOOR).intValue();
        BigDecimal fraction = depth.subtract(BigDecimal.valueOf(fullShiftCount));
        Map<Integer, BigDecimal> weights = new LinkedHashMap<>();
        for (int index = 0; index < Math.min(requiredShiftCount, orderedShifts.size()); index++) {
            BigDecimal weight = index == fullShiftCount && fraction.signum() > 0
                    ? fraction : BigDecimal.ONE;
            weights.put(orderedShifts.get(index).getClassIndex(), weight);
        }
        return weights;
    }

    private Cd15ScheduleCandidate copyWithWeight(Cd15ScheduleCandidate source, BigDecimal weight) {
        Cd15NaturalDemand demand = source.getDemand();
        Cd15NaturalDemand weightedDemand = Cd15NaturalDemand.builder()
                .factoryCode(demand.getFactoryCode())
                .scheduleDate(demand.getScheduleDate())
                .cxBatchNo(demand.getCxBatchNo())
                .cxMachineCode(demand.getCxMachineCode())
                .constructionCode(demand.getConstructionCode())
                .constructionVersion(demand.getConstructionVersion())
                .classField(demand.getClassField())
                .classIndex(demand.getClassIndex())
                .naturalDemandQty(demand.getNaturalDemandQty().multiply(weight))
                .build();
        return Cd15ScheduleCandidate.builder()
                .demand(weightedDemand)
                .material(source.getMaterial())
                .shift(source.getShift())
                .classIndex(source.getClassIndex())
                .cuttingAngle(source.getCuttingAngle())
                .steelStripCode(source.getSteelStripCode())
                .bigRollCode(source.getBigRollCode())
                .stockMetersAtSix(source.getStockMetersAtSix())
                .shortageInCurrentShift(source.isShortageInCurrentShift())
                .continueFromPreviousShift(source.isContinueFromPreviousShift())
                .build();
    }
    private Cd15NaturalDemand toNaturalDemand(CxScheduleResult schedule, int classIndex) {
        BigDecimal planQty = this.toBigDecimal(this.getFieldValue(schedule,
                String.format("class%dPlanQty", classIndex)));
        String recipeNo = this.toString(this.getFieldValue(schedule,
                String.format("class%dRecipeNo", classIndex)));
        if (planQty.signum() <= 0 || !StringUtils.hasText(recipeNo)
                || !StringUtils.hasText(schedule.getEmbryoCode())) {
            return null;
        }
        return Cd15NaturalDemand.builder()
                .factoryCode(schedule.getFactoryCode())
                .scheduleDate(schedule.getScheduleDate())
                .cxBatchNo(schedule.getCxBatchNo())
                .cxMachineCode(schedule.getCxMachineCode())
                .constructionCode(schedule.getEmbryoCode())
                .constructionVersion(recipeNo)
                .classField("class" + classIndex)
                .classIndex(classIndex)
                .naturalDemandQty(planQty)
                .build();
    }

    private Cd15ScheduleCandidate toCandidate(Cd15NaturalDemand demand,
                                              Cd15ConstructionMaterial material,
                                              Cd15ShiftDescriptor shift,
                                              Map<String, BigDecimal> stockMetersBySteelStrip) {
        BigDecimal stockMetersAtSix = stockMetersBySteelStrip.getOrDefault(
                material.getSteelStripCode(), BigDecimal.ZERO);
        return Cd15ScheduleCandidate.builder()
                .demand(demand)
                .material(material)
                .shift(shift)
                .classIndex(demand.getClassIndex())
                .cuttingAngle(material.getCuttingAngle())
                .steelStripCode(material.getSteelStripCode())
                .bigRollCode(material.getBigRollCode())
                .stockMetersAtSix(stockMetersAtSix)
                .shortageInCurrentShift(stockMetersAtSix.signum() <= 0)
                .continueFromPreviousShift(false)
                .build();
    }

    private boolean match(Cd15NaturalDemand demand, Cd15ConstructionMaterial material) {
        return material != null
                && Objects.equals(demand.getConstructionCode(), material.getConstructionCode())
                && Objects.equals(demand.getConstructionVersion(), material.getConstructionVersion());
    }

    private Object getFieldValue(Object source, String fieldName) {
        if (source == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method method = source.getClass().getMethod(methodName);
            return method.invoke(source);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取字段失败: " + source.getClass().getSimpleName() + "." + fieldName, exception);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private String toString(Object value) {
        return value == null ? null : value.toString().trim();
    }
}
