package com.zlt.aps.mdm.api.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 品牌信息值
 *
 * @author ZLT
 * @date 20250512
 */
@Data
public class ProductBrandDto implements Serializable {
    /**
     * 品牌编码
     */
    private String brand;
    /**
     * 品牌名称
     */
    private String brandName;
}
