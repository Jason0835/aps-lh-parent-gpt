package com.zlt.aps.lh.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 扩展硫化机台信息
 */
@Data
public class LhMachineInfoVo extends LhMachineInfo {


    @ApiModelProperty(value = "剩余时间")
    private long remainTime;

    @ApiModelProperty(value = "剩余产能")
    private Integer remainCapacity;

    @ApiModelProperty(value = "在机模具信息")
    private String onLineMoldInfo;

    @ApiModelProperty(value = "换模标记")
    private String isChangeMoldFlag;

    @ApiModelProperty(value = "当日总剩余定额（各班次剩余定额求和）")
    private Integer dailyRemainingQuota;

    @ApiModelProperty(value = "一班定额")
    private Integer class1Quota;

    @ApiModelProperty(value = "二班定额")
    private Integer class2Quota;

    @ApiModelProperty(value = "三班定额")
    private Integer class3Quota;

    @ApiModelProperty(value = "一班维修时长")
    private Integer class1MaintainTime;

    @ApiModelProperty(value = "二班维修时长")
    private Integer class2MaintainTime;

    @ApiModelProperty(value = "三班维修时长")
    private Integer class3MaintainTime;

    @ApiModelProperty(value = "机台可用开始时间")
    @TableField(exist = false)
    private Date machineAvailableStartTime;

    @ApiModelProperty(value = "机台可用结束时间")
    @TableField(exist = false)
    private Date machineAvailableEndTime;

    @ApiModelProperty(value = "续作换模时间")
    private Date continueChangeMoldDate;
}
