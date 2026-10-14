package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.component.NewSpecDelayDaysResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;
import com.zlt.aps.lh.service.impl.StructureSwitchSchedulingService;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * 续作及换活字块后的独立结构切换步骤，只接管已冻结的有效候选。
 * @author APS
 * @since 2026/10/09
 */
@Component
public class StructureSwitchProductionHandler extends AbsScheduleStepHandler {
    /**
     * 负责真实班次推进及结构切换组合竞争。
     */
    @Resource
    private StructureSwitchSchedulingService schedulingService;
    /**
     * 共享新增侧首次消费前的后置候选快照。
     */
    @Resource
    private NewProductionHandler newProductionHandler;
    /**
     * 复用既有SKU业务排序字段计算。
     */
    @Resource
    private NewSpecDelayDaysResolver delayDaysResolver;

    /**
     * 无候选或关闭特殊规则时完全跳过，不初始化资源、改步骤或触发候选消费。
     *
     * @param context 换活字块完成后的实时上下文
     */
    public void handleIfApplicable(LhScheduleContext context) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context)
                || context.getStructureSwitchCandidateSkuMap().isEmpty() || context.isInterrupted()
                || !schedulingService.hasRemainingCandidates(context)) {
            return;
        }
        context.setCurrentStep(ScheduleStepEnum.S4_4_7_STRUCTURE_SWITCH.getCode());
        this.handle(context);
    }

    /**
     * 准备首次消费前快照与排序字段，执行独立结构切换排产。
     *
     * @param context 续作和换活字块完成后的实时排程上下文
     */
    @Override
    protected void doHandle(LhScheduleContext context) {
        schedulingService.prepareCandidates(context);
        newProductionHandler.prepareCandidateSnapshots(context);
        delayDaysResolver.refreshDelayDays(context);
        schedulingService.schedule(context);
    }

    /**
     * 获取本步骤的中文名称。
     *
     * @return 结构切换排产步骤名称
     */
    @Override
    protected String getStepName() {
        return ScheduleStepEnum.S4_4_7_STRUCTURE_SWITCH.getDescription();
    }
}
