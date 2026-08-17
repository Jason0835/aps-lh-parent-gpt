package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleSourceMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineShiftStockMapper;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import com.zlt.aps.cd15.engine.service.Cd15RollingShiftStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 定时滚动班次库存读取实现。
 */
@Service
@RequiredArgsConstructor
public class Cd15RollingShiftStockServiceImpl implements Cd15RollingShiftStockService {

    private final Cd15EngineShiftStockMapper shiftStockMapper;
    private final Cd15AutoScheduleSourceMapper sourceMapper;

    @Override
    public boolean exists(Cd15RollingTarget target) {
        return !this.select(target).isEmpty();
    }

    @Override
    public List<Cd15StockSource> loadRequired(Cd15RollingTarget target) {
        List<Cd15ShiftStock> rows = this.select(target);
        if (rows.isEmpty()) {
            throw new IllegalStateException("目标班次库存尚未就绪: "
                    + target.getHandoverTime() + "/" + target.getTargetShiftCode());
        }
        if (rows.stream().anyMatch(item -> item.getMaterialCode() == null
                || item.getMaterialCode().trim().isEmpty())) {
            throw new IllegalStateException("目标班次库存存在空钢带代码");
        }
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(Cd15ShiftStock::getMaterialCode,
                        Collectors.counting()));
        String duplicate = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (duplicate != null) {
            throw new IllegalStateException("目标班次库存存在重复钢带代码: " + duplicate);
        }
        return rows.stream().map(sourceMapper::mapShiftStock).collect(Collectors.toList());
    }

    @Override
    public String fingerprint(Cd15RollingTarget target) {
        String source = this.select(target).stream()
                .map(item -> item.getFactoryCode() + ":" + item.getStockDate() + ":" + item.getShiftCode() + ":"
                        + item.getShiftStartTime() + ":" + item.getMaterialCode() + ":"
                        + item.getStockNum() + ":" + item.getModifyNum() + ":"
                        + item.getBadNum())
                .collect(Collectors.joining("|"));
        return this.sha256(source);
    }

    private List<Cd15ShiftStock> select(Cd15RollingTarget target) {
        if (target == null || target.getHandoverTime() == null
                || target.getTargetShiftCode() == null) {
            throw new IllegalArgumentException("滚动目标交班时间和班次编码不能为空");
        }
        Date shiftStartTime = Date.from(target.getHandoverTime()
                .atZone(ZoneId.systemDefault()).toInstant());
        return shiftStockMapper.selectList(new LambdaQueryWrapper<Cd15ShiftStock>()
                .eq(Cd15ShiftStock::getFactoryCode, target.getFactoryCode())
                .eq(Cd15ShiftStock::getShiftCode, target.getTargetShiftCode())
                .eq(Cd15ShiftStock::getShiftStartTime, shiftStartTime)
                .orderByAsc(Cd15ShiftStock::getMaterialCode)
                .orderByAsc(Cd15ShiftStock::getId));
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JVM不支持SHA-256", exception);
        }
    }
}
