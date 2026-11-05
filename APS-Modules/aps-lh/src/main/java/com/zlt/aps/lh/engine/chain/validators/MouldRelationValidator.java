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

import javax.annotation.Resource;
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

    /**
     * 本轮需求资格的只读解析入口，仅用于收窄模具禁用校验。
     */
    @Resource
    private MouldValidationScopeResolver mouldValidationScopeResolver;

    /**
     * 校验模具关系完整性，仅在同一可排物料的全部关联模具均禁用时报告禁用错误。
     *
     * @param context 当前工厂、窗口及基础数据
     * @return 关系完整且不存在全部关联模具均禁用的可排物料时返回 true
     */
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
        Set<String> eligibleMaterialCodes = mouldValidationScopeResolver.resolveMaterialCodes(context);
        Set<String> missingModelInfoMouldCodeSet = new LinkedHashSet<>();
        Set<String> disabledMouldCodeSet = this.collectAllDisabledMouldCodes(
                context, eligibleMaterialCodes, missingModelInfoMouldCodeSet);
        Map<String, MdmModelInfo> modelInfoMap = context.getModelInfoMap();
//        if (!missingModelInfoMouldCodeSet.isEmpty()) {
//            String summaryMsg = buildSummaryMessage(
//                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldMissingSimple"),
//                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldMissingMany"),
//                    getValidatorName(), missingModelInfoMouldCodeSet);
//            log.warn("检测到模具台账缺失, 工厂: {}, 模具号: {}", context.getFactoryCode(),
//                    String.join(MOULD_CODE_DELIMITER, missingModelInfoMouldCodeSet));
//            context.addValidationError(summaryMsg);
//            List<MouldValidationErrorDetail> details = buildMissingDetails(missingModelInfoMouldCodeSet);
//            context.addValidationErrorDetails(details);
//        }
        if (!disabledMouldCodeSet.isEmpty()) {
            String summaryMsg = this.buildSummaryMessage(
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldDisabledSimple"),
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.mouldDisabledMany"),
                    getValidatorName(), disabledMouldCodeSet);
            log.warn("检测到全部关联模具禁用的可排物料, 工厂: {}, 模具号: {}", context.getFactoryCode(),
                    String.join(MOULD_CODE_DELIMITER, disabledMouldCodeSet));
            context.addValidationError(summaryMsg);
            List<MouldValidationErrorDetail> details = this.buildDisabledDetails(disabledMouldCodeSet, modelInfoMap);
            context.addValidationErrorDetails(details);
            return false;
        }
        log.info("模具关系校验通过, SKU模具关系数: {}", context.getSkuMouldRelMap().size());
        return true;
    }

    /**
     * 按可排物料分别判断全部禁用，只汇总命中物料的模具号。
     * <p>到货来源和台账缺失不能证明禁用；保留它们在关联集合中，避免仅凭部分已知模具误判全部禁用。</p>
     *
     * @param context 当前批次的模具关系与台账
     * @param eligibleMaterialCodes 已通过本轮需求范围判断的物料
     * @param missingModelInfoMouldCodeSet 沿用既有台账缺失收集集合
     * @return 全部关联模具禁用物料对应的去重模具号
     */
    private Set<String> collectAllDisabledMouldCodes(LhScheduleContext context, Set<String> eligibleMaterialCodes,
                                                     Set<String> missingModelInfoMouldCodeSet) {
        Set<String> allDisabledMouldCodes = new LinkedHashSet<>();
        Map<String, MdmModelInfo> modelInfoMap = context.getModelInfoMap();
        for (String materialCode : eligibleMaterialCodes) {
            List<MdmSkuMouldRel> mouldRelList = context.getSkuMouldRelMap().get(materialCode);
            if (CollectionUtils.isEmpty(mouldRelList)) {
                continue;
            }
            Set<String> materialMouldCodes = new LinkedHashSet<>();
            Set<String> materialDisabledMouldCodes = new LinkedHashSet<>();
            for (MdmSkuMouldRel mouldRel : mouldRelList) {
                String mouldCode = StringUtils.trim(mouldRel.getMouldCode());
                if (StringUtils.isEmpty(mouldCode)) {
                    continue;
                }
                materialMouldCodes.add(mouldCode);
                if (Objects.nonNull(mouldRel.getBoardingDate())) {
                    continue;
                }
                MdmModelInfo modelInfo = modelInfoMap.get(mouldCode);
                if (Objects.isNull(modelInfo)) {
                    missingModelInfoMouldCodeSet.add(mouldCode);
                    continue;
                }
                if (!MouldStatusUtil.isEnabled(modelInfo.getMouldStatus())) {
                    materialDisabledMouldCodes.add(mouldCode);
                }
            }
            // 每个物料独立比较，其他物料的启用模具不能抵消当前物料的全部禁用。
            if (!materialMouldCodes.isEmpty() && materialMouldCodes.equals(materialDisabledMouldCodes)) {
                allDisabledMouldCodes.addAll(materialDisabledMouldCodes);
                log.warn("可排物料全部关联模具禁用, 工厂: {}, 批次: {}, 物料: {}, 模具号: {}",
                        context.getFactoryCode(), context.getBatchNo(), materialCode,
                        String.join(MOULD_CODE_DELIMITER, materialDisabledMouldCodes));
            }
        }
        return allDisabledMouldCodes;
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
