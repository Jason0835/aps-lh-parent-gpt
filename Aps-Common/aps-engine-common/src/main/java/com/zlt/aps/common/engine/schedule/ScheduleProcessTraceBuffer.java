package com.zlt.aps.common.engine.schedule;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动排程中文过程日志格式化缓冲器。
 *
 * <p>缓冲器分别保存批次级和班次级摘要、完整事件，最终根据日志级别统一渲染。班次级日志按班次顺序
 * 输出固定分段标题；FULL 模式下摘要会被转换为具备六个中文必填段落的连续编号事件，班次标题不参与编号。</p>
 */
public class ScheduleProcessTraceBuffer {

    /** 过程日志中独立小数文本的匹配规则。 */
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("(?<![\\d.])(-?\\d+\\.\\d+)(?![\\d.])");

    /** 按发生顺序保存的批次级摘要文本或完整事件。 */
    private final List<Object> entries = new ArrayList<>();

    /** 按班次顺序保存的班次级摘要文本或完整事件。 */
    private final Map<Integer, List<Object>> shiftEntryMap = new TreeMap<>();

    /** 按班次和业务分区保存的摘要文本或完整事件。 */
    private final Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> shiftSectionEntryMap = new TreeMap<>();

    /** 按班次顺序保存必须在库存、计划量和机台评分之后输出的产能扣减摘要或完整事件。 */
    private final Map<Integer, List<Object>> deferredShiftEntryMap = new TreeMap<>();

    /** 按班次和业务分区保存的延后摘要文本或完整事件。 */
    private final Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> deferredShiftSectionEntryMap = new TreeMap<>();

    /** 保存必须在所有班次日志之后输出的尾部摘要。 */
    private final List<Object> tailEntries = new ArrayList<>();

    /** 当前有效日志级别。 */
    private ScheduleProcessLogLevel level = ScheduleProcessLogLevel.DEFAULT_LEVEL;

    /** 原始配置值。 */
    private String configuredValue;

    /** 是否发生非法值回退。 */
    private boolean fallback;

    /**
     * 配置过程日志级别。
     *
     * @param value 参数配置值
     */
    public void configure(String value) {
        this.configuredValue = value;
        this.level = ScheduleProcessLogLevel.parse(value);
        this.fallback = StrUtil.isNotBlank(value) && !ScheduleProcessLogLevel.isSupported(value);
        if (ScheduleProcessLogLevel.OFF == this.level) {
            this.entries.clear();
            this.shiftEntryMap.clear();
            this.shiftSectionEntryMap.clear();
            this.deferredShiftEntryMap.clear();
            this.deferredShiftSectionEntryMap.clear();
            this.tailEntries.clear();
            return;
        }
        if (this.fallback) {
            this.entries.add(0, "过程日志级别参数值“" + value + "”不受支持，已回退为 SUMMARY；支持值为 OFF、SUMMARY、FULL。");
        }
    }

    /**
     * 追加现有摘要日志。
     *
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args   日志参数
     */
    public void appendSummary(String format, Object... args) {
        if (ScheduleProcessLogLevel.OFF == this.level || StrUtil.isBlank(format)) {
            return;
        }
        Object[] plainArgs = this.toPlainArgs(args);
        this.entries.add(MessageFormat.format(format, plainArgs));
    }

    /**
     * 追加指定班次的摘要日志。
     *
     * @param shiftOrder 班次顺序
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendShiftSummary(Integer shiftOrder, String format, Object... args) {
        this.appendShiftSummary(shiftOrder, (ScheduleProcessLogSection) null, format, args);
    }

    /**
     * 追加指定班次和分区的摘要日志。
     *
     * @param shiftOrder 班次顺序
     * @param section    业务分区；为空时沿用未分区兼容输出
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendShiftSummary(Integer shiftOrder, ScheduleProcessLogSection section,
                                   String format, Object... args) {
        if (ScheduleProcessLogLevel.OFF == this.level || StrUtil.isBlank(format)) {
            return;
        }
        Object[] plainArgs = this.toPlainArgs(args);
        this.getEntryList(shiftOrder, section).add(MessageFormat.format(format, plainArgs));
    }

    /**
     * 追加指定班次的延后摘要日志，渲染时位于该班次普通摘要之后。
     *
     * @param shiftOrder 班次顺序
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendDeferredShiftSummary(Integer shiftOrder, String format, Object... args) {
        this.appendDeferredShiftSummary(shiftOrder, (ScheduleProcessLogSection) null, format, args);
    }

    /**
     * 追加指定班次和分区的延后摘要日志。
     *
     * @param shiftOrder 班次顺序
     * @param section    业务分区；为空时沿用未分区兼容输出
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendDeferredShiftSummary(Integer shiftOrder, ScheduleProcessLogSection section,
                                            String format, Object... args) {
        if (ScheduleProcessLogLevel.OFF == this.level || StrUtil.isBlank(format)) {
            return;
        }
        Object[] plainArgs = this.toPlainArgs(args);
        this.getDeferredEntryList(shiftOrder, section).add(MessageFormat.format(format, plainArgs));
    }

    /**
     * 追加必须在批次级和班次级日志之后输出的尾部摘要。
     *
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args   日志参数
     */
    public void appendTailSummary(String format, Object... args) {
        if (ScheduleProcessLogLevel.OFF == this.level || StrUtil.isBlank(format)) {
            return;
        }
        Object[] plainArgs = this.toPlainArgs(args);
        this.tailEntries.add(MessageFormat.format(format, plainArgs));
    }

    /**
     * 追加完整中文过程事件。
     *
     * @param event 完整过程事件
     */
    public void appendFull(ScheduleProcessTraceEvent event) {
        if (ScheduleProcessLogLevel.FULL == this.level && event != null) {
            this.entries.add(event);
        }
    }

    /**
     * 追加指定班次的完整中文过程事件。
     *
     * @param shiftOrder 班次顺序
     * @param event      完整过程事件
     */
    public void appendShiftFull(Integer shiftOrder, ScheduleProcessTraceEvent event) {
        this.appendShiftFull(shiftOrder, null, event);
    }

    /**
     * 追加指定班次和分区的完整中文过程事件。
     *
     * @param shiftOrder 班次顺序
     * @param section    业务分区；为空时沿用未分区兼容输出
     * @param event      完整过程事件
     */
    public void appendShiftFull(Integer shiftOrder, ScheduleProcessLogSection section,
                                ScheduleProcessTraceEvent event) {
        if (ScheduleProcessLogLevel.FULL == this.level && event != null) {
            this.getEntryList(shiftOrder, section).add(event);
        }
    }

    /**
     * 追加指定班次的延后完整事件，渲染时位于该班次普通事件之后。
     *
     * @param shiftOrder 班次顺序
     * @param event      完整过程事件
     */
    public void appendDeferredShiftFull(Integer shiftOrder, ScheduleProcessTraceEvent event) {
        this.appendDeferredShiftFull(shiftOrder, null, event);
    }

    /**
     * 追加指定班次和分区的延后完整过程事件。
     *
     * @param shiftOrder 班次顺序
     * @param section    业务分区；为空时沿用未分区兼容输出
     * @param event      完整过程事件
     */
    public void appendDeferredShiftFull(Integer shiftOrder, ScheduleProcessLogSection section,
                                        ScheduleProcessTraceEvent event) {
        if (ScheduleProcessLogLevel.FULL == this.level && event != null) {
            this.getDeferredEntryList(shiftOrder, section).add(event);
        }
    }

    /**
     * 获取当前有效日志级别。
     *
     * @return 日志级别
     */
    public ScheduleProcessLogLevel getLevel() {
        return level;
    }

    /**
     * 获取原始配置值。
     *
     * @return 原始配置值
     */
    public String getConfiguredValue() {
        return configuredValue;
    }

    /**
     * 判断是否发生非法值回退。
     *
     * @return 回退返回 true，否则返回 false
     */
    public boolean isFallback() {
        return fallback;
    }

    /**
     * 获取最终会输出的事件数量。
     *
     * @return OFF 为 0，其他级别为输出条目数
     */
    public int getEventCount() {
        if (ScheduleProcessLogLevel.OFF == this.level) {
            return 0;
        }
        if (ScheduleProcessLogLevel.SUMMARY == this.level) {
            return this.countSummaryEntries(this.entries) + this.shiftEntryMap.values().stream()
                    .mapToInt(this::countSummaryEntries).sum()
                    + this.countSummarySectionEntries(this.shiftSectionEntryMap)
                    + this.deferredShiftEntryMap.values().stream().mapToInt(this::countSummaryEntries).sum()
                    + this.countSummarySectionEntries(this.deferredShiftSectionEntryMap)
                    + this.countSummaryEntries(this.tailEntries);
        }
        return this.entries.size() + this.shiftEntryMap.values().stream().mapToInt(List::size).sum()
                + this.countFullSectionEntries(this.shiftSectionEntryMap)
                + this.deferredShiftEntryMap.values().stream().mapToInt(List::size).sum()
                + this.countFullSectionEntries(this.deferredShiftSectionEntryMap)
                + this.tailEntries.size();
    }

    /**
     * 渲染最终过程日志正文。
     *
     * @return 中文过程日志；OFF 返回空字符串
     */
    public String render() {
        if (ScheduleProcessLogLevel.OFF == this.level) {
            return "";
        }
        StringBuilder resultBuffer = new StringBuilder(4096);
        Set<Integer> shiftOrders = this.getAllShiftOrders();
        if (ScheduleProcessLogLevel.SUMMARY == this.level) {
            this.appendSummaryEntries(resultBuffer, this.entries);
            shiftOrders.forEach(shiftOrder -> this.appendSummaryShiftEntries(resultBuffer, shiftOrder));
            this.appendSummaryEntries(resultBuffer, this.tailEntries);
            return this.normalizeText(resultBuffer.toString());
        }
        int sequence = 0;
        for (Object entry : this.entries) {
            sequence = this.appendFullEntry(resultBuffer, sequence, entry);
        }
        for (Integer shiftOrder : shiftOrders) {
            if (!this.hasFullShiftEntries(shiftOrder)) {
                continue;
            }
            resultBuffer.append(this.buildShiftTitle(shiftOrder)).append(System.lineSeparator());
            sequence = this.appendFullShiftEntries(resultBuffer, sequence, shiftOrder);
        }
        for (Object entry : this.tailEntries) {
            sequence = this.appendFullEntry(resultBuffer, sequence, entry);
        }
        return this.normalizeText(resultBuffer.toString());
    }

    /**
     * 获取指定班次的日志容器；无有效班次时降级为批次级日志，确保异常日志不丢失。
     *
     * @param shiftOrder 班次顺序
     * @return 对应的日志容器
     */
    private List<Object> getEntryList(Integer shiftOrder) {
        if (shiftOrder == null || shiftOrder <= 0) {
            return this.entries;
        }
        return this.shiftEntryMap.computeIfAbsent(shiftOrder, key -> new ArrayList<>());
    }

    /**
     * 获取指定班次和分区的日志容器；无有效班次时降级为批次级日志。
     *
     * @param shiftOrder 班次顺序
     * @param section    业务分区
     * @return 对应的日志容器
     */
    private List<Object> getEntryList(Integer shiftOrder, ScheduleProcessLogSection section) {
        if (section == null) {
            return this.getEntryList(shiftOrder);
        }
        if (shiftOrder == null || shiftOrder <= 0) {
            return this.entries;
        }
        return this.shiftSectionEntryMap
                .computeIfAbsent(shiftOrder, key -> new EnumMap<>(ScheduleProcessLogSection.class))
                .computeIfAbsent(section, key -> new ArrayList<>());
    }

    /**
     * 获取指定班次的延后日志容器；无有效班次时降级为批次级日志。
     *
     * @param shiftOrder 班次顺序
     * @return 对应的延后日志容器
     */
    private List<Object> getDeferredEntryList(Integer shiftOrder) {
        if (shiftOrder == null || shiftOrder <= 0) {
            return this.entries;
        }
        return this.deferredShiftEntryMap.computeIfAbsent(shiftOrder, key -> new ArrayList<>());
    }

    /**
     * 获取指定班次和分区的延后日志容器；无有效班次时降级为批次级日志。
     *
     * @param shiftOrder 班次顺序
     * @param section    业务分区
     * @return 对应的日志容器
     */
    private List<Object> getDeferredEntryList(Integer shiftOrder, ScheduleProcessLogSection section) {
        if (section == null) {
            return this.getDeferredEntryList(shiftOrder);
        }
        if (shiftOrder == null || shiftOrder <= 0) {
            return this.entries;
        }
        return this.deferredShiftSectionEntryMap
                .computeIfAbsent(shiftOrder, key -> new EnumMap<>(ScheduleProcessLogSection.class))
                .computeIfAbsent(section, key -> new ArrayList<>());
    }

    /**
     * 获取所有存在日志的班次。
     *
     * @return 按班次顺序排列的班次集合
     */
    private Set<Integer> getAllShiftOrders() {
        Set<Integer> shiftOrders = new TreeSet<>();
        shiftOrders.addAll(this.shiftEntryMap.keySet());
        shiftOrders.addAll(this.shiftSectionEntryMap.keySet());
        shiftOrders.addAll(this.deferredShiftEntryMap.keySet());
        shiftOrders.addAll(this.deferredShiftSectionEntryMap.keySet());
        return shiftOrders;
    }

    /**
     * 输出一个班次的 SUMMARY 日志，分区标题只在分区有内容时输出。
     *
     * @param resultBuffer 输出缓冲器
     * @param shiftOrder   班次顺序
     */
    private void appendSummaryShiftEntries(StringBuilder resultBuffer, Integer shiftOrder) {
        if (!this.hasSummaryShiftEntries(shiftOrder)) {
            return;
        }
        resultBuffer.append(this.buildShiftTitle(shiftOrder)).append(System.lineSeparator());
        this.appendSummaryEntries(resultBuffer, this.shiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>()));
        this.appendSummarySectionEntries(resultBuffer, shiftOrder, this.shiftSectionEntryMap,
                this.deferredShiftSectionEntryMap);
        this.appendSummaryEntries(resultBuffer, this.deferredShiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>()));
    }

    /**
     * 输出一个班次的 FULL 日志，分区标题不占用事件序号。
     *
     * @param resultBuffer 输出缓冲器
     * @param sequence     当前事件序号
     * @param shiftOrder   班次顺序
     * @return 更新后的事件序号
     */
    private int appendFullShiftEntries(StringBuilder resultBuffer, int sequence, Integer shiftOrder) {
        for (Object entry : this.shiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>())) {
            sequence = this.appendFullEntry(resultBuffer, sequence, entry);
        }
        sequence = this.appendFullSectionEntries(resultBuffer, sequence, shiftOrder,
                this.shiftSectionEntryMap, this.deferredShiftSectionEntryMap);
        for (Object entry : this.deferredShiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>())) {
            sequence = this.appendFullEntry(resultBuffer, sequence, entry);
        }
        return sequence;
    }

    /**
     * 判断指定班次是否有 SUMMARY 日志。
     *
     * @param shiftOrder 班次顺序
     * @return 有日志返回 true，否则返回 false
     */
    private boolean hasSummaryShiftEntries(Integer shiftOrder) {
        return this.countSummaryEntries(this.shiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>())) > 0
                || this.hasSummarySectionEntries(shiftOrder, this.shiftSectionEntryMap)
                || this.countSummaryEntries(this.deferredShiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>())) > 0
                || this.hasSummarySectionEntries(shiftOrder, this.deferredShiftSectionEntryMap);
    }

    /**
     * 判断指定班次是否有 FULL 日志。
     *
     * @param shiftOrder 班次顺序
     * @return 有日志返回 true，否则返回 false
     */
    private boolean hasFullShiftEntries(Integer shiftOrder) {
        return !this.shiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>()).isEmpty()
                || this.hasFullSectionEntries(shiftOrder, this.shiftSectionEntryMap)
                || !this.deferredShiftEntryMap.getOrDefault(shiftOrder, new ArrayList<>()).isEmpty()
                || this.hasFullSectionEntries(shiftOrder, this.deferredShiftSectionEntryMap);
    }

    /**
     * 输出指定班次的 SUMMARY 分区日志。
     *
     * @param resultBuffer 输出缓冲器
     * @param shiftOrder   班次顺序
     * @param sectionMap   普通分区日志
     * @param deferredMap  延后分区日志
     */
    private void appendSummarySectionEntries(StringBuilder resultBuffer, Integer shiftOrder,
                                             Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> sectionMap,
                                             Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> deferredMap) {
        for (ScheduleProcessLogSection section : ScheduleProcessLogSection.values()) {
            List<Object> entries = sectionMap.getOrDefault(shiftOrder, new EnumMap<>(ScheduleProcessLogSection.class))
                    .getOrDefault(section, new ArrayList<>());
            List<Object> deferredEntries = deferredMap.getOrDefault(shiftOrder,
                            new EnumMap<>(ScheduleProcessLogSection.class))
                    .getOrDefault(section, new ArrayList<>());
            if (this.countSummaryEntries(entries) == 0 && this.countSummaryEntries(deferredEntries) == 0) {
                continue;
            }
            resultBuffer.append(this.buildSectionTitle(section)).append(System.lineSeparator());
            this.appendSummaryEntries(resultBuffer, entries);
            this.appendSummaryEntries(resultBuffer, deferredEntries);
        }
    }

    /**
     * 输出指定班次的 FULL 分区日志。
     *
     * @param resultBuffer 输出缓冲器
     * @param sequence     当前事件序号
     * @param shiftOrder   班次顺序
     * @param sectionMap   普通分区日志
     * @param deferredMap  延后分区日志
     * @return 更新后的事件序号
     */
    private int appendFullSectionEntries(StringBuilder resultBuffer, int sequence, Integer shiftOrder,
                                         Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> sectionMap,
                                         Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> deferredMap) {
        for (ScheduleProcessLogSection section : ScheduleProcessLogSection.values()) {
            List<Object> entries = sectionMap.getOrDefault(shiftOrder, new EnumMap<>(ScheduleProcessLogSection.class))
                    .getOrDefault(section, new ArrayList<>());
            List<Object> deferredEntries = deferredMap.getOrDefault(shiftOrder,
                            new EnumMap<>(ScheduleProcessLogSection.class))
                    .getOrDefault(section, new ArrayList<>());
            if (entries.isEmpty() && deferredEntries.isEmpty()) {
                continue;
            }
            resultBuffer.append(this.buildSectionTitle(section)).append(System.lineSeparator());
            for (Object entry : entries) {
                sequence = this.appendFullEntry(resultBuffer, sequence, entry);
            }
            for (Object entry : deferredEntries) {
                sequence = this.appendFullEntry(resultBuffer, sequence, entry);
            }
        }
        return sequence;
    }

    /**
     * 统计分区 SUMMARY 日志数量。
     *
     * @param sectionMap 分区日志
     * @return SUMMARY 日志数量
     */
    private int countSummarySectionEntries(
            Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> sectionMap) {
        return sectionMap.values().stream()
                .flatMap(sectionEntries -> sectionEntries.values().stream())
                .mapToInt(this::countSummaryEntries)
                .sum();
    }

    /**
     * 统计分区 FULL 日志数量。
     *
     * @param sectionMap 分区日志
     * @return FULL 日志数量
     */
    private int countFullSectionEntries(
            Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> sectionMap) {
        return sectionMap.values().stream()
                .flatMap(sectionEntries -> sectionEntries.values().stream())
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 判断指定班次的分区 SUMMARY 日志是否存在。
     *
     * @param shiftOrder 班次顺序
     * @param sectionMap 分区日志
     * @return 存在返回 true，否则返回 false
     */
    private boolean hasSummarySectionEntries(Integer shiftOrder,
                                              Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> sectionMap) {
        return sectionMap.getOrDefault(shiftOrder, new EnumMap<>(ScheduleProcessLogSection.class)).values().stream()
                .anyMatch(entries -> this.countSummaryEntries(entries) > 0);
    }

    /**
     * 判断指定班次的分区 FULL 日志是否存在。
     *
     * @param shiftOrder 班次顺序
     * @param sectionMap 分区日志
     * @return 存在返回 true，否则返回 false
     */
    private boolean hasFullSectionEntries(Integer shiftOrder,
                                           Map<Integer, Map<ScheduleProcessLogSection, List<Object>>> sectionMap) {
        return sectionMap.getOrDefault(shiftOrder, new EnumMap<>(ScheduleProcessLogSection.class)).values().stream()
                .anyMatch(entries -> !entries.isEmpty());
    }

    /**
     * 追加摘要级日志条目。
     *
     * @param resultBuffer 输出缓冲器
     * @param sourceEntries 待输出条目
     */
    private void appendSummaryEntries(StringBuilder resultBuffer, List<Object> sourceEntries) {
        sourceEntries.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .forEach(summary -> resultBuffer.append(summary).append(System.lineSeparator()));
    }

    /**
     * 统计摘要级会输出的日志数量。
     *
     * @param sourceEntries 待统计条目
     * @return 摘要日志数量
     */
    private int countSummaryEntries(List<Object> sourceEntries) {
        return (int) sourceEntries.stream().filter(String.class::isInstance).count();
    }

    /**
     * 输出一条 FULL 级日志并返回更新后的事件序号。
     *
     * @param resultBuffer 输出缓冲器
     * @param sequence     当前事件序号
     * @param entry        待输出条目
     * @return 更新后的事件序号
     */
    private int appendFullEntry(StringBuilder resultBuffer, int sequence, Object entry) {
        ScheduleProcessTraceEvent event = entry instanceof ScheduleProcessTraceEvent
                ? (ScheduleProcessTraceEvent) entry : this.buildSummaryEvent(String.valueOf(entry));
        int nextSequence = sequence + 1;
        this.appendEvent(resultBuffer, nextSequence, event);
        return nextSequence;
    }

    /**
     * 构建班次日志固定分段标题。
     *
     * @param shiftOrder 班次顺序
     * @return 分段标题
     */
    private String buildShiftTitle(Integer shiftOrder) {
        return MessageFormat.format("----------班次{0}----------", shiftOrder);
    }

    /**
     * 构建业务分区标题。
     *
     * @param section 业务分区
     * @return 分区标题
     */
    private String buildSectionTitle(ScheduleProcessLogSection section) {
        return "----------" + section.getDisplayName() + "----------";
    }

    /**
     * 将摘要行转换为符合 FULL 契约的完整事件。
     *
     * @param summary 摘要内容
     * @return 完整事件
     */
    private ScheduleProcessTraceEvent buildSummaryEvent(String summary) {
        return new ScheduleProcessTraceEvent(
                "过程摘要", "批次级", "步骤边界与关键公式",
                "自动排程运行上下文。", "当前步骤产生的摘要文本。",
                "摘要用于标识步骤边界或保留兼容的关键公式。", summary,
                "本条摘要已纳入连续事件序列。", "与相邻完整事件共同说明后续计算过程。"
        );
    }

    /**
     * 追加一个带连续序号的完整事件块。
     *
     * @param resultBuffer 结果缓冲器
     * @param sequence     连续序号
     * @param event        过程事件
     */
    private void appendEvent(StringBuilder resultBuffer, int sequence, ScheduleProcessTraceEvent event) {
        resultBuffer.append('[').append(String.format("%06d", sequence)).append(']')
                .append('[').append(this.requiredText(event.getStageName(), "未说明阶段")).append(']')
                .append("[任务=").append(this.requiredText(event.getTaskBusinessKey(), "批次级")).append(']')
                .append("[规则=").append(this.requiredText(event.getRuleName(), "未说明规则")).append(']')
                .append(System.lineSeparator());
        resultBuffer.append("数据来源：").append(this.requiredText(event.getDataSource(), "当前排程上下文，业务来源未单独说明。"))
                .append(System.lineSeparator());
        resultBuffer.append("原始输入：").append(this.requiredText(event.getOriginalInput(), "本事件无额外输入。"))
                .append(System.lineSeparator());
        resultBuffer.append("规则说明：").append(this.requiredText(event.getRuleDescription(), "按当前步骤既定规则处理。"))
                .append(System.lineSeparator());
        resultBuffer.append("代入计算：").append(this.requiredText(event.getSubstitutionProcess(), "本事件仅记录状态变化，无数值公式。"))
                .append(System.lineSeparator());
        resultBuffer.append("计算结果：").append(this.requiredText(event.getCalculationResult(), "本事件处理完成。"))
                .append(System.lineSeparator());
        resultBuffer.append("结果去向：").append(this.requiredText(event.getResultDestination(), "进入下一排程步骤。"))
                .append(System.lineSeparator());
    }

    /**
     * 将 BigDecimal 参数转换为无科学计数法文本。
     *
     * @param args 原始参数
     * @return 格式化参数
     */
    private Object[] toPlainArgs(Object[] args) {
        Object[] plainArgs = args == null ? new Object[0] : args.clone();
        for (int index = 0; index < plainArgs.length; index++) {
            if (plainArgs[index] instanceof BigDecimal) {
                plainArgs[index] = ((BigDecimal) plainArgs[index]).toPlainString();
            }
        }
        return plainArgs;
    }

    /**
     * 获取非空中文段落内容，避免输出无法解释的 null。
     *
     * @param value        原始值
     * @param defaultValue 缺省中文说明
     * @return 非空文本
     */
    private String requiredText(String value, String defaultValue) {
        return StrUtil.isBlank(value) || "null".equalsIgnoreCase(value.trim()) ? defaultValue : value;
    }

    /**
     * 规范正文展示，将无解释的 null 转为中文并移除小数末尾零。
     *
     * @param text 原始文本
     * @return 中文与数值展示规范后的文本
     */
    private String normalizeText(String text) {
        String readableText = text.replaceAll("(?i)(?<![A-Za-z0-9_])null(?![A-Za-z0-9_])", "未提供");
        Matcher matcher = DECIMAL_PATTERN.matcher(readableText);
        StringBuffer normalizedBuffer = new StringBuffer();
        while (matcher.find()) {
            String normalizedNumber = new BigDecimal(matcher.group(1)).stripTrailingZeros().toPlainString();
            matcher.appendReplacement(normalizedBuffer, Matcher.quoteReplacement(normalizedNumber));
        }
        matcher.appendTail(normalizedBuffer);
        return normalizedBuffer.toString();
    }
}
