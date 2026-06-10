package com.zlt.aps.cx.service.impl.validation;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MaterialValidationStrategy extends BaseValidationStrategy {

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.MATERIAL_INFO;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        List<MdmMaterialInfo> materials = context.getMaterials();

        if (isEmpty(lhResults)) {
            addInfo(result, I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.noTask"), null);
            return;
        }

        Set<String> requiredMaterials = lhResults.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> requiredEmbryoCodes = lhResults.stream()
                .map(LhScheduleResult::getEmbryoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (requiredMaterials.isEmpty() && requiredEmbryoCodes.isEmpty()) {
            addWarn(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.missingBothCode"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.missingBothCode.suggestion"));
            return;
        }

        if (isEmpty(materials)) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.materialEmpty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.materialEmpty.suggestion"));
            return;
        }

        Set<String> existingMaterials = materials.stream()
                .map(MdmMaterialInfo::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> existingEmbryoCodes = materials.stream()
                .map(MdmMaterialInfo::getEmbryoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> missingByMaterialCode = new HashSet<>(requiredMaterials);
        missingByMaterialCode.removeAll(existingMaterials);

        Set<String> missingByEmbryoCode = new HashSet<>(requiredEmbryoCodes);
        missingByEmbryoCode.removeAll(existingEmbryoCodes);

        Set<String> allMissing = new HashSet<>();
        allMissing.addAll(missingByMaterialCode);
        allMissing.addAll(missingByEmbryoCode);

        if (!allMissing.isEmpty()) {
            String missingList = String.join(", ",
                    allMissing.size() > 5
                        ? Arrays.asList(allMissing.iterator().next() + "...")
                        : allMissing);
            addError(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.materialMissing"),
                            allMissing.size(), missingByMaterialCode.size(), missingByEmbryoCode.size()),
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.materialMissing.suggestion"), missingList));
        }

        Set<String> materialsWithoutCode = materials.stream()
                .filter(m -> m.getMaterialCode() == null || m.getMaterialCode().isEmpty())
                .map(MdmMaterialInfo::getMaterialCode)
                .collect(Collectors.toSet());

        if (!materialsWithoutCode.isEmpty()) {
            addWarn(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.codeMissing"), materialsWithoutCode.size()),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.codeMissing.suggestion"));
        }

        int coveredCount = requiredMaterials.size() - missingByMaterialCode.size() + requiredEmbryoCodes.size() - missingByEmbryoCode.size();
        addInfo(result,
                StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.material.summary"), materials.size(), coveredCount),
                null);
    }
}
