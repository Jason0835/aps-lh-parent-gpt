package com.zlt.aps.lh.component;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 按共用胎胚的初始在机范围约束新增进入K21专供机台。
 * <p>只读取批次初始MES输入及有效SKU集合，预演、正式绑定和回滚均不写入线路归属。</p>
 *
 * @author APS
 * @since 2026/10/18
 */
public final class SharedEmbryoSupplyAdmissionPolicy {

    /**
     * 本次需求限定的专供产线前缀，不扩展到其他专供产线。
     */
    private static final String K21_MACHINE_PREFIX = "K21";

    /**
     * MES在机状态为空时沿用现有续作识别的正规状态口径。
     */
    private static final String NORMAL_PRODUCT_STATUS = "S";

    /**
     * 工具类禁止实例化。
     */
    private SharedEmbryoSupplyAdmissionPolicy() {
    }

    /**
     * 解析配置匹配的K21候选是否违反共用胎胚原在机范围。
     * <p>仅同产品状态的有效生产单元参与判断；同物料多物理机也属于共用。
     * 没有可信原在机或已有K21来源时不增加限制，仍交由原硬约束校验。</p>
     *
     * @param context 本批上下文，MES在机输入与机台初始快照已加载
     * @param machineCode 当前候选机台，可为L/R侧
     * @param sku 当前真实待排候选
     * @param dedicatedMachines 当前SKU所属年月版本的配置专供物理机集合
     * @return 违反范围时返回国际化拒绝详情，否则返回空字符串
     */
    public static String resolveRejectionReason(LhScheduleContext context, String machineCode,
            SkuScheduleDTO sku, Set<String> dedicatedMachines) {
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        if (Objects.isNull(context) || Objects.isNull(sku) || StringUtils.isBlank(sku.getEmbryoCode())
                || !StringUtils.startsWith(physicalMachineCode, K21_MACHINE_PREFIX)
                || !dedicatedMachines.contains(physicalMachineCode)) {
            return StringUtils.EMPTY;
        }
        Set<String> effectiveEmbryoKeys = new LinkedHashSet<>(context.getActiveEmbryoSkuMap()
                .getOrDefault(sku.getEmbryoCode(), Collections.emptyList()));
        // S4.3可行性预评估早于全量SKU索引登记，结构分组与后续索引共同提供真实候选身份。
        Set<String> activeSkuKeys = Stream.concat(context.getAllSkuScheduleDtoMap().values().stream(),
                        context.getStructureSkuMap().values().stream().flatMap(skus -> skus.stream()))
                .filter(activeSku -> isSameStatusSku(activeSku, sku))
                .map(activeSku -> MonthPlanDateResolver.buildMaterialStatusKey(
                        activeSku.getMaterialCode(), activeSku.getProductStatus()))
                .filter(effectiveEmbryoKeys::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 待激活的提前生产候选也属于本次准入对象，但不反向修改有效集合或日计划账本。
        activeSkuKeys.add(MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus()));
        Set<String> originalMachines = context.getMachineOnlineInfoMap().entrySet().stream()
                .filter(entry -> context.getInitialMachineScheduleMap().containsKey(entry.getKey()))
                .filter(entry -> isActiveOnlineSku(entry.getValue(), sku, activeSkuKeys))
                .map(Map.Entry::getKey)
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean sharedEmbryo = activeSkuKeys.size() > 1 || originalMachines.size() > 1;
        if (!sharedEmbryo || originalMachines.isEmpty()
                || originalMachines.stream().anyMatch(code -> code.startsWith(K21_MACHINE_PREFIX))) {
            return StringUtils.EMPTY;
        }
        return MessageFormat.format(I18nUtil.getMessage("ui.lh.schedule.sharedEmbryoK21AdmissionRejected"),
                sku.getEmbryoCode(), sku.getProductStatus(), activeSkuKeys, originalMachines, physicalMachineCode);
    }

    /**
     * 判断有效集合中的SKU是否与候选属于同一胎胚和产品状态。
     *
     * @param activeSku 全量SKU索引中的有效来源
     * @param candidate 当前候选
     * @return 胎胚及产品状态均精确匹配时返回true
     */
    private static boolean isSameStatusSku(SkuScheduleDTO activeSku, SkuScheduleDTO candidate) {
        return Objects.nonNull(activeSku)
                && StringUtils.equals(activeSku.getEmbryoCode(), candidate.getEmbryoCode())
                && StringUtils.equals(StringUtils.trimToEmpty(activeSku.getProductStatus()),
                        StringUtils.trimToEmpty(candidate.getProductStatus()));
    }

    /**
     * 根据初始MES物料及产品状态识别仍有效的关联在机，不读取已被新排产改写的当前物料。
     *
     * @param onlineInfo 本批初始MES在机输入
     * @param candidate 当前候选
     * @param activeSkuKeys 同胎胚同状态的有效SKU复合键集合
     * @return 初始在机属于有效来源时返回true
     */
    private static boolean isActiveOnlineSku(LhMachineOnlineInfo onlineInfo, SkuScheduleDTO candidate,
            Set<String> activeSkuKeys) {
        if (Objects.isNull(onlineInfo) || StringUtils.isBlank(onlineInfo.getMaterialCode())) {
            return false;
        }
        String productStatus = StringUtils.defaultIfBlank(onlineInfo.getProductStatus(), NORMAL_PRODUCT_STATUS).trim();
        return StringUtils.equals(productStatus, StringUtils.trimToEmpty(candidate.getProductStatus()))
                && activeSkuKeys.contains(MonthPlanDateResolver.buildMaterialStatusKey(
                        onlineInfo.getMaterialCode().trim(), productStatus));
    }
}
