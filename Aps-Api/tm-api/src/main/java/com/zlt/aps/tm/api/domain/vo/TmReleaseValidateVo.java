package com.zlt.aps.tm.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎面发布校验结果。
 */
@Data
@ApiModel(value = "胎面发布校验结果", description = "发布前原子校验摘要")
public class TmReleaseValidateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否允许提交发布。 */
    @ApiModelProperty(value = "是否允许提交发布", name = "allowed")
    private Boolean allowed;

    /** 选中结果数量。 */
    @ApiModelProperty(value = "选中结果数量", name = "selectedCount")
    private Integer selectedCount;

    /** 校验问题列表。 */
    @ApiModelProperty(value = "校验问题列表", name = "issues")
    private List<TmAutoScheduleIssueVo> issues = new ArrayList<>();
}
