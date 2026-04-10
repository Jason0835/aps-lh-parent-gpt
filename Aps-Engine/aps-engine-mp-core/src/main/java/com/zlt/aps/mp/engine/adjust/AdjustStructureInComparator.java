package com.zlt.aps.mp.engine.adjust;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * 结构内调整 排序器
 * 1、有设调整优先级（adjustPriority）的，按调整优先级升序；
 * 2、没有设调整优先级的，按如下规则：
 *
 * 2.1、有高优先级需求的（HEIGHT_QTY）；
 * 2.1.1 标有“物料优先”的；（SCM_PRIORITY）
 * 2.1.2 模具受限（活字块数为2的）净需求量小的优先；（TYPE_BLOCK_QTY）
 * 2.1.3 库销比低的优先；（INVENTORY_SALES_RATIO，类型是BigDecimal）
 * 2.1.4 需求量大的优先（HEIGHT_QTY）
 *
 * 2.2、有净需求量的（CURRENT_NET_QTY）；
 * 2.2.1 标有“物料优先”的；（SCM_PRIORITY）
 * 2.2.2 模具受限（活字块数为2的）净需求量小的优先；（TYPE_BLOCK_QTY）
 * 2.2.3 库销比低的优先；（INVENTORY_SALES_RATIO,类型是BigDecimal）
 * 2.2.4 需求量大的优先（CURRENT_NET_QTY）；
 */
public class AdjustStructureInComparator implements Comparator<MpAdjustStructureIn> {

    @Override
    public int compare(MpAdjustStructureIn o1, MpAdjustStructureIn o2) {
        // 1. 先按 adjustPriority 排序（有值的升序，null 放最后）
        Integer priority1 = o1.getAdjustPriority();
        Integer priority2 = o2.getAdjustPriority();
        if (priority1 != null || priority2 != null) {
            // 使用 nullsLast 确保 null 值排在后面
            int priorityCompare = Comparator.nullsLast(Integer::compareTo)
                    .compare(priority1, priority2);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 如果两个都有优先级且相同，则继续按无优先级规则排序（以保证确定性）
            // 注意：如果两者都有优先级且相等，则视为同一级别，需要进一步比较分组等
        }

        // 2. 都没有调整优先级（或优先级相同），按需求类型分组
        int group1 = getDemandGroup(o1);
        int group2 = getDemandGroup(o2);
        if (group1 != group2) {
            return Integer.compare(group1, group2);
        }

        // 3. 同一组内按子规则排序
        switch (group1) {
            case 1: // 有 HEIGHT_QTY
                return compareHeightQtyGroup(o1, o2);
            case 2: // 有 CURRENT_NET_QTY
                return compareCurrentNetQtyGroup(o1, o2);
            default: // 其他（无需求）
                return 0; // 保持原序，或可再按其他字段（如ID）保证稳定
        }
    }

    /**
     * 需求分组：
     * 1 - 有 HEIGHT_QTY（>0）
     * 2 - 有 CURRENT_NET_QTY（>0）
     * 3 - 其他
     */
    private int getDemandGroup(MpAdjustStructureIn obj) {
        Integer heightQty = obj.getHeightQty();
        if (heightQty != null && heightQty > 0) {
            return 1;
        }
        Integer currentNetQty = obj.getCurrentNetQty();
        if (currentNetQty != null && currentNetQty > 0) {
            return 2;
        }
        return 3;
    }

    /**
     * 有 HEIGHT_QTY 的排序规则
     */
    private int compareHeightQtyGroup(MpAdjustStructureIn o1, MpAdjustStructureIn o2) {
        // 2.1.1 物料优先优先（true 在前）
        boolean scm1 = isScmPriority(o1);
        boolean scm2 = isScmPriority(o2);
        if (scm1 != scm2) {
            return Boolean.compare(scm2, scm1); // scm1 true 时返回 -1，即排前面
        }

        // 2.1.2 模具受限（活字块数为2）且净需求量小的优先
        boolean moldLimit1 = isMoldLimit(o1);
        boolean moldLimit2 = isMoldLimit(o2);
        if (moldLimit1 && moldLimit2) {
            // 两者都受限，比较 HEIGHT_QTY 升序
            int qtyCompare = Integer.compare(o1.getHeightQty(), o2.getHeightQty());
            if (qtyCompare != 0) {
                return qtyCompare;
            }
        } else if (moldLimit1 && !moldLimit2) {
            return -1; // 受限的排在前面
        } else if (!moldLimit1 && moldLimit2) {
            return 1;
        }
        // 都不受限，继续

        // 2.1.3 库销比低的优先（升序）
        BigDecimal ratio1 = o1.getInventorySalesRatio();
        BigDecimal ratio2 = o2.getInventorySalesRatio();
        // 假设 null 视为最大值（放后面），使用 nullsLast
        int ratioCompare = Comparator.nullsLast(BigDecimal::compareTo)
                .compare(ratio1, ratio2);
        if (ratioCompare != 0) {
            return ratioCompare;
        }

        // 2.1.4 需求量大的优先（降序）
        return Integer.compare(o2.getHeightQty(), o1.getHeightQty());
    }

    /**
     * 有 CURRENT_NET_QTY 的排序规则
     */
    private int compareCurrentNetQtyGroup(MpAdjustStructureIn o1, MpAdjustStructureIn o2) {
        // 2.2.1 物料优先优先
        boolean scm1 = isScmPriority(o1);
        boolean scm2 = isScmPriority(o2);
        if (scm1 != scm2) {
            return Boolean.compare(scm2, scm1);
        }

        // 2.2.2 模具受限且净需求量小的优先（净需求量指 CURRENT_NET_QTY）
        boolean moldLimit1 = isMoldLimit(o1);
        boolean moldLimit2 = isMoldLimit(o2);
        if (moldLimit1 && moldLimit2) {
            int qtyCompare = Integer.compare(o1.getCurrentNetQty(), o2.getCurrentNetQty());
            if (qtyCompare != 0) {
                return qtyCompare;
            }
        } else if (moldLimit1 && !moldLimit2) {
            return -1;
        } else if (!moldLimit1 && moldLimit2) {
            return 1;
        }

        // 2.2.3 库销比低的优先
        BigDecimal ratio1 = o1.getInventorySalesRatio();
        BigDecimal ratio2 = o2.getInventorySalesRatio();
        int ratioCompare = Comparator.nullsLast(BigDecimal::compareTo)
                .compare(ratio1, ratio2);
        if (ratioCompare != 0) {
            return ratioCompare;
        }

        // 2.2.4 需求量大的优先（降序）
        return Integer.compare(o2.getCurrentNetQty(), o1.getCurrentNetQty());
    }

    private boolean isScmPriority(MpAdjustStructureIn obj) {
        return obj != null && obj.getScmPriority() != null && YesOrNoEnum.YES.getCode().equals(obj.getScmPriority());
    }

    private boolean isMoldLimit(MpAdjustStructureIn obj) {
        Integer blockQty = obj.getTypeBlockQty();
        return blockQty != null && blockQty == 2;
    }
}