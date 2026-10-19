package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 历史指定承接的实际动作快照，隔离历史排序类型与本次执行类型。
 * 原装机模具及物料关系决定动作，资源占用与可用性仍由模具分配器逐次校验。
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
     * 保存不随候选资源扣减或回滚变化的动作事实。
     *
     * @param changeMouldType 实际交替类型
     * @param originalMouldCodes 原装机模具组
     * @param reason 判定原因
     */
    private PreviousAlternateActionPlan(String changeMouldType, Set<String> originalMouldCodes, String reason) {
        this.changeMouldType = changeMouldType;
        this.originalMouldCodes = Collections.unmodifiableSet(new LinkedHashSet<>(originalMouldCodes));
        this.reason = reason;
    }

    /**
     * 冻结并复用历史承接实际动作，异料必须由原完整装机组与后料关系确定。
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
        // 历史异料但MES已显示后料时，仍须按完整装机模具关系确定本次01/02。
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
            typeBlock = StringUtils.isNotBlank(event.getMaterialCode())
                    && originalMoulds.size() == requiredMouldQty
                    && targetMoulds.containsAll(originalMoulds);
            reason = typeBlock ? "后料支持原完整装机模具组，按实际关系换活字块"
                    : "原完整装机模具组不满足后料关系，按实际关系正规换模";
        }
        PreviousAlternateActionPlan action = new PreviousAlternateActionPlan(typeBlock
                ? MouldChangeTypeEnum.TYPE_BLOCK.getCode() : MouldChangeTypeEnum.REGULAR.getCode(),
                originalMoulds, reason);
        // 仅缓存固定输入事实；候选选定模具与资源仍由既有预演快照和重放机制维护。
        event.setActualActionPlan(action);
        return action;
    }
}
