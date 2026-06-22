package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * APS胎圈排程完成量回报接口
 *
 * @author APS Team
 * @since 2026/06/18
 */
@ApiModel(value = "APS胎圈排程完成量回报接口", description = "APS胎圈排程完成量回报接口")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TQ_SCHE_FINISH_QTY")
public class TqScheFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 胎圈工单号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.tqScheFinishQty.orderNo")
    @ApiModelProperty(value = "胎圈工单号")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 排程日期
     */
    @ImportExcelValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.tqScheFinishQty.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 胎圈代码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.tqScheFinishQty.beadCode")
    @ApiModelProperty(value = "胎圈代码")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    /**
     * 物料编码（NC）
     */
    @Excel(name = "ui.data.column.tqScheFinishQty.materialCode")
    @ApiModelProperty(value = "物料编码（NC）")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 夜班(22点-6点)完成量
     */
    @Excel(name = "ui.data.column.tqScheFinishQty.nightFinishQty")
    @ApiModelProperty(value = "夜班(22点-6点)完成量")
    @TableField(value = "NIGHT_FINISH_QTY")
    private BigDecimal nightFinishQty;

    /**
     * 早班(6点-14点)完成量
     */
    @Excel(name = "ui.data.column.tqScheFinishQty.dayFinishQty")
    @ApiModelProperty(value = "早班(6点-14点)完成量")
    @TableField(value = "DAY_FINISH_QTY")
    private BigDecimal dayFinishQty;

    /**
     * 中班(14点-22点)完成量
     */
    @Excel(name = "ui.data.column.tqScheFinishQty.midFinishQty")
    @ApiModelProperty(value = "中班(14点-22点)完成量")
    @TableField(value = "MID_FINISH_QTY")
    private BigDecimal midFinishQty;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 分公司编码
     */
    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 厂别
     */
    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

}
