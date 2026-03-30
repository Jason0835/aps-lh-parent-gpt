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
import java.util.Date;

/**
 * 模具交替计划完成回报实体
 *
 * @author APS Team
 * @since 2026/03/29
 */
@ApiModel(value = "模具交替计划完成回报实体", description = "模具交替计划完成回报实体")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOLD_ALTER_PLAN_FINISH")
public class MdmMoldAlterPlanFinish extends BaseEntity implements Serializable {

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

    @ApiModelProperty(value = "硫化机台编号")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "左右模")
    @TableField(value = "LEFT_RIGHT_MOLD")
    private String leftRightMold;

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

    @ApiModelProperty(value = "删除标识")
    @TableField(value = "IS_DELETE")
    private Integer isDelete;

}
