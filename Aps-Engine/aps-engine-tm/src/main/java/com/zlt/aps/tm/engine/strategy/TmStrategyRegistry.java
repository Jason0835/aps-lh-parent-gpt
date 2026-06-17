package com.zlt.aps.tm.engine.strategy;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎面排程策略注册表。
 *
 * <p>通过 Spring 注入收集需求量、计划量、机台过滤、机台评分和任务排序策略，并按编码注册。
 * 获取不存在的策略时明确抛出异常，避免主流程静默走错算法。</p>
 */
@Component
public class TmStrategyRegistry {

    private final Map<String, ITmDemandQtyStrategy> demandQtyStrategyMap = new HashMap<>();

    private final Map<String, ITmPlanQtyStrategy> planQtyStrategyMap = new HashMap<>();

    private final Map<String, ITmMachineFilterRule> machineFilterRuleMap = new HashMap<>();

    private final Map<String, ITmMachineScoreStrategy> machineScoreStrategyMap = new HashMap<>();

    private final Map<String, ITmTaskSortStrategy> taskSortStrategyMap = new HashMap<>();

    /**
     * 创建策略注册表。
     *
     * @param demandQtyStrategies     需求量策略集合
     * @param planQtyStrategies       计划量策略集合
     * @param machineFilterRules      机台过滤规则集合
     * @param machineScoreStrategies  机台评分策略集合
     * @param taskSortStrategies      任务排序策略集合
     */
    public TmStrategyRegistry(List<ITmDemandQtyStrategy> demandQtyStrategies,
                              List<ITmPlanQtyStrategy> planQtyStrategies,
                              List<ITmMachineFilterRule> machineFilterRules,
                              List<ITmMachineScoreStrategy> machineScoreStrategies,
                              List<ITmTaskSortStrategy> taskSortStrategies) {
        registerDemandQtyStrategies(demandQtyStrategies);
        registerPlanQtyStrategies(planQtyStrategies);
        registerMachineFilterRules(machineFilterRules);
        registerMachineScoreStrategies(machineScoreStrategies);
        registerTaskSortStrategies(taskSortStrategies);
    }

    /**
     * 获取需求量策略。
     *
     * @param algorithmCode 算法编码
     * @return 需求量策略
     */
    public ITmDemandQtyStrategy getDemandQtyStrategy(String algorithmCode) {
        ITmDemandQtyStrategy strategy = demandQtyStrategyMap.get(algorithmCode);
        if (strategy == null) {
            throw new IllegalArgumentException("未注册胎面需求量算法:" + algorithmCode);
        }
        return strategy;
    }

    /**
     * 获取计划量策略。
     *
     * @param strategyCode 策略编码
     * @return 计划量策略
     * @throws IllegalArgumentException 策略编码未注册时抛出
     */
    public ITmPlanQtyStrategy getPlanQtyStrategy(String strategyCode) {
        ITmPlanQtyStrategy strategy = planQtyStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new IllegalArgumentException("未注册胎面计划量策略:" + strategyCode);
        }
        return strategy;
    }

    /**
     * 获取机台过滤规则。
     *
     * @param ruleCode 规则编码
     * @return 机台过滤规则
     * @throws IllegalArgumentException 规则编码未注册时抛出
     */
    public ITmMachineFilterRule getMachineFilterRule(String ruleCode) {
        ITmMachineFilterRule rule = machineFilterRuleMap.get(ruleCode);
        if (rule == null) {
            throw new IllegalArgumentException("未注册胎面机台过滤规则:" + ruleCode);
        }
        return rule;
    }

    /**
     * 获取机台评分策略。
     *
     * @param strategyCode 策略编码
     * @return 机台评分策略
     * @throws IllegalArgumentException 策略编码未注册时抛出
     */
    public ITmMachineScoreStrategy getMachineScoreStrategy(String strategyCode) {
        ITmMachineScoreStrategy strategy = machineScoreStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new IllegalArgumentException("未注册胎面机台评分策略:" + strategyCode);
        }
        return strategy;
    }

    /**
     * 获取任务排序策略。
     *
     * @param strategyCode 策略编码
     * @return 任务排序策略
     * @throws IllegalArgumentException 策略编码未注册时抛出
     */
    public ITmTaskSortStrategy getTaskSortStrategy(String strategyCode) {
        ITmTaskSortStrategy strategy = taskSortStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new IllegalArgumentException("未注册胎面任务排序策略:" + strategyCode);
        }
        return strategy;
    }

    private void registerDemandQtyStrategies(List<ITmDemandQtyStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITmDemandQtyStrategy strategy : strategies) {
            String code = strategy.getAlgorithmCode();
            ITmDemandQtyStrategy existing = demandQtyStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎面需求量算法编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            demandQtyStrategyMap.put(code, strategy);
        }
    }

    private void registerPlanQtyStrategies(List<ITmPlanQtyStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITmPlanQtyStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITmPlanQtyStrategy existing = planQtyStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎面计划量策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            planQtyStrategyMap.put(code, strategy);
        }
    }

    private void registerMachineFilterRules(List<ITmMachineFilterRule> rules) {
        if (rules == null) {
            return;
        }
        for (ITmMachineFilterRule rule : rules) {
            String code = rule.getRuleCode();
            ITmMachineFilterRule existing = machineFilterRuleMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎面机台过滤规则编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + rule.getClass().getSimpleName());
            }
            machineFilterRuleMap.put(code, rule);
        }
    }

    private void registerMachineScoreStrategies(List<ITmMachineScoreStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITmMachineScoreStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITmMachineScoreStrategy existing = machineScoreStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎面机台评分策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            machineScoreStrategyMap.put(code, strategy);
        }
    }

    private void registerTaskSortStrategies(List<ITmTaskSortStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITmTaskSortStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITmTaskSortStrategy existing = taskSortStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎面任务排序策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            taskSortStrategyMap.put(code, strategy);
        }
    }
}
