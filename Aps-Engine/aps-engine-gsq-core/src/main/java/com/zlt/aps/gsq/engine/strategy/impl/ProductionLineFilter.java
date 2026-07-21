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
 * 产线固定规则过滤策略（钢丝圈独有）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>22.5英寸规格固定在3号线生产</li>
 *   <li>19.5英寸规格固定在2号线生产</li>
 *   <li>17.5英寸及以下规格固定在1号线生产</li>
 *   <li>其他规格按机台默认产线分配</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class ProductionLineFilter implements IMachineFilterStrategy {

    /** 22.5英寸 */
    private static final BigDecimal INCH_22_5 = new BigDecimal("22.5");
    /** 19.5英寸 */
    private static final BigDecimal INCH_19_5 = new BigDecimal("19.5");
    /** 17.5英寸 */
    private static final BigDecimal INCH_17_5 = new BigDecimal("17.5");

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public String getName() {
        return "产线固定规则过滤";
    }

    @Override
    public List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                        GsqScheduleResultVo scheduleVo,
                                        GsqScheduleContext context) {
        String steelRingCode = scheduleVo.getSteelRingCode();
        BigDecimal requiredInch = scheduleVo.getMachineInch();
        if (requiredInch == null) {
            return machines;
        }

        // 根据英寸确定目标产线
        Integer targetLine = determineTargetLine(requiredInch);
        if (targetLine == null) {
            return machines;
        }

        Map<String, Integer> machineLineMap = context.getMachineProductionLineMap();

        List<GsqMachineInfo> filtered = machines.stream()
                .filter(m -> {
                    Integer machineLine = machineLineMap.get(m.getMachineCode());
                    return machineLine != null && machineLine.equals(targetLine);
                })
                .collect(Collectors.toList());

        log.debug("[{}] 规格[{}] 寸口[{}] 目标产线[{}] 过滤后剩余: {}台",
                getName(), steelRingCode, requiredInch, targetLine, filtered.size());

        return filtered;
    }

    /**
     * 根据英寸确定目标产线。
     *
     * @param inch 寸口值
     * @return 产线编号（1/2/3），null表示无固定规则
     */
    private Integer determineTargetLine(BigDecimal inch) {
        if (inch.compareTo(INCH_22_5) >= 0) {
            return 3; // 22.5及以上 → 3号线
        }
        if (inch.compareTo(INCH_19_5) >= 0) {
            return 2; // 19.5~22.5 → 2号线
        }
        if (inch.compareTo(INCH_17_5) <= 0) {
            return 1; // 17.5及以下 → 1号线
        }
        return null;
    }
}
