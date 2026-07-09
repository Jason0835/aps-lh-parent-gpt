package com.zlt.aps.lh.engine.chain.validators;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.constant.LhDataValidationGroupConstant;
import com.zlt.aps.lh.api.domain.dto.MouldValidationErrorDetail;
import com.zlt.aps.lh.api.enums.ValidationPolicyEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.chain.IDataValidator;
import com.zlt.aps.lh.util.MouldStatusUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SKU与模具关系校验器
 *
 * @author APS
 */
@Slf4j
@Component
public class MouldRelationValidator implements IDataValidator {
    private static final String VALIDATOR_KEY = "mouldRelationValidator";
    private static final String MOULD_CODE_DELIMITER = "\u3001";
    private static final int SUMMARY_DISPLAY_LIMIT = 3;

    @Override
    public boolean validate(LhScheduleContext context) {
        if (CollectionUtils.isEmpty(context.getSkuMouldRelMap())) {
            log.warn("SKU与模具关系数据为空, 工厂: {}", context.getFactoryCode());
            context.addValidationError("[" + getValidatorName() + "] "
                    + String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldRelEmpty"), context.getFactoryDisplayName()));
            return false;
        }
        long missingMouldCount = context.getMonthPlanList().stream()
                .filter(p -> p.getMaterialCode() != null
                        && !context.getSkuMouldRelMap().containsKey(p.getMaterialCode()))
                .count();
        if (missingMouldCount > 0) {
            log.warn("有{}个月计划SKU缺少模具关系数据（可能正常，如续作时已有模具）", missingMouldCount);
            context.addValidationError("[" + getValidatorName() + "] "
                    + String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.monthPlanMouldRelMissing"), context.getFactoryDisplayName()));
            return false;
        }
        Set<String> monthPlanSkuSet = context.getMonthPlanList().stream()
                .map(p -> p.getMaterialCode())
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> missingModelInfoMouldCodeSet = new LinkedHashSet<>();
        Set<String> disabledMouldCodeSet = new LinkedHashSet<>();
        Map<String, MdmModelInfo> modelInfoMap = context.getModelInfoMap();
        for (String materialCode : monthPlanSkuSet) {
            List<MdmSkuMouldRel> mouldRelList = context.getSkuMouldRelMap().get(materialCode);
            if (CollectionUtils.isEmpty(mouldRelList)) {
                continue;
            }
            for (MdmSkuMouldRel mouldRel : mouldRelList) {
                String mouldCode = StringUtils.trim(mouldRel.getMouldCode());
                if (StringUtils.isEmpty(mouldCode)) {
                    continue;
                }
                if (Objects.nonNull(mouldRel.getBoardingDate())) {
                    continue;
                }
                MdmModelInfo modelInfo = modelInfoMap.get(mouldCode);
                if (Objects.isNull(modelInfo)) {
                    missingModelInfoMouldCodeSet.add(mouldCode);
                    continue;
                }
                if (!MouldStatusUtil.isEnabled(modelInfo.getMouldStatus())) {
                    disabledMouldCodeSet.add(mouldCode);
                }
            }
        }
        if (!missingModelInfoMouldCodeSet.isEmpty()) {
            String summaryMsg = buildSummaryMessage(
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldMissingSimple"),
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldMissingMany"),
                    getValidatorName(), missingModelInfoMouldCodeSet);
            log.warn("检测到模具台账缺失, 工厂: {}, 模具号: {}", context.getFactoryCode(),
                    String.join(MOULD_CODE_DELIMITER, missingModelInfoMouldCodeSet));
            context.addValidationError(summaryMsg);
            List<MouldValidationErrorDetail> details = buildMissingDetails(missingModelInfoMouldCodeSet);
            context.addValidationErrorDetails(details);
        }
//        if (!disabledMouldCodeSet.isEmpty()) {
//            String summaryMsg = buildSummaryMessage(
//                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldDisabledSimple"),
//                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldDisabledMany"),
//                    getValidatorName(), disabledMouldCodeSet);
//            log.warn("检测到禁用模具, 工厂: {}, 模具号: {}", context.getFactoryCode(),
//                    String.join(MOULD_CODE_DELIMITER, disabledMouldCodeSet));
//            context.addValidationError(summaryMsg);
//            List<MouldValidationErrorDetail> details = buildDisabledDetails(disabledMouldCodeSet, modelInfoMap);
//            context.addValidationErrorDetails(details);
//        }
        if (!missingModelInfoMouldCodeSet.isEmpty()) {
            return false;
        }
        log.info("模具关系校验通过, SKU模具关系数: {}", context.getSkuMouldRelMap().size());
        return true;
    }

    /**
     * 构建摘要消息：数量较少时全部展示，数量较多时只展示前N个并附加"等"
     *
     * @param simpleTemplate   简洁模板（无示例）
     * @param exampleTemplate  带示例模板
     * @param validatorName    校验器名称
     * @param mouldCodeSet     模具编号集合
     * @return 摘要消息
     */
    private String buildSummaryMessage(String simpleTemplate, String manyTemplate,
                                       String validatorName, Set<String> mouldCodeSet) {
        int total = mouldCodeSet.size();
        if (total <= SUMMARY_DISPLAY_LIMIT) {
            String mouldCodeText = String.join(MOULD_CODE_DELIMITER, mouldCodeSet);
            return String.format(simpleTemplate, validatorName, mouldCodeText);
        }
        List<String> examples = mouldCodeSet.stream()
                .limit(SUMMARY_DISPLAY_LIMIT)
                .collect(Collectors.toList());
        String exampleText = String.join(MOULD_CODE_DELIMITER, examples);
        return String.format(manyTemplate, validatorName, total, exampleText);
    }

    /**
     * 构建台账缺失的结构化错误明细
     */
    private List<MouldValidationErrorDetail> buildMissingDetails(Set<String> missingMouldCodeSet) {
        List<MouldValidationErrorDetail> details = new ArrayList<>(missingMouldCodeSet.size());
        for (String mouldCode : missingMouldCodeSet) {
            details.add(new MouldValidationErrorDetail(
                    mouldCode, "", "", "",
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldMissingStatus"), I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldMissingStatusShort")));
        }
        return details;
    }

    /**
     * 构建禁用模具的结构化错误明细（携带台账中的模具名称、规格等信息）
     */
    private List<MouldValidationErrorDetail> buildDisabledDetails(Set<String> disabledMouldCodeSet,
                                                                   Map<String, MdmModelInfo> modelInfoMap) {
        List<MouldValidationErrorDetail> details = new ArrayList<>(disabledMouldCodeSet.size());
        for (String mouldCode : disabledMouldCodeSet) {
            MdmModelInfo modelInfo = modelInfoMap.get(mouldCode);
            String mouldNo = modelInfo != null ? StringUtils.defaultString(modelInfo.getMouldNo(), "") : "";
            String specifications = modelInfo != null ? StringUtils.defaultString(modelInfo.getSpecifications(), "") : "";
            String mouldType = modelInfo != null ? StringUtils.defaultString(modelInfo.getMouldType(), "") : "";
            details.add(new MouldValidationErrorDetail(
                    mouldCode, mouldNo, specifications, mouldType,
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldDisabledStatus"), I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldDisabledStatusShort")));
        }
        return details;
    }

    @Override
    public String getValidatorName() {
        return I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldRelName");
    }

    @Override
    public String getValidatorKey() {
        return VALIDATOR_KEY;
    }

    @Override
    public int getGroup() {
        return LhDataValidationGroupConstant.BASE_DATA_INTEGRITY;
    }

    @Override
    public ValidationPolicyEnum getValidationPolicy() {
        return ValidationPolicyEnum.COLLECT_ALL;
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
