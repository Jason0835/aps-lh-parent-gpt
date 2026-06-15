package com.zlt.aps.tq.engine.strategy.impl;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        // 从context中获取检修计划数据
        // key格式：日期|班次编码（如"2025-01-01|3"）
        // 需要根据当前排程的日期和班次构建key
        // scheduleVo中的classIndex对应的班次编码需要转换
        // 这里简化处理：遍历context中的maintenanceMachineMap，检查机台是否在检修中

        if (context.getMaintenanceMachineMap() == null || context.getMaintenanceMachineMap().isEmpty()) {
            log.debug("[维修计划过滤] 无检修计划数据，跳过");
            return candidateMachines;
        }

        // 获取当前排程日期下所有检修中的机台ID
        List<Long> allMaintenanceMachineIds = context.getMaintenanceMachineMap().values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allMaintenanceMachineIds.isEmpty()) {
            return candidateMachines;
        }

        List<TqMachineInfo> filtered = candidateMachines.stream()
                .filter(m -> !allMaintenanceMachineIds.contains(m.getId()))
                .collect(Collectors.toList());

        if (filtered.size() < candidateMachines.size()) {
            log.debug("[维修计划过滤] 过滤掉{}台检修中的机台", candidateMachines.size() - filtered.size());
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
