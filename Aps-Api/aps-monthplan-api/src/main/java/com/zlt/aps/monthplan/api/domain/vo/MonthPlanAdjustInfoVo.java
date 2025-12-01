package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 月计划调整控制信息对象
 *
 * @author ZLT
 * @date 20250320
 */
@Data
@ApiModel(value = "月计划调整控制信息对象", description = "月计划调整控制信息对象")
public class MonthPlanAdjustInfoVo implements Serializable {
    /**
     * 开始调整的日期数
     */
    @ApiModelProperty(value = "开始调整的日期数", name = "startAdjustDay")
    private Integer startAdjustDay;

    /**
     * 开始调整的日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "开始调整的日期", name = "startAdjustDate")
    private Date startAdjustDate;
    /**
     * 不可调整的天数列表（停工日）
     */
    @ApiModelProperty(value = "不可调整的天数列表（停工日）", name = "noAdjustDayList", hidden = true)
    private List<Integer> noAdjustDayList;
    /**
     * 当前操作日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "当前操作日", name = "operateDate", hidden = true)
    private Date operateDate;

    /**
     * 分厂定稿版本信息
     */
    @ApiModelProperty(value = "分厂定稿版本信息", name = "finalVersionInfo")
    private FactoryMonthPlanFinalVersionInfoVo finalVersionInfo;

    /**
     * 获取停工日
     *
     * @return
     */
    @ApiModelProperty(value = "获取停工日", name = "stopDays", hidden = true)
    public Set<Integer> getStopDays() {
        Set<Integer> stopDays = new HashSet<>();
        if (CollectionUtils.isEmpty(noAdjustDayList)) {
            return stopDays;
        }
        noAdjustDayList.forEach(noAdjustDay -> stopDays.add(noAdjustDay));
        return stopDays;
    }
}
