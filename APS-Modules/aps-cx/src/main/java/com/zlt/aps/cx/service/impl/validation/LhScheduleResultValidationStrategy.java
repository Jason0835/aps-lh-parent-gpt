package com.zlt.aps.cx.service.impl.validation;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LhScheduleResultValidationStrategy extends BaseValidationStrategy {

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
            "EMBRYO_NO",
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
        validateStructureAllocationConfig(context, lhResults, result);
        validateStructureTreadConfig(context, lhResults, result);
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
            checkField(r.getEmbryoNo(), "EMBRYO_NO", materialCode, missingCountMap, missingSampleMap);
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
                String message = StringUtils.format(
                        I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.fieldMissing"),
                        missingCount, fieldName, field, sampleList);
                errorMessages.add(message);
                addError(result, message, getFixSuggestion(field));
            }
        }

        if (!hasError) {
            addInfo(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.complete"), totalCount),
                    null);
        } else {
            long missingFields = errorMessages.size();
            long totalMissing = missingCountMap.values().stream().mapToInt(Integer::intValue).sum();
            addInfo(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.summary"), missingFields, totalMissing),
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
            String message = StringUtils.format(
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialMissing"),
                    missingMaterials.size(), String.join(", ", missingMaterials));
            addError(result, message,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialMissing.suggestion"));
        } else {
            addInfo(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.materialInfoComplete"),
                            configuredMaterialCodes.size(), lhMaterialCodes.size()),
                    null);
        }

        log.info("物料信息配置校验完成：配置物料数={}, 硫化任务物料数={}, 缺失数={}",
                configuredMaterialCodes.size(), lhMaterialCodes.size(), missingMaterials.size());
    }

    private void validateStructureAllocationConfig(ScheduleContextVo context, List<LhScheduleResult> lhResults,
                                                  ScheduleDataValidationResult result) {
        Map<String, List<MpCxCapacityConfiguration>> structureAllocationMap = context.getStructureAllocationMap();

        if (structureAllocationMap == null || structureAllocationMap.isEmpty()) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureAllocationEmpty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureAllocationEmpty.suggestion"));
            return;
        }

        Set<String> lhStructures = lhResults.stream()
                .map(LhScheduleResult::getStructureName)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        List<String> missingStructures = new ArrayList<>();
        for (String structure : lhStructures) {
            if (!structureAllocationMap.containsKey(structure)) {
                missingStructures.add(structure);
            }
        }

        if (!missingStructures.isEmpty()) {
            String message = StringUtils.format(
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureMissing"),
                    missingStructures.size(), String.join(", ", missingStructures));
            addError(result, message,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureMissing.suggestion"));
        } else {
            addInfo(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureAllocationComplete"),
                            structureAllocationMap.size(), lhStructures.size()),
                    null);
        }

        log.info("结构排产配置校验完成：配置结构数={}, 硫化任务结构数={}, 缺失数={}",
                structureAllocationMap.size(), lhStructures.size(), missingStructures.size());
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
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        Set<String> lhStructures = lhResults.stream()
                .map(lh -> lh.getStructureName() + "|" + lh.getEmbryoCode())
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        List<String> missingStructures = new ArrayList<>();
        for (String structure : lhStructures) {
            if (!configuredStructures.contains(structure)) {
                missingStructures.add(structure);
            }
        }

        if (!missingStructures.isEmpty()) {
            String message = StringUtils.format(
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadMissing"),
                    missingStructures.size(), String.join(", ", missingStructures));
            addError(result, message,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadMissing.suggestion"));
        } else {
            addInfo(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.lhResult.structureTreadConfigComplete"),
                            configuredStructures.size(), lhStructures.size()),
                    null);
        }

        log.info("结构整车配置校验完成：配置结构数={}, 硫化任务结构数={}, 缺失数={}",
                configuredStructures.size(), lhStructures.size(), missingStructures.size());
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
}
