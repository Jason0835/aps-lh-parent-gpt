package com.zlt.aps.mp.engine.domain.dto;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sku日排产信息对象
 * 用以辅助判断 收尾时间点等信息
 *
 * @author ZLT
 * @date 20251230
 */
@Getter
public class SkuDayProductionInfoHelper implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDay;
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
     * 主胎胚描述
     */
    private String mainMaterialDesc;

    /**
     * 主花纹
     */
    private String mainPattern;

    /**
     * 使用的模具编号
     */
    private Set<String> usedMouldSet;
    /**
     * 分组名
     * TBR 结构名
     * PCR 英寸
     */
    private String groupName;
    /**
     * 日总排产量
     */
    private Integer sumProductionQty;
    /**
     * 单模日硫化量
     */
    private Integer dayVulcanizationQty;
    /**
     * 换模或是换活字块的损耗量
     */
    private Integer lossQty;

    /**
     * 创建空排产数据对象
     *
     * @param productionDay  排产日
     * @param productionPlan 排产计划--无关具体Id
     * @param productionQty  实际排产量
     * @param lossQty        损耗量(换模或是换活字块)
     * @param usedMouldSet   排产的模具集合
     */
    public static SkuDayProductionInfoHelper buildEmpty(Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan, Integer productionQty, Integer lossQty, Set<String> usedMouldSet) {
        String materialDesc = productionPlan.getMaterialDesc();
        String materialCode = productionPlan.getMaterialCode();
        String groupName = productionPlan.getStructureName();
        Integer dayVulcanizationQty = productionPlan.getDayVulcanizationQty().intValue();
        SkuDayProductionInfoHelper helper = new SkuDayProductionInfoHelper(productionDay, materialDesc, materialCode, groupName, dayVulcanizationQty);
        helper.sumProductionQty = productionQty;
        helper.embryoCode = productionPlan.getEmbryoCode();
        helper.usedMouldSet = usedMouldSet;
        helper.lossQty = lossQty;
        helper.mainMaterialDesc = productionPlan.getMainMaterialDesc();
        helper.mainPattern = productionPlan.getMainPattern();
        return helper;
    }

    /**
     * 复制
     *
     * @param origin
     * @return
     */
    public static SkuDayProductionInfoHelper createClone(SkuDayProductionInfoHelper origin) {
        if (null == origin) {
            return null;
        }
        SkuDayProductionInfoHelper result = new SkuDayProductionInfoHelper();
        result.productionDay = origin.getProductionDay();
        result.materialDesc = origin.getMaterialDesc();
        result.materialCode = origin.getMaterialCode();
        result.embryoCode = origin.getEmbryoCode();
        Set<String> newUsedMouldSet = new HashSet<>();
        newUsedMouldSet.addAll(origin.getUsedMouldSet());
        result.usedMouldSet = newUsedMouldSet;
        result.groupName = origin.getGroupName();
        result.sumProductionQty = origin.getSumProductionQty();
        result.dayVulcanizationQty = origin.getDayVulcanizationQty();
        result.lossQty = origin.getLossQty();
        return result;
    }

    /**
     * 增加排产量
     *
     * @param productionQty 需要增加的排产量
     */
    public void addProductionDayQty(Integer productionQty, Integer lossQty) {
        if (null == sumProductionQty) {
            sumProductionQty = BigDecimal.ZERO.intValue();
        }
        if (null == productionQty) {
            productionQty = BigDecimal.ZERO.intValue();
        }
        sumProductionQty = sumProductionQty + productionQty;
        if (null == lossQty) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        if (null == this.lossQty) {
            this.lossQty = BigDecimal.ZERO.intValue();
        }
        this.lossQty = this.lossQty + lossQty;
    }

    /**
     * 当前排产是否是模具满产
     *
     * @param context 排产上下文
     * @return
     */
    public boolean isFullProduction(Context context) {
        Integer dayLhMachineQty = dayVulcanizationQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //考虑开产日
        dayLhMachineQty = context.getOpenDayMaxQty(productionDay, dayLhMachineQty);
        return sumProductionQty >= dayLhMachineQty;
    }

    /**
     * 是否匹配同日同Sku
     *
     * @param currentProduction
     * @return
     */
    public boolean isMatchSameDayAndSku(SkuDayProductionInfoHelper currentProduction) {
        if (!productionDay.equals(currentProduction.getProductionDay())) {
            return false;
        }
        return embryoCode.equals(currentProduction.getEmbryoCode());
    }

    /**
     * 同模具同Sku不同优先级排产量合并
     * 同天
     * true 表示可合并 false表示不可合并
     *
     * @param skuDayProductionInfo
     */
    public boolean mergeNewProductionInfo(SkuDayProductionInfoHelper skuDayProductionInfo) {
        if (null == skuDayProductionInfo) {
            return false;
        }
        if (!materialDesc.equals(skuDayProductionInfo.getMaterialDesc())) {
            return false;
        }
        if (!productionDay.equals(skuDayProductionInfo.getProductionDay())) {
            return false;
        }
        Set<String> newProduction = skuDayProductionInfo.getUsedMouldSet();
        if (CollectionUtils.isEmpty(usedMouldSet) || CollectionUtils.isEmpty(newProduction)) {
            return false;
        }
        Set<String> intersectionSet = usedMouldSet.stream().filter(newProduction::contains).collect(Collectors.toSet());
        if (intersectionSet.size() != usedMouldSet.size()) {
            return false;
        }
        Integer addQty = Optional.ofNullable(skuDayProductionInfo.getSumProductionQty()).orElse(BigDecimal.ZERO.intValue());
        Integer sumQty = Optional.ofNullable(sumProductionQty).orElse(BigDecimal.ZERO.intValue());
        sumProductionQty = sumQty + addQty;
        Integer addLossQty = Optional.ofNullable(skuDayProductionInfo.getLossQty()).orElse(BigDecimal.ZERO.intValue());
        Integer lossQty = Optional.ofNullable(this.lossQty).orElse(BigDecimal.ZERO.intValue());
        this.lossQty = lossQty + addLossQty;
        return true;
    }

    /**
     * 最后余量
     *
     * @return
     */
    public Integer getLastRemainder() {
        return sumProductionQty % getDayLhMachineQty();
    }

    /**
     * 硫化机台日硫化量
     *
     * @return
     */
    public Integer getDayLhMachineQty() {
        return dayVulcanizationQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }

    /**
     * 构建函数
     *
     * @param productionDay       排产日
     * @param materialDesc        物料描述
     * @param materialCode        物料编码
     * @param groupName           分组名
     * @param dayVulcanizationQty 日硫化产能
     */
    private SkuDayProductionInfoHelper(Integer productionDay, String materialDesc, String materialCode, String groupName, Integer dayVulcanizationQty) {
        this.productionDay = productionDay;
        this.materialDesc = materialDesc;
        this.materialCode = materialCode;
        this.groupName = groupName;
        this.sumProductionQty = BigDecimal.ZERO.intValue();
        this.dayVulcanizationQty = dayVulcanizationQty;
    }

    /**
     * 空的构造函数
     */
    private SkuDayProductionInfoHelper() {

    }
}
