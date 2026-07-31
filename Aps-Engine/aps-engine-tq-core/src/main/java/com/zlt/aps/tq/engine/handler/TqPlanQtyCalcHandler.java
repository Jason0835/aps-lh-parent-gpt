package com.zlt.aps.tq.engine.handler;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.ITqPlanQtyStrategy;
import com.zlt.aps.tq.engine.strategy.TqStrategyRegistry;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * S2.3 计划量计算Handler。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>调用 {@link ITqPlanQtyStrategy#calcPlanQty} 执行 6 班滚动计划量计算</li>
 *   <li>策略内部包含备库触发判断、备库分摊、计划量取整与工装限制</li>
 *   <li>累加各班计划量到 {@link TqTotalPlanQtyVo}</li>
 * </ol>
 *
 * <p>注意：本 Handler 依赖 S2.2 产出的 closeOutSpecFlag，因此必须先执行 S2.2。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqPlanQtyCalcHandler extends AbsTqScheduleStepHandler {

    /** 默认策略编码 */
    private static final String DEFAULT_STRATEGY_CODE = "DEFAULT";

    @Resource
    private TqStrategyRegistry strategyRegistry;

    @Override
    protected String getStepName() {
        return "S2.3-计划量计算";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        TqScheduleParams params = context.getParams();
        TqTotalPlanQtyVo totalPlanQtyVo = context.getTotalPlanQtyVo();

        String strategyCode = resolvePlanQtyStrategyCode(params);
        ITqPlanQtyStrategy strategy = strategyRegistry.getPlanQtyStrategy(strategyCode);
        log.info("[S2.3] 使用计划量策略: {} ({})", strategyCode, strategy.getClass().getSimpleName());

        // 遍历排程列表，调用策略计算 6 班计划量
        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            strategy.calcPlanQty(scheduleVo, totalPlanQtyVo, context);
        }

        log.info("[S2.3] 计划量计算完成, 总计划量:{}", toJSONString(totalPlanQtyVo));
    }

    /**
     * 解析计划量策略编码。
     *
     * <p>优先级：新参数 {@code TQ_PLAN_QTY_STRATEGY_CODE} &gt; 默认 DEFAULT。</p>
     *
     * @param params 排程参数
     * @return 策略编码
     */
    private String resolvePlanQtyStrategyCode(TqScheduleParams params) {
        String code = params.getPlanQtyStrategyCode();
        return (code == null || code.isEmpty()) ? DEFAULT_STRATEGY_CODE : code;
    }
}
