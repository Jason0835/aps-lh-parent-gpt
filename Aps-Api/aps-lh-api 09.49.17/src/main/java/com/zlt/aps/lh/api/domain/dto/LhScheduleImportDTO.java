package com.zlt.aps.lh.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import lombok.Data;

@Data
public class LhScheduleImportDTO {

    private ImportContext importContext;

    private LhScheduleResult scheduleResult;

}
