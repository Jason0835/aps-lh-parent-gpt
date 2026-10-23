package com.zlt.aps.lh.service.impl;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.*;
import com.zlt.aps.lh.service.impl.LhCleaningPlanFinalizer.PlanOutputSupport;
import com.zlt.aps.lh.util.CleaningScheduleRuleUtil;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;
/**
 * 排程侧唯一清洗编排入口，组织初始化、只读候选、原资源时点、提交复核和最终输出。
 * 排量、扣账及容量区间算法仍由原生产链和公共内核执行。
 *
 * @author APS
 * @since 2026/10/19
 */
@Component
public class LhCleaningScheduleService {
    /**
     * 初始化类型解析组件。
     */
    @Resource
    private LhCleaningPlanResolver planResolver = new LhCleaningPlanResolver();
    /**
     * 只读组合与时间解析组件。
     */
    @Resource
    private LhCleaningOverlapResolver overlapResolver = new LhCleaningOverlapResolver();
    /**
     * 清洗最终输出组件。
     */
    @Resource
    private LhCleaningPlanFinalizer finalizer = new LhCleaningPlanFinalizer();
    /**
     * 已落实同物料喷砂置换与精度的专用组合策略。
     */
    @Resource
    private LhSandBlastPrecisionOverlapPolicy precisionOverlapPolicy = new LhSandBlastPrecisionOverlapPolicy();
    /**
     * 原喷砂资源算法，运行态仍保存在当前上下文。
     */
    private final ContinuationSandBlastDispositionService sandBlastDispositionService =
            new ContinuationSandBlastDispositionService();
    /**
     * 在原初始化位置发布清洗约束并剥离普通停机中的清洗来源。
     *
     * @param context 当前批次
     * @return 机台清洗窗口
     */
    public Map<String, List<MachineCleaningWindowDTO>> initializePlans(LhScheduleContext context) {
        return planResolver.initializePlans(context);
    }
    /**
     * 兼容原初始化调用，业务只由公共初始化组件执行。
     *
     * @param context 当前批次
     * @return 机台清洗窗口
     */
    public Map<String, List<MachineCleaningWindowDTO>> buildScheduledCleaningWindowMap(LhScheduleContext context) {
        return this.initializePlans(context);
    }
    /**
     * 获取仍须执行的在机清洗，离机事实通过统一结果另行返回。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @return 当前有效在机窗口
     */
    public List<MachineCleaningWindowDTO> resolveMachineCleaningWindowList(
            LhScheduleContext context, String machineCode) {
        return overlapResolver.resolveMachineWindows(context, machineCode);
    }
    /**
     * 读取原续作物料的容量窗口，离机喷砂移除后仍扣除新模具置换准备及首检时间。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @return 普通清洗及同 SKU 置换的机台容量窗口
     */
    public List<MachineCleaningWindowDTO> resolveMachineCapacityWindowList(LhScheduleContext context,
            String machineCode) {
        MachineCleaningWindowDTO replacement = context.getContinuationSandBlastWindowMap().get(machineCode);
        return overlapResolver.appendReplacementCapacityWindow(context, machineCode,
                replacement == null ? null : replacement.getContinuationMaterialCode(),
                replacement == null ? null : replacement.getContinuationProductStatus(),
                this.resolveMachineCleaningWindowList(context, machineCode));
    }
    /**
     * 只读解析结果场景，容量窗口过滤与事件处置独立。
     *
     * @param context 当前批次
     * @param result 候选或实际结果
     * @param firstProduction 首个有量班次起点
     * @param typeBlock 是否使用换活字块时长
     * @return 只读清洗结果
     */
    public CleaningPlanResult resolveForScheduling(LhScheduleContext context, LhScheduleResult result,
            Date firstProduction, boolean typeBlock) {
        return overlapResolver.resolveForResult(context, result, firstProduction, typeBlock);
    }
    /**
     * 只读解析新增候选，预判收尾仅影响容量输入，不能取消真实清洗事件。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @param sku 候选物料
     * @param switchStart 切换开始
     * @param firstProduction 原开产时点
     * @return 各事件及当前容量窗口
     */
    public CleaningPlanResult resolveForScheduling(LhScheduleContext context, String machineCode,
            SkuScheduleDTO sku, Date switchStart, Date firstProduction) {
        CleaningPlanResult cleaning = overlapResolver.resolveForScheduling(context, machineCode, switchStart,
                firstProduction, CleaningScheduleRuleUtil.shouldSkipCleaningBySkuEnding(context, sku));
        MachineCleaningWindowDTO replacement = context.getContinuationSandBlastWindowMap().get(machineCode);
        if (replacement == null || replacement.getReplacementReadyTime() == null) {
            return cleaning;
        }
        List<MachineCleaningWindowDTO> original = cleaning.getEffectiveWindows();
        List<MachineCleaningWindowDTO> capacity = overlapResolver.appendReplacementCapacityWindow(context,
                machineCode, sku == null ? null : sku.getMaterialCode(),
                sku == null ? null : sku.getProductStatus(), original);
        return capacity == original ? cleaning : new CleaningPlanResult(cleaning.getEvents(), capacity);
    }
    /**
     * 读取原换活字块与清洗实际相交时的恢复时点。
     *
     * @param context 当前批次
     * @param machine 运行侧机台
     * @param switchStart 切换开始
     * @param productionStart 原开产时点
     * @return 原规则确定的开产时点
     */
    public Date resolveTypeBlockProductionStartTime(LhScheduleContext context, MachineScheduleDTO machine,
            Date switchStart, Date productionStart) {
        return overlapResolver.resolveTypeBlockProductionStartTime(context, machine, switchStart, productionStart);
    }
    /**
     * 在续作产能计算前落实喷砂资源及符合资格的置换精度组合，不能延迟到最终输出。
     *
     * @param context 当前批次
     */
    public void prepareBeforeContinuous(LhScheduleContext context) {
        sandBlastDispositionService.prepare(context);
        precisionOverlapPolicy.prepare(context);
        this.publishCurrentEvents(context);
    }
    /**
     * 在续作原扣账前后撤销未达到触发点的置换及关联精度，恢复原时间并释放额度。
     *
     * @param context 当前批次
     */
    public void cancelUnreachedReplacements(LhScheduleContext context) {
        sandBlastDispositionService.cancelUnreachedReplacements(context);
        precisionOverlapPolicy.reconcile(context);
        this.publishCurrentEvents(context);
    }
    /**
     * 由生产主链确认下机后应用离机资源边界，数量扣账仍归生产主链。
     *
     * @param context 当前批次
     * @param machine 原机台
     * @param source 原清洗窗口
     * @param event 已确认离机事件
     */
    public void detach(LhScheduleContext context, MachineScheduleDTO machine,
            MachineCleaningWindowDTO source, MachineCleaningWindowDTO event) {
        sandBlastDispositionService.detach(context, machine, source, event);
        context.getCleaningPlanRuntimeState().apply(
                overlapResolver.resolveEvent(context, machine.getMachineCode(), event));
        this.publishCurrentEvents(context);
    }
    /**
     * 正式结果提交后按现有算法恢复已取消清洗的模具边界，供下一候选即时读取。
     * 重复复核同一事实不重复发布清洗状态，也不消费数量或补排名额。
     *
     * @param context 当前正式结果上下文
     */
    public void reviewCommittedResults(LhScheduleContext context) {
        sandBlastDispositionService.reconcileCancelledEvents(context);
        precisionOverlapPolicy.reconcile(context);
        this.publishCurrentEvents(context);
    }
    /**
     * 将当前实际事实发布到单一运行态；候选评估不调用本方法。
     *
     * @param context 当前批次
     */
    private void publishCurrentEvents(LhScheduleContext context) {
        for (Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO> item
                : finalizer.collectCleaningPlanItems(context)) {
            String machine = finalizer.resolveCleaningMachineCode(item.getKey(), item.getValue());
            context.getCleaningPlanRuntimeState().apply(overlapResolver.resolveEvent(context, machine,
                    item.getValue()));
        }
    }
    /**
     * 在原最终摘要位置追加喷砂置换说明。
     *
     * @param context 当前批次
     */
    public void appendReplacementAnalysis(LhScheduleContext context) {
        sandBlastDispositionService.appendReplacementAnalysis(context);
    }
    /**
     * 核验主链已经分配的喷砂首检并绑定摘要，不新增数量、不从其他班次挪量。
     * <p>清洗、首检和正常生产的真实结束时间已由共享时间内核计算；这里只将首个
     * 有量班次的展示起点对齐到首检起点，并记录可对账的数量与时间。</p>
     *
     * @param context 排程上下文
     */
    public void applySandBlastFirstInspectionToFinalResults(LhScheduleContext context) {
        finalizer.applySandBlastFirstInspectionToFinalResults(context);
    }
    /**
     * 按机台、实际交接时间及清洗类型识别本次已执行的离机清洗计划。
     *
     * @param context 排程上下文
     * @param plan 待保存交替计划
     * @return 对应清洗事件；普通交替计划返回 null
     */
    public MachineCleaningWindowDTO resolveOffMachineCleaningEvent(LhScheduleContext context, LhMouldChangePlan plan) {
        return finalizer.resolveOffMachineCleaningEvent(context, plan);
    }
    /**
     * 判断交替计划是否必须保留喷砂交接中的精确模具。
     *
     * @param context 排程上下文
     * @param plan 当前交替计划
     * @param productStatus 后料产品状态
     * @return true-交接事件或释放后正规换模，禁止派生模具号
     */
    public boolean isSandBlastExactMouldPlan(
            LhScheduleContext context, LhMouldChangePlan plan, String productStatus) {
        return finalizer.isSandBlastExactMouldPlan(context, plan, productStatus);
    }
    /**
     * 解析喷砂交接计划类型，置换为换模，原模具离机清洗为喷砂。
     *
     * @param event 已确认交接事件
     * @return 交替类型编码
     */
    public String resolveSandBlastPlanType(MachineCleaningWindowDTO event) {
        return finalizer.resolveSandBlastPlanType(event);
    }
    /**
     * 解析交接计划模具号，置换记录新模具，喷砂清洗记录固定为空。
     *
     * @param event 已确认交接事件
     * @return 精确模具号
     */
    public String resolveSandBlastPlanMouldCode(MachineCleaningWindowDTO event) {
        return finalizer.resolveSandBlastPlanMouldCode(event);
    }
    /**
     * 对仅喷砂释放原机台的实际交替计划补充备注并清空模具号。
     *
     * @param context 排程上下文
     * @param plan 当前实际交替计划
     */
    public void applyOnlySandBlastSourcePlanPresentation(
            LhScheduleContext context, LhMouldChangePlan plan) {
        finalizer.applyOnlySandBlastSourcePlanPresentation(context, plan);
    }
    /**
     * 识别仅喷砂释放原机台在触发后的首类实际交替计划。
     *
     * @param context 排程上下文
     * @param plan 待判断计划
     * @return 对应离机喷砂事件；非原机交替返回null
     */
    public MachineCleaningWindowDTO resolveOnlySandBlastSourcePlanEvent(
            LhScheduleContext context, LhMouldChangePlan plan) {
        return finalizer.resolveOnlySandBlastSourcePlanEvent(context, plan);
    }
    /**
     * 识别仅喷砂释放 SKU 在其他机台形成的实际接收计划。
     *
     * @param context 排程上下文
     * @param plan 待判断计划
     * @param productStatus 后物料产品状态
     * @return 实际模具整组匹配的离机喷砂事件；普通同物料计划返回null
     */
    public MachineCleaningWindowDTO resolveOnlySandBlastReceivingPlanEvent(
            LhScheduleContext context, LhMouldChangePlan plan, String productStatus) {
        return finalizer.resolveOnlySandBlastReceivingPlanEvent(context, plan, productStatus);
    }
    /**
     * 基于清洗窗口追加模具清洗交替计划。
     *
     * @param context 排程上下文
     * @param plans 模具交替计划列表
     * @param planOrder 当前计划顺序
     * @param outputSupport 原交替计划公共字段及对账适配
     * @param planProductStatusMap 交替计划对应产品状态
     * @return 下一个计划顺序
     */
    public int appendCleaningMouldChangePlans(LhScheduleContext context,
                                               List<LhMouldChangePlan> plans,
                                               int planOrder,
                                               Map<LhMouldChangePlan, String> planProductStatusMap,
                                               PlanOutputSupport outputSupport) {
        return finalizer.appendCleaningMouldChangePlans(context, plans, planOrder, outputSupport, planProductStatusMap);
    }
    /**
     * 按最终排程结果重建清洗处置与设备停机计划日期回填项。
     * <p>初始化阶段只负责生成候选窗口和保留来源计划信息；最终阶段统一执行
     * “三天内收尾取消 > 正规换模取消 > 独立清洗”的优先级，并将回填日期统一归零到自然日。</p>
     *
     * @param context 排程上下文
     */
    public void finalizeCleaningDisposition(LhScheduleContext context) {
        this.finalizeArrangements(context);
    }
    /**
     * 在原终裁时点生成清洗最终输出，并发布最终实际状态。
     *
     * @param context 最终结果上下文
     */
    public void finalizeArrangements(LhScheduleContext context) {
        finalizer.finalizeCleaningDisposition(context);
        if (context != null) {
            this.publishCurrentEvents(context);
        }
    }
    /**
     * 诊断周日手工喷砂是否满足交替计划条数阈值。
     *
     * @param context 排程上下文
     */
    public void validateManualSundaySandBlastThreshold(LhScheduleContext context) {
        finalizer.validateManualSundaySandBlastThreshold(context);
    }
    /**
     * 在自动排程保存前拆分交替计划类型，纯清洗不补造换模动作。
     *
     * @param context 已完成模具号与备注处理的排程上下文
     */
    public void splitMouldChangePlanCleaningTypes(LhScheduleContext context) {
        finalizer.splitMouldChangePlanCleaningTypes(context);
    }
    /**
     * 判断是否为清洗类交替计划。
     *
     * @param plan 模具交替计划
     * @return true-清洗类；false-非清洗类
     */
    public boolean isCleaningMouldChangePlan(LhMouldChangePlan plan) {
        return finalizer.isCleaningMouldChangePlan(plan);
    }
    /**
     * 收集清洗计划项。
     *
     * @param context 排程上下文
     * @return 清洗计划项
     */
    public List<Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO>> collectCleaningPlanItems(
            LhScheduleContext context) {
        return finalizer.collectCleaningPlanItems(context);
    }
    /**
     * 解析清洗计划对应机台。
     *
     * @param machine 机台
     * @param cleaningWindow 清洗窗口
     * @return 机台编码
     */
    public String resolveCleaningMachineCode(MachineScheduleDTO machine, MachineCleaningWindowDTO cleaningWindow) {
        return finalizer.resolveCleaningMachineCode(machine, cleaningWindow);
    }
    /**
     * 解析清洗交替类型。
     *
     * @param cleaningWindow 清洗窗口
     * @return 模具交替类型
     */
    public String resolveCleaningMouldChangeType(MachineCleaningWindowDTO cleaningWindow) {
        return finalizer.resolveCleaningMouldChangeType(cleaningWindow);
    }
    /**
     * 命中清洗与换活字块组合场景时，写入最后一个重叠班次的原因分析。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 班次
     */
    public void applyTypeBlockCleaningAnalysis(LhScheduleContext context,
                                                LhScheduleResult result,
                                                List<LhShiftConfigVO> shifts) {
        finalizer.applyTypeBlockCleaningAnalysis(context, result, shifts);
    }
    /**
     * 干冰清洗与续作收尾重叠时，写入最后一个重叠班次原因。
     *
     * @param result 续作排程结果
     * @param shifts 排程窗口班次
     * @param cleaningWindowList 机台清洗窗口
     * @param isEnding 是否收尾
     */
    public void applyDryIceCleaningEndingAnalysis(LhScheduleResult result,
                                                   List<LhShiftConfigVO> shifts,
                                                   List<MachineCleaningWindowDTO> cleaningWindowList,
                                                   boolean isEnding) {
        finalizer.applyDryIceCleaningEndingAnalysis(result, shifts, cleaningWindowList, isEnding);
    }
}
