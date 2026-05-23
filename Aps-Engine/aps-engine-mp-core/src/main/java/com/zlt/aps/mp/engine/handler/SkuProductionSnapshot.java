package com.zlt.aps.mp.engine.handler;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * 分组新一轮分配到成型机台后
 * 在模拟排产前，Sku的待排产量数据快照
 *
 * @author ZLT
 * @date 2026-05-23
 */
@Getter
public class SkuProductionSnapshot implements Serializable {
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 高优先级还需排产量
     */
    private Integer heightProductionQty;
    /**
     * 总的还需排产量-净需求排产量
     */
    private Integer productionQty;

    /**
     * 构建数据
     *
     * @param materialDesc        sku物料描述
     * @param heightProductionQty 高优先级待排产量
     * @param productionQty       净需求待排产量
     * @return
     */
    public static SkuProductionSnapshot buildSnapshot(String materialDesc, Integer heightProductionQty, Integer productionQty) {
        if (StringUtils.isBlank(materialDesc)) {
            return null;
        }
        return new SkuProductionSnapshot(materialDesc, heightProductionQty, productionQty);
    }

    /**
     * 构造数据
     *
     * @param materialDesc
     * @param heightProductionQty
     * @param productionQty
     */
    private SkuProductionSnapshot(String materialDesc, Integer heightProductionQty, Integer productionQty) {
        this.materialDesc = materialDesc;
        this.heightProductionQty = heightProductionQty;
        this.productionQty = productionQty;
    }
}
