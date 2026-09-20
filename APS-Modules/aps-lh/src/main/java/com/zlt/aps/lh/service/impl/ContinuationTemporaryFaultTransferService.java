package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.ContinuationTemporaryFaultTransferEvent;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 续作机台临时性故障模具置换服务。
 *
 * <p>本服务只识别达到连续两班禁产阈值的故障并维护迁移事件。续作数量截断、正常选机、
 * 换模/换活字块时间轴和数量扣账继续复用各自主链。</p>
 */
@Slf4j
@Service
public class ContinuationTemporaryFaultTransferService {

    /** 触发故障模具置换的最小连续禁产班次数。 */
    public static final int MIN_CONTINUOUS_BLOCKED_SHIFT_COUNT = 2;

    /**
     * 按物理机台合并相邻故障并冻结续作迁移事件。
     *
     * @param context 排程上下文
     */
    public void prepare(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getContinuousSkuList())
                || CollectionUtils.isEmpty(context.getTemporaryFaultWindowMap())
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())
                || !CollectionUtils.isEmpty(context.getContinuationTemporaryFaultTransferEventMap())) {
            return;
        }
        Map<String, List<MachineFaultWindowDTO>> physicalFaultMap = context.getTemporaryFaultWindowMap()
                .values().stream().filter(Objects::nonNull)
                .filter(fault -> StringUtils.isNotEmpty(fault.getMachineCode()))
                .filter(fault -> Objects.nonNull(fault.getStartTime()) && Objects.nonNull(fault.getEndTime())
                        && fault.getStartTime().before(fault.getEndTime()))
                .collect(Collectors.groupingBy(fault -> LhSingleControlMachineUtil
                        .resolvePhysicalMachineCode(fault.getMachineCode()), LinkedHashMap::new, Collectors.toList()));
        for (SkuScheduleDTO sourceSku : context.getContinuousSkuList()) {
            if (Objects.isNull(sourceSku) || StringUtils.isEmpty(sourceSku.getContinuousMachineCode())) {
                continue;
            }
            String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    sourceSku.getContinuousMachineCode());
            List<MachineFaultWindowDTO> faultList = physicalFaultMap.get(physicalMachineCode);
            if (CollectionUtils.isEmpty(faultList)) {
                continue;
            }
            this.prepareMachineEvent(context, sourceSku, physicalMachineCode, faultList);
        }
    }

    /**
     * 识别单台续作机台最早达到阈值的连续故障区间。
     *
     * @param context 排程上下文
     * @param sourceSku 原续作SKU
     * @param physicalMachineCode 原物理机台
     * @param faultList 该物理机台的有效故障
     */
    private void prepareMachineEvent(LhScheduleContext context, SkuScheduleDTO sourceSku,
            String physicalMachineCode, List<MachineFaultWindowDTO> faultList) {
        List<MachineFaultWindowDTO> orderedFaultList = new ArrayList<MachineFaultWindowDTO>(faultList);
        orderedFaultList.sort(Comparator.comparing(MachineFaultWindowDTO::getStartTime)
                .thenComparing(MachineFaultWindowDTO::getEndTime));
        List<Date[]> mergedIntervalList = this.mergeFaultIntervals(orderedFaultList);
        for (Date[] interval : mergedIntervalList) {
            List<Integer> affectedShiftIndexList = this.resolveAffectedShiftIndexes(
                    context.getScheduleWindowShifts(), sourceSku, interval[0], interval[1]);
            boolean triggered = affectedShiftIndexList.size() >= MIN_CONTINUOUS_BLOCKED_SHIFT_COUNT;
            log.info("续作临时故障模具置换判断, machineCode: {}, physicalMachineCode: {}, materialCode: {}, "
                            + "faultStartTime: {}, faultEndTime: {}, affectedShifts: {}, threshold: {}, triggered: {}",
                    sourceSku.getContinuousMachineCode(), physicalMachineCode, sourceSku.getMaterialCode(),
                    LhScheduleTimeUtil.formatDateTime(interval[0]), LhScheduleTimeUtil.formatDateTime(interval[1]),
                    affectedShiftIndexList, MIN_CONTINUOUS_BLOCKED_SHIFT_COUNT, triggered);
            if (!triggered) {
                continue;
            }
            ContinuationTemporaryFaultTransferEvent event = new ContinuationTemporaryFaultTransferEvent();
            event.setOriginalMachineCode(sourceSku.getContinuousMachineCode());
            event.setOriginalPhysicalMachineCode(physicalMachineCode);
            event.setMaterialCode(sourceSku.getMaterialCode());
            event.setMaterialDesc(sourceSku.getMaterialDesc());
            event.setProductStatus(sourceSku.getProductStatus());
            event.setFaultStartTime(interval[0]);
            event.setFaultEndTime(interval[1]);
            event.setAffectedShiftIndexList(affectedShiftIndexList);
            event.setOriginalMouldCode(LhMouldCodeUtil.joinMouldCode(
                    LhMouldCodeUtil.resolveInMachineMouldCodeSet(
                            context, sourceSku.getContinuousMachineCode())));
            orderedFaultList.stream()
                    .filter(fault -> fault.getStartTime().before(interval[1])
                            && fault.getEndTime().after(interval[0]))
                    .map(MachineFaultWindowDTO::getPlanId).filter(Objects::nonNull)
                    .forEach(event.getSourcePlanIdList()::add);
            context.getContinuationTemporaryFaultTransferEventMap().put(
                    sourceSku.getContinuousMachineCode(), event);
            String detail = this.buildDecisionDetail(event, true);
            PriorityTraceLogHelper.appendProcessLog(context, "续作临时故障模具置换判断", detail);
            log.info("续作临时故障达到模具置换阈值, {}", detail);
            return;
        }
    }

    /**
     * 合并重叠或首尾相接的临时故障区间。
     *
     * @param faultList 已按开始时间排序的故障
     * @return 合并后的开始、结束时间数组
     */
    private List<Date[]> mergeFaultIntervals(List<MachineFaultWindowDTO> faultList) {
        List<Date[]> intervalList = new ArrayList<Date[]>(faultList.size());
        for (MachineFaultWindowDTO fault : faultList) {
            if (intervalList.isEmpty()) {
                intervalList.add(new Date[]{fault.getStartTime(), fault.getEndTime()});
                continue;
            }
            Date[] previous = intervalList.get(intervalList.size() - 1);
            if (!fault.getStartTime().after(previous[1])) {
                if (fault.getEndTime().after(previous[1])) {
                    previous[1] = fault.getEndTime();
                }
            } else {
                intervalList.add(new Date[]{fault.getStartTime(), fault.getEndTime()});
            }
        }
        return intervalList;
    }

    /**
     * 按真实班次可生产秒数识别连续无法形成一模计划量的班次。
     *
     * @param shiftList 排程班次
     * @param sourceSku 续作SKU
     * @param faultStartTime 合并故障开始
     * @param faultEndTime 合并故障结束
     * @return 连续无法生产的班次序号；仅保留最长连续段
     */
    private List<Integer> resolveAffectedShiftIndexes(List<LhShiftConfigVO> shiftList,
            SkuScheduleDTO sourceSku, Date faultStartTime, Date faultEndTime) {
        List<Integer> longest = new ArrayList<Integer>(4);
        List<Integer> current = new ArrayList<Integer>(4);
        int previousShiftIndex = -1;
        for (LhShiftConfigVO shift : shiftList) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                    || Objects.isNull(shift.getShiftStartDateTime())
                    || Objects.isNull(shift.getShiftEndDateTime())) {
                continue;
            }
            long shiftStart = shift.getShiftStartDateTime().getTime();
            long shiftEnd = shift.getShiftEndDateTime().getTime();
            long overlapStart = Math.max(shiftStart, faultStartTime.getTime());
            long overlapEnd = Math.min(shiftEnd, faultEndTime.getTime());
            long unavailableMillis = Math.max(0L, overlapEnd - overlapStart);
            long productiveSeconds = Math.max(0L, (shiftEnd - shiftStart - unavailableMillis) / 1000L);
            boolean unableToProduce = unavailableMillis > 0L
                    && productiveSeconds < Math.max(1, sourceSku.getLhTimeSeconds());
            if (!unableToProduce) {
                if (current.size() > longest.size()) {
                    longest = new ArrayList<Integer>(current);
                }
                current.clear();
                previousShiftIndex = -1;
                continue;
            }
            if (previousShiftIndex > 0 && shift.getShiftIndex() != previousShiftIndex + 1) {
                if (current.size() > longest.size()) {
                    longest = new ArrayList<Integer>(current);
                }
                current.clear();
            }
            current.add(shift.getShiftIndex());
            previousShiftIndex = shift.getShiftIndex();
        }
        return current.size() > longest.size() ? current : longest;
    }

    /**
     * 登记前日交替目标机台尝试结果。
     *
     * @param context 排程上下文
     * @param event 迁移事件
     * @param targetMachineCode 历史目标机台
     * @param success 是否实际提交成功
     * @param transferMode 实际上机方式
     * @param failureReason 失败原因
     */
    public void recordPreviousAlternateAttempt(LhScheduleContext context,
            ContinuationTemporaryFaultTransferEvent event, String targetMachineCode,
            boolean success, String transferMode, String failureReason) {
        event.setPreviousAlternateMatched(true);
        event.setPreviousAlternateMachineCode(targetMachineCode);
        event.setFailureReason(success ? null : failureReason);
        if (success) {
            event.setTargetMachineCode(targetMachineCode);
            event.setTransferMode(transferMode);
        }
        String detail = this.buildDecisionDetail(event, success)
                + ", previousAlternateTarget=" + targetMachineCode
                + ", transferMode=" + transferMode
                + ", failureReason=" + failureReason;
        PriorityTraceLogHelper.appendProcessLog(context, "续作临时故障前日交替复用", detail);
        log.info("续作临时故障前日交替复用, {}", detail);
    }

    /**
     * 记录没有可复用前日机台或全部历史目标失败，后续转普通候选池。
     *
     * @param context 排程上下文
     * @param event 迁移事件
     * @param failureReason 未命中原因
     */
    public void recordPreviousAlternateNotMatched(LhScheduleContext context,
            ContinuationTemporaryFaultTransferEvent event, String failureReason) {
        event.setFailureReason(failureReason);
        String detail = this.buildDecisionDetail(event, false)
                + ", previousAlternateMatched=" + event.isPreviousAlternateMatched()
                + ", previousAlternateTarget=" + event.getPreviousAlternateMachineCode()
                + ", fallback=换活字块/新增候选池"
                + ", failureReason=" + failureReason;
        PriorityTraceLogHelper.appendProcessLog(context, "续作临时故障前日交替复用", detail);
        log.info("续作临时故障前日交替复用未落地, {}", detail);
    }

    /**
     * 构造故障迁移统一日志明细。
     *
     * @param event 迁移事件
     * @param success 本阶段是否成功
     * @return 可落过程日志的明细
     */
    private String buildDecisionDetail(ContinuationTemporaryFaultTransferEvent event, boolean success) {
        return "originalMachine=" + event.getOriginalMachineCode()
                + ", physicalMachine=" + event.getOriginalPhysicalMachineCode()
                + ", materialCode=" + event.getMaterialCode()
                + ", productStatus=" + event.getProductStatus()
                + ", faultStart=" + LhScheduleTimeUtil.formatDateTime(event.getFaultStartTime())
                + ", faultEnd=" + LhScheduleTimeUtil.formatDateTime(event.getFaultEndTime())
                + ", affectedShifts=" + event.getAffectedShiftIndexList()
                + ", originalMould=" + event.getOriginalMouldCode()
                + ", success=" + success;
    }
}
