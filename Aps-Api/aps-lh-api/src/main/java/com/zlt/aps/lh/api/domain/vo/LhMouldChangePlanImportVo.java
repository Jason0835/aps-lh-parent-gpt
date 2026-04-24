package com.zlt.aps.lh.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 模具交替计划实体
 *
 * @author APS Team
 * @since 2026/04/01
 */
@ApiModel(value = "模具交替计划实体", description = "模具交替计划实体")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_lh_mould_change_plan")
public class LhMouldChangePlanImportVo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "分厂编号")
    @Excel(name = "ui.data.column.lhMouldChangePlan.factoryCode", dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "硫化结果批次号")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.lhResultBatchNo")
    @TableField(value = "LH_RESULT_BATCH_NO")
    private String lhResultBatchNo;

    @ApiModelProperty(value = "工单号")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "计划日期")
    @Excel(name = "ui.data.column.lhMouldChangePlan.planDate", dateFormat = "yyyy-MM-dd")
    @TableField(value = "PLAN_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date planDate;

    @ApiModelProperty(value = "计划顺位")
    @Excel(name = "ui.data.column.lhMouldChangePlan.planOrder")
    @TableField(value = "PLAN_ORDER")
    private Integer planOrder;

    @ApiModelProperty(value = "班次")
    @Excel(name = "ui.data.column.lhMouldChangePlan.classIndex", dictType = "CLASS_NUM")
    @TableField(value = "CLASS_INDEX")
    private String classIndex;

    @ApiModelProperty(value = "排程日期")
    @Excel(name = "ui.data.column.lhMouldChangePlan.scheduleDate", dateFormat = "yyyy-MM-dd")
    @TableField(value = "SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;

    @ApiModelProperty(value = "左右模 L-左模；R-右模；LR-左右模")
    @Excel(name = "ui.data.column.lhMouldChangePlan.leftRightMould",dictType = "lr_molds")
    @TableField(value = "LEFT_RIGHT_MOULD")
    private String leftRightMould;

    @ApiModelProperty(value = "硫化机台编号")
    @Excel(name = "ui.data.column.lhMouldChangePlan.lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "硫化机台名称")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    @ApiModelProperty(value = "前规格物料编码")
    @Excel(name = "ui.data.column.lhMouldChangePlan.beforeMaterialCode")
    @TableField(value = "BEFORE_MATERIAL_CODE")
    private String beforeMaterialCode;

    @ApiModelProperty(value = "前规格物料描述")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.beforeMaterialDesc")
    @TableField(value = "BEFORE_MATERIAL_DESC")
    private String beforeMaterialDesc;

    @ApiModelProperty(value = "交替类型 数据字典：CHANGE_MOULD_TYPE")
    @Excel(name = "ui.data.column.lhMouldChangePlan.changeMouldType", dictType = "CHANGE_MOULD_TYPE", width = 25)
    @TableField(value = "CHANGE_MOULD_TYPE")
    private String changeMouldType;

    @ApiModelProperty(value = "后规格物料编码")
    @Excel(name = "ui.data.column.lhMouldChangePlan.afterMaterialCode")
    @TableField(value = "AFTER_MATERIAL_CODE")
    private String afterMaterialCode;

    @ApiModelProperty(value = "后规格物料描述")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.afterMaterialDesc")
    @TableField(value = "AFTER_MATERIAL_DESC")
    private String afterMaterialDesc;

    @ApiModelProperty(value = "更换时间")
    @Excel(name = "ui.data.column.lhMouldChangePlan.changeTime", dateFormat = "yyyy-MM-dd")
    @TableField(value = "CHANGE_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date changeTime;

    @ApiModelProperty(value = "模具号")
    @Excel(name = "ui.data.column.lhMouldChangePlan.mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    @ApiModelProperty(value = "是否发布 0-未发布，1-已发布 2-发布失败 3-超时发布 4-待发布")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.isRelease", dictType = "IS_RELEASE")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "模具交替完成状态 0-未完成；1-已完成")
//    @Excel(name = "ui.data.column.lhMouldChangePlan.mouldStatus", dictType = "finish_completion")
    @TableField(value = "MOULD_STATUS")
    private String mouldStatus;

    @ApiModelProperty(value = "备注说明字段")
    @Excel(name = "ui.data.column.lhMouldChangePlan.remark", width = 50)
    @TableField(value = "REMARK")
    private String remark;

    /**
     * 区间查询参数，不映射到数据库字段。
     */
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planDateStart;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planDateEnd;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date scheduleDateStart;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date scheduleDateEnd;
}
