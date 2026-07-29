package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 特殊材料 BOM 关联关系 VO
 * 包含胎胚、半部件物料号、原材料物料号、示方书工艺信息（长度/宽度/幅宽）
 *
 * @author ZLT
 * @date 20260729
 */
@Data
public class SpecialMaterialBomRelationVo implements Serializable {

    /**
     * 胎胚
     */
    private String embryoCode;

    /**
     * 半部件物料号
     */
    private String semiPartCode;

    /**
     * 原材料物料号（特殊材料）
     */
    private String childMaterialCode;

    /**
     * 长度（ProcessCodeEnum.LENGTH 的 PROCESS_VALUE）
     */
    private String processLength;

    /**
     * 宽度（ProcessCodeEnum.WIDTH 的 PROCESS_VALUE）
     */
    private String processWidth;

    /**
     * 幅宽（ProcessCodeEnum.FABRIC_WIDTH 的 PROCESS_VALUE）
     */
    private String processFabricWidth;
}
