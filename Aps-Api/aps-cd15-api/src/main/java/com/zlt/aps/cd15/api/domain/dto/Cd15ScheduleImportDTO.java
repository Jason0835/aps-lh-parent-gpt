package com.zlt.aps.cd15.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import lombok.Data;

/** CD15排程结果模板导入参数。 */
@Data
public class Cd15ScheduleImportDTO {
    private ImportContext importContext;
    private Cd15ScheduleResult scheduleResult;
}
