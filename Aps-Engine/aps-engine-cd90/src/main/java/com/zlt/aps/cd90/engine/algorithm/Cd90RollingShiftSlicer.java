package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java.util.stream.IntStream;
/** 从目标班开始截取定时滚动可调整班次。 */
@Component
public class Cd90RollingShiftSlicer {

    /** 目标班之前的班次不进入返回集合。 */
    public List<Cd90ShiftDescriptor> slice(List<Cd90ShiftDescriptor> shifts,
                                           String targetClassField) {
        List<Cd90ShiftDescriptor> source = shifts == null
                ? Collections.emptyList() : shifts;
        int targetIndex = IntStream.range(0, source.size())
                .filter(index -> Objects.equals(targetClassField, source.get(index).getClassField()))
                .findFirst().orElse(-1);
        if (targetIndex == -1) {
            throw new IllegalArgumentException("滚动目标班次不在当前排程窗口: " + targetClassField);
        }
        return new ArrayList<>(source.subList(targetIndex, source.size()));
    }
}
