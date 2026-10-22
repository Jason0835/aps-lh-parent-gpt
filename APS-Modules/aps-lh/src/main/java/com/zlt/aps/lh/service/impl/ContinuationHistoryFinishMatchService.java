package com.zlt.aps.lh.service.impl;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * 在正式时间下机决定之后隔离复核余量收尾匹配，完整复用历史后料的资源和首检链。
 *
 * @author Codex
 * @date 2026/10/21
 */
@Slf4j
@Service
public class ContinuationHistoryFinishMatchService {

    /**
     * 延迟取得已有承接服务，避免续作策略与新增策略注册期间形成初始化依赖环。
     */
    @Lazy
    @Resource
    private PreviousAlternatePlanReuseService previousAlternatePlanReuseService;

    /**
     * 临时应用候选并执行原承接链，成功、拒绝和异常均恢复正式状态，不重新计算时间下机。
     *
     * @param context 已有正式时间下机决定但尚未扣账的上下文
     * @param histories 本次数量绑定发生变化的运行侧及唯一历史计划
     * @param applyCandidate 原统一提交入口，硬校验失败时返回 false
     * @param prepareHandoff 执行原最终化及设备复评，候选数量仍满足守恒时返回 true
     * @return 所有变化机台均在历史班次承接且供胚首班有正量时返回 true
     */
    public boolean validate(LhScheduleContext context, Map<String, LhMouldChangePlan> histories,
            BooleanSupplier applyCandidate, BooleanSupplier prepareHandoff) {
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        sources.addAll(context.getScheduleResultSourceSkuMap().values());
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        boolean accepted = false;
        String reason = "候选数量或设备最终化校验未通过";
        try {
            // 只借用已有隔离承接作用域抑制递归匹配，不进入时间负荷预演或重新选择下机班次。
            context.setPreviousAlternateGroupPreview(true);
            if (applyCandidate.getAsBoolean() && prepareHandoff.getAsBoolean()) {
                previousAlternatePlanReuseService.reuse(context);
                List<String> rejected = histories.entrySet().stream()
                        .filter(entry -> !this.hasHistoricalHandoff(context, entry.getKey(), entry.getValue()))
                        .map(entry -> this.describeRejectedHandoff(context, entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
                accepted = rejected.isEmpty();
                reason = accepted ? "全部指定承接兑现" : rejected.toString();
            }
        } finally {
            snapshot.restore(context);
        }
        String detail = "工厂=" + context.getFactoryCode() + ", 批次=" + context.getBatchNo()
                + ", 机台=" + histories.keySet() + ", 通过=" + accepted
                + ", 结论=" + (accepted ? "历史班次及供胚首班承接复核通过" : "未形成完整合法承接，保留原节点分配")
                + ", 原因=" + reason
                + ", 时间下机请求与决定=保持原事实，正式结果及资源账本=已恢复";
        PriorityTraceLogHelper.appendProcessLog(context, "余量收尾历史匹配承接复核", detail);
        log.info("余量收尾历史匹配承接复核, {}", detail);
        return accepted;
    }

    /**
     * 在恢复快照前保存未兑现关系及实际交替时间，避免正式过程日志只留下无原因的拒绝。
     *
     * @param context 隔离承接完成的上下文
     * @param machineCode 未兑现的运行侧机台
     * @param history 唯一历史关系
     * @return 可关联计划、班次及实际交替时刻的诊断
     */
    private String describeRejectedHandoff(LhScheduleContext context, String machineCode, LhMouldChangePlan history) {
        String actual = context.getPreviousAlternateResultPlanMap().entrySet().stream()
                .filter(entry -> Objects.nonNull(history) && Objects.equals(history.getId(), entry.getValue().getId()))
                .map(Map.Entry::getKey).filter(result -> Objects.equals(machineCode, result.getLhMachineCode()))
                .map(result -> LhScheduleTimeUtil.formatDateTime(result.getMouldChangeStartTime()))
                .collect(Collectors.joining(","));
        return "机台=" + machineCode + ",计划ID=" + (Objects.isNull(history) ? null : history.getId())
                + ",历史班次=" + (Objects.isNull(history) ? null : history.getClassIndex())
                + ",实际交替=" + actual + ",拒绝=指定后料未排或实际班次及供胚首班正量不满足";
    }

    /**
     * 以本次原计划关联核对实际动作班次，不能用其他后料或历史展示时间替代成功事实。
     *
     * @param context 隔离承接完成的上下文
     * @param machineCode 变化的运行侧机台
     * @param history 该侧唯一历史计划
     * @return 指定后料有正量且交替、供胚首班均兑现时返回 true
     */
    private boolean hasHistoricalHandoff(LhScheduleContext context, String machineCode, LhMouldChangePlan history) {
        PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(machineCode);
        if (Objects.isNull(history) || Objects.isNull(history.getId()) || Objects.isNull(event)
                || Objects.isNull(event.getPlan()) || !Objects.equals(history.getId(), event.getPlan().getId())
                || Objects.isNull(event.getPlannedShiftStartTime()) || Objects.isNull(event.getPlannedShiftEndTime())) {
            return false;
        }
        return context.getPreviousAlternateResultPlanMap().entrySet().stream()
                .filter(entry -> Objects.equals(history.getId(), entry.getValue().getId()))
                .map(Map.Entry::getKey)
                .filter(result -> Objects.equals(machineCode, result.getLhMachineCode()))
                .filter(result -> context.getScheduleResultList().contains(result))
                .filter(result -> context.matchesPreviousAlternateAfterMaterial(history,
                        result.getMaterialCode(), result.getProductStatus()))
                .filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0)
                .anyMatch(result -> this.isWithin(result.getMouldChangeStartTime(),
                        event.getPlannedShiftStartTime(), event.getPlannedShiftEndTime())
                        && this.hasSupplyFirstShiftQuantity(context, result, event));
    }

    /**
     * 历史目标落在供胚首班时，要求该后料真实正量从本班供胚之后开始，避免换模成功仍空班。
     *
     * @param context 完整真实班次和供胚来源
     * @param result 已关联指定后料的正量结果
     * @param event 历史班次边界
     * @return 非供胚首班目标，或本班供胚后已有正量时返回 true
     */
    private boolean hasSupplyFirstShiftQuantity(LhScheduleContext context, LhScheduleResult result,
            PreviousAlternatePlanReleaseEvent event) {
        CxEmbryoLhTime source = context.getStructureSwitchSourceMap().get(result.getStructureName());
        if (Objects.isNull(source) || !this.isWithin(source.getEarliestLhTime(),
                event.getPlannedShiftStartTime(), event.getPlannedShiftEndTime())) {
            return true;
        }
        LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), source.getEarliestLhTime());
        if (Objects.isNull(shift)) {
            return false;
        }
        Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
        return Objects.nonNull(quantity) && quantity > 0
                && this.isWithin(ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex()),
                        source.getEarliestLhTime(), shift.getShiftEndDateTime());
    }

    /**
     * 判断真实时间是否位于左闭右开的业务区间，缺失时间不能证明承接成功。
     *
     * @param value 待判断的实际时间
     * @param start 区间开始
     * @param end 区间结束
     * @return 时间有效且位于区间内时返回 true
     */
    private boolean isWithin(Date value, Date start, Date end) {
        return Objects.nonNull(value) && Objects.nonNull(start) && Objects.nonNull(end)
                && !value.before(start) && value.before(end);
    }
}
