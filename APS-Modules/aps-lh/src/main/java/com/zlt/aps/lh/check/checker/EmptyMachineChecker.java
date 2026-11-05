package com.zlt.aps.lh.check.checker;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.util.LhVirtualMachineUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 检查项2：机台全空检查（提示级）。
 *
 * <p>校验口径：</p>
 * <ul>
 *   <li>机台范围取硫化机台配置表（T_LH_MACHINE_INFO）的启用机台，按机台顺序遍历；</li>
 *   <li>单控机台 L/R 归并为同一物理机台（K1501L/K1501R → K1501），同一物理机台只报一条；</li>
 *   <li>排程结果中该物理机台任一班（class1~8）有计划量，或行上带停机/保养/清洗标记，即视为未全空；</li>
 *   <li>虚拟机台（V 前缀）不参与；机台配置或排程结果为空时跳过，避免全量误报。</li>
 * </ul>
 *
 * @author APS
 * @date 2026/11/02
 */
@Slf4j
@Component
@Order(2)
public class EmptyMachineChecker implements LhScheduleChecker {

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.emptyMachine";

    /** 异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.emptyMachine";

    @Override
    public String getCode() {
        return CODE_EMPTY_MACHINE;
    }

    @Override
    public String getNameKey() {
        return NAME_KEY;
    }

    @Override
    public boolean isHardControl() {
        return false;
    }

    @Override
    public List<LhScheduleCheckResult> check(LhScheduleCheckContext context) {
        List<LhScheduleCheckResult> abnormalList = new ArrayList<>();
        if (!context.hasResults()) {
            log.warn("机台全空检查：排程结果为空，跳过校验, factoryCode={}", context.getFactoryCode());
            return abnormalList;
        }
        if (context.getMachineInfoList() == null || context.getMachineInfoList().isEmpty()) {
            log.warn("机台全空检查：未加载到启用的硫化机台配置，跳过校验, factoryCode={}",
                    context.getFactoryCode());
            return abnormalList;
        }
        // 1. 汇总已排（或豁免）的物理机台集合
        Set<String> coveredPhysicalCodeSet = this.resolveCoveredPhysicalCodes(context);
        // 2. 遍历配置机台：同一物理机台只报一次
        Set<String> reportedPhysicalCodeSet = new HashSet<>(16);
        for (LhMachineInfo machineInfo : context.getMachineInfoList()) {
            if (machineInfo == null) {
                continue;
            }
            String machineCode = StringUtils.trimToNull(machineInfo.getMachineCode());
            if (machineCode == null) {
                continue;
            }
            // 虚拟机台不在机台主数据中，不参与全空判定
            if (LhVirtualMachineUtil.isVirtualMachine(machineCode)) {
                continue;
            }
            String physicalMachineCode = this.resolvePhysicalMachineCode(machineCode);
            if (physicalMachineCode == null || !reportedPhysicalCodeSet.add(physicalMachineCode)) {
                continue;
            }
            if (coveredPhysicalCodeSet.contains(physicalMachineCode)) {
                continue;
            }
            abnormalList.add(this.buildAbnormal(machineCode, machineInfo));
        }
        return abnormalList;
    }

    /**
     * 汇总已排或无（停机/保养/清洗）的物理机台编码集合。
     *
     * @param context 校验上下文
     * @return 物理机台编码集合（大写、去空格）
     */
    private Set<String> resolveCoveredPhysicalCodes(LhScheduleCheckContext context) {
        Set<String> coveredPhysicalCodeSet = new LinkedHashSet<>(64);
        for (LhScheduleResult result : context.getResults()) {
            if (result == null) {
                continue;
            }
            String machineCode = StringUtils.trimToNull(result.getLhMachineCode());
            if (machineCode == null || LhVirtualMachineUtil.isVirtualMachine(machineCode)) {
                continue;
            }
            if (!this.hasPlanQtyOrExemptMark(context, result)) {
                continue;
            }
            String physicalMachineCode = this.resolvePhysicalMachineCode(machineCode);
            if (physicalMachineCode != null) {
                coveredPhysicalCodeSet.add(physicalMachineCode);
            }
        }
        return coveredPhysicalCodeSet;
    }

    /**
     * 判断行是否有排产量或停机/保养/清洗豁免标记。
     *
     * @param context 校验上下文
     * @param result  排程结果行
     * @return true-有排产量或带豁免标记
     */
    private boolean hasPlanQtyOrExemptMark(LhScheduleCheckContext context, LhScheduleResult result) {
        if (result.getMaintenanceStartTime() != null
                || result.getShutdownStartTime() != null
                || result.getCleaningStartTime() != null) {
            return true;
        }
        for (int shiftIndex = 1; shiftIndex <= SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer planQty = context.getShiftPlanQty(result, shiftIndex);
            if (planQty != null && planQty > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析物理机台编码（单控 L/R 归并、统一大写去空格）。
     *
     * @param machineCode 机台编码
     * @return 物理机台编码；无法解析返回 null
     */
    private String resolvePhysicalMachineCode(String machineCode) {
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                StringUtils.trimToEmpty(machineCode));
        if (StringUtils.isBlank(physicalMachineCode)) {
            return null;
        }
        return physicalMachineCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 构建异常记录。
     *
     * @param machineCode 配置机台编码
     * @param machineInfo 机台配置
     * @return 异常记录
     */
    private LhScheduleCheckResult buildAbnormal(String machineCode, LhMachineInfo machineInfo) {
        LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
        abnormal.setCheckCode(this.getCode());
        abnormal.setDimensionKey(machineCode);
        abnormal.setMessage(MessageFormat.format(
                I18nUtil.getMessage(MESSAGE_KEY), machineCode));
        Map<String, Object> detail = new LinkedHashMap<>(4);
        detail.put("machineCode", machineCode);
        detail.put("machineName", machineInfo.getMachineName());
        detail.put("machineOrder", machineInfo.getMachineOrder());
        abnormal.setDetailJson(JSONUtil.toJsonStr(detail));
        return abnormal;
    }
}
