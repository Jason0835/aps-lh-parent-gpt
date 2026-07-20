package com.zlt.aps.gsq.engine.strategy.impl;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定点机台过滤策略。
 *
 * <p>规则：</p>
 * <ol>
 *   <li>如果规格配置了"限定机台"(specifyCanMachineMap)，则只保留这些机台</li>
 *   <li>如果规格配置了"不可作业机台"(specifyNotMachineMap)，则排除这些机台</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class SpecifyMachineFilter implements IMachineFilterStrategy {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public String getName() {
        return "定点机台过滤";
    }

    @Override
    public List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                        GsqScheduleResultVo scheduleVo,
                                        GsqScheduleContext context) {
        String steelRingCode = scheduleVo.getSteelRingCode();
        Map<String, String> canMap = context.getSpecifyCanMachineMap();
        Map<String, String> notMap = context.getSpecifyNotMachineMap();

        // 1. 限定机台：只保留配置中指定的机台
        String canMachineStr = canMap.get(steelRingCode);
        if (canMachineStr != null && !canMachineStr.isEmpty()) {
            List<String> canMachineIds = Arrays.asList(canMachineStr.split(","));
            machines = machines.stream()
                    .filter(m -> canMachineIds.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
            log.debug("[{}] 规格[{}] 限定机台过滤后剩余: {}台", getName(), steelRingCode, machines.size());
        }

        // 2. 不可作业机台：排除配置中指定的机台
        String notMachineStr = notMap.get(steelRingCode);
        if (notMachineStr != null && !notMachineStr.isEmpty()) {
            List<String> notMachineIds = Arrays.asList(notMachineStr.split(","));
            machines = machines.stream()
                    .filter(m -> !notMachineIds.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
            log.debug("[{}] 规格[{}] 排除不可作业机台后剩余: {}台", getName(), steelRingCode, machines.size());
        }

        return machines;
    }
}
