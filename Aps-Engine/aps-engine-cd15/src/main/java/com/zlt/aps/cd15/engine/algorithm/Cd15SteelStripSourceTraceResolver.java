package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15EmbryoPlanSurplus;
import com.zlt.aps.cd15.engine.model.Cd15NaturalDemand;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** 按实际成型需求和施工BOM解析钢带来源追溯信息。 */
@Component
@RequiredArgsConstructor
public class Cd15SteelStripSourceTraceResolver {

    private static final int CX_BATCH_NO_MAX_LENGTH = 500;
    private static final int CX_MACHINE_CODES_MAX_LENGTH = 300;

    private final Cd15ScheduleCandidateBuilder scheduleCandidateBuilder;

    /**
     * 按钢带生成成型批次、成型机台和胎胚月计划剩余量。
     *
     * @param input 自动排程输入
     * @param embryoPlanSurpluses 胎胚月计划剩余量
     * @return 以钢带代码为键的来源追溯信息
     */
    public Map<String, Cd15SteelStripSourceTrace> resolve(
            Cd15AutoScheduleInput input,
            List<Cd15EmbryoPlanSurplus> embryoPlanSurpluses) {
        List<Cd15ConstructionMaterial> materials = input == null
                || input.getConstructionMaterials() == null
                ? Collections.emptyList() : input.getConstructionMaterials();
        Map<ConstructionKey, List<Cd15ConstructionMaterial>> materialsByConstruction =
                materials.stream()
                        .filter(item -> item != null
                                && StringUtils.hasText(item.getConstructionCode())
                                && StringUtils.hasText(item.getConstructionVersion())
                                && StringUtils.hasText(item.getSteelStripCode()))
                        .collect(Collectors.groupingBy(item -> new ConstructionKey(
                                item.getConstructionCode(), item.getConstructionVersion())));

        Map<String, SourceAccumulator> accumulators = new LinkedHashMap<>();
        this.scheduleCandidateBuilder.buildNaturalDemands(input).stream()
                .forEach(demand -> materialsByConstruction
                        .getOrDefault(new ConstructionKey(
                                demand.getConstructionCode(), demand.getConstructionVersion()),
                                Collections.emptyList())
                        .forEach(material -> accumulators
                                .computeIfAbsent(material.getSteelStripCode().trim(),
                                        ignored -> new SourceAccumulator())
                                .add(demand)));

        Map<String, BigDecimal> surplusByEmbryo = this.safe(embryoPlanSurpluses).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getEmbryoCode())
                        && item.getPlanSurplusQuantity() != null)
                .collect(Collectors.toMap(
                        item -> item.getEmbryoCode().trim(),
                        Cd15EmbryoPlanSurplus::getPlanSurplusQuantity,
                        (first, second) -> first));

        return accumulators.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> this.buildTrace(entry.getKey(), entry.getValue(), surplusByEmbryo),
                (first, second) -> first,
                LinkedHashMap::new));
    }

    /** 根据字段长度限制生成稳定的逗号分隔值。 */
    private String joinAndValidate(Set<String> values, int maxLength, String columnName) {
        String joined = values.isEmpty() ? null : String.join(",", values);
        if (joined != null && joined.length() > maxLength) {
            throw new IllegalArgumentException(columnName + "长度超过" + maxLength);
        }
        return joined;
    }

    /** 生成单个钢带的来源追溯信息。 */
    private Cd15SteelStripSourceTrace buildTrace(
            String steelStripCode,
            SourceAccumulator accumulator,
            Map<String, BigDecimal> surplusByEmbryo) {
        String cxBatchNo = this.joinAndValidate(
                accumulator.batchNos, CX_BATCH_NO_MAX_LENGTH, "CX_BATCH_NO");
        String cxMachineCodes = this.joinAndValidate(
                accumulator.machineCodes, CX_MACHINE_CODES_MAX_LENGTH, "CX_MACHINE_CODES");
        boolean missingSurplus = accumulator.distinctEmbryoCodes.stream()
                .anyMatch(embryoCode -> !surplusByEmbryo.containsKey(embryoCode)
                        || surplusByEmbryo.get(embryoCode) == null);
        if (missingSurplus) {
            return Cd15SteelStripSourceTrace.builder().steelStripCode(steelStripCode)
                    .cxBatchNo(cxBatchNo).cxMachineCodes(cxMachineCodes)
                    .planSurplusQty(null).build();
        }
        BigDecimal planSurplusQty = accumulator.distinctEmbryoCodes.stream()
                .map(surplusByEmbryo::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Cd15SteelStripSourceTrace.builder().steelStripCode(steelStripCode)
                .cxBatchNo(cxBatchNo).cxMachineCodes(cxMachineCodes)
                .planSurplusQty(planSurplusQty).build();
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

    /** 单个钢带的来源集合累加器。 */
    private static final class SourceAccumulator {
        private final Set<String> batchNos = new TreeSet<>();
        private final Set<String> machineCodes = new TreeSet<>();
        private final Set<String> distinctEmbryoCodes = new TreeSet<>();

        private void add(Cd15NaturalDemand demand) {
            this.distinctEmbryoCodes.add(demand.getConstructionCode().trim());
            if (StringUtils.hasText(demand.getCxBatchNo())) {
                this.batchNos.add(demand.getCxBatchNo().trim());
            }
            if (StringUtils.hasText(demand.getCxMachineCode())) {
                this.machineCodes.add(demand.getCxMachineCode().trim());
            }
        }
    }
}
