package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 库存地点生产顺序
 * </p>
 *
 * @author chen
 * @since 2021-07-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CX_STOCK_LOCATION_SORT")
@ApiModel(value = "CxStockLocationSort对象", description = "库存地点生产顺序")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class CxStockLocationSort extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "库存地点字典项KEY，国内配套：T1，国内营销T2、海外配套T3、海外营销 T4，编码采用同主计划一样编码")
    @TableField("STOCK_LOCATION")
    private String stockLocation;

    @ApiModelProperty(value = "生产顺序，1,2,3.4")
    @TableField("PRODUCT_SORT")
    private Integer productSort;
}
