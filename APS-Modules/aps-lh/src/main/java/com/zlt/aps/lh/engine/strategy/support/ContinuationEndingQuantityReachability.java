package com.zlt.aps.lh.engine.strategy.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 续作连续排量的精确余数可达性计算，仅供现有收尾分摊器调用。
 * <p>每台合法量是“已满班前缀＋末班归整量”的有限等差区间并集。以归整单位最小公倍数
 * 分桶，将目标上亿条的等差序列压成区间；区间相加与合并保持精确，不按目标条数分配内存。</p>
 */
public final class ContinuationEndingQuantityReachability {
    /** 内存边界由压缩区间数限定，超过边界必须报告搜索未完成，不能报告产能不足。 */
    private static final int MAX_INTERVAL_COUNT = 32768;
    /** 仅限制计算复杂度，不改变业务归整单位。 */
    private static final int MAX_COMMON_MODULUS = 4096;
    /** 极端异质产能下控制区间组合次数，避免指数级枚举占满调度线程。 */
    private static final long MAX_TRANSITIONS = 2000000L;
    /** 真实机台班产。 */
    private final int[][] capacities;
    /** 各机台残班单位。 */
    private final int[] multiples;
    /** 本次搜索是否完整，预算耗尽时为false。 */
    private boolean complete = true;
    /** 当前保留的压缩区间数。 */
    private int intervalCount;
    /** 已计算的区间组合次数。 */
    private long transitions;
    /** 未完成搜索的诊断。 */
    private String diagnostic;

    /** @param capacities 真实班产 @param multiples 残班单位 */
    public ContinuationEndingQuantityReachability(int[][] capacities, int[] multiples) {
        this.capacities = capacities;
        this.multiples = multiples;
    }

    /** @return 是否完整证明可达性，false不代表数量不可达 */
    public boolean isComplete() {
        return complete;
    }

    /** @return 搜索边界诊断 */
    public String getDiagnostic() {
        return diagnostic;
    }

    /**
     * 在给定区间内精确计算不超过目标的最大合法总量，并优先保留原连续基线。
     * @param targetQty 组级可消费上限
     * @param minimum 每台下限
     * @param maximum 每台上限
     * @param preferred 已验证基线，重建时尽量保持其连续排量
     * @return 最大可达总量的数量向量；约束无解或预算耗尽返回null，用isComplete区分
     */
    public int[] allocate(int targetQty, int[] minimum, int[] maximum, int[] preferred) {
        int modulus = this.resolveCommonModulus();
        if (!complete) {
            return null;
        }
        int machines = capacities.length;
        List<Map<Integer, NavigableMap<Long, Long>>> legal = new ArrayList<>(machines);
        List<Map<Integer, NavigableMap<Long, Long>>> suffix = new ArrayList<>(machines + 1);
        for (int machine = 0; machine < machines; machine++) {
            legal.add(this.buildLegalQuantities(machine, minimum[machine], Math.min(targetQty, maximum[machine]), modulus));
            suffix.add(null);
        }
        Map<Integer, NavigableMap<Long, Long>> empty = new TreeMap<>();
        this.addInterval(empty, 0, 0, 0);
        suffix.add(empty);
        for (int machine = machines - 1; machine >= 0 && complete; machine--) {
            suffix.set(machine, this.combine(legal.get(machine), suffix.get(machine + 1), modulus, targetQty));
        }
        if (!complete || suffix.get(0).isEmpty()) {
            return null;
        }
        int total = this.maximumReachable(suffix.get(0), modulus);
        int[] quantities = new int[machines];
        for (int machine = 0; machine < machines; machine++) {
            quantities[machine] = this.selectQuantity(legal.get(machine), suffix.get(machine + 1),
                    total, modulus, preferred[machine]);
            total -= quantities[machine];
        }
        return quantities;
    }

    /** @return 小公倍数；过大只报告未完成，禁止改写归整单位 */
    private int resolveCommonModulus() {
        long common = 1;
        for (int multiple : multiples) {
            common = common / this.gcd((int) common, multiple) * multiple;
            if (common > MAX_COMMON_MODULUS) {
                this.limit("残班单位公倍数超过有界余数搜索范围");
                return 0;
            }
        }
        return (int) common;
    }

    /** @param first 正整数 @param second 正整数 @return 最大公约数 */
    private int gcd(int first, int second) {
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first;
    }

    /**
     * 每个末班对应一个等差区间，完整班产作为独立合法端点保留。
     * @param machine 机台序号
     * @param minimum 下限
     * @param maximum 上限
     * @param modulus 公倍数
     * @return 余数到商区间的精确映射
     */
    private Map<Integer, NavigableMap<Long, Long>> buildLegalQuantities(int machine, int minimum,
            int maximum, int modulus) {
        Map<Integer, NavigableMap<Long, Long>> values = new TreeMap<>();
        long prefix = 0;
        int unit = multiples[machine];
        for (int capacity : capacities[machine]) {
            if (prefix > maximum || !complete) {
                break;
            }
            long first = prefix + Math.max(0, ((long) minimum - prefix + unit - 1) / unit) * unit;
            long last = Math.min(maximum, prefix + capacity);
            // 残班必须小于当前完整班产；完整班产不强制变为残班单位的倍数。
            last = Math.min(last, prefix + Math.max(0, capacity - 1));
            this.addProgression(values, first, last, unit, modulus);
            prefix += capacity;
            if (prefix >= minimum && prefix <= maximum) {
                this.addInterval(values, (int) (prefix % modulus), prefix / modulus, prefix / modulus);
            }
        }
        if (minimum == 0) {
            this.addInterval(values, 0, 0, 0);
        }
        return values;
    }

    /** @param values 输出区间 @param first 首值 @param last 上限 @param unit 步长 @param modulus 公倍数 */
    private void addProgression(Map<Integer, NavigableMap<Long, Long>> values, long first, long last,
            int unit, int modulus) {
        for (long start = first; start <= last && start < first + modulus && complete; start += unit) {
            long end = start + (last - start) / modulus * modulus;
            this.addInterval(values, (int) (start % modulus), start / modulus, end / modulus);
        }
    }

    /**
     * 同步长的整数区间相加后仍为区间；因此合并不会引入实际不可达的数量。
     * @param first 一台机台的合法量
     * @param second 后续机台的可达量
     * @param modulus 公倍数
     * @param targetQty 数量上限
     * @return 合并结果
     */
    private Map<Integer, NavigableMap<Long, Long>> combine(Map<Integer, NavigableMap<Long, Long>> first,
            Map<Integer, NavigableMap<Long, Long>> second, int modulus, int targetQty) {
        Map<Integer, NavigableMap<Long, Long>> result = new TreeMap<>();
        for (Map.Entry<Integer, NavigableMap<Long, Long>> left : first.entrySet()) {
            for (Map.Entry<Integer, NavigableMap<Long, Long>> right : second.entrySet()) {
                int residue = (left.getKey() + right.getKey()) % modulus;
                if (residue > targetQty) {
                    continue;
                }
                int carry = (left.getKey() + right.getKey()) / modulus;
                long maximum = ((long) targetQty - residue) / modulus;
                for (Map.Entry<Long, Long> leftRange : left.getValue().entrySet()) {
                    for (Map.Entry<Long, Long> rightRange : right.getValue().entrySet()) {
                        if (++transitions > MAX_TRANSITIONS) {
                            this.limit("归整可达性区间组合达到计算上限，未证明余量不可排");
                        }
                        if (!complete) {
                            return result;
                        }
                        long low = leftRange.getKey() + rightRange.getKey() + carry;
                        long high = Math.min(maximum, leftRange.getValue() + rightRange.getValue() + carry);
                        this.addInterval(result, residue, low, high);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 就地合并重叠/相邻区间，只保留有效区间，不积累全部候选对象。
     * @param values 余数映射
     * @param residue 余数
     * @param low 商下限
     * @param high 商上限
     */
    private void addInterval(Map<Integer, NavigableMap<Long, Long>> values, int residue, long low, long high) {
        if (low > high || !complete) {
            return;
        }
        NavigableMap<Long, Long> ranges = values.computeIfAbsent(residue, key -> new TreeMap<Long, Long>());
        Map.Entry<Long, Long> before = ranges.floorEntry(low);
        if (Objects.nonNull(before) && before.getValue() + 1 >= low) {
            low = before.getKey();
            high = Math.max(high, before.getValue());
            ranges.remove(before.getKey());
            intervalCount--;
        }
        Map.Entry<Long, Long> after = ranges.ceilingEntry(low);
        while (Objects.nonNull(after) && after.getKey() <= high + 1) {
            high = Math.max(high, after.getValue());
            ranges.remove(after.getKey());
            intervalCount--;
            after = ranges.ceilingEntry(low);
        }
        ranges.put(low, high);
        if (++intervalCount > MAX_INTERVAL_COUNT) {
            this.limit("归整可达性压缩区间达到内存上限，未证明余量不可排");
        }
    }

    /** @param values 精确可达区间 @param modulus 公倍数 @return 最大可达量 */
    private int maximumReachable(Map<Integer, NavigableMap<Long, Long>> values, int modulus) {
        long maximum = 0;
        for (Map.Entry<Integer, NavigableMap<Long, Long>> entry : values.entrySet()) {
            maximum = Math.max(maximum, entry.getValue().lastEntry().getValue() * modulus + entry.getKey());
        }
        return (int) maximum;
    }

    /**
     * 从精确可达后缀反推一台机台的量，优先接近连续基线，同距离时优先下机少排。
     * @param legal 当前机台合法区间
     * @param suffix 后续机台可达区间
     * @param total 待分配总量
     * @param modulus 公倍数
     * @param preferred 基线数量
     * @return 有可达后缀的当前机台量
     */
    private int selectQuantity(Map<Integer, NavigableMap<Long, Long>> legal,
            Map<Integer, NavigableMap<Long, Long>> suffix, int total, int modulus, int preferred) {
        long selected = -1;
        long distance = Long.MAX_VALUE;
        for (Map.Entry<Integer, NavigableMap<Long, Long>> entry : legal.entrySet()) {
            int residue = entry.getKey();
            int otherResidue = Math.floorMod(total - residue, modulus);
            NavigableMap<Long, Long> other = suffix.get(otherResidue);
            if (Objects.isNull(other) || total < residue + otherResidue) {
                continue;
            }
            long quotient = ((long) total - residue - otherResidue) / modulus;
            for (Map.Entry<Long, Long> range : entry.getValue().entrySet()) {
                for (Map.Entry<Long, Long> next : other.entrySet()) {
                    long low = Math.max(range.getKey(), quotient - next.getValue());
                    long high = Math.min(range.getValue(), quotient - next.getKey());
                    if (low > high) {
                        continue;
                    }
                    long wanted = Math.floorDiv((long) preferred - residue, modulus);
                    for (long adjacent = wanted; adjacent <= wanted + 1; adjacent++) {
                        long value = Math.max(low, Math.min(high, adjacent)) * modulus + residue;
                        long delta = Math.abs(value - preferred);
                        if (delta < distance || (delta == distance && value < selected)) {
                            selected = value;
                            distance = delta;
                        }
                    }
                }
            }
        }
        if (selected < 0) {
            throw new IllegalStateException("归整可达区间无法重建守恒数量");
        }
        return (int) selected;
    }

    /** @param reason 搜索边界原因 */
    private void limit(String reason) {
        complete = false;
        diagnostic = reason;
    }
}
