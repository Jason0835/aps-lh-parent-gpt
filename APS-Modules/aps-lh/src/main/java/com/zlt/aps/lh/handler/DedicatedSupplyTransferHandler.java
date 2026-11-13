package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.service.impl.DedicatedSupplyTransferService;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * 结构切换及回补后的硫化机专供成型机转产处理。
 */
@Component
public class DedicatedSupplyTransferHandler extends AbsScheduleStepHandler {
    /**
     * 共享首次消费前快照，结构切换跳过时仍需初始化。
     */
    @Resource
    private NewProductionHandler newProductionHandler;

    /**
     * 原子下机与后料排产服务。
     */
    @Resource
    private DedicatedSupplyTransferService transferService;

    /**
     * 冻结首次消费前候选并执行独立专供转产。
     *
     * @param context 结构切换及回补完成的上下文
     */
    @Override
    protected void doHandle(LhScheduleContext context) {
        context.setCurrentStep(ScheduleStepEnum.S4_5_4_DEDICATED_SUPPLY_TRANSFER.getCode());
        newProductionHandler.prepareCandidateSnapshots(context);
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
