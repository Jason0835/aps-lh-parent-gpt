package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面自动滚动班次库存对象。
 */
@Data
@ApiModel(value = "胎面自动滚动班次库存对象", description = "按MES物理日和班序隔离的胎面库存快照")
@TableName(value = "T_TM_SHIFT_STOCK")
public class TmShiftStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** MES库存物理日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "MES库存物理日期", name = "stockDate")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    /** 班次顺序，取值一至六。 */
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    @TableField(value = "SHIFT_ORDER")
    private Integer shiftOrder;

    /** 胎面编码。 */
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /** 库存数量。 */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private BigDecimal stockQty;

    /** 不良数量。 */
    @ApiModelProperty(value = "不良数量", name = "badQty")
    @TableField(value = "BAD_QTY")
    private BigDecimal badQty;

    /** 调整数量。 */
    @ApiModelProperty(value = "调整数量", name = "adjustQty")
    @TableField(value = "ADJUST_QTY")
    private BigDecimal adjustQty;
}
