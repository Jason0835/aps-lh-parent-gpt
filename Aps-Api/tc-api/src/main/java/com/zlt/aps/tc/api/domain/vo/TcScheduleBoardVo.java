package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎侧排程看板聚合结果。
 */
@Data
@ApiModel(value = "胎侧排程看板聚合结果")
public class TcScheduleBoardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 已排结果分页。 */
    @ApiModelProperty(value = "已排结果分页")
    private TcScheduleBoardPageVo scheduledPage = new TcScheduleBoardPageVo();

    /** 日期班次列。 */
    @ApiModelProperty(value = "日期班次列")
    private List<TcScheduleBoardDateColumnVo> dateColumns = new ArrayList<>();

    /** 日期对应当前有效批次。 */
    @ApiModelProperty(value = "日期对应当前有效批次")
    private Map<String, String> batchMap = new LinkedHashMap<>();

    /** 看板汇总。 */
    @ApiModelProperty(value = "看板汇总")
    private TcScheduleBoardSummaryVo summary = new TcScheduleBoardSummaryVo();

    /** 当前范围未排任务数。 */
    @ApiModelProperty(value = "当前范围未排任务数")
    private Long unplannedCount = 0L;
}
