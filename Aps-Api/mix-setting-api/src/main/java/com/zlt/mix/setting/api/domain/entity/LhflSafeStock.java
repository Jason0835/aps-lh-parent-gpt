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
 * 安全库存对象 t_lhfl_safe_stock
 * @author hakimryan
 *
 */
@ApiModel(value = "硫磺辅料安全库存对象", description = "硫磺辅料安全库存对象 ")
@TableName("t_lhfl_safe_stock")
@KeySequence(value = "seq_t_lhfl_safe_stock", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class LhflSafeStock extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_LHFL_STOCK
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_LHFL_STOCK", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.safeStock.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.safeStock.mixArea", maxLength = 10, required = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String mixArea;
    /**
     * 物料名称
     */
    @Excel(name = "setting.safeStock.material")
    @ImportValidated(name = "setting.safeStock.material", maxLength = 30, required = true)
    @ApiModelProperty(value = "物料名称", position = 40)
    private String material;

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
     * 备注
     */
    @Excel(name = "setting.safeStock.remark")
    @ImportValidated(name = "setting.safeStock.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 60)
    private String remark;

}
