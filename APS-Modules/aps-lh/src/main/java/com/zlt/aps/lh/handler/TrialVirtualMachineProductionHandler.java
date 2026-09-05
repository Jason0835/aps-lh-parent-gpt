package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSpecialMaterialUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.SingleMouldShiftQtyUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * S4.5.3 试制/量试虚拟机台最终兜底处理器。
 *
 * <p>该步骤只处理 S4.5 正常新增排产前冻结的试制/量试候选。在全部实际机台排产和置换
 * 完成后，按已排结果重新计算硫化余量，为每个仍有余量的 SKU 分配一台本批次内唯一的
 * V1001～V9999 虚拟机台，并从排程窗口首个中班开始按标准班产连续排量。</p>
 *
 * <p>虚拟机台只写排程结果，不写 {@code machineScheduleMap}、机台班次产能、模具、换模、
 * 换活字块、首检或胎胚库存资源账本，因此不会参与任何后续真实资源竞争。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class TrialVirtualMachineProductionHandler extends AbsScheduleStepHandler {

    /** 虚拟机台编码起点。 */
    private static final int VIRTUAL_MACHINE_START_SEQUENCE = 1001;
    /** 虚拟机台编码终点。 */
    private static final int VIRTUAL_MACHINE_END_SEQUENCE = 9999;
    /** 虚拟机台编码前缀。 */
    private static final String VIRTUAL_MACHINE_PREFIX = "V";
    /** 虚拟机台名称。 */
    private static final String VIRTUAL_MACHINE_NAME = "试制/量试虚拟机台";
    /** 虚拟机台不绑定真实模具，结果使用单模口径避免尾量被模数向上修正。 */
    private static final int VIRTUAL_MACHINE_MOULD_QTY = 1;
    /** 虚拟机台班次原因。 */
    private static final String VIRTUAL_MACHINE_SHIFT_ANALYSIS = "试制/量试虚拟机台兜底排产";
    /** 虚拟机台排产场景。 */
    private static final String VIRTUAL_MACHINE_SCENE = "试制/量试虚拟机台兜底";

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    /**
     * 执行试制/量试虚拟机台最终兜底排产。
     *
     * @param context 排程上下文
     * @return void
     */
    @Override
    protected void doHandle(LhScheduleContext context) {
        if (CollectionUtils.isEmpty(context.getTrialVirtualMachineCandidateList())) {
            log.info("试制量试虚拟机台排产跳过, factoryCode: {}, batchNo: {}, reason: 无兜底候选",
                    context.getFactoryCode(), context.getBatchNo());
            return;
        }
        List<LhShiftConfigVO> schedulingShifts = this.resolveVirtualSchedulingShifts(context);
        if (CollectionUtils.isEmpty(schedulingShifts)) {
            log.warn("试制量试虚拟机台排产跳过, factoryCode: {}, batchNo: {}, reason: 排程窗口无中班及后续班次",
                    context.getFactoryCode(), context.getBatchNo());
            return;
        }

        Set<String> usedMachineCodeSet = this.collectUsedMachineCodes(context);
        Set<String> processedSkuKeySet = new LinkedHashSet<String>(
                context.getTrialVirtualMachineCandidateList().size() * 2);
        int nextSequence = VIRTUAL_MACHINE_START_SEQUENCE;
        int scheduledSkuCount = 0;
        for (SkuScheduleDTO sku : context.getTrialVirtualMachineCandidateList()) {
            if (Objects.isNull(sku) || !PendingSkuUnscheduledRule.isTrialOrMassTrialSku(sku)) {
                continue;
            }
            String skuKey = this.buildSkuKey(sku);
            if (!processedSkuKeySet.add(skuKey) || sku.getSurplusQty() <= 0) {
                continue;
            }

            int actualMachineScheduledQty = this.resolveScheduledQty(context, sku);
            int remainingCuringQty = Math.max(0, sku.getSurplusQty() - actualMachineScheduledQty);
            String originalUnscheduledReason = this.resolveOriginalUnscheduledReason(context, sku);
            if (remainingCuringQty <= 0) {
                this.reconcileUnscheduledResult(context, sku, 0, originalUnscheduledReason);
                log.info("试制量试虚拟机台排产跳过, materialCode: {}, productStatus: {}, "
                                + "surplusQty: {}, actualMachineScheduledQty: {}, reason: 实际机台已排完硫化余量",
                        sku.getMaterialCode(), sku.getProductStatus(), sku.getSurplusQty(),
                        actualMachineScheduledQty);
                continue;
            }

            int shiftCapacity = Math.max(0, sku.getShiftCapacity());
            if (shiftCapacity <= 0) {
                log.warn("试制量试虚拟机台排产失败, materialCode: {}, productStatus: {}, "
                                + "remainingCuringQty: {}, reason: 班产小于等于0",
                        sku.getMaterialCode(), sku.getProductStatus(), remainingCuringQty);
                continue;
            }
            String virtualMachineCode = this.allocateVirtualMachineCode(
                    usedMachineCodeSet, nextSequence);
            if (StringUtils.isEmpty(virtualMachineCode)) {
                log.warn("试制量试虚拟机台排产失败, materialCode: {}, productStatus: {}, "
                                + "remainingCuringQty: {}, reason: V1001至V9999编码已耗尽",
                        sku.getMaterialCode(), sku.getProductStatus(), remainingCuringQty);
                continue;
            }
            nextSequence = Integer.parseInt(virtualMachineCode.substring(1)) + 1;
            usedMachineCodeSet.add(virtualMachineCode);

            int remainingAfterVirtual = this.scheduleOnVirtualMachine(
                    context, sku, virtualMachineCode, schedulingShifts,
                    shiftCapacity, remainingCuringQty, originalUnscheduledReason);
            int virtualScheduledQty = remainingCuringQty - remainingAfterVirtual;
            if (virtualScheduledQty <= 0) {
                continue;
            }
            scheduledSkuCount++;
            this.reconcileUnscheduledResult(
                    context, sku, remainingAfterVirtual, originalUnscheduledReason);
        }
        log.info("试制量试虚拟机台排产完成, factoryCode: {}, batchNo: {}, candidateCount: {}, "
                        + "scheduledSkuCount: {}, resultCount: {}, unscheduledCount: {}",
                context.getFactoryCode(), context.getBatchNo(),
                processedSkuKeySet.size(), scheduledSkuCount,
                context.getScheduleResultList().size(), context.getUnscheduledResultList().size());
    }

    /**
     * 将单个 SKU 按班次连续排入虚拟机台。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param virtualMachineCode 虚拟机台编码
     * @param schedulingShifts 从首个中班开始的连续班次
     * @param shiftCapacity 标准班产
     * @param remainingCuringQty 待排硫化余量
     * @param originalUnscheduledReason 正常新增阶段未排原因
     * @return 虚拟机台排产后的剩余硫化余量
     */
    private int scheduleOnVirtualMachine(LhScheduleContext context,
                                         SkuScheduleDTO sku,
                                         String virtualMachineCode,
                                         List<LhShiftConfigVO> schedulingShifts,
                                         int shiftCapacity,
                                         int remainingCuringQty,
                                         String originalUnscheduledReason) {
        LhScheduleResult result = this.buildVirtualScheduleResult(
                context, sku, virtualMachineCode, shiftCapacity);
        targetScheduleQtyResolver.syncProductionRemainingQtyToRemaining(
                context, sku, remainingCuringQty, VIRTUAL_MACHINE_SCENE + "初始化硫化余量");
        int remaining = remainingCuringQty;
        StringBuilder shiftPlanSummary = new StringBuilder(192);
        for (LhShiftConfigVO shift : schedulingShifts) {
            if (remaining <= 0) {
                break;
            }
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                    || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getShiftEndDateTime())) {
                continue;
            }
            int expectedPlanQty = Math.min(shiftCapacity, remaining);
            int planQty = targetScheduleQtyResolver.deductVirtualMachineCuringRemainingQty(
                    context, sku, expectedPlanQty, VIRTUAL_MACHINE_SCENE, virtualMachineCode);
            if (planQty <= 0) {
                break;
            }
            Date shiftEndTime = ShiftCapacityResolverUtil.resolveShiftPlanEndTime(
                    Collections.emptyList(), Collections.emptyList(), virtualMachineCode,
                    shift.getShiftStartDateTime(), shift.getShiftEndDateTime(), planQty, shiftCapacity);
            ShiftFieldUtil.setShiftPlanQty(
                    result, shift.getShiftIndex(), planQty,
                    shift.getShiftStartDateTime(), shiftEndTime);
            ShiftFieldUtil.setShiftAnalysis(
                    result, shift.getShiftIndex(), VIRTUAL_MACHINE_SHIFT_ANALYSIS);
            remaining -= planQty;
            if (shiftPlanSummary.length() > 0) {
                shiftPlanSummary.append(", ");
            }
            shiftPlanSummary.append("class").append(shift.getShiftIndex())
                    .append('=').append(planQty);
            log.info("试制量试虚拟机台逐班排产, materialCode: {}, productStatus: {}, "
                            + "virtualMachineCode: {}, shiftIndex: {}, shiftCapacity: {}, "
                            + "planQty: {}, remainingCuringQty: {}",
                    sku.getMaterialCode(), sku.getProductStatus(), virtualMachineCode,
                    shift.getShiftIndex(), shiftCapacity, planQty, remaining);
        }
        sku.setRemainingScheduleQty(remaining);
        if (ShiftFieldUtil.resolveScheduledQty(result) <= 0) {
            return remainingCuringQty;
        }
        ShiftFieldUtil.syncDailyPlanQty(result);
        result.setSpecEndTime(this.resolveLastPlanEndTime(result));
        ShiftFieldUtil.applyLastPlannedShiftEndMark(result, remaining == 0);
        ShiftFieldUtil.applyProductStatusShiftEndOverride(result);
        context.getScheduleResultList().add(result);
        context.getScheduleResultSourceSkuMap().put(result, sku);

        String detail = new StringBuilder(512)
                .append("factoryCode=").append(context.getFactoryCode())
                .append(", batchNo=").append(context.getBatchNo())
                .append(", materialCode=").append(sku.getMaterialCode())
                .append(", productStatus=").append(sku.getProductStatus())
                .append(", constructionStage=").append(sku.getConstructionStage())
                .append(", originalUnscheduledReason=").append(originalUnscheduledReason)
                .append(", surplusQty=").append(sku.getSurplusQty())
                .append(", virtualMachineCode=").append(virtualMachineCode)
                .append(", startShift=class").append(schedulingShifts.get(0).getShiftIndex())
                .append(", shiftCapacity=").append(shiftCapacity)
                .append(", shiftPlanQty={").append(shiftPlanSummary).append('}')
                .append(", remainingCuringQty=").append(remaining)
                .toString();
        PriorityTraceLogHelper.appendProcessLog(context, "试制量试虚拟机台排产", detail);
        log.info("试制量试虚拟机台SKU排产完成, {}", detail);
        return remaining;
    }

    /**
     * 构建虚拟机台排程结果基础字段。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param virtualMachineCode 虚拟机台编码
     * @param shiftCapacity 标准班产
     * @return 虚拟机台排程结果
     */
    private LhScheduleResult buildVirtualScheduleResult(LhScheduleContext context,
                                                        SkuScheduleDTO sku,
                                                        String virtualMachineCode,
                                                        int shiftCapacity) {
        LhScheduleResult result = new LhScheduleResult();
        result.setFactoryCode(context.getFactoryCode());
        result.setBatchNo(context.getBatchNo());
        result.setOrderNo(orderNoGenerator.generateOrderNo(context.getScheduleTargetDate()));
        result.setLhMachineCode(virtualMachineCode);
        result.setLhMachineName(VIRTUAL_MACHINE_NAME);
        result.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(null, virtualMachineCode));
        result.setMachineOrder(Integer.parseInt(virtualMachineCode.substring(1)));
        result.setMaterialCode(sku.getMaterialCode());
        result.setMaterialDesc(sku.getMaterialDesc());
        result.setSpecCode(sku.getSpecCode());
        result.setSpecDesc(sku.getSpecDesc());
        result.setEmbryoCode(sku.getEmbryoCode());
        result.setEmbryoStock(Math.max(sku.getEmbryoStock(), 0));
        result.setMainMaterialDesc(sku.getMainMaterialDesc());
        result.setStructureName(sku.getStructureName());
        result.setScheduleDate(context.getScheduleTargetDate());
        result.setRealScheduleDate(context.getScheduleDate());
        result.setLhTime(sku.getLhTimeSeconds());
        result.setMouldQty(VIRTUAL_MACHINE_MOULD_QTY);
        result.setSingleMouldShiftQty(SingleMouldShiftQtyUtil.resolveSingleMouldShiftQty(
                context, sku, VIRTUAL_MACHINE_MOULD_QTY));
        result.setDailyPlanQty(0);
        result.setTotalDailyPlanQty(sku.getMonthPlanQty());
        result.setMouldSurplusQty(sku.getSurplusQty());
        result.setMonthPlanSumTotal(sku.getMonthPlanSumTotal());
        result.setTotalFinishQty(sku.getFinishedQty());
        result.setStandardCapacity(ShiftCapacityResolverUtil.resolveDailyStandardQty(
                context, sku.getMaterialCode()));
        result.setIsEarlyProduction("0");
        result.setIsEnd("1");
        result.setIsDelivery(sku.isDeliveryLocked() ? "1" : "0");
        result.setIsRelease("0");
        result.setDataSource("0");
        result.setIsDelete(0);
        result.setScheduleType(ScheduleTypeEnum.NEW_SPEC.getCode());
        result.setIsChangeMould("0");
        result.setIsTypeBlock("0");
        result.setConstructionStage(sku.getConstructionStage());
        result.setProductStatus(sku.getProductStatus());
        result.setMonthPlanVersion(sku.getMonthPlanVersion());
        result.setProductionVersion(sku.getProductionVersion());
        result.setIsTrial(sku.isTrial() ? "1" : "0");
        result.setProductionStatus("0");
        result.setHasSpecialMaterial(LhSpecialMaterialUtil.resolveHasSpecialMaterial(context, sku));
        this.fillConstructionFormula(context, sku, result);
        return result;
    }

    /**
     * 复用当前 SKU 的示方书关系填充结果及各班次示方字段。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param result 虚拟机台排程结果
     * @return void
     */
    private void fillConstructionFormula(LhScheduleContext context,
                                         SkuScheduleDTO sku,
                                         LhScheduleResult result) {
        MdmSkuConstructionRef constructionRef = context.findSkuConstructionRef(
                sku.getMaterialCode(), sku.getProductStatus());
        String embryoNo = Objects.nonNull(constructionRef) ? constructionRef.getEmbryoNo() : null;
        String textNo = Objects.nonNull(constructionRef) ? constructionRef.getTextNo() : null;
        String lhNo = Objects.nonNull(constructionRef) ? constructionRef.getLhNo() : null;
        String lhType = Objects.nonNull(constructionRef) ? constructionRef.getLhType() : null;
        result.setEmbryoNo(embryoNo);
        result.setTextNo(textNo);
        result.setLhNo(lhNo);
        result.setChangedTrialStatus(lhType);
        for (int shiftIndex = 1;
             shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT;
             shiftIndex++) {
            ShiftFieldUtil.setShiftCureFormula(result, shiftIndex, lhNo, lhType);
        }
    }

    /**
     * 获取排程窗口首个中班及其后全部连续班次。
     *
     * @param context 排程上下文
     * @return 从中班开始的班次列表
     */
    private List<LhShiftConfigVO> resolveVirtualSchedulingShifts(LhScheduleContext context) {
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        if (CollectionUtils.isEmpty(shifts)) {
            shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        }
        if (CollectionUtils.isEmpty(shifts)) {
            return Collections.emptyList();
        }
        for (int index = 0; index < shifts.size(); index++) {
            LhShiftConfigVO shift = shifts.get(index);
            if (Objects.nonNull(shift) && shift.isAfternoonShift()) {
                return new ArrayList<LhShiftConfigVO>(shifts.subList(index, shifts.size()));
            }
        }
        return Collections.emptyList();
    }

    /**
     * 汇总本次执行已存在的真实/虚拟机台编码，避免虚拟编码重复。
     *
     * @param context 排程上下文
     * @return 已使用机台编码集合
     */
    private Set<String> collectUsedMachineCodes(LhScheduleContext context) {
        Set<String> machineCodeSet = new LinkedHashSet<String>(
                context.getMachineScheduleMap().size() + context.getScheduleResultList().size());
        machineCodeSet.addAll(context.getMachineScheduleMap().keySet());
        context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .map(LhScheduleResult::getLhMachineCode)
                .filter(StringUtils::isNotEmpty)
                .forEach(machineCodeSet::add);
        return machineCodeSet;
    }

    /**
     * 从指定序号起分配首个未使用的虚拟机台编码。
     *
     * @param usedMachineCodeSet 已使用机台编码
     * @param startSequence 起始序号
     * @return 虚拟机台编码；编码耗尽返回null
     */
    private String allocateVirtualMachineCode(Set<String> usedMachineCodeSet,
                                              int startSequence) {
        for (int sequence = Math.max(VIRTUAL_MACHINE_START_SEQUENCE, startSequence);
             sequence <= VIRTUAL_MACHINE_END_SEQUENCE;
             sequence++) {
            String machineCode = VIRTUAL_MACHINE_PREFIX + sequence;
            if (!usedMachineCodeSet.contains(machineCode)) {
                return machineCode;
            }
        }
        return null;
    }

    /**
     * 汇总当前 SKU 已通过实际机台形成的结果数量。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return 已排产量
     */
    private int resolveScheduledQty(LhScheduleContext context, SkuScheduleDTO sku) {
        return context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> this.isSameSku(result, sku))
                .mapToInt(ShiftFieldUtil::resolveScheduledQty)
                .sum();
    }

    /**
     * 获取正常新增阶段为当前 SKU 记录的未排原因。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return 未排原因；不存在时返回统一说明
     */
    private String resolveOriginalUnscheduledReason(LhScheduleContext context,
                                                    SkuScheduleDTO sku) {
        for (LhUnscheduledResult result : context.getUnscheduledResultList()) {
            if (Objects.nonNull(result) && this.isSameSku(result, sku)
                    && StringUtils.isNotEmpty(result.getUnscheduledReason())) {
                return result.getUnscheduledReason();
            }
        }
        return "正常新增排产后仍有剩余硫化余量";
    }

    /**
     * 根据虚拟机台排产结果清理或更新同 SKU 未排记录。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param remainingQty 虚拟机台排产后的剩余量
     * @param originalReason 正常新增阶段原未排原因
     * @return void
     */
    private void reconcileUnscheduledResult(LhScheduleContext context,
                                            SkuScheduleDTO sku,
                                            int remainingQty,
                                            String originalReason) {
        LhUnscheduledResult retainedResult = null;
        List<LhUnscheduledResult> matchingResults = new ArrayList<LhUnscheduledResult>(2);
        for (LhUnscheduledResult result : context.getUnscheduledResultList()) {
            if (Objects.nonNull(result) && this.isSameSku(result, sku)) {
                matchingResults.add(result);
                if (Objects.isNull(retainedResult)) {
                    retainedResult = result;
                }
            }
        }
        context.getUnscheduledResultList().removeAll(matchingResults);
        if (remainingQty <= 0) {
            return;
        }
        if (Objects.isNull(retainedResult)) {
            retainedResult = this.buildUnscheduledResult(context, sku);
        }
        retainedResult.setUnscheduledQty(remainingQty);
        retainedResult.setUnscheduledReason("虚拟机台排程窗口容量不足，原未排原因：" + originalReason);
        context.getUnscheduledResultList().add(retainedResult);
    }

    /**
     * 构建虚拟机台窗口仍不足时的未排结果。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return 未排结果
     */
    private LhUnscheduledResult buildUnscheduledResult(LhScheduleContext context,
                                                       SkuScheduleDTO sku) {
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
        result.setMouldQty(VIRTUAL_MACHINE_MOULD_QTY);
        result.setDataSource("0");
        result.setIsDelete(0);
        return result;
    }

    /**
     * 获取虚拟结果最后一个有量班次的计划结束时间。
     *
     * @param result 虚拟机台排程结果
     * @return 最后计划结束时间
     */
    private Date resolveLastPlanEndTime(LhScheduleResult result) {
        int lastShiftIndex = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
        return lastShiftIndex > 0 ? ShiftFieldUtil.getShiftEndTime(result, lastShiftIndex) : null;
    }

    /**
     * 判断结果与 SKU 是否为同一物料和产品状态。
     *
     * @param result 排程结果
     * @param sku SKU
     * @return true-同一SKU
     */
    private boolean isSameSku(LhScheduleResult result, SkuScheduleDTO sku) {
        return Objects.nonNull(result) && Objects.nonNull(sku)
                && StringUtils.equals(result.getMaterialCode(), sku.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(result.getProductStatus()),
                StringUtils.trimToEmpty(sku.getProductStatus()));
    }

    /**
     * 判断未排结果与 SKU 是否为同一物料和产品状态。
     *
     * @param result 未排结果
     * @param sku SKU
     * @return true-同一SKU
     */
    private boolean isSameSku(LhUnscheduledResult result, SkuScheduleDTO sku) {
        return Objects.nonNull(result) && Objects.nonNull(sku)
                && StringUtils.equals(result.getMaterialCode(), sku.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(result.getProductStatus()),
                StringUtils.trimToEmpty(sku.getProductStatus()));
    }

    /**
     * 构建物料和产品状态复合键。
     *
     * @param sku SKU
     * @return 复合键
     */
    private String buildSkuKey(SkuScheduleDTO sku) {
        return StringUtils.trimToEmpty(sku.getMaterialCode()) + '|'
                + StringUtils.trimToEmpty(sku.getProductStatus());
    }

    /**
     * 获取步骤名称。
     *
     * @return 步骤名称
     */
    @Override
    protected String getStepName() {
        return "S4.5.3试制/量试虚拟机台排产";
    }
}
