package com.zlt.aps.lh.api.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 支领计划对象 t_mix_take_plan
 * 
 * @author zlt
 * @date 2021-11-09
 */
@ApiModel(value = "支领计划对象", description = "支领计划对象 ")
@Data
public class MixTakePlan extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.take.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 物料编号 */
    @Excel(name = "ui.data.column.take.materialCode")
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 机台名称 */
    @Excel(name = "ui.data.column.take.machineName")
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    /** 计划合计 */
    @Excel(name = "ui.data.column.take.planTotalQty")
    @ApiModelProperty(value = "计划合计")
    private Long planTotalQty;

    /** 中班计划 */
    @Excel(name = "ui.data.column.take.class1PlanQty")
    @ApiModelProperty(value = "中班计划")
    private Long class1PlanQty;

    /** 中班支领 */
    @Excel(name = "ui.data.column.take.class1TakeQty")
    @ApiModelProperty(value = "中班支领")
    private Long class1TakeQty;

    /** 夜班计划 */
    @Excel(name = "ui.data.column.take.class2PlanQty")
    @ApiModelProperty(value = "夜班计划")
    private Long class2PlanQty;

    /** 夜班支领 */
    @Excel(name = "ui.data.column.take.class2TakeQty")
    @ApiModelProperty(value = "夜班支领")
    private Long class2TakeQty;

    /** 白班计划 */
    @Excel(name = "ui.data.column.take.class3PlanQty")
    @ApiModelProperty(value = "白班计划")
    private Long class3PlanQty;

    /** 白班支领 */
    @Excel(name = "ui.data.column.take.class3TakeQty")
    @ApiModelProperty(value = "白班支领")
    private Long class3TakeQty;

    /** 计划日期 */
    @Excel(name = "ui.data.column.take.planDate")
    @ApiModelProperty(value = "计划日期")
    private String planDate;

    /** 删除标识 */
    @ApiModelProperty(value = "计划日期")
    private String delFlag;





}
