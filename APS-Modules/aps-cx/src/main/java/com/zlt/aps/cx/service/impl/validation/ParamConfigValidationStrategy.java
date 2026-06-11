package com.zlt.aps.cx.service.impl.validation;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class ParamConfigValidationStrategy extends BaseValidationStrategy {

    private static final Set<String> REQUIRED_PARAM_CODES = new HashSet<>(Arrays.asList(
            "LOSS_RATE"
    ));

    private static final Map<String, String> PARAM_DEFAULTS = new HashMap<>();

    private static final Map<String, String> PARAM_DESCRIPTIONS = new HashMap<>();

    static {
        PARAM_DEFAULTS.put("LOSS_RATE", "0.02");
        PARAM_DESCRIPTIONS.put("LOSS_RATE", "损耗率，用于计算实际产能");
    }

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.PARAM_CONFIG;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        Map<String, CxParamConfig> paramConfigMap = context.getParamConfigMap();

        if (paramConfigMap == null || paramConfigMap.isEmpty()) {
            addWarn(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.empty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.empty.suggestion"));
            return;
        }

        for (String paramCode : REQUIRED_PARAM_CODES) {
            CxParamConfig config = paramConfigMap.get(paramCode);
            String description = PARAM_DESCRIPTIONS.get(paramCode);
            String defaultValue = PARAM_DEFAULTS.get(paramCode);

            if (config == null) {
                addWarn(result,
                        String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.paramMissing"), paramCode),
                        String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.paramMissing.suggestion"), defaultValue, description));
            } else if (config.getParamValue() == null || config.getParamValue().trim().isEmpty()) {
                addWarn(result,
                        String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.paramValueEmpty"), paramCode),
                        String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.paramValueEmpty.suggestion"), defaultValue));
            } else {
                try {
                    new BigDecimal(config.getParamValue());
                    addInfo(result,
                            String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.paramConfigured"), paramCode, config.getParamValue()),
                            null);
                } catch (NumberFormatException e) {
                    addWarn(result,
                            String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.paramValueInvalid"), paramCode, config.getParamValue()),
                            String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.paramValueInvalid.suggestion"), defaultValue));
                }
            }
        }

        addInfo(result,
                String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.paramConfig.summary"), paramConfigMap.size()),
                null);
    }
}
