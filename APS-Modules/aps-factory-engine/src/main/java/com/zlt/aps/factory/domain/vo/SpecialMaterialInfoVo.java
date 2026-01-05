package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 特殊材料信息对象
 *
 * @author ZLT
 * @date 20251222
 */
@Data
public class SpecialMaterialInfoVo implements Serializable {
    /**
     * 特殊原材料编码
     */
    private String materialCode;
    /**
     * 特殊原材料描述
     */
    private String materialDesc;
    /**
     * 标准长
     */
    private Long standardLength;
    /**
     * 库存量
     */
    private Long stock;
    /**
     * 总的已排产量
     */
    private Long sumProductionQty;
    /**
     * 现有库存可生产量
     */
    private Long existingInventoryCapacity;

    /**
     * 创建初始化的特殊材料库存对象实例
     *
     * @param stockInfo 特殊材料编码
     * @return
     */
    public static SpecialMaterialInfoVo createInitInfo(SpecialMaterialStockVo stockInfo) {
        SpecialMaterialInfoVo info = new SpecialMaterialInfoVo();
        info.setMaterialCode(stockInfo.getMaterialCode());
        info.setMaterialDesc(stockInfo.getMaterialDesc());
        info.setStandardLength(stockInfo.getStandardLength());
        info.setStock(stockInfo.getStock());
        info.setSumProductionQty(BigDecimal.ZERO.longValue());
        info.setExistingInventoryCapacity(BigDecimal.ZERO.longValue());
        return info;
    }

    /**
     * 增加现有库存转换后的可生产胎胚量
     *
     * @param stock     库存量
     * @param maxDosage 1个胎胚的消耗量
     */
    public void addInventoryCapacity(Long stock, BigDecimal maxDosage) {
        if (null == stock || null == maxDosage) {
            return;
        }
        if (stock <= BigDecimal.ZERO.longValue()) {
            return;
        }
        Long inventoryCapacity = BigDecimal.valueOf(stock).divide(maxDosage, 0, RoundingMode.DOWN).longValue();
        if (null == existingInventoryCapacity) {
            existingInventoryCapacity = BigDecimal.ZERO.longValue();
        }
        existingInventoryCapacity = existingInventoryCapacity + inventoryCapacity;
    }
}
