package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15SplitCutGroup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * CD15 分裁组合构建器。
 */
@Component
public class Cd15SplitCutGroupBuilder {

    /**
     * 从剩余候选中寻找可与当前候选组成分裁的第二条候选。
     *
     * @param candidate 当前候选
     * @param remainingCandidates 当前 CLASS 下仍未处理的候选池
     * @param angleWidthMaxByAngle 角度最大分裁宽度配置
     * @return 可分裁组合
     */
    public Optional<Cd15SplitCutGroup> findSplitGroup(Cd15ScheduleCandidate candidate,
                                                      List<Cd15ScheduleCandidate> remainingCandidates,
                                                      Map<String, BigDecimal> angleWidthMaxByAngle) {
        if (!this.canTrySplit(candidate, angleWidthMaxByAngle) || remainingCandidates == null) {
            return Optional.empty();
        }
        BigDecimal maxWidth = angleWidthMaxByAngle.get(candidate.getCuttingAngle());
        return remainingCandidates.stream()
                .filter(item -> item != candidate)
                .filter(item -> this.sameSplitScope(candidate, item))
                .map(item -> this.toGroup(candidate, item))
                .filter(group -> group.getCombinedWidth().compareTo(maxWidth) <= 0)
                .findFirst();
    }

    private boolean canTrySplit(Cd15ScheduleCandidate candidate,
                                Map<String, BigDecimal> angleWidthMaxByAngle) {
        if (candidate == null || candidate.getMaterial() == null
                || candidate.getMaterial().isReinforcement()) {
            return false;
        }
        if (!StringUtils.hasText(candidate.getCuttingAngle()) || angleWidthMaxByAngle == null) {
            return false;
        }
        BigDecimal maxWidth = angleWidthMaxByAngle.get(candidate.getCuttingAngle());
        return maxWidth != null && maxWidth.signum() > 0;
    }

    private boolean sameSplitScope(Cd15ScheduleCandidate candidate, Cd15ScheduleCandidate other) {
        return other != null && other.getMaterial() != null
                && !other.getMaterial().isReinforcement()
                && candidate.getClassIndex() == other.getClassIndex()
                && Objects.equals(candidate.getCuttingAngle(), other.getCuttingAngle())
                && Objects.equals(candidate.getBigRollCode(), other.getBigRollCode())
                && !Objects.equals(candidate.getSteelStripCode(), other.getSteelStripCode());
    }

    private Cd15SplitCutGroup toGroup(Cd15ScheduleCandidate first, Cd15ScheduleCandidate second) {
        return Cd15SplitCutGroup.builder()
                .firstCandidate(first)
                .secondCandidate(second)
                .combinedWidth(this.width(first).add(this.width(second)))
                .build();
    }

    private BigDecimal width(Cd15ScheduleCandidate candidate) {
        return candidate.getMaterial().getCraftWidth() == null
                ? BigDecimal.ZERO : candidate.getMaterial().getCraftWidth();
    }
}