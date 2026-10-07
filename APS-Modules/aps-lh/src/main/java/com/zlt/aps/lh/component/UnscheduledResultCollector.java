package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.api.enums.UnscheduledGroupTypeEnum;
import com.zlt.aps.lh.api.enums.UnscheduledReasonEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.ContinuationRemainderFinishGroup;
import com.zlt.aps.lh.engine.strategy.support.UnscheduledDemandSnapshot;
import com.zlt.aps.lh.engine.strategy.support.UnscheduledReasonEvent;
import com.zlt.aps.lh.engine.strategy.support.UnscheduledResultRuntime;
import com.zlt.aps.lh.util.MonthPlanDayQtyUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 硫化未排结果公共收集器。
 *
 * <p>组件本身不保存批次数据，全部需求快照和原因事件存放在本次
 * {@link LhScheduleContext}。组件只管理未排诊断，不参与排产准入、机台视角选SKU、
 * 候选排序、排量计算及任何资源扣账。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class UnscheduledResultCollector {

    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    private static final String AUTO_DATA_SOURCE = "0";
    private static final int NORMAL_DELETE_FLAG = 0;
    private static final int MAX_REASON_DETAIL_LENGTH = 2000;
    /** 续作阶段写入的临时零结果诊断，后续新增或换活字块可以继续满足同一需求。 */
    private static final String CONTINUATION_ZERO_PLAN_REASON = "续作结果裁剪为0";

    /**
     * 在月计划SKU进入目标量调整前登记原始需求快照。
     *
     * @param context 排程上下文
     * @param sku 原始SKU
     * @param sourcePlan 当前月计划来源记录
     */
    public void registerMonthlyDemand(LhScheduleContext context,
                                      SkuScheduleDTO sku,
                                      FactoryMonthPlanProductionFinalResult sourcePlan) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return;
        }
        String demandKey = this.buildMonthlyDemandKey(context, sku, sourcePlan);
        UnscheduledDemandSnapshot snapshot = new UnscheduledDemandSnapshot();
        snapshot.setDemandKey(demandKey);
        snapshot.setSourceType(StringUtils.defaultIfEmpty(
                sku.getSourceType(), SkuScheduleSourceTypeEnum.NORMAL_NEW_SPEC.getCode()));
        snapshot.setMaterialCode(sku.getMaterialCode());
        snapshot.setProductStatus(sku.getProductStatus());
        snapshot.setNormalProductionDate(this.resolveNormalProductionDate(
                context, sku, sourcePlan));
        snapshot.setOriginalDemandQty(Math.max(
                Math.max(0, sku.resolveTargetScheduleQty()),
                Math.max(0, sku.getOriginalWindowPlanQty())));
        snapshot.setEffectiveDemand(Objects.nonNull(snapshot.getNormalProductionDate())
                || snapshot.getOriginalDemandQty() > 0
                || sku.getFutureMonthPlanQtyAfterWindow() > 0);
        snapshot.setMonthPlanYear(sku.getMonthPlanYear());
        snapshot.setMonthPlanMonth(sku.getMonthPlanMonth());
        snapshot.setMonthPlanVersion(sku.getMonthPlanVersion());
        snapshot.setProductionVersion(sku.getProductionVersion());
        snapshot.setInitialSurplusQty(sku.getSurplusQty());
        this.registerSnapshot(context, sku, snapshot);
    }

    /**
     * 登记硫化日计划调整形成的独立需求。
     *
     * @param context 排程上下文
     * @param sku 日计划调整SKU
     */
    public void registerDayPlanAdjustDemand(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return;
        }
        LocalDate scheduleDate = this.toLocalDate(context.getScheduleDate());
        String demandKey = new StringBuilder(128)
                .append("DAY_PLAN_ADJUST|").append(context.getBatchNo()).append('|')
                .append(MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus())).append('|')
                .append(scheduleDate).toString();
        UnscheduledDemandSnapshot snapshot = new UnscheduledDemandSnapshot();
        snapshot.setDemandKey(demandKey);
        snapshot.setSourceType(SkuScheduleSourceTypeEnum.DAY_PLAN_ADJUST.getCode());
        snapshot.setMaterialCode(sku.getMaterialCode());
        snapshot.setProductStatus(sku.getProductStatus());
        snapshot.setNormalProductionDate(scheduleDate);
        snapshot.setEffectiveDemand(true);
        snapshot.setOriginalDemandQty(Math.max(0, sku.resolveTargetScheduleQty()));
        snapshot.setMonthPlanYear(sku.getMonthPlanYear());
        snapshot.setMonthPlanMonth(sku.getMonthPlanMonth());
        snapshot.setMonthPlanVersion(sku.getMonthPlanVersion());
        snapshot.setProductionVersion(sku.getProductionVersion());
        this.registerSnapshot(context, sku, snapshot);
    }

    /**
     * 将续作加机、换活字块回流和置换迁移等派生SKU绑定到原始需求。
     *
     * @param context 排程上下文
     * @param sourceSku 来源SKU
     * @param derivedSku 派生SKU
     */
    public void bindDerivedDemand(LhScheduleContext context,
                                  SkuScheduleDTO sourceSku,
                                  SkuScheduleDTO derivedSku) {
        if (Objects.isNull(context) || Objects.isNull(derivedSku)) {
            return;
        }
        // 后续补偿副本继承历史下机的精确时间，避免复制候选后只剩日期而提前重新上机。
        java.util.Date previousAlternateAvailableTime = context.getPreviousAlternateCandidateAvailableTimeMap().get(sourceSku);
        if (Objects.nonNull(previousAlternateAvailableTime)) {
            context.getPreviousAlternateCandidateAvailableTimeMap().put(derivedSku, previousAlternateAvailableTime);
        }
        UnscheduledDemandSnapshot sourceSnapshot = this.findSnapshot(context, sourceSku);
        if (Objects.isNull(sourceSnapshot)) {
            return;
        }
        /*
         * 续作加机、换活字块回流和置换迁移只是同一原始需求的执行形态，
         * 不得创建新的未排需求或用受限后的实际日期覆盖原正常开产日期。
         */
        context.getUnscheduledResultRuntime().getDemandKeyBySku()
                .put(derivedSku, sourceSnapshot.getDemandKey());
    }

    /**
     * 构建包含统一基础字段的未排结果。
     *
     * @param context 排程上下文
     * @param sku 来源SKU
     * @param unscheduledQty 未排数量
     * @param reasonCode 原因定义
     * @param reasonSummary 可读摘要；为空时使用枚举摘要
     * @param reasonDetail 原因详情
     * @return 未排结果
     */
    public LhUnscheduledResult buildResult(LhScheduleContext context,
                                           SkuScheduleDTO sku,
                                           int unscheduledQty,
                                           UnscheduledReasonEnum reasonCode,
                                           String reasonSummary,
                                           String reasonDetail) {
        LhUnscheduledResult result = new LhUnscheduledResult();
        result.setFactoryCode(context.getFactoryCode());
        result.setBatchNo(context.getBatchNo());
        result.setScheduleDate(context.getScheduleTargetDate());
        result.setMonthPlanVersion(sku.getMonthPlanVersion());
        result.setProductionVersion(sku.getProductionVersion());
        result.setMaterialCode(sku.getMaterialCode());
        result.setProductStatus(sku.getProductStatus());
        result.setMaterialDesc(sku.getMaterialDesc());
        result.setStructureName(sku.getStructureName());
        result.setMainMaterialDesc(sku.getMainMaterialDesc());
        result.setSpecCode(sku.getSpecCode());
        result.setSpecDesc(sku.getSpecDesc());
        result.setEmbryoCode(sku.getEmbryoCode());
        result.setMouldQty(sku.getMouldQty());
        result.setUnscheduledQty(Math.max(0, unscheduledQty));
        result.setDataSource(AUTO_DATA_SOURCE);
        result.setIsDelete(NORMAL_DELETE_FLAG);
        this.applyReason(result, reasonCode, reasonSummary, reasonDetail);
        this.attachDemand(context, sku, result);
        this.applyGroupType(context, result);
        return result;
    }

    /**
     * 追加最终未排记录并登记原因事件。
     *
     * @param context 排程上下文
     * @param sku 来源SKU
     * @param result 未排记录
     */
    public void add(LhScheduleContext context,
                    SkuScheduleDTO sku,
                    LhUnscheduledResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return;
        }
        this.attachDemand(context, sku, result);
        UnscheduledReasonEnum reasonCode = this.ensureReasonMetadata(result);
        this.recordReasonEvent(context, result.getRuntimeDemandKey(), reasonCode,
                result.getUnscheduledReasonDetail());
        this.applyGroupType(context, result);
        context.getUnscheduledResultList().add(result);
    }

    /**
     * 按原始需求替换未排投影，避免同一需求多副本重复记录。
     *
     * @param context 排程上下文
     * @param sku 来源SKU
     * @param result 新未排记录
     */
    public void addOrReplace(LhScheduleContext context,
                             SkuScheduleDTO sku,
                             LhUnscheduledResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return;
        }
        this.attachDemand(context, sku, result);
        String demandKey = result.getRuntimeDemandKey();
        List<LhUnscheduledResult> replacedResults = new ArrayList<LhUnscheduledResult>(2);
        for (LhUnscheduledResult existing : context.getUnscheduledResultList()) {
            if (Objects.isNull(existing)) {
                continue;
            }
            boolean sameDemand = StringUtils.isNotEmpty(demandKey)
                    ? StringUtils.equals(demandKey, existing.getRuntimeDemandKey())
                    : this.isSameSku(existing, result.getMaterialCode(), result.getProductStatus());
            if (sameDemand) {
                replacedResults.add(existing);
            }
        }
        replacedResults.forEach(existing -> this.discard(context, existing));
        this.add(context, sku, result);
    }

    /**
     * 清理指定需求的最终未排投影，供真实或虚拟机台完整解决需求时使用。
     *
     * @param context 排程上下文
     * @param sku 已解决SKU
     */
    public void remove(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return;
        }
        context.getUnscheduledResultList().removeIf(result ->
                Objects.nonNull(result) && this.isSameDemand(context, sku, result));
    }

    /**
     * 撤销候选试算期间生成的单条未排记录及其同源原因事件。
     *
     * @param context 排程上下文
     * @param result 需要撤销的试算记录
     */
    public void discard(LhScheduleContext context, LhUnscheduledResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return;
        }
        context.getUnscheduledResultList().remove(result);
        List<UnscheduledReasonEvent> events = context.getUnscheduledResultRuntime()
                .getReasonEventMap().get(result.getRuntimeDemandKey());
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        events.removeIf(event -> Objects.nonNull(event)
                && Objects.nonNull(event.getReasonCode())
                && StringUtils.equals(event.getReasonCode().getCode(), result.getUnscheduledReasonCode())
                && StringUtils.equals(event.getDetail(), result.getUnscheduledReasonDetail()));
    }

    /**
     * 判断未排记录是否属于指定原始需求。
     *
     * @param context 排程上下文
     * @param sku 来源SKU
     * @param result 未排记录
     * @return true-同一原始需求；false-不同需求
     */
    public boolean isSameDemand(LhScheduleContext context,
                                SkuScheduleDTO sku,
                                LhUnscheduledResult result) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(result)) {
            return false;
        }
        String demandKey = context.getUnscheduledResultRuntime().getDemandKeyBySku().get(sku);
        if (StringUtils.isNotEmpty(demandKey) && StringUtils.isNotEmpty(result.getRuntimeDemandKey())) {
            return StringUtils.equals(demandKey, result.getRuntimeDemandKey());
        }
        return this.isSameSku(result, sku);
    }

    /**
     * 按原始需求查找当前未排投影。
     *
     * @param context 排程上下文
     * @param sku 来源SKU
     * @return 同一需求的未排记录，不存在返回null
     */
    public LhUnscheduledResult find(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return null;
        }
        for (LhUnscheduledResult result : context.getUnscheduledResultList()) {
            if (this.isSameDemand(context, sku, result)) {
                return result;
            }
        }
        return null;
    }

    /**
     * 登记不立即形成最终未排的Machine×SKU尝试原因。
     *
     * @param context 排程上下文
     * @param sku 候选SKU
     * @param reasonCode 原因定义
     * @param detail 已执行分支产生的证据
     */
    public void recordAttempt(LhScheduleContext context,
                              SkuScheduleDTO sku,
                              UnscheduledReasonEnum reasonCode,
                              String detail) {
        UnscheduledDemandSnapshot snapshot = this.findSnapshot(context, sku);
        if (Objects.nonNull(snapshot)) {
            this.recordReasonEvent(context, snapshot.getDemandKey(), reasonCode, detail);
        }
    }

    /**
     * 更新已有未排记录的结构化原因并登记事件，未排数量继续由原业务分支维护。
     *
     * @param context 排程上下文
     * @param sku 来源SKU
     * @param result 已有未排记录
     * @param reasonCode 原因定义；为空时按原摘要转换
     * @param reasonSummary 可读摘要
     * @param reasonDetail 原分支证据
     */
    public void updateReason(LhScheduleContext context,
                             SkuScheduleDTO sku,
                             LhUnscheduledResult result,
                             UnscheduledReasonEnum reasonCode,
                             String reasonSummary,
                             String reasonDetail) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return;
        }
        UnscheduledReasonEnum effectiveReason = Objects.isNull(reasonCode)
                ? this.resolveReasonCode(reasonSummary) : reasonCode;
        this.attachDemand(context, sku, result);
        this.applyReason(result, effectiveReason, reasonSummary, reasonDetail);
        this.applyGroupType(context, result);
        this.recordReasonEvent(context, result.getRuntimeDemandKey(), effectiveReason, reasonDetail);
    }

    /**
     * 按原始需求归并阶段未排，保留后续裁量所需的数量诊断。
     *
     * <p>只整理诊断投影，不读取或改写机台、模具、胎胚、日计划及生产余量账本。</p>
     *
     * @param context 排程上下文
     */
    public void finalizeResults(LhScheduleContext context) {
        this.finalizeResults(context, false);
    }

    /**
     * 在S4.6全部数量调整结束后核对最终余量并归并未排，其他阶段不得调用。
     *
     * @param context 即将持久化的最终八班上下文
     */
    public void finalizeBeforePersistence(LhScheduleContext context) {
        // 指定试制量试不进入虚拟机，必须在全部后置裁量结束后按最终真实产量补齐未排。
        this.reconcilePreviousAlternateTrials(context);
        this.finalizeResults(context, true);
        // 先归并已有原因，再按最终八班结果补齐或清除本批余量收尾需求的真实缺口。
        this.reconcileRemainderFinishGroups(context);
    }

    /**
     * 对指定交替试制量试核对最终真实缺口；登记关系不代表已排成功。
     * @param context 已完成全部真实排产及后置裁量的上下文
     */
    private void reconcilePreviousAlternateTrials(LhScheduleContext context) {
        if (Objects.isNull(context)) {
            return;
        }
        for (Map.Entry<String, SkuScheduleDTO> entry : context.getPreviousAlternateTrialSkuMap().entrySet()) {
            SkuScheduleDTO sku = entry.getValue();
            int scheduledQty = context.getScheduleResultList().stream().filter(Objects::nonNull)
                    .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                            && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                    .mapToInt(ShiftFieldUtil::resolveScheduledQty).sum();
            Integer targetQty = context.getSkuProductionTargetQtyMap().get(entry.getKey());
            // 已建账需求以最终合法总目标减实际结果；尚未建账的拒绝需求复用中心只读余量口径。
            int remainingQty = Objects.nonNull(targetQty) ? Math.max(0, targetQty - scheduledQty)
                    : targetScheduleQtyResolver.previewProductionRemainingQty(context, sku);
            if (remainingQty <= 0) {
                // 实际完成才清除未排；无合法需求的零量准入诊断仍沿用原保留规则。
                if (scheduledQty > 0) {
                    this.remove(context, sku);
                }
                continue;
            }
            LhUnscheduledResult existing = this.find(context, sku);
            String failure = context.getPreviousAlternateTrialFailureMap().get(entry.getKey());
            UnscheduledReasonEnum reason = StringUtils.isNotEmpty(failure)
                    ? this.resolveReasonCode(failure) : UnscheduledReasonEnum.WINDOW_FINAL_UNSCHEDULED;
            if (StringUtils.isEmpty(failure) && Objects.nonNull(existing)) {
                // 日计划、减量等前置拒绝仍使用实际原因，禁止虚构模具或结构失败。
                failure = existing.getUnscheduledReason();
                reason = this.ensureReasonMetadata(existing);
            }
            String detail = String.format("前日指定交替试制量试不进入虚拟机, 工厂=%s, 批次=%s, "
                            + "物料=%s, 状态=%s, 真实已排=%s, 未排=%s, 指定承接证据=%s",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                    scheduledQty, remainingQty, StringUtils.defaultIfEmpty(failure, "排程窗口结束仍有未完成需求"));
            this.addOrReplace(context, sku, this.buildResult(context, sku, remainingQty,
                    reason, reason.getSummary(), StringUtils.left(detail, MAX_REASON_DETAIL_LENGTH)));
        }
    }

    /**
     * 按收尾提交时冻结的合法目标核对最终缺口，不修改生产、日计划或资源账本。
     * 同源未排沿用已有原因；后续补排完成时删除同源投影，其他独立需求保持不变。
     * @param context 全部数量调整完成、即将保存的排程上下文
     */
    private void reconcileRemainderFinishGroups(LhScheduleContext context) {
        if (Objects.isNull(context)) {
            return;
        }
        for (ContinuationRemainderFinishGroup group : context.getContinuationRemainderFinishGroups()) {
            if (Objects.isNull(group.getFinalTargetQty())) {
                continue;
            }
            SkuScheduleDTO sku = group.getSourceSku();
            int scheduledQty = context.getScheduleResultList().stream()
                    .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                            && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                    .mapToInt(ShiftFieldUtil::resolveScheduledQty).sum();
            int remainingQty = Math.max(0, group.getFinalTargetQty() - scheduledQty);
            String detail = String.format("工厂=%s, 批次=%s, 排程日期=%s, 物料=%s, 状态=%s, 合法目标=%s, 最终已排=%s, 未排=%s",
                    context.getFactoryCode(), context.getBatchNo(), context.getScheduleTargetDate(),
                    sku.getMaterialCode(), sku.getProductStatus(), group.getFinalTargetQty(), scheduledQty, remainingQty);
            if (remainingQty == 0) {
                // 只移除同一原始需求，不能影响同物料的其他状态或独立日调整需求。
                this.remove(context, sku);
            } else {
                LhUnscheduledResult result = this.find(context, sku);
                if (Objects.isNull(result)) {
                    // 本组合法窗口及数量约束下仍有缺口，复用窗口未完成原因，不猜测具体资源阻断。
                    result = this.buildResult(context, sku, remainingQty,
                            UnscheduledReasonEnum.WINDOW_FINAL_UNSCHEDULED, null, detail);
                    this.add(context, sku, result);
                } else if (!Objects.equals(result.getUnscheduledQty(), remainingQty)) {
                    // 数量未变化时保留原详情；保存前追加诊断仍遵守既有字段长度，避免超过数据库上限。
                    result.setUnscheduledQty(remainingQty);
                    this.updateReason(context, sku, result,
                            UnscheduledReasonEnum.fromCode(result.getUnscheduledReasonCode()),
                            result.getUnscheduledReason(), StringUtils.left(
                                    detail + "；原原因=" + result.getUnscheduledReasonDetail(), MAX_REASON_DETAIL_LENGTH));
                }
            }
            log.info("续作余量收尾最终缺口核对, {}", detail);
        }
    }

    /**
     * 复用阶段归并规则，仅保存前入口执行最终数量核对。
     *
     * @param context 排程上下文
     * @param reconcileFinalQty 是否已经完成全部后置数量调整
     */
    private void finalizeResults(LhScheduleContext context, boolean reconcileFinalQty) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getUnscheduledResultList())) {
            return;
        }
        Map<String, List<LhUnscheduledResult>> resultGroupMap =
                new LinkedHashMap<String, List<LhUnscheduledResult>>(context.getUnscheduledResultList().size());
        int fallbackSequence = 0;
        for (LhUnscheduledResult result : context.getUnscheduledResultList()) {
            if (Objects.isNull(result) || StringUtils.isEmpty(result.getMaterialCode())) {
                continue;
            }
            this.ensureReasonMetadata(result);
            this.attachDemand(context, null, result);
            // 核对续作零结果及单胎胚硬目标诊断；其他未排继续使用各自需求口径，不改生产账本。
            if (reconcileFinalQty && this.reconcileContinuationZeroPlan(context, result)) {
                continue;
            }
            this.applyGroupType(context, result);
            String demandKey = StringUtils.defaultIfEmpty(result.getRuntimeDemandKey(),
                    "UNRESOLVED|" + result.getMaterialCode() + '|'
                            + StringUtils.trimToEmpty(result.getProductStatus()) + '|'
                            + (++fallbackSequence));
            resultGroupMap.computeIfAbsent(
                    demandKey, key -> new ArrayList<LhUnscheduledResult>(2)).add(result);
        }
        List<LhUnscheduledResult> finalizedResults =
                new ArrayList<LhUnscheduledResult>(resultGroupMap.size());
        for (Map.Entry<String, List<LhUnscheduledResult>> entry : resultGroupMap.entrySet()) {
            LhUnscheduledResult primaryResult = this.selectPrimaryResult(entry.getValue());
            if (Objects.isNull(primaryResult)) {
                continue;
            }
            UnscheduledReasonEnum primaryReason = UnscheduledReasonEnum.fromCode(
                    primaryResult.getUnscheduledReasonCode());
            UnscheduledReasonEnum finalReason = this.selectPrimaryReason(
                    context, entry.getKey(), primaryReason);
            int finalQty = entry.getValue().stream()
                    .map(LhUnscheduledResult::getUnscheduledQty)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max().orElse(0);
            if (finalQty <= 0 && !finalReason.isRetainZeroQty()) {
                continue;
            }
            primaryResult.setUnscheduledQty(Math.max(0, finalQty));
            if (finalReason != primaryReason) {
                primaryResult.setUnscheduledReason(finalReason.getSummary());
            }
            primaryResult.setUnscheduledReasonCode(finalReason.getCode());
            primaryResult.setUnscheduledReasonStage(finalReason.getStage());
            primaryResult.setUnscheduledReasonDetail(this.buildFinalReasonDetail(
                    context, entry.getKey(), finalReason, primaryResult, entry.getValue()));
            finalizedResults.add(primaryResult);
        }
        context.setUnscheduledResultList(finalizedResults);
        Map<Integer, Long> groupCountMap = finalizedResults.stream().collect(
                java.util.stream.Collectors.groupingBy(
                        LhUnscheduledResult::getGroupType, LinkedHashMap::new,
                        java.util.stream.Collectors.counting()));
        log.info("硫化未排结果统一收口完成, factoryCode: {}, batchNo: {}, finalCount: {}, groupCount: {}",
                context.getFactoryCode(), context.getBatchNo(), finalizedResults.size(), groupCountMap);
    }

    /**
     * 用最终八班产量收敛续作零结果诊断，避免后续补排成功仍保留阶段未排量。
     *
     * @param context 全部排产及后置裁量已完成的上下文
     * @param result 待收口未排记录
     * @return 已无剩余需求、应移除此记录时返回true
     */
    private boolean reconcileContinuationZeroPlan(LhScheduleContext context, LhUnscheduledResult result) {
        UnscheduledDemandSnapshot snapshot = this.findSnapshot(context, result);
        if (Objects.isNull(snapshot)) {
            return false;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                result.getMaterialCode(), result.getProductStatus());
        Integer embryoTargetQty = this.resolveNonSharedEmbryoEndingDemandTarget(context, snapshot, skuKey);
        if (Objects.isNull(embryoTargetQty)
                && (!StringUtils.equals(CONTINUATION_ZERO_PLAN_REASON, result.getUnscheduledReason())
                || Objects.isNull(snapshot.getInitialSurplusQty()) || snapshot.getInitialSurplusQty() <= 0)) {
            return false;
        }
        long demandCount = context.getUnscheduledResultRuntime().getDemandSnapshotMap().values().stream()
                .filter(Objects::nonNull)
                .filter(demand -> StringUtils.equals(skuKey, MonthPlanDateResolver.buildMaterialStatusKey(
                        demand.getMaterialCode(), demand.getProductStatus())))
                .count();
        if (demandCount != 1) {
            // 同SKU存在独立月计划或日调整需求时，汇总产量无法唯一归属，禁止互相抵扣。
            return false;
        }
        int scheduledQty = context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(scheduled -> StringUtils.equals(skuKey, MonthPlanDateResolver.buildMaterialStatusKey(
                        scheduled.getMaterialCode(), scheduled.getProductStatus())))
                .mapToInt(ShiftFieldUtil::resolveScheduledQty).sum();
        int demandTargetQty = Objects.nonNull(embryoTargetQty) ? embryoTargetQty : snapshot.getInitialSurplusQty();
        int remainingQty = Math.max(0, demandTargetQty - scheduledQty);
        int originalQty = Objects.isNull(result.getUnscheduledQty()) ? 0 : result.getUnscheduledQty();
        if (originalQty > remainingQty) {
            result.setUnscheduledQty(remainingQty);
            log.info("续作或单胎胚未排按最终产量收口, factoryCode: {}, batchNo: {}, materialCode: {}, "
                            + "productStatus: {}, demandTargetQty: {}, scheduledQty: {}, beforeQty: {}, afterQty: {}",
                    context.getFactoryCode(), context.getBatchNo(), result.getMaterialCode(), result.getProductStatus(),
                    demandTargetQty, scheduledQty, originalQty, remainingQty);
        }
        return remainingQty == 0;
    }

    /**
     * 从同源SKU及补偿副本确认单胎胚收尾身份，读取已建立的合法硬目标，不按零月余量删除需求。
     * @param context 最终排产上下文
     * @param snapshot 原始需求快照
     * @param skuKey 物料及产品状态键
     * @return 单胎胚硬目标；其他需求返回null
     */
    private Integer resolveNonSharedEmbryoEndingDemandTarget(LhScheduleContext context,
            UnscheduledDemandSnapshot snapshot, String skuKey) {
        Integer targetQty = context.getSkuProductionTargetQtyMap().get(skuKey);
        if (Objects.isNull(targetQty)) {
            return null;
        }
        boolean exactEndingDemand = context.getUnscheduledResultRuntime().getDemandKeyBySku().entrySet().stream()
                .filter(entry -> StringUtils.equals(snapshot.getDemandKey(), entry.getValue()))
                .map(Map.Entry::getKey).filter(Objects::nonNull)
                .anyMatch(sku -> sku.isStrictTargetQty()
                        && targetScheduleQtyResolver.isNonSharedEmbryoStockEnding(context, sku));
        return exactEndingDemand ? Math.max(0, targetQty) : null;
    }

    private void registerSnapshot(LhScheduleContext context,
                                  SkuScheduleDTO sku,
                                  UnscheduledDemandSnapshot snapshot) {
        UnscheduledResultRuntime runtime = context.getUnscheduledResultRuntime();
        runtime.getDemandSnapshotMap().putIfAbsent(snapshot.getDemandKey(), snapshot);
        runtime.getDemandKeyBySku().put(sku, snapshot.getDemandKey());
    }

    private String buildMonthlyDemandKey(LhScheduleContext context,
                                         SkuScheduleDTO sku,
                                         FactoryMonthPlanProductionFinalResult plan) {
        return new StringBuilder(160)
                .append("MONTH_PLAN|").append(context.getBatchNo()).append('|')
                .append(Objects.isNull(plan) || Objects.isNull(plan.getId())
                        ? StringUtils.defaultString(sku.getMonthPlanVersion()) : plan.getId())
                .append('|').append(Objects.isNull(plan) ? sku.getMonthPlanYear() : plan.getYear())
                .append('|').append(Objects.isNull(plan) ? sku.getMonthPlanMonth() : plan.getMonth())
                .append('|').append(MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus())).toString();
    }

    private LocalDate resolveNormalProductionDate(LhScheduleContext context,
                                                  SkuScheduleDTO sku,
                                                  FactoryMonthPlanProductionFinalResult sourcePlan) {
        LocalDate sourceDate = this.resolveFirstUnmetPlanDate(
                sourcePlan, Math.max(0, sku.getFinishedQty()));
        if (Objects.nonNull(sourceDate)) {
            return sourceDate;
        }
        List<LocalDate> positiveDateList = new ArrayList<LocalDate>(16);
        for (FactoryMonthPlanProductionFinalResult plan : context.getLoadedMonthPlanList()) {
            if (!this.isSamePlanSku(plan, sku)
                    || this.isSameSourcePlan(plan, sourcePlan)) {
                continue;
            }
            this.collectPositiveDates(plan, positiveDateList);
        }
        positiveDateList.sort(Comparator.naturalOrder());
        return CollectionUtils.isEmpty(positiveDateList) ? null : positiveDateList.get(0);
    }

    /**
     * 按来源计划累计量扣除已完成量，定位当前仍未满足需求对应的正常开产日期。
     *
     * @param plan 原始月计划记录
     * @param finishedQty 已完成量
     * @return 首个未满足计划日期；来源计划已完成时返回null
     */
    private LocalDate resolveFirstUnmetPlanDate(
            FactoryMonthPlanProductionFinalResult plan,
            int finishedQty) {
        if (Objects.isNull(plan) || Objects.isNull(plan.getYear())
                || Objects.isNull(plan.getMonth()) || plan.getMonth() < 1 || plan.getMonth() > 12) {
            return null;
        }
        YearMonth yearMonth = YearMonth.of(plan.getYear(), plan.getMonth());
        int remainingFinishedQty = Math.max(0, finishedQty);
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            int dayPlanQty = Math.max(0, MonthPlanDayQtyUtil.resolveDayQty(plan, day));
            if (dayPlanQty <= 0) {
                continue;
            }
            if (remainingFinishedQty >= dayPlanQty) {
                remainingFinishedQty -= dayPlanQty;
                continue;
            }
            return yearMonth.atDay(day);
        }
        return null;
    }

    private boolean isSameSourcePlan(FactoryMonthPlanProductionFinalResult left,
                                     FactoryMonthPlanProductionFinalResult right) {
        if (left == right) {
            return true;
        }
        return Objects.nonNull(left) && Objects.nonNull(right)
                && ((Objects.nonNull(left.getId()) && Objects.nonNull(right.getId())
                && Objects.equals(left.getId(), right.getId()))
                || (Objects.equals(left.getYear(), right.getYear())
                && Objects.equals(left.getMonth(), right.getMonth())
                && StringUtils.equals(left.getMaterialCode(), right.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(left.getProductStatus()),
                StringUtils.trimToEmpty(right.getProductStatus()))
                && StringUtils.equals(left.getMonthPlanVersion(), right.getMonthPlanVersion())
                && StringUtils.equals(left.getProductionVersion(), right.getProductionVersion())));
    }

    private void collectPositiveDates(FactoryMonthPlanProductionFinalResult plan,
                                      List<LocalDate> target) {
        if (Objects.isNull(plan) || Objects.isNull(plan.getYear())
                || Objects.isNull(plan.getMonth()) || plan.getMonth() < 1 || plan.getMonth() > 12) {
            return;
        }
        YearMonth yearMonth = YearMonth.of(plan.getYear(), plan.getMonth());
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            if (MonthPlanDayQtyUtil.resolveDayQty(plan, day) > 0) {
                target.add(yearMonth.atDay(day));
            }
        }
    }

    private boolean isSamePlanSku(FactoryMonthPlanProductionFinalResult plan, SkuScheduleDTO sku) {
        return Objects.nonNull(plan)
                && StringUtils.equals(plan.getMaterialCode(), sku.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(plan.getProductStatus()),
                StringUtils.trimToEmpty(sku.getProductStatus()));
    }

    private void attachDemand(LhScheduleContext context,
                              SkuScheduleDTO sku,
                              LhUnscheduledResult result) {
        if (StringUtils.isNotEmpty(result.getRuntimeDemandKey())) {
            return;
        }
        UnscheduledDemandSnapshot snapshot = this.findSnapshot(context, sku);
        if (Objects.isNull(snapshot)) {
            snapshot = this.findSnapshot(context, result);
        }
        if (Objects.nonNull(snapshot)) {
            result.setRuntimeDemandKey(snapshot.getDemandKey());
        }
    }

    private UnscheduledDemandSnapshot findSnapshot(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return null;
        }
        String demandKey = context.getUnscheduledResultRuntime().getDemandKeyBySku().get(sku);
        return context.getUnscheduledResultRuntime().getDemandSnapshotMap().get(demandKey);
    }

    private UnscheduledDemandSnapshot findSnapshot(LhScheduleContext context,
                                                    LhUnscheduledResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return null;
        }
        if (StringUtils.isNotEmpty(result.getRuntimeDemandKey())) {
            UnscheduledDemandSnapshot directSnapshot = context.getUnscheduledResultRuntime()
                    .getDemandSnapshotMap().get(result.getRuntimeDemandKey());
            if (Objects.nonNull(directSnapshot)) {
                return directSnapshot;
            }
        }
        List<UnscheduledDemandSnapshot> candidates = new ArrayList<UnscheduledDemandSnapshot>(2);
        for (UnscheduledDemandSnapshot snapshot
                : context.getUnscheduledResultRuntime().getDemandSnapshotMap().values()) {
            if (Objects.nonNull(snapshot)
                    && StringUtils.equals(snapshot.getMaterialCode(), result.getMaterialCode())
                    && StringUtils.equals(StringUtils.trimToEmpty(snapshot.getProductStatus()),
                    StringUtils.trimToEmpty(result.getProductStatus()))) {
                candidates.add(snapshot);
            }
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.stream()
                .filter(snapshot -> StringUtils.equals(
                        snapshot.getMonthPlanVersion(), result.getMonthPlanVersion()))
                .filter(snapshot -> StringUtils.equals(
                        snapshot.getProductionVersion(), result.getProductionVersion()))
                .findFirst().orElse(null);
    }

    private void applyGroupType(LhScheduleContext context, LhUnscheduledResult result) {
        UnscheduledDemandSnapshot snapshot = this.findSnapshot(context, result);
        LocalDate windowEndDate = this.toLocalDate(context.getWindowEndDate());
        boolean inWindow = Objects.nonNull(snapshot) && snapshot.isEffectiveDemand()
                && Objects.nonNull(snapshot.getNormalProductionDate())
                && Objects.nonNull(windowEndDate)
                && !snapshot.getNormalProductionDate().isAfter(windowEndDate);
        result.setGroupType(inWindow
                ? UnscheduledGroupTypeEnum.IN_SCHEDULE_WINDOW.getValue()
                : UnscheduledGroupTypeEnum.OTHER.getValue());
    }

    private void applyReason(LhUnscheduledResult result,
                             UnscheduledReasonEnum reasonCode,
                             String reasonSummary,
                             String reasonDetail) {
        UnscheduledReasonEnum effectiveReason = Objects.isNull(reasonCode)
                ? UnscheduledReasonEnum.OTHER : reasonCode;
        result.setUnscheduledReasonCode(effectiveReason.getCode());
        result.setUnscheduledReasonStage(effectiveReason.getStage());
        result.setUnscheduledReason(StringUtils.defaultIfEmpty(
                reasonSummary, effectiveReason.getSummary()));
        result.setUnscheduledReasonDetail(reasonDetail);
    }

    private UnscheduledReasonEnum ensureReasonMetadata(LhUnscheduledResult result) {
        UnscheduledReasonEnum reasonCode = StringUtils.isNotEmpty(result.getUnscheduledReasonCode())
                ? UnscheduledReasonEnum.fromCode(result.getUnscheduledReasonCode())
                : this.resolveReasonCode(result.getUnscheduledReason());
        result.setUnscheduledReasonCode(reasonCode.getCode());
        result.setUnscheduledReasonStage(reasonCode.getStage());
        if (StringUtils.isEmpty(result.getUnscheduledReason())) {
            result.setUnscheduledReason(reasonCode.getSummary());
        }
        if (StringUtils.isEmpty(result.getUnscheduledReasonDetail())) {
            result.setUnscheduledReasonDetail(result.getUnscheduledReason());
        }
        return reasonCode;
    }

    /**
     * 将既有业务分支的可读原因映射为统一编码。
     *
     * <p>该方法只转换已经由原规则确定的原因，不重新执行或猜测排产规则。</p>
     *
     * @param reason 原业务原因
     * @return 统一原因定义
     */
    public UnscheduledReasonEnum resolveReasonCode(String reason) {
        if (StringUtils.isEmpty(reason)) {
            return UnscheduledReasonEnum.OTHER;
        }
        if (StringUtils.contains(reason, "减量清单")) {
            return UnscheduledReasonEnum.SKU_DECREMENT_HIT;
        }
        if (StringUtils.contains(reason, "排程窗口及提前生产范围内无日计划量")) {
            return UnscheduledReasonEnum.NO_DAILY_PLAN_IN_FULL_RANGE;
        }
        if (StringUtils.contains(reason, "窗口内无日计划")
                || StringUtils.contains(reason, "当前排程窗口无日计划")) {
            return UnscheduledReasonEnum.NO_DAILY_PLAN_IN_WINDOW;
        }
        if (StringUtils.contains(reason, "试制、量试月计划")
                || StringUtils.contains(reason, "新增排产试制量试不参与")
                || StringUtils.equals(reason, "试制量试当日不可排产")) {
            return UnscheduledReasonEnum.TRIAL_MASS_TRIAL_BLOCKED;
        }
        if (StringUtils.contains(reason, "小余量")) {
            return UnscheduledReasonEnum.SMALL_ENDING_SURPLUS;
        }
        if (StringUtils.contains(reason, "共用胎胚") && StringUtils.contains(reason, "余量为0")) {
            return UnscheduledReasonEnum.SHARED_EMBRYO_ALLOCATION_BLOCKED;
        }
        if (StringUtils.contains(reason, "余量为0且胎胚库存为0")) {
            return UnscheduledReasonEnum.ZERO_SURPLUS_AND_EMBRYO;
        }
        if (StringUtils.contains(reason, "没有排产目标量")) {
            return UnscheduledReasonEnum.NO_ORIGINAL_TARGET;
        }
        if (StringUtils.contains(reason, "同物料多状态续作跨窗口")) {
            return UnscheduledReasonEnum.CONTINUATION_LOCKED_REMAINING;
        }
        if (StringUtils.contains(reason, "双模SKU单控机台")) {
            return UnscheduledReasonEnum.SINGLE_CONTROL_CONTINUATION_BLOCKED;
        }
        if (StringUtils.contains(reason, "目标量被下调为0")) {
            return UnscheduledReasonEnum.TARGET_CLEARED_BY_RULE;
        }
        if (StringUtils.contains(reason, "胎胚") && StringUtils.contains(reason, "超出排程窗口")) {
            return UnscheduledReasonEnum.EMBRYO_AVAILABLE_OUT_OF_WINDOW;
        }
        if (StringUtils.contains(reason, "结构") && StringUtils.contains(reason, "机台")) {
            return UnscheduledReasonEnum.STRUCTURE_MACHINE_LIMIT;
        }
        if (StringUtils.contains(reason, "班次总量")
                || StringUtils.contains(reason, "总计划量限制")) {
            return UnscheduledReasonEnum.SHIFT_TOTAL_QTY_LIMIT;
        }
        if (StringUtils.contains(reason, "模具全部被占用")
                || StringUtils.contains(reason, "无空闲模具")) {
            return UnscheduledReasonEnum.NO_AVAILABLE_MOULD;
        }
        if (StringUtils.contains(reason, "模具数量不足")) {
            return UnscheduledReasonEnum.MOULD_QUANTITY_INSUFFICIENT;
        }
        if (StringUtils.contains(reason, "模具台账")) {
            return UnscheduledReasonEnum.MOULD_LEDGER_UNAVAILABLE;
        }
        if (StringUtils.contains(reason, "换模") && StringUtils.contains(reason, "次数")) {
            return UnscheduledReasonEnum.MOULD_CHANGE_EXCEEDED;
        }
        if (StringUtils.contains(reason, "切换完成时间超出当前业务日")) {
            return UnscheduledReasonEnum.CHANGEOVER_OUT_OF_BUSINESS_DAY;
        }
        if (StringUtils.contains(reason, "切换") && StringUtils.contains(reason, "超出排程窗口")) {
            return UnscheduledReasonEnum.CHANGEOVER_OUT_OF_WINDOW;
        }
        if (StringUtils.contains(reason, "剩余机台")
                || StringUtils.contains(reason, "已被") && StringUtils.contains(reason, "占用")) {
            return UnscheduledReasonEnum.NO_REMAINING_MATCHED_MACHINE;
        }
        if (StringUtils.contains(reason, "无可用硫化机台")
                || StringUtils.contains(reason, "无匹配")) {
            return UnscheduledReasonEnum.NO_HARD_MATCH_MACHINE;
        }
        if (StringUtils.contains(reason, "无可用产能")
                || StringUtils.contains(reason, "无可开产")
                || StringUtils.contains(reason, "裁剪为0")) {
            return UnscheduledReasonEnum.CAPACITY_INSUFFICIENT;
        }
        if (StringUtils.contains(reason, "基础数据") || StringUtils.contains(reason, "缺失")) {
            return UnscheduledReasonEnum.BASE_DATA_MISSING;
        }
        if (StringUtils.contains(reason, "精度") && StringUtils.contains(reason, "撤销")) {
            return UnscheduledReasonEnum.PRECISION_PRE_INSERT_ROLLBACK;
        }
        if (StringUtils.contains(reason, "精度") && StringUtils.contains(reason, "下机")) {
            return UnscheduledReasonEnum.PRECISION_FORCE_DOWN;
        }
        if (StringUtils.contains(reason, "虚拟机台")) {
            return UnscheduledReasonEnum.VIRTUAL_MACHINE_CAPACITY_INSUFFICIENT;
        }
        if (StringUtils.contains(reason, "置换")) {
            return UnscheduledReasonEnum.SUBSTITUTION_REMAINING;
        }
        if (StringUtils.contains(reason, "窗口最后一日")
                || StringUtils.contains(reason, "排程窗口结束")) {
            return UnscheduledReasonEnum.WINDOW_FINAL_UNSCHEDULED;
        }
        return UnscheduledReasonEnum.OTHER;
    }

    private void recordReasonEvent(LhScheduleContext context,
                                   String demandKey,
                                   UnscheduledReasonEnum reasonCode,
                                   String detail) {
        if (StringUtils.isEmpty(demandKey) || Objects.isNull(reasonCode)) {
            return;
        }
        UnscheduledResultRuntime runtime = context.getUnscheduledResultRuntime();
        UnscheduledReasonEvent event = new UnscheduledReasonEvent();
        event.setReasonCode(reasonCode);
        event.setDetail(detail);
        long nextSequence = runtime.getEventSequence() + 1;
        runtime.setEventSequence(nextSequence);
        event.setSequence(nextSequence);
        runtime.getReasonEventMap().computeIfAbsent(
                demandKey, key -> new ArrayList<UnscheduledReasonEvent>(4)).add(event);
    }

    private LhUnscheduledResult selectPrimaryResult(List<LhUnscheduledResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return null;
        }
        return results.stream().max((left, right) -> {
            UnscheduledReasonEnum leftReason = UnscheduledReasonEnum.fromCode(
                    left.getUnscheduledReasonCode());
            UnscheduledReasonEnum rightReason = UnscheduledReasonEnum.fromCode(
                    right.getUnscheduledReasonCode());
            int priorityResult = Integer.compare(leftReason.getPriority(), rightReason.getPriority());
            if (priorityResult != 0) {
                return priorityResult;
            }
            return rightReason.getCode().compareTo(leftReason.getCode());
        }).orElse(null);
    }

    private UnscheduledReasonEnum selectPrimaryReason(
            LhScheduleContext context,
            String demandKey,
            UnscheduledReasonEnum currentReason) {
        UnscheduledReasonEnum selectedReason = currentReason;
        List<UnscheduledReasonEvent> events = context.getUnscheduledResultRuntime()
                .getReasonEventMap().get(demandKey);
        if (CollectionUtils.isEmpty(events)) {
            return selectedReason;
        }
        for (UnscheduledReasonEvent event : events) {
            if (Objects.isNull(event) || Objects.isNull(event.getReasonCode())) {
                continue;
            }
            UnscheduledReasonEnum candidateReason = event.getReasonCode();
            if (candidateReason.getPriority() > selectedReason.getPriority()
                    || (candidateReason.getPriority() == selectedReason.getPriority()
                    && candidateReason.getCode().compareTo(selectedReason.getCode()) < 0)) {
                selectedReason = candidateReason;
            }
        }
        return selectedReason;
    }

    private String buildFinalReasonDetail(LhScheduleContext context,
                                          String demandKey,
                                          UnscheduledReasonEnum primaryReason,
                                          LhUnscheduledResult primary,
                                          List<LhUnscheduledResult> results) {
        Set<String> detailSet = new LinkedHashSet<String>(8);
        List<UnscheduledReasonEvent> events = context.getUnscheduledResultRuntime()
                .getReasonEventMap().get(demandKey);
        if (!CollectionUtils.isEmpty(events)) {
            // 主原因证据优先展示，避免多候选明细达到字段上限时截掉真正终局阻断。
            events.stream()
                    .filter(event -> Objects.nonNull(event)
                            && event.getReasonCode() == primaryReason)
                    .sorted(Comparator.comparingLong(UnscheduledReasonEvent::getSequence))
                    .map(UnscheduledReasonEvent::getDetail)
                    .filter(StringUtils::isNotEmpty)
                    .forEach(detailSet::add);
            events.stream()
                    .filter(event -> Objects.nonNull(event)
                            && event.getReasonCode() != primaryReason)
                    .sorted(Comparator.comparingLong(UnscheduledReasonEvent::getSequence))
                    .map(UnscheduledReasonEvent::getDetail)
                    .filter(StringUtils::isNotEmpty)
                    .forEach(detailSet::add);
        }
        if (StringUtils.isNotEmpty(primary.getUnscheduledReasonDetail())) {
            detailSet.add(primary.getUnscheduledReasonDetail());
        }
        results.stream().map(LhUnscheduledResult::getUnscheduledReasonDetail)
                .filter(StringUtils::isNotEmpty).forEach(detailSet::add);
        String detail = String.join("；", detailSet);
        return detail.length() <= MAX_REASON_DETAIL_LENGTH
                ? detail : detail.substring(0, MAX_REASON_DETAIL_LENGTH);
    }

    private boolean isSameSku(LhUnscheduledResult result, SkuScheduleDTO sku) {
        return this.isSameSku(result, sku.getMaterialCode(), sku.getProductStatus());
    }

    private boolean isSameSku(LhUnscheduledResult result,
                              String materialCode,
                              String productStatus) {
        return StringUtils.equals(result.getMaterialCode(), materialCode)
                && StringUtils.equals(StringUtils.trimToEmpty(result.getProductStatus()),
                StringUtils.trimToEmpty(productStatus));
    }

    private LocalDate toLocalDate(Date date) {
        return Objects.isNull(date) ? null : date.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
