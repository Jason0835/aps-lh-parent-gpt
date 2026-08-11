package com.zlt.aps.common.engine.schedule;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        if (ScheduleProcessLogLevel.OFF == this.level || StrUtil.isBlank(format)) {
            return;
        }
        Object[] plainArgs = this.toPlainArgs(args);
        this.getEntryList(shiftOrder).add(MessageFormat.format(format, plainArgs));
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
        if (ScheduleProcessLogLevel.FULL == this.level && event != null) {
            this.getEntryList(shiftOrder).add(event);
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
                    .mapToInt(this::countSummaryEntries).sum();
        }
        return this.entries.size() + this.shiftEntryMap.values().stream().mapToInt(List::size).sum();
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
        if (ScheduleProcessLogLevel.SUMMARY == this.level) {
            this.appendSummaryEntries(resultBuffer, this.entries);
            this.shiftEntryMap.forEach((shiftOrder, shiftEntries) -> {
                if (this.countSummaryEntries(shiftEntries) > 0) {
                    resultBuffer.append(this.buildShiftTitle(shiftOrder)).append(System.lineSeparator());
                    this.appendSummaryEntries(resultBuffer, shiftEntries);
                }
            });
            return this.normalizeText(resultBuffer.toString());
        }
        int sequence = 0;
        for (Object entry : this.entries) {
            sequence = this.appendFullEntry(resultBuffer, sequence, entry);
        }
        for (Map.Entry<Integer, List<Object>> shiftEntry : this.shiftEntryMap.entrySet()) {
            if (shiftEntry.getValue().isEmpty()) {
                continue;
            }
            resultBuffer.append(this.buildShiftTitle(shiftEntry.getKey())).append(System.lineSeparator());
            for (Object entry : shiftEntry.getValue()) {
                sequence = this.appendFullEntry(resultBuffer, sequence, entry);
            }
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
