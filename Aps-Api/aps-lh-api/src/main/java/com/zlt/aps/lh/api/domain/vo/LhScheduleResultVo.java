package com.zlt.aps.lh.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 扩展硫化排程结果
 */
@Data
public class LhScheduleResultVo extends LhScheduleResult {

    @ApiModelProperty(value = "是否单模")
    @TableField(exist = false)
    private String isSingleMold;

    @ApiModelProperty(value = "是否限制")
    @TableField(exist = false)
    private String isLimit;

    @ApiModelProperty(value = "是否交期")
    @TableField(exist = false)
    private String isDelivery;

    @ApiModelProperty(value = "是否续作")
    @TableField(exist = false)
    private String isContinue;

    @ApiModelProperty(value = "是否收尾")
    @TableField(exist = false)
    private String isEnd;

    @ApiModelProperty(value = "规格排序标记")
    @TableField(exist = false)
    private Integer priority;

    @ApiModelProperty(value = "剩余模数")
    @TableField(exist = false)
    private Integer remainMoldQty;

    @ApiModelProperty(value = "可用模具列表")
    @TableField(exist = false)
    private List<LhMoldInfoVo> availLhMoldInfoVoList;

    @ApiModelProperty(value = "可用硫化机台列表")
    @TableField(exist = false)
    private List<LhMachineInfoVo> availableLhMachineList;

    @ApiModelProperty(value = "备用可用硫化机台列表")
    @TableField(exist = false)
    private List<LhMachineInfoVo> copyAvailableLhMachineList;

    @ApiModelProperty(value = "续作机台列表")
    @TableField(exist = false)
    private List<String> continuedMachineList;

    @ApiModelProperty(value = "寸口")
    @TableField(exist = false)
    private BigDecimal proSize;

    @ApiModelProperty(value = "机械硫化时间(秒)", name = "machineryCuringTime")
    @TableField(exist = false)
    private Integer machineryCuringTime;

    @ApiModelProperty(value = "液压硫化时间(秒)", name = "hydraulicPressureCuringTime")
    @TableField(exist = false)
    private Integer hydraulicPressureCuringTime;

    @ApiModelProperty(value = "合模压力(PA)", name = "mouldClampingPressure")
    private BigDecimal mouldClampingPressure;

    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    @TableField(value = "MOLD_CAVITY")
    private String moldCavity;

    @ApiModelProperty(value = "获取品牌", name = "brandOrder")
    private String brandOrder;
}
