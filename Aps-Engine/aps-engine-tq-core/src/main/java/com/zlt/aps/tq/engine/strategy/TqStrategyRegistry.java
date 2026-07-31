package com.zlt.aps.tq.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈排程策略注册表。
 *
 * <p>通过 Spring 注入收集供应时长、需求量、计划量三类策略，并按编码注册。
 * 获取不存在的策略时明确抛出异常，避免主流程静默走错算法。</p>
 *
 * <p>支持通过以下参数动态切换策略：</p>
 * <ul>
 *   <li>{@code TQ_SUPPLY_TIME_STRATEGY_CODE}：供应时长算法（默认 BY_STOCK，兼容原 demandCalcMode=1）</li>
 *   <li>{@code TQ_DEMAND_QTY_STRATEGY_CODE}：需求量算法（默认 DEFAULT）</li>
 *   <li>{@code TQ_PLAN_QTY_STRATEGY_CODE}：计划量算法（默认 DEFAULT）</li>
 * </ul>
 *
 * <p>兼容性：原 {@code demandCalcMode} 参数保留 1 个版本，
 * 在各 Handler 中做策略 Code 映射后弃用。</p>
 *
 * @author APS
 */
@Component
public class TqStrategyRegistry {

    /** 供应时长策略 Map，key=策略编码 */
    private final Map<String, ITqSupplyTimeStrategy> supplyTimeStrategyMap = new HashMap<>();

    /** 需求量策略 Map，key=策略编码 */
    private final Map<String, ITqDemandQtyStrategy> demandQtyStrategyMap = new HashMap<>();

    /** 计划量策略 Map，key=策略编码 */
    private final Map<String, ITqPlanQtyStrategy> planQtyStrategyMap = new HashMap<>();

    /**
     * 创建策略注册表。
     *
     * <p>Spring 自动注入所有 {@link ITqSupplyTimeStrategy}、{@link ITqDemandQtyStrategy}、
     * {@link ITqPlanQtyStrategy} 实现类，并按各自 {@code getStrategyCode()} 注册到 Map。</p>
     *
     * @param supplyTimeStrategies 供应时长策略集合
     * @param demandQtyStrategies  需求量策略集合
     * @param planQtyStrategies    计划量策略集合
     */
    public TqStrategyRegistry(List<ITqSupplyTimeStrategy> supplyTimeStrategies,
                              List<ITqDemandQtyStrategy> demandQtyStrategies,
                              List<ITqPlanQtyStrategy> planQtyStrategies) {
        registerSupplyTimeStrategies(supplyTimeStrategies);
        registerDemandQtyStrategies(demandQtyStrategies);
        registerPlanQtyStrategies(planQtyStrategies);
    }

    /**
     * 获取供应时长策略。
     *
     * @param strategyCode 策略编码
     * @return 供应时长策略
     * @throws ServiceException 策略编码未注册时抛出
     */
    public ITqSupplyTimeStrategy getSupplyTimeStrategy(String strategyCode) {
        ITqSupplyTimeStrategy strategy = supplyTimeStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new ServiceException("胎圈供应时长策略未注册: " + strategyCode);
        }
        return strategy;
    }

    /**
     * 获取需求量策略。
     *
     * @param strategyCode 策略编码
     * @return 需求量策略
     * @throws ServiceException 策略编码未注册时抛出
     */
    public ITqDemandQtyStrategy getDemandQtyStrategy(String strategyCode) {
        ITqDemandQtyStrategy strategy = demandQtyStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new ServiceException("胎圈需求量策略未注册: " + strategyCode);
        }
        return strategy;
    }

    /**
     * 获取计划量策略。
     *
     * @param strategyCode 策略编码
     * @return 计划量策略
     * @throws ServiceException 策略编码未注册时抛出
     */
    public ITqPlanQtyStrategy getPlanQtyStrategy(String strategyCode) {
        ITqPlanQtyStrategy strategy = planQtyStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new ServiceException("胎圈计划量策略未注册: " + strategyCode);
        }
        return strategy;
    }

    /**
     * 注册供应时长策略，重复编码抛 IllegalStateException。
     */
    private void registerSupplyTimeStrategies(List<ITqSupplyTimeStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITqSupplyTimeStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITqSupplyTimeStrategy existing = supplyTimeStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎圈供应时长策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            supplyTimeStrategyMap.put(code, strategy);
        }
    }

    /**
     * 注册需求量策略，重复编码抛 IllegalStateException。
     */
    private void registerDemandQtyStrategies(List<ITqDemandQtyStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITqDemandQtyStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITqDemandQtyStrategy existing = demandQtyStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎圈需求量策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            demandQtyStrategyMap.put(code, strategy);
        }
    }

    /**
     * 注册计划量策略，重复编码抛 IllegalStateException。
     */
    private void registerPlanQtyStrategies(List<ITqPlanQtyStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITqPlanQtyStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITqPlanQtyStrategy existing = planQtyStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎圈计划量策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            planQtyStrategyMap.put(code, strategy);
        }
    }
}
