package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 换活字块关系判定工具。
 *
 * <p>统一“同胎胚且同模具 ⇒ 允许换活字块”的判定口径，供 S4.4 换活字块主链、
 * S4.5 新增排产等链路共用，避免各链路各自判断导致同胎胚同模具切换被误落成
 * 正规换模（01）。判定逻辑与既有换活字块主链保持一致：胎胚编码或胎胚描述命中
 * 其一即可视为同胎胚，模具按物料模具关系集合取交集判定。</p>
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
     * 判断机台当前在机物料与候选SKU是否满足换活字块关系：同胎胚且同模具。
     *
     * <p>机台当前物料为空、与候选物料相同、胎胚不匹配或模具无交集时均返回 false，
     * 与换活字块主链 {@code canChangeLetterBlock} 的既有口径保持一致。</p>
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @param sku 候选SKU
     * @return true-同胎胚且同模具，允许按换活字块处理；false-不满足换活字块关系
     */
    public static boolean isSameEmbryoAndSameMould(LhScheduleContext context,
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
        String machineEmbryoCode = normalizeCompareToken(resolveMachineEmbryoCode(context, machine));
        String skuEmbryoCode = normalizeCompareToken(sku.getEmbryoCode());
        String machineEmbryoDesc = normalizeCompareToken(resolveMachineEmbryoDesc(context, machine));
        String skuEmbryoDesc = normalizeCompareToken(resolveSkuEmbryoDesc(context, sku));
        // 胎胚编码或胎胚描述命中其一即视为同胎胚。
        boolean sameCarcass = (StringUtils.isNotEmpty(machineEmbryoCode)
                && StringUtils.equals(machineEmbryoCode, skuEmbryoCode))
                || (StringUtils.isNotEmpty(machineEmbryoDesc)
                && StringUtils.equals(machineEmbryoDesc, skuEmbryoDesc));
        if (!sameCarcass) {
            return false;
        }
        Set<String> machineMouldCodeSet = resolveMouldCodeSet(context, machineMaterialCode);
        Set<String> skuMouldCodeSet = resolveMouldCodeSet(context, sku.getMaterialCode());
        if (CollectionUtils.isEmpty(machineMouldCodeSet) || CollectionUtils.isEmpty(skuMouldCodeSet)) {
            return false;
        }
        // 模具按模具编码集合取交集，命中任一模具即视为同模具。
        for (String mouldCode : machineMouldCodeSet) {
            if (skuMouldCodeSet.contains(mouldCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析机台当前物料的胎胚编码。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @return 胎胚编码，未找到返回null
     */
    private static String resolveMachineEmbryoCode(LhScheduleContext context, MachineScheduleDTO machine) {
        MdmMaterialInfo materialInfo = resolveMachineMaterialInfo(context, machine);
        if (Objects.nonNull(materialInfo) && StringUtils.isNotEmpty(materialInfo.getEmbryoCode())) {
            return materialInfo.getEmbryoCode();
        }
        SkuScheduleDTO currentSku = findContinuousSkuByMaterialCode(context, machine.getCurrentMaterialCode());
        return Objects.isNull(currentSku) ? null : currentSku.getEmbryoCode();
    }

    /**
     * 解析机台当前物料的胎胚描述。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @return 胎胚描述，未找到返回null
     */
    private static String resolveMachineEmbryoDesc(LhScheduleContext context, MachineScheduleDTO machine) {
        MdmMaterialInfo materialInfo = resolveMachineMaterialInfo(context, machine);
        if (Objects.nonNull(materialInfo) && StringUtils.isNotEmpty(materialInfo.getEmbryoDesc())) {
            return materialInfo.getEmbryoDesc();
        }
        SkuScheduleDTO currentSku = findContinuousSkuByMaterialCode(context, machine.getCurrentMaterialCode());
        return Objects.isNull(currentSku) ? null : currentSku.getMainMaterialDesc();
    }

    /**
     * 解析候选SKU的胎胚描述。
     *
     * @param context 排程上下文
     * @param sku 候选SKU
     * @return 胎胚描述，未找到返回null
     */
    private static String resolveSkuEmbryoDesc(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return null;
        }
        MdmMaterialInfo materialInfo = context.getMaterialInfoMap().get(sku.getMaterialCode());
        if (Objects.nonNull(materialInfo) && StringUtils.isNotEmpty(materialInfo.getEmbryoDesc())) {
            return materialInfo.getEmbryoDesc();
        }
        return sku.getMainMaterialDesc();
    }

    /**
     * 解析机台当前物料的主数据信息。
     *
     * @param context 排程上下文
     * @param machine 当前机台
     * @return 物料主数据信息，未找到返回null
     */
    private static MdmMaterialInfo resolveMachineMaterialInfo(LhScheduleContext context,
                                                              MachineScheduleDTO machine) {
        if (Objects.isNull(context) || Objects.isNull(machine)) {
            return null;
        }
        return context.getMaterialInfoMap().get(machine.getCurrentMaterialCode());
    }

    /**
     * 按物料编码在续作SKU列表中查找当前在机SKU。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return 续作SKU，未找到返回null
     */
    private static SkuScheduleDTO findContinuousSkuByMaterialCode(LhScheduleContext context,
                                                                  String materialCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        for (SkuScheduleDTO sku : context.getContinuousSkuList()) {
            if (Objects.nonNull(sku) && StringUtils.equals(materialCode, sku.getMaterialCode())) {
                return sku;
            }
        }
        return null;
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
