package com.zlt.mix.setting.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 终炼胶库存信息对象Dto t_glue_stock
 *
 * @author Liam
 * @date 2022-04-13
 */
@ApiModel(value = "终炼胶库存信息对象Dto", description = "终炼胶库存信息对象Dto ")
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueStockDto extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_STOCK
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_STOCK", position = 10)
    private Long id;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "setting.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", position = 20)
    private Date stockDate;

    /**
     * 条码
     */
    @Excel(name = "setting.stock.barCode")
    @ImportValidated(name = "setting.stock.barCode", required = true, maxLength = 60, isCode = true)
    @ApiModelProperty(value = "条码", position = 25)
    private String barCode;

    /**
     * 有效截止时间（精确到时分秒）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(name = "setting.stock.validTime", required = true, date = true)
    @Excel(name = "setting.stock.validTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "有效截止时间", position = 27)
    private Date validTime;


    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.stock.mixArea", dictType = "MIX_AREA")
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String mixArea;

    /**
     * 胶料名称
     */
    @Excel(name = "setting.stock.glue")
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;

    /**
     * 库存量
     */
    @Excel(name = "setting.stock.stockNum")
    @ImportValidated(name = "setting.safeStock.safeStock", required = true, digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "库存量", position = 50)
    private BigDecimal stockNum;

    /**
     * 库存重量
     */
    @Excel(name = "setting.stock.stockWeightNum")
    @ImportValidated(name = "setting.stock.stockWeightNum", required = true, number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "库存重量", position = 60)
    private BigDecimal stockWeight;

    /**
     * 库存量
     */
    @Excel(name = "setting.stock.safeStock")
    @ImportValidated(name = "setting.safeStock.safeStock", required = true, digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "安全库存", position = 55)
    private BigDecimal safeStock;


    /**
     * 备注
     */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 60)
    private String remark;

}
