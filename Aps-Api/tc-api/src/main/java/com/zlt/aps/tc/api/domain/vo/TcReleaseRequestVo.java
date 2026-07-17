package com.zlt.aps.tc.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 胎侧排程异步发布请求。
 */
@Data
@ApiModel(value = "胎侧排程异步发布请求", description = "同一工厂日期当前批次的批量发布请求")
public class TcReleaseRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /** 排程日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private Date scheduleDate;

    /** 发布结果项。 */
    @ApiModelProperty(value = "发布结果项", name = "items")
    private List<TcReleaseItemVo> items;
}
