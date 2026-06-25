package com.zlt.aps.gdyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.domain.IFinishQtyImport;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：GdyyDayFinishQty.java
 * 描    述：钢丝压延排程计划每日各班完成量对象 t_gdyy_day_finish_qty
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-05
 */
@ApiModel(value = "钢丝压延排程计划每日各班完成量对象", description = "钢丝压延排程计划每日各班完成量对象")
@Data
@TableName(value = "T_GDYY_DAY_FINISH_QTY")
public class GdyyDayFinishQty extends BaseEntity implements IFinishQtyImport {

    private static final long serialVersionUID = 1L;

    /**
     * 排程时间
     */
    @ImportExcelValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.dayFinishQty.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程时间", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;
    /**
     * 钢压大卷编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.gdyy.code")
    @ApiModelProperty(value = "钢压大卷编号", name = "bigRollCode")
    @TableField(value = "BIG_ROLL_CODE")
    private String bigRollCode;

    /**
     * 生产线
     */
//    @Excel(name = "ui.data.column.dayFinishQty.machineId")
    @ApiModelProperty(value = "钢压大卷编号", name = "bigRollCode")
    @TableField(exist = false)
    private String machineId;

    /**
     * 中班(12点-24点)完成量
     */
//    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.class1Plan.metre")
    @ApiModelProperty(value = "中班(12点-24点)完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty = BigDecimal.ZERO;

    /**
     * 夜班(0点-12点)完成量
     */
//    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.class2Plan.metre")
    @ApiModelProperty(value = "夜班(0点-12点)完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty = BigDecimal.ZERO;

    /**
     * 工单号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;


    /**
     * 删除标识：0--正常，1-删除
     */
    @ApiModelProperty(value = "删除标识：0--正常，1-删除", name = "delFlag")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    /**
     * 获取夜班计划量
     *
     * @return 夜班计划量
     */
    @Override
    public BigDecimal getClass1FinishQty() {
        return class1FinishQty;
    }

    /**
     * 获取早班计划量
     *
     * @return 早班计划量
     */
    @Override
    public BigDecimal getClass2FinishQty() {
        return class2FinishQty;
    }

    /**
     * 赋值夜班计划量
     *
     * @param nightFinishQty 夜班计划量
     */
    @Override
    public void setNightFinishQty(BigDecimal nightFinishQty) {
        this.class1FinishQty = nightFinishQty;
    }

    /**
     * 赋值早班计划量
     *
     * @param dayFinishQty 早班计划量
     */
    @Override
    public void setDayFinishQty(BigDecimal dayFinishQty) {
        this.class2FinishQty = dayFinishQty;
    }

    /**
     * 获取代码对应的字段值
     *
     * @return 结果
     */
    @Override
    public String getCodeField() {
        return bigRollCode;
    }

    /**
     * 获取代码对应的字段值
     *
     * @return 结果
     */
    @Override
    public String getCodeField1() {
        return null;
    }
}