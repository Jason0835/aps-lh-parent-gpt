package com.zlt.aps.dj.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.domain.IFinishQtyImport;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 垫胶排程计划每日各班完成量
 * @TableName T_NC_DAY_FINISH_QTY
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName(value = "T_DJ_MACHINE_INFO")
public class DjDayFinishQty extends BaseEntity implements IFinishQtyImport {

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

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
     * 垫胶代码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.dayFinishQty.nc.code")
    @ApiModelProperty(value = "垫胶代码", name = "liningCode")
    @TableField(value = "LINING_CODE")
    private String liningCode;

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

    private static final long serialVersionUID = 1L;

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
        return liningCode;
    }

    /**
     * 获取代码对应的字段值
     *
     * @return 结果
     */
    @Override
    public String getCodeField1() {
        return "";
    }
}