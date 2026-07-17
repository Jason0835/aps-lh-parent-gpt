package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎侧发布选中结果项。
 */
@Data
@ApiModel(value = "胎侧发布选中结果项", description = "结果ID和前端期望任务版本")
public class TcReleaseItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程结果ID。 */
    @ApiModelProperty(value = "排程结果ID", name = "resultId")
    private Long resultId;

    /** 前端期望任务版本。 */
    @ApiModelProperty(value = "前端期望任务版本", name = "expectedTaskVersion")
    private Long expectedTaskVersion;
}
