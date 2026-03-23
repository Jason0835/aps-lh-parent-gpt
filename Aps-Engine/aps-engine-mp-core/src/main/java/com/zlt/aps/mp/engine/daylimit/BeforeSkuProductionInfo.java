package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/**
 * 前Sku排产信息对象
 *
 * @author ZLT
 * @date 20260318
 */
@Getter
public class BeforeSkuProductionInfo implements Serializable {
    /**
     * 排产的Sku描述
     */
    private String materialDesc;
    /**
     * 排产的Sku编码
     */
    private String materialCode;
    /**
     * 胎胚号
     */
    private String embryoCode;
    /**
     * 收尾日
     */
    private Integer closingDay;
    /**
     * 当前排产量
     */
    private Integer productionQty;
    /**
     * 日双模最大硫化量
     */
    private Integer dayMaxQty;
    /**
     * 排产模具
     */
    private Set<String> productionMouldSet;

    /**
     * 创建空的排产Sku信息
     *
     * @return
     */
    public static BeforeSkuProductionInfo buildEmpty(Integer closingDay) {
        return new BeforeSkuProductionInfo("", "", "", closingDay, BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue());
    }

    /**
     * 构建前Sku信息
     *
     * @param productionSkuInfo 排产计划
     * @param productionQty     排产量
     * @param closingDay        收尾日-即排产日
     * @param usedMouldSet      使用模具信息
     * @return
     */
    public static BeforeSkuProductionInfo createByProductionPlan(MonthPlanProductionRequirePlanVo productionSkuInfo, Integer productionQty, Integer closingDay, Set<String> usedMouldSet) {
        Integer singleMouldQty = Optional.ofNullable(productionSkuInfo.getDayVulcanizationQty()).orElse(BigDecimal.ZERO.intValue());
        Integer dayMaxQty = singleMouldQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        BeforeSkuProductionInfo sku = new BeforeSkuProductionInfo(productionSkuInfo.getMaterialDesc(), productionSkuInfo.getMaterialCode(),
                productionSkuInfo.getEmbryoCode(), closingDay, productionQty, dayMaxQty);
        sku.productionMouldSet = usedMouldSet;
        return sku;
    }

    /**
     * 创建
     *
     * @param materialDesc  物料描述
     * @param materialCode  物料编码
     * @param closingDay    收尾日
     * @param productionQty 排产量
     * @param dayMaxQty     日最大硫化量
     * @return
     */
    public static BeforeSkuProductionInfo createBySku(String materialDesc, String materialCode, Integer closingDay, Integer productionQty, Integer dayMaxQty, Set<String> usedMouldSet) {
        BeforeSkuProductionInfo sku = new BeforeSkuProductionInfo(materialDesc, materialCode, "", closingDay, productionQty, dayMaxQty);
        sku.productionMouldSet = usedMouldSet;
        return sku;
    }

    /**
     * 构造函数
     *
     * @param materialDesc  描述
     * @param materialCode  物料编码
     * @param embryoCode    胎胚号
     * @param productionQty 排产量
     * @param dayMaxQty     日硫化量
     */
    public BeforeSkuProductionInfo(String materialDesc, String materialCode, String embryoCode, Integer closingDay, Integer productionQty, Integer dayMaxQty) {
        this.materialDesc = materialDesc;
        this.materialCode = materialCode;
        this.embryoCode = embryoCode;
        this.closingDay = closingDay;
        this.productionQty = productionQty;
        this.dayMaxQty = dayMaxQty;
    }
}
