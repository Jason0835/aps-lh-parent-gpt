package com.zlt.aps.cd15.engine.algorithm;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 斜裁自动排程启用班次安全校验器。 */
@Component
public class Cd15EnabledShiftConfigValidator {

    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS[1-8]");

    /**
     * 校验启用班次的结果字段和全局顺序连续性。
     *
     * @param enabledShifts 当前工厂全部启用班次
     */
    public void validate(List<Cd15ShiftConfig> enabledShifts) {
        if (enabledShifts == null || enabledShifts.isEmpty()) {
            throw this.failure("ui.cd15.autoSchedule.shiftConfigEmpty");
        }
        this.validateClassFields(enabledShifts);
        this.validateShiftOrders(enabledShifts);
        this.validateBusinessOrder(enabledShifts);
    }

    /** 校验CLASS字段格式、唯一性以及从CLASS1开始连续。 */
    private void validateClassFields(List<Cd15ShiftConfig> enabledShifts) {
        List<String> classFields = enabledShifts.stream()
                .map(Cd15ShiftConfig::getClassField)
                .map(this::normalizeClassField)
                .collect(Collectors.toList());
        classFields.stream()
                .filter(classField -> !CLASS_FIELD_PATTERN.matcher(classField).matches())
                .findFirst()
                .ifPresent(classField -> {
                    throw this.failure(
                            "ui.cd15.autoSchedule.shiftClassFieldInvalid", classField);
                });
        Set<String> uniqueClassFields = new HashSet<>();
        classFields.stream()
                .filter(classField -> !uniqueClassFields.add(classField))
                .findFirst()
                .ifPresent(classField -> {
                    throw this.failure(
                            "ui.cd15.autoSchedule.shiftClassFieldDuplicate", classField);
                });
        for (int classIndex = 1; classIndex <= enabledShifts.size(); classIndex++) {
            String expectedClassField = "CLASS" + classIndex;
            if (!uniqueClassFields.contains(expectedClassField)) {
                throw this.failure(
                        "ui.cd15.autoSchedule.shiftClassFieldMissing", expectedClassField);
            }
        }
    }

    /** 校验SHIFT_ORDER非空、唯一并从1开始连续。 */
    private void validateShiftOrders(List<Cd15ShiftConfig> enabledShifts) {
        enabledShifts.stream()
                .filter(config -> config.getShiftOrder() == null)
                .findFirst()
                .ifPresent(config -> {
                    throw this.failure("ui.cd15.autoSchedule.shiftOrderEmpty",
                            this.normalizeClassField(config.getClassField()));
                });
        Set<Integer> uniqueShiftOrders = new HashSet<>();
        enabledShifts.stream()
                .map(Cd15ShiftConfig::getShiftOrder)
                .filter(shiftOrder -> !uniqueShiftOrders.add(shiftOrder))
                .findFirst()
                .ifPresent(shiftOrder -> {
                    throw this.failure(
                            "ui.cd15.autoSchedule.shiftOrderDuplicate", shiftOrder);
                });
        for (int expectedOrder = 1;
             expectedOrder <= enabledShifts.size(); expectedOrder++) {
            if (!uniqueShiftOrders.contains(expectedOrder)) {
                throw this.failure(
                        "ui.cd15.autoSchedule.shiftOrderMissing", expectedOrder);
            }
        }
    }

    /** 校验每个全局班次顺序必须映射同序号的CLASS字段。 */
    private void validateBusinessOrder(List<Cd15ShiftConfig> enabledShifts) {
        List<Cd15ShiftConfig> orderedShifts = enabledShifts.stream()
                .sorted(Comparator.comparingInt(Cd15ShiftConfig::getShiftOrder))
                .collect(Collectors.toList());
        for (int shiftIndex = 0; shiftIndex < orderedShifts.size(); shiftIndex++) {
            int expectedOrder = shiftIndex + 1;
            String expectedClassField = "CLASS" + expectedOrder;
            String actualClassField = this.normalizeClassField(
                    orderedShifts.get(shiftIndex).getClassField());
            if (!expectedClassField.equals(actualClassField)) {
                throw this.failure(
                        "ui.cd15.autoSchedule.shiftBusinessOrderInvalid",
                        expectedOrder, expectedClassField, actualClassField);
            }
        }
    }

    private String normalizeClassField(String classField) {
        return StringUtils.hasText(classField) ? classField.trim() : "";
    }

    private ServiceException failure(String messageKey, Object... arguments) {
        return new ServiceException(MessageFormat.format(
                I18nUtil.getMessage(messageKey), arguments));
    }
}
