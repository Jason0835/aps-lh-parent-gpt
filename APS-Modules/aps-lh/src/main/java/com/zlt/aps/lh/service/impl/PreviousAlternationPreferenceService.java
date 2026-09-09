package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.engine.strategy.support.DayTypeBlockReverseSelectionDirective;
import lombok.extern.slf4j.Slf4j;
import com.zlt.aps.lh.util.TypeBlockRelationUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiPredicate;

/**
 * 新增及换活字块单机选SKU的前次交替关系查询服务。
 *
 * <p>本服务无共享可变成员。历史索引只存于本次排程上下文，不保存可排性或生产资源账本；
 * 失败时只撤回本次新增的反选偏好，恢复原选择。T固定启用、T+1按开关启用，均在同一份前次交替计划中查询，
 * 不按历史计划日期或班次过滤，也不将历史换模班次绑定到本次排程。</p>
 */
@Slf4j
public class PreviousAlternationPreferenceService {

    /** 前次关系先按计划时间，再按计划顺序和主键保持稳定；不绑定本次切换班次。 */
    private static final Comparator<LhMouldChangePlan> PLAN_ORDER = Comparator
            .comparing(LhMouldChangePlan::getPlanDate, Comparator.nullsLast(Date::compareTo))
            .thenComparing(LhMouldChangePlan::getPlanOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(LhMouldChangePlan::getId, Comparator.nullsLast(Long::compareTo));

    /** 与LhScheduleContext登记机台预留的空产品状态口径一致。 */
    private static final String FORMAL_PRODUCT_STATUS = "S";

    /** T+1资源日相对窗口起点的偏移。 */
    private static final long NEXT_RESOURCE_DAY_OFFSET = 1L;

    /**
     * 判断当前资源日是否启用；关闭时不读取历史关系索引。
     *
     * @param context 本次排程上下文
     * @param resourceDate 当前资源业务日，不能传SKU日期池日期
     * @return 是否启用历史交替优先
     */
    public boolean isEnabled(LhScheduleContext context, LocalDate resourceDate) {
        if (Objects.isNull(context) || context.isIsolatedNextShiftPlan()
                || Objects.isNull(context.getScheduleDate()) || Objects.isNull(resourceDate)) {
            return false;
        }
        long offset = ChronoUnit.DAYS.between(this.toLocalDate(context.getScheduleDate()), resourceDate);
        return offset == 0 || (offset == NEXT_RESOURCE_DAY_OFFSET && this.isNextDayEnabled(context));
    }

    /**
     * 核对已知候选的历史关系，供日志和既有指令复核使用，不用于组织候选尝试顺序。
     *
     * @param context 排程上下文
     * @param resourceDate 资源业务日
     * @param machine 当前切换前机台运行态，L/R编码精确匹配
     * @param changeType 本次实际交替类型
     * @param materialCode 当前候选后物料
     * @return 命中的历史计划，无匹配时返回null
     */
    public LhMouldChangePlan match(LhScheduleContext context, LocalDate resourceDate,
                                   MachineScheduleDTO machine, String changeType, String materialCode) {
        // 此方法只为候选命中日志和提交复核提供证据，历史列表查询不以候选后物料为条件。
        return this.findPreviousPlans(context, resourceDate, machine, changeType).stream()
                .filter(plan -> StringUtils.equals(materialCode, plan.getAfterMaterialCode()))
                .findFirst().orElse(null);
    }

    /**
     * 按机台、交替类型和前物料获取前次计划，保持PLAN_DATE升序。
     *
     * @param context 本次排程上下文
     * @param resourceDate 当前资源日，仅判断是否启用，不过滤历史计划日期
     * @param machine 切换前机台运行态
     * @param changeType 本次交替动作类型
     * @return 历史有序关系列表，后物料是查询结果而非查询条件
     */
    public List<LhMouldChangePlan> findPreviousPlans(LhScheduleContext context, LocalDate resourceDate,
                                                    MachineScheduleDTO machine, String changeType) {
        if (!this.isEnabled(context, resourceDate) || Objects.isNull(machine)
                || StringUtils.isEmpty(machine.getMachineCode())
                || StringUtils.isEmpty(machine.getCurrentMaterialCode())
                || StringUtils.isEmpty(changeType)) {
            return Collections.emptyList();
        }
        List<LhMouldChangePlan> plans = this.resolveIndex(context).get(
                this.buildKey(machine.getMachineCode(), changeType, machine.getCurrentMaterialCode()));
        return Objects.isNull(plans) ? Collections.emptyList() : plans;
    }

    /**
     * 只返回原候选池内的历史后物料，按前次计划日期升序，不修改原候选列表。
     *
     * @param context 排程上下文
     * @param resourceDate 当前资源日
     * @param machine 当前机台
     * @param changeType 本次交替类型
     * @param candidates 原候选池
     * @return 按前次计划顺序排列的历史候选；关闭时不读取历史关系
     */
    public List<SkuScheduleDTO> preferredCandidates(LhScheduleContext context, LocalDate resourceDate,
            MachineScheduleDTO machine, String changeType, List<SkuScheduleDTO> candidates) {
        if (!this.isEnabled(context, resourceDate) || CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        List<LhMouldChangePlan> plans = this.findPreviousPlans(context, resourceDate, machine, changeType);
        return this.mapHistoricalCandidates(plans, candidates, Function.identity());
    }

    /**
     * 将历史有序后物料列表与当前候选池相交，按物料分组避免反复全量扫描候选池。
     *
     * @param plans 按PLAN_DATE排序的三维匹配关系
     * @param candidates 当前候选池，保持原列表不变
     * @param skuResolver 从现有候选读取SKU，不复制业务账本
     * @param <T> 原候选类型
     * @return 按历史顺序返回的候选，同物料不同状态保持原池内顺序
     */
    public <T> List<T> mapHistoricalCandidates(List<LhMouldChangePlan> plans, List<T> candidates,
                                              Function<T, SkuScheduleDTO> skuResolver) {
        return this.mapHistoricalCandidates(plans, candidates, skuResolver, (candidate, plan) -> true);
    }

    /**
     * 在历史列表与当前候选池相交时校验动作类型，避免新增链中的两种切换互相命中。
     *
     * @param plans 历史日期有序关系
     * @param candidates 当前候选池
     * @param skuResolver 候选SKU读取器
     * @param actionMatches 当前候选实际动作与历史类型校验
     * @param <T> 原候选类型
     * @return 历史顺序中的当前候选，同一对象只返回一次
     */
    public <T> List<T> mapHistoricalCandidates(List<LhMouldChangePlan> plans, List<T> candidates,
                                              Function<T, SkuScheduleDTO> skuResolver,
                                              BiPredicate<T, LhMouldChangePlan> actionMatches) {
        if (CollectionUtils.isEmpty(plans) || CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        Map<String, List<T>> candidatesByMaterial = new LinkedHashMap<String, List<T>>(candidates.size());
        for (T candidate : candidates) {
            if (Objects.nonNull(candidate)) {
                SkuScheduleDTO sku = skuResolver.apply(candidate);
                if (Objects.nonNull(sku)) {
                    candidatesByMaterial.computeIfAbsent(sku.getMaterialCode(),
                            ignored -> new ArrayList<T>(1)).add(candidate);
                }
            }
        }
        List<T> preferred = new ArrayList<T>(candidates.size());
        Set<T> visitedCandidates = Collections.newSetFromMap(new IdentityHashMap<T, Boolean>(candidates.size()));
        // 由历史计划依次取后物料，再与当前池相交；重复后物料只尝试一次，各产品状态保持原顺序。
        for (LhMouldChangePlan plan : plans) {
            List<T> matches = candidatesByMaterial.get(plan.getAfterMaterialCode());
            if (!CollectionUtils.isEmpty(matches)) {
                for (T candidate : matches) {
                    if (!visitedCandidates.contains(candidate) && actionMatches.test(candidate, plan)) {
                        visitedCandidates.add(candidate);
                        preferred.add(candidate);
                    }
                }
            }
        }
        return preferred;
    }

    /**
     * 只供同机台候选比较：历史计划日期越早越优先，无匹配排在有匹配之后。
     *
     * @param left 左候选匹配计划
     * @param right 右候选匹配计划
     * @return 同一历史关系返回0，由调用方保留原SKU排序
     */
    public int comparePlans(LhMouldChangePlan left, LhMouldChangePlan right) {
        return Comparator.nullsLast(PLAN_ORDER).compare(left, right);
    }

    /**
     * 复用新增时间轴的交替类型口径，同物料衔接不参加历史交替匹配。
     *
     * @param context 排程上下文
     * @param machine 切换前机台
     * @param sku 候选物料
     * @return 本次实际交替类型，无物料切换时返回null
     */
    public String resolveChangeType(LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        if (Objects.isNull(machine) || Objects.isNull(sku)
                || StringUtils.isEmpty(machine.getCurrentMaterialCode())
                || StringUtils.equals(machine.getCurrentMaterialCode(), sku.getMaterialCode())) {
            return null;
        }
        return TypeBlockRelationUtil.isSameEmbryoAndSameMould(context, machine, sku)
                ? MouldChangeTypeEnum.TYPE_BLOCK.getCode() : MouldChangeTypeEnum.REGULAR.getCode();
    }

    /**
     * 构造可复核的关系证据；最终是否复用由正式提交结果单独追加。
     *
     * @param context 排程上下文
     * @param resourceDate 当前资源业务日
     * @param machine 切换前机台
     * @param changeType 当前动作类型
     * @param materialCode 候选后物料
     * @return 中文匹配证据
     */
    public String describe(LhScheduleContext context, LocalDate resourceDate,
                           MachineScheduleDTO machine, String changeType, String materialCode) {
        boolean enabled = this.isEnabled(context, resourceDate);
        LhMouldChangePlan plan = this.match(context, resourceDate, machine, changeType, materialCode);
        String reason = "无";
        if (!enabled) {
            reason = "资源日未启用，执行原选择";
        } else if (Objects.isNull(plan)) {
            reason = "前次计划无机台、类型及前后物料完全一致的关系";
        }
        return new StringBuilder(256).append("资源池日期=").append(resourceDate)
                .append(", T+1前次交替匹配参数值=").append(this.isNextDayEnabled(context) ? 1 : 0)
                .append(", 前次交替匹配=").append(enabled ? "开启" : "关闭")
                .append(", 前次交替计划时间=").append(Objects.isNull(plan) ? null : plan.getPlanDate())
                .append(", 前次批次=").append(Objects.isNull(plan) ? null : plan.getLhResultBatchNo())
                .append(", 当前交替类型=").append(changeType)
                .append(", 前次交替类型=").append(Objects.isNull(plan) ? null : plan.getChangeMouldType())
                .append(", 机台=").append(Objects.isNull(machine) ? null : machine.getMachineCode())
                .append(", 前物料=").append(Objects.isNull(machine) ? null : machine.getCurrentMaterialCode())
                .append(", 后物料=").append(materialCode)
                .append(", 是否命中=").append(Objects.nonNull(plan))
                .append(", 未命中原因=").append(reason)
                .toString();
    }

    /**
     * 获取本机按天匹配时保存的历史候选范围，供当前班次逐个验证。
     *
     * @param context 排程上下文
     * @param machineCode 当前机台编码
     * @return 未完成的历史偏好指令，无偏好时返回null
     */
    public DayTypeBlockReverseSelectionDirective findDayTypeBlockPreference(LhScheduleContext context,
                                                                            String machineCode) {
        return context.getDayTypeBlockReverseSelectionDirectiveList().stream()
                .filter(directive -> !directive.isSatisfied() && !directive.isSuccess()
                        && StringUtils.equals(machineCode, directive.getMachineCode())
                        && !CollectionUtils.isEmpty(directive.getPreviousAlternationCandidateKeys()))
                .findFirst().orElse(null);
    }

    /**
     * 临时选中同机台下一条历史关系，仍尊重其他机台已有的物料归属。
     *
     * @param context 当前运行态
     * @param directive 原按天指令
     * @param skuKey 原候选池中保存的物料状态键
     * @return 当前候选是否仍可参与原单机预演；不分配生产资源
     */
    public boolean selectDayTypeBlockPreference(LhScheduleContext context,
            DayTypeBlockReverseSelectionDirective directive, String skuKey) {
        SkuScheduleDTO candidate = context.getNewSpecSkuList().stream().filter(Objects::nonNull)
                .filter(sku -> StringUtils.equals(skuKey, MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus())))
                .findFirst().orElse(null);
        if (Objects.isNull(candidate) || Objects.isNull(this.match(context, directive.getScheduleDate(),
                context.getMachineScheduleMap().get(directive.getMachineCode()),
                MouldChangeTypeEnum.TYPE_BLOCK.getCode(), candidate.getMaterialCode()))) {
            return false;
        }
        String reservationKey = this.buildReservationKey(candidate.getMaterialCode(), candidate.getProductStatus());
        boolean alreadySelected = context.getDayTypeBlockReverseSelectedSkuKeyMap().entrySet().stream()
                .anyMatch(entry -> !StringUtils.equals(directive.getMachineCode(), entry.getKey())
                        && StringUtils.equals(reservationKey, entry.getValue()));
        if (alreadySelected) {
            return false;
        }
        directive.setMaterialCode(candidate.getMaterialCode());
        directive.setProductStatus(candidate.getProductStatus());
        directive.setSkuSortRank(candidate.getSortRank());
        directive.setPreviousAlternationPreferred(true);
        context.getDayTypeBlockReverseSelectedSkuKeyMap().put(directive.getMachineCode(), reservationKey);
        return true;
    }

    /**
     * 历史候选当前不可排时恢复本机原首选，不撤销其他机台已经取得的物料归属。
     *
     * @param context 当前运行态
     * @param machineCode 历史候选机台
     * @return 是否撤回过历史偏好，供原选择器在当前班次重新选择
     */
    public boolean restoreDayTypeBlockPreference(LhScheduleContext context, String machineCode) {
        Iterator<DayTypeBlockReverseSelectionDirective> iterator =
                context.getDayTypeBlockReverseSelectionDirectiveList().iterator();
        while (iterator.hasNext()) {
            DayTypeBlockReverseSelectionDirective directive = iterator.next();
            if (!directive.isPreviousAlternationPreferred() || directive.isSatisfied()
                    || !StringUtils.equals(machineCode, directive.getMachineCode())) {
                continue;
            }
            String originalKey = this.buildReservationKey(
                    directive.getOriginalMaterialCode(), directive.getOriginalProductStatus());
            boolean alreadySelected = context.getDayTypeBlockReverseSelectedSkuKeyMap().entrySet().stream()
                    .anyMatch(entry -> !StringUtils.equals(machineCode, entry.getKey())
                            && StringUtils.equals(originalKey, entry.getValue()));
            directive.setPreviousAlternationPreferred(false);
            context.releaseDayTypeBlockReverseSelectedMachine(machineCode);
            if (alreadySelected) {
                // 物料归属仍由前序机台选择决定，不能为了恢复偏好前的首选再抢回物料。
                directive.setResultReason("历史候选不可排且原首选已被其他机台选中，恢复普通SKU竞争");
                iterator.remove();
            } else {
                directive.setMaterialCode(directive.getOriginalMaterialCode());
                directive.setProductStatus(directive.getOriginalProductStatus());
                directive.setSkuSortRank(directive.getOriginalSkuSortRank());
                directive.setAttempted(false);
                directive.setResultReason("历史候选不可排，已恢复本机原反选首选");
                context.getDayTypeBlockReverseSelectedSkuKeyMap().put(machineCode, originalKey);
            }
            log.info("按天换活字块历史偏好撤回, batchNo: {}, machineCode: {}, 处理结果={}, 是否最终复用=false",
                    context.getBatchNo(), machineCode, directive.getResultReason());
            return true;
        }
        return false;
    }

    /**
     * 构建并冻结前日最近有效批次关系索引，不过滤历史计划日期。
     *
     * @param context 已加载前日交替计划的上下文
     * @return 只读关系索引
     */
    private Map<String, List<LhMouldChangePlan>> resolveIndex(LhScheduleContext context) {
        if (Objects.nonNull(context.getPreviousAlternationPlanIndex())) {
            return context.getPreviousAlternationPlanIndex();
        }
        List<LhMouldChangePlan> source = context.getHistoricalReverseMouldChangePlanList();
        Map<String, List<LhMouldChangePlan>> index = new LinkedHashMap<String, List<LhMouldChangePlan>>(16);
        if (!CollectionUtils.isEmpty(source)) {
            // 先锁定批次，再索引全部换模和换活字块关系；更新时间不代表排程生成顺序。
            LhMouldChangePlan latest = source.stream().filter(Objects::nonNull)
                    .filter(plan -> StringUtils.isNotEmpty(plan.getLhResultBatchNo()))
                    .max(Comparator.comparing(LhMouldChangePlan::getCreateTime,
                            Comparator.nullsFirst(Date::compareTo))
                            .thenComparing(LhMouldChangePlan::getId, Comparator.nullsFirst(Long::compareTo)))
                    .orElse(null);
            if (Objects.nonNull(latest)) {
                source.stream().filter(Objects::nonNull)
                        .filter(plan -> StringUtils.equals(latest.getLhResultBatchNo(), plan.getLhResultBatchNo()))
                        .forEach(plan -> this.indexPlan(index, plan));
            }
        }
        Map<String, List<LhMouldChangePlan>> frozen = new LinkedHashMap<String, List<LhMouldChangePlan>>(index.size());
        index.forEach((key, plans) -> {
            plans.sort(PLAN_ORDER);
            frozen.put(key, Collections.unmodifiableList(plans));
        });
        context.setPreviousAlternationPlanIndex(Collections.unmodifiableMap(frozen));
        return context.getPreviousAlternationPlanIndex();
    }

    /**
     * 索引完整前次批次关系，历史计划时间用于优先排序和对账，不用于日期过滤。
     *
     * @param index 待构建索引
     * @param plan 历史计划
     */
    private void indexPlan(Map<String, List<LhMouldChangePlan>> index, LhMouldChangePlan plan) {
        if (StringUtils.isEmpty(plan.getLhMachineCode())
                || StringUtils.isEmpty(plan.getBeforeMaterialCode()) || StringUtils.isEmpty(plan.getAfterMaterialCode())
                || (!StringUtils.equals(MouldChangeTypeEnum.REGULAR.getCode(), plan.getChangeMouldType())
                && !StringUtils.equals(MouldChangeTypeEnum.TYPE_BLOCK.getCode(), plan.getChangeMouldType()))) {
            return;
        }
        String key = this.buildKey(plan.getLhMachineCode(), plan.getChangeMouldType(), plan.getBeforeMaterialCode());
        index.computeIfAbsent(key, ignored -> new ArrayList<LhMouldChangePlan>(2)).add(plan);
    }

    /**
     * 读取集中解析后的T+1开关，缺失配置按默认启用。
     *
     * @param context 配置上下文
     * @return T+1开关
     */
    private boolean isNextDayEnabled(LhScheduleContext context) {
        return Objects.isNull(context) || Objects.isNull(context.getScheduleConfig())
                || context.getScheduleConfig().isNextDayPreviousAlternationPriorityEnabled();
    }

    /**
     * 转换窗口日期，统一使用项目本地时区。
     *
     * @param value 窗口日期
     * @return 本地业务日期
     */
    private LocalDate toLocalDate(Date value) {
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 复用上下文既有空状态按正规登记的预留键口径，不改变候选身份。
     *
     * @param materialCode 物料编码
     * @param productStatus 候选产品状态
     * @return 机台预留键
     */
    private String buildReservationKey(String materialCode, String productStatus) {
        return MonthPlanDateResolver.buildMaterialStatusKey(materialCode,
                StringUtils.defaultIfEmpty(productStatus, FORMAL_PRODUCT_STATUS));
    }

    /**
     * 构建机台、交替类型和前物料的精确索引，不包含历史日期或后物料。
     *
     * @param machineCode 机台编码
     * @param changeType 交替类型
     * @param beforeMaterial 前物料
     * @return 关系索引键
     */
    private String buildKey(String machineCode, String changeType, String beforeMaterial) {
        return new StringBuilder(64).append(machineCode).append('|').append(changeType)
                .append('|').append(beforeMaterial).toString();
    }
}
