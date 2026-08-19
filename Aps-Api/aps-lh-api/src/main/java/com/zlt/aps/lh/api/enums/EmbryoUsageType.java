package com.zlt.aps.lh.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 胎胚使用类型枚举。
 *
 * <p>将原有“单胎胚/非共用胎胚”合并判断拆分为三个互斥类型，供目标量计算、
 * 胎胚库存收尾门控等逻辑统一使用，避免继续依赖多个语义容易冲突的 boolean 判断。</p>
 *
 * <p>判断优先级：</p>
 * <ol>
 *   <li>同物料多机台生产 -> 共用胎胚；</li>
 *   <li>T 日～本月底存在其他 SKU 使用相同胎胚编码 -> 共用胎胚；</li>
 *   <li>本月整月只有当前 SKU 使用该胎胚编码且非多机台 -> 单胎胚；</li>
 *   <li>其余（本月曾存在其他 SKU，但 T 日～月底已不存在且非多机台）-> 非共用胎胚。</li>
 * </ol>
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum EmbryoUsageType {

    /** 共用胎胚：同物料多机台生产，或 T 日～月底存在其他 SKU 使用相同胎胚编码 */
    SHARED("01", "共用胎胚"),
    /** 单胎胚：本月整月只有当前 SKU 使用该胎胚编码，且非多机台生产 */
    SINGLE("02", "单胎胚"),
    /** 非共用胎胚：本月曾存在其他 SKU 使用该胎胚，但 T 日～月底已不存在，且非多机台生产 */
    NON_SHARED("03", "非共用胎胚");

    /** 类型编码 */
    private final String code;

    /** 类型描述 */
    private final String description;
}
