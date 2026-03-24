package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 施工信息实体
 */
@Data
public class MdmConstructionInfo {

    /** ID */
    private Long id;

    /** 物料编码 */
    private String materialCode;

    /** 施工代码 */
    private String constructionCode;

    /** 施工名称 */
    private String constructionName;

    /** 硫化时间(分钟) */
    private Integer curingTime;

    /** 班产标准 */
    private BigDecimal shiftProductionStandard;

    /** 创建时间 */
    private java.util.Date createTime;

    /** 更新时间 */
    private java.util.Date updateTime;
}
