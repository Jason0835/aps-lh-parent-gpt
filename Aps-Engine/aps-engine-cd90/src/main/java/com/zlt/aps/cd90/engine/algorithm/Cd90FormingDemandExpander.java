package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将成型排程宽表和施工层位转换为帘布逐自然班次需求明细。
 */
@Component
@Slf4j
public class Cd90FormingDemandExpander {

    private static final BigDecimal MILLIMETERS_PER_METER = new BigDecimal("1000");
    private static final BigDecimal SHIFT_HOURS = new BigDecimal("8");
    private static final LocalTime FIRST_SHIFT_TIME = LocalTime.of(6, 0);

    /**
     * 按帘布代码和自然班次汇总成型条数及帘布需求米数。
     * 成型排程的CLASS1对应排程日前一天早班，之后每个CLASS字段递增8小时。
     *
     * @param schedules 成型排程窄模型
     * @param materials 施工层位帘布单耗
     * @return 按帘布代码、班次开始时间排序的需求明细
     */
    public List<Cd90DemandShift> expand(List<Cd90FormingScheduleSource> schedules,
                                        List<Cd90ConstructionMaterial> materials) {
        // 施工单耗先按胎胚代码+施工版本分组；同一版本同一帘布多层出现时累加毫米单耗。
        Map<String, Map<String, BigDecimal>> consumeByConstruction = groupUnitConsume(materials);
        Map<String, DemandAccumulator> demandByClothAndShift = new LinkedHashMap<>();

        for (Cd90FormingScheduleSource schedule : safe(schedules)) {
            if (schedule == null || schedule.getScheduleDate() == null) {
                continue;
            }
            List<BigDecimal> quantities = safe(schedule.getClassPlanQuantities());
            List<String> recipeNos = safe(schedule.getClassRecipeNos());
            for (int classIndex = 0; classIndex < Math.min(8, quantities.size()); classIndex++) {
                BigDecimal formingQuantity = value(quantities.get(classIndex));
                // 成型CLASS1从排程日前一天6点开始，后续CLASS字段按8小时自然班次展开。
                LocalDateTime startTime = schedule.getScheduleDate().minusDays(1)
                        .atTime(FIRST_SHIFT_TIME).plusHours(classIndex * 8L);
                String classField = "CLASS" + (classIndex + 1);
                String recipeNo = classIndex < recipeNos.size() ? recipeNos.get(classIndex) : null;
                if (!StringUtils.hasText(recipeNo)) {
                    if (formingQuantity.signum() > 0) {
                        log.warn("[直裁自动排程] 成型班次施工版本为空，跳过该班施工分解, "
                                        + "cxBatchNo={}, embryoCode={}, classField={}, startTime={}",
                                schedule.getCxBatchNo(), schedule.getEmbryoCode(), classField, startTime);
                    }
                    continue;
                }
                // 成型计划必须使用embryoCode+CLASSn_RECIPE_NO关联施工，不使用SAP品号或成型物料描述。
                Map<String, BigDecimal> clothConsumes = consumeByConstruction.get(
                        constructionKey(schedule.getEmbryoCode(), recipeNo));
                if (clothConsumes == null || clothConsumes.isEmpty()) {
                    if (formingQuantity.signum() > 0) {
                        log.warn("[直裁自动排程] 未找到胎胚施工版本，跳过该班施工分解, "
                                        + "cxBatchNo={}, embryoCode={}, constructionVersion={}, classField={}",
                                schedule.getCxBatchNo(), schedule.getEmbryoCode(), recipeNo, classField);
                    }
                    continue;
                }
                for (Map.Entry<String, BigDecimal> entry : clothConsumes.entrySet()) {
                    // 同帘布同自然班次可能来自多条成型计划，使用稳定键统一累计。
                    String key = entry.getKey() + "|" + startTime;
                    DemandAccumulator accumulator = demandByClothAndShift.computeIfAbsent(key,
                            ignored -> new DemandAccumulator(entry.getKey(), classField, startTime));
                    accumulator.add(formingQuantity, entry.getValue());
                }
            }
        }

        return demandByClothAndShift.values().stream()
                .map(DemandAccumulator::toDemandShift)
                .sorted(Comparator.comparing(Cd90DemandShift::getClothCode)
                        .thenComparing(Cd90DemandShift::getStartTime))
                .collect(Collectors.toList());
    }

    private Map<String, Map<String, BigDecimal>> groupUnitConsume(List<Cd90ConstructionMaterial> materials) {
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        for (Cd90ConstructionMaterial material : safe(materials)) {
            if (material == null || !StringUtils.hasText(material.getConstructionCode())
                    || !StringUtils.hasText(material.getConstructionVersion())
                    || !StringUtils.hasText(material.getClothCode())) {
                continue;
            }
            result.computeIfAbsent(constructionKey(material.getConstructionCode(),
                            material.getConstructionVersion()), ignored -> new LinkedHashMap<>())
                    .merge(material.getClothCode(), value(material.getUnitConsumeMillimeter()), BigDecimal::add);
        }
        return result;
    }

    private String constructionKey(String constructionCode, String constructionVersion) {
        return constructionCode + "|" + constructionVersion;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class DemandAccumulator {
        private final String clothCode;
        private final String classField;
        private final LocalDateTime startTime;
        private BigDecimal formingQuantity = BigDecimal.ZERO;
        private BigDecimal clothDemandQuantity = BigDecimal.ZERO;

        private DemandAccumulator(String clothCode, String classField, LocalDateTime startTime) {
            this.clothCode = clothCode;
            this.classField = classField;
            this.startTime = startTime;
        }

        private void add(BigDecimal quantity, BigDecimal unitConsumeMillimeter) {
            formingQuantity = formingQuantity.add(quantity);
            // 施工单耗以毫米/条保存，需求统一转换为米后参与库存和排程计算。
            clothDemandQuantity = clothDemandQuantity.add(quantity.multiply(unitConsumeMillimeter)
                    .divide(MILLIMETERS_PER_METER, 10, RoundingMode.HALF_UP));
        }

        private Cd90DemandShift toDemandShift() {
            return Cd90DemandShift.builder()
                    .clothCode(clothCode)
                    .classField(classField)
                    .shiftKey(clothCode + "|" + startTime)
                    .startTime(startTime)
                    .formingQuantity(normalize(formingQuantity))
                    .clothDemandQuantity(normalize(clothDemandQuantity))
                    .shiftHours(SHIFT_HOURS)
                    .included(true)
                    .stopped(formingQuantity.signum() <= 0)
                    .build();
        }

        private BigDecimal normalize(BigDecimal value) {
            BigDecimal normalized = value.stripTrailingZeros();
            return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
        }
    }
}
