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
 * 母炼库存信息对象 t_ml_glue_stock
 *
 * @author Liam
 * @date 2022-04-12
 */
@ApiModel(value = "母炼库存信息对象", description = "母炼库存信息对象 ")
@TableName("t_ml_glue_stock")
@KeySequence(value = "seq_t_ml_glue_stock", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MlGlueStock extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_ML_GLUE_STOCK
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_ML_GLUE_STOCK", position = 10)
    private Long id;
    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "setting.mlstock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(name = "setting.mlstock.stockDate", required = true, date = true)
    @ApiModelProperty(value = "库存日期", position = 20)
    private Date stockDate;

    /**
     * 条码
     */
    @Excel(name = "setting.mlstock.barCode")
    @ImportValidated(name = "setting.mlstock.barCode", maxLength = 60)
    @ApiModelProperty(value = "条码", position = 25)
    private String barCode;

    /**
     * 有效截止时间（精确到时分秒）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(name = "setting.mlstock.validTime", date = true)
    @Excel(name = "setting.mlstock.validTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "有效截止时间", position = 27)
    private Date validTime;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.mlstock.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.mlstock.mixArea", maxLength = 10, required = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String mixArea;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.mlstock.glue")
    @ImportValidated(name = "setting.mlstock.glue", maxLength = 30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;

    /**
     * 库存量
     */
    @Excel(name = "setting.mlstock.stockNum")
    @ImportValidated(name = "setting.mlstock.stockNum", required = true, digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "库存量", position = 50)
    private BigDecimal stockNum;

    /**
     * 库存重量
     */
    @Excel(name = "setting.mlstock.stockWeightNum")
    @ImportValidated(name = "setting.mlstock.stockWeightNum", required = true, number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "库存重量", position = 60)
    private BigDecimal stockWeight;

    /**
     * 备注
     */
    @Excel(name = "setting.mlstock.remark")
    @ImportValidated(name = "setting.mlstock.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 60)
    private String remark;

}
