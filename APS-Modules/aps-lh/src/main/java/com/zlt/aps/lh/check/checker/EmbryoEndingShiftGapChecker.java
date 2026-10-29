package com.zlt.aps.lh.check.checker;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.schedule.CxEmbryoEndingShift;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查项4：成型/硫化收尾班差检查（提示级）。
 *
 * <p>校验口径（按硫化排程窗口对齐，v2）：</p>
 * <ul>
 *   <li><b>窗口</b>：本次排程结果 class1~class8 的实际物理起止时刻，取最早班为窗口首班、最晚班为窗口末班
 *       （排程日 2026-10-28 的窗口即 10-27 早班 ~ 10-29 中班，不做写死）；</li>
 *   <li><b>硫化收尾</b>：取该胎胚在窗口内「最后一个有排量的班」；只有该班 classNIsEnd=1 才算收尾。
 *       若收尾标记之后仍有排量（说明窗口内并未真正收尾、只是跨段残留的收尾标记），整条跳过不比较；</li>
 *   <li><b>成型收尾</b>：只取已排程收尾（{@code SHIFT_SOURCE=1}），预测班（{@code SHIFT_SOURCE=2}，窗口外最多 4 班推算）
 *       不参与班差；成型尚未实排收尾时整条跳过；</li>
 *   <li><b>窗口外</b>：成型实排收尾落在硫化窗口之外时，不再折算「班差」，单独提示窗口未覆盖；</li>
 *   <li><b>比较</b>：两侧均换算为绝对班序号后比较，差值 &gt; 阈值（2）记异常。</li>
 * </ul>
 *
 * <p>绝对班序号 = 归属日 epochDay × 3 + 班型序（夜0/早1/中2）。硫化班按 classN 的物理开始时刻判定班型：
 * 22~24点属次日夜班、0~6点为夜班尾段、6~14点为早班、14~22点为中班。</p>
 *
 * <p>提示文案统一用「物理时刻 + class 槽位」表达，避免成型班型序（1夜/2早/3中）与硫化 class 槽位（1~8）
 * 两套坐标混在同一句话里造成误读。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
@Component
public class EmbryoEndingShiftGapChecker implements LhScheduleChecker {

    /** 收尾班差阈值：绝对班序号差超过该值才记异常（1 天 = 3 个班型单位） */
    static final int GAP_THRESHOLD = 2;

    /** 每天班型数量（夜/早/中） */
    private static final int SHIFT_KIND_PER_DAY = 3;

    /** 一天毫秒数 */
    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    /** 收尾标记值 */
    private static final String END_FLAG_YES = "1";

    /** 成型班次来源：已排程（预测班不参与班差） */
    private static final String SHIFT_SOURCE_SCHEDULED = "1";

    /** 展示用时刻格式 */
    private static final String DISPLAY_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    /** 展示用日期格式 */
    private static final String DISPLAY_DATE_PATTERN = "yyyy-MM-dd";

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.endingShiftGap";

    /** 班差异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.endingShiftGap";

    /** 成型收尾出窗消息多语言 key */
    private static final String OUT_OF_WINDOW_KEY = "ui.data.alert.lhScheduleCheck.endingShiftOutOfWindow";

    /** 班型：夜班 */
    private static final String KIND_NIGHT_KEY = "ui.data.alert.lhScheduleCheck.shiftKindNight";

    /** 班型：早班 */
    private static final String KIND_MORNING_KEY = "ui.data.alert.lhScheduleCheck.shiftKindMorning";

    /** 班型：中班 */
    private static final String KIND_AFTERNOON_KEY = "ui.data.alert.lhScheduleCheck.shiftKindAfternoon";

    @Override
    public String getCode() {
        return CODE_ENDING_SHIFT_GAP;
    }

    @Override
    public String getNameKey() {
        return NAME_KEY;
    }

    @Override
    public boolean isHardControl() {
        return false;
    }

    @Override
    public List<LhScheduleCheckResult> check(LhScheduleCheckContext context) {
        List<LhScheduleCheckResult> abnormalList = new ArrayList<>();
        if (!context.hasResults()) {
            return abnormalList;
        }
        // 1. 先算本次排程的物理窗口，窗口内/外决定用哪种提示口径
        ShiftWindow window = this.resolveWindow(context);
        if (window == null) {
            log.warn("硫化收尾班差校验：排程结果无有效班次开始时刻，无法确定窗口，跳过, factoryCode={}, scheduleDate={}",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            return abnormalList;
        }
        // 2. 硫化侧：窗口内真正收尾的胎胚（收尾班之后仍有排量的不算）
        Map<String, ShiftOrder> lhEndingOrders = this.resolveLhEndingOrders(context);
        if (lhEndingOrders.isEmpty()) {
            return abnormalList;
        }
        // 3. 成型侧：只认已排程收尾
        Map<String, ShiftOrder> cxEndingOrders = this.resolveCxEndingOrders(context.getCxEndingShiftList());
        for (Map.Entry<String, ShiftOrder> entry : lhEndingOrders.entrySet()) {
            String embryoCode = entry.getKey();
            ShiftOrder cxOrder = cxEndingOrders.get(embryoCode);
            if (cxOrder == null) {
                // 成型只有预测收尾或窗口内无收尾记录，无实排口径可比
                continue;
            }
            ShiftOrder lhOrder = entry.getValue();
            // 4a. 成型收尾落在硫化窗口之外：不折算班差，改为提示窗口未覆盖
            if (cxOrder.getAbsoluteOrder() < window.getStartOrder()
                    || cxOrder.getAbsoluteOrder() > window.getEndOrder()) {
                abnormalList.add(this.buildOutOfWindowRecord(embryoCode, cxOrder, lhOrder, window));
                continue;
            }
            // 4b. 窗口内比较绝对班序号差
            long gap = Math.abs(lhOrder.getAbsoluteOrder() - cxOrder.getAbsoluteOrder());
            if (gap <= GAP_THRESHOLD) {
                continue;
            }
            abnormalList.add(this.buildGapRecord(embryoCode, cxOrder, lhOrder, gap, window));
        }
        return abnormalList;
    }

    /**
     * 计算本次排程的物理窗口（最早班 ~ 最晚班）。
     *
     * @param context 校验上下文
     * @return 窗口；结果集无有效开始时刻返回 null
     */
    private ShiftWindow resolveWindow(LhScheduleCheckContext context) {
        long startOrder = Long.MAX_VALUE;
        long endOrder = Long.MIN_VALUE;
        Date startTime = null;
        Date endTime = null;
        for (LhScheduleResult result : context.getResults()) {
            for (int shiftIndex = 1; shiftIndex <= SHIFT_SLOT_COUNT; shiftIndex++) {
                Date slotStartTime = context.getShiftStartTime(result, shiftIndex);
                if (slotStartTime == null) {
                    continue;
                }
                long absoluteOrder = this.resolveAbsoluteOrderByStartTime(slotStartTime);
                // 同一绝对班序可能对应多个开班时刻（换模/插单导致的延迟开班），统一取最早的规范时刻
                if (absoluteOrder < startOrder
                        || (absoluteOrder == startOrder && slotStartTime.before(startTime))) {
                    startOrder = absoluteOrder;
                    startTime = slotStartTime;
                }
                if (absoluteOrder > endOrder
                        || (absoluteOrder == endOrder && slotStartTime.before(endTime))) {
                    endOrder = absoluteOrder;
                    endTime = slotStartTime;
                }
            }
        }
        if (startTime == null) {
            return null;
        }
        return new ShiftWindow(startOrder, endOrder, startTime, endTime);
    }

    /**
     * 解析硫化侧各胎胚的最晚收尾班。
     *
     * <p>先找该胎胚在窗口内最后一个有排量的班，只有该班标了收尾才算收尾；
     * 收尾标记之后仍有排量的（跨段残留标记）整条跳过。</p>
     *
     * @param context 校验上下文
     * @return 胎胚编码 → 收尾班
     */
    private Map<String, ShiftOrder> resolveLhEndingOrders(LhScheduleCheckContext context) {
        Map<String, EmbryoEndingAggregate> aggregateMap = new LinkedHashMap<>(16);
        for (LhScheduleResult result : context.getResults()) {
            String embryoCode = StringUtils.trimToNull(result.getEmbryoCode());
            if (embryoCode == null) {
                continue;
            }
            EmbryoEndingAggregate aggregate = aggregateMap.computeIfAbsent(
                    embryoCode, key -> new EmbryoEndingAggregate());
            for (int shiftIndex = 1; shiftIndex <= SHIFT_SLOT_COUNT; shiftIndex++) {
                Date startTime = context.getShiftStartTime(result, shiftIndex);
                if (startTime == null) {
                    // 无物理开始时刻的班次无法定位到自然班次，跳过
                    continue;
                }
                long absoluteOrder = this.resolveAbsoluteOrderByStartTime(startTime);
                Integer planQty = context.getShiftPlanQty(result, shiftIndex);
                if (planQty != null && planQty > 0 && absoluteOrder > aggregate.getLastPlanOrder()) {
                    aggregate.setLastPlanOrder(absoluteOrder);
                }
                boolean isEnd = END_FLAG_YES.equals(
                        StringUtils.trimToEmpty(context.getShiftIsEnd(result, shiftIndex)));
                if (isEnd && absoluteOrder > aggregate.getEndingOrder()) {
                    aggregate.setEndingOrder(absoluteOrder);
                    aggregate.setEndingShiftIndex(shiftIndex);
                    aggregate.setEndingStartTime(startTime);
                }
            }
        }
        Map<String, ShiftOrder> endingOrders = new LinkedHashMap<>(aggregateMap.size());
        for (Map.Entry<String, EmbryoEndingAggregate> entry : aggregateMap.entrySet()) {
            EmbryoEndingAggregate aggregate = entry.getValue();
            if (aggregate.getEndingShiftIndex() == 0) {
                // 窗口内没有任何收尾标记
                continue;
            }
            if (aggregate.getLastPlanOrder() > aggregate.getEndingOrder()) {
                // 收尾班之后仍有排量：窗口内并未真正收尾，不参与班差比较
                continue;
            }
            endingOrders.put(entry.getKey(), new ShiftOrder(aggregate.getEndingOrder(),
                    aggregate.getEndingShiftIndex(), aggregate.getEndingStartTime()));
        }
        return endingOrders;
    }

    /**
     * 解析成型侧各胎胚最晚收尾班（仅已排程收尾）。
     *
     * @param cxEndingShiftList 成型收尾记录
     * @return 胎胚编码 → 收尾班
     */
    private Map<String, ShiftOrder> resolveCxEndingOrders(List<CxEmbryoEndingShift> cxEndingShiftList) {
        Map<String, ShiftOrder> endingOrders = new LinkedHashMap<>(16);
        if (cxEndingShiftList == null || cxEndingShiftList.isEmpty()) {
            return endingOrders;
        }
        for (CxEmbryoEndingShift cxEnding : cxEndingShiftList) {
            String embryoCode = StringUtils.trimToNull(cxEnding.getEmbryoCode());
            if (embryoCode == null || cxEnding.getShiftDate() == null) {
                continue;
            }
            // 预测班（窗口外推算）不参与班差，避免与实际排程口径混比
            if (!SHIFT_SOURCE_SCHEDULED.equals(StringUtils.trimToEmpty(cxEnding.getShiftSource()))) {
                continue;
            }
            long absoluteOrder = this.resolveCxAbsoluteOrder(cxEnding.getShiftDate(), cxEnding.getShiftIndex());
            ShiftOrder current = endingOrders.get(embryoCode);
            if (current == null || absoluteOrder > current.getAbsoluteOrder()) {
                int kindOrder = this.resolveCxKindOrder(cxEnding.getShiftIndex());
                endingOrders.put(embryoCode, new ShiftOrder(absoluteOrder, kindOrder + 1,
                        this.resolveCxStartTime(cxEnding.getShiftDate(), kindOrder)));
            }
        }
        return endingOrders;
    }

    /**
     * 构建班差异常记录。
     *
     * @param embryoCode 胎胚编码
     * @param cxOrder 成型收尾班
     * @param lhOrder 硫化收尾班
     * @param gap 绝对班序号差
     * @param window 排程窗口
     * @return 异常记录
     */
    private LhScheduleCheckResult buildGapRecord(String embryoCode, ShiftOrder cxOrder, ShiftOrder lhOrder,
                                                 long gap, ShiftWindow window) {
        LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
        abnormal.setCheckCode(this.getCode());
        abnormal.setDimensionKey(embryoCode);
        abnormal.setMessage(MessageFormat.format(I18nUtil.getMessage(MESSAGE_KEY),
                embryoCode, this.formatShiftLabel(cxOrder.getStartTime(), false, 0),
                this.formatShiftLabel(lhOrder.getStartTime(), true, lhOrder.getShiftIndex()),
                String.valueOf(gap), this.formatDayCount(gap)));
        abnormal.setDetailJson(JSONUtil.toJsonStr(
                this.buildDetail(cxOrder, lhOrder, gap, window, false)));
        return abnormal;
    }

    /**
     * 构建成型收尾出窗异常记录。
     *
     * @param embryoCode 胎胚编码
     * @param cxOrder 成型收尾班
     * @param lhOrder 硫化收尾班
     * @param window 排程窗口
     * @return 异常记录
     */
    private LhScheduleCheckResult buildOutOfWindowRecord(String embryoCode, ShiftOrder cxOrder,
                                                         ShiftOrder lhOrder, ShiftWindow window) {
        LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
        abnormal.setCheckCode(this.getCode());
        abnormal.setDimensionKey(embryoCode);
        abnormal.setMessage(MessageFormat.format(I18nUtil.getMessage(OUT_OF_WINDOW_KEY),
                embryoCode, this.formatShiftLabel(cxOrder.getStartTime(), false, 0),
                this.formatShiftLabel(window.getStartTime(), false, 0),
                this.formatShiftLabel(window.getEndTime(), false, 0)));
        abnormal.setDetailJson(JSONUtil.toJsonStr(
                this.buildDetail(cxOrder, lhOrder, Math.abs(lhOrder.getAbsoluteOrder() - cxOrder.getAbsoluteOrder()),
                        window, true)));
        return abnormal;
    }

    /**
     * 构建异常明细。
     *
     * @param cxOrder 成型收尾班
     * @param lhOrder 硫化收尾班
     * @param gap 绝对班序号差
     * @param window 排程窗口
     * @param outOfWindow 是否窗口外
     * @return 明细键值对
     */
    private Map<String, Object> buildDetail(ShiftOrder cxOrder, ShiftOrder lhOrder, long gap,
                                            ShiftWindow window, boolean outOfWindow) {
        Map<String, Object> detail = new LinkedHashMap<>(12);
        detail.put("cxAbsoluteOrder", cxOrder.getAbsoluteOrder());
        detail.put("cxStartTime", this.formatTime(cxOrder));
        detail.put("lhAbsoluteOrder", lhOrder.getAbsoluteOrder());
        detail.put("lhShiftIndex", lhOrder.getShiftIndex());
        detail.put("lhStartTime", this.formatTime(lhOrder));
        detail.put("gap", gap);
        detail.put("outOfWindow", outOfWindow);
        if (window != null) {
            detail.put("windowStartOrder", window.getStartOrder());
            detail.put("windowEndOrder", window.getEndOrder());
            detail.put("windowStartTime", DateUtil.format(window.getStartTime(), DISPLAY_TIME_PATTERN));
            detail.put("windowEndTime", DateUtil.format(window.getEndTime(), DISPLAY_TIME_PATTERN));
        }
        return detail;
    }

    /**
     * 计算成型某班的绝对班序号。
     *
     * @param shiftDate 班次归属日
     * @param shiftIndex 班次编码（01夜/02早/03中，兼容 "1"/"01"）
     * @return 绝对班序号
     */
    private long resolveCxAbsoluteOrder(Date shiftDate, String shiftIndex) {
        return this.epochDay(shiftDate) * SHIFT_KIND_PER_DAY + this.resolveCxKindOrder(shiftIndex);
    }

    /**
     * 成型班次编码 → 班型序（01夜→0，02早→1，03中→2）。
     *
     * @param shiftIndex 班次编码
     * @return 班型序
     */
    private int resolveCxKindOrder(String shiftIndex) {
        String normalized = StringUtils.leftPad(StringUtils.trimToEmpty(shiftIndex), 2, '0');
        switch (normalized) {
            case "02":
                return 1;
            case "03":
                return 2;
            case "01":
            default:
                return 0;
        }
    }

    /**
     * 成型班次 → 物理开班时刻（夜班 22:00 属归属日前一天）。
     *
     * @param shiftDate 班次归属日
     * @param kindOrder 班型序（0夜/1早/2中）
     * @return 开班时刻
     */
    private Date resolveCxStartTime(Date shiftDate, int kindOrder) {
        Date dayStart = DateUtil.beginOfDay(shiftDate);
        switch (kindOrder) {
            case 1:
                return DateUtil.offsetHour(dayStart, 6);
            case 2:
                return DateUtil.offsetHour(dayStart, 14);
            case 0:
            default:
                // 夜班 22:00 开班，归属日为结束日
                return DateUtil.offsetHour(dayStart, -2);
        }
    }

    /**
     * 按物理开始时刻换算绝对班序号。
     *
     * @param startTime 物理开始时刻
     * @return 绝对班序号
     */
    private long resolveAbsoluteOrderByStartTime(Date startTime) {
        int hour = DateUtil.hour(startTime, true);
        long baseEpochDay = this.epochDay(startTime);
        if (hour >= 22) {
            // 22点起为夜班，归属次日
            return (baseEpochDay + 1) * SHIFT_KIND_PER_DAY;
        }
        if (hour < 6) {
            // 0~6点为夜班尾段，归属当日
            return baseEpochDay * SHIFT_KIND_PER_DAY;
        }
        if (hour < 14) {
            // 6~14点早班
            return baseEpochDay * SHIFT_KIND_PER_DAY + 1;
        }
        // 14~22点中班
        return baseEpochDay * SHIFT_KIND_PER_DAY + 2;
    }

    /**
     * 计算日期归属的 epochDay（自然日序号）。
     *
     * @param date 日期
     * @return epochDay
     */
    private long epochDay(Date date) {
        return DateUtil.beginOfDay(date).getTime() / MILLIS_PER_DAY;
    }

    /**
     * 格式化班次标签：归属日 + 班型（如 2026-10-31 夜班），与业务台账的班次口径一致。
     *
     * <p>跨天夜班按归属日展示（22:00 开班归属次日），避免出现「10-30 22:00」这种与台账日期对不上的表达。</p>
     *
     * @param startTime 开班时刻
     * @param withClass 是否附加 class 槽位
     * @param shiftIndex class 槽位序号
     * @return 展示文本
     */
    private String formatShiftLabel(Date startTime, boolean withClass, int shiftIndex) {
        if (startTime == null) {
            return StringUtils.EMPTY;
        }
        int hour = DateUtil.hour(startTime, true);
        int kindOrder;
        Date shiftDate;
        if (hour >= 22) {
            kindOrder = 0;
            shiftDate = DateUtil.offsetDay(DateUtil.beginOfDay(startTime), 1);
        } else if (hour < 6) {
            kindOrder = 0;
            shiftDate = DateUtil.beginOfDay(startTime);
        } else if (hour < 14) {
            kindOrder = 1;
            shiftDate = DateUtil.beginOfDay(startTime);
        } else {
            kindOrder = 2;
            shiftDate = DateUtil.beginOfDay(startTime);
        }
        String label = DateUtil.format(shiftDate, DISPLAY_DATE_PATTERN) + StringUtils.SPACE
                + this.resolveKindLabel(kindOrder);
        return withClass ? label + "(class" + shiftIndex + ")" : label;
    }

    /**
     * 班型序 → 多语言班型名称。
     *
     * @param kindOrder 班型序（0夜/1早/2中）
     * @return 班型名称
     */
    private String resolveKindLabel(int kindOrder) {
        switch (kindOrder) {
            case 1:
                return I18nUtil.getMessage(KIND_MORNING_KEY);
            case 2:
                return I18nUtil.getMessage(KIND_AFTERNOON_KEY);
            case 0:
            default:
                return I18nUtil.getMessage(KIND_NIGHT_KEY);
        }
    }

    /**
     * 格式化班次时刻（空值返回空串），用于异常明细留存原始时刻。
     *
     * @param order 班次
     * @return yyyy-MM-dd HH:mm
     */
    private String formatTime(ShiftOrder order) {
        return order == null || order.getStartTime() == null
                ? StringUtils.EMPTY : DateUtil.format(order.getStartTime(), DISPLAY_TIME_PATTERN);
    }

    /**
     * 绝对班序号差 → 约合天数（1 天 = 3 个班型单位）。
     *
     * @param gap 绝对班序号差
     * @return 天数文本（整除不带小数）
     */
    private String formatDayCount(long gap) {
        if (gap % SHIFT_KIND_PER_DAY == 0) {
            return String.valueOf(gap / SHIFT_KIND_PER_DAY);
        }
        return String.format("%.1f", gap / (double) SHIFT_KIND_PER_DAY);
    }

    /**
     * 排程窗口（最早班 ~ 最晚班）。
     */
    @Data
    private static class ShiftWindow {

        /** 窗口首班绝对班序号 */
        private final long startOrder;

        /** 窗口末班绝对班序号 */
        private final long endOrder;

        /** 窗口首班开班时刻 */
        private final Date startTime;

        /** 窗口末班开班时刻 */
        private final Date endTime;
    }

    /**
     * 胎胚收尾聚合（窗口内最后排量班 + 最后一个收尾班）。
     */
    @Data
    private static class EmbryoEndingAggregate {

        /** 窗口内最后一个有排量的班（绝对班序号） */
        private long lastPlanOrder = Long.MIN_VALUE;

        /** 窗口内最后一个收尾班（绝对班序号） */
        private long endingOrder = Long.MIN_VALUE;

        /** 收尾班槽位（1~8，0 表示无收尾） */
        private int endingShiftIndex;

        /** 收尾班开班时刻 */
        private Date endingStartTime;
    }

    /**
     * 收尾班信息（绝对序号 + 班次序号 + 开班时刻）。
     */
    @Data
    private static class ShiftOrder {

        /** 绝对班序号 */
        private final long absoluteOrder;

        /** 班次序号（硫化侧为 class 槽位 1~8；成型侧为班型序 1夜/2早/3中） */
        private final int shiftIndex;

        /** 开班时刻 */
        private final Date startTime;
    }
}
