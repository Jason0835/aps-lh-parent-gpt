package com.zlt.aps.lh.check.checker;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldShellInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查项3：模壳周转检查（提示级）。
 *
 * <p>校验口径：</p>
 * <ul>
 *   <li>数据源为模壳台账（T_MDM_MOULD_SHELL_INFO，工厂维度）；</li>
 *   <li>模壳数量取台账「总数量」TOTAL_QTY（与模壳数量检查同一口径），在机数量取「机台数量」MACHINE_QTY；</li>
 *   <li>差额 = 模壳数量 − 在机数量；差额小于「周转数量」TURNOVER_QTY 时记一条预警（差额为负同样预警）；</li>
 *   <li>周转数量为空或 0 表示未维护周转预留，不参与预警。</li>
 * </ul>
 *
 * @author APS
 * @date 2026/11/02
 */
@Slf4j
@Component
@Order(3)
public class MouldShellTurnoverChecker implements LhScheduleChecker {

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.mouldShellTurnover";

    /** 异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.mouldShellTurnover";

    @Override
    public String getCode() {
        return CODE_MOULD_SHELL_TURNOVER;
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
        if (context.getMouldShellInfoList() == null || context.getMouldShellInfoList().isEmpty()) {
            log.warn("模壳周转检查：未加载到模壳台账，跳过校验, factoryCode={}", context.getFactoryCode());
            return abnormalList;
        }
        for (MdmMouldShellInfo shellInfo : context.getMouldShellInfoList()) {
            if (shellInfo == null) {
                continue;
            }
            int turnoverQty = shellInfo.getTurnoverQty() == null ? 0 : shellInfo.getTurnoverQty();
            // 未维护周转数量时无法判定，跳过
            if (turnoverQty <= 0) {
                continue;
            }
            int totalQty = shellInfo.getTotalQty() == null ? 0 : shellInfo.getTotalQty();
            int machineQty = shellInfo.getMachineQty() == null ? 0 : shellInfo.getMachineQty();
            int gapQty = totalQty - machineQty;
            if (gapQty >= turnoverQty) {
                continue;
            }
            abnormalList.add(this.buildAbnormal(shellInfo, totalQty, machineQty, gapQty, turnoverQty));
        }
        return abnormalList;
    }

    /**
     * 构建异常记录。
     *
     * @param shellInfo   模壳台账
     * @param totalQty    模壳数量（总数量）
     * @param machineQty  在机数量（机台数量）
     * @param gapQty      差额（模壳数量 − 在机数量）
     * @param turnoverQty 周转数量
     * @return 异常记录
     */
    private LhScheduleCheckResult buildAbnormal(MdmMouldShellInfo shellInfo, int totalQty,
                                                int machineQty, int gapQty, int turnoverQty) {
        String shellStandard = StringUtils.trimToEmpty(shellInfo.getMouldSetCode());
        LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
        abnormal.setCheckCode(this.getCode());
        abnormal.setDimensionKey(shellStandard);
        abnormal.setMessage(MessageFormat.format(I18nUtil.getMessage(MESSAGE_KEY),
                shellStandard, totalQty, machineQty, gapQty, turnoverQty));
        Map<String, Object> detail = new LinkedHashMap<>(8);
        detail.put("shellStandard", shellStandard);
        detail.put("totalQty", totalQty);
        detail.put("machineQty", machineQty);
        detail.put("gapQty", gapQty);
        detail.put("turnoverQty", turnoverQty);
        abnormal.setDetailJson(JSONUtil.toJsonStr(detail));
        return abnormal;
    }
}
