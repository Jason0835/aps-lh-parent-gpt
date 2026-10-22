package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 精确历史交替后料的普通结构机台上限豁免规则。
 * 只解析当前执行身份，不登记机台、不扣除统计，也不豁免结构切换专用名额。
 *
 * @author APS
 * @date 2026/10/21
 */
@Slf4j
public final class PreviousAlternateStructureLimitExemptionPolicy {

    /**
     * 禁止实例化无状态规则工具。
     */
    private PreviousAlternateStructureLimitExemptionPolicy() {
    }

    /**
     * 解析当前局部历史组合或强制历史事件对应的有效源计划。
     * 不扫描其他历史关系，防止普通候选借用同SKU的历史资格。
     *
     * @param context 当前排程上下文
     * @param sku 当前后物料及产品状态
     * @param machineCode 本次校验的运行态机台
     * @return 精确命中的有效源计划；未命中返回null
     */
    public static LhMouldChangePlan resolvePlan(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode) {
        if (Objects.isNull(context) || Objects.isNull(sku) || StringUtils.isBlank(machineCode)) {
            return null;
        }
        Scope scope = context.getActivePreviousAlternateStructureLimitScope();
        LhMouldChangePlan plan;
        if (Objects.nonNull(scope)) {
            // 空身份也是明确的作用域边界，不能回退借用外层活动事件。
            plan = scope.plan;
        } else {
            PreviousAlternatePlanReleaseEvent event = context.getActivePreviousAlternateEvent();
            if (Objects.isNull(event) || Objects.isNull(event.getPlan())
                    || !StringUtils.equals(event.getMachineCode(), event.getPlan().getLhMachineCode())) {
                return null;
            }
            plan = event.getPlan();
        }
        if (Objects.isNull(plan)
                || !context.isEligiblePreviousAlternateCombination(sku, plan, plan.getLhMachineCode())) {
            return null;
        }
        String specifiedMachine = plan.getLhMachineCode();
        // 单侧计划只匹配本侧；历史源为整机时才允许沿用原整组声明的运行侧。
        boolean sameMachine = StringUtils.equals(specifiedMachine, machineCode)
                || (StringUtils.equals(specifiedMachine,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(specifiedMachine))
                && StringUtils.equals(specifiedMachine,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode)));
        return sameMachine ? plan : null;
    }

    /**
     * 为已确认的历史执行组合建立局部普通结构上限身份，退出时恢复外层计划。
     *
     * @param context 当前排程上下文
     * @param sku 已通过历史候选资格的后料
     * @param plan 本轮已冻结的有效源计划
     * @param machineCode 源计划指定的运行态机台
     * @return 自动恢复外层身份的局部作用域
     */
    public static Scope openScope(LhScheduleContext context, SkuScheduleDTO sku,
            LhMouldChangePlan plan, String machineCode) {
        return new Scope(context, context.isEligiblePreviousAlternateCombination(sku, plan, machineCode)
                ? plan : null);
    }

    /**
     * 在成功结果完成原索引刷新后记录各实际占用班次的累计数量。
     * 关联预演单独标识，日志不修改结果、统计或资源。
     *
     * @param context 已完成本次承接提交的上下文
     * @param plan 结果对应的历史源计划
     * @param result 本次新增且已关联历史来源的结果
     */
    public static void logCommittedStatistics(LhScheduleContext context, LhMouldChangePlan plan,
            LhScheduleResult result) {
        if (Objects.isNull(context.getStructureShiftInMachineIndex())) {
            return;
        }
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if (!FirstInspectionQtyUtil.hasProductionOccupation(context, result, shift.getShiftIndex())) {
                continue;
            }
            log.info("历史交替后料结构机台累计, factoryCode: {}, batchNo: {}, sourceBatchNo: {}, planId: {}, "
                            + "materialCode: {}, productStatus: {}, machineCode: {}, physicalMachineCode: {}, "
                            + "structureName: {}, workDate: {}, shiftIndex: {}, rawMachineCount: {}, "
                            + "effectiveMachineCount: {}, groupPreview: {}, timedMachineOffPreview: {}",
                    context.getFactoryCode(), context.getBatchNo(), plan.getLhResultBatchNo(), plan.getId(),
                    result.getMaterialCode(), result.getProductStatus(), result.getLhMachineCode(),
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()),
                    result.getStructureName(), shift.getWorkDate(), shift.getShiftIndex(),
                    context.getStructureShiftInMachineIndex().resolveRawInMachinePhysicalCodes(
                            result.getStructureName(), shift.getShiftIndex()).size(),
                    context.getStructureShiftInMachineIndex().resolveInMachineCount(
                            result.getStructureName(), shift.getShiftIndex()), context.isPreviousAlternateGroupPreview(),
                    Objects.nonNull(context.getTimedMachineOffActionPreviewCapture()));
        }
    }

    /**
     * 只保存调用作用域的源计划，不纳入资源快照或共享资格缓存。
     * 内层提交快照恢复后仍由本作用域关闭动作恢复外层身份。
     *
     * @author APS
     * @date 2026/10/21
     */
    public static final class Scope implements AutoCloseable {

        /**
         * 当前排程上下文。
         */
        private final LhScheduleContext context;

        /**
         * 本次确认的历史源计划；未命中的组合为空。
         */
        private final LhMouldChangePlan plan;

        /**
         * 进入本层前的局部作用域，支持嵌套及明确的空身份边界。
         */
        private final Scope previousScope;

        /**
         * 保存外层计划并建立本次身份，未命中的组合不继承外层局部计划。
         *
         * @param context 当前排程上下文
         * @param plan 本次有效源计划；普通组合为空
         */
        private Scope(LhScheduleContext context, LhMouldChangePlan plan) {
            this.context = context;
            this.plan = plan;
            this.previousScope = context.getActivePreviousAlternateStructureLimitScope();
            context.setActivePreviousAlternateStructureLimitScope(this);
        }

        /**
         * 正常返回或异常退出时恢复进入本层前的局部源计划。
         */
        @Override
        public void close() {
            this.context.setActivePreviousAlternateStructureLimitScope(this.previousScope);
        }
    }
}
