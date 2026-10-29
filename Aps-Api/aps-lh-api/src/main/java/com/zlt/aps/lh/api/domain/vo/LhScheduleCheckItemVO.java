package com.zlt.aps.lh.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 硫化排程校验项结果VO（按检查项分组）。
 *
 * @author APS
 * @date 2026/10/27
 */
@Data
@ApiModel(value = "硫化排程校验项结果", description = "按检查项分组的异常记录")
public class LhScheduleCheckItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 检查项编码
     */
    @ApiModelProperty(value = "检查项编码")
    private String checkCode;

    /**
     * 检查项名称（已按当前语言国际化）
     */
    @ApiModelProperty(value = "检查项名称")
    private String checkName;

    /**
     * 检查级别（1-强控 2-提示）
     */
    @ApiModelProperty(value = "检查级别")
    private String checkLevel;

    /**
     * 未确认异常数
     */
    @ApiModelProperty(value = "未确认异常数")
    private Integer pendingCount;

    /**
     * 最近一次校验时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最近一次校验时间")
    private Date checkTime;

    /**
     * 异常记录明细
     */
    @ApiModelProperty(value = "异常记录明细")
    private List<LhScheduleCheckResult> records = new ArrayList<>();
}
