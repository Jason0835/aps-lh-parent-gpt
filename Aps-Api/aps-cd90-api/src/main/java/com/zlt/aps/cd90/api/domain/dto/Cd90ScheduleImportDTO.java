package com.zlt.aps.cd90.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import lombok.Data;

/** CD90排程结果模板导入参数。 */
@Data
public class Cd90ScheduleImportDTO {
    /** 导入文件上下文。 */
    private ImportContext importContext;
    /** 导入范围条件。 */
    private Cd90ScheduleResult scheduleResult;
}
