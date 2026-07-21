package com.zlt.aps.gsq.engine.strategy.impl;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检修计划过滤策略。
 *
 * <p>规则：排除当前班次处于检修状态的机台。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class MaintenanceFilter implements IMachineFilterStrategy {

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    public String getName() {
        return "检修计划过滤";
    }

    @Override
    public List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                        GsqScheduleResultVo scheduleVo,
                                        GsqScheduleContext context) {
        // 构建当前班次的检修机台列表Key：日期|班次编码
        String currentClassCode = context.getCurrentClassCode();
        String maintenanceKey = context.getScheduleDate() + "|" + currentClassCode;

        Map<String, List<String>> maintenanceMap = context.getMaintenanceMachineMap();
        List<String> maintenanceMachines = maintenanceMap.get(maintenanceKey);

        if (maintenanceMachines == null || maintenanceMachines.isEmpty()) {
            log.debug("[{}] 当前班次[{}]无检修机台", getName(), maintenanceKey);
            return machines;
        }

        List<GsqMachineInfo> filtered = machines.stream()
                .filter(m -> !maintenanceMachines.contains(m.getMachineCode()))
                .collect(Collectors.toList());

        log.debug("[{}] 班次[{}] 检修机台数[{}] 过滤后剩余: {}台",
                getName(), maintenanceKey, maintenanceMachines.size(), filtered.size());

        return filtered;
    }
}
