package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 工厂周期结构最低硫化配比配置
 *
 * @author ZLT
 * @date 20251222
 */
@Data
public class CycleStructureMinLhMachineQtyVo implements Serializable {
    /**
     * 产品结构
     */
    private String structureName;
    /**
     * 最低硫化配比值
     */
    private Integer minLhMachineQty;

    /**
     * 月份-最低硫化配比值
     */
    private Integer monthMinLhMachineQty;
}
