package com.zlt.aps.xwyy.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import lombok.Data;

/** 纤维压延排程结果固定模板导入参数。 */
@Data
public class XwyyScheduleImportDTO {
    /** 导入文件上下文。 */
    private ImportContext importContext;
    /** 导入范围条件。 */
    private XwyyScheduleResult scheduleResult;
}
