package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 本轮排程独占的备注运行态，局部证据与正式结果关联分开保存。
 * 嵌套预演不会向外层发布；只有调用方明确接纳成功结果后才能投影。
 *
 * @author APS
 * @date 2026/10/23
 */
public final class LhShiftRemarkRuntime {

    /**
     * 沿用硫化正规产品状态的空值口径。
     */
    private static final String FORMAL_PRODUCT_STATUS = "S";
    /**
     * 整机动作至少包含两个不同的实际侧位。
     */
    private static final int WHOLE_MACHINE_SIDE_COUNT = 2;

    /**
     * 结果对象身份关联，不以可变实体的 equals 或数据库编号关联。
     */
    private Map<LhScheduleResult, List<LhShiftDelayEvidence>> resultEvidence = new IdentityHashMap<>();
    /**
     * 精确历史任务的已采纳拒绝，待后续同任务成功绑定，不用于普通候选。
     */
    private Map<LhMouldChangePlan, List<LhShiftDelayEvidence>> historicalEvidence = new IdentityHashMap<>();
    /**
     * 当前局部收集域，不保存到快照或班次9副本。
     */
    private Scope activeScope;
    /**
     * 当前上下文内单调递增的尝试身份。
     */
    private long nextAttemptId;

    /**
     * 开启独立局部域，关闭时恢复父域并丢弃未被采用的证据。
     *
     * @param sku 当前物料状态
     * @param machineCode 当前实际机台或侧位
     * @return 由调用方关闭的局部域
     */
    public Scope openScope(SkuScheduleDTO sku, String machineCode) {
        Scope scope = new Scope(this, activeScope, ++nextAttemptId, sku, machineCode);
        activeScope = scope;
        return scope;
    }

    /**
     * 在实际命中拒绝的原分支收集区间，不主动执行任何约束或资源计算。
     *
     * @param reason 稳定原因
     * @param source 原判断入口
     * @param machineCode 实际作用机台
     * @param sku 当前物料，未传时使用局部域身份
     * @param from 有依据的起点
     * @param until 有依据的结束边界
     */
    public void record(LhShiftDelayReason reason, String source, String machineCode,
            SkuScheduleDTO sku, Date from, Date until) {
        this.record(reason, source, machineCode, sku, from, until, null);
    }

    /**
     * 收集原约束已计算的标量事实，不为诊断重新统计资源。
     *
     * @param reason 稳定原因
     * @param source 原判断入口
     * @param machineCode 实际作用机台
     * @param sku 当前物料状态
     * @param from 拒绝区间开始
     * @param until 拒绝区间结束
     * @param facts 原分支的数量或资源快照
     */
    public void record(LhShiftDelayReason reason, String source, String machineCode,
            SkuScheduleDTO sku, Date from, Date until, String facts) {
        if (activeScope == null || from == null || until == null || !from.before(until)) {
            return;
        }
        String material = sku == null ? activeScope.materialCode : sku.getMaterialCode();
        String status = sku == null ? activeScope.productStatus : sku.getProductStatus();
        activeScope.add(new LhShiftDelayEvidence(activeScope.attemptId, reason, source,
                machineCode, material, status, from.getTime(), until.getTime(), facts));
    }

    /**
     * 将兼容接口返回的附件交给当前候选域，无活动候选时不保存。
     *
     * @param evidence 原判断的不可变附件
     */
    public void includeCurrent(List<LhShiftDelayEvidence> evidence) {
        if (activeScope != null) {
            activeScope.include(evidence);
        }
    }

    /**
     * 实际重用原模具取消了准备动作时，撤销局部计划中的对应延后原因。
     *
     * @param reason 已被真实时间轴消除的原因
     */
    public void removeCurrentReason(LhShiftDelayReason reason) {
        if (activeScope != null && activeScope.evidenceMap != null) {
            activeScope.evidenceMap.values().removeIf(evidence -> evidence.getReason() == reason);
        }
    }

    /**
     * 将当前模具请求的明确数量拒绝限制到实际命中班次，不外推到下一次尝试日期。
     *
     * @param context 当前班次窗口
     * @param sku 当前物料
     * @param allocation 原分配或预检返回值
     * @param source 原调用入口
     */
    public void recordMould(LhScheduleContext context, SkuScheduleDTO sku,
            MouldResourceAllocationResult allocation, String source) {
        if (activeScope == null || allocation == null || allocation.isAllowed() || !allocation.isQuantityShortage()
                || allocation.getResourceReferenceTime() == null) {
            return;
        }
        LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), allocation.getResourceReferenceTime());
        if (shift != null) {
            this.record(LhShiftDelayReason.MOULD_SHORTAGE, source, allocation.getMachineCode(), sku,
                    allocation.getResourceReferenceTime(), allocation.getShortageBlockedUntil() == null
                            ? shift.getShiftEndDateTime() : allocation.getShortageBlockedUntil(),
                    "required=" + allocation.getRequiredMouldQty() + ",remaining=" + allocation.getRemainingAvailableMouldQty()
                            + ",causes=" + allocation.getDiagnosticCauses());
        }
    }

    /**
     * 只为精确历史指定任务保留已实际采纳的拒绝，资格或需求先拒绝时不得调用。
     *
     * @param plan 正式历史任务来源
     * @param evidence 本任务实际拒绝证据
     */
    public void retainHistorical(LhMouldChangePlan plan, List<LhShiftDelayEvidence> evidence) {
        if (plan == null || evidence.isEmpty()) {
            return;
        }
        historicalEvidence.put(plan, LhShiftRemarkRuntime.merge(historicalEvidence.get(plan), evidence));
    }

    /**
     * 取得同一精确历史任务的拒绝附件，只有最终正式来源映射存在时才能使用。
     *
     * @param plan 最终正式历史来源
     * @return 同任务不可变附件，无来源或没有拒绝时为空
     */
    public List<LhShiftDelayEvidence> getHistoricalEvidence(LhMouldChangePlan plan) {
        return historicalEvidence.getOrDefault(plan, Collections.emptyList());
    }

    /**
     * 取得只读正式关联供最终备注服务消费。
     *
     * @return 结果对象身份与不可变证据列表
     */
    public Map<LhScheduleResult, List<LhShiftDelayEvidence>> getResultEvidence() {
        return Collections.unmodifiableMap(resultEvidence);
    }

    /**
     * 捕获可发布状态，不复制上下文、结果或候选矩阵。
     *
     * @return 独立映射及共享不可变值的快照
     */
    public LhShiftRemarkRuntime snapshot() {
        LhShiftRemarkRuntime snapshot = new LhShiftRemarkRuntime();
        snapshot.resultEvidence = new IdentityHashMap<>(resultEvidence);
        snapshot.historicalEvidence = new IdentityHashMap<>(historicalEvidence);
        return snapshot;
    }

    /**
     * 恢复发布状态，保持外层仍存活的局部域及尝试序号。
     *
     * @param snapshot 原尝试前快照
     */
    public void restore(LhShiftRemarkRuntime snapshot) {
        resultEvidence = new IdentityHashMap<>(snapshot.resultEvidence);
        historicalEvidence = new IdentityHashMap<>(snapshot.historicalEvidence);
    }

    /**
     * 按身份及明确区间合并附件，重复探测不累积重复对象。
     *
     * @param first 已有附件，可为空
     * @param second 本次附件
     * @return 不可变去重结果
     */
    public static List<LhShiftDelayEvidence> merge(List<LhShiftDelayEvidence> first,
            List<LhShiftDelayEvidence> second) {
        if ((first == null || first.isEmpty()) && second.isEmpty()) {
            return Collections.emptyList();
        }
        if (first == null || first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        Map<String, LhShiftDelayEvidence> merged = new LinkedHashMap<>();
        first.forEach(evidence -> merged.put(LhShiftRemarkRuntime.key(evidence), evidence));
        second.forEach(evidence -> merged.putIfAbsent(LhShiftRemarkRuntime.key(evidence), evidence));
        return Collections.unmodifiableList(new ArrayList<>(merged.values()));
    }

    /**
     * 生成局部去重键，不参与候选缓存、排序或业务相等性。
     *
     * @param evidence 不可变附件
     * @return 原因、实际范围和物料状态的去重身份
     */
    private static String key(LhShiftDelayEvidence evidence) {
        return evidence.getReason() + "|" + evidence.getMachineCode() + "|" + evidence.getMaterialCode()
                + "|" + evidence.getProductStatus() + "|" + evidence.getBlockedFrom() + "|" + evidence.getBlockedUntil();
    }

    /**
     * 单候选或单正式提交的局部附件容器，未接纳前不进入正式映射。
     *
     * @author APS
     * @date 2026/10/23
     */
    public static final class Scope implements AutoCloseable {

        /**
         * 当前上下文独占运行态。
         */
        private final LhShiftRemarkRuntime runtime;
        /**
         * 外层收集域，用于关闭时恢复。
         */
        private final Scope parent;
        /**
         * 局部尝试序号。
         */
        private final long attemptId;
        /**
         * 本次后物料。
         */
        private final String materialCode;
        /**
         * 本次产品状态。
         */
        private final String productStatus;
        /**
         * 本次作用机台。
         */
        private final String machineCode;
        /**
         * 只保存本局部实际拒绝，不跨候选共享。
         */
        private Map<String, LhShiftDelayEvidence> evidenceMap;

        /**
         * 创建局部收集域。
         *
         * @param runtime 本轮运行态
         * @param parent 外层域
         * @param attemptId 尝试身份
         * @param sku 当前物料状态
         * @param machineCode 当前机台
         */
        private Scope(LhShiftRemarkRuntime runtime, Scope parent, long attemptId,
                SkuScheduleDTO sku, String machineCode) {
            this.runtime = runtime;
            this.parent = parent;
            this.attemptId = attemptId;
            if (sku != null) {
                this.materialCode = sku.getMaterialCode();
                this.productStatus = sku.getProductStatus();
            } else if (parent != null) {
                this.materialCode = parent.materialCode;
                this.productStatus = parent.productStatus;
            } else {
                this.materialCode = null;
                this.productStatus = null;
            }
            this.machineCode = machineCode;
        }

        /**
         * 加入约束原分支返回的附件。
         *
         * @param evidence 已冻结的证据
         */
        private void add(LhShiftDelayEvidence evidence) {
            if (evidenceMap == null) {
                evidenceMap = new LinkedHashMap<>();
            }
            evidenceMap.putIfAbsent(LhShiftRemarkRuntime.key(evidence), evidence);
        }

        /**
         * 接收已被选择的计划附件，未选中的计划不调用。
         *
         * @param evidence 最终采用的不可变附件
         */
        public void include(List<LhShiftDelayEvidence> evidence) {
            evidence.forEach(this::add);
        }

        /**
         * 将已确定历史组合的真实拒绝交回外层任务，普通淘汰候选不得调用。
         */
        public void retainInParent() {
            if (parent != null) {
                parent.include(this.freeze());
            }
        }

        /**
         * 冻结当前局部附件供候选计划携带。
         *
         * @return 不可变局部证据
         */
        public List<LhShiftDelayEvidence> freeze() {
            return evidenceMap == null || evidenceMap.isEmpty() ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(evidenceMap.values()));
        }

        /**
         * 仅绑定本次真实新增、同物料状态的有效结果，失败调用不发布。
         * 成对结果属于同次整机动作时，证据范围提升为整机以同时核对两侧占用。
         *
         * @param context 正式提交后的上下文
         * @param firstResult 本次动作前结果数
         */
        public void accept(LhScheduleContext context, int firstResult) {
            List<LhScheduleResult> results = context.getScheduleResultList();
            List<LhShiftDelayEvidence> evidence = this.freeze();
            if (evidence.isEmpty()) {
                return;
            }
            List<LhScheduleResult> acceptedResults = results.subList(firstResult, results.size()).stream()
                    .filter(this::matchesResult).collect(java.util.stream.Collectors.toList());
            boolean wholeMachine = machineCode != null && acceptedResults.stream()
                    .map(LhScheduleResult::getLhMachineCode).distinct().limit(WHOLE_MACHINE_SIDE_COUNT).count() >= WHOLE_MACHINE_SIDE_COUNT;
            if (wholeMachine) {
                // 同次整机提交的两个实际侧位共用整机作用域，最终必须核对两侧所有物料。
                evidence = evidence.stream().map(item -> new LhShiftDelayEvidence(item.getAttemptId(), item.getReason(),
                        item.getSource(), LhSingleControlMachineUtil.resolvePhysicalMachineCode(item.getMachineCode()),
                        item.getMaterialCode(), item.getProductStatus(), item.getBlockedFrom(), item.getBlockedUntil(), item.getFacts()))
                        .collect(java.util.stream.Collectors.toList());
                evidence = Collections.unmodifiableList(evidence);
            }
            for (LhScheduleResult result : acceptedResults) {
                runtime.resultEvidence.put(result, LhShiftRemarkRuntime.merge(runtime.resultEvidence.get(result), evidence));
            }
        }

        /**
         * 以同次任务的物料、状态及物理资源核对新结果，禁止跨任务接纳。
         *
         * @param result 本次新增结果
         * @return 是否属于当前成功任务且有正计划量
         */
        private boolean matchesResult(LhScheduleResult result) {
            return Objects.equals(materialCode, result.getMaterialCode())
                    && StringUtils.equals(StringUtils.defaultIfBlank(productStatus, FORMAL_PRODUCT_STATUS),
                    StringUtils.defaultIfBlank(result.getProductStatus(), FORMAL_PRODUCT_STATUS))
                    && (machineCode == null || Objects.equals(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode),
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                    && ShiftFieldUtil.resolveScheduledQty(result) > 0;
        }

        /**
         * 恢复外层收集域，未接纳附件随当前域释放。
         */
        @Override
        public void close() {
            runtime.activeScope = parent;
        }
    }
}
