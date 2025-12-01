package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class FactoryParamVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源分厂编号
     */
    private String factoryCode;

    /**
     * 源物料编号
     */
    private String productTypeCode;


    /**
     * 目标分厂编号
     */
    private String factoryCode1;

    /**
     * 目标物料编号
     */
    private String productTypeCode1;
}
