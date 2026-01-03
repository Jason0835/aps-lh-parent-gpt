package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.enums.ProductionQtyModelEnum;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

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
     * 获取总的还需排产量
     *
     * @return
     */
    public Integer getSumNeedProductionQty() {
        if (CollectionUtils.isEmpty(needProductionList)) {
            return BigDecimal.ZERO.intValue();
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
}
