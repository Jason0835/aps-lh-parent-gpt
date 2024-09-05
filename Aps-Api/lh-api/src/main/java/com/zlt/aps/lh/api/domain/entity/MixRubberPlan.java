package com.zlt.aps.lh.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 胶料日计划计划对象 t_mix_rubber_plan
 * 
 * @author zlt
 * @date 2021-11-10
 */
@ApiModel(value = "胶料日计划计划对象", description = "胶料日计划计划对象 ")
@Data
public class MixRubberPlan extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.rubberPlan.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 物料编号 */
    @Excel(name = "ui.data.column.rubberPlan.materialCode")
    @ApiModelProperty(value = "物料编号")
    private String materialCode;

    /** 机台编号 */
    @Excel(name = "ui.data.column.rubberPlan.machineCode")
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    /** 机台名称 */
    @Excel(name = "ui.data.column.rubberPlan.macheinName")
    @ApiModelProperty(value = "机台名称")
    private String macheinName;

    /** 中班计划 */
    @Excel(name = "ui.data.column.rubberPlan.class1PlanQty")
    @ApiModelProperty(value = "中班计划")
    private Long class1PlanQty;

    /** 夜班计划 */
    @Excel(name = "ui.data.column.rubberPlan.class2PlanQty")
    @ApiModelProperty(value = "夜班计划")
    private Long class2PlanQty;

    /** 白班计划 */
    @Excel(name = "ui.data.column.rubberPlan.class3PlanQty")
    @ApiModelProperty(value = "白班计划")
    private Long class3PlanQty;

    /** 计划日期 */
    @Excel(name = "ui.data.column.rubberPlan.planDate")
    @ApiModelProperty(value = "计划日期")
    private String planDate;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "计划日期")
    private String delFlag;





}
