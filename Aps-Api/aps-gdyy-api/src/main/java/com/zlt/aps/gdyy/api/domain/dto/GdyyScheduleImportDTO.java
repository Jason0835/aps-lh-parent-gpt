package com.zlt.aps.gdyy.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import lombok.Data;

/** 钢带压延排程结果固定模板导入参数。 */
@Data
public class GdyyScheduleImportDTO {
    /** 导入文件上下文。 */
    private ImportContext importContext;
    /** 导入范围条件。 */
    private GdyyScheduleResult scheduleResult;
}
