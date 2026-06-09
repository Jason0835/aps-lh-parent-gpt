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
 * <p>过滤规则：排除在排程日期当天处于维修状态的机台。</p>
 *
 * <p>注意：当前暂无维修计划数据源，此策略为预留扩展点，
 * 待维修计划接口补充后生效。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class MaintenanceFilter implements IMachineFilterStrategy {

    @Override
    public List<TqMachineInfo> filter(List<TqMachineInfo> candidateMachines, TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        // TODO: 待维修计划数据源补充后实现过滤逻辑
        // 预留逻辑：
        // List<Long> maintenanceMachineIds = maintenanceService.getMaintenanceMachineIds(context.getScheduleDate());
        // return candidateMachines.stream()
        //     .filter(m -> !maintenanceMachineIds.contains(m.getId()))
        //     .collect(Collectors.toList());

        log.debug("[维修计划过滤] 暂无维修计划数据，跳过");
        return candidateMachines;
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
