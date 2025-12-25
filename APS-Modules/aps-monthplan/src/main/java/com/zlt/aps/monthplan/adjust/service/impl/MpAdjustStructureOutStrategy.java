package com.zlt.aps.monthplan.adjust.service.impl;

import com.zlt.aps.monthplan.api.annotation.WeekAdjustType;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;

/**
 * 结构外调整策略
 * @author wengpc
 */
@Slf4j
@Service
@WeekAdjustType(adjustType = WeekAdjustTypeEnum.STRUCTURE_OUT)
public class MpAdjustStructureOutStrategy extends AbstractBaseWeekAdjustService {

    @Override
    public void doGenerateAdjust(MpRollAdjustContextDTO contextDTO) {
        // todo 结构外调整逻辑
        contextDTO.setMpAdjustStructureInList(Collections.emptyList());
    }

    @Override
    public void doAutoAdjust(MpRollAdjustContextDTO contextDTO) {

    }

    @Override
    public void doConfirmAdjust(MpRollAdjustContextDTO contextDTO) {

    }

}
