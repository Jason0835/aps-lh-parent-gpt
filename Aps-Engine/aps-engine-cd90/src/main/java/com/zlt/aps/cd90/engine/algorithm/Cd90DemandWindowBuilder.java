package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.constant.Cd90StopMode;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 成型需求自然窗口构建器。
 */
@Component
public class Cd90DemandWindowBuilder {

    /**
     * 根据停产模式构建当前直裁班次对应的成型需求窗口。
     *
     * @param availableShifts 从供应起始班次开始的自然成型班次
     * @param demandWindow 配置需求窗口班数
     * @param stopMode 停产模式
     * @param stopBoundaryIndex 长停产边界索引，0表示第一个班次即进入停产
     * @return 本次需求窗口
     */
    public List<Cd90DemandShift> build(List<Cd90DemandShift> availableShifts,
                                      int demandWindow,
                                      String stopMode,
                                      Integer stopBoundaryIndex) {
        if (demandWindow <= 0) {
            throw new IllegalArgumentException("成型需求窗口班数必须大于0");
        }
        List<Cd90DemandShift> shifts = availableShifts == null
                ? Collections.emptyList() : availableShifts;
        int endExclusive = Math.min(demandWindow, shifts.size());

        if (Cd90StopMode.ONE_DAY_FORMING_STOP.equals(stopMode)) {
            endExclusive = Math.min(demandWindow + 1, shifts.size());
        } else if (Cd90StopMode.LONG_STOP.equals(stopMode)) {
            if (stopBoundaryIndex == null || stopBoundaryIndex < 0) {
                throw new IllegalArgumentException("长停产必须提供有效停产边界");
            }
            endExclusive = Math.min(endExclusive, stopBoundaryIndex);
        } else if (!Cd90StopMode.NORMAL.equals(stopMode)) {
            throw new IllegalArgumentException("未知停产处理模式");
        }
        return new ArrayList<>(shifts.subList(0, endExclusive));
    }
}
