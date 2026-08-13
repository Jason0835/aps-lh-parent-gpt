package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleParamsMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleResultMapper;
import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputVersionService;
import com.zlt.aps.cd90.engine.service.Cd90RollingInputVersionService;
import com.zlt.aps.cd90.engine.service.Cd90RollingShiftStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 复用自动排程输入指纹并叠加当前批次执行状态。 */
@Service
@RequiredArgsConstructor
public class Cd90RollingInputVersionServiceImpl implements Cd90RollingInputVersionService {

    private static final int MAX_CLASS_COUNT = 6;

    private final Cd90AutoScheduleInputVersionService baseVersionService;
    private final Cd90EngineScheduleResultMapper scheduleResultMapper;
    private final Cd90AutoScheduleParamsMapper paramsMapper;
    private final Cd90RollingShiftStockService rollingShiftStockService;

    /** 生成确定性SHA-256版本，任一关键输入变化都会产生新版本。 */
    @Override
    public String fingerprint(Cd90RollingTarget target) {
        if (target == null || target.getScheduleDate() == null
                || target.getResourceBaselineDate() == null) {
            throw new IllegalArgumentException("滚动目标、排程日期和资源基线日期不能为空");
        }
        String base = baseVersionService.fingerprintWithoutStock(
                target.getFactoryCode(), target.getScheduleDate(),
                target.getResourceBaselineDate(),
                target.getTargetShiftCode());
        String shiftStock = rollingShiftStockService.fingerprint(target);
        List<Cd90ScheduleResult> results = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .eq(Cd90ScheduleResult::getFactoryCode, target.getFactoryCode())
                        .eq(Cd90ScheduleResult::getScheduleDate,
                                Date.valueOf(target.getScheduleDate()))
                        .eq(Cd90ScheduleResult::getBatchNo, target.getBatchNo())
                        .orderByAsc(Cd90ScheduleResult::getId));
        String currentSchedule = results.stream()
                .map(this::scheduleVersion)
                .collect(Collectors.joining("|"));
        String parameters = paramsMapper.selectList(
                        new LambdaQueryWrapper<Cd90Params>()
                                .eq(Cd90Params::getFactoryCode, target.getFactoryCode())
                                .orderByAsc(Cd90Params::getParamCode)
                                .orderByAsc(Cd90Params::getId))
                .stream().map(item -> item.getId() + ":" + item.getParamCode() + ":"
                        + item.getParamValue() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        return this.sha256(base + "#" + shiftStock + "#"
                + target.getTargetShiftCode() + "#"
                + target.getTargetClassField() + "#" + target.getBatchNo() + "#"
                + currentSchedule + "#" + parameters);
    }

    /** 把完成量、顺序、锁定和生产状态纳入当前批次版本。 */
    private String scheduleVersion(Cd90ScheduleResult result) {
        String classValues = IntStream.rangeClosed(1, MAX_CLASS_COUNT)
                .mapToObj(index -> result.getFieldValueByFieldName(
                                String.format("class%dPlanQty", index)) + ":"
                        + result.getFieldValueByFieldName(
                                String.format("class%dFinishQty", index)) + ":"
                        + result.getFieldValueByFieldName(
                                String.format("class%dProduceOrder", index)))
                .collect(Collectors.joining(","));
        return result.getId() + ":" + result.getBatchNo() + ":"
                + result.getMachineCode() + ":" + result.getProductionStatus() + ":"
                + result.getIsLocked() + ":" + result.getUpdateTime() + ":" + classValues;
    }

    /** 使用JVM标准SHA-256实现，避免引入额外摘要依赖。 */
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
