package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 当前班次待排规格稳定排序器。
 */
@Component
public class Cd15ScheduleCandidateSorter {

    /**
     * 按缺料、最早缺料时点、库存供应时长、钢带代码和裁断角度排序。
     *
     * @param candidates 待排规格
     * @return 新的有序列表，不修改输入列表
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates) {
        return sort(candidates, Collections.emptyList());
    }

    /**
     * 兼容单一连续生产参照；当前班已有任务时使用该入口。
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates,
                                            Cd15MachineTailState tail) {
        return sort(candidates, tail == null
                ? Collections.emptyList() : Collections.singletonList(tail));
    }

    /**
     * 相同缺料优先级内，对每个候选取全部机台尾状态中可获得的最佳连续等级。
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates,
                                            Collection<Cd15MachineTailState> tails) {
        return this.sort(candidates, tails, null);
    }

    /**
     * 同大卷连续等级优先不变；连续等级相同时，按机台当前角度方向选择下一角度。
     *
     * @param candidates 待排规格
     * @param tails 当前可参考的机台尾状态
     * @param previousDifferentAngle 当前机尾在本机台上的前一个不同角度
     * @return 新的有序列表，不修改输入列表
     */
    public List<Cd15ScheduleCandidate> sort(List<Cd15ScheduleCandidate> candidates,
                                            Collection<Cd15MachineTailState> tails,
                                            String previousDifferentAngle) {
        List<Cd15ScheduleCandidate> result = candidates == null
                ? new ArrayList<>() : new ArrayList<>(candidates);
        AngleDirection angleDirection = this.resolveAngleDirection(tails, previousDifferentAngle);
        result.sort(Comparator
                .comparing(Cd15ScheduleCandidate::isShortageInCurrentShift, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::isContinueFromPreviousShift, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::isNewSpecAdvance, Comparator.reverseOrder())
                .thenComparing(Cd15ScheduleCandidate::getEarliestShortageTime,
                        Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingInt(item -> this.continuityRank(item, tails))
                .thenComparing((first, second) ->
                        this.compareAngleDirection(first, second, angleDirection))
                .thenComparing(Cd15ScheduleCandidate::getBigRollCode,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(Cd15ScheduleCandidate::getCuttingAngle,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(item -> this.value(item.getStockSupplyHours()))
                .thenComparing(Cd15ScheduleCandidate::getSteelStripCode,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(Cd15ScheduleCandidate::getMaterialKey,
                        Comparator.nullsLast(String::compareTo)));
        return result;
    }

    /**
     * 同方向有候选时继续同方向；同方向没有候选时，从最近的反方向角度开始回转。
     */
    private int compareAngleDirection(Cd15ScheduleCandidate first,
                                      Cd15ScheduleCandidate second,
                                      AngleDirection angleDirection) {
        if (angleDirection == null) {
            return 0;
        }
        BigDecimal firstAngle = this.decimal(first == null ? null : first.getCuttingAngle());
        BigDecimal secondAngle = this.decimal(second == null ? null : second.getCuttingAngle());
        if (firstAngle == null || secondAngle == null) {
            return 0;
        }
        int firstStage = this.angleDirectionStage(firstAngle, angleDirection);
        int secondStage = this.angleDirectionStage(secondAngle, angleDirection);
        if (firstStage != secondStage) {
            return Integer.compare(firstStage, secondStage);
        }
        int angleCompare = firstAngle.compareTo(secondAngle);
        boolean ascending = angleDirection.direction > 0;
        if (firstStage > 0) {
            ascending = !ascending;
        }
        return ascending ? angleCompare : -angleCompare;
    }

    /** 同方向角度为第一阶段，越过端点后才进入反方向阶段。 */
    private int angleDirectionStage(BigDecimal candidateAngle,
                                    AngleDirection angleDirection) {
        int compare = candidateAngle.compareTo(angleDirection.currentAngle);
        boolean sameDirection = angleDirection.direction > 0 ? compare >= 0 : compare <= 0;
        return sameDirection ? 0 : 1;
    }

    /** 只有唯一当前机尾且已出现过不同角度时，才形成明确的增减方向。 */
    private AngleDirection resolveAngleDirection(Collection<Cd15MachineTailState> tails,
                                                 String previousDifferentAngle) {
        if (tails == null || tails.size() != 1) {
            return null;
        }
        Cd15MachineTailState tail = tails.iterator().next();
        BigDecimal currentAngle = this.decimal(tail == null ? null : tail.getCuttingAngle());
        BigDecimal previousAngle = this.decimal(previousDifferentAngle);
        if (currentAngle == null || previousAngle == null) {
            return null;
        }
        int direction = currentAngle.compareTo(previousAngle);
        return direction == 0 ? null : new AngleDirection(currentAngle, direction);
    }

    private BigDecimal decimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** 候选在任意一台机台上可连续生产时，采用其中最优的连续等级。 */
    private int continuityRank(Cd15ScheduleCandidate item,
                               Collection<Cd15MachineTailState> tails) {
        if (item == null || tails == null || tails.isEmpty()) {
            return 5;
        }
        return tails.stream()
                .filter(Objects::nonNull)
                .mapToInt(tail -> continuityRank(item, tail))
                .min()
                .orElse(5);
    }

    private int continuityRank(Cd15ScheduleCandidate item, Cd15MachineTailState tail) {
        boolean sameSpec = StringUtils.hasText(tail.getMaterialKey())
                && StringUtils.hasText(item.getMaterialKey())
                ? Objects.equals(tail.getMaterialKey(), item.getMaterialKey())
                : Objects.equals(tail.getSteelStripCode(), item.getSteelStripCode())
                        && Objects.equals(tail.getCuttingAngle(), item.getCuttingAngle());
        boolean sameRoll = Objects.equals(tail.getBigRollCode(), item.getBigRollCode());
        boolean sameAngle = Objects.equals(tail.getCuttingAngle(), item.getCuttingAngle());
        if (sameSpec && sameRoll) {
            return 0;
        }
        if (sameSpec) {
            return 1;
        }
        if (sameRoll && sameAngle) {
            return 2;
        }
        if (sameRoll) {
            return 3;
        }
        return sameAngle ? 4 : 5;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.valueOf(Long.MAX_VALUE) : value;
    }

    /** 当前角度和已形成的增减方向。 */
    private static final class AngleDirection {
        private final BigDecimal currentAngle;
        private final int direction;

        private AngleDirection(BigDecimal currentAngle, int direction) {
            this.currentAngle = currentAngle;
            this.direction = direction;
        }
    }
}
