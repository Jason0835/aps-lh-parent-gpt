package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import org.apache.commons.lang3.StringUtils;

/**
 * 按有效成型批次的后结构转产快照统一豁免结构切换专属限制。
 * 只读配置，不授予历史指定资格，也不改变正常结构机台数和资源账本。
 */
public final class StructureTransferExemptionPolicy {

    /**
     * 禁止实例化无状态业务策略。
     */
    private StructureTransferExemptionPolicy() {
    }

    /**
     * 判断当前后结构是否在有效批次内存在明确转产记录。
     *
     * @param context 当前工厂及有效成型批次的排程上下文
     * @param structureName 与后结构大小写敏感精确匹配的结构名称
     * @return 同批同后结构任一记录标记转产时返回 true
     */
    public static boolean isTransferStructure(LhScheduleContext context, String structureName) {
        return context != null && StringUtils.isNotEmpty(structureName)
                && Boolean.TRUE.equals(context.getStructureTransferMap().get(structureName));
    }

    /**
     * 判断当前 SKU 是否免除结构切换专属限制。
     *
     * @param context 当前批次上下文
     * @param sku 当前物料及产品状态的候选
     * @return 当前 SKU 所属后结构为转产时返回 true
     */
    public static boolean shouldBypassStructureSwitch(LhScheduleContext context, SkuScheduleDTO sku) {
        return sku != null && isTransferStructure(context, sku.getStructureName());
    }
}
