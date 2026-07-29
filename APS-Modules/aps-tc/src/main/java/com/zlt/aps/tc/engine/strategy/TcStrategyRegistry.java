package com.zlt.aps.tc.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎侧排程策略注册表。
 *
 * <p>通过 Spring 注入收集需求量、计划量、机台过滤、机台评分和任务排序策略，并按编码注册。
 * 获取不存在的策略时明确抛出异常，避免主流程静默走错算法。</p>
 */
@Component
public class TcStrategyRegistry {

    private final Map<String, ITcDemandQtyStrategy> demandQtyStrategyMap = new HashMap<>();

    private final Map<String, ITcPlanQtyStrategy> planQtyStrategyMap = new HashMap<>();

    private final Map<String, ITcMachineFilterRule> machineFilterRuleMap = new HashMap<>();

    private final Map<String, ITcMachineScoreStrategy> machineScoreStrategyMap = new HashMap<>();

    private final Map<String, ITcTaskSortStrategy> taskSortStrategyMap = new HashMap<>();

    private final Map<String, ITcChainTaskPriorityStrategy> chainTaskPriorityStrategyMap = new HashMap<>();

    /**
     * 创建策略注册表。
     *
     * @param demandQtyStrategies     需求量策略集合
     * @param planQtyStrategies       计划量策略集合
     * @param machineFilterRules      机台过滤规则集合
     * @param machineScoreStrategies  机台评分策略集合
     * @param taskSortStrategies      任务排序策略集合
     * @param chainTaskPriorityStrategies 班次内任务优先策略集合
     */
    public TcStrategyRegistry(List<ITcDemandQtyStrategy> demandQtyStrategies,
                              List<ITcPlanQtyStrategy> planQtyStrategies,
                              List<ITcMachineFilterRule> machineFilterRules,
                              List<ITcMachineScoreStrategy> machineScoreStrategies,
                              List<ITcTaskSortStrategy> taskSortStrategies,
                              List<ITcChainTaskPriorityStrategy> chainTaskPriorityStrategies) {
        registerDemandQtyStrategies(demandQtyStrategies);
        registerPlanQtyStrategies(planQtyStrategies);
        registerMachineFilterRules(machineFilterRules);
        registerMachineScoreStrategies(machineScoreStrategies);
        registerTaskSortStrategies(taskSortStrategies);
        registerChainTaskPriorityStrategies(chainTaskPriorityStrategies);
    }

    /**
     * 获取需求量策略。
     *
     * @param algorithmCode 算法编码
     * @return 需求量策略
     */
    public ITcDemandQtyStrategy getDemandQtyStrategy(String algorithmCode) {
        ITcDemandQtyStrategy strategy = demandQtyStrategyMap.get(algorithmCode);
        if (strategy == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_STRATEGY_NOT_REGISTERED.getDefaultMessage() + ":" + algorithmCode);
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
    public ITcPlanQtyStrategy getPlanQtyStrategy(String strategyCode) {
        ITcPlanQtyStrategy strategy = planQtyStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_STRATEGY_NOT_REGISTERED.getDefaultMessage() + ":" + strategyCode);
        }
        return strategy;
    }

    /**
     * 获取机台过滤规则。
     *
     * @param ruleCode 规则编码
     * @return 机台过滤规则
     * @throws ServiceException 规则编码未注册时抛出
     */
    public ITcMachineFilterRule getMachineFilterRule(String ruleCode) {
        ITcMachineFilterRule rule = machineFilterRuleMap.get(ruleCode);
        if (rule == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_STRATEGY_NOT_REGISTERED.getDefaultMessage() + ":" + ruleCode);
        }
        return rule;
    }

    /**
     * 获取机台评分策略。
     *
     * @param strategyCode 策略编码
     * @return 机台评分策略
     * @throws ServiceException 策略编码未注册时抛出
     */
    public ITcMachineScoreStrategy getMachineScoreStrategy(String strategyCode) {
        ITcMachineScoreStrategy strategy = machineScoreStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_STRATEGY_NOT_REGISTERED.getDefaultMessage() + ":" + strategyCode);
        }
        return strategy;
    }

    /**
     * 获取任务排序策略。
     *
     * @param strategyCode 策略编码
     * @return 任务排序策略
     * @throws ServiceException 策略编码未注册时抛出
     */
    public ITcTaskSortStrategy getTaskSortStrategy(String strategyCode) {
        ITcTaskSortStrategy strategy = taskSortStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_STRATEGY_NOT_REGISTERED.getDefaultMessage() + ":" + strategyCode);
        }
        return strategy;
    }

    /**
     * 获取班次内任务优先策略。
     *
     * @param strategyCode 策略编码
     * @return 班次内任务优先策略
     * @throws ServiceException 策略编码未注册时抛出
     */
    public ITcChainTaskPriorityStrategy getChainTaskPriorityStrategy(String strategyCode) {
        ITcChainTaskPriorityStrategy strategy = chainTaskPriorityStrategyMap.get(strategyCode);
        if (strategy == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_STRATEGY_NOT_REGISTERED.getDefaultMessage() + ":" + strategyCode);
        }
        return strategy;
    }

    private void registerDemandQtyStrategies(List<ITcDemandQtyStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITcDemandQtyStrategy strategy : strategies) {
            String code = strategy.getAlgorithmCode();
            ITcDemandQtyStrategy existing = demandQtyStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎侧需求量算法编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            demandQtyStrategyMap.put(code, strategy);
        }
    }

    private void registerPlanQtyStrategies(List<ITcPlanQtyStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITcPlanQtyStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITcPlanQtyStrategy existing = planQtyStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎侧计划量策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            planQtyStrategyMap.put(code, strategy);
        }
    }

    private void registerMachineFilterRules(List<ITcMachineFilterRule> rules) {
        if (rules == null) {
            return;
        }
        for (ITcMachineFilterRule rule : rules) {
            String code = rule.getRuleCode();
            ITcMachineFilterRule existing = machineFilterRuleMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎侧机台过滤规则编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + rule.getClass().getSimpleName());
            }
            machineFilterRuleMap.put(code, rule);
        }
    }

    private void registerMachineScoreStrategies(List<ITcMachineScoreStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITcMachineScoreStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITcMachineScoreStrategy existing = machineScoreStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎侧机台评分策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            machineScoreStrategyMap.put(code, strategy);
        }
    }

    private void registerTaskSortStrategies(List<ITcTaskSortStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITcTaskSortStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITcTaskSortStrategy existing = taskSortStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎侧任务排序策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            taskSortStrategyMap.put(code, strategy);
        }
    }

    private void registerChainTaskPriorityStrategies(List<ITcChainTaskPriorityStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (ITcChainTaskPriorityStrategy strategy : strategies) {
            String code = strategy.getStrategyCode();
            ITcChainTaskPriorityStrategy existing = chainTaskPriorityStrategyMap.get(code);
            if (existing != null) {
                throw new IllegalStateException(
                        "胎侧班次内任务优先策略编码重复: " + code
                                + "，已注册: " + existing.getClass().getSimpleName()
                                + "，新注册: " + strategy.getClass().getSimpleName());
            }
            chainTaskPriorityStrategyMap.put(code, strategy);
        }
    }
}
