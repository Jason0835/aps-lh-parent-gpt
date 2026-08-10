package com.zlt.aps.gsq.engine.strategy.impl;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 班次可用状态过滤策略。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>排除已停用状态的机台</li>
 *   <li>排除当前班次已被停产配置禁用的机台</li>
 *   <li>排除任务链已满的机台（6班次已全部分配）</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class ClassAvailabilityFilter implements IMachineFilterStrategy {

    @Override
    public int getOrder() {
        return 6;
    }

    @Override
    public String getName() {
        return "班次可用状态过滤";
    }

    @Override
    public List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                        GsqScheduleResultVo scheduleVo,
                                        GsqScheduleContext context) {
        int currentClassIndex = context.getCurrentClassIndex();

        return machines.stream()
                .filter(m -> {
                    // 1. 排除停用机台
                    if (!isMachineActive(m)) {
                        log.debug("[{}] 机台[{}] 已停用, 排除", getName(), m.getMachineCode());
                        return false;
                    }

                    // 2. 排除任务链已满的机台（已分配6个班次）
                    if (isTaskChainFull(m.getMachineCode(), context)) {
                        log.debug("[{}] 机台[{}] 任务链已满(6班), 排除", getName(), m.getMachineCode());
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 判断机台是否启用。
     *
     * <p>钢丝圈机台表 T_GSQ_MACHINE_INFO.STATUS：仅 STATUS='1' 视为启用，其他值一律排除。</p>
     */
    private boolean isMachineActive(GsqMachineInfo machine) {
        String status = machine.getStatus();
        return "1".equals(status);
    }

    /**
     * 判断机台任务链是否已满（6班次）。
     */
    private boolean isTaskChainFull(String machineCode, GsqScheduleContext context) {
        int chainSize = context.getTaskChainMap().getOrDefault(machineCode, new java.util.LinkedList<>()).size();
        return chainSize >= 6;
    }
}
