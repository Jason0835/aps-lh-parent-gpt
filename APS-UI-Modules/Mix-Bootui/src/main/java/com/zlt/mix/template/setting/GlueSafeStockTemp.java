package com.zlt.mix.template.setting;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 安全库存对象Temp t_glue_safe_stock
 * 作为导入的模板
 *
 * @author Gim
 * @date 2022-03-21
 */
@ApiModel(value = "安全库存对象Temp", description = "安全库存对象Temp")
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueSafeStockTemp extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

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
     * 安全库存（车）
     */
    @Excel(name = "setting.safeStock.safeStock")
    @ImportValidated(name = "setting.safeStock.safeStock", required = true, number = true, min = 0, max = 9999999)
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
