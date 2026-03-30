package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 模具交替计划实体
 *
 * @author APS Team
 * @since 2026/03/29
 */
@ApiModel(value = "模具交替计划实体", description = "模具交替计划实体")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOLD_ALTER_PLAN")
public class MdmMoldAlterPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "硫化批次号")
    @TableField(value = "LH_BATCH_NO")
    private String lhBatchNo;

    @ApiModelProperty(value = "工单号")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "计划日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    @ApiModelProperty(value = "班次")
    @TableField(value = "CLASS_INDEX")
    private String classIndex;

    @ApiModelProperty(value = "硫化机台编号")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "计划顺位")
    @TableField(value = "PLAN_INDEX")
    private BigDecimal planIndex;

    @ApiModelProperty(value = "左右模")
    @TableField(value = "LEFT_RIGHT_MOLD")
    private String leftRightMold;

    @ApiModelProperty(value = "当前物料编码（NC）")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "当前物料编码（MES）")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "当前物料描述")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    @ApiModelProperty(value = "计划物料编码（NC）")
    @TableField(value = "PLAN_MATERIAL_CODE")
    private String planMaterialCode;

    @ApiModelProperty(value = "计划物料编码（MES）")
    @TableField(value = "MES_PLAN_MATERIAL_CODE")
    private String mesPlanMaterialCode;

    @ApiModelProperty(value = "计划物料描述")
    @TableField(value = "PLAN_SPEC_DESC")
    private String planSpecDesc;

    @ApiModelProperty(value = "交替类型")
    @TableField(value = "CHANGE_MOLD_TYPE")
    private String changeMoldType;

    @ApiModelProperty(value = "模具号")
    @TableField(value = "MOLD_NO")
    private String moldNo;

    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;

    @ApiModelProperty(value = "模具交替完成状态")
    @TableField(value = "FINISH_STATUS")
    private String finishStatus;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

}
