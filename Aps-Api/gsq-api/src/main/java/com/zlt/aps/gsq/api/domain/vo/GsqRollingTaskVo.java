package com.zlt.aps.gsq.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 钢丝圈自动滚动任务响应
 *
 * @author APS
 */
@Data
@ApiModel(value = "钢丝圈自动滚动任务响应", description = "自动滚动任务进度和调整摘要")
public class GsqRollingTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 任务状态 */
    private String taskStatus;

    /** 进度 */
    private Integer progress;

    /** 当前阶段 */
    private String currentStage;

    /** 当前阶段名称 */
    private String currentStageName;

    /** 工厂编码 */
    private String factoryCode;

    /** 目标班次序号 */
    private Integer targetShiftOrder;

    /** 排程日期 */
    private String scheduleDate;

    /** 批次号 */
    private String batchNo;

    /** 输入指纹 */
    private String inputVersion;

    /** 影响记录数 */
    private Integer affectedCount;

    /** 调整前库存 */
    private Double beforeStockQty;

    /** 调整后库存 */
    private Double afterStockQty;

    /** 问题列表 */
    @ApiModelProperty(value = "问题列表", name = "issues")
    private List<String> issues = new ArrayList<>();

    /** 调整摘要 */
    @ApiModelProperty(value = "调整摘要", name = "summary")
    private Map<String, Object> summary = new LinkedHashMap<>();
}
