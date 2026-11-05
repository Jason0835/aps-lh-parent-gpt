package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.component.NewSpecDelayDaysResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.service.impl.StructureEndingContinuationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 结构切换之后、普通新增之前的结构收尾续作增机回补步骤。
 *
 * @author Codex
 * @date 2026/10/29
 */
@Component
public class StructureEndingContinuationHandler extends AbsScheduleStepHandler {
    /**
     * 三步规则后两步的独立编排。
     */
    @Resource
    private StructureEndingContinuationService schedulingService;
    /**
     * 复用新增首次消费前的候选快照。
     */
    @Resource
    private NewProductionHandler newProductionHandler;
    /**
     * 沿用新增排序的当前延误天数。
     */
    @Resource
    private NewSpecDelayDaysResolver delayDaysResolver;

    /**
     * 仅在存在已接管需求且未中断时执行独立步骤。
     *
     * @param context 结构切换结束后的上下文
     */
    public void handleIfApplicable(LhScheduleContext context) {
        if (context.isInterrupted() || context.getStructureEndingContinuationStates().isEmpty()) {
            return;
        }
        context.setCurrentStep(ScheduleStepEnum.S4_4_8_STRUCTURE_ENDING_CONTINUATION.getCode());
        this.handle(context);
    }

    /**
     * 保存候选快照，刷新排序字段并完成增机和原机回补。
     *
     * @param context 共享实时上下文
     */
    @Override
    protected void doHandle(LhScheduleContext context) {
        newProductionHandler.prepareCandidateSnapshots(context);
        delayDaysResolver.refreshDelayDays(context);
        schedulingService.schedule(context);
    }

    /**
     * 返回本步骤的中文名称。
     *
     * @return 结构收尾续作增机回补步骤名称
     */
    @Override
    protected String getStepName() {
        return ScheduleStepEnum.S4_4_8_STRUCTURE_ENDING_CONTINUATION.getDescription();
    }
}
