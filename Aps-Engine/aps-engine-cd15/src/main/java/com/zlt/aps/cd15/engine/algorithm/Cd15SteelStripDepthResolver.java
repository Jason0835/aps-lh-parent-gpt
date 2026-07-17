package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.common.engine.enums.MachineRangeEnum;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 根据有效成型计划统计每条钢带的供成型机台数并匹配备库深度。
 */
@Component
public class Cd15SteelStripDepthResolver {

    public Map<String, BigDecimal> resolve(List<CxScheduleResult> schedules,
                                           List<Cd15ConstructionMaterial> materials,
                                           List<Cd15ShiftDescriptor> shifts,
                                           List<Cd15DepthConfig> configs) {
        Map<String, Set<String>> steelStripsByConstruction = this.groupSteelStripsByConstruction(materials);
        Map<String, Set<String>> machinesBySteelStrip = new TreeMap<>();
        Set<String> missingMachineSteelStrips = new TreeSet<>();
        this.safe(schedules).stream()
                .filter(schedule -> schedule != null)
                .forEach(schedule -> this.collectScheduleMachines(schedule, shifts,
                        steelStripsByConstruction, machinesBySteelStrip, missingMachineSteelStrips));
        if (!missingMachineSteelStrips.isEmpty()) {
            throw new IllegalArgumentException("存在正需求但成型机台代码为空的钢带: " + missingMachineSteelStrips);
        }
        List<Cd15DepthConfig> validConfigs = this.validateAndSortConfigs(configs);
        return Collections.unmodifiableMap(machinesBySteelStrip.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> this.matchDepth(entry.getKey(), entry.getValue().size(), validConfigs),
                        (first, second) -> first, LinkedHashMap::new)));
    }

    private Map<String, Set<String>> groupSteelStripsByConstruction(List<Cd15ConstructionMaterial> materials) {
        return this.safe(materials).stream()
                .filter(material -> material != null
                        && StringUtils.hasText(material.getConstructionCode())
                        && StringUtils.hasText(material.getConstructionVersion())
                        && StringUtils.hasText(material.getSteelStripCode()))
                .collect(Collectors.groupingBy(material -> this.constructionKey(
                                material.getConstructionCode(), material.getConstructionVersion()),
                        LinkedHashMap::new,
                        Collectors.mapping(material -> material.getSteelStripCode().trim(),
                                Collectors.toCollection(LinkedHashSet::new))));
    }

    private void collectScheduleMachines(CxScheduleResult schedule,
                                         List<Cd15ShiftDescriptor> shifts,
                                         Map<String, Set<String>> steelStripsByConstruction,
                                         Map<String, Set<String>> machinesBySteelStrip,
                                         Set<String> missingMachineSteelStrips) {
        this.safe(shifts).stream()
                .map(Cd15ShiftDescriptor::getClassIndex)
                .filter(classIndex -> this.readBigDecimal(schedule,
                        String.format("class%dPlanQty", classIndex)).signum() > 0)
                .map(classIndex -> this.readString(schedule, String.format("class%dRecipeNo", classIndex)))
                .filter(StringUtils::hasText)
                .map(version -> steelStripsByConstruction.get(this.constructionKey(
                        schedule.getEmbryoCode(), version)))
                .filter(codes -> codes != null && !codes.isEmpty())
                .flatMap(Set::stream)
                .forEach(steelStripCode -> {
                    if (!StringUtils.hasText(schedule.getCxMachineCode())) {
                        missingMachineSteelStrips.add(steelStripCode);
                    } else {
                        machinesBySteelStrip.computeIfAbsent(steelStripCode, ignored -> new TreeSet<>())
                                .add(schedule.getCxMachineCode().trim());
                    }
                });
    }

    private List<Cd15DepthConfig> validateAndSortConfigs(List<Cd15DepthConfig> configs) {
        List<Cd15DepthConfig> values = this.safe(configs);
        values.forEach(config -> {
            if (config == null || config.getMachineQty() == null
                    || MachineRangeEnum.getByCode(config.getMachineRange()) == null
                    || config.getDepthClassQty() == null || config.getDepthClassQty().signum() <= 0) {
                throw new IllegalArgumentException("斜裁备库深度配置存在无效机台范围、机台数或备库班数");
            }
        });
        return values.stream()
                .sorted(Comparator.comparing(Cd15DepthConfig::getMachineQty).reversed()
                        .thenComparing(Cd15DepthConfig::getMachineRange))
                .collect(Collectors.toList());
    }

    private BigDecimal matchDepth(String steelStripCode,
                                  int machineCount,
                                  List<Cd15DepthConfig> configs) {
        List<Cd15DepthConfig> matches = configs.stream()
                .filter(config -> MachineRangeEnum.getByCode(config.getMachineRange())
                        .matches(machineCount, config.getMachineQty()))
                .collect(Collectors.toList());
        if (matches.size() != 1) {
            throw new IllegalArgumentException("钢带备库深度必须唯一匹配, steelStripCode=" + steelStripCode
                    + ", formingMachineCount=" + machineCount + ", matchCount=" + matches.size());
        }
        return matches.get(0).getDepthClassQty();
    }

    private String constructionKey(String constructionCode, String constructionVersion) {
        return (constructionCode == null ? "" : constructionCode.trim()) + "|"
                + (constructionVersion == null ? "" : constructionVersion.trim());
    }

    private BigDecimal readBigDecimal(Object source, String fieldName) {
        Object value = this.readValue(source, fieldName);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private String readString(Object source, String fieldName) {
        Object value = this.readValue(source, fieldName);
        return value == null ? null : value.toString().trim();
    }

    private Object readValue(Object source, String fieldName) {
        if (source == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method method = source.getClass().getMethod(methodName);
            return method.invoke(source);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取字段失败: " + source.getClass().getSimpleName() + "." + fieldName,
                    exception);
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}