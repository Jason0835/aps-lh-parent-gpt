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
 * 钢丝直径过滤策略（钢丝圈独有）。
 *
 * <p>规则：机台支持的钢丝直径列表必须包含规格所需的钢丝直径。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class WireDiameterFilter implements IMachineFilterStrategy {

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public String getName() {
        return "钢丝直径过滤";
    }

    @Override
    public List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                        GsqScheduleResultVo scheduleVo,
                                        GsqScheduleContext context) {
        String steelRingCode = scheduleVo.getSteelRingCode();
        Map<String, String> wireDiameterMap = context.getWireDiameterMap();
        String requiredDiameter = wireDiameterMap.get(steelRingCode);

        if (requiredDiameter == null || requiredDiameter.isEmpty()) {
            log.warn("[{}] 规格[{}] 未配置钢丝直径, 跳过钢丝直径过滤", getName(), steelRingCode);
            return machines;
        }

        Map<String, List<String>> machineWireDiameterMap = context.getMachineWireDiameterMap();

        return machines.stream()
                .filter(m -> {
                    List<String> supportedDiameters = machineWireDiameterMap.get(m.getMachineCode());
                    if (supportedDiameters == null || supportedDiameters.isEmpty()) {
                        return false;
                    }
                    return supportedDiameters.contains(requiredDiameter);
                })
                .collect(Collectors.toList());
    }
}
