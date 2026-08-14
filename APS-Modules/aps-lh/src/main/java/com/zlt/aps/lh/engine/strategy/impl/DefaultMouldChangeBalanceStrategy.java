/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 默认模具切换均衡策略实现
 * <p>启用换模均衡后，每日总次数为硬限制，早8/中7为正式分配的参考上限（满后顺延/错峰），
 * 夜班不切换。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class DefaultMouldChangeBalanceStrategy implements IMouldChangeBalanceStrategy {

    /** dailyMouldChangeCountMap value数组下标：[0]=早班换模数, [1]=中班换模数 */
    private static final int IDX_MORNING = 0;
    private static final int IDX_AFTERNOON = 1;
    private static final int MAX_ALLOCATION_ATTEMPTS = 16;

    @Override
    public boolean hasCapacity(LhScheduleContext context, Date targetDate) {
        String dateKey = formatDateKey(targetDate);
        int[] counts = context.getDailyMouldChangeCountMap().getOrDefault(dateKey, new int[]{0, 0});
        int totalUsed = counts[IDX_MORNING] + counts[IDX_AFTERNOON];
        int dailyLimit = getDailyLimit(context);
        return totalUsed < dailyLimit;
    }

    @Override
    public Date allocateMouldChange(LhScheduleContext context, String machineCode, Date endingTime) {
        return allocateMouldChange(
                context,
                machineCode,
                endingTime,
                LhScheduleTimeUtil.getMouldChangeTotalHours(context));
    }

    @Override
    public Date allocateMouldChange(LhScheduleContext context,
                                    String machineCode,
                                    Date endingTime,
                                    int switchDurationHours) {
        if (!isChangeoverBalanceEnabled(context)) {
            return allocateLegacyMouldChange(context, machineCode, endingTime, switchDurationHours);
        }
        return allocateMouldChange(context, machineCode, endingTime, switchDurationHours, null, ACTION_CHANGEOVER);
    }

    @Override
    public Date allocateMouldChange(LhScheduleContext context,
                                    String machineCode,
                                    Date endingTime,
                                    int switchDurationHours,
                                    SkuScheduleDTO sku,
                                    String actionType) {
        // 未指定业务日日终时，允许早班满8后落到当天中班（续作/换活字块/新增通用语义）。
        return allocateMouldChange(context, machineCode, endingTime, switchDurationHours,
                sku, actionType, null);
    }

    /**
     * 指定SKU、动作类型和业务日日终约束的均衡分配。
     *
     * <p>启用换模均衡后，正式换模/换活字块按早8/中7/日15统一收口，避免早班换模过于集中：
     * 自然最早落点在早班且早班已达参考上限时，先尝试当天中班；中班不可承接
     * （中班已满、每日已满或中班换模无法在业务日日终前完成）时顺延次日早班。
     * 中班已达参考上限时同样顺延次日早班，不把中班推到8次。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param endingTime 前SKU收尾时间
     * @param switchDurationHours 切换时长（小时）
     * @param sku 当前待排SKU
     * @param actionType 切换动作类型
     * @param businessDayEndTime 当前业务日日终时间；null表示不限制中班完成时刻
     * @return 换模分配的班次和时间
     */
    @Override
    public Date allocateMouldChange(LhScheduleContext context,
                                    String machineCode,
                                    Date endingTime,
                                    int switchDurationHours,
                                    SkuScheduleDTO sku,
                                    String actionType,
                                    Date businessDayEndTime) {
        if (!isChangeoverBalanceEnabled(context)) {
            return allocateLegacyMouldChange(context, machineCode, endingTime, switchDurationHours);
        }
        if (endingTime == null) {
            return null;
        }

        clearBlockedReason(context, sku);
        Date adjustedTime = endingTime;
        // 首台日终约束下的顺延只记录一次过程日志，避免同一事件在多次尝试中重复刷日志。
        boolean dayEndDeferLogged = false;

        // 最多向后探索有限次数，避免极端数据导致死循环
        for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
            // 先处理设备停机窗口：05允许并行，其他停机仍从停机结束时刻继续判断。
            Date downtimeAdjustedTime = resolveDowntimeAdjustedStartTime(
                    context, machineCode, adjustedTime, switchDurationHours);
            if (downtimeAdjustedTime.after(adjustedTime)) {
                adjustedTime = downtimeAdjustedTime;
                continue;
            }

            // 若在禁止换模时间段内（20:00-次日6:00），顺延到禁止时段结束后的第一个早班（凌晨段为当日早班，晚间段为次日早班）
            if (LhScheduleTimeUtil.isNoMouldChangeTime(context, adjustedTime)) {
                adjustedTime = LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(
                        context, adjustedTime);
                continue;
            }

            String dateKey = formatDateKey(adjustedTime);
            int[] counts = context.getDailyMouldChangeCountMap()
                    .computeIfAbsent(dateKey, key -> new int[]{0, 0});
            int dailyLimit = getDailyLimit(context);
            int totalUsed = getTotalUsed(counts);
            int morningLimit = getMorningLimit(context);
            int afternoonLimit = getAfternoonLimit(context);

            // 每日总次数为硬上限：达到后T+2直接拒绝，之前日期顺延次日早班。
            if (totalUsed >= dailyLimit) {
                if (isOnOrAfterScheduleTargetDate(context, adjustedTime)) {
                    recordBlockedReason(context, sku, dailyLimit);
                    log.warn("换模/换活字块每日次数达到T+2上限，进入未排, materialCode: {}, embryoCode: {}, "
                                    + "actionType: {}, 日期: {}, 当天总次数: {}/{}, 早班次数: {}, 中班次数: {}",
                            sku == null ? null : sku.getMaterialCode(),
                            sku == null ? null : sku.getEmbryoCode(),
                            StringUtils.defaultIfEmpty(actionType, ACTION_CHANGEOVER),
                            dateKey, totalUsed, dailyLimit, counts[IDX_MORNING], counts[IDX_AFTERNOON]);
                    return null;
                }
                Date nextDayMorningStart = getNextCalendarDayMorningStart(context, adjustedTime);
                log.info("换模/换活字块每日次数已达上限，顺延到后一天, materialCode: {}, embryoCode: {}, "
                                + "actionType: {}, 当前日期: {}, 顺延日期: {}, 当天总次数: {}/{}",
                        sku == null ? null : sku.getMaterialCode(),
                        sku == null ? null : sku.getEmbryoCode(),
                        StringUtils.defaultIfEmpty(actionType, ACTION_CHANGEOVER),
                        dateKey, LhScheduleTimeUtil.formatDate(nextDayMorningStart), totalUsed, dailyLimit);
                adjustedTime = nextDayMorningStart;
                continue;
            }

            if (LhScheduleTimeUtil.isMorningShift(context, adjustedTime)) {
                // 早班未达参考上限：保持最早合法时间。
                if (counts[IDX_MORNING] < morningLimit) {
                    return registerMouldChangeAndLog(
                            context, adjustedTime, sku, actionType, dateKey);
                }
                // 早班已达参考上限：尝试当天中班，避免早班换模过于集中。
                Date afternoonCandidate = resolveAfternoonBalanceCandidate(
                        context, machineCode, dateKey, adjustedTime,
                        switchDurationHours, counts, afternoonLimit, businessDayEndTime);
                if (Objects.nonNull(afternoonCandidate)) {
                    return registerMouldChangeAndLog(
                            context, afternoonCandidate, sku, actionType,
                            formatDateKey(afternoonCandidate));
                }
                // 中班不可承接：顺延次日早班；首台日终约束下必须留下过程日志便于对账。
                if (Objects.nonNull(businessDayEndTime) && !dayEndDeferLogged) {
                    appendDayEndDeferProcessLog(
                            context, dateKey, adjustedTime, sku, businessDayEndTime);
                    dayEndDeferLogged = true;
                }
                // 软目标顺延同样受排程窗口约束：顺延落点已到/越过窗口结束日时直接拒绝，
                // 避免生成窗口外换模时间（与每日15次硬上限顺延语义保持一致）。
                if (isOnOrAfterScheduleTargetDate(context, adjustedTime)) {
                    log.warn("换模/换活字块早中班参考上限顺延超出排程窗口，进入未排, materialCode: {}, "
                                    + "embryoCode: {}, actionType: {}, 日期: {}, 早班次数: {}, 中班次数: {}",
                            sku == null ? null : sku.getMaterialCode(),
                            sku == null ? null : sku.getEmbryoCode(),
                            StringUtils.defaultIfEmpty(actionType, ACTION_CHANGEOVER),
                            dateKey, counts[IDX_MORNING], counts[IDX_AFTERNOON]);
                    return null;
                }
                adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
                continue;
            }

            if (LhScheduleTimeUtil.isAfternoonShift(context, adjustedTime)) {
                // 中班未达参考上限：直接落中班。
                if (counts[IDX_AFTERNOON] < afternoonLimit) {
                    return registerMouldChangeAndLog(
                            context, adjustedTime, sku, actionType, dateKey);
                }
                // 中班已达参考上限：顺延次日早班，不把中班推到8次。
                if (isOnOrAfterScheduleTargetDate(context, adjustedTime)) {
                    log.warn("换模/换活字块中班参考上限顺延超出排程窗口，进入未排, materialCode: {}, "
                                    + "embryoCode: {}, actionType: {}, 日期: {}, 早班次数: {}, 中班次数: {}",
                            sku == null ? null : sku.getMaterialCode(),
                            sku == null ? null : sku.getEmbryoCode(),
                            StringUtils.defaultIfEmpty(actionType, ACTION_CHANGEOVER),
                            dateKey, counts[IDX_MORNING], counts[IDX_AFTERNOON]);
                    return null;
                }
                adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
                continue;
            }

            adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
        }

        log.warn("换模均衡分配失败，无可用换模班次, 原始时间: {}",
                LhScheduleTimeUtil.formatDateTime(endingTime));
        return null;
    }

    /**
     * 无副作用预演换模/换活字块落点。
     *
     * <p>该方法逐分支复用正式分配的判断顺序，只读取真实早/中班计数，不调用登记、
     * 回滚、未排原因和过程日志方法。关闭均衡开关时只保留停机与20:00禁换模约束，
     * 与新增正式基础换模入口保持一致。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param endingTime 机台具备切换条件的时间
     * @param switchDurationHours 切换时长
     * @param sku 当前 SKU
     * @param actionType 切换动作类型
     * @param businessDayEndTime 当前业务日日终
     * @return 预演开始时间；无可用班次返回null
     */
    @Override
    public Date previewMouldChange(LhScheduleContext context,
                                   String machineCode,
                                   Date endingTime,
                                   int switchDurationHours,
                                   SkuScheduleDTO sku,
                                   String actionType,
                                   Date businessDayEndTime) {
        if (Objects.isNull(endingTime)) {
            return null;
        }
        Date adjustedTime = endingTime;
        for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
            Date downtimeAdjustedTime = resolveDowntimeAdjustedStartTime(
                    context, machineCode, adjustedTime, switchDurationHours);
            if (downtimeAdjustedTime.after(adjustedTime)) {
                adjustedTime = downtimeAdjustedTime;
                continue;
            }
            if (LhScheduleTimeUtil.isNoMouldChangeTime(context, adjustedTime)) {
                adjustedTime = LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(
                        context, adjustedTime);
                continue;
            }
            if (!isChangeoverBalanceEnabled(context)) {
                return adjustedTime;
            }

            String dateKey = formatDateKey(adjustedTime);
            int[] counts = context.getDailyMouldChangeCountMap()
                    .getOrDefault(dateKey, new int[]{0, 0});
            if (getTotalUsed(counts) >= getDailyLimit(context)) {
                if (isOnOrAfterScheduleTargetDate(context, adjustedTime)) {
                    return null;
                }
                adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
                continue;
            }
            if (LhScheduleTimeUtil.isMorningShift(context, adjustedTime)) {
                if (counts[IDX_MORNING] < getMorningLimit(context)) {
                    return adjustedTime;
                }
                Date afternoonCandidate = resolveAfternoonBalanceCandidate(
                        context, machineCode, dateKey, adjustedTime, switchDurationHours,
                        counts, getAfternoonLimit(context), businessDayEndTime);
                if (Objects.nonNull(afternoonCandidate)) {
                    return afternoonCandidate;
                }
                if (isOnOrAfterScheduleTargetDate(context, adjustedTime)) {
                    return null;
                }
                adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
                continue;
            }
            if (LhScheduleTimeUtil.isAfternoonShift(context, adjustedTime)) {
                if (counts[IDX_AFTERNOON] < getAfternoonLimit(context)) {
                    return adjustedTime;
                }
                if (isOnOrAfterScheduleTargetDate(context, adjustedTime)) {
                    return null;
                }
                adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
                continue;
            }
            adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
        }
        return null;
    }

    /**
     * 解析早班满8后当天中班的候选落点。
     *
     * <p>中班候选必须满足：中班未达参考上限、停机/禁止换模避让后仍落在当天中班、
     * 中班换模能在业务日日终前完成（仅当传入日终约束时）。任一条件不满足返回null，
     * 由调用方顺延次日早班，禁止为软目标跨天或把中班推到8次。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param dateKey 当天日期键
     * @param morningTime 当前早班候选时间
     * @param switchDurationHours 切换时长（小时）
     * @param counts 当天早/中班计数
     * @param afternoonLimit 中班参考上限
     * @param businessDayEndTime 业务日日终时间；null表示不限制中班完成时刻
     * @return 中班候选时间；不可承接时返回null
     */
    private Date resolveAfternoonBalanceCandidate(LhScheduleContext context,
                                                  String machineCode,
                                                  String dateKey,
                                                  Date morningTime,
                                                  int switchDurationHours,
                                                  int[] counts,
                                                  int afternoonLimit,
                                                  Date businessDayEndTime) {
        if (counts[IDX_AFTERNOON] >= afternoonLimit) {
            return null;
        }
        Date afternoonProbeTime = LhScheduleTimeUtil.getAfternoonShiftStart(context, morningTime);
        Date afternoonTime = resolveEndingStaggerPreviewStartTime(
                context, machineCode, afternoonProbeTime, switchDurationHours);
        // 停机/禁止换模避让后必须仍落在当天中班，不允许为软目标跨天或倒排到早班。
        if (Objects.isNull(afternoonTime)
                || !StringUtils.equals(dateKey, formatDateKey(afternoonTime))
                || !LhScheduleTimeUtil.isAfternoonShift(context, afternoonTime)) {
            return null;
        }
        // 首台当日必须开产：中班换模无法在业务日日终前完成时，禁止承接中班。
        if (Objects.nonNull(businessDayEndTime)) {
            Date afternoonCompleteTime =
                    LhScheduleTimeUtil.addHours(afternoonTime, switchDurationHours);
            if (Objects.isNull(afternoonCompleteTime)
                    || !afternoonCompleteTime.before(businessDayEndTime)) {
                return null;
            }
        }
        return afternoonTime;
    }

    /**
     * 登记换模/换活字块班次次数并输出落点日志。
     *
     * @param context 排程上下文
     * @param allocatedTime 已落定切换时间
     * @param sku 当前待排SKU
     * @param actionType 切换动作类型
     * @param dateKey 落点日期键
     * @return 已落定切换时间
     */
    private Date registerMouldChangeAndLog(LhScheduleContext context,
                                           Date allocatedTime,
                                           SkuScheduleDTO sku,
                                           String actionType,
                                           String dateKey) {
        registerMouldChangeCount(context, allocatedTime);
        int[] updatedCounts = context.getDailyMouldChangeCountMap().get(dateKey);
        log.info("换模/换活字块班次落点完成, materialCode: {}, embryoCode: {}, 是否共用胎胚: {}, "
                        + "actionType: {}, 日期: {}, 当天总次数: {}/{}, 早班次数: {}, 中班次数: {}, 最终换模班次: {}",
                sku == null ? null : sku.getMaterialCode(),
                sku == null ? null : sku.getEmbryoCode(),
                isSharedEmbryo(context, sku),
                StringUtils.defaultIfEmpty(actionType, ACTION_CHANGEOVER),
                dateKey, getTotalUsed(updatedCounts), getDailyLimit(context),
                updatedCounts[IDX_MORNING], updatedCounts[IDX_AFTERNOON],
                LhScheduleTimeUtil.isMorningShift(context, allocatedTime) ? "早班" : "中班");
        return allocatedTime;
    }

    /**
     * 记录早班满8且中班无法在业务日日终前完成时的顺延过程日志。
     *
     * @param context 排程上下文
     * @param dateKey 当前日期键
     * @param morningTime 被拒绝的早班候选时间
     * @param sku 当前待排SKU
     * @param businessDayEndTime 业务日日终时间
     */
    private void appendDayEndDeferProcessLog(LhScheduleContext context,
                                             String dateKey,
                                             Date morningTime,
                                             SkuScheduleDTO sku,
                                             Date businessDayEndTime) {
        String detail = "早班换模已达参考上限且中班无法在业务日日终前完成，顺延次日早班: "
                + "materialCode=" + (sku == null ? null : sku.getMaterialCode())
                + ", 日期=" + dateKey
                + ", 早班候选=" + LhScheduleTimeUtil.formatDateTime(morningTime)
                + ", 业务日日终=" + LhScheduleTimeUtil.formatDateTime(businessDayEndTime)
                + ", 每日上限=" + getDailyLimit(context)
                + ", 早班参考上限=" + getMorningLimit(context)
                + ", 中班参考上限=" + getAfternoonLimit(context);
        PriorityTraceLogHelper.appendProcessLog(context, "换模班次日终顺延", detail);
        log.info("{}", detail);
    }

    /**
     * 旧换模均衡逻辑。
     * <p>参数关闭时保持原早班8次/中班7次硬限制口径不变，避免关闭态排程结果漂移。</p>
     */
    private Date allocateLegacyMouldChange(LhScheduleContext context,
                                           String machineCode,
                                           Date endingTime,
                                           int switchDurationHours) {
        if (endingTime == null) {
            return null;
        }

        Date adjustedTime = endingTime;

        // 最多向后探索有限次数，避免极端数据导致死循环
        for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
            // 先处理设备停机窗口：05允许并行，其他停机仍从停机结束时刻继续判断。
            Date downtimeAdjustedTime = resolveDowntimeAdjustedStartTime(
                    context, machineCode, adjustedTime, switchDurationHours);
            if (downtimeAdjustedTime.after(adjustedTime)) {
                adjustedTime = downtimeAdjustedTime;
                continue;
            }

            // 若在禁止换模时间段内（20:00-次日6:00），顺延到禁止时段结束后的第一个早班（凌晨段为当日早班，晚间段为次日早班）
            if (LhScheduleTimeUtil.isNoMouldChangeTime(context, adjustedTime)) {
                adjustedTime = LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(context, adjustedTime);
                continue;
            }

            String dateKey = formatDateKey(adjustedTime);
            int[] counts = context.getDailyMouldChangeCountMap().computeIfAbsent(dateKey, k -> new int[]{0, 0});

            int morningLimit = getMorningLimit(context);
            int afternoonLimit = getAfternoonLimit(context);

            if (LhScheduleTimeUtil.isMorningShift(context, adjustedTime)) {
                // 当前时间在早班
                if (counts[IDX_MORNING] < morningLimit) {
                    counts[IDX_MORNING]++;
                    log.debug("换模分配到早班, 日期: {}, 早班已用: {}/{}", dateKey, counts[IDX_MORNING], morningLimit);
                    return adjustedTime;
                }
                // 早班已满，换模后移到当天中班开始时间
                adjustedTime = LhScheduleTimeUtil.getAfternoonShiftStart(context, adjustedTime);
                continue;
            }

            if (LhScheduleTimeUtil.isAfternoonShift(context, adjustedTime)) {
                // 当前时间在中班
                if (counts[IDX_AFTERNOON] < afternoonLimit) {
                    counts[IDX_AFTERNOON]++;
                    log.debug("换模分配到中班, 日期: {}, 中班已用: {}/{}", dateKey, counts[IDX_AFTERNOON], afternoonLimit);
                    return adjustedTime;
                }
                // 中班也满了，延后到日历次日早班（与禁止换模窗口后的「当日早班」语义不同）
                adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
                continue;
            }

            // 夜班不换模，直接顺延到日历次日早班（常规配置下多由禁止换模分支先行处理）
            adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
        }

        log.warn("换模均衡分配失败，无可用换模班次, 原始时间: {}",
                LhScheduleTimeUtil.formatDateTime(endingTime));
        return null;
    }

    @Override
    public void rollbackMouldChange(LhScheduleContext context, Date allocatedTime) {
        if (context == null || allocatedTime == null) {
            return;
        }
        String dateKey = formatDateKey(allocatedTime);
        int[] counts = context.getDailyMouldChangeCountMap().get(dateKey);
        if (counts == null) {
            return;
        }
        if (LhScheduleTimeUtil.isMorningShift(context, allocatedTime) && counts[IDX_MORNING] > 0) {
            counts[IDX_MORNING]--;
            return;
        }
        if (LhScheduleTimeUtil.isAfternoonShift(context, allocatedTime) && counts[IDX_AFTERNOON] > 0) {
            counts[IDX_AFTERNOON]--;
        }
    }

    @Override
    public Date previewEndingStaggerMouldChange(LhScheduleContext context,
                                                String machineCode,
                                                Date switchReadyTime,
                                                int switchDurationHours,
                                                SkuScheduleDTO sku,
                                                Map<String, int[]> simulatedCountMap) {
        if (context == null || switchReadyTime == null || simulatedCountMap == null) {
            return null;
        }
        Date adjustedTime = switchReadyTime;
        // 预演必须与正式allocateMouldChange使用同一落点规则：停机/禁换模避让后取最早合法班次，
        // 只有当天达到每日硬上限时才跨天，不能仅为早8中7软目标把早班动作虚拟挪到中班。
        for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
            Date earliestTime = resolveEndingStaggerPreviewStartTime(
                    context, machineCode, adjustedTime, switchDurationHours);
            if (earliestTime == null) {
                return null;
            }
            String dateKey = formatDateKey(earliestTime);
            int[] counts = simulatedCountMap.computeIfAbsent(dateKey, key -> new int[]{0, 0});
            int dailyLimit = getDailyLimit(context);
            if (getTotalUsed(counts) >= dailyLimit) {
                // 与正式分配一致：T+2达到上限直接拒绝；之前日期达到上限才顺延到次日早班。
                if (isOnOrAfterScheduleTargetDate(context, earliestTime)) {
                    return null;
                }
                adjustedTime = getNextCalendarDayMorningStart(context, earliestTime);
                continue;
            }
            if (LhScheduleTimeUtil.isMorningShift(context, earliestTime)) {
                counts[IDX_MORNING]++;
            } else if (LhScheduleTimeUtil.isAfternoonShift(context, earliestTime)) {
                counts[IDX_AFTERNOON]++;
            } else {
                adjustedTime = getNextCalendarDayMorningStart(context, earliestTime);
                continue;
            }
            log.debug("共用胎胚收尾错峰换模预演完成, materialCode: {}, machineCode: {}, 换模日期: {}, "
                            + "早班模拟次数: {}, 中班模拟次数: {}, 每日上限: {}, 早班目标: {}, 中班目标: {}",
                    sku == null ? null : sku.getMaterialCode(), machineCode, dateKey,
                    counts[IDX_MORNING], counts[IDX_AFTERNOON], dailyLimit,
                    getMorningLimit(context), getAfternoonLimit(context));
            return earliestTime;
        }
        return null;
    }

    /**
     * 在早班/中班候选中选择最有利于早8/中7软目标均衡的换模时间。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param earliestTime 最早可用换模时间
     * @param switchDurationHours 切换时长（小时）
     * @param countMap 用于评分的每日早/中班模拟计数
     * @return 选中的换模时间；无合法候选时返回null
     */
    private Date selectEndingStaggerBalancedTime(LhScheduleContext context,
                                                 String machineCode,
                                                 Date earliestTime,
                                                 int switchDurationHours,
                                                 Map<String, int[]> countMap) {
        List<Date> candidateTimeList = new ArrayList<Date>(2);
        candidateTimeList.add(earliestTime);
        // 最早落点在中班时只评估当前中班落点；落早班时同时评估“等待到当日中班”是否更均衡。
        // 中班候选经过停机/禁换模避让后若落到其他自然日或非中班，说明当日中班不可用，
        // 不能为了软目标提前跨天，跨天只能由每日15次硬限制触发。
        if (LhScheduleTimeUtil.isMorningShift(context, earliestTime)) {
            Date afternoonProbeTime = LhScheduleTimeUtil.getAfternoonShiftStart(context, earliestTime);
            Date afternoonTime = resolveEndingStaggerPreviewStartTime(
                    context, machineCode, afternoonProbeTime, switchDurationHours);
            if (afternoonTime != null
                    && !afternoonTime.equals(earliestTime)
                    && LhScheduleTimeUtil.isAfternoonShift(context, afternoonTime)
                    && StringUtils.equals(formatDateKey(afternoonTime), formatDateKey(earliestTime))) {
                candidateTimeList.add(afternoonTime);
            }
        }
        Date selectedTime = null;
        int selectedExceededShiftCount = Integer.MAX_VALUE;
        int selectedOverflowQty = Integer.MAX_VALUE;
        long selectedBalanceDeviation = Long.MAX_VALUE;
        for (Date candidateTime : candidateTimeList) {
            String dateKey = formatDateKey(candidateTime);
            int[] currentCounts = countMap.getOrDefault(dateKey, new int[]{0, 0});
            int morningCount = currentCounts.length > IDX_MORNING ? currentCounts[IDX_MORNING] : 0;
            int afternoonCount = currentCounts.length > IDX_AFTERNOON ? currentCounts[IDX_AFTERNOON] : 0;
            if (morningCount + afternoonCount >= getDailyLimit(context)) {
                continue;
            }
            int projectedMorningCount = morningCount
                    + (LhScheduleTimeUtil.isMorningShift(context, candidateTime) ? 1 : 0);
            int projectedAfternoonCount = afternoonCount
                    + (LhScheduleTimeUtil.isAfternoonShift(context, candidateTime) ? 1 : 0);
            if (projectedMorningCount == morningCount && projectedAfternoonCount == afternoonCount) {
                continue;
            }
            int exceededShiftCount = (projectedMorningCount > getMorningLimit(context) ? 1 : 0)
                    + (projectedAfternoonCount > getAfternoonLimit(context) ? 1 : 0);
            int overflowQty = Math.max(0, projectedMorningCount - getMorningLimit(context))
                    + Math.max(0, projectedAfternoonCount - getAfternoonLimit(context));
            long balanceDeviation = calculateShiftBalanceDeviation(
                    context, projectedMorningCount, projectedAfternoonCount);
            if (isBetterEndingStaggerPreviewCandidate(
                    exceededShiftCount, overflowQty, balanceDeviation, candidateTime,
                    selectedExceededShiftCount, selectedOverflowQty, selectedBalanceDeviation, selectedTime)) {
                selectedTime = candidateTime;
                selectedExceededShiftCount = exceededShiftCount;
                selectedOverflowQty = overflowQty;
                selectedBalanceDeviation = balanceDeviation;
            }
        }
        return selectedTime;
    }

    /**
     * 解析错峰后最早可用的换模开始时间。
     * <p>处理顺序与正式换模分配一致：先避让不可并行的设备停机，再避让禁止换模时段，
     * 最终只允许落在早班或中班。本方法只读上下文，不登记任何真实次数。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param startTime 起始探测时间
     * @param switchDurationHours 切换时长（小时）
     * @return 最早可用换模开始时间；超出探测上限时返回 {@code null}
     */
    private Date resolveEndingStaggerPreviewStartTime(LhScheduleContext context,
                                                      String machineCode,
                                                      Date startTime,
                                                      int switchDurationHours) {
        Date adjustedTime = startTime;
        for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
            Date downtimeAdjustedTime = resolveDowntimeAdjustedStartTime(
                    context, machineCode, adjustedTime, switchDurationHours);
            if (downtimeAdjustedTime.after(adjustedTime)) {
                adjustedTime = downtimeAdjustedTime;
                continue;
            }
            if (LhScheduleTimeUtil.isNoMouldChangeTime(context, adjustedTime)) {
                adjustedTime = LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(context, adjustedTime);
                continue;
            }
            if (LhScheduleTimeUtil.isMorningShift(context, adjustedTime)
                    || LhScheduleTimeUtil.isAfternoonShift(context, adjustedTime)) {
                return adjustedTime;
            }
            adjustedTime = getNextCalendarDayMorningStart(context, adjustedTime);
        }
        return null;
    }

    /**
     * 判断新的错峰预演落点是否更优。
     *
     * @param exceededShiftCount 新落点超过目标的班次数
     * @param overflowQty 新落点超过目标的累计次数
     * @param balanceDeviation 新落点与早中班目标比例的偏差
     * @param candidateTime 新落点时间
     * @param selectedExceededShiftCount 已选落点超过目标的班次数
     * @param selectedOverflowQty 已选落点超过目标的累计次数
     * @param selectedBalanceDeviation 已选落点的比例偏差
     * @param selectedTime 已选落点时间
     * @return true-新落点更优；false-保留已选落点
     */
    private boolean isBetterEndingStaggerPreviewCandidate(int exceededShiftCount,
                                                          int overflowQty,
                                                          long balanceDeviation,
                                                          Date candidateTime,
                                                          int selectedExceededShiftCount,
                                                          int selectedOverflowQty,
                                                          long selectedBalanceDeviation,
                                                          Date selectedTime) {
        if (selectedTime == null) {
            return true;
        }
        if (exceededShiftCount != selectedExceededShiftCount) {
            return exceededShiftCount < selectedExceededShiftCount;
        }
        if (overflowQty != selectedOverflowQty) {
            return overflowQty < selectedOverflowQty;
        }
        if (balanceDeviation != selectedBalanceDeviation) {
            return balanceDeviation < selectedBalanceDeviation;
        }
        return candidateTime.before(selectedTime);
    }

    /**
     * 计算早中班模拟次数与目标比例的偏差。
     *
     * @param context 排程上下文
     * @param morningCount 早班次数
     * @param afternoonCount 中班次数
     * @return 偏差绝对值，越小越接近目标比例
     */
    private long calculateShiftBalanceDeviation(LhScheduleContext context,
                                                int morningCount,
                                                int afternoonCount) {
        return Math.abs((long) morningCount * getAfternoonLimit(context)
                - (long) afternoonCount * getMorningLimit(context));
    }

    @Override
    public int getRemainingCapacity(LhScheduleContext context, Date targetDate) {
        String dateKey = formatDateKey(targetDate);
        int[] counts = context.getDailyMouldChangeCountMap().getOrDefault(dateKey, new int[]{0, 0});
        int totalUsed = counts[IDX_MORNING] + counts[IDX_AFTERNOON];
        int dailyLimit = getDailyLimit(context);
        return Math.max(0, dailyLimit - totalUsed);
    }

    /**
     * 判断是否启用换模均衡新口径。
     *
     * @param context 排程上下文
     * @return true-启用；false-关闭
     */
    private boolean isChangeoverBalanceEnabled(LhScheduleContext context) {
        return context != null
                && context.getScheduleConfig() != null
                && context.getScheduleConfig().isChangeoverBalanceEnabled();
    }

    /**
     * 统计当天已使用换模/换活字块总次数。
     *
     * @param counts 当天早/中班计数
     * @return 当天总次数
     */
    private int getTotalUsed(int[] counts) {
        if (counts == null || counts.length < 2) {
            return 0;
        }
        return counts[IDX_MORNING] + counts[IDX_AFTERNOON];
    }

    /**
     * 判断当前SKU是否属于本月共用胎胚。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @return true-共用胎胚；false-单胎胚或无法识别
     */
    private boolean isSharedEmbryo(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null || StringUtils.isEmpty(sku.getMaterialCode())) {
            return false;
        }
        return Boolean.TRUE.equals(context.getMaterialSharedEmbryoMap().get(sku.getMaterialCode()));
    }

    /**
     * 解析共用胎胚本次应落定的均衡班次。
     * <p>只能在不早于当前可切换时间的候选班次内均衡。
     * 单胎胚不会进入本方法（调用方已通过 isSharedEmbryo 过滤）。
     * 共用胎胚均衡规则：只有原本落早班且早班次数已达阈值（再落会超过）时，才挪到中班；
     * 原本落中班的不因中班次数多而强制挪动。</p>
     *
     * @param context 排程上下文
     * @param candidateTime 当前候选切换时间
     * @param counts 当天早/中班计数
     * @return 均衡后的候选切换时间
     */
    private Date resolveSharedEmbryoBalancedTime(LhScheduleContext context, Date candidateTime, int[] counts) {
        if (candidateTime == null || counts == null || counts.length < 2) {
            return candidateTime;
        }
        // 共用胎胚换模均衡：只有原本落早班且早班次数已超过阈值时，才挪到中班
        // 中班不强制挪动，单胎胚不进入此方法
        int morningLimit = getMorningLimit(context);
        if (LhScheduleTimeUtil.isMorningShift(context, candidateTime)
                && counts[IDX_MORNING] >= morningLimit) {
            return LhScheduleTimeUtil.getAfternoonShiftStart(context, candidateTime);
        }
        return candidateTime;
    }

    /**
     * 登记本次换模/换活字块次数。
     *
     * @param context 排程上下文
     * @param allocatedTime 已落定切换时间
     * @return true-登记成功；false-无法登记
     */
    private boolean registerMouldChangeCount(LhScheduleContext context, Date allocatedTime) {
        if (context == null || allocatedTime == null) {
            return false;
        }
        String dateKey = formatDateKey(allocatedTime);
        int[] counts = context.getDailyMouldChangeCountMap().computeIfAbsent(dateKey, key -> new int[]{0, 0});
        if (LhScheduleTimeUtil.isMorningShift(context, allocatedTime)) {
            counts[IDX_MORNING]++;
            return true;
        }
        if (LhScheduleTimeUtil.isAfternoonShift(context, allocatedTime)) {
            counts[IDX_AFTERNOON]++;
            return true;
        }
        return false;
    }

    /**
     * 判断候选日期是否已经达到排程窗口最后一天。
     *
     * @param context 排程上下文
     * @param candidateTime 候选切换时间
     * @return true-已到窗口结束日(T+2)或更晚；false-仍可顺延
     */
    private boolean isOnOrAfterScheduleTargetDate(LhScheduleContext context, Date candidateTime) {
        if (context == null || context.getWindowEndDate() == null || candidateTime == null) {
            return false;
        }
        Date candidateDate = LhScheduleTimeUtil.clearTime(candidateTime);
        Date targetDate = LhScheduleTimeUtil.clearTime(context.getWindowEndDate());
        return !candidateDate.before(targetDate);
    }

    /**
     * 记录T+2换模/换活字块日上限阻塞原因，供未排结果复用。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param dailyLimit 每日换模/换活字块上限
     */
    private void recordBlockedReason(LhScheduleContext context, SkuScheduleDTO sku, int dailyLimit) {
        if (context == null || sku == null || StringUtils.isEmpty(sku.getMaterialCode())) {
            return;
        }
        context.getMouldChangeLimitBlockedReasonMap().put(sku.getMaterialCode(),
                "窗口结束日 换模/换活字块次数超过每日" + dailyLimit + "次上限");
    }

    /**
     * 清理当前SKU上一次换模上限阻塞原因，避免候选重试成功后残留旧原因。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     */
    private void clearBlockedReason(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null || StringUtils.isEmpty(sku.getMaterialCode())) {
            return;
        }
        context.getMouldChangeLimitBlockedReasonMap().remove(sku.getMaterialCode());
    }

    /**
     * 解析扣除设备停机后的最早换模开始时间。
     * <p>05-计划性维修属于下机维修，换模或换活字块允许在维修窗口内并行完成，因此不再把
     * 切换开始时间顺延到 05 结束；后续由统一维修时间轴执行 max(维修结束, 切换结束)+预热。
     * 00～04、06、09 等其他停机仍保持原顺延语义，不扩大本次规则影响范围。</p>
     */
    private Date resolveDowntimeAdjustedStartTime(LhScheduleContext context,
                                                  String machineCode,
                                                  Date candidateStartTime,
                                                  int switchDurationHours) {
        if (context == null
                || StringUtils.isEmpty(machineCode)
                || candidateStartTime == null
                || CollectionUtils.isEmpty(context.getDevicePlanShutList())) {
            return candidateStartTime;
        }
        Date candidateEndTime = LhScheduleTimeUtil.addHours(
                candidateStartTime, switchDurationHours);
        Date latestOverlapEndTime = null;
        for (MdmDevicePlanShut planShut : context.getDevicePlanShutList()) {
            if (planShut == null
                    || !StringUtils.equals(machineCode, planShut.getMachineCode())
                    || StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(),
                    planShut.getMachineStopType())
                    || planShut.getBeginDate() == null
                    || planShut.getEndDate() == null
                    || !planShut.getBeginDate().before(planShut.getEndDate())) {
                continue;
            }
            if (!candidateStartTime.before(planShut.getEndDate())
                    || !planShut.getBeginDate().before(candidateEndTime)) {
                continue;
            }
            if (latestOverlapEndTime == null || planShut.getEndDate().after(latestOverlapEndTime)) {
                latestOverlapEndTime = planShut.getEndDate();
            }
        }
        return latestOverlapEndTime != null ? latestOverlapEndTime : candidateStartTime;
    }

    /**
     * 日历次日早班开始时间（用于中班换模配额已满等「已进入可换模日段」后的再顺延）
     */
    private Date getNextCalendarDayMorningStart(LhScheduleContext context, Date currentTime) {
        Date nextDay = LhScheduleTimeUtil.addDays(LhScheduleTimeUtil.clearTime(currentTime), 1);
        return LhScheduleTimeUtil.buildTime(nextDay, LhScheduleTimeUtil.getMorningStartHour(context), 0, 0);
    }

    private String formatDateKey(Date date) {
        return LhScheduleTimeUtil.formatDate(date);
    }

    private int getDailyLimit(LhScheduleContext context) {
        return LhScheduleTimeUtil.getDailyMouldChangeLimit(context);
    }

    private int getMorningLimit(LhScheduleContext context) {
        return LhScheduleTimeUtil.getMorningMouldChangeLimit(context);
    }

    private int getAfternoonLimit(LhScheduleContext context) {
        return LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context);
    }
}
