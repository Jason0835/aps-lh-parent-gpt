package com.zlt.aps.lh.engine.strategy.support;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;

/**
 * 续作多物理机台余量收尾试算器。只处理数量向量，不持有或修改排程上下文。
 * <p>机台按优先下机到相对保留排序。每次只保留一个候选和一个最优结果，
 * 枚举相邻生产收尾班次及排序分界，不枚举机台子集或逐条搬量。数量可达性采用有界压缩余数区间，
 * 内存随机台/班次及压缩区间数增长，绝不按目标条数分配数组。</p>
 */
public final class ContinuationEndingAllocationPlanner {
    /** 各物理机台按时间排序的真实有效班产，单控整机为两侧合计。 */
    private final int[][] capacities;
    /** 各机台残班归整单位，整班奇数产能仍原样保留。 */
    private final int[] multiples;
    /** 当前窗口的班次标识，长度限定连续生产窗口。 */
    private final boolean[] nightShifts;
    /** 已证明可排的连续基线，优化失败时使用；数组大小仅为机台数。 */
    private int[] baseline;
    /** 目标未排量；必须结合可达性证明状态解释原因。 */
    private int remainingQty;
    /** false表示有界搜索尚未证明是否存在完整组合，不能宣称容量不足。 */
    private boolean quantitySearchComplete = true;
    /** 数量搜索诊断。 */
    private String quantityDiagnostic;

    /**
     * 构造当前组的有界试算器。
     * @param capacities 各物理机台各班真实有效产能
     * @param multiples 残班归整单位
     * @param nightShifts 班次是否为晚班
     */
    public ContinuationEndingAllocationPlanner(int[][] capacities, int[] multiples, boolean[] nightShifts) {
        if (capacities.length != multiples.length || capacities.length == 0) {
            throw new IllegalArgumentException("续作收尾机台产能与归整单位不完整");
        }
        for (int machine = 0; machine < capacities.length; machine++) {
            if (multiples[machine] <= 0 || capacities[machine].length != nightShifts.length
                    || Arrays.stream(capacities[machine]).anyMatch(capacity -> capacity < 0)) {
                throw new IllegalArgumentException("续作收尾真实班产或归整单位无效");
            }
        }
        this.capacities = capacities;
        this.multiples = multiples;
        this.nightShifts = nightShifts;
    }

    /**
     * 在连续合法基线上试算整组分摊；候选无解时保留基线，不增加总量。
     * @param targetQty 现有归整目标与实际可消费余量共同确定的上限
     * @param endingShift 指定机台和数量的最后有量班次；无生产返回窗口外序号
     * @param endingLimit 各机台指定收尾班次的最大连续量，中班仅允许截止前尾量
     * @param evaluate 整组联合预演评分，null表示资源不合法；分项越大越优
     * @return 每台物理机台的连续生产总量
     */
    public int[] allocate(int targetQty, IntBinaryOperator endingShift,
                          IntBinaryOperator endingLimit, Function<int[], long[]> evaluate) {
        return this.allocate(targetQty, endingShift, endingLimit, (candidate, original) -> evaluate.apply(candidate));
    }

    /**
     * 先证明可排总量，再相对合法基线比较夜班利用和相邻生产收尾班次，不锁死一次贪心少排量。
     * @param targetQty 组级可消费上限
     * @param endingShift 最后有量班次计算
     * @param endingLimit 各机台指定收尾班次的最大连续量，中班仅允许截止前尾量
     * @param evaluate 候选与固定连续基线的整组比较，null拒绝候选
     * @return 最终合法数量向量
     */
    public int[] allocate(int targetQty, IntBinaryOperator endingShift,
                          IntBinaryOperator endingLimit, BiFunction<int[], int[], long[]> evaluate) {
        quantitySearchComplete = true;
        quantityDiagnostic = null;
        int[] best = this.buildContinuousBaseline(Math.max(0, targetQty));
        best = this.consumeRoundingRemainder(best, Math.max(0, targetQty));
        baseline = best.clone();
        int scheduledQty = Arrays.stream(best).sum();
        remainingQty = Math.max(0, targetQty - scheduledQty);
        // 搜索预算未完成时不能把贪心少排量当作新的优化目标，只保留已验证基线并报告未证明状态。
        if (!quantitySearchComplete || capacities.length < 2 || scheduledQty <= 0) {
            return best;
        }
        long[] bestScore = evaluate.apply(best, baseline);
        int shiftCount = nightShifts.length;
        int[][] lower = new int[shiftCount][capacities.length];
        int[][] upper = new int[shiftCount][capacities.length];
        // 完工时间随连续排量单调推进，通过二分定位班次边界，避免按目标条数循环。
        for (int machine = 0; machine < capacities.length; machine++) {
            int maximum = this.floorQuantity(machine, scheduledQty);
            for (int shift = 0; shift < shiftCount; shift++) {
                lower[shift][machine] = this.firstQuantityAtShift(machine, maximum, shift + 1, endingShift);
                upper[shift][machine] = Math.min(this.lastQuantityAtShift(machine, maximum, shift + 1, endingShift),
                        endingLimit.applyAsInt(machine, shift + 1));
            }
        }
        for (int shift = 0; shift < shiftCount; shift++) {
            for (int split = 0; split <= capacities.length; split++) {
                int[] candidate = this.allocateAdjacentGroups(scheduledQty, shift, split, lower, upper);
                if (Objects.isNull(candidate)) {
                    continue;
                }
                long[] score = evaluate.apply(candidate, baseline);
                if (this.isBetter(candidate, score, best, bestScore)) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    /** @return 固定合法连续基线副本，优化失败不能回到旧逐日降模 */
    public int[] getBaseline() {
        return baseline.clone();
    }

    /** @return 组级上限内尚未分配量 */
    public int getRemainingQty() {
        return remainingQty;
    }

    /** @return 是否已完整证明数量可达性 */
    public boolean isQuantitySearchComplete() {
        return quantitySearchComplete;
    }

    /** @return 数量未完成搜索的原因 */
    public String getQuantityDiagnostic() {
        return quantityDiagnostic;
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
        return quantities;
    }

    /**
     * 使用精确余数区间可达性补齐贪心余量，允许多机台协同回退多个合法数量单位。
     * @param quantities 当前连续数量
     * @param targetQty 组级目标上限
     * @return 最大合法可排量的连续基线；搜索达限时保留已验证量并明确诊断
     */
    private int[] consumeRoundingRemainder(int[] quantities, int targetQty) {
        if (Arrays.stream(quantities).sum() == targetQty) {
            return quantities;
        }
        int[] maximum = new int[capacities.length];
        for (int machine = 0; machine < capacities.length; machine++) {
            maximum[machine] = this.floorQuantity(machine, targetQty);
        }
        ContinuationEndingQuantityReachability reachability =
                new ContinuationEndingQuantityReachability(capacities, multiples);
        int[] exact = reachability.allocate(targetQty, new int[capacities.length], maximum, quantities);
        quantitySearchComplete = reachability.isComplete();
        quantityDiagnostic = reachability.getDiagnostic();
        return Objects.nonNull(exact) ? exact : quantities;
    }

    /**
     * 只枚举按降模顺序的前后两组，不进行指数级机台排列组合。
     * @param total 严格守恒的已排总量
     * @param firstShift 前组生产收尾班次的数组位置
     * @param split 前组机台数
     * @param lower 每班次允许的最少连续量
     * @param upper 每班次允许的最多连续量
     * @return 合法数量候选，数量或区间不可行返回null
     */
    private int[] allocateAdjacentGroups(int total, int firstShift, int split,
                                        int[][] lower, int[][] upper) {
        int[] candidate = new int[capacities.length];
        int[] limits = new int[capacities.length];
        long minimumTotal = 0;
        for (int machine = 0; machine < capacities.length; machine++) {
            int shift = machine < split ? firstShift : firstShift + 1;
            if (shift >= nightShifts.length) {
                return null;
            }
            int minimum = lower[shift][machine];
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
        int[] minimum = candidate.clone();
        int remaining = total - (int) minimumTotal;
        // 保留机台先承接可搬入量，优先下机机台只保留其必要份额；没有固定比例。
        for (int machine = capacities.length - 1; machine >= 0 && remaining > 0; machine--) {
            int limit = candidate[machine] + Math.min(remaining, limits[machine] - candidate[machine]);
            int next = this.floorQuantity(machine, limit);
            remaining -= next - candidate[machine];
            candidate[machine] = next;
        }
        if (remaining == 0) {
            return candidate;
        }
        // 分组内同样不能把归整余数误判为无解；仍在同一分摊器中复用精确可达性，不开平行流程。
        int[] exact = new ContinuationEndingQuantityReachability(capacities, multiples)
                .allocate(total, minimum, limits, candidate);
        return Objects.nonNull(exact) && Arrays.stream(exact).sum() == total ? exact : null;
    }

    /**
     * 二分查找进入指定生产收尾班次的最小合法连续量。
     * @param machine 机台序号
     * @param maximum 最大可排量
     * @param shift 生产收尾班次序号
     * @param endingShift 最后有量班次计算
     * @return 最小合法量，不存在返回-1
     */
    private int firstQuantityAtShift(int machine, int maximum, int shift, IntBinaryOperator endingShift) {
        int low = 1;
        int high = maximum;
        while (low < high) {
            int middle = low + (high - low) / 2;
            int quantity = this.floorQuantity(machine, middle);
            if (quantity <= 0 || endingShift.applyAsInt(machine, quantity) < shift) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        int quantity = this.floorQuantity(machine, low);
        return quantity > 0 && endingShift.applyAsInt(machine, quantity) == shift ? quantity : -1;
    }

    /**
     * 二分查找最后有量班次仍为指定班次的最大连续量。
     * @param machine 机台序号
     * @param maximum 最大可排量
     * @param shift 生产收尾班次序号
     * @param endingShift 最后有量班次计算
     * @return 最大合法量，不存在返回-1
     */
    private int lastQuantityAtShift(int machine, int maximum, int shift, IntBinaryOperator endingShift) {
        int low = 0;
        int high = maximum;
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            int quantity = this.floorQuantity(machine, middle);
            if (quantity <= 0 || endingShift.applyAsInt(machine, quantity) <= shift) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        int quantity = this.floorQuantity(machine, low);
        return quantity > 0 && endingShift.applyAsInt(machine, quantity) == shift ? quantity : -1;
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
