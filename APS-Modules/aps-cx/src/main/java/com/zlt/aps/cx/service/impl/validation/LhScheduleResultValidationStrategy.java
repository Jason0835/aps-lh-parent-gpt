package com.zlt.aps.cx.service.impl.validation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.MdmSkuConstructionRefMapper;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LhScheduleResultValidationStrategy extends BaseValidationStrategy {

    @Autowired
    private MdmSkuConstructionRefMapper skuConstructionRefMapper;

    private static final Set<String> REQUIRED_FIELDS = new HashSet<>(Arrays.asList(
            "MATERIAL_CODE",
            "EMBRYO_CODE",
            "STRUCTURE_NAME",
            "MATERIAL_DESC",
            "MAIN_MATERIAL_DESC",
            "LH_TIME",
            "MOULD_QTY",
            "SINGLE_MOULD_SHIFT_QTY",
            "CONSTRUCTION_STAGE",
            "PRODUCTION_VERSION"
    ));

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.LH_SCHEDULE_RESULT;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {

        List<LhScheduleResult> lhResults = context.getLhScheduleResults();

        if (lhResults == null || lhResults.isEmpty()) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.empty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.empty.suggestion"));
            return;
        }

        int totalCount = lhResults.size();
        log.info("开始校验硫化排程结果数据完整性，共 {} 条记录，需校验 {} 个必填字段", totalCount, REQUIRED_FIELDS.size());

        validateRequiredFields(lhResults, totalCount, result);
        validateMaterialInfoConfig(context, lhResults, result);
        //validateStructureAllocationConfig(context, lhResults, result);
        validateStructureTreadConfig(context, lhResults, result);
        validateRecipeTypeMatching(lhResults, result);
    }

    private void validateRequiredFields(List<LhScheduleResult> lhResults, int totalCount,
                                       ScheduleDataValidationResult result) {
        Map<String, Integer> missingCountMap = new HashMap<>();
        Map<String, List<String>> missingSampleMap = new HashMap<>();
        for (String field : REQUIRED_FIELDS) {
            missingCountMap.put(field, 0);
            missingSampleMap.put(field, new ArrayList<>());
        }

        for (LhScheduleResult r : lhResults) {
            String materialCode = r.getMaterialCode() != null ? r.getMaterialCode() : "未知物料";

            checkField(r.getMaterialCode(), "MATERIAL_CODE", materialCode, missingCountMap, missingSampleMap);
            checkField(r.getEmbryoCode(), "EMBRYO_CODE", materialCode, missingCountMap, missingSampleMap);
            checkField(r.getStructureName(), "STRUCTURE_NAME", materialCode, missingCountMap, missingSampleMap);
            checkField(r.getMaterialDesc(), "MATERIAL_DESC", materialCode, missingCountMap, missingSampleMap);
            checkField(r.getMainMaterialDesc(), "MAIN_MATERIAL_DESC", materialCode, missingCountMap, missingSampleMap);
            checkNumericField(r.getLhTime(), "LH_TIME", materialCode, missingCountMap, missingSampleMap);
            checkNumericField(r.getMouldQty(), "MOULD_QTY", materialCode, missingCountMap, missingSampleMap);
            checkNumericField(r.getSingleMouldShiftQty(), "SINGLE_MOULD_SHIFT_QTY", materialCode, missingCountMap, missingSampleMap);
            checkField(r.getConstructionStage(), "CONSTRUCTION_STAGE", materialCode, missingCountMap, missingSampleMap);
            checkField(r.getProductionVersion(), "PRODUCTION_VERSION", materialCode, missingCountMap, missingSampleMap);
        }

        boolean hasError = false;
        List<String> errorMessages = new ArrayList<>();

        for (String field : REQUIRED_FIELDS) {
            int missingCount = missingCountMap.get(field);
            String fieldName = I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.field." + field);

            if (missingCount > 0) {
                hasError = true;
                List<String> samples = missingSampleMap.get(field);
                String sampleList = samples.isEmpty() ? "无" : String.join(", ", samples);
                String message = String.format(
                        I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.fieldMissing"),
                        missingCount, fieldName, field, sampleList);
                errorMessages.add(message);
                addError(result, message, getFixSuggestion(field));
            }
        }

        if (!hasError) {
            addInfo(result,
                    String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.complete"), totalCount),
                    null);
        } else {
            long missingFields = errorMessages.size();
            long totalMissing = missingCountMap.values().stream().mapToInt(Integer::intValue).sum();
            addInfo(result,
                    String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.summary"), missingFields, totalMissing),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.summary.suggestion"));
        }

        log.info("硫化排程结果必填字段校验完成：总数={}, 缺失字段数={}, 受影响记录={}",
                totalCount, errorMessages.size(),
                missingCountMap.values().stream().mapToInt(Integer::intValue).sum());
    }

    private void validateMaterialInfoConfig(ScheduleContextVo context, List<LhScheduleResult> lhResults,
                                           ScheduleDataValidationResult result) {
        List<MdmMaterialInfo> materials = context.getMaterials();

        if (materials == null || materials.isEmpty()) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialInfoEmpty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialInfoEmpty.suggestion"));
            return;
        }

        Set<String> configuredMaterialCodes = materials.stream()
                .map(MdmMaterialInfo::getMaterialCode)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        Set<String> lhMaterialCodes = lhResults.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        List<String> missingMaterials = new ArrayList<>();
        for (String materialCode : lhMaterialCodes) {
            if (!configuredMaterialCodes.contains(materialCode)) {
                missingMaterials.add(materialCode);
            }
        }

        if (!missingMaterials.isEmpty()) {
            String message = String.format(
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialMissing"),
                    missingMaterials.size(), String.join(", ", missingMaterials));
            addError(result, message,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialMissing.suggestion"));
        } else {
            addInfo(result,
                    String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialInfoComplete"),
                            configuredMaterialCodes.size(), lhMaterialCodes.size()),
                    null);
        }

        log.info("物料信息配置校验完成：配置物料数={}, 硫化任务物料数={}, 缺失数={}",
                configuredMaterialCodes.size(), lhMaterialCodes.size(), missingMaterials.size());
    }

    private void validateStructureTreadConfig(ScheduleContextVo context, List<LhScheduleResult> lhResults,
                                             ScheduleDataValidationResult result) {
        List<CxStructureTreadConfig> structureTreadConfigs = context.getStructureTreadConfigs();

        if (structureTreadConfigs == null || structureTreadConfigs.isEmpty()) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadConfigEmpty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadConfigEmpty.suggestion"));
            return;
        }

        Set<String> configuredStructures = structureTreadConfigs.stream()
                .map(config -> {
                    String structure = config.getStructureCode();
                    String embryo = config.getEmbryoCode();
                    return structure + "|" + embryo;
                })
                .filter(obj -> true)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        Set<String> lhStructures = lhResults.stream()
                .map(lh -> lh.getStructureName() + "|" + lh.getEmbryoCode())
                .filter(obj -> true)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        List<String> missingStructures = new ArrayList<>();
        for (String structure : lhStructures) {
            if (!configuredStructures.contains(structure)) {
                missingStructures.add(structure);
            }
        }

        if (!missingStructures.isEmpty()) {
            String message = String.format(
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadMissing"),
                    missingStructures.size(), String.join(", ", missingStructures));
            addError(result, message,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadMissing.suggestion"));
        } else {
            addInfo(result,
                    String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadConfigComplete"),
                            configuredStructures.size(), lhStructures.size()),
                    null);
        }

        log.info("结构整车配置校验完成：配置结构数={}, 硫化任务结构数={}, 缺失数={}",
                configuredStructures.size(), lhStructures.size(), missingStructures.size());
    }

    /**
     * 校验示方书类型（embryoType）匹配有效性
     *
     * <p>业务强制要求：所有硫化排程记录必须能从SKU与示方书关系表（T_MDM_SKU_CONSTRUCTION_REF）
     * 中匹配到有效的embryoType，不允许返回null。
     *
     * <p>匹配规则（与CoreScheduleAlgorithmServiceImpl.resolveRecipeType一致）：
     * <ul>
     *   <li>constructionStage → trialStatus 映射：01→X, 02→T, 其他→S</li>
     *   <li>降级顺序：S级尝试 S→T→X，T级尝试 T→X，X级仅尝试 X</li>
     *   <li>多物料（含逗号）仅取第一个物料编码</li>
     * </ul>
     *
     * @param lhResults 硫化排程结果列表
     * @param result    校验结果
     */
    private void validateRecipeTypeMatching(List<LhScheduleResult> lhResults,
                                            ScheduleDataValidationResult result) {
        // ---- 加载SKU与示方书关系映射 ----
        Map<String, String> skuRecipeTypeMap = new HashMap<>();
        try {
            LambdaQueryWrapper<MdmSkuConstructionRef> skuQueryWrapper = new LambdaQueryWrapper<>();
            skuQueryWrapper.select(
                    MdmSkuConstructionRef::getMaterialCode,
                    MdmSkuConstructionRef::getTrialStatus,
                    MdmSkuConstructionRef::getEmbryoType);
            List<MdmSkuConstructionRef> skuRefList = skuConstructionRefMapper.selectList(skuQueryWrapper);
            if (skuRefList != null) {
                for (MdmSkuConstructionRef ref : skuRefList) {
                    if (ref.getMaterialCode() != null && ref.getTrialStatus() != null && ref.getEmbryoType() != null) {
                        String mapKey = ref.getMaterialCode() + "|" + ref.getTrialStatus();
                        skuRecipeTypeMap.putIfAbsent(mapKey, ref.getEmbryoType());
                    }
                }
            }
            log.info("示方书类型校验：SKU与示方书关系映射加载完成，共 {} 条记录", skuRecipeTypeMap.size());
        } catch (Exception e) {
            log.error("示方书类型校验：加载SKU与示方书关系映射失败", e);
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.recipeTypeMapLoadError"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.recipeTypeMapLoadError.suggestion"));
            return;
        }

        if (skuRecipeTypeMap.isEmpty()) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.recipeTypeMapEmpty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.recipeTypeMapEmpty.suggestion"));
            return;
        }

        // ---- 逐条校验匹配有效性 ----
        List<String> unmatchedMaterials = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();

        for (LhScheduleResult lh : lhResults) {
            String materialCode = lh.getMaterialCode();
            String constructionStage = lh.getConstructionStage();

            if (materialCode == null || materialCode.trim().isEmpty()) {
                continue;
            }
            if (constructionStage == null || constructionStage.trim().isEmpty()) {
                continue;
            }

            // 去重：同一物料+施工阶段只校验一次
            String deduplicateKey = materialCode + "|" + constructionStage;
            if (processedKeys.contains(deduplicateKey)) {
                continue;
            }
            processedKeys.add(deduplicateKey);

            String recipeType = resolveRecipeType(skuRecipeTypeMap, materialCode, constructionStage);
            if (recipeType == null) {
                String trialStatus = mapConstructionStageToTrialStatus(constructionStage);
                unmatchedMaterials.add(materialCode + "(施工阶段:" + constructionStage + ",trialStatus:" + trialStatus + ")");
                log.warn("示方书类型匹配失败：物料={}, 施工阶段={}, trialStatus={}, 已尝试降级匹配仍未找到有效embryoType",
                        materialCode, constructionStage, trialStatus);
            }
        }

        if (!unmatchedMaterials.isEmpty()) {
            String message = String.format(
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.recipeTypeNotFound"),
                    unmatchedMaterials.size(), String.join(", ", unmatchedMaterials));
            addError(result, message,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.recipeTypeNotFound.suggestion"));
        } else {
            addInfo(result,
                    String.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.recipeTypeMatchComplete"),
                            processedKeys.size()),
                    null);
        }

        log.info("示方书类型匹配校验完成：需校验物料数={}, 匹配失败数={}", processedKeys.size(), unmatchedMaterials.size());
    }

    /**
     * 解析示方书类型（与CoreScheduleAlgorithmServiceImpl.resolveRecipeType逻辑一致）
     *
     * <p>匹配规则：
     * <ul>
     *   <li>constructionStage → trialStatus 映射：01→X, 02→T, 其他→S</li>
     *   <li>降级顺序：S级尝试 S→T→X，T级尝试 T→X，X级仅尝试 X</li>
     *   <li>多物料（含逗号）仅取第一个物料编码</li>
     *   <li>匹配不上返回 null</li>
     * </ul>
     *
     * @param skuRecipeTypeMap  SKU关系映射（materialCode|trialStatus → embryoType）
     * @param materialCode      物料编码（多个逗号分隔时仅取第一个）
     * @param constructionStage 施工阶段（01/02/03）
     * @return 示方书类型，匹配不上则返回 null
     */
    private String resolveRecipeType(Map<String, String> skuRecipeTypeMap, String materialCode, String constructionStage) {
        if (materialCode == null || materialCode.isEmpty()) {
            return null;
        }

        String trialStatus = mapConstructionStageToTrialStatus(constructionStage);

        // 定义降级顺序
        String[] stagesToTry;
        if ("S".equals(trialStatus)) {
            stagesToTry = new String[]{"S", "T", "X"};
        } else if ("T".equals(trialStatus)) {
            stagesToTry = new String[]{"T", "X"};
        } else {
            stagesToTry = new String[]{"X"};
        }

        // 多物料合并时仅取第一个
        String firstMaterial = materialCode;
        int commaIdx = materialCode.indexOf(',');
        if (commaIdx > 0) {
            firstMaterial = materialCode.substring(0, commaIdx);
        }

        for (String stage : stagesToTry) {
            String skuKey = firstMaterial + "|" + stage;
            String embryoType = skuRecipeTypeMap.get(skuKey);
            if (embryoType != null) {
                return embryoType;
            }
        }

        return null;
    }

    /**
     * 施工阶段映射为trialStatus
     * 01→X（试制）, 02→T（量试）, 其他→S（正式）
     *
     * @param constructionStage 施工阶段
     * @return trialStatus
     */
    private String mapConstructionStageToTrialStatus(String constructionStage) {
        if ("01".equals(constructionStage)) {
            return "X";
        } else if ("02".equals(constructionStage)) {
            return "T";
        } else {
            return "S";
        }
    }

    private void checkField(String value, String field, String materialCode,
                           Map<String, Integer> missingCountMap,
                           Map<String, List<String>> missingSampleMap) {
        if (value == null || value.trim().isEmpty()) {
            missingCountMap.merge(field, 1, Integer::sum);
            List<String> samples = missingSampleMap.get(field);
            if (samples.size() < 5) {
                samples.add(materialCode);
            }
        }
    }

    private void checkNumericField(Integer value, String field, String materialCode,
                                  Map<String, Integer> missingCountMap,
                                  Map<String, List<String>> missingSampleMap) {
        if (value == null || value <= 0) {
            missingCountMap.merge(field, 1, Integer::sum);
            List<String> samples = missingSampleMap.get(field);
            if (samples.size() < 5) {
                samples.add(materialCode + "(当前值:" + value + ")");
            }
        }
    }

    private String getFixSuggestion(String field) {
        String key = "ui.data.column.cxScheduleResult.validation.lhResult.suggestion." + field;
        String suggestion = I18nUtil.getMessage(key);
        if (suggestion != null && !suggestion.equals(key)) {
            return suggestion;
        }
        return I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.suggestion.DEFAULT");
    }

    /**
     * 校验单条硫化排程记录是否有效（所有必填字段均有值）
     * 用于判断历史记录是否为人工导入的无效数据
     *
     * @param record 硫化排程记录
     * @return true=有效, false=无效（存在必填字段缺失）
     */
    public static boolean isValidRecord(LhScheduleResult record) {
        if (record == null) {
            return false;
        }
        // 字符串字段：null或空串视为无效
        if (isBlank(record.getMaterialCode())) return false;
        if (isBlank(record.getEmbryoCode())) return false;
        if (isBlank(record.getStructureName())) return false;
        if (isBlank(record.getMaterialDesc())) return false;
        if (isBlank(record.getMainMaterialDesc())) return false;
        if (isBlank(record.getConstructionStage())) return false;
        if (isBlank(record.getProductionVersion())) return false;
        // 数值字段：null或<=0视为无效
        if (record.getLhTime() == null || record.getLhTime() <= 0) return false;
        if (record.getMouldQty() == null || record.getMouldQty() <= 0) return false;
        if (record.getSingleMouldShiftQty() == null || record.getSingleMouldShiftQty() <= 0) return false;
        return true;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
