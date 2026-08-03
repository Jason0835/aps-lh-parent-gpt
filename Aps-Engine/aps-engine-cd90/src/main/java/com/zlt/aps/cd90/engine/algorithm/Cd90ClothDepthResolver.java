package com.zlt.aps.cd90.engine.algorithm;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 根据有效成型计划统计每个帘布的供成型机台数，并匹配备库深度配置。
 */
@Slf4j
@Component
public class Cd90ClothDepthResolver {

    /**
     * 解析逐帘布备库深度。
     *
     * @param schedules 成型排程窄模型
     * @param materials 施工分解后的帘布材料
     * @param configs 当前工厂的备库深度配置
     * @return 帘布代码到备库班数的稳定映射
     */
    public Map<String, BigDecimal> resolve(List<Cd90FormingScheduleSource> schedules,
                                           List<Cd90ConstructionMaterial> materials,
                                           List<Cd90DepthConfig> configs) {
        Map<String, Set<String>> clothsByConstruction = this.groupClothsByConstruction(materials);
        Map<String, Set<String>> machinesByCloth = new TreeMap<>();
        Set<String> missingMachineCloths = new TreeSet<>();

        this.safe(schedules).stream()
                .filter(schedule -> schedule != null)
                .forEach(schedule -> this.collectScheduleMachines(schedule, clothsByConstruction,
                        machinesByCloth, missingMachineCloths));
        if (!missingMachineCloths.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18nUtil.getMessage("ui.data.column.cd90DepthConfig.missingMachine"),
                    missingMachineCloths));
        }

        List<Cd90DepthConfig> validConfigs = this.validateAndSortConfigs(configs);
        Map<String, BigDecimal> depthByCloth = machinesByCloth.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> this.matchDepth(entry.getKey(), entry.getValue().size(), validConfigs),
                        (first, second) -> first, LinkedHashMap::new));
        log.info("[直裁自动排程] 逐帘布备库深度解析完成, clothCount={}, machineCountByCloth={}, depthByCloth={}",
                depthByCloth.size(), machinesByCloth.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().size(),
                        (first, second) -> first, LinkedHashMap::new)), depthByCloth);
        return Collections.unmodifiableMap(depthByCloth);
    }

    /** 按胎胚代码和施工版本汇总可用帘布代码。 */
    private Map<String, Set<String>> groupClothsByConstruction(List<Cd90ConstructionMaterial> materials) {
        return this.safe(materials).stream()
                .filter(material -> material != null
                        && StringUtils.hasText(material.getConstructionCode())
                        && StringUtils.hasText(material.getConstructionVersion())
                        && StringUtils.hasText(material.getClothCode()))
                .collect(Collectors.groupingBy(material -> this.constructionKey(
                                material.getConstructionCode(), material.getConstructionVersion()),
                        LinkedHashMap::new,
                        Collectors.mapping(Cd90ConstructionMaterial::getClothCode,
                                Collectors.toCollection(LinkedHashSet::new))));
    }

    /** 只统计计划量大于0且施工匹配成功的成型班次。 */
    private void collectScheduleMachines(Cd90FormingScheduleSource schedule,
                                         Map<String, Set<String>> clothsByConstruction,
                                         Map<String, Set<String>> machinesByCloth,
                                         Set<String> missingMachineCloths) {
        List<BigDecimal> quantities = this.safe(schedule.getClassPlanQuantities());
        List<String> recipeNos = this.safe(schedule.getClassRecipeNos());
        IntStream.range(0, Math.min(8, quantities.size()))
                .filter(index -> this.value(quantities.get(index)).signum() > 0)
                .filter(index -> index < recipeNos.size() && StringUtils.hasText(recipeNos.get(index)))
                .mapToObj(index -> clothsByConstruction.get(this.constructionKey(
                        schedule.getEmbryoCode(), recipeNos.get(index))))
                .filter(clothCodes -> clothCodes != null && !clothCodes.isEmpty())
                .flatMap(Set::stream)
                .forEach(clothCode -> {
                    if (!StringUtils.hasText(schedule.getCxMachineCode())) {
                        missingMachineCloths.add(clothCode);
                    } else {
                        machinesByCloth.computeIfAbsent(clothCode, ignored -> new TreeSet<>())
                                .add(schedule.getCxMachineCode().trim());
                    }
                });
    }

    /** 深度配置基础字段非法时立即终止，避免错误配置被静默跳过。 */
    private List<Cd90DepthConfig> validateAndSortConfigs(List<Cd90DepthConfig> configs) {
        List<Cd90DepthConfig> values = this.safe(configs);
        values.stream().forEach(config -> {
            if (config == null || config.getMinMachineQty() == null
                    || config.getMinMachineQty() <= 0
                    || (config.getMaxMachineQty() != null
                    && config.getMaxMachineQty() < config.getMinMachineQty())
                    || config.getDepthClassQty() == null || config.getDepthClassQty().signum() <= 0) {
                throw new IllegalArgumentException(
                        I18nUtil.getMessage("ui.data.column.cd90DepthConfig.invalidRange"));
            }
        });
        return values.stream()
                .sorted(java.util.Comparator.comparing(Cd90DepthConfig::getMinMachineQty))
                .collect(Collectors.toList());
    }

    /** 每个帘布必须且只能命中一条配置，禁止默认深度掩盖基础数据问题。 */
    private BigDecimal matchDepth(String clothCode, int machineCount, List<Cd90DepthConfig> configs) {
        List<Cd90DepthConfig> matches = configs.stream()
                .filter(config -> this.matches(config, machineCount))
                .collect(Collectors.toList());
        if (matches.size() != 1) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18nUtil.getMessage("ui.data.column.cd90DepthConfig.matchCount"),
                    clothCode, machineCount, matches.size()));
        }
        return matches.get(0).getDepthClassQty();
    }

    /** 判断成型机台数是否落在配置闭区间内，上限为空表示无上限。 */
    private boolean matches(Cd90DepthConfig config, int machineCount) {
        return machineCount >= config.getMinMachineQty()
                && (config.getMaxMachineQty() == null
                || machineCount <= config.getMaxMachineQty());
    }

    private String constructionKey(String constructionCode, String constructionVersion) {
        return constructionCode + "|" + constructionVersion;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
