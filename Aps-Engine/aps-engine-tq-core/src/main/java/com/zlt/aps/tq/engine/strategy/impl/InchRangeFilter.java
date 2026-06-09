package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 寸口过滤策略。
 *
 * <p>过滤规则：根据胎圈的寸口（dimension）字段，过滤出寸口匹配的机台。
 * 机台可做的寸口值由 TqMachineChuck（机台-寸口绑定关系）决定。</p>
 *
 * <p>过滤逻辑：</p>
 * <ol>
 *   <li>如果排程记录无寸口信息（dimension为null），跳过过滤，保留所有候选机台</li>
 *   <li>如果机台在 TqMachineChuck 中有寸口绑定记录，则只有寸口值匹配时才保留</li>
 *   <li>如果机台在 TqMachineChuck 中无任何寸口绑定记录（未配置），默认保留（兼容未配置寸口的情况）</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class InchRangeFilter implements IMachineFilterStrategy {

    @Override
    public List<TqMachineInfo> filter(List<TqMachineInfo> candidateMachines, TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        BigDecimal dimension = scheduleVo.getDimension();
        if (dimension == null) {
            // 无寸口信息，不进行过滤
            log.debug("[寸口过滤] 胎圈{}无寸口信息，跳过过滤", scheduleVo.getBeadCode());
            return candidateMachines;
        }

        List<TqMachineInfo> filtered = candidateMachines.stream().filter(m -> {
            List<BigDecimal> chuckList = context.getMachineChuckMap().get(m.getId());
            // 未配置寸口绑定关系的机台，默认保留（兼容未配置的情况）
            if (chuckList == null || chuckList.isEmpty()) {
                return true;
            }
            // 寸口值匹配则保留
            return chuckList.stream().anyMatch(chuck -> chuck.compareTo(dimension) == 0);
        }).collect(Collectors.toList());

        log.debug("[寸口过滤] 胎圈{}寸口={}, 候选机台数={} → 过滤后={}",
                scheduleVo.getBeadCode(), dimension, candidateMachines.size(), filtered.size());

        return filtered;
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public String getStrategyName() {
        return "寸口过滤";
    }
}
