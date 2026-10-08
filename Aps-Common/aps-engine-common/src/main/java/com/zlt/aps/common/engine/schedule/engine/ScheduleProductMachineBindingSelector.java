package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 前一排程日产品机台初始绑定选择器。
 *
 * <p>同一产品在多个机台存在计划时，按机台汇总计划量取最大值；数量相同时按机台编码升序，
 * 保证 TM 和 TC 两个领域使用完全一致的确定性规则。</p>
 */
public final class ScheduleProductMachineBindingSelector {

    private ScheduleProductMachineBindingSelector() {
    }

    /**
     * 为每个产品选择前一排程日早班计划量最大的机台。
     *
     * @param productMachineQuantityMap 产品编码到机台计划量的映射
     * @return 产品编码到初始绑定机台的映射
     */
    public static Map<String, String> selectLargestMachine(
            Map<String, Map<String, BigDecimal>> productMachineQuantityMap) {
        if (productMachineQuantityMap == null || productMachineQuantityMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> bindingMap = new LinkedHashMap<>();
        productMachineQuantityMap.forEach((productCode, machineQuantityMap) -> {
            if (productCode == null || productCode.trim().isEmpty()
                    || machineQuantityMap == null || machineQuantityMap.isEmpty()) {
                return;
            }
            machineQuantityMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && !entry.getKey().trim().isEmpty())
                    .filter(entry -> entry.getValue() != null
                            && entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .ifPresent(machineCode -> bindingMap.put(productCode.trim(), machineCode.trim()));
        });
        return bindingMap;
    }
}
