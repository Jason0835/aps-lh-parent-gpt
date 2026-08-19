package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
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
 * 将成型排程宽表和施工层位转换为钢带逐自然班次需求明细。
 */
@Component
@Slf4j
public class Cd15FormingDemandExpander {

    private static final BigDecimal MILLIMETERS_PER_METER = new BigDecimal("1000");
    private static final BigDecimal SHIFT_HOURS = new BigDecimal("8");
    private static final LocalTime FIRST_SHIFT_TIME = LocalTime.of(6, 0);

    /**
     * 按施工材料键和自然班次汇总成型条数及钢带需求米数。
     *
     * @param schedules 成型排程窄模型
     * @param materials 施工层位钢带单耗
     * @return 按钢带、角度、工艺尺寸和班次排序的需求明细
     */
    public List<Cd15DemandShift> expand(List<Cd15FormingScheduleSource> schedules,
                                        List<Cd15ConstructionMaterial> materials) {
        Map<String, List<Cd15ConstructionMaterial>> materialsByConstruction =
                this.groupMaterialsByConstruction(materials);
        Map<String, DemandAccumulator> demandByMaterialAndShift = new LinkedHashMap<>();

        for (Cd15FormingScheduleSource schedule : this.safe(schedules)) {
            if (schedule == null || schedule.getScheduleDate() == null) {
                continue;
            }
            List<BigDecimal> quantities = this.safe(schedule.getClassPlanQuantities());
            List<String> recipeNos = this.safe(schedule.getClassRecipeNos());
            for (int classIndex = 0; classIndex < Math.min(8, quantities.size()); classIndex++) {
                BigDecimal formingQuantity = this.value(quantities.get(classIndex));
                LocalDateTime startTime = schedule.getScheduleDate().minusDays(1)
                        .atTime(FIRST_SHIFT_TIME).plusHours(classIndex * 8L);
                String classField = "CLASS" + (classIndex + 1);
                String recipeNo = classIndex < recipeNos.size() ? recipeNos.get(classIndex) : null;
                if (!StringUtils.hasText(recipeNo)) {
                    if (formingQuantity.signum() > 0) {
                        log.warn("[斜裁自动排程] 成型班次施工版本为空，跳过该班施工分解, "
                                        + "cxBatchNo={}, embryoCode={}, classField={}, startTime={}",
                                schedule.getCxBatchNo(), schedule.getEmbryoCode(), classField, startTime);
                    }
                    continue;
                }
                List<Cd15ConstructionMaterial> constructionMaterials = materialsByConstruction.get(
                        this.constructionKey(schedule.getEmbryoCode(), recipeNo));
                if (constructionMaterials == null || constructionMaterials.isEmpty()) {
                    if (formingQuantity.signum() > 0) {
                        log.warn("[斜裁自动排程] 未找到胎胚施工版本，跳过该班施工分解, "
                                        + "cxBatchNo={}, embryoCode={}, constructionVersion={}, classField={}",
                                schedule.getCxBatchNo(), schedule.getEmbryoCode(), recipeNo, classField);
                    }
                    continue;
                }
                for (Cd15ConstructionMaterial material : constructionMaterials) {
                    String materialKey = this.materialKey(material);
                    String key = materialKey + "|" + startTime;
                    DemandAccumulator accumulator = demandByMaterialAndShift.computeIfAbsent(key,
                            ignored -> new DemandAccumulator(materialKey, material, classField, startTime));
                    accumulator.add(formingQuantity);
                }
            }
        }

        return demandByMaterialAndShift.values().stream()
                .map(DemandAccumulator::toDemandShift)
                .sorted(Comparator.comparing(Cd15DemandShift::getSteelStripCode)
                        .thenComparing(Cd15DemandShift::getCuttingAngle,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(Cd15DemandShift::getBigRollCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(Cd15DemandShift::getCraftWidth,
                                Comparator.nullsLast(BigDecimal::compareTo))
                        .thenComparing(Cd15DemandShift::getStartTime))
                .collect(Collectors.toList());
    }

    private Map<String, List<Cd15ConstructionMaterial>> groupMaterialsByConstruction(
            List<Cd15ConstructionMaterial> materials) {
        return this.safe(materials).stream()
                .filter(material -> material != null
                        && StringUtils.hasText(material.getConstructionCode())
                        && StringUtils.hasText(material.getConstructionVersion())
                        && StringUtils.hasText(material.getSteelStripCode()))
                .collect(Collectors.groupingBy(material -> this.constructionKey(
                                material.getConstructionCode(), material.getConstructionVersion()),
                        LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 同班次、同钢带、同大卷、同角度及同工艺尺寸使用同一需求键。
     * 左右加强层同码且工艺尺寸一致时键相同，形成量会自然累计两次。
     */
    private String materialKey(Cd15ConstructionMaterial material) {
        return this.text(material.getSteelStripCode()) + "|"
                + this.text(material.getBigRollCode()) + "|"
                + this.text(material.getCuttingAngle()) + "|"
                + this.decimalText(material.getCraftWidth()) + "|"
                + this.decimalText(material.getUnitConsumeMillimeter()) + "|"
                + this.decimalText(material.getCurlLength());
    }

    private String constructionKey(String constructionCode, String constructionVersion) {
        return constructionCode + "|" + constructionVersion;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String decimalText(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class DemandAccumulator {
        private final String materialKey;
        private final Cd15ConstructionMaterial material;
        private final String classField;
        private final LocalDateTime startTime;
        private BigDecimal formingQuantity = BigDecimal.ZERO;
        private BigDecimal steelStripDemandQuantity = BigDecimal.ZERO;

        private DemandAccumulator(String materialKey,
                                  Cd15ConstructionMaterial material,
                                  String classField,
                                  LocalDateTime startTime) {
            this.materialKey = materialKey;
            this.material = material;
            this.classField = classField;
            this.startTime = startTime;
        }

        private void add(BigDecimal quantity) {
            BigDecimal unitConsume = material.getUnitConsumeMillimeter() == null
                    ? BigDecimal.ZERO : material.getUnitConsumeMillimeter();
            formingQuantity = formingQuantity.add(quantity);
            steelStripDemandQuantity = steelStripDemandQuantity.add(quantity.multiply(unitConsume)
                    .divide(MILLIMETERS_PER_METER, 10, RoundingMode.HALF_UP));
        }

        private Cd15DemandShift toDemandShift() {
            return Cd15DemandShift.builder()
                    .steelStripCode(material.getSteelStripCode())
                    .materialKey(materialKey)
                    .bigRollCode(material.getBigRollCode())
                    .cuttingAngle(material.getCuttingAngle())
                    .craftWidth(material.getCraftWidth())
                    .unitConsumeMillimeter(material.getUnitConsumeMillimeter())
                    .cordWidth(material.getCordWidth())
                    .curlLength(material.getCurlLength())
                    .classField(classField)
                    .shiftKey(materialKey + "|" + startTime)
                    .startTime(startTime)
                    .formingQuantity(this.normalize(formingQuantity))
                    .steelStripDemandQuantity(this.normalize(steelStripDemandQuantity))
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
