package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 直裁排程每日完成量回报。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "直裁排程每日完成量回报", description = "直裁排程每日完成量回报")
@TableName("t_cd90_sche_finish_qty")
public class Cd90ScheFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 直裁工单号。 */
    @ApiModelProperty("直裁工单号")
    @TableField("ORDER_NO")
    private String orderNo;

    /** MES完成量归属日期。 */
    @ApiModelProperty("MES完成量归属日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    /** 直裁机台编码。 */
    @ApiModelProperty("直裁机台编码")
    @TableField("MACHINE_CODE")
    private String machineCode;

    /** 帘布大卷编号。 */
    @ApiModelProperty("帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

    /** 帘布代号。 */
    @ApiModelProperty("帘布代号")
    @TableField("CLOTH_CODE")
    private String clothCode;

    /** 物料编码（NC）。 */
    @ApiModelProperty("物料编码（NC）")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /** 物料编码（MES）。 */
    @ApiModelProperty("物料编码（MES）")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 一班（夜班）完成量。 */
    @ApiModelProperty("一班（夜班）完成量")
    @TableField("CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty;

    /** 二班（早班）完成量。 */
    @ApiModelProperty("二班（早班）完成量")
    @TableField("CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty;

    /** 三班（中班）完成量。 */
    @ApiModelProperty("三班（中班）完成量")
    @TableField("CLASS3_FINISH_QTY")
    private BigDecimal class3FinishQty;

    /** 一班（夜班）未完成原因。 */
    @ApiModelProperty("一班（夜班）未完成原因")
    @TableField("CLASS1_UN_REASON")
    private String class1UnReason;

    /** 二班（早班）未完成原因。 */
    @ApiModelProperty("二班（早班）未完成原因")
    @TableField("CLASS2_UN_REASON")
    private String class2UnReason;

    /** 三班（中班）未完成原因。 */
    @ApiModelProperty("三班（中班）未完成原因")
    @TableField("CLASS3_UN_REASON")
    private String class3UnReason;

    /** 一班（夜班）作业人员。 */
    @ApiModelProperty("一班（夜班）作业人员")
    @TableField("CLASS1_PERSON")
    private String class1Person;

    /** 二班（早班）作业人员。 */
    @ApiModelProperty("二班（早班）作业人员")
    @TableField("CLASS2_PERSON")
    private String class2Person;

    /** 三班（中班）作业人员。 */
    @ApiModelProperty("三班（中班）作业人员")
    @TableField("CLASS3_PERSON")
    private String class3Person;

    /** 版本号。 */
    @ApiModelProperty("版本号")
    @TableField("DATA_VERSION")
    private String dataVersion;

    /** 分公司编码。 */
    @ApiModelProperty("分公司编码")
    @TableField("COMPANY_CODE")
    private String companyCode;

    /** 厂别。 */
    @ApiModelProperty("厂别")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
