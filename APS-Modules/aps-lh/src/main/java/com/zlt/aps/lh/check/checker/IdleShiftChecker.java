package com.zlt.aps.lh.check.checker;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查项1：空班检查（提示级）。
 *
 * <p>校验口径：</p>
 * <ul>
 *   <li>按机台聚合 class1~8 各班计划量，某班计划量合计为 0/空即视为空班；</li>
 *   <li>V 机台已在上下文加载时剔除；行带 maintenance/shutdown/cleaning 开始时间（非空）时该行豁免；</li>
 *   <li>班次9的独立计划不纳入（仅 class1~8）；</li>
 *   <li>连续空班数 &gt; 阈值（1）记一条异常，维度键=机台编码。</li>
 * </ul>
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
@Component
public class IdleShiftChecker implements LhScheduleChecker {

    /** 连续空班阈值：超过该值才记异常（1 表示连续 2 班及以上空班才报） */
    static final int CONTINUOUS_IDLE_THRESHOLD = 1;

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.idleShift";

    /** 异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.idleShift";

    @Override
    public String getCode() {
        return CODE_IDLE_SHIFT;
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
            return abnormalList;
        }
        // 1. 按机台聚合：每个班记录「是否有计划量（含豁免）」
        Map<String, boolean[]> machineShiftPlanFlags = new HashMap<>(16);
        for (LhScheduleResult result : context.getResults()) {
            String machineCode = StringUtils.trimToNull(result.getLhMachineCode());
            if (machineCode == null) {
                continue;
            }
            boolean[] shiftPlanFlags = machineShiftPlanFlags.computeIfAbsent(
                    machineCode, key -> new boolean[SHIFT_SLOT_COUNT]);
            // 行级豁免：带停机/保养/清洗时间的行不参与空班判定
            boolean exempt = this.hasMaintenanceFlag(result);
            for (int shiftIndex = 1; shiftIndex <= SHIFT_SLOT_COUNT; shiftIndex++) {
                Integer planQty = context.getShiftPlanQty(result, shiftIndex);
                if (exempt || (planQty != null && planQty > 0)) {
                    shiftPlanFlags[shiftIndex - 1] = true;
                }
            }
        }
        // 2. 逐机台扫描连续空班段
        for (Map.Entry<String, boolean[]> entry : machineShiftPlanFlags.entrySet()) {
            String machineCode = entry.getKey();
            List<int[]> idleSegments = this.findIdleSegments(entry.getValue());
            for (int[] segment : idleSegments) {
                int idleCount = segment[1] - segment[0] + 1;
                if (idleCount <= CONTINUOUS_IDLE_THRESHOLD) {
                    continue;
                }
                LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
                abnormal.setCheckCode(this.getCode());
                abnormal.setDimensionKey(machineCode);
                abnormal.setMessage(MessageFormat.format(
                        I18nUtil.getMessage(MESSAGE_KEY), machineCode, segment[0], idleCount));
                abnormal.setDetailJson(JSONUtil.toJsonStr(this.buildSegmentDetail(segment)));
                abnormalList.add(abnormal);
            }
        }
        return abnormalList;
    }

    /**
     * 判断行是否带停机/保养/清洗豁免标记（任一非空即豁免）。
     *
     * @param result 排程结果行
     * @return true-豁免
     */
    private boolean hasMaintenanceFlag(LhScheduleResult result) {
        return result.getMaintenanceStartTime() != null
                || result.getShutdownStartTime() != null
                || result.getCleaningStartTime() != null;
    }

    /**
     * 扫描连续空班段。
     *
     * @param shiftPlanFlags 各班是否有计划量的标记数组（下标 0 对应班次1）
     * @return 连续空班段列表，每段为 [起始班次, 结束班次]（班次序号 1-based）
     */
    private List<int[]> findIdleSegments(boolean[] shiftPlanFlags) {
        List<int[]> segments = new ArrayList<>();
        int segmentStart = -1;
        for (int index = 0; index < shiftPlanFlags.length; index++) {
            boolean idle = !shiftPlanFlags[index];
            if (idle && segmentStart < 0) {
                segmentStart = index + 1;
            }
            if (!idle && segmentStart > 0) {
                segments.add(new int[]{segmentStart, index});
                segmentStart = -1;
            }
        }
        // 尾部连续空班
        if (segmentStart > 0) {
            segments.add(new int[]{segmentStart, shiftPlanFlags.length});
        }
        return segments;
    }

    /**
     * 构建空班段明细（落库 detail_json 便于追溯）。
     *
     * @param segment 空班段 [起始班次, 结束班次]
     * @return 明细键值对
     */
    private Map<String, Object> buildSegmentDetail(int[] segment) {
        Map<String, Object> detail = new LinkedHashMap<>(4);
        detail.put("idleStartShift", segment[0]);
        detail.put("idleEndShift", segment[1]);
        detail.put("idleShiftCount", segment[1] - segment[0] + 1);
        return detail;
    }
}
