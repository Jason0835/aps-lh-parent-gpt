package com.zlt.aps.gsq.api.domain.entity;

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
 * APS钢丝圈排程完成量回报接口
 *
 * <p>用于接收MES回报的钢丝圈排程班次完成量数据（夜/早/中三班），
 * 同步流程：MES中间表(MES_GSQ_SCHE_FINISH_QTY) → APS落库表(T_GSQ_SCHE_FINISH_QTY) → 回写T_GSQ_SCHEDULE_RESULT各班次完成量。</p>
 *
 * <p>6班制3天窗口映射（与胎圈一致）：</p>
 * <ul>
 *   <li>D-1（MES日期=排程日期-1）：中班→1班完成量</li>
 *   <li>D  （MES日期=排程日期）：夜班→2班，早班→3班，中班→4班完成量</li>
 *   <li>D+1（MES日期=排程日期+1）：夜班→5班，早班→6班完成量</li>
 * </ul>
 *
 * @author APS Team
 * @since 2026/08/14
 */
@ApiModel(value = "APS钢丝圈排程完成量回报接口", description = "APS钢丝圈排程完成量回报接口")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_GSQ_SCHE_FINISH_QTY")
public class GsqScheFinishQty extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 钢丝圈工单号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.gsqScheFinishQty.orderNo")
    @ApiModelProperty(value = "钢丝圈工单号")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 排程日期
     */
    @ImportExcelValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.gsqScheFinishQty.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 钢丝圈代码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.gsqScheFinishQty.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /**
     * 物料编码（NC）
     */
    @Excel(name = "ui.data.column.gsqScheFinishQty.materialCode")
    @ApiModelProperty(value = "物料编码（NC）")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 夜班(22点-6点)完成量
     */
    @Excel(name = "ui.data.column.gsqScheFinishQty.nightFinishQty")
    @ApiModelProperty(value = "夜班(22点-6点)完成量")
    @TableField(value = "NIGHT_FINISH_QTY")
    private BigDecimal nightFinishQty;

    /**
     * 早班(6点-14点)完成量
     */
    @Excel(name = "ui.data.column.gsqScheFinishQty.dayFinishQty")
    @ApiModelProperty(value = "早班(6点-14点)完成量")
    @TableField(value = "DAY_FINISH_QTY")
    private BigDecimal dayFinishQty;

    /**
     * 中班(14点-22点)完成量
     */
    @Excel(name = "ui.data.column.gsqScheFinishQty.midFinishQty")
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
