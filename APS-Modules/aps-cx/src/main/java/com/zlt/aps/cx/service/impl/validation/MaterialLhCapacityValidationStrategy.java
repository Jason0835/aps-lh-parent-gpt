package com.zlt.aps.cx.service.impl.validation;

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

/**
 * 物料日硫化产能校验策略
 *
 * <p>根据参数配置的日硫化量计算模式，校验对应字段是否有值：
 * <ul>
 *   <li>模式1 MES_CAPACITY → 校验 mesCapacity</li>
 *   <li>模式2 STANDARD_CAPACITY → 校验 standardCapacity</li>
 *   <li>模式3 APS_CAPACITY → 校验 apsCapacity</li>
 * </ul>
 *
 * @author APS Team
 */
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
            addInfo(result, "无硫化排程任务，无需校验物料日硫化产能", null);
            return;
        }

        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap == null || lhCapacityMap.isEmpty()) {
            addError(result,
                    "物料日硫化产能映射(materialLhCapacityMap)为空",
                    "请检查物料日硫化产能基础表是否正确配置");
            return;
        }

        // 1. 获取参数配置的计算模式
        DayVulcanizationModeEnum mode = getMode(context);
        log.info("日硫化产能校验：当前模式={}，校验字段={}", mode.getDesc(), mode.getFieldName());

        // 2. 收集硫化任务涉及的所有物料编码
        Set<String> requiredMaterials = lhResults.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requiredMaterials.isEmpty()) {
            addWarn(result, "硫化任务中缺少物料编码", "请检查硫化排程结果数据");
            return;
        }

        // 3. 提取对应字段的值
        Function<MonthPlanProductLhCapacityVo, Integer> fieldExtractor = getFieldExtractor(mode);

        // 4. 逐个校验
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

        // 5. 报告缺失
        if (!missingFromMap.isEmpty()) {
            addError(result,
                    missingFromMap.size() + "个物料在日硫化产能表中未找到",
                    "请检查以下物料是否已在日硫化产能表中配置：" + String.join(", ", truncateList(missingFromMap)));
        }

        if (!fieldNullOrZero.isEmpty()) {
            String list = String.join(", ", truncateList(fieldNullOrZero));
            addError(result,
                    fieldNullOrZero.size() + "个物料缺少" + mode.getFieldName() + "（模式=" + mode.getCode() + "）",
                    "请为以下物料配置" + mode.getDesc() + "(" + mode.getFieldName() + ")：" + list);
        }

        addInfo(result,
                "物料日硫化产能：共" + requiredMaterials.size() + "种物料，"
                        + "模式=" + mode.getDesc() + "(" + mode.getCode() + ")，"
                        + "完整" + validCount + "个，缺失" + (missingFromMap.size() + fieldNullOrZero.size()) + "个",
                null);
    }

    /**
     * 获取日硫化量计算模式
     */
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

    /**
     * 根据模式获取对应字段的取值方法
     */
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
