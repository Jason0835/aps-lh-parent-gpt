package com.zlt.aps.cx.service.impl.validation;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class MoldingMachineValidationStrategy extends BaseValidationStrategy {

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.MOLDING_MACHINE;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        List<MdmMoldingMachine> machines = context.getAvailableMachines();

        if (isEmpty(machines)) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.empty"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.empty.suggestion"));
            return;
        }

        long activeCount = machines.stream()
                .filter(m -> m.getIsActive() != null && ApsConstant.APS_STRING_1.equals(m.getIsActive()))
                .count();

        if (activeCount == 0) {
            addError(result,
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.noActive"),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.noActive.suggestion"));
        } else if (activeCount < machines.size()) {
            addWarn(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.partialInactive"), activeCount, machines.size()),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.partialInactive.suggestion"));
        }

        long validCount = machines.stream()
                .filter(m -> m.getCxMachineCode() != null)
                .count();

        if (validCount < machines.size()) {
            addWarn(result,
                    StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.missingCode"), validCount, machines.size()),
                    I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.missingCode.suggestion"));
        }

        addInfo(result,
                StringUtils.format(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validation.machine.summary"), machines.size(), activeCount),
                null);
    }
}
