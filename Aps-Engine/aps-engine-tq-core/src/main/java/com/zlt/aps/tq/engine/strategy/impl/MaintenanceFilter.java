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
 * 排除在排程日期<b>当前班次</b>处于检修状态的机台。</p>
 *
 * <p>注意：只排除当前正在排产的班次有检修计划的机台，
 * 其他班次的检修不影响当前班次的机台可选性。</p>
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

        // 获取当前正在排产的班次编码（由TqMachineAssignHandler.searchOptionalMachineList设置）
        String currentClassCode = context.getCurrentClassCode();
        if (currentClassCode == null || currentClassCode.isEmpty()) {
            log.warn("[维修计划过滤] 当前班次编码为空，跳过过滤");
            return candidateMachines;
        }

        // 按当前班次精确查询检修机台，不影响其他班次的机台可选性
        String key = scheduleDate + "|" + currentClassCode;
        List<String> maintenanceMachines = context.getMaintenanceMachineMap().get(key);

        if (maintenanceMachines == null || maintenanceMachines.isEmpty()) {
            return candidateMachines;
        }

        List<TqMachineInfo> filtered = candidateMachines.stream()
                .filter(m -> !maintenanceMachines.contains(m.getMachineCode()))
                .collect(Collectors.toList());

        if (filtered.size() < candidateMachines.size()) {
            log.debug("[维修计划过滤] 排程日期{}班次{}过滤掉{}台检修中的机台",
                    scheduleDate, currentClassCode, candidateMachines.size() - filtered.size());
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
