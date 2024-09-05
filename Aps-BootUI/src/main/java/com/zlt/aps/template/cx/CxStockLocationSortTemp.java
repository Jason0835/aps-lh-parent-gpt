package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 库存地点生产顺序对象 t_cx_stock_location_sort
 *
 * @author chen
 * @date 2021-07-22
 */
@Data
@ApiModel(value = "库存地点生产顺序对象", description = "库存地点生产顺序对象 ")
public class CxStockLocationSortTemp extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 库存地点字典项KEY，国内配套：T1，国内营销T2、海外配套T3、海外营销 T4，编码采用同主计划一样编码
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.stockLocationSort.stockLocation", dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "库存地点字典项KEY，国内配套：T1，国内营销T2、海外配套T3、海外营销 T4，编码采用同主计划一样编码")
    private String stockLocation;

    /**
     * 生产顺序，1,2,3.4
     */
    @ImportValidated(required = true, number = true, min = 0, maxLength = 999)
    @Excel(name = "ui.data.column.stockLocationSort.productSort")
    @ApiModelProperty(value = "生产顺序，1,2,3.4")
    private Integer productSort;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @ImportValidated(maxLength = 300)
    private String remark;

}
