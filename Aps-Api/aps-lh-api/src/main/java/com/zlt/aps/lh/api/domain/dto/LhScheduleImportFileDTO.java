package com.zlt.aps.lh.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/3/5
 */
@Data
public class LhScheduleImportFileDTO implements Serializable {


    private static final long serialVersionUID = -5152400502966861397L;

    @ApiModelProperty(value = "文件上下文")
    private ImportContext importContext;

    @ApiModelProperty(value = "导入ID")
    private Long importLogId;

    @ApiModelProperty(value = "排程时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date scheduleDate;
}
