package com.zlt.aps.cx.service.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.mapper.MdmWorkCalendarMapper;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开/停产班次判定工具 — 读取 {@code T_MDM_WORK_CALENDAR}，按<b>班次粒度</b>（非天粒度）判定当前班次类型。
 *
 * <h3>在排程中的位置</h3>
 * <pre>
 * CoreScheduleAlgorithmServiceImpl.executeSchedule
 *   → preloadCache（排程开始前一次性加载日历）
 *   → 每班次 isShiftStopped → 停产班跳过整班排程
 * TaskGroupService
 *   → determineShiftType → OPEN_START 关键产品过滤 / 开停产任务标记 / 停产反推
 * ShiftScheduleService
 *   → determineShiftType → 开产首班6h封顶 / 停产前班反推 / 停产班策略
 * </pre>
 *
 * <h3>数据源与字段</h3>
 * <ul>
 *   <li>表：{@code T_MDM_WORK_CALENDAR}（实体 {@link MdmWorkCalendar}）</li>
 *   <li>工序：优先 {@code PROC_CODE='CX'}（成型），无数据时回退 {@code '03'}</li>
 *   <li>班次标志：{@code ONE_SHIFT_FLAG} / {@code TWO_SHIFT_FLAG} / {@code THREE_SHIFT_FLAG}，
 *       {@code 0=停产}、{@code 1=开产}</li>
 *   <li>天标志：{@code DAY_FLAG}（仅用于 {@link #findNearestDayFlag} 等<b>按天</b>兼容方法，开产判定已废弃天级 API）</li>
 * </ul>
 *
 * <h3>核心判定：{@link #determineShiftType}（班次级，不可与天级 DAY_FLAG 混用）</h3>
 * <table>
 *   <tr><th>ShiftType</th><th>条件</th><th>典型下游行为</th></tr>
 *   <tr><td>CLOSED</td><td>本班 SHIFT_FLAG=0</td><td>主循环跳过该班；任务标记 isClosingDayTask</td></tr>
 *   <tr><td>OPEN_START</td><td>本班=1 且 上班=0</td><td>首班6小时产能封顶；开产首班不排关键产品（除非全结构关键）</td></tr>
 *   <tr><td>BEFORE_CLOSE</td><td>本班=1 且 下班=0</td><td>停产前班反推胎胚需求；跨天封顶</td></tr>
 *   <tr><td>NORMAL</td><td>本班=1 且 上下班班均=1</td><td>普通波浪分配</td></tr>
 * </table>
 *
 * <h3>班次时间轴（默认每天 3 班，dayShiftOrder 1→2→3）</h3>
 * <pre>
 * … → D-1三班 → D一班 → D二班 → D三班 → D+1一班 → …
 * getPreviousShiftFlag：一班看前一天末班；getNextShiftFlag：三班看次日一班
 * </pre>
 *
 * <h3>缓存策略</h3>
 * <p>Key = {@code factoryCode|yyyy-MM-dd}。排程入口 {@link #preloadCache} 加载排程区间 ±{@link #CACHE_EXTRA_DAYS} 天；
 * 懒加载/越界时 {@link #extendCache} 扩展。无日历记录时班次标志<b>默认视为开产（"1"）</b>。
 *
 * @author APS Team
 * @see com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl
 * @see TaskGroupService
 * @see ShiftScheduleService
 */
@Slf4j
@Component
public class ScheduleDayTypeHelper {

    @Autowired
    private MdmWorkCalendarMapper workCalendarMapper;

    /** 工作日历班次标志：停产 */
    private static final String SHIFT_FLAG_STOP = "0";

    /** 工作日历班次标志：开产 */
    private static final String SHIFT_FLAG_START = "1";

    /** 日历缓存：{@code factoryCode|yyyy-MM-dd} → 当日 {@link MdmWorkCalendar} 行 */
    private Map<String, MdmWorkCalendar> calendarCache = new HashMap<>();

    /** 是否已完成至少一次 preload/extend */
    private volatile boolean cacheLoaded = false;

    /** 当前缓存主工厂（懒加载时从数据行回填） */
    private String cachedFactoryCode;

    /** 缓存覆盖的日期闭区间 */
    private LocalDate cacheStartDate;
    private LocalDate cacheEndDate;

    /** preload 时在请求区间两侧额外加载的天数，避免前后班次跨日查不到 */
    private static final int CACHE_EXTRA_DAYS = 5;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // ==================== ShiftType：班次业务语义 ====================

    /**
     * 班次类型枚举 — {@link #determineShiftType} 的返回值，驱动开停产分支。
     *
     * <p>判定仅依赖相邻班次的 SHIFT_FLAG，<b>不</b>使用 DAY_FLAG。
     */
    public enum ShiftType {
        /** 本班 SHIFT_FLAG=0：该班次不排产或走停产精排策略 */
        CLOSED("停产班"),
        /** 本班=1 且上一班=0：开产后的第一个生产班次（首班产能、关键产品规则） */
        OPEN_START("开产首个班次"),
        /** 本班=1 且下一班=0：停产前的最后一个生产班次（停锅反推、跨天封顶） */
        BEFORE_CLOSE("停产前一个班次"),
        /** 本班=1 且上下班班均为 1：常规定额/波浪分配 */
        NORMAL("正常班");

        private final String desc;

        ShiftType(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 按天停产标识查询结果 — 供 {@link #findNearestDayFlag} / {@link #isStopDay} 等<b>天级兼容 API</b>使用。
     */
    public static class DayFlagInfo {
        /** 向前扫描命中的、带 DAY_FLAG 的日期 */
        public final LocalDate nearestDate;
        /** DAY_FLAG：0=停，1=开 */
        public final String dayFlag;

        public DayFlagInfo(LocalDate nearestDate, String dayFlag) {
            this.nearestDate = nearestDate;
            this.dayFlag = dayFlag;
        }
    }

    /**
     * 单班次标志快照（调试/扩展用数据结构）。
     */
    public static class ShiftFlagInfo {
        public final LocalDate date;
        public final int shiftOrder;
        public final String shiftName;
        /** 0=停，1=开 */
        public final String flag;

        public ShiftFlagInfo(LocalDate date, int shiftOrder, String shiftName, String flag) {
            this.date = date;
            this.shiftOrder = shiftOrder;
            this.shiftName = shiftName;
            this.flag = flag;
        }
    }

    // ==================== 缓存：加载、查询、扩展 ====================

    /** 构造缓存键：{@code factoryCode|LocalDate} */
    private String buildCacheKey(String factoryCode, LocalDate date) {
        return (factoryCode != null ? factoryCode : "UNKNOWN") + "|" + date.toString();
    }

    /** 构造缓存键：{@code factoryCode|yyyy-MM-dd} */
    private String buildCacheKey(String factoryCode, String dateStr) {
        return (factoryCode != null ? factoryCode : "UNKNOWN") + "|" + dateStr;
    }

    /**
     * 预加载工作日历（推荐在 {@code executeSchedule} 开头调用一次）。
     *
     * <p><b>查询回退链</b>（依次尝试直到有数据）：
     * <ol>
     *   <li>PROC_CODE=CX + factoryCode + 日期区间</li>
     *   <li>PROC_CODE=03 + factoryCode</li>
     *   <li>PROC_CODE=CX 不限工厂</li>
     *   <li>PROC_CODE=03 不限工厂</li>
     * </ol>
     *
     * <p>实际查询区间为 {@code [startDate - CACHE_EXTRA_DAYS, endDate + CACHE_EXTRA_DAYS]}。
     *
     * @param startDate   排程起始日（含）
     * @param endDate     排程结束日（含）
     * @param factoryCode 工厂编码；可为 null（从结果行回填）
     */
    public void preloadCache(LocalDate startDate, LocalDate endDate, String factoryCode) {
        LocalDate actualStart = startDate.minusDays(CACHE_EXTRA_DAYS);
        LocalDate actualEnd = endDate.plusDays(CACHE_EXTRA_DAYS);

        log.info("预加载工作日历缓存: 工厂={}, 日期范围={} ~ {}", factoryCode, actualStart, actualEnd);

        try {
            LambdaQueryWrapper<MdmWorkCalendar> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmWorkCalendar::getProcCode, "CX");
            if (factoryCode != null && !factoryCode.isEmpty()) {
                wrapper.eq(MdmWorkCalendar::getFactoryCode, factoryCode);
            }
            wrapper.ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                    .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));

            List<MdmWorkCalendar> list = workCalendarMapper.selectList(wrapper);

            if (list.isEmpty() && factoryCode != null) {
                log.info("PROC_CODE='CX' 未查到工作日历数据(工厂={}), 尝试 PROC_CODE='03'", factoryCode);
                LambdaQueryWrapper<MdmWorkCalendar> wrapper03 = new LambdaQueryWrapper<>();
                wrapper03.eq(MdmWorkCalendar::getProcCode, "03")
                        .eq(MdmWorkCalendar::getFactoryCode, factoryCode)
                        .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                        .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));
                list = workCalendarMapper.selectList(wrapper03);
            }

            if (list.isEmpty() && factoryCode != null) {
                log.info("按工厂+工序仍查不到数据, 尝试仅按 PROC_CODE='CX' 不限工厂");
                LambdaQueryWrapper<MdmWorkCalendar> wrapperNoFactory = new LambdaQueryWrapper<>();
                wrapperNoFactory.eq(MdmWorkCalendar::getProcCode, "CX")
                        .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                        .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));
                list = workCalendarMapper.selectList(wrapperNoFactory);

                if (list.isEmpty()) {
                    log.info("仅按 PROC_CODE='CX' 也查不到, 尝试仅按 PROC_CODE='03' 不限工厂");
                    LambdaQueryWrapper<MdmWorkCalendar> wrapper03NoFactory = new LambdaQueryWrapper<>();
                    wrapper03NoFactory.eq(MdmWorkCalendar::getProcCode, "03")
                            .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                            .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));
                    list = workCalendarMapper.selectList(wrapper03NoFactory);
                }
            }

            calendarCache.clear();
            String effectiveFactoryCode = factoryCode;
            for (MdmWorkCalendar calendar : list) {
                if (calendar.getProductionDate() != null) {
                    String calFactoryCode = calendar.getFactoryCode() != null ? calendar.getFactoryCode() : factoryCode;
                    if (effectiveFactoryCode == null && calFactoryCode != null) {
                        effectiveFactoryCode = calFactoryCode;
                    }
                    String dateStr = formatDateKey(calendar.getProductionDate());
                    String key = buildCacheKey(calFactoryCode, dateStr);
                    calendarCache.put(key, calendar);
                }
            }

            cachedFactoryCode = effectiveFactoryCode;
            cacheStartDate = actualStart;
            cacheEndDate = actualEnd;
            cacheLoaded = true;

            log.info("工作日历缓存加载完成: 工厂={}, {} 条记录", effectiveFactoryCode, list.size());

            for (MdmWorkCalendar cal : list) {
                if (cal.getProductionDate() != null) {
                    String dateStr = formatDateKey(cal.getProductionDate());
                    log.info("工作日历缓存: 日期={}, 一班={}, 二班={}, 三班={}, DAY_FLAG={}, 工厂={}",
                            dateStr, cal.getOneShiftFlag(), cal.getTwoShiftFlag(),
                            cal.getThreeShiftFlag(), cal.getDayFlag(), cal.getFactoryCode());
                }
            }
        } catch (Exception e) {
            log.error("预加载工作日历缓存失败", e);
            cacheLoaded = false;
        }
    }

    /** {@link #preloadCache(LocalDate, LocalDate, String)} 的 factoryCode=null 重载 */
    public void preloadCache(LocalDate startDate, LocalDate endDate) {
        preloadCache(startDate, endDate, null);
    }

    /**
     * 懒加载：未 preload 或工厂切换时，以 queryDate ±30 天触发 preload。
     */
    private void ensureCacheLoaded(LocalDate date, String factoryCode) {
        if (!cacheLoaded || (factoryCode != null && !factoryCode.equals(cachedFactoryCode))) {
            LocalDate start = date.minusDays(30);
            LocalDate end = date.plusDays(30);
            preloadCache(start, end, factoryCode);
        }
    }

    private String formatDateKey(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    /**
     * 按工厂+日期取日历行：先查缓存，越界则 extend，仍无则返回 null（调用方多默认开产）。
     */
    private MdmWorkCalendar getCalendar(LocalDate queryDate, String factoryCode) {
        ensureCacheLoaded(queryDate, factoryCode);

        String key = buildCacheKey(factoryCode, queryDate);
        MdmWorkCalendar calendar = calendarCache.get(key);

        if (calendar == null && cachedFactoryCode != null && !cachedFactoryCode.equals(factoryCode)) {
            key = buildCacheKey(cachedFactoryCode, queryDate);
            calendar = calendarCache.get(key);
        }

        if (calendar == null && cacheStartDate != null && cacheEndDate != null) {
            if (queryDate.isBefore(cacheStartDate) || queryDate.isAfter(cacheEndDate)) {
                extendCache(queryDate, factoryCode);

                key = buildCacheKey(factoryCode, queryDate);
                calendar = calendarCache.get(key);

                if (calendar == null && cachedFactoryCode != null) {
                    key = buildCacheKey(cachedFactoryCode, queryDate);
                    calendar = calendarCache.get(key);
                }
            }
        }

        return calendar;
    }

    /** 查询日落入缓存区间外时，向前后各扩展约 30 天并 merge 查询结果 */
    private synchronized void extendCache(LocalDate date, String factoryCode) {
        if (cacheStartDate != null && cacheEndDate != null) {
            if (date.isBefore(cacheStartDate) || date.isAfter(cacheEndDate)) {
                log.info("扩展工作日历缓存: 工厂={}, 当前范围 {} ~ {}, 新增日期 {}",
                        factoryCode, cacheStartDate, cacheEndDate, date);

                LocalDate newStart = cacheStartDate.isBefore(date) ? cacheStartDate : date.minusDays(30);
                LocalDate newEnd = cacheEndDate.isAfter(date) ? cacheEndDate : date.plusDays(30);

                LambdaQueryWrapper<MdmWorkCalendar> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MdmWorkCalendar::getProcCode, "CX");
                if (factoryCode != null && !factoryCode.isEmpty()) {
                    wrapper.eq(MdmWorkCalendar::getFactoryCode, factoryCode);
                }
                wrapper.ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newStart))
                        .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newEnd));

                List<MdmWorkCalendar> list = workCalendarMapper.selectList(wrapper);

                if (list.isEmpty() && factoryCode != null) {
                    LambdaQueryWrapper<MdmWorkCalendar> wrapper03 = new LambdaQueryWrapper<>();
                    wrapper03.eq(MdmWorkCalendar::getProcCode, "03")
                            .eq(MdmWorkCalendar::getFactoryCode, factoryCode)
                            .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newStart))
                            .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newEnd));
                    list = workCalendarMapper.selectList(wrapper03);
                }

                for (MdmWorkCalendar calendar : list) {
                    if (calendar.getProductionDate() != null) {
                        String calFactoryCode = calendar.getFactoryCode() != null ? calendar.getFactoryCode() : factoryCode;
                        String dateKey = formatDateKey(calendar.getProductionDate());
                        String key = buildCacheKey(calFactoryCode, dateKey);
                        calendarCache.put(key, calendar);
                    }
                }

                cacheStartDate = newStart;
                cacheEndDate = newEnd;
            }
        }
    }

    private String getShiftName(int shiftOrder) {
        switch (shiftOrder) {
            case 1: return "一班";
            case 2: return "二班";
            case 3: return "三班";
            default: return "未知班次";
        }
    }

    /**
     * 每日班次数。当前固定 3；若未来班次配置可变，应与此处及 prev/next 逻辑联动修改。
     */
    private int getShiftsPerDay(LocalDate date) {
        return 3;
    }

    // ==================== 班次 SHIFT_FLAG 读取与相邻班次 ====================

    /**
     * 获取指定班次的开停产标志（带工厂编号）
     *
     * @param date        日期
     * @param shiftOrder  班次序号（1,2,3）
     * @param factoryCode 工厂编号
     * @return 开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    public String getShiftFlag(LocalDate date, int shiftOrder, String factoryCode) {
        MdmWorkCalendar calendar = getCalendar(date, factoryCode);

        if (calendar == null) {
            log.warn("未找到工作日历配置，工厂={}, 日期={}, 班次={}, 默认视为开产", factoryCode, date, getShiftName(shiftOrder));
            return SHIFT_FLAG_START;
        }

        switch (shiftOrder) {
            case 1:
                return calendar.getOneShiftFlag();
            case 2:
                return calendar.getTwoShiftFlag();
            case 3:
                return calendar.getThreeShiftFlag();
            default:
                log.warn("未知的班次序号: {}，默认视为开产", shiftOrder);
                return SHIFT_FLAG_START;
        }
    }

    /**
     * 获取上一个班次的开停产标志（带工厂编号）
     */
    public String getPreviousShiftFlag(LocalDate date, int shiftOrder, String factoryCode) {
        LocalDate prevDate = date;
        int prevShiftOrder;

        // 获取当前排产每天班次数（从班次配置中获取，默认3班）
        int shiftsPerDay = getShiftsPerDay(date);

        switch (shiftOrder) {
            case 1:
                // 一班的"上一个班次" = 前一天的最后一个班次
                prevDate = date.minusDays(1);
                prevShiftOrder = shiftsPerDay;
                break;
            case 2:
                // 二班的"上一个班次" = 当天的一班
                prevShiftOrder = 1;
                break;
            case 3:
                // 三班的"上一个班次" = 当天的二班
                prevShiftOrder = 2;
                break;
            default:
                log.warn("未知的班次序号: {}", shiftOrder);
                return SHIFT_FLAG_START;
        }

        return getShiftFlag(prevDate, prevShiftOrder, factoryCode);
    }

    /**
     * 获取下一个班次的开停产标志（带工厂编号）
     */
    public String getNextShiftFlag(LocalDate date, int shiftOrder, String factoryCode) {
        LocalDate nextDate = date;
        int nextShiftOrder;

        switch (shiftOrder) {
            case 1:
                // 一班的"下一个班次" = 当天的二班
                nextShiftOrder = 2;
                break;
            case 2:
                // 二班的"下一个班次" = 当天的三班
                nextShiftOrder = 3;
                break;
            case 3:
                // 三班的"下一个班次" = 下一天的一班
                nextDate = date.plusDays(1);
                nextShiftOrder = 1;
                break;
            default:
                log.warn("未知的班次序号: {}", shiftOrder);
                return SHIFT_FLAG_START;
        }

        return getShiftFlag(nextDate, nextShiftOrder, factoryCode);
    }

    // ==================== determineShiftType：班次类型核心判定 ====================

    public ShiftType determineShiftType(LocalDate date, int shiftOrder) {
        return determineShiftType(date, shiftOrder, null);
    }

    /**
     * 按班次判定开/停产类型 — TaskGroupService、ShiftScheduleService、CoreScheduleAlgorithmServiceImpl 的统一入口。
     *
     * <p><b>判定顺序（短路，不可调换）</b>：
     * <ol>
     *   <li>本班 flag=0 → {@link ShiftType#CLOSED}</li>
     *   <li>本班=1 且 {@link #getPreviousShiftFlag}=0 → {@link ShiftType#OPEN_START}</li>
     *   <li>本班=1 且 {@link #getNextShiftFlag}=0 → {@link ShiftType#BEFORE_CLOSE}</li>
     *   <li>否则 → {@link ShiftType#NORMAL}</li>
     * </ol>
     *
     * <p>注意：OPEN_START 与 BEFORE_CLOSE 可能同日共存于不同班次；同一班次不会同时为两者。
     */
    public ShiftType determineShiftType(LocalDate date, int shiftOrder, String factoryCode) {
        String currentFlag = getShiftFlag(date, shiftOrder, factoryCode);

        // 1. 判断是否为停产班
        if (SHIFT_FLAG_STOP.equals(currentFlag)) {
            log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 结果=停产班",
                    factoryCode, date, shiftOrder);
            return ShiftType.CLOSED;
        }

        // 2. 本班次是开产，判断是开产首个班还是停产前一个班
        String prevFlag = getPreviousShiftFlag(date, shiftOrder, factoryCode);
        String nextFlag = getNextShiftFlag(date, shiftOrder, factoryCode);

        // 上个班次是停产 -> 开产首个班次
        if (SHIFT_FLAG_STOP.equals(prevFlag)) {
            log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 上个班次停产, 结果=开产首个班次",
                    factoryCode, date, shiftOrder);
            return ShiftType.OPEN_START;
        }

        // 下个班次是停产 -> 停产前一个班次
        if (SHIFT_FLAG_STOP.equals(nextFlag)) {
            log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 下个班次停产, 结果=停产前一个班次",
                    factoryCode, date, shiftOrder);
            return ShiftType.BEFORE_CLOSE;
        }

        // 正常班
        log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 结果=正常班(上下班次均正常)",
                factoryCode, date, shiftOrder);
        return ShiftType.NORMAL;
    }

    // ==================== 便捷布尔判定 ====================

    /** 是否停产班（等价于 {@code determineShiftType == CLOSED}） */
    public boolean isClosingShift(LocalDate date, int shiftOrder, String factoryCode) {
        return determineShiftType(date, shiftOrder, factoryCode) == ShiftType.CLOSED;
    }

    /**
     * 本班 SHIFT_FLAG 是否为 0 — 用于主循环「停产班次跳过排程」。
     *
     * <p>与 {@link #isClosingShift} 等价语义，但直接读 flag，不经过 OPEN_START/BEFORE_CLOSE 分支。
     */
    public boolean isShiftStopped(LocalDate date, int dayShiftOrder, String factoryCode) {
        String flag = getShiftFlag(date, dayShiftOrder, factoryCode);
        log.debug("班次停产判断：工厂={}, 日期={}, 当天第{}班, 标志={}, 结果={}",
                factoryCode, date, dayShiftOrder, flag, SHIFT_FLAG_STOP.equals(flag));
        return SHIFT_FLAG_STOP.equals(flag);
    }

    /**
     * 判断某天是否整天停产（带工厂编号）
     */
    public boolean isFullDayStopped(LocalDate date, String factoryCode) {
        MdmWorkCalendar calendar = getCalendar(date, factoryCode);
        if (calendar == null) {
            return false;
        }
        if ("0".equals(calendar.getDayFlag())) {
            return true;
        }
        boolean shift1Stopped = SHIFT_FLAG_STOP.equals(calendar.getOneShiftFlag());
        boolean shift2Stopped = SHIFT_FLAG_STOP.equals(calendar.getTwoShiftFlag());
        boolean shift3Stopped = SHIFT_FLAG_STOP.equals(calendar.getThreeShiftFlag());
        return shift1Stopped && shift2Stopped && shift3Stopped;
    }

    /**
     * 当日是否存在任一班次停产 — TaskGroupService 跨天封顶等场景使用。
     */
    public boolean hasAnyClosingShift(LocalDate date, String factoryCode) {
        MdmWorkCalendar calendar = getCalendar(date, factoryCode);
        if (calendar == null) {
            return false;
        }
        return SHIFT_FLAG_STOP.equals(calendar.getOneShiftFlag())
                || SHIFT_FLAG_STOP.equals(calendar.getTwoShiftFlag())
                || SHIFT_FLAG_STOP.equals(calendar.getThreeShiftFlag());
    }

    // ==================== 按天 DAY_FLAG 兼容 API（开产判定请用 determineShiftType） ====================

    /**
     * 从当前排产日期往前找最近一个有 dayFlag 标识的日期（带工厂编号）
     */
    public DayFlagInfo findNearestDayFlag(LocalDate date, String factoryCode) {
        // 最多往前查 30 天
        for (int i = 0; i < 30; i++) {
            LocalDate queryDate = date.minusDays(i);
            MdmWorkCalendar calendar = getCalendar(queryDate, factoryCode);
            if (calendar != null && calendar.getDayFlag() != null) {
                return new DayFlagInfo(queryDate, calendar.getDayFlag());
            }
        }
        return null;
    }

    /**
     * 判断是否为停产日（已停产，带工厂编号）
     */
    public boolean isStopDay(LocalDate date, String factoryCode) {
        DayFlagInfo flagInfo = findNearestDayFlag(date, factoryCode);
        if (flagInfo == null || flagInfo.dayFlag == null) {
            return false;
        }
        return "0".equals(flagInfo.dayFlag) && date.isAfter(flagInfo.nearestDate);
    }

    public boolean isStopFlagDay(LocalDate date, String factoryCode) {
        DayFlagInfo flagInfo = findNearestDayFlag(date, factoryCode);
        if (flagInfo == null || flagInfo.dayFlag == null) {
            return false;
        }
        return "0".equals(flagInfo.dayFlag) && !date.isAfter(flagInfo.nearestDate);
    }

    // ==================== 时间与班次序号映射 ====================

    /**
     * 将 HH:mm 时刻映射到 {@code CxShiftConfig.dayShiftOrder}。
     *
     * <p><b>匹配规则</b>：
     * <ul>
     *   <li>遍历已排序班次配置，找 {@code startTime <= time < endTime}（非跨天班次）</li>
     *   <li>跨天班次（start &gt; end）：{@code time >= start || time < end}</li>
     *   <li>未命中时：若 time 晚于某班 start，取最后一班；若早于首班 start（如停锅 05:30、首班 06:00），归首班</li>
     * </ul>
     *
     * <p>用于硫化停锅/开模时间与班次窗口对齐（TaskGroupService 停产反推）。
     *
     * @param timeStr      HH:mm
     * @param shiftConfigs 当日班次配置（应按 dayShiftOrder 排序）
     * @return dayShiftOrder，无法匹配时 null
     */
    public Integer getShiftOrderByTime(String timeStr, List<com.zlt.aps.cx.entity.config.CxShiftConfig> shiftConfigs) {
        if (timeStr == null || shiftConfigs == null || shiftConfigs.isEmpty()) {
            return null;
        }
        for (com.zlt.aps.cx.entity.config.CxShiftConfig shiftConfig : shiftConfigs) {
            String startTime = shiftConfig.getStartTime();
            String endTime = shiftConfig.getEndTime();
            if (startTime != null && endTime != null) {
                String start = startTime.length() >= 5 ? startTime.substring(0, 5) : startTime;
                String end = endTime.length() >= 5 ? endTime.substring(0, 5) : endTime;
                boolean inShift;
                if (start.compareTo(end) <= 0) {
                    inShift = timeStr.compareTo(start) >= 0 && timeStr.compareTo(end) < 0;
                } else {
                    inShift = timeStr.compareTo(start) >= 0 || timeStr.compareTo(end) < 0;
                }
                if (inShift) {
                    return shiftConfig.getDayShiftOrder();
                }
            }
        }
        for (int i = shiftConfigs.size() - 1; i >= 0; i--) {
            com.zlt.aps.cx.entity.config.CxShiftConfig shiftConfig = shiftConfigs.get(i);
            String startTime = shiftConfig.getStartTime();
            if (startTime != null) {
                String start = startTime.length() >= 5 ? startTime.substring(0, 5) : startTime;
                if (timeStr.compareTo(start) >= 0) {
                    return shiftConfig.getDayShiftOrder();
                }
            }
        }
        com.zlt.aps.cx.entity.config.CxShiftConfig firstShift = shiftConfigs.get(0);
        if (firstShift != null && firstShift.getDayShiftOrder() != null) {
            return firstShift.getDayShiftOrder();
        }
        return null;
    }
}
