package com.zlt.aps.lh.engine.strategy.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 在已确定释放角色及数量边界内验证历史机台先后，按真实时间轴分配守恒数量。
 * 固定机台不参与调整；未命中机台之间不增加时间顺序约束。
 *
 * @author Codex
 * @date 2026/10/09
 */
public final class ContinuationEndingReleaseOrderAllocator {
    /**
     * 限制精确数量状态数，超界只报告未完成，不将其认定为产能不足。
     */
    private static final int MAX_QUANTITY_STATES = 32768;
    /**
     * 防止异常大组在单次排程中无界枚举。
     */
    private static final long MAX_TRANSITIONS = 2000000L;
    /**
     * 当前计算未能证明顺序可行时的原因。
     */
    private String diagnostic;

    /**
     * 在不减少原可排总量、不越过角色上下限的前提下寻找历史优先分配。
     * 每个累计量只保留允许前序机台最晚释放的状态，后续搜索无需保留被支配路径。
     *
     * @param machines 固定机台真实画像
     * @param ranks 对应历史排名，负数为不可调整机台
     * @param minimum 已冻结的生产阶段下限
     * @param maximum 角色、产能和窗口共同限定的数量上限
     * @param preferred 原数量算法生成的守恒基线
     * @return 满足顺序的守恒数量；硬边界无解或计算未完成时返回空并提供原因
     */
    public int[] allocate(List<ContinuationEndingMachineProfile> machines, int[] ranks,
            int[] minimum, int[] maximum, int[] preferred) {
        if (this.isOrdered(machines, ranks, preferred)) {
            return preferred;
        }
        List<Integer> positions = IntStream.range(0, machines.size()).boxed()
                .filter(position -> ranks[position] >= 0)
                .sorted(Comparator.comparingInt((Integer position) -> ranks[position]).reversed())
                .collect(Collectors.toList());
        long targetLong = positions.stream().mapToLong(position -> preferred[position]).sum();
        if (targetLong >= MAX_QUANTITY_STATES) {
            diagnostic = "历史释放顺序精确分配达到数量状态边界，未证明优先顺序可行";
            return null;
        }
        int target = (int) targetLong;
        Map<Integer, AllocationState> states = new LinkedHashMap<>();
        states.put(0, new AllocationState(null, -1, 0, Long.MAX_VALUE, 0));
        long transitions = 0;
        for (Integer position : positions) {
            ContinuationEndingMachineProfile profile = machines.get(position);
            List<AllocationState> options = new ArrayList<>();
            for (int quantity = minimum[position]; quantity <= Math.min(target, maximum[position]); quantity++) {
                if (profile.isLegalQuantity(quantity)) {
                    Date ready = profile.switchReadyTime(quantity);
                    if (Objects.nonNull(ready)) {
                        options.add(new AllocationState(null, position, quantity, ready.getTime(),
                                Math.abs((long) quantity - preferred[position])));
                    }
                }
            }
            Map<Integer, AllocationState> next = new LinkedHashMap<>();
            for (Map.Entry<Integer, AllocationState> entry : states.entrySet()) {
                AllocationState tail = entry.getValue();
                for (AllocationState option : options) {
                    if (++transitions > MAX_TRANSITIONS) {
                        diagnostic = "历史释放顺序精确分配达到组合边界，未证明优先顺序可行";
                        return null;
                    }
                    int total = entry.getKey() + option.quantity;
                    if (total > target || (ranks[position] != PreviousAlternateMachineOrder.UNMATCHED
                            && option.earliestRelease > tail.earliestRelease)) {
                        continue;
                    }
                    long earliest = Math.min(option.earliestRelease, tail.earliestRelease);
                    long distance = option.distance + tail.distance;
                    AllocationState previous = next.get(total);
                    if (Objects.isNull(previous) || earliest > previous.earliestRelease
                            || (earliest == previous.earliestRelease && distance < previous.distance)) {
                        next.put(total, new AllocationState(tail, position, option.quantity, earliest, distance));
                    }
                }
            }
            states = next;
        }
        AllocationState selected = states.get(target);
        if (Objects.isNull(selected)) {
            diagnostic = "角色节点、真实产能或固定阶段下限不允许在保持可排总量时满足历史释放顺序";
            return null;
        }
        int[] quantities = Arrays.copyOf(preferred, preferred.length);
        while (Objects.nonNull(selected.previous)) {
            quantities[selected.position] = selected.quantity;
            selected = selected.previous;
        }
        return quantities;
    }

    /**
     * 核对实际物理释放时间，仅比较命中历史的机台与其低优先级机台。
     *
     * @param machines 真实机台画像
     * @param ranks 历史排名，负数为固定边界
     * @param quantities 已计算数量
     * @return 所有可调整历史机台均按优先级释放时为真
     */
    private boolean isOrdered(List<ContinuationEndingMachineProfile> machines, int[] ranks, int[] quantities) {
        Date[] releases = IntStream.range(0, machines.size())
                .mapToObj(position -> machines.get(position).switchReadyTime(quantities[position])).toArray(Date[]::new);
        for (int earlier = 0; earlier < machines.size(); earlier++) {
            if (ranks[earlier] < 0 || ranks[earlier] == PreviousAlternateMachineOrder.UNMATCHED) {
                continue;
            }
            for (int later = 0; later < machines.size(); later++) {
                if (ranks[later] > ranks[earlier] && (Objects.isNull(releases[earlier])
                        || Objects.isNull(releases[later]) || releases[earlier].after(releases[later]))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 读取硬边界阻断或有界计算未完成的说明。
     *
     * @return 无法采用优先分配时的具体原因
     */
    public String getDiagnostic() {
        return diagnostic;
    }

    /**
     * 一个累计数量的最优后缀状态及重建路径。
     */
    private static final class AllocationState {
        /**
         * 已分配的低优先级后缀。
         */
        private final AllocationState previous;
        /**
         * 当前机台位置。
         */
        private final int position;
        /**
         * 当前机台合法数量。
         */
        private final int quantity;
        /**
         * 后缀最早物理释放时间，也是高优先级机台的最晚释放界限。
         */
        private final long earliestRelease;
        /**
         * 与原基线数量的总偏差，用于相同边界下保持既有分配。
         */
        private final long distance;

        /**
         * 保存一个不可变分配状态。
         *
         * @param previous 后缀状态
         * @param position 当前机台位置
         * @param quantity 当前合法数量
         * @param earliestRelease 后缀最早释放时间
         * @param distance 与基线的累计偏差
         */
        private AllocationState(AllocationState previous, int position, int quantity,
                long earliestRelease, long distance) {
            this.previous = previous;
            this.position = position;
            this.quantity = quantity;
            this.earliestRelease = earliestRelease;
            this.distance = distance;
        }
    }
}
