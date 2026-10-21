package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 历史指定承接的实际动作快照，隔离历史排序类型与本次执行类型。
 * 前料身份和完整装机模具关系共同决定动作，资源占用与可用性仍由模具分配器逐次校验。
 *
 * @author APS
 * @date 2026/10/13
 */
@Getter
public final class PreviousAlternateActionPlan implements Serializable {

    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 本次实际交替类型，正规换模或换活字块。
     */
    private final String changeMouldType;

    /**
     * 下机前完整装机模具的只读快照，释放后不得用后料绑定反推原组。
     */
    private final Set<String> originalMouldCodes;

    /**
     * 实际动作判定原因。
     */
    private final String reason;

    /**
     * 历史动作的前物料，与MES当前已上后料的身份分别保存。
     */
    private final String beforeMaterialCode;

    /**
     * 前物料实际产品状态；只有实际前料状态或本批唯一SKU来源才能确定。
     */
    private final String beforeProductStatus;

    /**
     * 保存不随候选资源扣减或回滚变化的动作事实。
     *
     * @param changeMouldType 实际交替类型
     * @param originalMouldCodes 原装机模具组
     * @param reason 判定原因
     * @param beforeMaterialCode 历史前物料
     * @param beforeProductStatus 精确前物料状态，来源有歧义时为空
     */
    private PreviousAlternateActionPlan(String changeMouldType, Set<String> originalMouldCodes, String reason,
            String beforeMaterialCode, String beforeProductStatus) {
        this.changeMouldType = changeMouldType;
        this.originalMouldCodes = Collections.unmodifiableSet(new LinkedHashSet<>(originalMouldCodes));
        this.reason = reason;
        this.beforeMaterialCode = beforeMaterialCode;
        this.beforeProductStatus = beforeProductStatus;
    }

    /**
     * 冻结历史前料身份及实际动作，异料必须使用属于前料的完整装机组证明共模。
     * 同料历史动作继续执行原有整组、单副换模或换活字块规则。
     *
     * @param context 本次机台及物料模具关系快照
     * @param event 已冻结原机物料和模具的历史事件
     * @return 本事件预检、时间轴及正式提交共用的只读动作
     */
    public static PreviousAlternateActionPlan resolve(LhScheduleContext context,
            PreviousAlternatePlanReleaseEvent event) {
        if (Objects.nonNull(event.getActualActionPlan())) {
            return event.getActualActionPlan();
        }
        Set<String> originalMoulds = event.getOriginalMouldCodes().stream()
                .filter(StringUtils::isNotBlank).map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 只有历史关系本身同料且实际在机仍是该料，才保留有意指定的同料动作。
        // MES已显示后料时，其装机模具不能直接冒充历史前料模具。
        boolean sameMaterial = event.resolveProductionRole()
                == PreviousAlternatePlanReleaseEvent.ProductionRole.SAME_MATERIAL_ACTION;
        boolean typeBlock;
        String reason;
        if (sameMaterial) {
            // 同料动作包含有意更换模具的业务指令，不按关系相同改成免换模或换活字块。
            typeBlock = MouldChangeTypeEnum.TYPE_BLOCK.getCode().equals(event.getPlan().getChangeMouldType());
            reason = "同料交替保留既有动作及单副或整组边界";
        } else {
            Set<String> targetMoulds = context.getSkuMouldRelMap()
                    .getOrDefault(event.getPlan().getAfterMaterialCode(), Collections.emptyList()).stream()
                    .filter(Objects::nonNull).map(MdmSkuMouldRel::getMouldCode)
                    .filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toSet());
            int requiredMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                    context.getMachineScheduleMap().get(event.getMachineCode()));
            boolean originalGroupBelongsToBefore = event.resolveProductionRole()
                    != PreviousAlternatePlanReleaseEvent.ProductionRole.AFTER_MATERIAL_PREPARATION
                    || context.getSkuMouldRelMap()
                    .getOrDefault(event.getPlan().getBeforeMaterialCode(), Collections.emptyList()).stream()
                    .filter(Objects::nonNull).map(MdmSkuMouldRel::getMouldCode)
                    .filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toSet())
                    .containsAll(originalMoulds);
            typeBlock = StringUtils.isNotBlank(event.getMaterialCode())
                    && originalGroupBelongsToBefore
                    && originalMoulds.size() == requiredMouldQty
                    && targetMoulds.containsAll(originalMoulds);
            reason = typeBlock ? "前后料支持原完整装机模具组，按实际关系换活字块"
                    : "完整装机组不能证明前后料共模，按实际关系正规换模";
        }
        PreviousAlternateActionPlan action = new PreviousAlternateActionPlan(typeBlock
                ? MouldChangeTypeEnum.TYPE_BLOCK.getCode() : MouldChangeTypeEnum.REGULAR.getCode(),
                originalMoulds, reason, event.getPlan().getBeforeMaterialCode(),
                resolveBeforeProductStatus(context, event));
        // 仅缓存固定输入事实；候选选定模具与资源仍由既有预演快照和重放机制维护。
        event.setActualActionPlan(action);
        return action;
    }

    /**
     * 从实际前料或本批唯一前料SKU冻结产品状态，不把历史后料状态用于前料计算。
     *
     * @param context 本批完整SKU来源
     * @param event 原始机台及历史动作
     * @return 精确前料状态，来源缺失或跨状态有歧义时为空
     */
    private static String resolveBeforeProductStatus(LhScheduleContext context,
            PreviousAlternatePlanReleaseEvent event) {
        if (StringUtils.equals(event.getMaterialCode(), event.getPlan().getBeforeMaterialCode())) {
            return event.getProductStatus();
        }
        Stream<String> scheduledStatuses = context.getAllSkuScheduleDtoMap().values().stream()
                .filter(Objects::nonNull)
                .filter(sku -> StringUtils.equals(event.getPlan().getBeforeMaterialCode(), sku.getMaterialCode()))
                .map(SkuScheduleDTO::getProductStatus);
        // 被零余量准入过滤的前料只能从本批原始月计划及精确余量索引证明状态。
        Stream<String> filteredStatuses = context.getLoadedMonthPlanList().stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(event.getPlan().getBeforeMaterialCode(), plan.getMaterialCode()))
                .map(plan -> plan.getProductStatus())
                .filter(status -> context.getInitialMaterialStatusSurplusQtyMap().containsKey(
                        MonthPlanDateResolver.buildMaterialStatusKey(event.getPlan().getBeforeMaterialCode(), status)));
        List<String> statuses = Stream.concat(scheduledStatuses, filteredStatuses).filter(StringUtils::isNotBlank)
                .distinct().collect(Collectors.toList());
        return statuses.size() == 1 ? statuses.get(0) : null;
    }
}
