package com.zlt.aps.mps.domain;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * Bom对象
 */
@Data
public class TMesPlmBomInfo {
    /**
     * ID
     */
    private Long id;
    /**
     * 胎胚号-物料表的PLM_CODE
     */
    private String embryoCode;
    /**
     * 胎胚版本-物料表的Edit_no
     */
    private String embryoVersion;

    /**
     * 父物料号-BOM表的PARENT_MATERIAL_CODE
     */
    private String parentMaterialCode;

    /**
     * 父物料号-BOM表的PARENT_CODE
     */
    private String parentCode;

    /**
     * 父物料类型-BOM表的PARENT_MATERIAL_NAME_CODE
     */
    private String parentMaterialNameCode;
    
    /**
     * 父物料版本-BOM表的SAP_VERSION
     */
    private String parentMaterialVersion;

    /**
     * 子物料号-BOM表的CHILD_MATERIAL_CODE
     */
    private String childMaterialCode;

    /**
     * 子物料号-BOM表的CHILD_CODE
     */
    private String childCode;

    /**
     * 子物料类型-BOM表的CHILD_MATERIAL_NAME_CODE
     */
    private String childMaterialNameCode;
    
    /**
     * 子物料名称-只有胎胚有
     */
    private String childMaterialName;
    
    /**
     * 子物料版本-BOM表的CHILD_MATERIAL_VERSION
     */
    private String childMaterialVersion;
    
    /**
     * 子物料SAP号-物料表的SAP_CODE
     */
    private String childSapCode;
    
    /**
     * 用量-BOM表的dosage
     */
    private BigDecimal dosage;
    
    /**
     * 单位-BOM表的unit
     */
    private String unit;
    
    /**
     * 工艺参数-BOM表的RO_CLASSIFICATION_ATTRS
     */
    private String roClassificationAttrs;

    /**
     * 物料参数-施工表的CLASSIFICATION_ATTRS
     */
    private String classificationAttrs;
    
    /**
     * 安装部位-施工表的AH_COMPONENT_LOCATION
     */
    private String aHComponentLocation;
    
    /**
     * 生产阶段-施工表PRODUCTION_STAGE_CODE
     */
    private String productionStageCode;
    
    /**
     * 成型法
     */
    private String moldingMethod;
    
    /**
     * bom数据版本
     */
    private String bomDataVersion;

    /**
     * 物料数据版本
     */
    private String matDataVersion;
    
    private Boolean isSearch = false;
    
    private TMesPlmBomInfo parent;
    
    private List<TMesPlmBomInfo> children;
}
