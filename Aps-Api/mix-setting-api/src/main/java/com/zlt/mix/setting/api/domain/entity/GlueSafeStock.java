package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 安全库存对象 t_glue_safe_stock
 *
 * @author Gim
 * @date 2022-03-21
 */
@ApiModel(value = "安全库存对象", description = "安全库存对象 ")
@TableName("t_glue_safe_stock")
@KeySequence(value = "seq_t_glue_safe_stock", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueSafeStock extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_STOCK
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_STOCK", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.safeStock.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.safeStock.mixArea", maxLength = 10, required = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String mixArea;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.safeStock.glue")
    @ImportValidated(name = "setting.safeStock.glue", maxLength = 30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;

    /**
     * 库存量
     */
    @Excel(name = "setting.safeStock.stockNum")
    @ApiModelProperty(value = "库存量", position = 50)
    private transient BigDecimal stockNum;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private transient Date todayDate;

    /**
     * 安全库存（车）
     */
    @Excel(name = "setting.safeStock.safeStock")
    @ImportValidated(name = "setting.safeStock.safeStock", required = true, digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "安全库存（车）", position = 50)
    private BigDecimal safeStock;

    /**
     * 预生产库存倍数
     */
    @Excel(name = "setting.safeStock.reserveStockRate")
    @ImportValidated(name = "setting.safeStock.reserveStockRate", number = true, min = 0, max = 100)
    @ApiModelProperty(value = "预生产库存倍数", position = 55)
    private BigDecimal reserveStockRate;
    /**
     * 备注
     */
    @Excel(name = "setting.safeStock.remark")
    @ImportValidated(name = "setting.safeStock.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 60)
    private String remark;

}
