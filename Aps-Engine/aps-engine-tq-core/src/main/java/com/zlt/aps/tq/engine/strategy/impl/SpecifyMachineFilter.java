package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定点机台过滤策略。
 *
 * <p>过滤规则：</p>
 * <ol>
 *   <li>如果胎圈代码在"限制作业"映射中存在，则只保留限制作业指定的机台</li>
 *   <li>如果胎圈代码在"不可作业"映射中存在，则排除不可作业指定的机台</li>
 *   <li>如果都不存在，则不进行过滤</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component
public class SpecifyMachineFilter implements IMachineFilterStrategy {

    @Override
    public List<TqMachineInfo> filter(List<TqMachineInfo> candidateMachines, TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        String beadCode = scheduleVo.getBeadCode();

        // 1. 限制作业：只保留指定机台
        String canMachineCodes = context.getSpecifyCanMachineMap().get(beadCode);
        if (canMachineCodes != null && !canMachineCodes.isEmpty()) {
            List<String> canCodeList = Arrays.asList(canMachineCodes.split(","));
            List<TqMachineInfo> filtered = candidateMachines.stream()
                    .filter(m -> canCodeList.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(filtered)) {
                log.debug("[定点机台过滤] 胎圈{}有限制作业机台, 候选机台从{}个过滤到{}个", beadCode, candidateMachines.size(), filtered.size());
                return filtered;
            }
        }

        // 2. 不可作业：排除指定机台
        String notMachineCodes = context.getSpecifyNotMachineMap().get(beadCode);
        if (notMachineCodes != null && !notMachineCodes.isEmpty()) {
            List<String> notCodeList = Arrays.asList(notMachineCodes.split(","));
            List<TqMachineInfo> filtered = candidateMachines.stream()
                    .filter(m -> !notCodeList.contains(m.getMachineCode()))
                    .collect(Collectors.toList());
            log.debug("[定点机台过滤] 胎圈{}有不可作业机台, 候选机台从{}个过滤到{}个", beadCode, candidateMachines.size(), filtered.size());
            return filtered;
        }

        return candidateMachines;
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public String getStrategyName() {
        return "定点机台过滤";
    }
}
