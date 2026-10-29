package com.zlt.aps.lh.check.checker;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldShellInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检查项6：模壳数量检查（强控），复刻月度计划侧 checkMouldShellLimit 的两阶段算法。
 *
 * <p>阶段划分：</p>
 * <ol>
 *   <li>阶段0 建索引：规格+主花纹 → 模壳标准、模具号 → 模壳标准、模壳标准 → 总数、物料 → SKU模具关系；</li>
 *   <li>阶段1 行解析：优先按行模具号反查模壳标准；模具号为空时回退 SKU 关系表，
 *       取关系的模具号反查或按「规格+主花纹」查标准；</li>
 *   <li>阶段2 非组合校验：同班同单壳的 mouldQty 合计超过台账总数即记异常，
 *       并得到各壳剩余额度（所有台账壳初始化 remain=totalQty）；</li>
 *   <li>阶段3 组合模壳：组合行需求量与成员壳最小剩余额度比较，超出记异常，随后依次扣减成员壳额度。</li>
 * </ol>
 *
 * <p>占用只统计 mouldQty&gt;0 且该班有计划量的行；换活字块不特殊扣减。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
@Component
public class MouldShellCheckChecker implements LhScheduleChecker {

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.mouldShell";

    /** 异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.mouldShellLimit";

    /** 规格与主花纹拼接分隔符 */
    private static final String SPEC_PATTERN_SEPARATOR = "|";

    /** 多值分隔符 */
    private static final String MULTI_VALUE_SEPARATOR = ",";

    @Override
    public String getCode() {
        return CODE_MOULD_SHELL;
    }

    @Override
    public String getNameKey() {
        return NAME_KEY;
    }

    @Override
    public boolean isHardControl() {
        return true;
    }

    @Override
    public List<LhScheduleCheckResult> check(LhScheduleCheckContext context) {
        List<LhScheduleCheckResult> abnormalList = new ArrayList<>();
        // 无模壳台账时无法判定，跳过避免全量误报
        if (!context.hasResults() || context.getMouldShellInfoList() == null
                || context.getMouldShellInfoList().isEmpty()) {
            return abnormalList;
        }
        // ===== 阶段0：建索引 =====
        Map<String, String> specPatternShellMap = this.buildSpecPatternShellMap(context.getMouldInfoList());
        Map<String, String> mouldCodeShellMap = this.buildMouldCodeShellMap(context.getMouldInfoList());
        Map<String, Integer> shellTotalQtyMap = this.buildShellTotalQtyMap(context.getMouldShellInfoList());
        Map<String, List<MdmSkuMouldRel>> materialRelMap = this.buildMaterialRelMap(context.getSkuMouldRelList());

        // 单壳占用（壳 → 班次 → 数量）与组合壳占用（组合键 → 班次 → 数量）
        Map<String, Map<Integer, Integer>> singleUsageMap = new LinkedHashMap<>(16);
        Map<String, Map<Integer, Integer>> comboUsageMap = new LinkedHashMap<>(16);
        for (LhScheduleResult result : context.getResults()) {
            Set<String> shells = this.resolveRowShells(result, mouldCodeShellMap, specPatternShellMap, materialRelMap);
            if (shells.isEmpty()) {
                continue;
            }
            Set<Integer> planShiftIndexes = this.resolvePlanShiftIndexes(context, result);
            if (planShiftIndexes.isEmpty()) {
                continue;
            }
            int mouldQty = result.getMouldQty() == null ? 0 : result.getMouldQty();
            if (mouldQty <= 0) {
                continue;
            }
            if (shells.size() == 1) {
                String shell = shells.iterator().next();
                this.accumulateUsage(singleUsageMap, shell, planShiftIndexes, mouldQty);
            } else {
                String comboKey = String.join(MULTI_VALUE_SEPARATOR, shells);
                this.accumulateUsage(comboUsageMap, comboKey, planShiftIndexes, mouldQty);
            }
        }

        // ===== 阶段2：非组合校验 + 剩余额度（所有台账壳初始化 remain=totalQty） =====
        Map<String, Integer> shellRemainMap = new HashMap<>(shellTotalQtyMap.size() * 2);
        for (Map.Entry<String, Integer> entry : shellTotalQtyMap.entrySet()) {
            String shell = entry.getKey();
            int totalQty = entry.getValue() == null ? 0 : entry.getValue();
            Map<Integer, Integer> shiftUsage = singleUsageMap.get(shell);
            int maxUsage = 0;
            if (shiftUsage != null) {
                for (Map.Entry<Integer, Integer> usageEntry : shiftUsage.entrySet()) {
                    int used = usageEntry.getValue();
                    maxUsage = Math.max(maxUsage, used);
                    if (used > totalQty) {
                        abnormalList.add(this.buildAbnormal(
                                shell, usageEntry.getKey(), used, used - totalQty));
                    }
                }
            }
            shellRemainMap.put(shell, Math.max(0, totalQty - maxUsage));
        }

        // ===== 阶段3：组合模壳依次扣减 =====
        for (Map.Entry<String, Map<Integer, Integer>> entry : comboUsageMap.entrySet()) {
            String comboKey = entry.getKey();
            List<String> memberShells = Arrays.asList(comboKey.split(MULTI_VALUE_SEPARATOR));
            // 成员壳未全部建账时无法判定剩余额度，跳过
            boolean allMembersKnown = memberShells.stream().allMatch(shellRemainMap::containsKey);
            if (!allMembersKnown) {
                continue;
            }
            for (Map.Entry<Integer, Integer> usageEntry : entry.getValue().entrySet()) {
                int shiftIndex = usageEntry.getKey();
                int demand = usageEntry.getValue();
                int minRemain = memberShells.stream()
                        .map(shellRemainMap::get)
                        .min(Integer::compareTo)
                        .orElse(0);
                if (demand > minRemain) {
                    abnormalList.add(this.buildAbnormal(comboKey, shiftIndex, demand, demand - minRemain));
                }
                // 依次扣减成员壳剩余额度（下限0）
                for (String shell : memberShells) {
                    shellRemainMap.put(shell, Math.max(0, shellRemainMap.get(shell) - demand));
                }
            }
        }
        return abnormalList;
    }

    /**
     * 构建「规格+主花纹 → 模壳标准」索引（同一键对应多个壳标准时用逗号拼接为组合壳）。
     *
     * @param mouldInfoList 模具台账
     * @return 索引
     */
    private Map<String, String> buildSpecPatternShellMap(List<MdmModelInfo> mouldInfoList) {
        Map<String, Set<String>> tempMap = new HashMap<>(16);
        if (mouldInfoList != null) {
            for (MdmModelInfo mouldInfo : mouldInfoList) {
                String shellStandard = StringUtils.trimToNull(mouldInfo.getShellStandard());
                if (shellStandard == null) {
                    continue;
                }
                String specPatternKey = this.buildSpecPatternKey(
                        mouldInfo.getSpecifications(), mouldInfo.getMainPattern());
                if (specPatternKey == null) {
                    continue;
                }
                tempMap.computeIfAbsent(specPatternKey, key -> new LinkedHashSet<>()).add(shellStandard);
            }
        }
        Map<String, String> specPatternShellMap = new HashMap<>(tempMap.size() * 2);
        for (Map.Entry<String, Set<String>> entry : tempMap.entrySet()) {
            specPatternShellMap.put(entry.getKey(), String.join(MULTI_VALUE_SEPARATOR, entry.getValue()));
        }
        return specPatternShellMap;
    }

    /**
     * 构建「模具号 → 模壳标准」索引。
     *
     * @param mouldInfoList 模具台账
     * @return 索引
     */
    private Map<String, String> buildMouldCodeShellMap(List<MdmModelInfo> mouldInfoList) {
        Map<String, String> mouldCodeShellMap = new HashMap<>(16);
        if (mouldInfoList == null) {
            return mouldCodeShellMap;
        }
        for (MdmModelInfo mouldInfo : mouldInfoList) {
            String mouldCode = StringUtils.trimToNull(mouldInfo.getMouldCode());
            String shellStandard = StringUtils.trimToNull(mouldInfo.getShellStandard());
            if (mouldCode == null || shellStandard == null) {
                continue;
            }
            mouldCodeShellMap.put(mouldCode, shellStandard);
        }
        return mouldCodeShellMap;
    }

    /**
     * 构建「模壳标准 → 总数」索引。
     *
     * @param mouldShellInfoList 模壳台账
     * @return 索引
     */
    private Map<String, Integer> buildShellTotalQtyMap(List<MdmMouldShellInfo> mouldShellInfoList) {
        Map<String, Integer> shellTotalQtyMap = new HashMap<>(16);
        if (mouldShellInfoList == null) {
            return shellTotalQtyMap;
        }
        for (MdmMouldShellInfo shellInfo : mouldShellInfoList) {
            String mouldSetCode = StringUtils.trimToNull(shellInfo.getMouldSetCode());
            if (mouldSetCode == null) {
                continue;
            }
            shellTotalQtyMap.merge(mouldSetCode,
                    shellInfo.getTotalQty() == null ? 0 : shellInfo.getTotalQty(), Integer::sum);
        }
        return shellTotalQtyMap;
    }

    /**
     * 构建「物料编码 → SKU模具关系列表」索引。
     *
     * @param skuMouldRelList SKU模具关系
     * @return 索引
     */
    private Map<String, List<MdmSkuMouldRel>> buildMaterialRelMap(List<MdmSkuMouldRel> skuMouldRelList) {
        Map<String, List<MdmSkuMouldRel>> materialRelMap = new HashMap<>(16);
        if (skuMouldRelList == null) {
            return materialRelMap;
        }
        for (MdmSkuMouldRel rel : skuMouldRelList) {
            String materialCode = StringUtils.trimToNull(rel.getMaterialCode());
            if (materialCode == null) {
                continue;
            }
            materialRelMap.computeIfAbsent(materialCode, key -> new ArrayList<>()).add(rel);
        }
        return materialRelMap;
    }

    /**
     * 解析一行排程结果占用的模壳标准集合。
     *
     * <p>路径A：行模具号（支持逗号多值）反查模具台账；
     * 路径B：行模具号为空（或路径A未解析出任何壳）时，回退 SKU 关系表。</p>
     *
     * @param result 排程结果行
     * @param mouldCodeShellMap 模具号索引
     * @param specPatternShellMap 规格主花纹索引
     * @param materialRelMap 物料关系索引
     * @return 模壳标准集合（空集表示无法判定）
     */
    private Set<String> resolveRowShells(LhScheduleResult result,
                                         Map<String, String> mouldCodeShellMap,
                                         Map<String, String> specPatternShellMap,
                                         Map<String, List<MdmSkuMouldRel>> materialRelMap) {
        Set<String> shells = new LinkedHashSet<>();
        // 路径A：按行模具号反查
        String rowMouldCode = StringUtils.trimToEmpty(result.getMouldCode());
        if (StringUtils.isNotEmpty(rowMouldCode)) {
            for (String singleMouldCode : rowMouldCode.split(MULTI_VALUE_SEPARATOR)) {
                String shell = mouldCodeShellMap.get(StringUtils.trimToEmpty(singleMouldCode));
                if (StringUtils.isNotBlank(shell)) {
                    this.addShells(shells, shell);
                }
            }
        }
        if (!shells.isEmpty()) {
            return shells;
        }
        // 路径B：回退 SKU 关系表
        String materialCode = StringUtils.trimToNull(result.getMaterialCode());
        if (materialCode == null) {
            return shells;
        }
        List<MdmSkuMouldRel> relList = materialRelMap.get(materialCode);
        if (relList == null) {
            return shells;
        }
        for (MdmSkuMouldRel rel : relList) {
            String relMouldCode = StringUtils.trimToNull(rel.getMouldCode());
            if (relMouldCode != null) {
                String shell = mouldCodeShellMap.get(relMouldCode);
                if (StringUtils.isNotBlank(shell)) {
                    this.addShells(shells, shell);
                    continue;
                }
            }
            String specPatternKey = this.buildSpecPatternKey(rel.getSpecifications(), rel.getMainPattern());
            if (specPatternKey == null) {
                specPatternKey = this.buildSpecPatternKey(rel.getSpecifications(), rel.getPattern());
            }
            if (specPatternKey == null) {
                continue;
            }
            String shell = specPatternShellMap.get(specPatternKey);
            if (StringUtils.isNotBlank(shell)) {
                this.addShells(shells, shell);
            }
        }
        return shells;
    }

    /**
     * 拆分并加入模壳标准（索引值可能为逗号拼接的组合壳）。
     *
     * @param shells 目标集合
     * @param shellValue 索引值
     */
    private void addShells(Set<String> shells, String shellValue) {
        for (String shell : shellValue.split(MULTI_VALUE_SEPARATOR)) {
            String trimmedShell = StringUtils.trimToEmpty(shell);
            if (StringUtils.isNotEmpty(trimmedShell)) {
                shells.add(trimmedShell);
            }
        }
    }

    /**
     * 构建「规格+主花纹」键。
     *
     * @param specifications 规格
     * @param mainPattern 主花纹
     * @return 键；任一为空返回 null
     */
    private String buildSpecPatternKey(String specifications, String mainPattern) {
        String trimmedSpec = StringUtils.trimToNull(specifications);
        String trimmedPattern = StringUtils.trimToNull(mainPattern);
        if (trimmedSpec == null || trimmedPattern == null) {
            return null;
        }
        return trimmedSpec + SPEC_PATTERN_SEPARATOR + trimmedPattern;
    }

    /**
     * 解析该行有计划量的班次序号集合。
     *
     * @param context 校验上下文
     * @param result 排程结果行
     * @return 班次序号集合（1~8）
     */
    private Set<Integer> resolvePlanShiftIndexes(LhScheduleCheckContext context, LhScheduleResult result) {
        Set<Integer> planShiftIndexes = new LinkedHashSet<>();
        for (int shiftIndex = 1; shiftIndex <= SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer planQty = context.getShiftPlanQty(result, shiftIndex);
            if (planQty != null && planQty > 0) {
                planShiftIndexes.add(shiftIndex);
            }
        }
        return planShiftIndexes;
    }

    /**
     * 累加占用数量。
     *
     * @param usageMap 占用表（键 → 班次 → 数量）
     * @param usageKey 占用键（单壳标准或组合键）
     * @param planShiftIndexes 有计划量的班次
     * @param mouldQty 占用数量
     */
    private void accumulateUsage(Map<String, Map<Integer, Integer>> usageMap, String usageKey,
                                 Set<Integer> planShiftIndexes, int mouldQty) {
        Map<Integer, Integer> shiftUsage = usageMap.computeIfAbsent(usageKey, key -> new HashMap<>(16));
        for (Integer shiftIndex : planShiftIndexes) {
            shiftUsage.merge(shiftIndex, mouldQty, Integer::sum);
        }
    }

    /**
     * 构建异常记录。
     *
     * @param shellKey 模壳标准（单壳或组合）
     * @param shiftIndex 班次序号
     * @param demand 需求量
     * @param overflow 超出数量
     * @return 异常记录
     */
    private LhScheduleCheckResult buildAbnormal(String shellKey, int shiftIndex,
                                                int demand, int overflow) {
        LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
        abnormal.setCheckCode(this.getCode());
        abnormal.setDimensionKey(shellKey);
        abnormal.setMessage(MessageFormat.format(
                I18nUtil.getMessage(MESSAGE_KEY), shellKey, shiftIndex, demand, overflow));
        Map<String, Object> detail = new LinkedHashMap<>(6);
        detail.put("shellStandard", shellKey);
        detail.put("shiftIndex", shiftIndex);
        detail.put("demandQty", demand);
        detail.put("overflowQty", overflow);
        abnormal.setDetailJson(JSONUtil.toJsonStr(detail));
        return abnormal;
    }
}
