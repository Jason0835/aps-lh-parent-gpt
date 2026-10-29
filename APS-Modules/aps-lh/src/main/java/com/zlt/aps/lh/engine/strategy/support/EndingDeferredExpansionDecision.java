package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;
import java.time.LocalDate;

/**
 * SKU收尾延期增机的只读评估结果，缺口只决定机台机会，不作为生产量账本。
 *
 * @author APS
 * @date 2026/10/27
 */
@Data
public class EndingDeferredExpansionDecision {
    /**
     * 是否命中本次限定的普通续作收尾场景。
     */
    private boolean applicable;
    /**
     * 结构转产表还原后的最大收尾日期。
     */
    private LocalDate structureEndingDate;
    /**
     * 在机机台和本批已提交结果尚未消耗的合法需求量。
     */
    private int remainingQty;
    /**
     * 有效机台在尚未扣账班次及延期区间的合计可完成量。
     */
    private int committedCapacityQty;
    /**
     * 延期后仍未覆盖的需求缺口。
     */
    private int shortageQty;
    /**
     * 当前有效物理机台数，单控左右侧合并计数。
     */
    private int committedMachineCount;

    /**
     * 判断是否仍需要尝试下一台机台，每次正式提交后重新计算形成最小可行前缀。
     *
     * @return 已命中且尚有产能缺口时返回true
     */
    public boolean needsExpansion() {
        return applicable && shortageQty > 0;
    }
}
