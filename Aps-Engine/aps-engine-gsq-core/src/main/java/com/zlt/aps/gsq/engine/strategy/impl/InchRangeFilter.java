package com.zlt.aps.gsq.engine.strategy.impl;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 寸口范围过滤策略。
 *
 * <p>规则：机台支持的寸口值列表必须包含规格所需的寸口值。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class InchRangeFilter implements IMachineFilterStrategy {

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public String getName() {
        return "寸口范围过滤";
    }

    @Override
    public List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                        GsqScheduleResultVo scheduleVo,
                                        GsqScheduleContext context) {
        String steelRingCode = scheduleVo.getSteelRingCode();
        // 从施工信息中获取规格所需寸口值
        BigDecimal requiredInch = scheduleVo.getMachineInch();
        if (requiredInch == null) {
            log.warn("[{}] 规格[{}] 未配置寸口, 跳过寸口过滤", getName(), steelRingCode);
            return machines;
        }

        Map<String, List<BigDecimal>> machineChuckMap = context.getMachineChuckMap();

        return machines.stream()
                .filter(m -> {
                    List<BigDecimal> supportedInches = machineChuckMap.get(m.getMachineCode());
                    if (supportedInches == null || supportedInches.isEmpty()) {
                        return false;
                    }
                    return supportedInches.stream()
                            .anyMatch(inch -> inch.compareTo(requiredInch) == 0);
                })
                .collect(Collectors.toList());
    }
}
