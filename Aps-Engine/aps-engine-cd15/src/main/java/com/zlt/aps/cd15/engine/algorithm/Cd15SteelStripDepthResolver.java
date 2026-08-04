package com.zlt.aps.cd15.engine.algorithm;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
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
 * 根据有效成型计划统计每个钢带的供成型机台数，并匹配备库深度配置。
 */
@Slf4j
@Component
public class Cd15SteelStripDepthResolver {

    /**
     * 解析逐钢带备库深度。
     *
     * @param schedules 成型排程窄模型
     * @param materials 施工分解后的钢带材料
     * @param configs 当前工厂的备库深度配置
     * @return 钢带代码到备库班数的稳定映射
     */
    public Map<String, BigDecimal> resolve(List<Cd15FormingScheduleSource> schedules,
                                           List<Cd15ConstructionMaterial> materials,
                                           List<Cd15DepthConfig> configs) {
        Map<String, Set<String>> steelStripsByConstruction = this.groupSteelStripsByConstruction(materials);
        Map<String, Set<String>> machinesBySteelStrip = new TreeMap<>();
        Set<String> missingMachineSteelStrips = new TreeSet<>();

        this.safe(schedules).stream()
                .filter(schedule -> schedule != null)
                .forEach(schedule -> this.collectScheduleMachines(schedule, steelStripsByConstruction,
                        machinesBySteelStrip, missingMachineSteelStrips));
        if (!missingMachineSteelStrips.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18nUtil.getMessage("ui.data.column.cd15DepthConfig.missingMachine"),
                    missingMachineSteelStrips));
        }

        List<Cd15DepthConfig> validConfigs = this.validateAndSortConfigs(configs);
        Map<String, BigDecimal> depthBySteelStrip = machinesBySteelStrip.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> this.matchDepth(entry.getKey(), entry.getValue().size(), validConfigs),
                        (first, second) -> first, LinkedHashMap::new));
        log.info("[斜裁自动排程] 逐钢带备库深度解析完成, clothCount={}, machineCountBySteelStrip={}, depthBySteelStrip={}",
                depthBySteelStrip.size(), machinesBySteelStrip.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().size(),
                        (first, second) -> first, LinkedHashMap::new)), depthBySteelStrip);
        return Collections.unmodifiableMap(depthBySteelStrip);
    }

    /** 按胎胚代码和施工版本汇总可用钢带代码。 */
    private Map<String, Set<String>> groupSteelStripsByConstruction(List<Cd15ConstructionMaterial> materials) {
        return this.safe(materials).stream()
                .filter(material -> material != null
                        && StringUtils.hasText(material.getConstructionCode())
                        && StringUtils.hasText(material.getConstructionVersion())
                        && StringUtils.hasText(material.getSteelStripCode()))
                .collect(Collectors.groupingBy(material -> this.constructionKey(
                                material.getConstructionCode(), material.getConstructionVersion()),
                        LinkedHashMap::new,
                        Collectors.mapping(Cd15ConstructionMaterial::getSteelStripCode,
                                Collectors.toCollection(LinkedHashSet::new))));
    }

    /** 只统计计划量大于0且施工匹配成功的成型班次。 */
    private void collectScheduleMachines(Cd15FormingScheduleSource schedule,
                                         Map<String, Set<String>> steelStripsByConstruction,
                                         Map<String, Set<String>> machinesBySteelStrip,
                                         Set<String> missingMachineSteelStrips) {
        List<BigDecimal> quantities = this.safe(schedule.getClassPlanQuantities());
        List<String> recipeNos = this.safe(schedule.getClassRecipeNos());
        IntStream.range(0, Math.min(8, quantities.size()))
                .filter(index -> this.value(quantities.get(index)).signum() > 0)
                .filter(index -> index < recipeNos.size() && StringUtils.hasText(recipeNos.get(index)))
                .mapToObj(index -> steelStripsByConstruction.get(this.constructionKey(
                        schedule.getEmbryoCode(), recipeNos.get(index))))
                .filter(steelStripCodes -> steelStripCodes != null && !steelStripCodes.isEmpty())
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

    /** 深度配置基础字段非法时立即终止，避免错误配置被静默跳过。 */
    private List<Cd15DepthConfig> validateAndSortConfigs(List<Cd15DepthConfig> configs) {
        List<Cd15DepthConfig> values = this.safe(configs);
        values.stream().forEach(config -> {
            if (config == null || config.getMinMachineQty() == null
                    || config.getMinMachineQty() <= 0
                    || (config.getMaxMachineQty() != null
                    && config.getMaxMachineQty() < config.getMinMachineQty())
                    || config.getDepthClassQty() == null || config.getDepthClassQty().signum() <= 0) {
                throw new IllegalArgumentException(
                        I18nUtil.getMessage("ui.data.column.cd15DepthConfig.invalidRange"));
            }
        });
        return values.stream()
                .sorted(java.util.Comparator.comparing(Cd15DepthConfig::getMinMachineQty))
                .collect(Collectors.toList());
    }

    /** 每个钢带必须且只能命中一条配置，禁止默认深度掩盖基础数据问题。 */
    private BigDecimal matchDepth(String steelStripCode, int machineCount, List<Cd15DepthConfig> configs) {
        List<Cd15DepthConfig> matches = configs.stream()
                .filter(config -> this.matches(config, machineCount))
                .collect(Collectors.toList());
        if (matches.size() != 1) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18nUtil.getMessage("ui.data.column.cd15DepthConfig.matchCount"),
                    steelStripCode, machineCount, matches.size()));
        }
        return matches.get(0).getDepthClassQty();
    }

    /** 判断成型机台数是否落在配置闭区间内，上限为空表示无上限。 */
    private boolean matches(Cd15DepthConfig config, int machineCount) {
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
