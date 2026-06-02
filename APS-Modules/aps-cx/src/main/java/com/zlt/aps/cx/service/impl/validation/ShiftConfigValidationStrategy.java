package com.zlt.aps.cx.service.impl.validation;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ShiftConfigValidationStrategy extends BaseValidationStrategy {

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.SHIFT_CONFIG;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        List<CxShiftConfig> shiftConfigs = context.getShiftConfigList();
        Integer scheduleDays = context.getScheduleDays();

        if (isEmpty(shiftConfigs)) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.empty"),
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.empty.suggestion"), factoryCode));
            return;
        }

        if (scheduleDays == null || scheduleDays < 1) {
            addError(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.scheduleDaysInvalid"), scheduleDays),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.scheduleDaysInvalid.suggestion"));
            return;
        }

        Map<Integer, Long> dayShiftCount = shiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay, Collectors.counting()));

        for (int day = 1; day <= scheduleDays; day++) {
            Long count = dayShiftCount.get(day);
            if (count == null || count == 0) {
                addWarn(result,
                        StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.dayMissing"), day),
                        StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.dayMissing.suggestion"), day));
            } else if (count < 2) {
                addInfo(result,
                        StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.dayShiftFew"), day, count),
                        I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.dayShiftFew.suggestion"));
            }
        }

        addInfo(result,
                StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.shiftConfig.summary"), shiftConfigs.size(), scheduleDays),
                null);
    }
}
