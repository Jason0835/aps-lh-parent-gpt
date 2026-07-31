package com.zlt.aps.tq.engine.handler;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.ITqSupplyTimeStrategy;
import com.zlt.aps.tq.engine.strategy.TqDemandCalcHelper;
import com.zlt.aps.tq.engine.strategy.TqStrategyRegistry;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S2.1 库存预测Handler。
 *
 * <p>职责（仅计算供应时长，不做计划量计算）：</p>
 * <ol>
 *   <li>根据参数 {@code TQ_SUPPLY_TIME_STRATEGY_CODE} 或兼容旧参数 {@code demandCalcMode} 路由策略</li>
 *   <li>遍历排程列表，调用 {@link ITqSupplyTimeStrategy#calcSupplyTime} 计算每条记录的供应时长</li>
 *   <li>算法1（BY_STOCK）模式下，按库存保证班数不足备库班数的规格做筛选</li>
 * </ol>
 *
 * <p>注意：原 {@code TqDemandCalcHandler.doHandle} 中的计划量计算和收尾判断已拆分至
 * S2.2 {@code TqDemandQtyCalcHandler} 和 S2.3 {@code TqPlanQtyCalcHandler}。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqStockPredictHandler extends AbsTqScheduleStepHandler {

    /** 策略编码常量：算法1（线下手工排产） */
    private static final String STRATEGY_CODE_BY_STOCK = "BY_STOCK";
    /** 策略编码常量：算法2（系统算法，逐班递减） */
    private static final String STRATEGY_CODE_BY_SHIFT = "BY_SHIFT";
    /** 兼容旧参数 demandCalcMode=1 */
    private static final Integer LEGACY_DEMAND_CALC_MODE_BY_STOCK = 1;

    @Resource
    private TqStrategyRegistry strategyRegistry;

    @Override
    protected String getStepName() {
        return "S2.1-库存预测";
    }

    @Override
    protected void doHandle(TqScheduleContext context) {
        TqScheduleParams params = context.getParams();

        // 解析供应时长策略编码：优先用新参数，兼容旧 demandCalcMode
        String strategyCode = resolveSupplyTimeStrategyCode(params);
        ITqSupplyTimeStrategy strategy = strategyRegistry.getSupplyTimeStrategy(strategyCode);
        log.info("[S2.1] 使用供应时长策略: {} ({})", strategyCode, strategy.getClass().getSimpleName());

        // 1. 遍历排程列表，计算每条记录的库存供应时长
        for (TqScheduleResultVo scheduleVo : context.getScheduleList()) {
            strategy.calcSupplyTime(scheduleVo, scheduleVo.getPlanStockQty(), context);
        }

        // 2. 算法1模式筛选：库存保证班数不足备库班数的规格（算法2不需要筛选）
        if (STRATEGY_CODE_BY_STOCK.equals(strategyCode)) {
            double backupShiftCount = params.getBackupShiftCount() == null ? 5D : params.getBackupShiftCount();
            List<TqScheduleResultVo> filteredList = context.getScheduleList().stream()
                    .filter(s -> {
                        double guaranteeShifts = s.getSupplyTime() == null ? 0D : s.getSupplyTime() / 8;
                        return guaranteeShifts < backupShiftCount;
                    })
                    .collect(Collectors.toList());
            context.setScheduleList(filteredList);
            log.info("[S2.1] 算法1模式筛选：库存保证班数不足{}班的规格数={}", (int) backupShiftCount, filteredList.size());
        }
    }

    /**
     * 解析供应时长策略编码。
     *
     * <p>优先级：</p>
     * <ol>
     *   <li>新参数 {@code TQ_SUPPLY_TIME_STRATEGY_CODE}</li>
     *   <li>旧参数 {@code demandCalcMode=1} → BY_STOCK，否则 → BY_SHIFT（默认）</li>
     * </ol>
     *
     * @param params 排程参数
     * @return 策略编码
     */
    private String resolveSupplyTimeStrategyCode(TqScheduleParams params) {
        String newCode = params.getSupplyTimeStrategyCode();
        if (newCode != null && !newCode.isEmpty()) {
            return newCode;
        }
        // 兼容旧参数 demandCalcMode
        Integer legacyMode = params.getDemandCalcMode();
        if (LEGACY_DEMAND_CALC_MODE_BY_STOCK.equals(legacyMode)) {
            return STRATEGY_CODE_BY_STOCK;
        }
        return STRATEGY_CODE_BY_SHIFT;
    }
}
