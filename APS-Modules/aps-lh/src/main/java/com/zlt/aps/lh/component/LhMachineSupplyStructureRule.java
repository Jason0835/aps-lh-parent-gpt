package com.zlt.aps.lh.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.entity.config.CxLhMachineSupplyConfig;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.mapper.CxLhMachineSupplyConfigMapper;
import com.zlt.aps.lh.mapper.MdmCxMachineFixedMapper;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 硫化机专供结构硬约束。
 *
 * <p>基础数据初始化阶段建立“产品结构→成型机集合”和“成型机→专供硫化机集合”索引，
 * 只有产品结构唯一绑定一台成型机时才启用专供硫化机硬约束。机台视角选SKU时只读取
 * 批次内存索引，不在候选循环中访问数据库。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class LhMachineSupplyStructureRule {

    /** 固定结构配置分隔符。 */
    private static final String STRUCTURE_SEPARATOR = ",";

    @Resource
    private CxLhMachineSupplyConfigMapper machineSupplyConfigMapper;

    @Resource
    private MdmCxMachineFixedMapper cxMachineFixedMapper;

    /**
     * 加载并建立当前工厂的结构、成型机和专供硫化机索引。
     *
     * @param context 排程上下文
     * @param factoryCode 当前工厂编号
     */
    public void loadAndAttach(LhScheduleContext context, String factoryCode) {
        List<CxLhMachineSupplyConfig> supplyConfigList = machineSupplyConfigMapper.selectList(
                new LambdaQueryWrapper<CxLhMachineSupplyConfig>()
                        .eq(CxLhMachineSupplyConfig::getIsActive, YesOrNoEnum.YES.getValue()));
        List<MdmCxMachineFixed> fixedMachineList = cxMachineFixedMapper.selectList(
                new LambdaQueryWrapper<MdmCxMachineFixed>()
                        .eq(MdmCxMachineFixed::getFactoryCode, factoryCode));

        Map<String, Set<String>> formingMachineSetByStructure = this.buildFormingMachineSetByStructure(
                fixedMachineList);
        Set<String> currentFactoryMachineCodeSet = context.getMachineInfoMap().keySet().stream()
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Set<String>> supplyLhMachineSetByFormingMachine = new LinkedHashMap<>(16);
        if (!CollectionUtils.isEmpty(supplyConfigList)) {
            supplyConfigList.stream()
                    .filter(Objects::nonNull)
                    .filter(config -> Objects.equals(
                            YesOrNoEnum.YES.getValue(), config.getIsActive()))
                    .filter(config -> StringUtils.isNotEmpty(config.getLhMachineCode()))
                    .filter(config -> StringUtils.isNotEmpty(config.getCxMachineCode()))
                    .filter(config -> currentFactoryMachineCodeSet.contains(
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                                    config.getLhMachineCode())))
                    .forEach(config -> this.registerSupplyMachine(
                            config, supplyLhMachineSetByFormingMachine));
        }
        context.setStructureFormingMachineMap(formingMachineSetByStructure);
        context.setFormingMachineSupplyLhMachineMap(supplyLhMachineSetByFormingMachine);
        this.logUniqueFormingMachineWithoutSupply(
                factoryCode, formingMachineSetByStructure, supplyLhMachineSetByFormingMachine);
        long restrictedStructureCount = formingMachineSetByStructure.values().stream()
                .filter(formingMachineSet -> formingMachineSet.size() == 1)
                .map(formingMachineSet -> formingMachineSet.iterator().next())
                .filter(supplyLhMachineSetByFormingMachine::containsKey)
                .count();
        log.info("[专供结构约束] 初始化完成, factoryCode: {}, 结构数: {}, 受限结构数: {}, 专供关系数: {}",
                factoryCode, formingMachineSetByStructure.size(), restrictedStructureCount,
                supplyLhMachineSetByFormingMachine.values().stream()
                        .mapToInt(Set::size).sum());
    }

    /**
     * 获取指定结构关联的成型机集合。
     *
     * @param context 排程上下文
     * @param structureCode 产品结构
     * @return 成型机集合；结构为空或没有有效固定配置时返回空集合
     */
    public Set<String> getFormingMachines(LhScheduleContext context, String structureCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(structureCode)
                || CollectionUtils.isEmpty(context.getStructureFormingMachineMap())) {
            return Collections.emptySet();
        }
        Set<String> formingMachines = context.getStructureFormingMachineMap()
                .get(structureCode.trim());
        return CollectionUtils.isEmpty(formingMachines)
                ? Collections.emptySet() : formingMachines;
    }

    /**
     * 获取指定结构允许使用的专供硫化机集合。
     *
     * @param context 排程上下文
     * @param structureCode 产品结构
     * @return 结构唯一绑定成型机时对应的物理硫化机集合；其他情况返回空集合
     */
    public Set<String> getAllowedLhMachines(LhScheduleContext context, String structureCode) {
        Set<String> formingMachines = this.getFormingMachines(context, structureCode);
        if (formingMachines.size() != 1
                || CollectionUtils.isEmpty(context.getFormingMachineSupplyLhMachineMap())) {
            return Collections.emptySet();
        }
        String formingMachine = formingMachines.iterator().next();
        Set<String> allowedLhMachines = context.getFormingMachineSupplyLhMachineMap()
                .get(formingMachine);
        return CollectionUtils.isEmpty(allowedLhMachines)
                ? Collections.emptySet() : allowedLhMachines;
    }

    /**
     * 判断当前硫化机是否允许选择指定产品结构。
     *
     * @param context 排程上下文
     * @param machineCode 运行态硫化机编码
     * @param structureCode 产品结构
     * @return true-结构未唯一绑定成型机、唯一成型机无专供配置或当前机台在允许范围；false-必须硬过滤
     */
    public boolean canMachineSelectStructure(
            LhScheduleContext context, String machineCode, String structureCode) {
        Set<String> allowedLhMachines = this.getAllowedLhMachines(context, structureCode);
        if (CollectionUtils.isEmpty(allowedLhMachines)) {
            return true;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return StringUtils.isNotEmpty(physicalMachineCode)
                && allowedLhMachines.contains(physicalMachineCode);
    }

    /**
     * 按产品结构汇总成型机集合。
     *
     * @param fixedMachineList 当前工厂成型固定机台配置
     * @return 产品结构到去重成型机集合的索引
     */
    private Map<String, Set<String>> buildFormingMachineSetByStructure(
            List<MdmCxMachineFixed> fixedMachineList) {
        if (CollectionUtils.isEmpty(fixedMachineList)) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> formingMachineSetByStructure = new LinkedHashMap<>(16);
        fixedMachineList.stream()
                .filter(Objects::nonNull)
                .filter(fixedMachine -> StringUtils.isNotEmpty(fixedMachine.getCxMachineCode()))
                .forEach(fixedMachine -> {
                    Set<String> structureSet = this.parseStructures(fixedMachine.getFixedStructure1());
                    if (!CollectionUtils.isEmpty(structureSet)) {
                        String cxMachineCode = fixedMachine.getCxMachineCode().trim()
                                .toUpperCase(Locale.ROOT);
                        structureSet.forEach(structure -> formingMachineSetByStructure
                                .computeIfAbsent(structure, key -> new LinkedHashSet<>())
                                .add(cxMachineCode));
                    }
                });
        return formingMachineSetByStructure;
    }

    /**
     * 将一条有效专供关系合并到成型机索引。
     *
     * @param config 专供关系配置
     * @param supplyLhMachineSetByFormingMachine 成型机到物理硫化机集合的索引
     */
    private void registerSupplyMachine(
            CxLhMachineSupplyConfig config,
            Map<String, Set<String>> supplyLhMachineSetByFormingMachine) {
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                config.getLhMachineCode());
        String cxMachineCode = config.getCxMachineCode().trim().toUpperCase(Locale.ROOT);
        if (StringUtils.isEmpty(physicalMachineCode)) {
            return;
        }
        supplyLhMachineSetByFormingMachine.computeIfAbsent(
                cxMachineCode, key -> new LinkedHashSet<>()).add(physicalMachineCode);
    }

    /**
     * 记录唯一成型机没有有效专供硫化机的配置缺口，此类结构保持原选SKU逻辑。
     *
     * @param factoryCode 当前工厂编号
     * @param formingMachineSetByStructure 产品结构到成型机集合的索引
     * @param supplyLhMachineSetByFormingMachine 成型机到物理硫化机集合的索引
     */
    private void logUniqueFormingMachineWithoutSupply(
            String factoryCode,
            Map<String, Set<String>> formingMachineSetByStructure,
            Map<String, Set<String>> supplyLhMachineSetByFormingMachine) {
        Map<String, Set<String>> structureSetByMissingFormingMachine = new LinkedHashMap<>(16);
        formingMachineSetByStructure.forEach((structure, formingMachineSet) -> {
            if (formingMachineSet.size() == 1) {
                String formingMachine = formingMachineSet.iterator().next();
                if (CollectionUtils.isEmpty(
                        supplyLhMachineSetByFormingMachine.get(formingMachine))) {
                    structureSetByMissingFormingMachine.computeIfAbsent(
                            formingMachine, key -> new LinkedHashSet<>()).add(structure);
                }
            }
        });
        structureSetByMissingFormingMachine.forEach((formingMachine, structureSet) ->
                log.info("[专供结构约束] 唯一成型机未配置有效专供硫化机，保持原选SKU逻辑, "
                                + "factoryCode: {}, formingMachine: {}, structures: {}",
                        factoryCode, formingMachine, structureSet));
    }

    /**
     * 拆分并清洗固定结构1配置。
     *
     * @param fixedStructure 固定结构1原始配置
     * @return 去空、去重后的结构集合
     */
    private Set<String> parseStructures(String fixedStructure) {
        if (StringUtils.isEmpty(fixedStructure)) {
            return Collections.emptySet();
        }
        return Arrays.stream(fixedStructure.split(STRUCTURE_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
