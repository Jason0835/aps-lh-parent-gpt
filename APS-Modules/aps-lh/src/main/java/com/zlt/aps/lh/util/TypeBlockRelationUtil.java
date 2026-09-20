package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 换活字块关系判定工具。
 *
 * <p>统一“只要同模具 ⇒ 允许换活字块”的判定口径，供 S4.4 换活字块主链、
 * S4.5 新增排产等链路共用，避免各链路各自判断导致同模具切换被误落成
 * 正规换模（01）。判定口径：机台当前在机物料非空且与候选SKU物料不同，
 * 且两侧物料的模具关系集合存在交集即为命中。模具按物料模具关系集合取交集判定，
 * 不再要求两侧胎胚编码或胎胚描述一致。</p>
 *
 * @author APS
 */
public final class TypeBlockRelationUtil {

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private TypeBlockRelationUtil() {
    }

    /**
     * 判断机台当前在机物料与候选SKU是否满足换活字块关系：只要同模具即可。
     *
     * <p>机台当前物料为空、与候选物料相同，或两侧模具关系集合无交集时返回 false。</p>
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param sku 候选SKU
     * @return true-同模具，允许按换活字块处理；false-不满足换活字块关系
     */
    public static boolean canChangeTypeBlock(LhScheduleContext context,
                                             MachineScheduleDTO machine,
                                             SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(machine) || Objects.isNull(sku)) {
            return false;
        }
        String machineMaterialCode = machine.getCurrentMaterialCode();
        if (StringUtils.isEmpty(machineMaterialCode)
                || StringUtils.equals(machineMaterialCode, sku.getMaterialCode())) {
            // 机台没有在机物料或候选物料就是在机物料本身时，不属于换活字块切换。
            return false;
        }
        Set<String> machineMouldCodeSet = resolveMouldCodeSet(context, machineMaterialCode);
        Set<String> skuMouldCodeSet = resolveMouldCodeSet(context, sku.getMaterialCode());
        if (CollectionUtils.isEmpty(machineMouldCodeSet) || CollectionUtils.isEmpty(skuMouldCodeSet)) {
            return false;
        }
        // 模具按模具编码集合取交集，命中任一模具即视为同模具，可直接换活字块。
        for (String mouldCode : machineMouldCodeSet) {
            if (skuMouldCodeSet.contains(mouldCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析物料对应的模具编码集合。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return 模具编码集合，无关系时返回空集合
     */
    private static Set<String> resolveMouldCodeSet(LhScheduleContext context, String materialCode) {
        Set<String> mouldCodeSet = new LinkedHashSet<>(4);
        if (Objects.isNull(context)
                || StringUtils.isEmpty(materialCode)
                || !context.getSkuMouldRelMap().containsKey(materialCode)) {
            return mouldCodeSet;
        }
        for (MdmSkuMouldRel mouldRel : context.getSkuMouldRelMap().get(materialCode)) {
            String mouldCode = mouldRel == null ? null : normalizeCompareToken(mouldRel.getMouldCode());
            if (StringUtils.isEmpty(mouldCode)) {
                continue;
            }
            mouldCodeSet.add(mouldCode);
        }
        return mouldCodeSet;
    }

    /**
     * 规范化比较文本，去首尾空格后返回。
     *
     * @param value 原始文本
     * @return 规范化文本，空值返回null
     */
    private static String normalizeCompareToken(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String normalizedValue = value.trim();
        return StringUtils.isEmpty(normalizedValue) ? null : normalizedValue;
    }
}
