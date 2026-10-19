package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateActionPlan;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.util.CleaningDispositionResolver;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 历史正规换模与喷砂重叠时的时间下机联合尝试，实际后料提交前不永久取消清洗。
 *
 * @author APS
 * @date 2026/10/18
 */
@Service
public class TimedMachineOffCleaningHandoffService {

    /**
     * 在原排产阶段内验证提前交接，未形成指定正规换模时整体恢复，再按原规则执行一次。
     * 仅在喷砂开始前或同刻接管机台，避免改变冻结画像中节点以前的真实生产量。
     *
     * @param context 数量尚未最终扣账的本批上下文
     * @param handoffPhase 原时间下机、数量扣账、设备复评和历史承接阶段
     */
    public void execute(LhScheduleContext context, Runnable handoffPhase) {
        Map<String, Date> candidates = this.resolveCandidates(context);
        if (candidates.isEmpty()) {
            handoffPhase.run();
            return;
        }
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        sources.addAll(context.getScheduleResultSourceSkuMap().values());
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        Map<String, Date> previousCandidates = context.getTimedMachineOffCleaningCandidateMap();
        Map<String, List<MachineCleaningWindowDTO>> coveredWindows = new LinkedHashMap<>();
        candidates.forEach((machineCode, changeStart) -> coveredWindows.put(machineCode,
                context.getMachineScheduleMap().get(machineCode).getCleaningWindowList().stream()
                        .filter(window -> CleaningDispositionResolver.isPendingSandBlastCovered(
                                context, window, changeStart)).collect(Collectors.toList())));
        boolean completed = false;
        List<String> unconfirmed;
        context.setTimedMachineOffCleaningCandidateMap(candidates);
        try {
            handoffPhase.run();
            unconfirmed = candidates.entrySet().stream()
                    .filter(entry -> this.wasCandidateUsed(context, entry.getKey(), entry.getValue()))
                    .filter(entry -> !this.isCommitted(context, entry.getKey(), entry.getValue(),
                            coveredWindows.get(entry.getKey())))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            if (unconfirmed.isEmpty()) {
                candidates.forEach((machineCode, changeStart) -> {
                    if (this.wasCandidateUsed(context, machineCode, changeStart)) {
                        context.getTimedMachineOffCleaningConfirmedMap().put(machineCode, changeStart);
                    }
                });
            }
            completed = true;
        } finally {
            context.setTimedMachineOffCleaningCandidateMap(previousCandidates);
            // 异常必须恢复并原样传播，不能把部分扣账或候选释放当作正式事实。
            if (!completed) {
                snapshot.restore(context);
            }
        }
        if (!unconfirmed.isEmpty()) {
            snapshot.restore(context);
            PriorityTraceLogHelper.appendProcessLog(context, "喷砂重叠换模联合尝试恢复",
                    "工厂=" + context.getFactoryCode() + ", 批次=" + context.getBatchNo()
                            + ", 机台=" + unconfirmed + ", 原因=指定正规换模未在候选节点提交，已恢复全部阶段状态并沿原规则计算");
            handoffPhase.run();
            return;
        }
        PriorityTraceLogHelper.appendProcessLog(context, "喷砂重叠换模联合尝试确认",
                "工厂=" + context.getFactoryCode() + ", 批次=" + context.getBatchNo()
                        + ", 候选节点=" + candidates + ", 结论=实际使用的候选均已落实指定正规换模，清洗按正式结果统一处置");
    }

    /**
     * 从已选降模请求和合格历史事件取得精确候选，不新增机台或改变普通下机排序。
     *
     * @param context 降模请求及已冻结历史动作
     * @return 允许联合尝试的运行侧机台与历史节点
     */
    private Map<String, Date> resolveCandidates(LhScheduleContext context) {
        Map<String, Date> candidates = new LinkedHashMap<>();
        context.getTimedMachineOffRequests().stream()
                .filter(request -> !request.getSourceSku().isStrictTargetQty())
                .forEach(request -> request.getProfile().getOriginals().forEach(result -> {
                    String machineCode = result.getLhMachineCode();
                    PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(machineCode);
                    MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
                    if (context.getContinuationSurplusEndingAllocatedResults().contains(result)
                            || context.getContinuationRemainderFinishGroups().stream()
                            .anyMatch(group -> group.getParticipatingResults().contains(result))
                            || Objects.isNull(event) || Objects.isNull(machine) || Objects.isNull(event.getOfflineTime())
                            || event.isRemainderFinishDeferred() || event.isSingleMouldReplacement()
                            || event.resolveProductionRole()
                            != PreviousAlternatePlanReleaseEvent.ProductionRole.BEFORE_MATERIAL_PRODUCTION
                            || !StringUtils.equals(event.getMaterialCode(), result.getMaterialCode())
                            || !StringUtils.equals(event.getProductStatus(), result.getProductStatus())
                            || !MouldChangeTypeEnum.REGULAR.getCode().equals(
                                    PreviousAlternateActionPlan.resolve(context, event).getChangeMouldType())) {
                        return;
                    }
                    if (machine.getCleaningWindowList().stream().anyMatch(window ->
                            CleaningDispositionResolver.isPendingSandBlastCovered(context, window, event.getOfflineTime())
                                    && CleaningDispositionResolver.resolve(context, machineCode, window).getDisposition()
                                    == CleaningDispositionResolver.Disposition.INDEPENDENT)) {
                        candidates.put(machineCode, event.getOfflineTime());
                    }
                }));
        return candidates;
    }

    /**
     * 判断正式时间决策是否实际使用联合尝试的精确节点。
     *
     * @param context 本轮阶段执行后的决策
     * @param machineCode 运行侧机台
     * @param changeStart 联合候选节点
     * @return 本机规则及物理释放均在该节点时为 true
     */
    private boolean wasCandidateUsed(LhScheduleContext context, String machineCode, Date changeStart) {
        return context.getTimedMachineOffDecisionMap().entrySet().stream()
                .filter(entry -> StringUtils.equals(machineCode, entry.getKey().getLhMachineCode()))
                .map(Map.Entry::getValue)
                .anyMatch(decision -> changeStart.equals(decision.getRuleOffMachineTime())
                        && changeStart.equals(decision.getOffMachineTime()));
    }

    /**
     * 用同机、指定后料状态及实际动作确认候选，不能用其他物料或延后换模证明清洗已取消。
     *
     * @param context 已提交的阶段结果
     * @param machineCode 运行侧机台
     * @param changeStart 精确候选节点
     * @param windows 本候选覆盖的原喷砂窗口
     * @return 指定正规换模有正量提交且覆盖窗口均已取消时为 true
     */
    private boolean isCommitted(LhScheduleContext context, String machineCode, Date changeStart,
            List<MachineCleaningWindowDTO> windows) {
        PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(machineCode);
        boolean committed = Objects.nonNull(event) && context.getScheduleResultList().stream()
                .filter(result -> StringUtils.equals(machineCode, result.getLhMachineCode()))
                .filter(result -> context.matchesPreviousAlternateAfterMaterial(event.getPlan(),
                        result.getMaterialCode(), result.getProductStatus()))
                .anyMatch(result -> "1".equals(result.getIsChangeMould()) && !"1".equals(result.getIsTypeBlock())
                        && changeStart.equals(result.getMouldChangeStartTime())
                        && ShiftFieldUtil.resolveScheduledQty(result) > 0);
        return committed && windows.stream().allMatch(window ->
                CleaningDispositionResolver.resolve(context, machineCode, window).getDisposition()
                        == CleaningDispositionResolver.Disposition.MOULD_CHANGE_CANCELLED);
    }
}
