package com.zlt.aps.lh.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化机台插单信息VO
 * @date 2025/3/19
 */
@Data
public class LhOrderInsertMachineInfoVO implements Serializable {

    private static final long serialVersionUID = -8597635845955945315L;

    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    private String factoryCode;

    @ApiModelProperty(value = "机台编号", name = "machineCode")
    private String machineCode;

    @ApiModelProperty(value = "机台名称", name = "machineName")
    private String machineName;

    @ApiModelProperty(value = "生产定额，单班一次生产量，单位：条", name = "quota")
    private Integer quota;

    @ApiModelProperty(value = "机械类型", name = "machineType")
    private String machineType;


    @ApiModelProperty(value = "今日计划量")
    private Integer todayPlanQty;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "今日计划开始时间")
    private Date todayStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "今日计划结束时间")
    private Date todayEndTime;

    @ApiModelProperty(value = "今日空闲量")
    private Integer todayAvailableQty;


    @ApiModelProperty(value = "一班计划量")
    private Integer class1PlanQty;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "一班计划开始时间")
    private Date class1StartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "一班计划结束时间")
    private Date class1EndTime;

    @ApiModelProperty(value = "一班空闲量")
    private Integer class1AvailableQty;

    @ApiModelProperty(value = "二班计划量")
    private Integer class2PlanQty;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "二班计划开始时间")
    private Date class2StartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "二班计划结束时间")
    private Date class2EndTime;

    @ApiModelProperty(value = "二班空闲量")
    private Integer class2AvailableQty;

    @ApiModelProperty(value = "三班计划量")
    private Integer class3PlanQty;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "三班计划开始时间")
    private Date class3StartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "三班计划结束时间")
    private Date class3EndTime;

    @ApiModelProperty(value = "三班空闲量")
    private Integer class3AvailableQty;

}
