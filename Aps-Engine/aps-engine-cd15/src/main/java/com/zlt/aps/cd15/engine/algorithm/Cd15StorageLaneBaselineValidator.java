package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 校验自动排程任务启动时冻结的当前班次库排资源基线。
 */
@Component
public class Cd15StorageLaneBaselineValidator {

    /**
     * 查找同一资源基线中重复维护的库排号。
     *
     * @param lanes 库排资源基线
     * @return 重复库排号
     */
    public List<String> findDuplicateLaneCodes(List<Cd15StorageLaneLimit> lanes) {
        if (lanes == null || lanes.isEmpty()) {
            return Collections.emptyList();
        }
        return lanes.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getStorageLaneCode()))
                .map(item -> item.getStorageLaneCode().trim())
                .collect(Collectors.groupingBy(
                        Function.identity(), LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 校验资源基线中的库排号唯一且均已绑定机台，避免重复计算容量或跨机台分配。
     *
     * @param baselineDate 资源基线日期
     * @param baselineShiftCode 资源基线班次
     * @param lanes 库排资源基线
     */
    public void validateUnique(LocalDate baselineDate,
                               String baselineShiftCode,
                               List<Cd15StorageLaneLimit> lanes) {
        List<String> missingMachineLaneCodes = lanes == null ? Collections.emptyList()
                : lanes.stream()
                        .filter(item -> item != null
                                && StringUtils.hasText(item.getStorageLaneCode())
                                && !StringUtils.hasText(item.getMachineCode()))
                        .map(Cd15StorageLaneLimit::getStorageLaneCode)
                        .collect(Collectors.toList());
        if (!missingMachineLaneCodes.isEmpty()) {
            throw new IllegalStateException("库排资源基线 " + baselineDate + "/"
                    + baselineShiftCode + " 存在未绑定机台的库排: "
                    + String.join(",", missingMachineLaneCodes));
        }
        List<String> duplicateLaneCodes = this.findDuplicateLaneCodes(lanes);
        if (!duplicateLaneCodes.isEmpty()) {
            throw new IllegalStateException("库排资源基线 " + baselineDate + "/"
                    + baselineShiftCode + " 存在重复库排号: "
                    + String.join(",", duplicateLaneCodes));
        }
    }
}
