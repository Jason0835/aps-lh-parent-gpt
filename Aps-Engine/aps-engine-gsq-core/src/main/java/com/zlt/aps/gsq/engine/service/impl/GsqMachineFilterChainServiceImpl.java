package com.zlt.aps.gsq.engine.service.impl;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.service.IGsqMachineFilterChainService;
import com.zlt.aps.gsq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钢丝圈机台过滤策略链Service实现。
 *
 * <p>按策略的order顺序串联执行所有过滤策略。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class GsqMachineFilterChainServiceImpl implements IGsqMachineFilterChainService {

    /** 策略列表（按order排序） */
    private List<IMachineFilterStrategy> strategies = new ArrayList<>();

    /**
     * 自动注入所有 IMachineFilterStrategy 实现并按order排序。
     */
    @PostConstruct
    public void init() {
        strategies.sort(Comparator.comparingInt(IMachineFilterStrategy::getOrder));
        log.info("[策略链] 初始化完成, 策略数量: {}, 顺序: {}",
                strategies.size(),
                strategies.stream()
                        .map(s -> s.getName() + "(" + s.getOrder() + ")")
                        .collect(Collectors.joining(" → ")));
    }

    @Override
    public List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                        GsqScheduleResultVo scheduleVo,
                                        GsqScheduleContext context) {
        if (machines == null || machines.isEmpty()) {
            return machines;
        }

        List<GsqMachineInfo> result = new ArrayList<>(machines);
        for (IMachineFilterStrategy strategy : strategies) {
            result = strategy.filter(result, scheduleVo, context);
            if (result.isEmpty()) {
                log.debug("[策略链] 策略[{}]过滤后无可用机台, 提前终止", strategy.getName());
                break;
            }
        }
        return result;
    }

    @Override
    public void registerStrategy(IMachineFilterStrategy strategy) {
        strategies.add(strategy);
    }
}
