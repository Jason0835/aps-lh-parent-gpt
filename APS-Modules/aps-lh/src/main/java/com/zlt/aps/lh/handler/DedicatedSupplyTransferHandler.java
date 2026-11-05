package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.service.impl.DedicatedSupplyTransferService;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * 新增排产完成后的硫化机专供成型机转产处理。
 */
@Component
public class DedicatedSupplyTransferHandler extends AbsScheduleStepHandler {
    /**
     * 原子下机与后料排产服务。
     */
    @Resource
    private DedicatedSupplyTransferService transferService;

    /**
     * 执行独立后处理，不修改前置排产步骤的准入规则。
     *
     * @param context 新增排产完成的上下文
     */
    @Override
    protected void doHandle(LhScheduleContext context) {
        context.setCurrentStep(ScheduleStepEnum.S4_5_4_DEDICATED_SUPPLY_TRANSFER.getCode());
        transferService.process(context);
    }

    /**
     * 返回本步骤名称。
     *
     * @return 中文步骤名称
     */
    @Override
    protected String getStepName() {
        return ScheduleStepEnum.S4_5_4_DEDICATED_SUPPLY_TRANSFER.getDescription();
    }
}
