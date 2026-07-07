package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15NaturalDemand;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CD15 成型逐班自然需求候选构建器。
 */
@Component
public class Cd15ScheduleCandidateBuilder {

    private static final int CLASS_COUNT = 8;

    /**
     * 根据成型结果的 class1~class8 计划量与施工版本，动态展开自然需求。
     *
     * @param input 自动排程输入
     * @return 成型自然需求列表
     */
    public List<Cd15NaturalDemand> buildNaturalDemands(Cd15AutoScheduleInput input) {
        List<CxScheduleResult> formingSchedules = input == null || input.getFormingSchedules() == null
                ? Collections.emptyList() : input.getFormingSchedules();
        return formingSchedules.stream()
                .filter(Objects::nonNull)
                .flatMap(schedule -> IntStream.rangeClosed(1, CLASS_COUNT)
                        .mapToObj(classIndex -> this.toNaturalDemand(schedule, classIndex)))
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
        return this.buildNaturalDemands(input).stream()
                .flatMap(demand -> materials.stream()
                        .filter(material -> this.match(demand, material))
                        .map(material -> this.toCandidate(demand, material, stockMetersBySteelStrip)))
                .collect(Collectors.toList());
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
                                              Map<String, BigDecimal> stockMetersBySteelStrip) {
        BigDecimal stockMetersAtSix = stockMetersBySteelStrip.getOrDefault(
                material.getSteelStripCode(), BigDecimal.ZERO);
        return Cd15ScheduleCandidate.builder()
                .demand(demand)
                .material(material)
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