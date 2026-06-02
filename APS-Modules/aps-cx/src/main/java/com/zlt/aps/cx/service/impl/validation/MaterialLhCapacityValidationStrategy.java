package com.zlt.aps.cx.service.impl.validation;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MaterialLhCapacityValidationStrategy extends BaseValidationStrategy {

    private static final String PARAM_DAY_VULCANIZATION_MODE = "DAY_VULCANIZATION_MODE";

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.MATERIAL_LH_CAPACITY;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {

        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        if (isEmpty(lhResults)) {
            addInfo(result, I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.noTask"), null);
            return;
        }

        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap == null || lhCapacityMap.isEmpty()) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.mapEmpty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.mapEmpty.suggestion"));
            return;
        }

        DayVulcanizationModeEnum mode = getMode(context);
        log.info("日硫化产能校验：当前模式={}，校验字段={}", mode.getDesc(), mode.getFieldName());

        Set<String> requiredMaterials = lhResults.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requiredMaterials.isEmpty()) {
            addWarn(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.missingMaterialCode"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.missingMaterialCode.suggestion"));
            return;
        }

        Function<MonthPlanProductLhCapacityVo, Integer> fieldExtractor = getFieldExtractor(mode);

        List<String> missingFromMap = new ArrayList<>();
        List<String> fieldNullOrZero = new ArrayList<>();
        int validCount = 0;

        for (String materialCode : requiredMaterials) {
            MonthPlanProductLhCapacityVo vo = lhCapacityMap.get(materialCode);
            if (vo == null) {
                missingFromMap.add(materialCode);
                continue;
            }
            Integer fieldValue = fieldExtractor.apply(vo);
            if (fieldValue == null || fieldValue <= 0) {
                fieldNullOrZero.add(materialCode);
            } else {
                validCount++;
            }
        }

        if (!missingFromMap.isEmpty()) {
            addError(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.materialNotFound"), missingFromMap.size()),
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.materialNotFound.suggestion"), String.join(", ", truncateList(missingFromMap))));
        }

        if (!fieldNullOrZero.isEmpty()) {
            String list = String.join(", ", truncateList(fieldNullOrZero));
            addError(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.fieldNullOrZero"), fieldNullOrZero.size(), mode.getFieldName(), mode.getCode()),
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.fieldNullOrZero.suggestion"), list, mode.getDesc(), mode.getFieldName()));
        }

        addInfo(result,
                StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhCapacity.summary"),
                        requiredMaterials.size(), mode.getDesc(), mode.getCode(), validCount, missingFromMap.size() + fieldNullOrZero.size()),
                null);
    }

    private DayVulcanizationModeEnum getMode(ScheduleContextVo context) {
        Map<String, CxParamConfig> paramConfigMap = context.getParamConfigMap();
        if (paramConfigMap != null) {
            CxParamConfig modeConfig = paramConfigMap.get(PARAM_DAY_VULCANIZATION_MODE);
            if (modeConfig != null && modeConfig.getParamValue() != null) {
                return DayVulcanizationModeEnum.getByCode(modeConfig.getParamValue());
            }
        }
        return DayVulcanizationModeEnum.STANDARD_CAPACITY;
    }

    private Function<MonthPlanProductLhCapacityVo, Integer> getFieldExtractor(DayVulcanizationModeEnum mode) {
        switch (mode) {
            case MES_CAPACITY:
                return MonthPlanProductLhCapacityVo::getMesCapacity;
            case STANDARD_CAPACITY:
                return MonthPlanProductLhCapacityVo::getStandardCapacity;
            case APS_CAPACITY:
                return MonthPlanProductLhCapacityVo::getApsCapacity;
            default:
                return MonthPlanProductLhCapacityVo::getStandardCapacity;
        }
    }

    private List<String> truncateList(List<String> list) {
        return list.size() > 10 ? list.subList(0, 10) : list;
    }
}
