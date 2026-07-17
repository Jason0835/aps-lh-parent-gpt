package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 按钢带和候选机台解析斜裁损耗率。
 */
@Component
public class Cd15LossRateResolver {

    /**
     * 优先匹配钢带加机台，未命中时匹配钢带通用配置。
     */
    public Optional<BigDecimal> resolve(String steelStripCode,
                                        String machineCode,
                                        List<Cd15LossSetting> settings) {
        List<Cd15LossSetting> values = settings == null ? Collections.emptyList() : settings;
        Cd15LossSetting matched = values.stream()
                .filter(item -> this.same(item == null ? null : item.getSteelStripCode(), steelStripCode)
                        && this.same(item.getMachineCode(), machineCode))
                .findFirst()
                .orElseGet(() -> values.stream()
                        .filter(item -> this.same(item == null ? null : item.getSteelStripCode(), steelStripCode)
                                && !StringUtils.hasText(item.getMachineCode()))
                        .findFirst()
                        .orElse(null));
        if (matched == null) {
            return Optional.empty();
        }
        BigDecimal lossRate = matched.getLossRate() == null
                ? null : BigDecimal.valueOf(matched.getLossRate());
        if (lossRate == null || lossRate.signum() < 0) {
            throw new IllegalArgumentException("斜裁损耗率不能为空或小于0, steelStripCode=" + steelStripCode
                    + ", machineCode=" + machineCode);
        }
        return Optional.of(lossRate);
    }

    private boolean same(String first, String second) {
        return StringUtils.hasText(first) && StringUtils.hasText(second)
                && Objects.equals(first.trim(), second.trim());
    }
}