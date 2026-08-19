package com.zlt.aps.mp.engine.adjust;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * 生产对齐移动 排序器
 * 1、没有高优先级但有中优先级的（MID_PRODUCTION_QTY）；
 * 1.1 中优先级排产量小的优先（MID_PRODUCTION_QTY）；
 * 1.2 库销比高的优先；（INVENTORY_SALES_RATIO,类型是BigDecimal）
 * 1.4 标有“物料优先”的；（SCM_PRIORITY）
 *
 * 2、有高优先级需求的（HEIGHT_PRODUCTION_QTY）；
 * 2.1 高优化级量小的优先（HEIGHT_PRODUCTION_QTY）
 * 2.2 库销比高的优先；（INVENTORY_SALES_RATIO，类型是BigDecimal）
 * 2.4 标有“物料优先”的；（SCM_PRIORITY）
 */
public class AdjustAlignMoveComparator implements Comparator<FactoryMonthPlanFinalAdjustVo> {

    @Override
    public int compare(FactoryMonthPlanFinalAdjustVo o1, FactoryMonthPlanFinalAdjustVo o2) {

        // 1. 按排产分组
        int group1 = getProductionGroup(o1);
        int group2 = getProductionGroup(o2);
        if (group1 != group2) {
            return Integer.compare(group1, group2);
        }

        // 3. 同一组内按子规则排序
        switch (group1) {
            case 1: // 有 MID_PRODUCTION_QTY
                return compareMidQtyGroup(o1, o2);
            case 2: // 有 HEIGHT_PRODUCTION_QTY
                return compareHeightQtyGroup(o1, o2);
            default: // 其他（无需求）
                return 0; // 保持原序，或可再按其他字段（如ID）保证稳定
        }
    }

    /**
     * 排产分组：
     * 1 - 没有 HEIGHT_PRODUCTION_QTY，有 MID_PRODUCTION_QTY（>0）
     * 2 - 有 HEIGHT_PRODUCTION_QTY（>0）
     * 3 - 其他
     */
    private int getProductionGroup(FactoryMonthPlanFinalAdjustVo obj) {
        Integer heightQty = obj.getHeightProductionQty();
        Integer midQty = obj.getMidProductionQty();
        if ((heightQty == null || heightQty == 0) && (midQty != null && midQty > 0)) {
            return 1;
        }

        if (heightQty != null && heightQty > 0) {
            return 2;
        }
        return 0;
    }

    /**
     * 有 HEIGHT_PRODUCTION_QTY 的排序规则
     */
    private int compareHeightQtyGroup(FactoryMonthPlanFinalAdjustVo o1, FactoryMonthPlanFinalAdjustVo o2) {
        // 2.1 高优先级排产量小的优先（升序）
        int heightQtyCompare = Comparator.nullsLast(Integer::compareTo)
                .compare(o2.getHeightProductionQty(), o1.getHeightProductionQty());
        if (heightQtyCompare != 0){
            return heightQtyCompare;
        }
        // 2.2 库销比高的优先
        BigDecimal ratio1 = o1.getInventorySalesRatio();
        BigDecimal ratio2 = o2.getInventorySalesRatio();
        int ratioCompare = Comparator.nullsLast(BigDecimal::compareTo)
                .compare(ratio2, ratio1);
        if (ratioCompare != 0) {
            return ratioCompare;
        }

        // 2.3 物料优先优先
        boolean scm1 = isScmPriority(o1);
        boolean scm2 = isScmPriority(o2);
        if (scm1 != scm2) {
            return Boolean.compare(scm1, scm2);
        }
        return 0;
    }

    /**
     * 有 MID_PRODUCTION_QTY 的排序规则
     */
    private int compareMidQtyGroup(FactoryMonthPlanFinalAdjustVo o1, FactoryMonthPlanFinalAdjustVo o2) {
        // 1.1 中优先级排产量小的优先（升序）
        int midQtyCompare = Comparator.nullsLast(Integer::compareTo)
                .compare(o2.getMidProductionQty(), o1.getMidProductionQty());
        if (midQtyCompare != 0){
            return midQtyCompare;
        }
        // 1.2 库销比高的优先
        BigDecimal ratio1 = o1.getInventorySalesRatio();
        BigDecimal ratio2 = o2.getInventorySalesRatio();
        int ratioCompare = Comparator.nullsLast(BigDecimal::compareTo)
                .compare(ratio2, ratio1);
        if (ratioCompare != 0) {
            return ratioCompare;
        }

        // 1.3 物料优先优先
        boolean scm1 = isScmPriority(o1);
        boolean scm2 = isScmPriority(o2);
        if (scm1 != scm2) {
            return Boolean.compare(scm1, scm2);
        }
        return 0;
    }

    private boolean isScmPriority(FactoryMonthPlanFinalAdjustVo obj) {
        return obj != null && obj.getScmPriority() != null && YesOrNoEnum.YES.getCode().equals(obj.getScmPriority());
    }

}