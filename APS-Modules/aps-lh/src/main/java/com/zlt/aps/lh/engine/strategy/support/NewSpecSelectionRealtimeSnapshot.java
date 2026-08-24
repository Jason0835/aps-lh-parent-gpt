package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * 新增 SKU 进入真实选机流程时的无副作用实时统计快照。
 *
 * <p>该对象只在当前 SKU 的候选机台正式试排前读取已经落地的排程结果和运行态账本，
 * 不写计划量、不分配换模次数、不登记结构机台，也不参与任何候选排序。一个选机回合只
 * 扫描一次当前结果列表，后续候选机台失败重试复用同一快照；当前 SKU 成功落地后，下一台
 * 机台选机回合会重新采集，因此能够看到上一台已经提交后的最新状态。</p>
 *
 * @author APS
 */
public final class NewSpecSelectionRealtimeSnapshot {

    /** 当前 SKU 现有规则已经解析出的最早胎胚可供硫化时间 */
    private final Date earliestEmbryoAvailableTime;
    /** 选机前窗口各班次已经落地的总计划量 */
    private final String realtimeShiftTotalPlanQty;
    /** 选机前窗口各班次已经正式占用的换模/换活字块次数 */
    private final String realtimeShiftChangeCount;
    /** 选机前当前结构按日已经占用的物理硫化机台数 */
    private final String realtimeStructureMachineCount;
    /** 当前业务日相对 T 日偏移 */
    private final int dateOffset;
    /** 当前 SKU 在本业务日真正进入新增选机流程的顺序 */
    private final int selectionOrder;

    private NewSpecSelectionRealtimeSnapshot(Date earliestEmbryoAvailableTime,
                                             String realtimeShiftTotalPlanQty,
                                             String realtimeShiftChangeCount,
                                             String realtimeStructureMachineCount,
                                             int dateOffset,
                                             int selectionOrder) {
        this.earliestEmbryoAvailableTime = copyDate(earliestEmbryoAvailableTime);
        this.realtimeShiftTotalPlanQty = realtimeShiftTotalPlanQty;
        this.realtimeShiftChangeCount = realtimeShiftChangeCount;
        this.realtimeStructureMachineCount = realtimeStructureMachineCount;
        this.dateOffset = dateOffset;
        this.selectionOrder = selectionOrder;
    }

    /**
     * 从当前已经落地的排程结果和统一运行态账本采集一次选机前快照。
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @param earliestEmbryoAvailableTime 当前主链已解析出的最早胎胚可供时间
     * @param orderEntry 当前业务日真实选机顺序明细
     * @param mouldChangeBalance 换模/换活字块统一计数策略
     * @param dateOffset 当前业务日相对 T 日偏移
     * @return 不影响排产的实时统计快照
     */
    public static NewSpecSelectionRealtimeSnapshot capture(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            Date earliestEmbryoAvailableTime,
            DailyNewSpecOrderLogEntry orderEntry,
            IMouldChangeBalanceStrategy mouldChangeBalance,
            int dateOffset) {
        int selectionOrder = 0;
        if (Objects.nonNull(orderEntry)) {
            selectionOrder = orderEntry.getSelectionOrder();
        }
        return new NewSpecSelectionRealtimeSnapshot(
                earliestEmbryoAvailableTime,
                buildShiftTotalPlanQty(context),
                buildShiftChangeCount(context, mouldChangeBalance),
                buildStructureMachineCount(context, sku),
                dateOffset,
                selectionOrder);
    }

    /**
     * 汇总当前已经落地结果的窗口班次计划量。
     *
     * @param context 排程上下文
     * @return {@code c1=2000,c2=3000,...} 完整八班文本
     */
    private static String buildShiftTotalPlanQty(LhScheduleContext context) {
        int[] shiftQtyArray = new int[LhScheduleConstant.MAX_SHIFT_SLOT_COUNT];
        if (Objects.nonNull(context) && CollectionUtils.isNotEmpty(context.getScheduleResultList())) {
            for (LhScheduleResult result : context.getScheduleResultList()) {
                if (Objects.isNull(result)) {
                    continue;
                }
                for (int shiftIndex = 1;
                     shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT;
                     shiftIndex++) {
                    Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                    if (Objects.nonNull(planQty) && planQty > 0) {
                        shiftQtyArray[shiftIndex - 1] += planQty;
                    }
                }
            }
        }
        return buildShiftValueText(shiftQtyArray);
    }

    /**
     * 按窗口班次读取统一换模/换活字块实时计数。
     *
     * @param context 排程上下文
     * @param mouldChangeBalance 换模/换活字块统一计数策略
     * @return {@code c1=1,c2=2,...} 完整八班文本
     */
    private static String buildShiftChangeCount(
            LhScheduleContext context,
            IMouldChangeBalanceStrategy mouldChangeBalance) {
        int[] shiftCountArray = new int[LhScheduleConstant.MAX_SHIFT_SLOT_COUNT];
        if (Objects.nonNull(context) && Objects.nonNull(mouldChangeBalance)
                && CollectionUtils.isNotEmpty(context.getScheduleWindowShifts())) {
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                        || shift.getShiftIndex() < 1
                        || shift.getShiftIndex() > LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
                    continue;
                }
                shiftCountArray[shift.getShiftIndex() - 1] =
                        mouldChangeBalance.getAllocatedChangeoverCount(
                                context, shift.getShiftStartDateTime());
            }
        }
        return buildShiftValueText(shiftCountArray);
    }

    /**
     * 按既有结构物理机台去重口径读取 T～T+2 实时已排机台数。
     *
     * @param context 排程上下文
     * @param sku 当前待选机 SKU
     * @return {@code T=1,T+1=2,T+2=2}
     */
    private static String buildStructureMachineCount(
            LhScheduleContext context,
            SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate())
                || Objects.isNull(sku)) {
            return "T=0,T+1=0,T+2=0";
        }
        LocalDate baseDate = context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        StringBuilder textBuilder = new StringBuilder(24);
        for (int dateOffset = 0; dateOffset < 3; dateOffset++) {
            if (dateOffset > 0) {
                textBuilder.append(',');
            }
            textBuilder.append(dateOffset == 0 ? "T" : "T+" + dateOffset)
                    .append('=')
                    .append(context.getStructureScheduledMachineCount(
                            baseDate.plusDays(dateOffset), sku.getStructureName()));
        }
        return textBuilder.toString();
    }

    /**
     * 拼接完整窗口班次值。
     *
     * @param shiftValueArray 班次值数组，下标 0 对应 c1
     * @return c1～c8 完整文本
     */
    private static String buildShiftValueText(int[] shiftValueArray) {
        StringBuilder textBuilder = new StringBuilder(72);
        for (int shiftIndex = 1;
             shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT;
             shiftIndex++) {
            if (shiftIndex > 1) {
                textBuilder.append(',');
            }
            textBuilder.append('c').append(shiftIndex).append('=')
                    .append(shiftValueArray[shiftIndex - 1]);
        }
        return textBuilder.toString();
    }

    private static Date copyDate(Date sourceDate) {
        return Objects.isNull(sourceDate) ? null : new Date(sourceDate.getTime());
    }

    public Date getEarliestEmbryoAvailableTime() {
        return copyDate(earliestEmbryoAvailableTime);
    }

    public String getRealtimeShiftTotalPlanQty() {
        return realtimeShiftTotalPlanQty;
    }

    public String getRealtimeShiftChangeCount() {
        return realtimeShiftChangeCount;
    }

    public String getRealtimeStructureMachineCount() {
        return realtimeStructureMachineCount;
    }

    public int getDateOffset() {
        return dateOffset;
    }

    public int getSelectionOrder() {
        return selectionOrder;
    }
}
