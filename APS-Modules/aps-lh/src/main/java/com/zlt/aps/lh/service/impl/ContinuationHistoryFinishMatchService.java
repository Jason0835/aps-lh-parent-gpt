package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * 隔离复核余量收尾对调的前料守恒和设备事实，后料承接在对调确定后正式执行。
 * 后料次数不足或跨历史班次不反向改变已经合法的前料错开方案。
 *
 * @author Codex
 * @date 2026/10/30
 */
@Slf4j
@Service
public class ContinuationHistoryFinishMatchService {

    /**
     * 临时提交候选并执行续作最终化及设备复评，成功、拒绝和异常均恢复正式状态。
     * 本步骤不消费后料资源；正式后料从最终释放时刻尝试换模、首检及窗口内顺延。
     *
     * @param context 已有正式时间下机决定但尚未扣账的上下文
     * @param histories 本次参与匹配的可靠历史关系，仅用于诊断
     * @param applyCandidate 原统一提交入口，数量或时间硬校验失败时返回false
     * @param prepareHandoff 复用原最终化及设备复评，前料方案仍守恒时返回true
     * @return 前料候选和设备约束均通过时返回true
     */
    public boolean validate(LhScheduleContext context, Map<String, LhMouldChangePlan> histories,
            BooleanSupplier applyCandidate, BooleanSupplier prepareHandoff) {
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        sources.addAll(context.getScheduleResultSourceSkuMap().values());
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        boolean accepted = false;
        try {
            // 抑制最终化中的递归匹配，数量、账本及设备状态仍由原入口完整校验。
            context.setPreviousAlternateGroupPreview(true);
            accepted = applyCandidate.getAsBoolean() && prepareHandoff.getAsBoolean();
        } finally {
            snapshot.restore(context);
        }
        String detail = "工厂=" + context.getFactoryCode() + ", 批次=" + context.getBatchNo()
                + ", 机台=" + histories.keySet() + ", 通过=" + accepted
                + ", 结论=" + (accepted ? "前料对调守恒及设备复核通过，后料按真实释放正式承接并允许次数顺延"
                : "候选数量或设备约束未通过，保留原错开分配")
                + ", 正式结果及资源账本=已恢复";
        PriorityTraceLogHelper.appendProcessLog(context, "余量收尾历史匹配承接复核", detail);
        log.info("余量收尾历史匹配承接复核, {}", detail);
        return accepted;
    }
}
