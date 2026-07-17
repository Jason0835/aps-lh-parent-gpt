package com.zlt.aps.tm.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎面排程结果模板导入请求对象。
 */
@Data
@ApiModel(value = "胎面排程结果模板导入请求", description = "承载导入文件和工厂、模板日期上下文")
public class TmScheduleResultImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 导入文件上下文。 */
    @ApiModelProperty(value = "导入文件上下文")
    private ImportContext importContext;

    /** 导入条件，使用工厂和模板日期字段。 */
    @ApiModelProperty(value = "导入条件")
    private TmScheduleResult scheduleResult;
}
