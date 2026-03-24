package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * BOM示方书信息实体
 */
@Data
public class MdmBomInfo {

    /** ID */
    private Long id;

    /** 父物料编码 */
    private String parentMaterialCode;

    /** 子物料编码 */
    private String childMaterialCode;

    /** 子物料名称 */
    private String childMaterialName;

    /** 用量 */
    private BigDecimal usageQty;

    /** 单位 */
    private String unit;

    /** 工序 */
    private String process;

    /** 创建时间 */
    private java.util.Date createTime;

    /** 更新时间 */
    private java.util.Date updateTime;
}
