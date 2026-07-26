package com.zlt.aps.tc.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎侧排程结果模板导入请求对象。
 */
@Data
@ApiModel(value = "胎侧排程结果模板导入请求", description = "承载导入文件和工厂、排程日期上下文")
public class TcScheduleResultImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 导入文件上下文。 */
    @ApiModelProperty(value = "导入文件上下文")
    private ImportContext importContext;

    /** 导入条件，仅使用工厂和排程日期。 */
    @ApiModelProperty(value = "导入条件")
    private TcScheduleResult scheduleResult;
}
