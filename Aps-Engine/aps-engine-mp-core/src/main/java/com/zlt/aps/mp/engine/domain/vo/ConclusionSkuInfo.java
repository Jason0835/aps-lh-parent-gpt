package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收尾的Sku信息
 *
 * @author ZLT
 * @date 20260327
 */
@Slf4j
@Getter
public class ConclusionSkuInfo implements Serializable {
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 生胎代码
     */
    private String embryoCode;
    /**
     * 余量
     */
    private Integer remainingQuantity;
    /**
     * 排产日
     */
    private Integer productionDay;

    /**
     * 构造函数
     *
     * @param materialDesc      物料描述
     * @param materialCode      编码
     * @param embryoCode        生胎编码
     * @param remainingQuantity 余量
     * @param productionDay     排产日
     */
    public ConclusionSkuInfo(String materialDesc, String materialCode, String embryoCode, Integer remainingQuantity, Integer productionDay) {
        this.materialDesc = materialDesc;
        this.materialCode = materialCode;
        this.embryoCode = embryoCode;
        this.remainingQuantity = remainingQuantity;
        this.productionDay = productionDay;
    }

    /**
     * 根据续作信息创建收尾
     *
     * @param continueSkuInfo 续作信息
     * @param productionDay   排产日
     * @return
     */
    public static ConclusionSkuInfo createEmptyConclusionByContinueSku(CxContinueSkuInfoHelper continueSkuInfo, Integer productionDay) {
        return new ConclusionSkuInfo(continueSkuInfo.getMaterialDesc(), continueSkuInfo.getMaterialCode(), continueSkuInfo.getEmbryoCode(), BigDecimal.ONE.intValue(), productionDay);
    }

    /**
     * 根据排产Sku信息，构建收尾信息
     *
     * @param skuDayProductionInfo 余量Sku排产信息
     * @return
     */
    public static ConclusionSkuInfo createConclusionBySkuDayProductionInfo(SkuDayProductionInfoHelper skuDayProductionInfo) {
        return new ConclusionSkuInfo(skuDayProductionInfo.getMaterialDesc(), skuDayProductionInfo.getMaterialCode(), skuDayProductionInfo.getEmbryoCode(), skuDayProductionInfo.getSumProductionQty(), skuDayProductionInfo.getProductionDay());
    }
}
