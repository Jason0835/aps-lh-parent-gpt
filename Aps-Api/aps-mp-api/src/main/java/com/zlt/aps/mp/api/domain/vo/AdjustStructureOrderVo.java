package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 结构调整 排序
 *
 * @author ZLT
 * @date 20250509
 */
@Data
public class AdjustStructureOrderVo implements Serializable {

    /**
     * 结构名称
     */
    private String structureName;

    /**
     * 高优先级SKU的个数
     */
    private Integer heightPriorityCount;

    /**
     * 模具受限的SKU个数
     */
    private Integer mouldLimitCount;

    /**
     * 是否特殊材料
     */
    private String hasSpecialMaterial;

}
