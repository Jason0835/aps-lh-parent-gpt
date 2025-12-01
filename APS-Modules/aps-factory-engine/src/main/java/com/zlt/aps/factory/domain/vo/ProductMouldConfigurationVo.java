package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 物料的模具关系配置
 *
 * @author ZLT
 * @date 20250219
 */
@Data
public class ProductMouldConfigurationVo implements Serializable {

    /**
     * 可用分厂编号
     */
    private String factoryCode;
    /**
     * 物料编号
     */
    private String productCode;
    /**
     * 模具号
     */
    private String mouldCode;
    /**
     * 规格代号
     */
    private String specCode;
}
