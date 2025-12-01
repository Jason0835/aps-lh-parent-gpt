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
 * 安全库存对象Temp t_lhfl_safe_stock
 * 作为导入的模板
 * @author hakimryan
 *
 */
@ApiModel(value = "硫磺辅料安全库存对象Temp", description = "硫磺辅料安全库存对象Temp")
@Data
@EqualsAndHashCode(callSuper = true)
public class LhflSafeStockTemp extends ZltBaseEntity {

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
    @Excel(name = "setting.safeStock.material")
    @ImportValidated(name = "setting.safeStock.material", maxLength = 30, required = true)
    @ApiModelProperty(value = "物料名称", position = 40)
    private String material;


    /**
     * 安全库存（车）
     */
    @Excel(name = "setting.safeStock.safeStock")
    @ImportValidated(name = "setting.safeStock.safeStock", required = true, number = true, min = 0, max = 9999999)
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
