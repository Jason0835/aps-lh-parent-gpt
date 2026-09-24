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
        return this.allocateMouldChange(context, machineCode, endingTime, switchDurationHours,
                null, ACTION_CHANGEOVER, null);
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
        // 未提供生产日边界的旧入口只执行普通早中班配额，不能仅凭动作名称取得豁免。
        return this.allocateMouldChange(context, machineCode, endingTime, switchDurationHours,
                sku, actionType, businessDayEndTime, null);
    }

    /** 按真实准备边界正式分配，参数及返回值含义见策略接口。 */
    @Override
    public Date allocateMouldChange(LhScheduleContext context, String machineCode, Date endingTime,
            int switchDurationHours, SkuScheduleDTO sku, String actionType,
            Date businessDayEndTime, Date preparationBeforeTime) {
        if (Objects.isNull(context) || Objects.isNull(endingTime)) {
            return null;
        }
        if (this.isChangeoverBalanceEnabled(context)) {
            this.clearBlockedReason(context, sku);
        }
        // 所有预演和正式分配只使用同一个只读决策内核；此处是唯一真实次数登记入口。
        Date allocatedTime = this.resolveMouldChangeStart(context, machineCode, endingTime,
                switchDurationHours, actionType, businessDayEndTime, preparationBeforeTime,
                context.getDailyMouldChangeCountMap());
        this.recordMouldChangeDecision(context, machineCode, endingTime, switchDurationHours,
                sku, businessDayEndTime, allocatedTime);
        if (Objects.isNull(allocatedTime)) {
            log.warn("换模/换活字块无合法落点, materialCode: {}, machineCode: {}, actionType: {}, readyTime: {}",
                    Objects.nonNull(sku) ? sku.getMaterialCode() : null, machineCode, actionType,
                    LhScheduleTimeUtil.formatDateTime(endingTime));
            return null;
        }
        // 日上限或停机避让可能把前日准备推至生产当天，日志也必须记录实际动作身份。
        String committedActionType = this.isCrossDayPreparationAction(actionType)
                && !this.isPreparationBeforeBoundary(allocatedTime, preparationBeforeTime)
                ? ACTION_NEW_SPEC_MOULD_CHANGE : actionType;
        return this.registerMouldChangeAndLog(context, allocatedTime, sku, committedActionType,
                this.formatDateKey(allocatedTime));
    }

    /**
     * 无副作用预演换模/换活字块落点。
     *
     * <p>直接复用正式分配的只读内核，只读取真实计数。开关关闭时沿用正式旧口径的
     * 早/中班限制，不能因预演而绕过正式限制；不调用登记、回滚或未排日志。</p>
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
        return this.previewMouldChange(context, machineCode, endingTime, switchDurationHours,
                sku, actionType, businessDayEndTime, null);
    }

    /** 按真实准备边界只读预演，参数及返回值含义见策略接口。 */
    @Override
    public Date previewMouldChange(LhScheduleContext context, String machineCode, Date endingTime,
            int switchDurationHours, SkuScheduleDTO sku, String actionType,
            Date businessDayEndTime, Date preparationBeforeTime) {
        return Objects.isNull(context) ? null : this.resolveMouldChangeStart(context, machineCode, endingTime,
                switchDurationHours, actionType, businessDayEndTime, preparationBeforeTime,
                context.getDailyMouldChangeCountMap());
    }

    /**
     * 换模/换活字块唯一无副作用落点内核。只读传入次数，禁止访问真实次数写入口。
     * <p>开关开启采用现有日上限、早中班上限和跨日准备例外；关闭保持原正式早中班限制。
     * 日期避让、动作类型、耗时和业务日日终判断均由预演与正式分配共同消费。</p>
     * @param context 上下文
     * @param machineCode 机台编码
     * @param readyTime 可交接时间
     * @param durationHours 动作耗时
     * @param actionType 动作类型
     * @param dayEnd 当前业务日日终
     * @param preparationBeforeTime 生产业务日起点；实际准备必须严格早于该时刻
     * @param countMap 当前资源快照（真实或独立模拟次数）
     * @return 最早合法落点；没有落点返回null
     */
    private Date resolveMouldChangeStart(LhScheduleContext context, String machineCode, Date readyTime,
            int durationHours, String actionType, Date dayEnd, Date preparationBeforeTime, Map<String, int[]> countMap) {
        if (Objects.isNull(context) || Objects.isNull(readyTime) || Objects.isNull(countMap)) {
            return null;
        }
        boolean balanced = this.isChangeoverBalanceEnabled(context);
        // 已提交的时间下机边界统一约束预演和正式入口，不受旧均衡开关或跨日准备例外影响。
        Date timedBoundary = context.getTimedMachineOffBoundary(machineCode);
        boolean timedOff = Objects.nonNull(timedBoundary);
        Date cursor = timedOff && timedBoundary.after(readyTime) ? timedBoundary : readyTime;
        Date timedWindowEnd = timedOff ? context.getScheduleWindowShifts().stream()
                .map(shift -> shift.getShiftEndDateTime()).max(Date::compareTo).orElse(null) : null;
        for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
            if (timedOff && (Objects.isNull(timedWindowEnd) || !cursor.before(timedWindowEnd))) {
                return null;
            }
            Date adjusted = this.resolveDowntimeAdjustedStartTime(context, machineCode, cursor, durationHours);
            if (adjusted.after(cursor)) {
                cursor = adjusted;
                continue;
            }
            if (LhScheduleTimeUtil.isNoMouldChangeTime(context, cursor)) {
                cursor = LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(context, cursor);
                continue;
            }
            String dateKey = this.formatDateKey(cursor);
            int[] counts = countMap.getOrDefault(dateKey, new int[]{0, 0});
            if ((balanced || timedOff) && this.getTotalUsed(counts) >= this.getDailyLimit(context)) {
                if (this.isOnOrAfterScheduleTargetDate(context, cursor)) {
                    return null;
                }
                cursor = this.getNextCalendarDayMorningStart(context, cursor);
                continue;
            }
            if (balanced && !timedOff && this.isCrossDayPreparationAction(actionType)
                    && this.isPreparationBeforeBoundary(cursor, preparationBeforeTime)
                    && this.isSwitchCompletionBeforeBusinessDayEnd(cursor, durationHours, dayEnd)) {
                return cursor;
            }
            if (LhScheduleTimeUtil.isMorningShift(context, cursor)) {
                if (counts[IDX_MORNING] < this.getMorningLimit(context)) {
                    return cursor;
                }
                if (!balanced && !timedOff) {
                    // 关闭态保持原正式行为：早班满后从中班起点继续避让，不附加新日终限制。
                    cursor = LhScheduleTimeUtil.getAfternoonShiftStart(context, cursor);
                    continue;
                }
                Date afternoon = this.resolveAfternoonBalanceCandidate(context, machineCode, dateKey,
                        cursor, durationHours, counts, this.getAfternoonLimit(context), dayEnd);
                if (Objects.nonNull(afternoon)) {
                    return afternoon;
                }
            } else if (LhScheduleTimeUtil.isAfternoonShift(context, cursor)
                    && counts[IDX_AFTERNOON] < this.getAfternoonLimit(context)) {
                return cursor;
            }
            if ((balanced || timedOff) && this.isOnOrAfterScheduleTargetDate(context, cursor)) {
                return null;
            }
            cursor = this.getNextCalendarDayMorningStart(context, cursor);
        }
        return null;
    }

    /**
     * 正式调用才记录阻塞与日终顺延，预演不产生上下文副作用。
     * @param context 上下文
     * @param machineCode 机台编码
     * @param readyTime 原可交接时间
     * @param durationHours 切换时长
     * @param sku SKU
     * @param dayEnd 业务日日终
     * @param allocatedTime 内核确定的落点
     */
    private void recordMouldChangeDecision(LhScheduleContext context, String machineCode, Date readyTime,
            int durationHours, SkuScheduleDTO sku, Date dayEnd, Date allocatedTime) {
        if (!this.isChangeoverBalanceEnabled(context)) {
            return;
        }
        if (Objects.isNull(allocatedTime) && Objects.nonNull(context.getWindowEndDate())) {
            int[] counts = context.getDailyMouldChangeCountMap().get(this.formatDateKey(context.getWindowEndDate()));
            if (this.getTotalUsed(counts) >= this.getDailyLimit(context)) {
                this.recordBlockedReason(context, sku, this.getDailyLimit(context));
            }
        }
        if (Objects.isNull(dayEnd)) {
            return;
        }
        Date first = this.resolveEndingStaggerPreviewStartTime(context, machineCode, readyTime, durationHours);
        if (Objects.isNull(first) || !LhScheduleTimeUtil.isMorningShift(context, first)) {
            return;
        }
        String dateKey = this.formatDateKey(first);
        int[] counts = context.getDailyMouldChangeCountMap().getOrDefault(dateKey, new int[]{0, 0});
        if (counts[IDX_MORNING] >= this.getMorningLimit(context)
                && this.getTotalUsed(counts) < this.getDailyLimit(context)
                && Objects.isNull(this.resolveAfternoonBalanceCandidate(context, machineCode, dateKey, first,
                durationHours, counts, this.getAfternoonLimit(context), dayEnd))) {
            this.appendDayEndDeferProcessLog(context, dateKey, first, sku, dayEnd);
        }
    }

    /**
     * 判断当前动作是否为生产日前贴近下一个业务日首班的跨日准备。
     *
     * @param actionType 换模动作类型
     * @return true-跨日准备；false-普通换模、提前生产换模或换活字块
     */
    private boolean isCrossDayPreparationAction(String actionType) {
        return StringUtils.equals(
                ACTION_CROSS_DAY_PREPARATION_MOULD_CHANGE, actionType);
    }

    /**
     * 判断实际准备起点是否仍在生产业务日之前，不沿用顺延前的候选身份。
     * @param startTime 停机和配额避让后的实际开始时间
     * @param preparationBeforeTime 明确的生产业务日起点
     * @return 是否可以使用跨日准备班次豁免
     */
    private boolean isPreparationBeforeBoundary(Date startTime, Date preparationBeforeTime) {
        return Objects.nonNull(startTime) && Objects.nonNull(preparationBeforeTime)
                && startTime.before(preparationBeforeTime);
    }

    /**
     * 校验跨日准备换模是否能在当前生产业务日日终前完成。
     *
     * @param switchStartTime 换模开始时间
     * @param switchDurationHours 换模时长
     * @param businessDayEndTime 当前生产业务日日终；为空时不追加日终限制
     * @return true-可以承接；false-完成时间到达或越过业务日日终
     */
    private boolean isSwitchCompletionBeforeBusinessDayEnd(
            Date switchStartTime,
            int switchDurationHours,
            Date businessDayEndTime) {
        if (Objects.isNull(businessDayEndTime)) {
            return true;
        }
        Date switchCompleteTime = LhScheduleTimeUtil.addHours(
                switchStartTime, switchDurationHours);
        return Objects.nonNull(switchCompleteTime)
                && switchCompleteTime.before(businessDayEndTime);
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
        // 总时长包含首检，日终等值交给完整时间轴验证首检正量；越过日终仍不承接。
        if (Objects.nonNull(businessDayEndTime)) {
            Date afternoonCompleteTime =
                    LhScheduleTimeUtil.addHours(afternoonTime, switchDurationHours);
            if (Objects.isNull(afternoonCompleteTime)
                    || afternoonCompleteTime.after(businessDayEndTime)) {
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

    /**
     * 读取指定班次已经正式分配的换模/换活字块次数。
     *
     * <p>直接读取 {@code dailyMouldChangeCountMap} 的早班/中班槽位，保证排查字段与
     * {@link #allocateMouldChange}、{@link #rollbackMouldChange} 使用完全相同的实时账本。</p>
     *
     * @param context 排程上下文
     * @param shiftTime 班次内任一时间
     * @return 当前班次已经正式占用的切换次数
     */
    @Override
    public int getAllocatedChangeoverCount(LhScheduleContext context, Date shiftTime) {
        if (Objects.isNull(context) || Objects.isNull(shiftTime)) {
            return 0;
        }
        int[] counts = context.getDailyMouldChangeCountMap().get(formatDateKey(shiftTime));
        if (Objects.isNull(counts)) {
            return 0;
        }
        if (LhScheduleTimeUtil.isMorningShift(context, shiftTime)) {
            return Math.max(0, counts[IDX_MORNING]);
        }
        if (LhScheduleTimeUtil.isAfternoonShift(context, shiftTime)) {
            return Math.max(0, counts[IDX_AFTERNOON]);
        }
        return 0;
    }

    @Override
    public Date previewEndingStaggerMouldChange(LhScheduleContext context,
                                                String machineCode,
                                                Date switchReadyTime,
                                                int switchDurationHours,
                                                SkuScheduleDTO sku,
                                                Map<String, int[]> simulatedCountMap) {
        return this.previewEndingStaggerMouldChange(context, machineCode, switchReadyTime, switchDurationHours,
                sku, ACTION_CHANGEOVER, null, simulatedCountMap);
    }

    @Override
    public Date previewEndingStaggerMouldChange(LhScheduleContext context, String machineCode,
            Date switchReadyTime, int switchDurationHours, SkuScheduleDTO sku, String actionType,
            Date businessDayEndTime, Map<String, int[]> simulatedCountMap) {
        // 内核不修改Map，失败时不会留下空日期或部分次数；成功才计入组内模拟账本。
        Date time = this.resolveMouldChangeStart(context, machineCode, switchReadyTime,
                switchDurationHours, actionType, businessDayEndTime, null, simulatedCountMap);
        if (Objects.nonNull(time)) {
            this.registerMouldChangeCount(context, time, simulatedCountMap);
        }
        return time;
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
        return Objects.nonNull(context)
                && this.registerMouldChangeCount(context, allocatedTime, context.getDailyMouldChangeCountMap());
    }

    /**
     * 将已通过内核验证的动作登记到指定次数账本。
     * @param context 时间配置
     * @param allocatedTime 合法落点
     * @param countMap 真实账本或独立模拟账本
     * @return 是否登记成功
     */
    private boolean registerMouldChangeCount(LhScheduleContext context, Date allocatedTime, Map<String, int[]> countMap) {
        if (context == null || allocatedTime == null) {
            return false;
        }
        String dateKey = formatDateKey(allocatedTime);
        int[] counts = countMap.computeIfAbsent(dateKey, key -> new int[]{0, 0});
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
