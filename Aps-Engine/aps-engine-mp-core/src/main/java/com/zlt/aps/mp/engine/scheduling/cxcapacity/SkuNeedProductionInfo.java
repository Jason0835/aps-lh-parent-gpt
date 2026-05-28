package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ProductionQtyModelEnum;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * sku需排产信息
 * 包含排产模式
 * 及对应的计划
 *
 * @author ZLT
 * @date 20250221
 */
@Getter
public class SkuNeedProductionInfo implements Serializable {
    /**
     * 排产量模式
     */
    private ProductionQtyModelEnum productionQtyModel;
    /**
     * 需排产计划
     */
    private List<MonthPlanProductionRequirePlanVo> needProductionList;

    public SkuNeedProductionInfo(ProductionQtyModelEnum productionQtyModel, List<MonthPlanProductionRequirePlanVo> needProductionList) {
        this.productionQtyModel = productionQtyModel;
        this.needProductionList = needProductionList;
    }

    /**
     * 获取排产的Sku-物料描述
     * 进入到此环节的数据，排产计划Sku物料描述不能为空
     *
     * @return
     */
    public String getMaterialDesc() {
        return getFieldNameValue(MonthPlanProductionRequirePlanVo::getMaterialDesc);
    }

    /**
     * 获取排产Sku-分组名
     * 进入到此环节的数据，排产计划Sku的分组名不能为空
     *
     * @return
     */
    public String getGroupName() {
        return getFieldNameValue(MonthPlanProductionRequirePlanVo::getStructureName);
    }

    /**
     * 获取总的还需排产量
     *
     * @return
     */
    public Integer getSumNeedProductionQty() {
        if (CollectionUtils.isEmpty(needProductionList)) {
            return BigDecimal.ZERO.intValue();
        }
        if (ProductionQtyModelEnum.REMAIN_MATCHING_QTY == productionQtyModel) {
            return needProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
        }
        if (ProductionQtyModelEnum.NET_QTY == productionQtyModel) {
            return needProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
        }
        return needProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
    }

    /**
     * 获取计划的双模日硫化量
     *
     * @return
     */
    public Integer getDayMaxProductionQty() {
        if (CollectionUtils.isEmpty(needProductionList)) {
            return BigDecimal.ZERO.intValue();
        }
        return needProductionList.get(BigDecimal.ZERO.intValue()).getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }

    /**
     * 最大还需排产天数
     *
     * @return
     */
    public Integer getMaxNeedDays() {
        return BigDecimal.valueOf(getSumNeedProductionQty()).divide(BigDecimal.valueOf(getDayMaxProductionQty()), BigDecimal.ZERO.intValue(), RoundingMode.UP).intValue();
    }

    /**
     * 获取String属性值
     *
     * @param fieldNameGetter 属性信息
     * @return
     */
    private String getFieldNameValue(Function<MonthPlanProductionRequirePlanVo, String> fieldNameGetter) {
        if (null == fieldNameGetter || CollectionUtils.isEmpty(needProductionList)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> hasBankList = needProductionList.stream().filter(single -> StringUtils.isBlank(fieldNameGetter.apply(single))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(hasBankList)) {
            return null;
        }
        Set<String> fieldNameValueSet = needProductionList.stream().map(fieldNameGetter).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(fieldNameValueSet)) {
            return null;
        }
        if (fieldNameValueSet.size() != BigDecimal.ONE.intValue()) {
            return null;
        }
        return fieldNameGetter.apply(needProductionList.get(BigDecimal.ZERO.intValue()));
    }
}
