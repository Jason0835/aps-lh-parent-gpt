package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * 只读解析续作机台对应的SKU，原续作预览和正式分配共用精确优先、降级在后的规则。
 *
 * @author Codex
 * @date 2026/10/27
 */
@Slf4j
public final class ContinuationSkuMatcher {

    /**
     * 禁止实例化无状态匹配器。
     */
    private ContinuationSkuMatcher() {
    }

    /**
     * 在独立集合内匹配续作来源，保持MES顺序且不修改SKU、机台或数量账本。
     *
     * @param context 已归集SKU和可排机台的上下文
     * @param sourceResolver 按机台解析原MES或已确认历史前料身份
     * @return 按实际匹配顺序排列的机台与源SKU映射
     */
    public static Map<String, SkuScheduleDTO> resolve(LhScheduleContext context,
            Function<String, LhMachineOnlineInfo> sourceResolver) {
        Map<String, List<SkuScheduleDTO>> pending = new LinkedHashMap<>();
        context.getStructureSkuMap().values().stream().flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(sku -> StringUtils.isNotBlank(sku.getMaterialCode()))
                .forEach(sku -> pending.computeIfAbsent(StringUtils.trim(sku.getMaterialCode()),
                        key -> new ArrayList<>()).add(sku));
        Map<String, SkuScheduleDTO> templates = new LinkedHashMap<>();
        Map<String, SkuScheduleDTO> matches = new LinkedHashMap<>();
        Set<String> staleMachines = resolveSupersededMachines(context);
        resolvePass(context, sourceResolver, pending, templates, matches, staleMachines, false);
        resolvePass(context, sourceResolver, pending, templates, matches, staleMachines, true);
        return matches;
    }

    /**
     * 同物料状态已有T日在机证据时，较早快照不能再扩成普通续作机台。
     * 仅返回准入排除集合，原MES及装机模具仍保留；没有T日证据时沿用既有回溯。
     *
     * @param context 原始MES快照及窗口T日
     * @return 被同SKU当日在机证据替代的机台集合
     */
    private static Set<String> resolveSupersededMachines(LhScheduleContext context) {
        Set<String> currentSkuKeys = new HashSet<>();
        Set<String> staleMachines = new HashSet<>();
        if (Objects.isNull(context.getScheduleDate())) {
            return staleMachines;
        }
        Date tDay = LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        context.getMachineOnlineInfoMap().values().stream().filter(Objects::nonNull)
                .filter(online -> Objects.nonNull(online.getOnlineDate())
                        && Objects.equals(LhScheduleTimeUtil.clearTime(online.getOnlineDate()), tDay))
                .filter(online -> StringUtils.isNotBlank(online.getMaterialCode()))
                .forEach(online -> currentSkuKeys.add(MonthPlanDateResolver.buildMaterialStatusKey(
                        online.getMaterialCode().trim(), normalizeStatus(online.getProductStatus()))));
        context.getMachineOnlineInfoMap().forEach((machineCode, online) -> {
            if (Objects.nonNull(online) && Objects.nonNull(online.getOnlineDate())
                    && LhScheduleTimeUtil.clearTime(online.getOnlineDate()).before(tDay)
                    && StringUtils.isNotBlank(online.getMaterialCode())
                    && currentSkuKeys.contains(MonthPlanDateResolver.buildMaterialStatusKey(
                    online.getMaterialCode().trim(), normalizeStatus(online.getProductStatus())))) {
                staleMachines.add(machineCode);
            }
        });
        return staleMachines;
    }

    /**
     * 完成一轮身份匹配，所有精确来源先占模板，再允许未知状态沿用原物料降级。
     *
     * @param context 可排机台及MES顺序
     * @param sourceResolver 生产身份解析器
     * @param pending 尚未匹配的独立候选集合
     * @param templates 已命中SKU的源对象
     * @param matches 本次只读匹配结果
     * @param staleMachines 不能由旧MES快照取得普通续作身份的机台
     * @param allowFallback 是否允许按物料降级
     */
    private static void resolvePass(LhScheduleContext context,
            Function<String, LhMachineOnlineInfo> sourceResolver,
            Map<String, List<SkuScheduleDTO>> pending, Map<String, SkuScheduleDTO> templates,
            Map<String, SkuScheduleDTO> matches, Set<String> staleMachines, boolean allowFallback) {
        for (String machineCode : context.getMachineOnlineInfoMap().keySet()) {
            if (StringUtils.isEmpty(machineCode) || !context.getMachineScheduleMap().containsKey(machineCode)
                    || matches.containsKey(machineCode)) {
                continue;
            }
            LhMachineOnlineInfo source = sourceResolver.apply(machineCode);
            if (staleMachines.contains(machineCode)
                    && source == context.getMachineOnlineInfoMap().get(machineCode)) {
                // 历史派生前料已有独立资格，不把它与仅回溯得到的MES身份混为一谈。
                if (!allowFallback) {
                    log.info("旧MES续作身份排除, 工厂={}, 批次={}, T日={}, 机台={}, 物料={}, 状态={}, MES日期={}, "
                                    + "原因=同物料状态已有T日在机记录，旧记录只保留装机事实",
                            context.getFactoryCode(), context.getBatchNo(), context.getScheduleDate(),
                            machineCode, source.getMaterialCode(), source.getProductStatus(), source.getOnlineDate());
                }
                continue;
            }
            if (Objects.isNull(source) || StringUtils.isBlank(source.getMaterialCode())) {
                continue;
            }
            String materialCode = StringUtils.trim(source.getMaterialCode());
            String status = normalizeStatus(source.getProductStatus());
            String exactKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, status);
            SkuScheduleDTO template = templates.get(exactKey);
            if (Objects.nonNull(template)) {
                matches.put(machineCode, template);
                continue;
            }
            List<SkuScheduleDTO> candidates = pending.get(materialCode);
            if (Objects.isNull(candidates) || candidates.isEmpty()) {
                continue;
            }
            SkuScheduleDTO matched = candidates.stream()
                    .filter(sku -> StringUtils.equals(status, StringUtils.trim(sku.getProductStatus())))
                    .findFirst().orElse(null);
            if (Objects.isNull(matched) && allowFallback) {
                matched = candidates.get(0);
            }
            if (Objects.nonNull(matched)) {
                candidates.remove(matched);
                matches.put(machineCode, matched);
                templates.putIfAbsent(MonthPlanDateResolver.buildMaterialStatusKey(
                        matched.getMaterialCode(), matched.getProductStatus()), matched);
            }
        }
    }

    /**
     * 将空MES产品状态按既有续作口径归一为正规状态。
     *
     * @param status 原产品状态
     * @return 去除空白的产品状态，空值返回S
     */
    public static String normalizeStatus(String status) {
        return StringUtils.isBlank(status) ? TrialStatusEnum.FORMAL.getCode() : status.trim();
    }
}
