package com.zlt.aps.tq.engine.handler;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.ITqDemandQtyStrategy;
import com.zlt.aps.tq.engine.strategy.TqDemandCalcHelper;
import com.zlt.aps.tq.engine.strategy.TqStrategyRegistry;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * S2.2 需求量计算Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>调用 {@link ITqDemandQtyStrategy#calcDemandQty} 做收尾判断，设置 closeOutSpecFlag</li>
 *   <li>调用 {@link TqDemandCalcHelper#setStatusAndCloseTip} 设置收尾提示标识和生产状态</li>
 * </ol>
 *
 * <p>注意：备库触发判断和 6 班计划量计算已移至 S2.3 {@code TqPlanQtyCalcHandler}，
 * 因为备库触发依赖于每班排产后的可用库存状态。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqDemandQtyCalcHandler extends AbsTqScheduleStepHandler {

    /** 默认策略编码 */
    private static final String DEFAULT_STRATEGY_CODE = "DEFAULT";

    @Resource
    private TqStrategyRegistry strategyRegistry;

    @Override
    protected String getStepName() {
        return "S2.2-需求量计算";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        String strategyCode = resolveDemandQtyStrategyCode(params);
        ITqDemandQtyStrategy strategy = strategyRegistry.getDemandQtyStrategy(strategyCode);
        log.info("[S2.2] 使用需求量策略: {} ({})", strategyCode, strategy.getClass().getSimpleName());

        // 1. 遍历排程列表，做收尾判断（产出 closeOutSpecFlag）
        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            strategy.calcDemandQty(scheduleVo, context);

            // 2. 设置收尾提示标识和生产状态（基于胎胚关联汇总判断）
            TqDemandCalcHelper.setStatusAndCloseTip(scheduleVo, context);
        }

        log.info("[S2.2] 需求量计算完成, 排程记录数={}", context.getScheduleList().size());
    }

    /**
     * 解析需求量策略编码。
     *
     * <p>优先级：新参数 {@code TQ_DEMAND_QTY_STRATEGY_CODE} &gt; 默认 DEFAULT。</p>
     *
     * @param params 排程参数
     * @return 策略编码
     */
    private String resolveDemandQtyStrategyCode(TqScheduleParams params) {
        String code = params.getDemandQtyStrategyCode();
        return (code == null || code.isEmpty()) ? DEFAULT_STRATEGY_CODE : code;
    }
}
