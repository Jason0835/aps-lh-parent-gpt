package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 维修计划过滤策略。
 *
 * <p>过滤规则：根据T_TQ_MACHINE_MAINTENANCE_PLAN表，
 * 排除在排程日期对应班次处于检修状态的机台。</p>
 */
@Slf4j
@Component
public class MaintenanceFilter implements IMachineFilterStrategy {

    @Override
    public List<TqMachineInfo> filter(List<TqMachineInfo> candidateMachines, TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        if (context.getMaintenanceMachineMap() == null || context.getMaintenanceMachineMap().isEmpty()) {
            log.debug("[维修计划过滤] 无检修计划数据，跳过");
            return candidateMachines;
        }

        String scheduleDate = context.getScheduleDate();
        if (scheduleDate == null || scheduleDate.isEmpty()) {
            log.warn("[维修计划过滤] 排程日期为空，跳过过滤");
            return candidateMachines;
        }

        List<String> todayMaintenanceMachines = new ArrayList<>();
        String[] shifts = {"01", "02", "03"};
        for (String shift : shifts) {
            String key = scheduleDate + "|" + shift;
            List<String> machines = context.getMaintenanceMachineMap().get(key);
            if (machines != null) {
                todayMaintenanceMachines.addAll(machines);
            }
        }

        if (todayMaintenanceMachines.isEmpty()) {
            return candidateMachines;
        }

        List<TqMachineInfo> filtered = candidateMachines.stream()
                .filter(m -> !todayMaintenanceMachines.contains(m.getMachineCode()))
                .collect(Collectors.toList());

        if (filtered.size() < candidateMachines.size()) {
            log.debug("[维修计划过滤] 排程日期{}过滤掉{}台检修中的机台", scheduleDate, candidateMachines.size() - filtered.size());
        }

        return filtered;
    }

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public String getStrategyName() {
        return "维修计划过滤";
    }
}
