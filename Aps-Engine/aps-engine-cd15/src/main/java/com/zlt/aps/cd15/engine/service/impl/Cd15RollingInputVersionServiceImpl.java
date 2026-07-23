package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleParamsMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15RollingInputVersionService;
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
public class Cd15RollingInputVersionServiceImpl implements Cd15RollingInputVersionService {

    private static final int MAX_CLASS_COUNT = 8;

    private final Cd15AutoScheduleInputVersionService baseVersionService;
    private final Cd15EngineScheduleResultMapper scheduleResultMapper;
    private final Cd15AutoScheduleParamsMapper paramsMapper;

    /** 生成确定性SHA-256版本，任一关键输入变化都会产生新版本。 */
    @Override
    public String fingerprint(Cd15RollingTarget target) {
        if (target == null || target.getScheduleDate() == null) {
            throw new IllegalArgumentException("滚动目标和排程日期不能为空");
        }
        String base = baseVersionService.fingerprint(
                target.getFactoryCode(), target.getScheduleDate(),
                target.getHandoverTime().toLocalDate(),
                target.getTargetShiftCode());
        List<Cd15ScheduleResult> results = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .eq(Cd15ScheduleResult::getFactoryCode, target.getFactoryCode())
                        .eq(Cd15ScheduleResult::getScheduleDate,
                                Date.valueOf(target.getScheduleDate()))
                        .eq(Cd15ScheduleResult::getCd15BatchNo, target.getBatchNo())
                        .orderByAsc(Cd15ScheduleResult::getId));
        String currentSchedule = results.stream()
                .map(this::scheduleVersion)
                .collect(Collectors.joining("|"));
        String parameters = paramsMapper.selectList(
                        new LambdaQueryWrapper<Cd15Params>()
                                .eq(Cd15Params::getFactoryCode, target.getFactoryCode())
                                .orderByAsc(Cd15Params::getParamCode)
                                .orderByAsc(Cd15Params::getId))
                .stream().map(item -> item.getId() + ":" + item.getParamCode() + ":"
                        + item.getParamValue() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        return this.sha256(base + "#" + target.getTargetShiftCode() + "#"
                + target.getTargetClassField() + "#" + target.getBatchNo() + "#"
                + currentSchedule + "#" + parameters);
    }

    /** 把完成量、顺序、锁定和生产状态纳入当前批次版本。 */
    private String scheduleVersion(Cd15ScheduleResult result) {
        String classValues = IntStream.rangeClosed(1, MAX_CLASS_COUNT)
                .mapToObj(index -> result.getFieldValueByFieldName(
                                String.format("class%dPlanQty", index)) + ":"
                        + result.getFieldValueByFieldName(
                                String.format("class%dFinishQty", index)) + ":"
                        + result.getFieldValueByFieldName(
                                String.format("class%dProduceOrder", index)))
                .collect(Collectors.joining(","));
        return result.getId() + ":" + result.getCd15BatchNo() + ":"
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
