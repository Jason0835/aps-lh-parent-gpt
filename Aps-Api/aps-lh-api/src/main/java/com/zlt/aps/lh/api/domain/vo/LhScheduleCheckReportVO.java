package com.zlt.aps.lh.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 硫化排程校验报告VO。
 *
 * @author APS
 * @date 2026/10/27
 */
@Data
@ApiModel(value = "硫化排程校验报告", description = "按检查项分组汇总的校验结果")
public class LhScheduleCheckReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号")
    private String factoryCode;

    /**
     * 业务排程日（yyyy-MM-dd）
     */
    @ApiModelProperty(value = "业务排程日")
    private String scheduleDate;

    /**
     * 检查项结果列表（按检查项注册顺序，无异常时也输出空分组）
     */
    @ApiModelProperty(value = "检查项结果列表")
    private List<LhScheduleCheckItemVO> items = new ArrayList<>();

    /**
     * 是否存在未确认异常
     */
    @ApiModelProperty(value = "是否存在未确认异常")
    private Boolean hasPending = Boolean.FALSE;

    /**
     * 是否存在未确认的强控异常
     */
    @ApiModelProperty(value = "是否存在未确认的强控异常")
    private Boolean hasHardPending = Boolean.FALSE;

    /**
     * 数据是否已过期：校验之后排程结果被修改（结果表 update_time 晚于校验时间）
     */
    @ApiModelProperty(value = "数据是否已过期")
    private Boolean dataStale = Boolean.FALSE;

    /**
     * 最近一次校验时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最近一次校验时间")
    private Date checkTime;
}
