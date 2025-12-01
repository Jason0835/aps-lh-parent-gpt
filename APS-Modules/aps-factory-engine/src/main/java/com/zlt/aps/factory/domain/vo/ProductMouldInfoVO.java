package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 物料的可用模具信息
 *
 * @author Chad
 * 2021年7月16日18:3:45
 */
@Data
public class ProductMouldInfoVO implements Serializable {

    /**
     * 分厂编号
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
    /**
     * 转化-模具、规格关系
     */
    private Map<String, String> mouldMap;
    /**
     * 转化-模具列表
     */
    private List<MouldInfoVO> mouldInfoList;

}
