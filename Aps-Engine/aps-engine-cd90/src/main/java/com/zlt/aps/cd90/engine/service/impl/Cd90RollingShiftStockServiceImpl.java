package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftStock;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleSourceMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineShiftStockMapper;
import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.service.Cd90RollingShiftStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时滚动班次库存读取实现。
 */
@Service
@RequiredArgsConstructor
public class Cd90RollingShiftStockServiceImpl implements Cd90RollingShiftStockService {

    private final Cd90EngineShiftStockMapper shiftStockMapper;
    private final Cd90AutoScheduleSourceMapper sourceMapper;

    @Override
    public boolean exists(Cd90RollingTarget target) {
        return !this.select(target).isEmpty();
    }

    @Override
    public List<Cd90StockSource> loadRequired(Cd90RollingTarget target) {
        List<Cd90ShiftStock> rows = this.select(target);
        if (rows.isEmpty()) {
            throw new IllegalStateException("目标班次库存尚未就绪: "
                    + target.getHandoverTime() + "/" + target.getTargetShiftCode());
        }
        if (rows.stream().anyMatch(item -> item.getMaterialCode() == null
                || item.getMaterialCode().trim().isEmpty())) {
            throw new IllegalStateException("目标班次库存存在空帘布代码");
        }
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(Cd90ShiftStock::getMaterialCode,
                        Collectors.counting()));
        String duplicate = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (duplicate != null) {
            throw new IllegalStateException("目标班次库存存在重复帘布代码: " + duplicate);
        }
        return rows.stream().map(sourceMapper::mapShiftStock).collect(Collectors.toList());
    }

    @Override
    public String fingerprint(Cd90RollingTarget target) {
        String source = this.select(target).stream()
                .map(item -> item.getId() + ":" + item.getFactoryCode() + ":"
                        + item.getStockDate() + ":" + item.getShiftCode() + ":"
                        + item.getShiftStartTime() + ":" + item.getMaterialCode() + ":"
                        + item.getStockNum() + ":" + item.getModifyNum() + ":"
                        + item.getBadNum() + ":" + item.getSnapshotTime() + ":"
                        + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        return this.sha256(source);
    }

    private List<Cd90ShiftStock> select(Cd90RollingTarget target) {
        if (target == null || target.getHandoverTime() == null
                || target.getTargetShiftCode() == null) {
            throw new IllegalArgumentException("滚动目标交班时间和班次编码不能为空");
        }
        Date shiftStartTime = Date.from(target.getHandoverTime()
                .atZone(ZoneId.systemDefault()).toInstant());
        return shiftStockMapper.selectList(new LambdaQueryWrapper<Cd90ShiftStock>()
                .eq(Cd90ShiftStock::getFactoryCode, target.getFactoryCode())
                .eq(Cd90ShiftStock::getShiftCode, target.getTargetShiftCode())
                .eq(Cd90ShiftStock::getShiftStartTime, shiftStartTime)
                .orderByAsc(Cd90ShiftStock::getMaterialCode)
                .orderByAsc(Cd90ShiftStock::getId));
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
