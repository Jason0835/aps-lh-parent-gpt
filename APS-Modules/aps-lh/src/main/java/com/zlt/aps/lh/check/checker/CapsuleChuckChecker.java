package com.zlt.aps.lh.check.checker;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmCapsuleChuck;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检查项7：胶囊卡盘数量检查（强控），复刻月度计划侧 checkCapsuleChuckLimit。
 *
 * <p>校验口径：</p>
 * <ul>
 *   <li>维度互斥：卡盘台账 specifications（规格/结构）非空时按结构维度匹配行的 structureName；
 *       否则按 proSize（英寸）维度匹配物料主数据的 proSize；</li>
 *   <li>卡盘总数 = internalQty + newChuckQty；</li>
 *   <li>占用 = 该维度该班开动机台数 × 每台卡盘数（2）；</li>
 *   <li>占用超过总数记异常。</li>
 * </ul>
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
@Component
public class CapsuleChuckChecker implements LhScheduleChecker {

    /** 每台机台占用卡盘数 */
    static final int CHUCK_PER_MACHINE = 2;

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.capsuleChuck";

    /** 异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.capsuleChuckLimit";

    /** 多值分隔符 */
    private static final String MULTI_VALUE_SEPARATOR = ",";

    @Override
    public String getCode() {
        return CODE_CAPSULE_CHUCK;
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
        // 无卡盘台账时无法判定，跳过避免全量误报
        if (!context.hasResults() || context.getCapsuleChuckList() == null
                || context.getCapsuleChuckList().isEmpty()) {
            return abnormalList;
        }
        // 物料编码 → 英寸（proSize）
        Map<String, String> materialProSizeMap = this.buildMaterialProSizeMap(context.getMaterialInfoList());
        for (MdmCapsuleChuck capsuleChuck : context.getCapsuleChuckList()) {
            String specifications = StringUtils.trimToEmpty(capsuleChuck.getSpecifications());
            String proSize = StringUtils.trimToEmpty(capsuleChuck.getProSize());
            // 维度互斥：规格非空优先走结构维度，否则走英寸维度
            if (StringUtils.isNotEmpty(specifications)) {
                abnormalList.addAll(this.checkDimension(context, capsuleChuck, specifications,
                        this.buildStructureMachineMatcher(specifications)));
            } else if (StringUtils.isNotEmpty(proSize)) {
                abnormalList.addAll(this.checkDimension(context, capsuleChuck, proSize,
                        this.buildProSizeMachineMatcher(proSize, materialProSizeMap)));
            }
        }
        return abnormalList;
    }

    /**
     * 按指定维度校验某个卡盘台账的占用是否超限。
     *
     * @param context 校验上下文
     * @param capsuleChuck 卡盘台账
     * @param dimensionKey 维度键（规格值或英寸值）
     * @param machineMatcher 该维度下命中结果的判定器
     * @return 异常记录列表
     */
    private List<LhScheduleCheckResult> checkDimension(LhScheduleCheckContext context,
                                                       MdmCapsuleChuck capsuleChuck,
                                                       String dimensionKey,
                                                       java.util.function.Predicate<LhScheduleResult> machineMatcher) {
        List<LhScheduleCheckResult> abnormalList = new ArrayList<>();
        int totalQty = (capsuleChuck.getInternalQty() == null ? 0 : capsuleChuck.getInternalQty())
                + (capsuleChuck.getNewChuckQty() == null ? 0 : capsuleChuck.getNewChuckQty());
        for (int shiftIndex = 1; shiftIndex <= SHIFT_SLOT_COUNT; shiftIndex++) {
            // 统计该班开动机台数（机台去重）
            Set<String> machineCodes = new HashSet<>(16);
            for (LhScheduleResult result : context.getResults()) {
                Integer planQty = context.getShiftPlanQty(result, shiftIndex);
                if (planQty == null || planQty <= 0) {
                    continue;
                }
                if (!machineMatcher.test(result)) {
                    continue;
                }
                String machineCode = StringUtils.trimToNull(result.getLhMachineCode());
                if (machineCode != null) {
                    machineCodes.add(machineCode);
                }
            }
            int usageQty = machineCodes.size() * CHUCK_PER_MACHINE;
            if (usageQty <= totalQty) {
                continue;
            }
            LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
            abnormal.setCheckCode(this.getCode());
            abnormal.setDimensionKey(dimensionKey);
            abnormal.setMessage(MessageFormat.format(
                    I18nUtil.getMessage(MESSAGE_KEY), dimensionKey, shiftIndex, usageQty, usageQty - totalQty));
            Map<String, Object> detail = new LinkedHashMap<>(8);
            detail.put("dimensionKey", dimensionKey);
            detail.put("shiftIndex", shiftIndex);
            detail.put("machineCodes", machineCodes);
            detail.put("usageQty", usageQty);
            detail.put("totalQty", totalQty);
            abnormal.setDetailJson(JSONUtil.toJsonStr(detail));
            abnormalList.add(abnormal);
        }
        return abnormalList;
    }

    /**
     * 构建结构维度判定器：行的 structureName 命中规格值集合。
     *
     * @param specifications 台账规格（逗号分隔）
     * @return 判定器
     */
    private java.util.function.Predicate<LhScheduleResult> buildStructureMachineMatcher(String specifications) {
        Set<String> structureSet = new HashSet<>(Arrays.asList(specifications.split(MULTI_VALUE_SEPARATOR)));
        return result -> structureSet.contains(StringUtils.trimToEmpty(result.getStructureName()));
    }

    /**
     * 构建英寸维度判定器：行的物料主数据 proSize 命中英寸值集合。
     *
     * @param proSize 台账英寸（逗号分隔）
     * @param materialProSizeMap 物料编码 → 英寸
     * @return 判定器
     */
    private java.util.function.Predicate<LhScheduleResult> buildProSizeMachineMatcher(
            String proSize, Map<String, String> materialProSizeMap) {
        Set<String> proSizeSet = new HashSet<>(Arrays.asList(proSize.split(MULTI_VALUE_SEPARATOR)));
        return result -> {
            String materialProSize = materialProSizeMap.get(StringUtils.trimToEmpty(result.getMaterialCode()));
            return materialProSize != null && proSizeSet.contains(materialProSize);
        };
    }

    /**
     * 构建「物料编码 → 英寸」索引。
     *
     * @param materialInfoList 物料主数据
     * @return 索引
     */
    private Map<String, String> buildMaterialProSizeMap(List<MdmMaterialInfo> materialInfoList) {
        Map<String, String> materialProSizeMap = new HashMap<>(16);
        if (materialInfoList == null) {
            return materialProSizeMap;
        }
        for (MdmMaterialInfo materialInfo : materialInfoList) {
            String materialCode = StringUtils.trimToNull(materialInfo.getMaterialCode());
            String proSize = StringUtils.trimToNull(materialInfo.getProSize());
            if (materialCode == null || proSize == null) {
                continue;
            }
            materialProSizeMap.put(materialCode, proSize);
        }
        return materialProSizeMap;
    }
}
