package com.zlt.aps.gsq.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 钢丝圈排程结果模板导入请求对象。
 *
 * <p>承载导入文件和工厂、排程日期上下文，供专用模板 {@code excelModel/gsqScheduleResult.xlsx}
 * 导入使用，对齐胎圈 {@code TqScheduleResultImportDTO}。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "钢丝圈排程结果模板导入请求", description = "承载导入文件和工厂、排程日期上下文")
public class GsqScheduleResultImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 导入文件上下文。 */
    @ApiModelProperty(value = "导入文件上下文")
    private ImportContext importContext;

    /** 导入条件，仅使用工厂和排程日期。 */
    @ApiModelProperty(value = "导入条件")
    private GsqScheduleResult scheduleResult;
}