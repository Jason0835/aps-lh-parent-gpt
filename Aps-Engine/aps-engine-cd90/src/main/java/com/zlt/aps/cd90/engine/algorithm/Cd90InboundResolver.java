package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90InboundRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 前序直裁实际入库与计划入库互斥解析器。
 */
@Component
public class Cd90InboundResolver {

    /**
     * 同一任务有MES实际入库时只保留实际记录，否则保留计划记录。
     *
     * @param records 实际和计划入库记录
     * @return 去重后的有效入库记录
     */
    public List<Cd90InboundRecord> resolve(List<Cd90InboundRecord> records) {
        if (records == null) {
            return Collections.emptyList();
        }
        records.forEach(item -> {
            if (item.getTaskKey() == null || item.getVehicleCount() < 0) {
                throw new IllegalArgumentException("入库任务标识不能为空且车辆数不能小于0");
            }
        });
        Map<String, List<Cd90InboundRecord>> grouped = records.stream()
                .collect(Collectors.groupingBy(Cd90InboundRecord::getTaskKey,
                        LinkedHashMap::new, Collectors.toList()));
        List<Cd90InboundRecord> result = new ArrayList<>();
        grouped.values().forEach(group -> {
            boolean hasActual = group.stream().anyMatch(Cd90InboundRecord::isActual);
            group.stream().filter(item -> item.isActual() == hasActual).forEach(result::add);
        });
        return result;
    }
}
