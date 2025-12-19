package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 工厂模具分配比例
 *
 * @author ZLT
 * @date 20251217
 */
@Data
public class MouldAllocationInfoVo implements Serializable {

    /**
     * 工厂编码
     */
    private String factoryCode;
    /**
     * 主花纹
     */
    private String mainPattern;
    /**
     * 花纹
     */
    private String pattern;
    /**
     * 结构名
     */
    private String structureName;
    /**
     * 规格
     */
    private String specifications;
    /**
     * 分配数量
     */
    private Integer allocationQty;
}
