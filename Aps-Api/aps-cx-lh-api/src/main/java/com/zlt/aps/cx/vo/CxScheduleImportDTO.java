package com.zlt.aps.cx.vo;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import lombok.Data;

/**
 * 成型排程导入DTO
 *
 * @author APS Team
 */
@Data
public class CxScheduleImportDTO {

    private ImportContext importContext;

    private CxScheduleResult scheduleResult;
}
