package com.zlt.aps.lh.engine.strategy.support;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;

/**
 * 续作多物理机台余量收尾试算器。只处理数量向量，不持有或修改排程上下文。
 * <p>机台按优先下机到相对保留排序。每次只保留一个候选和一个最优结果，
 * 枚举相邻换模班次及排序分界，不枚举机台子集或逐条搬量，空间为 O(机台数×班次数)。</p>
 */
public final class ContinuationEndingAllocationPlanner {
    /** 各物理机台按时间排序的真实有效班产，单控整机为两侧合计。 */
    private final int[][] capacities;
    /** 各机台残班归整单位，整班奇数产能仍原样保留。 */
    private final int[] multiples;
    /** 当前窗口的晚班标识，仅用于试算连续补至晚班末尾。 */
    private final boolean[] nightShifts;

    /**
     * 构造当前组的有界试算器。
     * @param capacities 各物理机台各班真实有效产能
     * @param multiples 残班归整单位
     * @param nightShifts 班次是否为晚班
     */
    public ContinuationEndingAllocationPlanner(int[][] capacities, int[] multiples, boolean[] nightShifts) {
        this.capacities = capacities;
        this.multiples = multiples;
        this.nightShifts = nightShifts;
    }

    /**
     * 在连续合法基线上试算整组分摊；候选无解时保留基线，不增加总量。
     * @param targetQty 现有归整目标与实际可消费余量共同确定的上限
     * @param changeShift 独立预演指定机台、数量的实际可换模班次；不可换模返回窗口外序号
     * @param evaluate 整组联合预演评分，null表示资源不合法；分项越大越优
     * @return 每台物理机台的连续生产总量
     */
    public int[] allocate(int targetQty, IntBinaryOperator changeShift, Function<int[], long[]> evaluate) {
        int[] best = this.buildContinuousBaseline(targetQty);
        int scheduledQty = Arrays.stream(best).sum();
        if (capacities.length < 2 || scheduledQty <= 0) {
            return best;
        }
        long[] bestScore = evaluate.apply(best);
        int shiftCount = nightShifts.length;
        int[][] lower = new int[shiftCount][capacities.length];
        int[][] upper = new int[shiftCount][capacities.length];
        // 完工时间随连续排量单调推进，通过二分定位班次边界，避免按目标条数循环。
        for (int machine = 0; machine < capacities.length; machine++) {
            int maximum = this.floorQuantity(machine, scheduledQty);
            for (int shift = 0; shift < shiftCount; shift++) {
                lower[shift][machine] = this.firstQuantityAtShift(machine, maximum, shift + 1, changeShift);
                upper[shift][machine] = this.lastQuantityAtShift(machine, maximum, shift + 1, changeShift);
            }
        }
        for (int shift = 0; shift < shiftCount; shift++) {
            for (int split = 0; split <= capacities.length; split++) {
                // false先尝试原区间；true再尝试把需要等早班的机台连续补至前一晚班结束。
                for (boolean fillNight : new boolean[]{false, true}) {
                    int[] candidate = this.allocateAdjacentGroups(scheduledQty, shift, split, lower, upper, fillNight);
                    if (Objects.isNull(candidate)) {
                        continue;
                    }
                    long[] score = evaluate.apply(candidate);
                    if (this.isBetter(candidate, score, best, bestScore)) {
                        best = candidate;
                        bestScore = score;
                    }
                }
            }
        }
        return best;
    }

    /**
     * 按时间优先填满各机台有效班产，剩余尾量优先留给保留机台。
     * @param targetQty 可分配总量
     * @return 不超过目标且不存在人为中间欠满班的基线
     */
    private int[] buildContinuousBaseline(int targetQty) {
        int[] quantities = new int[capacities.length];
        boolean[] finished = new boolean[capacities.length];
        int remaining = Math.max(0, targetQty);
        for (int shift = 0; shift < nightShifts.length && remaining > 0; shift++) {
            for (int machine = capacities.length - 1; machine >= 0 && remaining > 0; machine--) {
                if (finished[machine] || capacities[machine][shift] <= 0) {
                    continue;
                }
                int available = Math.min(remaining, capacities[machine][shift]);
                int next = this.floorQuantity(machine, quantities[machine] + available);
                remaining -= next - quantities[machine];
                finished[machine] = next - quantities[machine] < capacities[machine][shift];
                quantities[machine] = next;
            }
        }
        // 奇数整班与偶数残班并存时，允许回退一个整班尾单位再由另一台接收，避免可排4条却只排3条。
        this.consumeRoundingRemainder(quantities, remaining);
        return quantities;
    }

    /**
     * 守恒调整奇数整班造成的归整余数，只尝试机台对，不按条数循环。
     * @param quantities 当前连续数量
     * @param remaining 尚未分配的目标余数
     */
    private void consumeRoundingRemainder(int[] quantities, int remaining) {
        for (int donor = 0; donor < quantities.length && remaining > 0; donor++) {
            if (quantities[donor] <= 0) {
                continue;
            }
            int reduced = this.floorQuantity(donor, quantities[donor] - 1);
            int released = quantities[donor] - reduced;
            for (int receiver = quantities.length - 1; receiver >= 0 && remaining > 0; receiver--) {
                if (receiver == donor) {
                    continue;
                }
                int limit = (int) Math.min(Integer.MAX_VALUE, (long) quantities[receiver] + remaining + released);
                int received = this.floorQuantity(receiver, limit) - quantities[receiver];
                if (received > released && received - released <= remaining) {
                    quantities[donor] = reduced;
                    quantities[receiver] += received;
                    remaining -= received - released;
                    break;
                }
            }
        }
    }

    /**
     * 只枚举按降模顺序的前后两组，不进行指数级机台排列组合。
     * @param total 严格守恒的已排总量
     * @param firstShift 前组可换模班次的数组位置
     * @param split 前组机台数
     * @param lower 每班次允许的最少连续量
     * @param upper 每班次允许的最多连续量
     * @param fillNight 是否提高早班换模机台的下限至前一晚班末
     * @return 合法数量候选，数量或区间不可行返回null
     */
    private int[] allocateAdjacentGroups(int total, int firstShift, int split,
                                        int[][] lower, int[][] upper, boolean fillNight) {
        int[] candidate = new int[capacities.length];
        int[] limits = new int[capacities.length];
        long minimumTotal = 0;
        for (int machine = 0; machine < capacities.length; machine++) {
            int shift = machine < split ? firstShift : firstShift + 1;
            if (shift >= nightShifts.length) {
                return null;
            }
            int minimum = lower[shift][machine];
            if (fillNight && shift > 0 && nightShifts[shift - 1]) {
                minimum = Math.max(minimum, this.quantityThroughShift(machine, shift - 1));
            }
            if (minimum <= 0 || minimum > upper[shift][machine]) {
                return null;
            }
            candidate[machine] = minimum;
            limits[machine] = upper[shift][machine];
            minimumTotal += minimum;
        }
        if (minimumTotal > total) {
            return null;
        }
        int remaining = total - (int) minimumTotal;
        // 保留机台先承接可搬入量，优先下机机台只保留其必要份额；没有固定比例。
        for (int machine = capacities.length - 1; machine >= 0 && remaining > 0; machine--) {
            int limit = candidate[machine] + Math.min(remaining, limits[machine] - candidate[machine]);
            int next = this.floorQuantity(machine, limit);
            remaining -= next - candidate[machine];
            candidate[machine] = next;
        }
        return remaining == 0 ? candidate : null;
    }

    /**
     * 二分查找进入指定实际换模班次的最小合法连续量。
     * @param machine 机台序号
     * @param maximum 最大可排量
     * @param shift 换模班次序号
     * @param changeShift 换模预演
     * @return 最小合法量，不存在返回-1
     */
    private int firstQuantityAtShift(int machine, int maximum, int shift, IntBinaryOperator changeShift) {
        int low = 1;
        int high = maximum;
        while (low < high) {
            int middle = low + (high - low) / 2;
            int quantity = this.floorQuantity(machine, middle);
            if (quantity <= 0 || changeShift.applyAsInt(machine, quantity) < shift) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        int quantity = this.floorQuantity(machine, low);
        return quantity > 0 && changeShift.applyAsInt(machine, quantity) == shift ? quantity : -1;
    }

    /**
     * 二分查找仍可在指定实际换模班次开始换模的最大连续量。
     * @param machine 机台序号
     * @param maximum 最大可排量
     * @param shift 换模班次序号
     * @param changeShift 换模预演
     * @return 最大合法量，不存在返回-1
     */
    private int lastQuantityAtShift(int machine, int maximum, int shift, IntBinaryOperator changeShift) {
        int low = 0;
        int high = maximum;
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            int quantity = this.floorQuantity(machine, middle);
            if (quantity <= 0 || changeShift.applyAsInt(machine, quantity) <= shift) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        int quantity = this.floorQuantity(machine, low);
        return quantity > 0 && changeShift.applyAsInt(machine, quantity) == shift ? quantity : -1;
    }

    /**
     * 取不超过上限的合法连续量：整班容量原样保留，只归整最后残班。
     * @param machine 机台序号
     * @param limit 数量上限
     * @return 合法数量
     */
    private int floorQuantity(int machine, int limit) {
        int quantity = 0;
        for (int capacity : capacities[machine]) {
            int remaining = limit - quantity;
            if (remaining < capacity) {
                return quantity + Math.max(0, remaining) / multiples[machine] * multiples[machine];
            }
            quantity += capacity;
        }
        return quantity;
    }

    /**
     * 汇总至指定晚班末的真实容量，防止容量累计整数溢出。
     * @param machine 机台序号
     * @param lastShift 最后计入的班次位置
     * @return 连续补满量
     */
    private int quantityThroughShift(int machine, int lastShift) {
        long quantity = 0;
        for (int shift = 0; shift <= lastShift; shift++) {
            quantity += capacities[machine][shift];
        }
        return (int) Math.min(Integer.MAX_VALUE, quantity);
    }

    /**
     * 先比较整组错峰和等待时间，再按下机排序比较数量倾斜。
     * @param candidate 当前候选
     * @param score 当前评分，null为不可行
     * @param best 已验证方案
     * @param bestScore 已验证方案评分
     * @return 当前候选是否更优
     */
    private boolean isBetter(int[] candidate, long[] score, int[] best, long[] bestScore) {
        if (Objects.isNull(score)) {
            return false;
        }
        if (Objects.isNull(bestScore)) {
            return true;
        }
        for (int index = 0; index < score.length; index++) {
            if (score[index] != bestScore[index]) {
                return score[index] > bestScore[index];
            }
        }
        for (int machine = 0; machine < candidate.length; machine++) {
            if (candidate[machine] != best[machine]) {
                return candidate[machine] < best[machine];
            }
        }
        return false;
    }
}
