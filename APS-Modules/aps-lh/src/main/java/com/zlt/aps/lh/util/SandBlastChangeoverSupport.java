package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.service.impl.ContinuationSandBlastRecoveryService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 喷砂原模恢复及历史指定接收共用的准备和首检时间来源。
 * 喷砂总窗口已包含换模及首检，原模清洗完成后只承接窗口尾段，不追加普通换模时长。
 *
 * @author APS
 * @date 2026/11/16
 */
public final class SandBlastChangeoverSupport {

    /**
     * 小时换算毫秒数。
     */
    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;

    /**
     * 禁止实例化无状态规则工具。
     */
    private SandBlastChangeoverSupport() {
    }

    /**
     * 按明确恢复来源和目标运行侧取得喷砂首检窗口，普通同料候选及备用模具置换不继承。
     * 来源索引直接按机台读取，配对侧复用现有同源原模校验，不扫描全量模具或排产结果。
     *
     * @param context 本批清洗来源及历史承接作用域
     * @param sku 当前物料状态与恢复来源
     * @param machineCode 本次目标运行侧
     * @return 对应运行侧的固定首检来源；未命中时为空
     */
    public static MachineCleaningWindowDTO resolveInspectionSource(LhScheduleContext context,
            SkuScheduleDTO sku, String machineCode) {
        if (context == null || sku == null) {
            return null;
        }
        if (!ContinuationSandBlastRecoveryService.isRecoveryCandidate(sku)) {
            return HistoricalMouldCleaningTransferSupport.resolveReceiving(context, sku, machineCode);
        }
        List<MachineCleaningWindowDTO> events = context.resolveOnlySandBlastRequeueEvents(sku);
        if (events.isEmpty() || StringUtils.isNotBlank(events.get(0).getDeferredReplacementMouldCode())
                || !ContinuationSandBlastRecoveryService.allowsMachine(context, sku, machineCode)) {
            return null;
        }
        MachineCleaningWindowDTO source = context.getOnlySandBlastContinuationReleaseWindowMap().get(machineCode);
        return source != null && source.isOffMachineCleaning()
                && StringUtils.isBlank(source.getReplacementMouldCode())
                && StringUtils.isBlank(source.getDeferredReplacementMouldCode()) ? source : null;
    }

    /**
     * 原模清洗完成后仅保留喷砂窗口内的固定首检尾段，其他换模保持既有时长。
     * 原机占用、换模时段和额度造成的真实延期仍由公共时间轴处理。
     *
     * @param context 排程上下文
     * @param sku 当前物料状态
     * @param machineCode 目标运行侧
     * @param ordinaryHours 普通换模总时长
     * @return 喷砂首检尾段小时数或普通换模时长
     */
    public static int resolvePreparationHours(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, int ordinaryHours) {
        MachineCleaningWindowDTO source = resolveInspectionSource(context, sku, machineCode);
        return source == null ? ordinaryHours
                : Math.toIntExact((source.getReadyTime().getTime() - source.getCleanEndTime().getTime())
                        / MILLIS_PER_HOUR);
    }
}
