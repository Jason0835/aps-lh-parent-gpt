package com.zlt.aps.lh.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 接收传输页面主子表APS硫化模具调整计划对象
 * @author: Chen
 * @since: 2022/6/15 18:47
 */
@ApiModel(value = "主子表APS硫化模具调整计划对象", description = "主子表APS硫化模具调整计划对象")
@Data
public class LhApsMoldAdjustPlanDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /**
     * 硫化机台名称
     */
    @ApiModelProperty(value = "硫化机台名称")
    private String lhMachineName;

    /** 计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期")
    private Date planDate;

    @ApiModelProperty(value = "子表数据集合")
    private List<LhApsMoldAdjustPlan> apsMoldAdjustPlanList = new ArrayList<>();
}
