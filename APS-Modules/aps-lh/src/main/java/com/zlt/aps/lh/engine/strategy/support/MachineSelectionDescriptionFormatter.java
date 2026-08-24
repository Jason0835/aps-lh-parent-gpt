package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SKU 选机实时快照描述格式化器。
 *
 * <p>只消费正式选机已经冻结的候选顺序、时间计划和软排序指标，不重新匹配机台、
 * 不重新计算比较键，也不会对候选列表执行任何排序。</p>
 *
 * @author APS
 */
public final class MachineSelectionDescriptionFormatter {

    private MachineSelectionDescriptionFormatter() {
    }

    /**
     * 按真实选机候选顺序生成 SKU 选机描述。
     *
     * @param traceSnapshot 选机时点冻结快照
     * @return 多候选按机台换行分隔的描述；没有正式候选时返回空
     */
    public static String format(MachinePriorityTraceSnapshot traceSnapshot) {
        if (Objects.isNull(traceSnapshot)) {
            return null;
        }
        List<MachineScheduleDTO> actualCandidates = traceSnapshot.getOrderedCandidates().stream()
                .filter(Objects::nonNull)
                .filter(candidate -> traceSnapshot.isActualSelectable(candidate.getMachineCode()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(actualCandidates)) {
            return null;
        }
        StringBuilder descriptionBuilder = new StringBuilder(
                Math.max(256, actualCandidates.size() * 192));
        for (MachineScheduleDTO candidate : actualCandidates) {
            if (descriptionBuilder.length() > 0) {
                // 每台候选独占一行，便于结果表直接按行还原真实选机顺序。
                descriptionBuilder.append('\n');
            }
            appendCandidate(descriptionBuilder, traceSnapshot, candidate);
        }
        return descriptionBuilder.toString();
    }

    /**
     * 拼接单台候选机台的冻结指标。
     *
     * @param descriptionBuilder 输出文本
     * @param traceSnapshot 选机时点冻结快照
     * @param candidate 当前正式候选
     */
    private static void appendCandidate(
            StringBuilder descriptionBuilder,
            MachinePriorityTraceSnapshot traceSnapshot,
            MachineScheduleDTO candidate) {
        String machineCode = candidate.getMachineCode();
        MachinePriorityMetricSnapshot metricSnapshot =
                traceSnapshot.resolvePriorityMetricSnapshot(machineCode);
        descriptionBuilder.append(traceSnapshot.resolveDisplayMachineCode(machineCode))
                .append("[收尾时间=")
                .append(formatTime(traceSnapshot.resolvePriorityTraceEndingTime(machineCode)))
                .append(",正式可开产时间=")
                .append(formatTime(traceSnapshot.resolveRealAvailableProductionTime(machineCode)))
                .append(",同胎胚=")
                .append(resolveYesNo(Objects.nonNull(metricSnapshot)
                        && metricSnapshot.getEmbryoMatchScore() == 0))
                .append(",同模壳=")
                .append(resolveYesNo(Objects.nonNull(metricSnapshot)
                        && metricSnapshot.getMouldShellMatchScore() == 0))
                .append(",同规格=")
                .append(resolveYesNo(Objects.nonNull(metricSnapshot)
                        && metricSnapshot.getSpecMatchScore() == 0))
                .append(",胶囊共用性=")
                .append(resolveYesNo(Objects.nonNull(metricSnapshot)
                        && metricSnapshot.getCapsuleScore() == 0))
                .append(",同英寸=")
                .append(resolveYesNo(Objects.nonNull(metricSnapshot)
                        && metricSnapshot.getProSizeMatchScore() == 0))
                .append(",相近英寸=")
                .append(resolveInchDistance(metricSnapshot))
                .append(']');
    }

    private static String formatTime(java.util.Date time) {
        return Objects.isNull(time) ? StringUtils.EMPTY : LhScheduleTimeUtil.formatDateTime(time);
    }

    private static String resolveYesNo(boolean matched) {
        return matched ? "是" : "否";
    }

    private static String resolveInchDistance(MachinePriorityMetricSnapshot metricSnapshot) {
        if (Objects.isNull(metricSnapshot)) {
            return StringUtils.EMPTY;
        }
        return BigDecimal.valueOf(metricSnapshot.getInchDistance())
                .stripTrailingZeros()
                .toPlainString();
    }
}
