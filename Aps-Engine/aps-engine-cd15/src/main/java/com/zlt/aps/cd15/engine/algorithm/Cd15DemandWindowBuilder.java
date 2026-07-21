package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.constant.Cd15StopMode;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 成型需求自然窗口构建器。
 */
@Component
@RequiredArgsConstructor
public class Cd15DemandWindowBuilder {

    private final Cd15FractionalDemandWindowSelector demandWindowSelector;

    /**
     * 根据停产模式和逐钢带备库深度构建当前斜裁班次对应的成型需求窗口。
     *
     * @param availableShifts 从供应起始班次开始的自然成型班次
     * @param depthClassQty 当前钢带匹配到的备库班数
     * @param stopMode 停产模式
     * @param stopBoundaryIndex 长停产边界索引，0表示第一个班次即进入停产
     * @return 本次需求窗口
     */
    public List<Cd15DemandShift> build(List<Cd15DemandShift> availableShifts,
                                      BigDecimal depthClassQty,
                                      String stopMode,
                                      Integer stopBoundaryIndex) {
        List<Cd15DemandShift> shifts = availableShifts == null
                ? Collections.emptyList() : availableShifts;
        List<Cd15DemandShift> baseWindow = demandWindowSelector.select(shifts, depthClassQty);

        if (Cd15StopMode.ONE_DAY_FORMING_STOP.equals(stopMode)) {
            List<Cd15DemandShift> result = new ArrayList<>(baseWindow);
            int nextShiftIndex = baseWindow.size();
            if (nextShiftIndex < shifts.size()) {
                result.addAll(demandWindowSelector.select(
                        shifts.subList(nextShiftIndex, nextShiftIndex + 1), BigDecimal.ONE));
            }
            return result;
        }
        if (Cd15StopMode.LONG_STOP.equals(stopMode)) {
            if (stopBoundaryIndex == null || stopBoundaryIndex < 0) {
                throw new IllegalArgumentException("长停产必须提供有效停产边界");
            }
            return new ArrayList<>(baseWindow.subList(
                    0, Math.min(baseWindow.size(), stopBoundaryIndex)));
        }
        if (!Cd15StopMode.NORMAL.equals(stopMode)) {
            throw new IllegalArgumentException("未知停产处理模式");
        }
        return baseWindow;
    }
}
