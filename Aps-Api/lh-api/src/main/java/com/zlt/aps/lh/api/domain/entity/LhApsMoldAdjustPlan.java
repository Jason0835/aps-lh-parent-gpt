package com.zlt.aps.lh.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 硫化模具调整计划对象 T_APS_LH_MOLD_ADJUST_PLAN
 *
 * @author Joran.Zhang
 * @date 2022-06-06
 */
@ApiModel(value = "APS硫化模具调整计划对象", description = "APS硫化模具调整计划对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class LhApsMoldAdjustPlan extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应序列SEQ_APS_LH_MOLD_ADJUST_PLAN */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 对应MES单据号数据 */
    @ApiModelProperty(value = "工单号")
    private String moldOrderNo;

    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;



    /** 计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期")
    private Date planDate;

    /** 硫化机台 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.lhMachineName")
    @ApiModelProperty(value = "硫化机台")
    @ImportValidated(required = true)
    private String lhMachineName;

    /** 前规格品号 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.beforeSapCode")
    @ApiModelProperty(value = "前规格品号")
    private String beforeSapCode;

    /** 前规格描述 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.beforeSpecDesc")
    @ApiModelProperty(value = "前规格描述")
    private String beforeSpecDesc;

    /** 前规格胎胚 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.beforeEmbryoCode")
    @ApiModelProperty(value = "前规格胎胚")
    private String beforeEmbryoCode;

    /** 胎胚库存 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.tireRoughStock")
    @ApiModelProperty(value = "库存数")
    @ImportValidated(digits = true)
    private Integer tireRoughStock;

    /** 使用模数 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.useMoldNumber")
    @ApiModelProperty(value = "使用模数")
    @ImportValidated(digits = true)
    private Integer useMoldNumber;

    /** 左右模信息，左模L/右模R */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.leftRightMold")
    @ApiModelProperty(value = "左右模信息，左模L/右模R")
    private String leftRightMold;

    /** 左模具编码 */
    @ApiModelProperty(value = "左模具信息")
    private String leftMoldCode;

    /** 右模具编码 */
    @ApiModelProperty(value = "右模具信息")
    private String rightMoldCode;

    /** 变更类型：数据字典维护拆模换、点数换、合并收尾、拆模合并、左模收尾合并、右模收尾合并 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.changeType",dictType = "MOLD_CHANGE_TYPE")
    @ApiModelProperty(value = "变更类型：数据字典维护拆模换、点数换、合并收尾、拆模合并、左模收尾合并、右模收尾合并")
    @ImportValidated(required = true)
    private String changeType;

    /** 后规格品号 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.afterSapCode")
    @ApiModelProperty(value = "后规格品号")
    @ImportValidated(required = true)
    private String afterSapCode;

    /** 后规格描述 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.afterSpecDesc")
    @ApiModelProperty(value = "后规格描述")
    private String afterSpecDesc;

    /** 后规格胎胚 */
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.afterEmbryoCode")
    @ApiModelProperty(value = "后规格胎胚")
    @ImportValidated(required = true)
    private String afterEmbryoCode;

    /** 换模时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.changeMoldTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "换模时间")
    private Date changeMoldTime;

/*    *//** 完成时间 *//*
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "完成时间")
    private Date finishTime;*/

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    @ApiModelProperty(value = "数据来源,1:手动添加；2：导入", position = 500)
    private String dataSource;

    /**
     * 读取的excel是否为合并单元格
     */
    public Boolean mergeRow=false;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    @ApiModelProperty(value = "模具计划记录id数组")
    private Long[] ids;

    @Excel(name = "ui.data.column.lhApsMoldAdjustPlan.isExecute", dictType = "IS_HAVE")
    @ApiModelProperty(value = "是否执行，0：否，1：是")
    private String isExecute;
}
