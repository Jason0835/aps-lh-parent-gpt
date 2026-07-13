package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ClothSourceTrace;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90EmbryoPlanSurplus;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 解析实际参与帘布需求的成型批次、机台及月计划剩余量。 */
@Component
public class Cd90ClothSourceTraceResolver {

    private static final int CX_BATCH_NO_MAX_LENGTH = 500;
    private static final int CX_MACHINE_CODES_MAX_LENGTH = 300;

    /**
     * 按帘布生成来源追溯信息。
     *
     * @param formingSchedules 成型计划
     * @param constructionMaterials 施工BOM帘布材料
     * @param embryoPlanSurpluses 胎胚月计划剩余量
     * @return 以帘布代码为键的来源追溯信息
     */
    public Map<String, Cd90ClothSourceTrace> resolve(
            List<Cd90FormingScheduleSource> formingSchedules,
            List<Cd90ConstructionMaterial> constructionMaterials,
            List<Cd90EmbryoPlanSurplus> embryoPlanSurpluses) {
        Map<ConstructionKey, List<Cd90ConstructionMaterial>> materialsByConstruction =
                safe(constructionMaterials).stream()
                        .filter(item -> item != null
                                && StringUtils.hasText(item.getConstructionCode())
                                && StringUtils.hasText(item.getConstructionVersion())
                                && StringUtils.hasText(item.getClothCode()))
                        .collect(Collectors.groupingBy(
                                item -> new ConstructionKey(item.getConstructionCode(),
                                        item.getConstructionVersion())));

        Map<String, SourceAccumulator> accumulators = new LinkedHashMap<>();
        safe(formingSchedules).stream()
                .filter(schedule -> schedule != null
                        && StringUtils.hasText(schedule.getEmbryoCode()))
                .forEach(schedule -> collectScheduleSources(
                        schedule, materialsByConstruction, accumulators));

        Map<String, BigDecimal> surplusByEmbryo = safe(embryoPlanSurpluses).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getEmbryoCode())
                        && item.getPlanSurplusQuantity() != null)
                .collect(Collectors.toMap(
                        item -> item.getEmbryoCode().trim(),
                        Cd90EmbryoPlanSurplus::getPlanSurplusQuantity,
                        (first, second) -> first));

        return accumulators.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> buildTrace(entry.getKey(), entry.getValue(), surplusByEmbryo),
                (first, second) -> first,
                LinkedHashMap::new));
    }

    /** 只有正计划量及其对应CLASS配方命中的施工帘布才计入来源。 */
    private void collectScheduleSources(
            Cd90FormingScheduleSource schedule,
            Map<ConstructionKey, List<Cd90ConstructionMaterial>> materialsByConstruction,
            Map<String, SourceAccumulator> accumulators) {
        List<BigDecimal> quantities = safe(schedule.getClassPlanQuantities());
        List<String> recipeNos = safe(schedule.getClassRecipeNos());
        String embryoCode = schedule.getEmbryoCode().trim();
        IntStream.range(0, Math.min(quantities.size(), recipeNos.size()))
                .filter(index -> quantities.get(index) != null
                        && quantities.get(index).signum() > 0
                        && StringUtils.hasText(recipeNos.get(index)))
                .mapToObj(index -> new ConstructionKey(embryoCode, recipeNos.get(index)))
                .flatMap(key -> materialsByConstruction
                        .getOrDefault(key, Collections.emptyList()).stream())
                .forEach(material -> accumulators
                        .computeIfAbsent(material.getClothCode().trim(),
                                ignored -> new SourceAccumulator())
                        .add(embryoCode, schedule.getCxBatchNo(),
                                schedule.getCxMachineCode()));
    }

    /** PLAN_SURPLUS_QTY按实际关联胎胚去重求和，任一胎胚缺失时保留空值。 */
    private Cd90ClothSourceTrace buildTrace(
            String clothCode,
            SourceAccumulator accumulator,
            Map<String, BigDecimal> surplusByEmbryo) {
        String cxBatchNo = joinAndValidate(
                accumulator.batchNos, CX_BATCH_NO_MAX_LENGTH, "CX_BATCH_NO");
        String cxMachineCodes = joinAndValidate(
                accumulator.machineCodes, CX_MACHINE_CODES_MAX_LENGTH, "CX_MACHINE_CODES");
        boolean missingSurplus = accumulator.distinctEmbryoCodes.stream()
                .anyMatch(embryoCode -> !surplusByEmbryo.containsKey(embryoCode)
                        || surplusByEmbryo.get(embryoCode) == null);
        if (missingSurplus) {
            return Cd90ClothSourceTrace.builder().clothCode(clothCode)
                    .cxBatchNo(cxBatchNo).cxMachineCodes(cxMachineCodes)
                    .planSurplusQty(null).build();
        }
        BigDecimal planSurplusQty = accumulator.distinctEmbryoCodes.stream()
                .map(surplusByEmbryo::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Cd90ClothSourceTrace.builder().clothCode(clothCode)
                .cxBatchNo(cxBatchNo).cxMachineCodes(cxMachineCodes)
                .planSurplusQty(planSurplusQty).build();
    }

    private String joinAndValidate(Set<String> values, int maxLength, String columnName) {
        String joined = values.isEmpty() ? null : String.join(",", values);
        if (joined != null && joined.length() > maxLength) {
            throw new IllegalArgumentException(columnName + "长度超过" + maxLength);
        }
        return joined;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /** 胎胚和施工版本组成的结构化关联键。 */
    private static final class ConstructionKey {
        private final String embryoCode;
        private final String constructionVersion;

        private ConstructionKey(String embryoCode, String constructionVersion) {
            this.embryoCode = embryoCode.trim();
            this.constructionVersion = constructionVersion.trim();
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) {
                return true;
            }
            if (!(value instanceof ConstructionKey)) {
                return false;
            }
            ConstructionKey other = (ConstructionKey) value;
            return Objects.equals(this.embryoCode, other.embryoCode)
                    && Objects.equals(this.constructionVersion, other.constructionVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.embryoCode, this.constructionVersion);
        }
    }

    private static final class SourceAccumulator {
        private final Set<String> batchNos = new TreeSet<>();
        private final Set<String> machineCodes = new TreeSet<>();
        private final Set<String> distinctEmbryoCodes = new TreeSet<>();

        private void add(String embryoCode, String batchNo, String machineCode) {
            this.distinctEmbryoCodes.add(embryoCode);
            if (StringUtils.hasText(batchNo)) {
                this.batchNos.add(batchNo.trim());
            }
            if (StringUtils.hasText(machineCode)) {
                this.machineCodes.add(machineCode.trim());
            }
        }
    }
}
