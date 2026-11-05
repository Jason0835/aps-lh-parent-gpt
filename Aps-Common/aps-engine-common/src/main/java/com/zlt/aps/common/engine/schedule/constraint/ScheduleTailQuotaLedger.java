package com.zlt.aps.common.engine.schedule.constraint;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当日成型总余量额度台账，按「工厂|排程日期|产品|单位长度」一个桶一份。
 *
 * <p>台账只记录当日已经定稿排出的计划量（{@code assignedMap}）和两阶段阶段为在排任务预留的额度
 * （{@code reservedMap}）。封顶差额不入台账，它只是守恒校验的第三个去向，见排程方案 R12。</p>
 *
 * <p>本类无数据库、Spring 依赖，工作区复制时通过 {@link #copy()} 深拷贝。</p>
 */
public class ScheduleTailQuotaLedger {

    /**
     * 额度桶键，格式为 工厂|排程日期|产品编码|单位长度
     */
    private final String bucketKey;

    /**
     * 当日该桶的成型总余量折算米数
     */
    private final BigDecimal totalQuota;

    /**
     * 业务键到当日已排计划量的登记；同一业务键重复登记时覆盖
     */
    private final Map<String, BigDecimal> assignedMap = new LinkedHashMap<>();

    /**
     * 业务键到两阶段预留额度的登记；定稿登记时会释放同一业务键的预留
     */
    private final Map<String, BigDecimal> reservedMap = new LinkedHashMap<>();

    /**
     * 创建额度台账。
     *
     * @param bucketKey  额度桶键
     * @param totalQuota 当日成型总余量折算米数；为空按零处理
     */
    public ScheduleTailQuotaLedger(String bucketKey, BigDecimal totalQuota) {
        this.bucketKey = bucketKey;
        this.totalQuota = totalQuota == null ? BigDecimal.ZERO : totalQuota;
    }

    /**
     * 获取额度桶键。
     *
     * @return 额度桶键
     */
    public String getBucketKey() {
        return this.bucketKey;
    }

    /**
     * 获取当日成型总余量折算米数。
     *
     * @return 总余量折算米数
     */
    public BigDecimal getTotalQuota() {
        return this.totalQuota;
    }

    /**
     * 获取桶内当日已排计划量合计。
     *
     * @return 已排计划量合计，不含预留
     */
    public BigDecimal assigned() {
        BigDecimal assignedQty = BigDecimal.ZERO;
        for (BigDecimal value : this.assignedMap.values()) {
            assignedQty = assignedQty.add(this.nvl(value));
        }
        return assignedQty;
    }

    /**
     * 获取池可用额度，仅供日志与解释证据展示。
     *
     * <p>本方法不扣减同班其他任务的预留，不能用于封顶计算；封顶必须使用
     * {@link #availableFor(String)}。</p>
     *
     * @return 总余量减已排计划量
     */
    public BigDecimal available() {
        return this.totalQuota.subtract(this.assigned());
    }

    /**
     * 获取指定任务可用的额度，封顶计算的唯一取值入口。
     *
     * <p>等于总余量减已排计划量，再减其他任务尚未定稿的预留；本任务自身的预留被排除，
     * 避免两阶段下同一笔预留被扣减两次。</p>
     *
     * @param businessKey 任务业务键
     * @return 本任务可用额度
     */
    public BigDecimal availableFor(String businessKey) {
        return this.totalQuota.subtract(this.assigned()).subtract(this.reservedExcluding(businessKey));
    }

    /**
     * 登记任务定稿计划量，重复登记按业务键覆盖。
     *
     * <p>登记前先释放同一业务键的预留，否则该任务会同时出现在已排与预留里，
     * 导致同班后续任务被扣减两次。</p>
     *
     * @param businessKey 任务业务键
     * @param planQty     定稿计划量；为空按零处理
     */
    public void assign(String businessKey, BigDecimal planQty) {
        if (businessKey == null) {
            return;
        }
        this.release(businessKey);
        this.assignedMap.put(businessKey, this.nvl(planQty));
    }

    /**
     * 获取指定任务已登记的计划量。
     *
     * <p>产能拆分与工装取整会改写实际承接量；需要重新按额度收紧时，必须先扣掉本任务自身的旧登记，
     * 否则同一任务会被扣减两次。</p>
     *
     * @param businessKey 任务业务键
     * @return 该任务已登记的计划量；未登记时返回零
     */
    public BigDecimal assignedOf(String businessKey) {
        if (businessKey == null) {
            return BigDecimal.ZERO;
        }
        return this.nvl(this.assignedMap.get(businessKey));
    }

    /**
     * 撤销指定任务已登记的计划量。
     *
     * <p>两阶段同源请求吸收只做归属试算，不产生物理生产行；试算过程写入的登记必须撤销，
     * 否则后续任务会少看到这部分额度。</p>
     *
     * @param businessKey 任务业务键；不存在时为空操作
     */
    public void removeAssignment(String businessKey) {
        if (businessKey == null) {
            return;
        }
        this.assignedMap.remove(businessKey);
    }

    /**
     * 登记任务预留额度，重复预留按业务键覆盖。
     *
     * @param businessKey 任务业务键
     * @param reserveQty  预留额度；为空或非正数时不登记
     */
    public void reserve(String businessKey, BigDecimal reserveQty) {
        if (businessKey == null || reserveQty == null || reserveQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        this.reservedMap.put(businessKey, reserveQty);
    }

    /**
     * 释放指定任务的预留额度。
     *
     * @param businessKey 任务业务键；不存在时为空操作
     */
    public void release(String businessKey) {
        if (businessKey == null) {
            return;
        }
        this.reservedMap.remove(businessKey);
    }

    /**
     * 清空全部预留，由班末关账调用。
     */
    public void clearReservations() {
        this.reservedMap.clear();
    }

    /**
     * 复制台账，用于两阶段隔离工作区。
     *
     * @return 已排与预留均独立的新台账
     */
    public ScheduleTailQuotaLedger copy() {
        ScheduleTailQuotaLedger copy = new ScheduleTailQuotaLedger(this.bucketKey, this.totalQuota);
        copy.assignedMap.putAll(this.assignedMap);
        copy.reservedMap.putAll(this.reservedMap);
        return copy;
    }

    /**
     * 汇总除指定业务键以外的预留额度。
     *
     * @param businessKey 需要排除的业务键
     * @return 其他任务预留合计
     */
    private BigDecimal reservedExcluding(String businessKey) {
        BigDecimal reservedQty = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : this.reservedMap.entrySet()) {
            if (businessKey != null && businessKey.equals(entry.getKey())) {
                continue;
            }
            reservedQty = reservedQty.add(this.nvl(entry.getValue()));
        }
        return reservedQty;
    }

    /**
     * 将空值归零。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
