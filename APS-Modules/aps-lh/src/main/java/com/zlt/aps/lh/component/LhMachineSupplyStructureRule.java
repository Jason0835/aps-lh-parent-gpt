package com.zlt.aps.lh.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.entity.config.CxLhMachineSupplyConfig;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.context.StructureDedicatedMachineContext;
import com.zlt.aps.lh.mapper.CxLhMachineSupplyConfigMapper;
import com.zlt.aps.lh.mapper.LhMpStructureAllocationMapper;
import com.zlt.aps.lh.mapper.LhMachineInfoMapper;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
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
 * 统一结构专供关系：月计划转产表 → 成型机 → 启用专供硫化机。
 * 全部配置为硬限制，部分配置仅优先；查询只发生在批次初始化阶段。
 *
 * @author APS
 */
@Slf4j
@Component
public class LhMachineSupplyStructureRule {
    /** 正常月计划类型，与既有结构转产查询保持一致。 */
    private static final String NORMAL_PLAN_TYPE = "01";
    /** 年月和排产版本共同隔离快照，禁止跨月合并成型机。 */
    private static final String PLAN_KEY_FORMAT = "%s_%s_%s";

    @Resource
    private CxLhMachineSupplyConfigMapper machineSupplyConfigMapper;
    @Resource
    private LhMpStructureAllocationMapper structureAllocationMapper;
    @Resource
    private LhMachineInfoMapper machineInfoMapper;

    /**
     * 初始化本工厂、本批月计划范围内的专供快照。
     * @param context 排程上下文，机台和月计划必须已加载
     * @param factoryCode 工厂编号
     */
    public void loadAndAttach(LhScheduleContext context, String factoryCode) {
        // 配置有效性与当前班次可排性分开，不能因暂时停机把强专供降为弱专供。
        this.loadSupplyMachines(context, factoryCode);
        Map<String, Map<String, StructureDedicatedMachineContext>> snapshots = new LinkedHashMap<>(4);
        // 与现有月计划日期解析一致：包含已加载的跨月计划，不能只读取筛选后的主月SKU。
        List<FactoryMonthPlanProductionFinalResult> loadedPlans = CollectionUtils.isEmpty(context.getLoadedMonthPlanList())
                ? context.getMonthPlanList() : context.getLoadedMonthPlanList();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> planGroups = loadedPlans.stream()
                .filter(plan -> Objects.nonNull(plan.getYear()) && Objects.nonNull(plan.getMonth()))
                .filter(plan -> StringUtils.isNotEmpty(plan.getProductionVersion()))
                .collect(Collectors.groupingBy(plan -> String.format(PLAN_KEY_FORMAT,
                        plan.getYear(), plan.getMonth(), plan.getProductionVersion()),
                        LinkedHashMap::new, Collectors.toList()));
        planGroups.forEach((planKey, plans) -> snapshots.put(planKey,
                this.loadStructureSnapshots(context, factoryCode, plans)));
        context.setStructureDedicatedMachineContextMap(snapshots);
        log.info("[专供结构约束] 初始化完成, factoryCode: {}, batchNo: {}, 月计划范围数: {}, 专供物理机数: {}",
                factoryCode, context.getBatchNo(), snapshots.size(), context.getSupplyFormingMachinesByLhMachineMap().size());
    }

    /**
     * 读取启用配置并建立双向集合索引；逻辑删除交由框架过滤。
     * @param context 本批排程上下文
     * @param factoryCode 工厂编号，仅用于归属过滤，不用机台启用状态改变专供分类
     */
    private void loadSupplyMachines(LhScheduleContext context, String factoryCode) {
        List<CxLhMachineSupplyConfig> configs = machineSupplyConfigMapper.selectList(
                new LambdaQueryWrapper<CxLhMachineSupplyConfig>()
                        .eq(CxLhMachineSupplyConfig::getIsActive, YesOrNoEnum.YES.getValue()));
        // 排程机台Map仅含启用机台；这里读取工厂归属，停用机台的有效专供配置也不能使结构降级。
        Set<String> factoryMachines = machineInfoMapper.selectList(new LambdaQueryWrapper<LhMachineInfo>()
                        .select(LhMachineInfo::getMachineCode)
                        .eq(LhMachineInfo::getFactoryCode, factoryCode)).stream()
                .map(LhMachineInfo::getMachineCode)
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
        Map<String, Set<String>> byFormingMachine = new LinkedHashMap<>(16);
        Map<String, Set<String>> byLhMachine = new LinkedHashMap<>(16);
        configs.stream().filter(Objects::nonNull)
                .filter(config -> Objects.equals(config.getIsActive(), YesOrNoEnum.YES.getValue()))
                .filter(config -> StringUtils.isNotEmpty(StringUtils.trim(config.getCxMachineCode())))
                .filter(config -> StringUtils.isNotEmpty(config.getLhMachineCode()))
                .filter(config -> factoryMachines.contains(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        config.getLhMachineCode())))
                .forEach(config -> {
                    String formingMachine = config.getCxMachineCode().trim().toUpperCase(Locale.ROOT);
                    String lhMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(config.getLhMachineCode());
                    byFormingMachine.computeIfAbsent(formingMachine, key -> new LinkedHashSet<>(4)).add(lhMachine);
                    byLhMachine.computeIfAbsent(lhMachine, key -> new LinkedHashSet<>(4)).add(formingMachine);
                });
        context.setFormingMachineSupplyLhMachineMap(byFormingMachine);
        context.setSupplyFormingMachinesByLhMachineMap(byLhMachine);
    }

    /**
     * 查询单个年月版本的全部结构关联，不能按最早供胚记录缩减集合。
     * @param context 上下文
     * @param factoryCode 工厂
     * @param plans 同年月同版本月计划
     * @return 结构专供快照
     */
    private Map<String, StructureDedicatedMachineContext> loadStructureSnapshots(
            LhScheduleContext context, String factoryCode, List<FactoryMonthPlanProductionFinalResult> plans) {
        FactoryMonthPlanProductionFinalResult plan = plans.get(0);
        // 同年月版本下读取全部结构，覆盖没有正向月计划SKU的日计划调整物料。
        Map<String, StructureDedicatedMachineContext> snapshots = new LinkedHashMap<>(16);
        List<MpStructureAllocation> allocations = structureAllocationMapper.selectList(
                new LambdaQueryWrapper<MpStructureAllocation>()
                        .select(MpStructureAllocation::getStructureName, MpStructureAllocation::getCxMachineCode)
                        .eq(MpStructureAllocation::getFactoryCode, factoryCode)
                        .eq(MpStructureAllocation::getYear, plan.getYear())
                        .eq(MpStructureAllocation::getMonth, plan.getMonth())
                        .eq(MpStructureAllocation::getProductionVersion, plan.getProductionVersion())
                        .eq(MpStructureAllocation::getPlanType, NORMAL_PLAN_TYPE));
        Map<String, Set<String>> formingMachinesByStructure = new LinkedHashMap<>(16);
        allocations.stream().filter(Objects::nonNull)
                .filter(row -> StringUtils.isNotEmpty(StringUtils.trim(row.getStructureName())))
                .filter(row -> StringUtils.isNotEmpty(StringUtils.trim(row.getCxMachineCode())))
                .forEach(row -> formingMachinesByStructure.computeIfAbsent(row.getStructureName().trim(),
                        key -> new LinkedHashSet<>(4)).add(row.getCxMachineCode().trim().toUpperCase(Locale.ROOT)));
        formingMachinesByStructure.forEach((structure, machines) -> {
            Set<String> dedicatedForming = machines.stream()
                    .filter(context.getFormingMachineSupplyLhMachineMap()::containsKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> dedicatedLh = dedicatedForming.stream()
                    .flatMap(machine -> context.getFormingMachineSupplyLhMachineMap().get(machine).stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            StructureDedicatedMachineContext snapshot = new StructureDedicatedMachineContext(
                    structure, machines, dedicatedForming, dedicatedLh);
            snapshots.put(structure, snapshot);
            log.info("[专供结构约束] 结构分类, factoryCode: {}, year: {}, month: {}, version: {}, structure: {}, "
                            + "type: {}, formingMachines: {}, dedicatedFormingMachines: {}, dedicatedLhMachines: {}",
                    factoryCode, plan.getYear(), plan.getMonth(), plan.getProductionVersion(), structure,
                    snapshot.getType(), machines, dedicatedForming, dedicatedLh);
        });
        return snapshots;
    }

    /**
     * 按SKU实际所属月计划读取快照，不回退固定结构或其它月份。
     * @param context 上下文
     * @param sku 待排SKU
     * @return 匹配快照，无转产关系时返回null
     */
    public StructureDedicatedMachineContext getStructureContext(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(sku.getStructureName())) {
            return null;
        }
        String planKey = String.format(PLAN_KEY_FORMAT, sku.getMonthPlanYear(), sku.getMonthPlanMonth(), sku.getProductionVersion());
        Map<String, StructureDedicatedMachineContext> structures = context.getStructureDedicatedMachineContextMap().get(planKey);
        return Objects.isNull(structures) ? null : structures.get(sku.getStructureName().trim());
    }

    /**
     * 获取结构类型。
     * @param context 上下文
     * @param sku 待排SKU
     * @return 专供类型，无关联时为NONE
     */
    public DedicatedType getDedicatedType(LhScheduleContext context, SkuScheduleDTO sku) {
        StructureDedicatedMachineContext snapshot = this.getStructureContext(context, sku);
        return Objects.isNull(snapshot) ? DedicatedType.NONE : snapshot.getType();
    }

    /**
     * 读取结构全部成型机，供拒绝证据使用。
     * @param context 上下文
     * @param sku 待排SKU
     * @return 成型机集合
     */
    public Set<String> getFormingMachines(LhScheduleContext context, SkuScheduleDTO sku) {
        StructureDedicatedMachineContext snapshot = this.getStructureContext(context, sku);
        return Objects.isNull(snapshot) ? Collections.emptySet() : snapshot.getFormingMachines();
    }

    /**
     * 获取强、弱专供结构对应的全部硫化机。
     * @param context 上下文
     * @param sku 待排SKU
     * @return 物理硫化机集合
     */
    public Set<String> getPreferredLhMachines(LhScheduleContext context, SkuScheduleDTO sku) {
        StructureDedicatedMachineContext snapshot = this.getStructureContext(context, sku);
        return Objects.isNull(snapshot) ? Collections.emptySet() : snapshot.getDedicatedVulcanizingMachines();
    }

    /**
     * 只对强专供结构执行范围硬限制，弱专供和普通结构不增加拒绝条件。
     * @param context 上下文
     * @param machineCode 候选机台
     * @param sku 待排SKU
     * @return 是否通过专供准入，仍须执行其它硬约束
     */
    public boolean canMachineSelectStructure(LhScheduleContext context, String machineCode, SkuScheduleDTO sku) {
        return this.getDedicatedType(context, sku) != DedicatedType.EXCLUSIVE
                || this.isPreferredMachine(context, machineCode, sku);
    }

    /**
     * 获取机台专供成型机集合。
     * @param context 上下文
     * @param machineCode 机台编码
     * @return 对应成型机集合
     */
    public Set<String> getSuppliedFormingMachines(LhScheduleContext context, String machineCode) {
        return context.getSupplyFormingMachinesByLhMachineMap().getOrDefault(
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode), Collections.emptySet());
    }

    /**
     * 判断机台是否属于结构的专供范围。
     * @param context 上下文
     * @param machineCode 机台编码
     * @param sku 待排SKU
     * @return 是否命中专供关系，不代表资源可排
     */
    public boolean isPreferredMachine(LhScheduleContext context, String machineCode, SkuScheduleDTO sku) {
        return this.getPreferredLhMachines(context, sku).contains(
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
    }
    /**
     * 机台处理层级：专供机优先，层内保持原顺序。
     * @param context 上下文
     * @param machineCode 物理机或单控侧编码
     * @return 专供机为0，普通机为1
     */
    public int getMachinePriority(LhScheduleContext context, String machineCode) {
        return CollectionUtils.isEmpty(this.getSuppliedFormingMachines(context, machineCode)) ? 1 : 0;
    }

    /**
     * 当前机台候选层级；不匹配的弱专供候选仍属于普通层，不能过滤。
     * @param context 上下文
     * @param machineCode 当前机台
     * @param sku 待排SKU
     * @return 强专供、弱专供、普通的优先层级
     */
    public DedicatedType getMatchPriority(LhScheduleContext context, String machineCode, SkuScheduleDTO sku) {
        return this.isPreferredMachine(context, machineCode, sku)
                ? this.getDedicatedType(context, sku) : DedicatedType.NONE;
    }

}
