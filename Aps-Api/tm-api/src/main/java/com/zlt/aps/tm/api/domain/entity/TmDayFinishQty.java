package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.domain.IFinishQtyImport;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面排程计划每日各班完成量
 * @TableName T_TM_DAY_FINISH_QTY
 * @author chen
 */
@Data
@TableName("T_TM_DAY_FINISH_QTY")
public class TmDayFinishQty extends BaseEntity implements IFinishQtyImport {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

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
     * 胎面代码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.tm.code")
    @ApiModelProperty(value = "胎面代码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /**
     * 中班(12点-24点)完成量
     */
//    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.class1Plan.metre")
    @ApiModelProperty(value = "中班(12点-24点)完成量", name = "dayFinishQty")
    @TableField(value = "DAY_FINISH_QTY")
    private BigDecimal dayFinishQty = BigDecimal.ZERO;

    /**
     * 夜班(0点-12点)完成量
     */
//    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.class2Plan.metre")
    @ApiModelProperty(value = "中班(12点-24点)完成量", name = "nightFinishQty")
    @TableField(value = "NIGHT_FINISH_QTY")
    private BigDecimal nightFinishQty = BigDecimal.ZERO;

    /**
     * 工单号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 获取夜班计划量
     *
     * @return 夜班计划量
     */
    @Override
    public BigDecimal getClass1FinishQty() {
        return dayFinishQty;
    }

    /**
     * 获取早班计划量
     *
     * @return 早班计划量
     */
    @Override
    public BigDecimal getClass2FinishQty() {
        return nightFinishQty;
    }

    /**
     * 获取代码对应的字段值
     *
     * @return 结果
     */
    @Override
    public String getCodeField() {
        return treadCode;
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