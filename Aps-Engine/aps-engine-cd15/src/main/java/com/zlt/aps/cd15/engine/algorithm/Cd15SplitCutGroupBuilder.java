package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15SplitCutGroup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 在当前班次候选池中构建两条钢带的分裁组合。
 */
@Component
public class Cd15SplitCutGroupBuilder {

    /**
     * 按同大卷、同角度、不同钢带和角度最大宽度寻找稳定的第二条候选。
     *
     * @param first 第一条候选
     * @param remainingCandidates 当前班尚未执行的候选
     * @param angleWidthMaxByAngle 角度最大宽度配置
     * @param classField 当前班次字段
     * @return 分裁组合；不存在时返回空
     */
    public Optional<Cd15SplitCutGroup> find(
            Cd15ScheduleCandidate first,
            List<Cd15ScheduleCandidate> remainingCandidates,
            Map<String, BigDecimal> angleWidthMaxByAngle,
            String classField) {
        if (!this.canSplit(first) || remainingCandidates == null) {
            return Optional.empty();
        }
        Map<String, BigDecimal> widthByAngle = angleWidthMaxByAngle == null
                ? Collections.emptyMap() : angleWidthMaxByAngle;
        BigDecimal maxWidth = widthByAngle.get(first.getCuttingAngle().trim());
        if (maxWidth == null || maxWidth.signum() <= 0) {
            return Optional.empty();
        }
        return remainingCandidates.stream()
                .filter(this::canSplit)
                .filter(second -> this.sameScope(first, second))
                .filter(second -> this.sameSupplyClasses(first, second))
                .map(second -> this.group(first, second, classField))
                .filter(group -> group.getCombinedWidth().compareTo(maxWidth) <= 0)
                .findFirst();
    }

    private boolean canSplit(Cd15ScheduleCandidate candidate) {
        return candidate != null
                && !candidate.isContinueFromPreviousShift()
                && !candidate.isNewSpecAdvance()
                && StringUtils.hasText(candidate.getMaterialKey())
                && StringUtils.hasText(candidate.getSteelStripCode())
                && StringUtils.hasText(candidate.getBigRollCode())
                && StringUtils.hasText(candidate.getCuttingAngle())
                && candidate.getCraftWidth() != null
                && candidate.getCraftWidth().signum() > 0;
    }

    private boolean sameScope(Cd15ScheduleCandidate first,
                              Cd15ScheduleCandidate second) {
        boolean rollingPair = first.getRollingRequestedQuantity() != null
                || second.getRollingRequestedQuantity() != null;
        boolean sameExistingGroup = "SPLIT".equals(first.getCutMode())
                && "SPLIT".equals(second.getCutMode())
                && StringUtils.hasText(first.getSplitGroupKey())
                && Objects.equals(first.getSplitGroupKey(), second.getSplitGroupKey());
        return (!rollingPair || sameExistingGroup)
                && !Objects.equals(first.getMaterialKey(), second.getMaterialKey())
                && !Objects.equals(first.getSteelStripCode(), second.getSteelStripCode())
                && Objects.equals(first.getBigRollCode(), second.getBigRollCode())
                && Objects.equals(first.getCuttingAngle(), second.getCuttingAngle());
    }

    /** 分裁要求两条规格的库存可供成型班数一致。 */
    private boolean sameSupplyClasses(
            Cd15ScheduleCandidate first, Cd15ScheduleCandidate second) {
        if (first.getStockSupplyHours() == null
                || second.getStockSupplyHours() == null) {
            return first.getStockSupplyHours() == null
                    && second.getStockSupplyHours() == null;
        }
        return first.getStockSupplyHours().compareTo(
                second.getStockSupplyHours()) == 0;
    }

    private Cd15SplitCutGroup group(Cd15ScheduleCandidate first,
                                    Cd15ScheduleCandidate second,
                                    String classField) {
        String firstKey = first.getMaterialKey();
        String secondKey = second.getMaterialKey();
        String lowKey = firstKey.compareTo(secondKey) <= 0 ? firstKey : secondKey;
        String highKey = firstKey.compareTo(secondKey) <= 0 ? secondKey : firstKey;
        String existingGroupKey = Objects.equals(
                first.getSplitGroupKey(), second.getSplitGroupKey())
                ? first.getSplitGroupKey() : null;
        return Cd15SplitCutGroup.builder()
                .firstCandidate(first)
                .secondCandidate(second)
                .combinedWidth(first.getCraftWidth().add(second.getCraftWidth()))
                .groupKey(StringUtils.hasText(existingGroupKey)
                        ? existingGroupKey
                        : "SPLIT|" + classField + "|" + lowKey + "|" + highKey)
                .build();
    }
}