package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 当前班次待排规格稳定排序器。
 */
@Component
public class Cd15ScheduleCandidateSorter {

    private static final int EXACT_ROUTE_GROUP_LIMIT = 12;

    /**
     * 按本班缺料分层；非缺料候选优先保持大卷连续并减少换角。
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
     * 本班缺料保持紧急程度优先；非缺料候选按不可拆分大卷路线选择下一角度。
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
        Map<Cd15ScheduleCandidate, Integer> angleRouteRanks =
                this.buildAngleRouteRanks(result, tails, angleDirection);
        result.sort((first, second) -> this.compareCandidate(
                first, second, tails, angleDirection, angleRouteRanks));
        return result;
    }

    /**
     * 本班缺料时保持紧急程度优先；本班不缺料时先保持大卷连续并选择换角次数更少的路线。
     */
    private int compareCandidate(Cd15ScheduleCandidate first,
                                 Cd15ScheduleCandidate second,
                                 Collection<Cd15MachineTailState> tails,
                                 AngleDirection angleDirection,
                                 Map<Cd15ScheduleCandidate, Integer> angleRouteRanks) {
        int compare = Boolean.compare(
                second.isShortageInCurrentShift(), first.isShortageInCurrentShift());
        if (compare != 0) {
            return compare;
        }
        compare = Boolean.compare(
                second.isContinueFromPreviousShift(), first.isContinueFromPreviousShift());
        if (compare != 0) {
            return compare;
        }
        compare = Boolean.compare(second.isNewSpecAdvance(), first.isNewSpecAdvance());
        if (compare != 0) {
            return compare;
        }
        if (first.isShortageInCurrentShift()) {
            compare = this.compareShortageTime(first, second);
            if (compare != 0) {
                return compare;
            }
        }
        compare = Integer.compare(
                this.continuityRank(first, tails), this.continuityRank(second, tails));
        if (compare != 0) {
            return compare;
        }
        if (!first.isShortageInCurrentShift()) {
            compare = Integer.compare(
                    angleRouteRanks.getOrDefault(first, Integer.MAX_VALUE),
                    angleRouteRanks.getOrDefault(second, Integer.MAX_VALUE));
            if (compare != 0) {
                return compare;
            }
        }
        compare = this.compareAngleDirection(first, second, angleDirection);
        if (compare != 0) {
            return compare;
        }
        compare = this.compareAngle(first, second);
        if (compare != 0) {
            return compare;
        }
        compare = Comparator.nullsLast(String::compareTo).compare(
                first.getBigRollCode(), second.getBigRollCode());
        if (compare != 0) {
            return compare;
        }
        if (!first.isShortageInCurrentShift()) {
            compare = this.compareShortageTime(first, second);
            if (compare != 0) {
                return compare;
            }
        }
        compare = this.value(first.getStockSupplyHours())
                .compareTo(this.value(second.getStockSupplyHours()));
        if (compare != 0) {
            return compare;
        }
        compare = Comparator.nullsLast(String::compareTo).compare(
                first.getSteelStripCode(), second.getSteelStripCode());
        if (compare != 0) {
            return compare;
        }
        return Comparator.nullsLast(String::compareTo).compare(
                first.getMaterialKey(), second.getMaterialKey());
    }

    private int compareShortageTime(Cd15ScheduleCandidate first,
                                    Cd15ScheduleCandidate second) {
        return Comparator.nullsLast(LocalDateTime::compareTo).compare(
                first.getEarliestShortageTime(), second.getEarliestShortageTime());
    }

    /** 数值角度用于无既定方向时稳定选择边界角度，非数字值最后按原文本兜底。 */
    private int compareAngle(Cd15ScheduleCandidate first,
                             Cd15ScheduleCandidate second) {
        BigDecimal firstAngle = this.decimal(first == null ? null : first.getCuttingAngle());
        BigDecimal secondAngle = this.decimal(second == null ? null : second.getCuttingAngle());
        int compare = Comparator.nullsLast(BigDecimal::compareTo).compare(firstAngle, secondAngle);
        if (compare != 0) {
            return compare;
        }
        return Comparator.nullsLast(String::compareTo).compare(
                first == null ? null : first.getCuttingAngle(),
                second == null ? null : second.getCuttingAngle());
    }

    /**
     * 非缺料候选按续作和新增规格层级分别规划，避免角度路线跨越既有特殊优先级。
     */
    private Map<Cd15ScheduleCandidate, Integer> buildAngleRouteRanks(
            List<Cd15ScheduleCandidate> candidates,
            Collection<Cd15MachineTailState> tails,
            AngleDirection angleDirection) {
        Map<Cd15ScheduleCandidate, Integer> result = new IdentityHashMap<>();
        Map<String, List<Cd15ScheduleCandidate>> candidatesByPriority = new LinkedHashMap<>();
        candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> !candidate.isShortageInCurrentShift())
                .forEach(candidate -> {
                    result.put(candidate, 2);
                    String priorityKey = candidate.isContinueFromPreviousShift() + "|"
                            + candidate.isNewSpecAdvance();
                    candidatesByPriority.computeIfAbsent(
                            priorityKey, key -> new ArrayList<>()).add(candidate);
                });
        candidatesByPriority.values().forEach(priorityCandidates -> {
            RouteChoice choice = this.chooseRouteStart(
                    priorityCandidates, tails, angleDirection);
            if (choice == null) {
                return;
            }
            priorityCandidates.forEach(candidate -> {
                if (!Objects.equals(choice.groupKey, candidate.getBigRollCode())) {
                    return;
                }
                BigDecimal candidateAngle = this.decimal(candidate.getCuttingAngle());
                result.put(candidate, candidateAngle != null
                        && candidateAngle.compareTo(choice.startAngle) == 0 ? 0 : 1);
            });
        });
        return result;
    }

    /** 以大卷为不可拆分路线段，选择剩余路线换角次数最少的起始大卷和角度。 */
    private RouteChoice chooseRouteStart(List<Cd15ScheduleCandidate> candidates,
                                         Collection<Cd15MachineTailState> tails,
                                         AngleDirection angleDirection) {
        Map<String, AngleGroup> groupByRoll = new LinkedHashMap<>();
        candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> StringUtils.hasText(candidate.getBigRollCode()))
                .forEach(candidate -> {
                    BigDecimal angle = this.decimal(candidate.getCuttingAngle());
                    if (angle != null) {
                        groupByRoll.computeIfAbsent(
                                candidate.getBigRollCode(), AngleGroup::new).add(angle);
                    }
                });
        List<AngleGroup> groups = new ArrayList<>(groupByRoll.values());
        if (groups.isEmpty()) {
            return null;
        }
        Cd15MachineTailState currentTail = tails != null && tails.size() == 1
                ? tails.iterator().next() : null;
        BigDecimal currentAngle = this.decimal(
                currentTail == null ? null : currentTail.getCuttingAngle());
        String requiredGroupKey = currentTail != null
                && groupByRoll.containsKey(currentTail.getBigRollCode())
                ? currentTail.getBigRollCode() : null;
        Map<String, Integer> memo = new HashMap<>();
        RouteChoice best = null;
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            AngleGroup group = groups.get(groupIndex);
            if (requiredGroupKey != null && !requiredGroupKey.equals(group.groupKey)) {
                continue;
            }
            for (RouteOrientation orientation : group.orientations()) {
                int remainingChanges = groups.size() <= EXACT_ROUTE_GROUP_LIMIT
                        ? this.minimumRemainingChanges(
                                groups, 1 << groupIndex, orientation.endAngle, memo)
                        : this.greedyRemainingChanges(
                                groups, groupIndex, orientation.endAngle);
                int routeChanges = this.transitionCost(currentAngle, orientation.startAngle)
                        + remainingChanges;
                RouteChoice current = new RouteChoice(
                        group.groupKey, orientation.startAngle, routeChanges,
                        this.directionStage(orientation.startAngle, angleDirection),
                        this.angleDistance(currentAngle, orientation.startAngle));
                if (best == null || current.compareTo(best) < 0) {
                    best = current;
                }
            }
        }
        return best;
    }

    /** 精确计算当前大卷路线之后的最少跨大卷换角次数。 */
    private int minimumRemainingChanges(List<AngleGroup> groups,
                                        int selectedMask,
                                        BigDecimal currentAngle,
                                        Map<String, Integer> memo) {
        if (selectedMask == (1 << groups.size()) - 1) {
            return 0;
        }
        String memoKey = selectedMask + "|" + currentAngle.stripTrailingZeros().toPlainString();
        Integer cached = memo.get(memoKey);
        if (cached != null) {
            return cached;
        }
        int best = Integer.MAX_VALUE;
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            if ((selectedMask & (1 << groupIndex)) != 0) {
                continue;
            }
            for (RouteOrientation orientation : groups.get(groupIndex).orientations()) {
                int remaining = this.minimumRemainingChanges(
                        groups,
                        selectedMask | (1 << groupIndex),
                        orientation.endAngle,
                        memo);
                best = Math.min(best,
                        this.transitionCost(currentAngle, orientation.startAngle) + remaining);
            }
        }
        memo.put(memoKey, best);
        return best;
    }

    /** 大卷组过多时，按相同角度和角度距离稳定估算剩余路线。 */
    private int greedyRemainingChanges(List<AngleGroup> groups,
                                       int selectedGroupIndex,
                                       BigDecimal currentAngle) {
        boolean[] selected = new boolean[groups.size()];
        selected[selectedGroupIndex] = true;
        int selectedCount = 1;
        int changes = 0;
        BigDecimal routeAngle = currentAngle;
        while (selectedCount < groups.size()) {
            int bestGroupIndex = -1;
            RouteOrientation bestOrientation = null;
            int bestTransition = Integer.MAX_VALUE;
            BigDecimal bestDistance = null;
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                if (selected[groupIndex]) {
                    continue;
                }
                for (RouteOrientation orientation : groups.get(groupIndex).orientations()) {
                    int transition = this.transitionCost(routeAngle, orientation.startAngle);
                    BigDecimal distance = this.angleDistance(routeAngle, orientation.startAngle);
                    if (transition < bestTransition
                            || transition == bestTransition
                            && (bestDistance == null || distance.compareTo(bestDistance) < 0)) {
                        bestGroupIndex = groupIndex;
                        bestOrientation = orientation;
                        bestTransition = transition;
                        bestDistance = distance;
                    }
                }
            }
            changes += bestTransition;
            selected[bestGroupIndex] = true;
            selectedCount++;
            routeAngle = bestOrientation.endAngle;
        }
        return changes;
    }

    private int transitionCost(BigDecimal currentAngle, BigDecimal nextAngle) {
        return currentAngle == null || currentAngle.compareTo(nextAngle) == 0 ? 0 : 1;
    }

    private int directionStage(BigDecimal angle, AngleDirection angleDirection) {
        return angleDirection == null ? 0 : this.angleDirectionStage(angle, angleDirection);
    }

    private BigDecimal angleDistance(BigDecimal first, BigDecimal second) {
        return first == null ? second.abs() : first.subtract(second).abs();
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
        boolean sameAngle = StringUtils.hasText(tail.getCuttingAngle())
                && StringUtils.hasText(item.getCuttingAngle())
                && Objects.equals(tail.getCuttingAngle(), item.getCuttingAngle());
        if (sameSpec && sameRoll) {
            return 0;
        }
        if (sameRoll && sameAngle) {
            return 1;
        }
        if (sameRoll) {
            return 2;
        }
        if (sameAngle) {
            return 3;
        }
        return sameSpec ? 4 : 5;
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

    /** 同一业务优先级下的大卷角度边界。 */
    private static final class AngleGroup {
        private final String groupKey;
        private BigDecimal minimumAngle;
        private BigDecimal maximumAngle;

        private AngleGroup(String groupKey) {
            this.groupKey = groupKey;
        }

        private void add(BigDecimal angle) {
            if (this.minimumAngle == null || angle.compareTo(this.minimumAngle) < 0) {
                this.minimumAngle = angle;
            }
            if (this.maximumAngle == null || angle.compareTo(this.maximumAngle) > 0) {
                this.maximumAngle = angle;
            }
        }

        private List<RouteOrientation> orientations() {
            if (this.minimumAngle.compareTo(this.maximumAngle) == 0) {
                return Collections.singletonList(
                        new RouteOrientation(this.minimumAngle, this.maximumAngle));
            }
            return Arrays.asList(
                    new RouteOrientation(this.minimumAngle, this.maximumAngle),
                    new RouteOrientation(this.maximumAngle, this.minimumAngle));
        }
    }

    private static final class RouteOrientation {
        private final BigDecimal startAngle;
        private final BigDecimal endAngle;

        private RouteOrientation(BigDecimal startAngle, BigDecimal endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }
    }

    /** 路线换角次数优先，其次保持既定方向并选择最近的起始角度。 */
    private static final class RouteChoice implements Comparable<RouteChoice> {
        private final String groupKey;
        private final BigDecimal startAngle;
        private final int routeChanges;
        private final int directionStage;
        private final BigDecimal distance;

        private RouteChoice(String groupKey,
                            BigDecimal startAngle,
                            int routeChanges,
                            int directionStage,
                            BigDecimal distance) {
            this.groupKey = groupKey;
            this.startAngle = startAngle;
            this.routeChanges = routeChanges;
            this.directionStage = directionStage;
            this.distance = distance;
        }

        @Override
        public int compareTo(RouteChoice other) {
            int compare = Integer.compare(this.routeChanges, other.routeChanges);
            if (compare != 0) {
                return compare;
            }
            compare = Integer.compare(this.directionStage, other.directionStage);
            if (compare != 0) {
                return compare;
            }
            compare = this.distance.compareTo(other.distance);
            if (compare != 0) {
                return compare;
            }
            compare = this.startAngle.compareTo(other.startAngle);
            if (compare != 0) {
                return compare;
            }
            return this.groupKey.compareTo(other.groupKey);
        }
    }
}
