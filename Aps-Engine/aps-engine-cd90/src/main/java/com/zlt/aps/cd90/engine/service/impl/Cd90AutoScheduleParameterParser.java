package com.zlt.aps.cd90.engine.service.impl;

import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.engine.constant.Cd90AutoScheduleParamCode;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 直裁自动排程参数解析器。
 */
@Component
public class Cd90AutoScheduleParameterParser {

    /**
     * 将数据库参数按PARAM_CODE解析成强类型快照。
     *
     * @param factoryCode 工厂编码
     * @param params 参数记录
     * @param enabledShiftCount 当前启用的直裁班次数
     * @return 参数快照
     */
    public Cd90AutoScheduleParameters parse(String factoryCode, List<Cd90Params> params, int enabledShiftCount) {
        if (!StringUtils.hasText(factoryCode)) {
            throw new IllegalArgumentException("自动排程工厂编码不能为空");
        }
        if (enabledShiftCount <= 0) {
            throw new IllegalArgumentException("未配置启用的直裁班次");
        }

        Map<String, String> values = params.stream()
                .filter(item -> StringUtils.hasText(item.getParamCode()))
                .collect(Collectors.toMap(
                        Cd90Params::getParamCode,
                        item -> item.getParamValue() == null ? "" : item.getParamValue().trim(),
                        (first, second) -> second,
                        LinkedHashMap::new));

        String demandCalcMode = required(values, Cd90AutoScheduleParamCode.DEMAND_CALC_MODE);
        if (!"AVERAGE".equals(demandCalcMode) && !"SUM".equals(demandCalcMode)) {
            throw invalid(Cd90AutoScheduleParamCode.DEMAND_CALC_MODE, "只能取AVERAGE或SUM");
        }

        int scheduleWindow = positiveInt(values, Cd90AutoScheduleParamCode.SCHEDULE_WINDOW);
        if (scheduleWindow > enabledShiftCount) {
            throw invalid(Cd90AutoScheduleParamCode.SCHEDULE_WINDOW,
                    "不能超过当前启用直裁班次数" + enabledShiftCount);
        }

        Map<String, String> sourceValues = Cd90AutoScheduleParamCode.ALL_CODES.stream()
                .collect(Collectors.toMap(code -> code, code -> values.getOrDefault(code, ""),
                        (first, second) -> second, LinkedHashMap::new));

        return Cd90AutoScheduleParameters.builder()
                .factoryCode(factoryCode)
                .demandCalcMode(demandCalcMode)
                .scheduleWindow(scheduleWindow)
                .maxRollChangePerShift(nonNegativeInt(values, Cd90AutoScheduleParamCode.MAX_ROLL_CHANGE_PER_SHIFT))
                .minStartQty(positiveDecimal(values, Cd90AutoScheduleParamCode.MIN_START_QTY))
                .machinePriority(parseMachinePriority(values.get(Cd90AutoScheduleParamCode.MACHINE_PRIORITY)))
                .maxTime4Shift(positiveInt(values, Cd90AutoScheduleParamCode.MAX_TIME_4SHIFT))
                .stopLookaheadDays(positiveInt(values, Cd90AutoScheduleParamCode.STOP_LOOKAHEAD_DAYS))
                .restartStockThreshold(nonNegativeDecimal(values, Cd90AutoScheduleParamCode.RESTART_STOCK_THRESHOLD))
                .rollTotalCount(positiveInt(values, Cd90AutoScheduleParamCode.ROLL_TOTAL_COUNT))
                .equalShareThreshold(positiveDecimal(values, Cd90AutoScheduleParamCode.EQUAL_SHARE_THRESHOLD))
                .rollCoilMeter(positiveDecimal(values, Cd90AutoScheduleParamCode.CRIMP_LENGTH))
                .specChangeMinutes(nonNegativeInt(values,
                        Cd90AutoScheduleParamCode.SAME_ROLL_DIFF_SPEC_CHANGE_MINUTES))
                .sameRollDiffSpecChangeMinutes(nonNegativeInt(values,
                        Cd90AutoScheduleParamCode.SAME_ROLL_DIFF_SPEC_CHANGE_MINUTES))
                .diffRollSameSpecChangeMinutes(nonNegativeInt(values,
                        Cd90AutoScheduleParamCode.DIFF_ROLL_SAME_SPEC_CHANGE_MINUTES))
                .diffRollDiffSpecChangeMinutes(nonNegativeInt(values,
                        Cd90AutoScheduleParamCode.DIFF_ROLL_DIFF_SPEC_CHANGE_MINUTES))
                .specialRollUseUpCodes(parseCodeList(values.get(
                        Cd90AutoScheduleParamCode.SPECIAL_ROLL_USE_UP_CODES)))
                .specialRollLookaheadShifts(nonNegativeInt(values,
                        Cd90AutoScheduleParamCode.SPECIAL_ROLL_LOOKAHEAD_SHIFTS))
                .specialRollExtraStockLimit(nonNegativeDecimal(values,
                        Cd90AutoScheduleParamCode.SPECIAL_ROLL_EXTRA_STOCK_LIMIT))
                .partialMinVehicleCount(positiveInt(values,
                        Cd90AutoScheduleParamCode.PARTIAL_MIN_VEHICLE_COUNT))
                .agingPeriodHours(positiveInt(values,
                        Cd90AutoScheduleParamCode.AGING_PERIOD_LIMIT))
                .newSpecLookbackDays(this.nonNegativeInt(values,
                        Cd90AutoScheduleParamCode.NEW_SPEC_LOOKBACK_DAYS))
                .newSpecAdvanceDays(this.positiveInt(values,
                        Cd90AutoScheduleParamCode.NEW_SPEC_ADVANCE_DAYS))
                .fallbackLossRatePercent(nonNegativeDecimal(values, Cd90AutoScheduleParamCode.LOSS_RATE))
                .taskTimeoutMinutes(positiveInt(values, Cd90AutoScheduleParamCode.TASK_TIMEOUT_MINUTES))
                .autoScheduleCron(values.getOrDefault(Cd90AutoScheduleParamCode.AUTO_SCHEDULE_CRON, ""))
                .sourceValues(Collections.unmodifiableMap(sourceValues))
                .fingerprint(buildFingerprint(sourceValues))
                .build();
    }

    private List<String> parseMachinePriority(String value) {
        return parseCodeList(value);
    }

    private List<String> parseCodeList(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private int positiveInt(Map<String, String> values, String code) {
        int value = parseInt(values, code);
        if (value <= 0) {
            throw invalid(code, "必须为正整数");
        }
        return value;
    }

    private int nonNegativeInt(Map<String, String> values, String code) {
        int value = parseInt(values, code);
        if (value < 0) {
            throw invalid(code, "必须为非负整数");
        }
        return value;
    }

    private int parseInt(Map<String, String> values, String code) {
        try {
            return Integer.parseInt(required(values, code));
        } catch (NumberFormatException exception) {
            throw invalid(code, "必须为整数");
        }
    }

    private BigDecimal positiveDecimal(Map<String, String> values, String code) {
        BigDecimal value = parseDecimal(values, code);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalid(code, "必须大于0");
        }
        return value;
    }

    private BigDecimal nonNegativeDecimal(Map<String, String> values, String code) {
        BigDecimal value = parseDecimal(values, code);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid(code, "不能小于0");
        }
        return value;
    }

    private BigDecimal parseDecimal(Map<String, String> values, String code) {
        try {
            return new BigDecimal(required(values, code));
        } catch (NumberFormatException exception) {
            throw invalid(code, "必须为数值");
        }
    }

    private String required(Map<String, String> values, String code) {
        String value = values.get(code);
        if (!StringUtils.hasText(value)) {
            throw invalid(code, "参数缺失或参数值为空");
        }
        return value;
    }

    private String buildFingerprint(Map<String, String> values) {
        String source = values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }

    private IllegalArgumentException invalid(String code, String reason) {
        return new IllegalArgumentException("自动排程参数" + code + reason);
    }
}
