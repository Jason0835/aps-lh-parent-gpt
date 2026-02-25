package com.zlt.aps.mp.api.domain.vo;

import com.zlt.aps.mp.api.domain.entity.ProductALevel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 关联出物料品牌
 *
 * @author ZLT
 * @date 20250512
 */
@Data
public class ProductALevelVo extends ProductALevel {
    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌，字典：biz_brand_type", name = "brand")
    private String brand;
}
