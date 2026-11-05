package com.zlt.aps.common.engine.schedule.engine;

import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动排程隔离试算工作区的基础复制工具。
 *
 * <p>只复制可变容器及其元素副本；不可变值对象和只读配置值由调用方按原引用共享。</p>
 */
public final class ScheduleWorkspaceCopyUtils {

    private ScheduleWorkspaceCopyUtils() {
    }

    /**
     * 复制一个具有无参构造器的简单运行态对象。
     *
     * @param source     来源对象
     * @param targetType 目标类型
     * @param <T>        对象类型
     * @return 属性值已复制的新对象；来源为空时返回空
     */
    public static <T> T copyBean(T source, Class<? extends T> targetType) {
        if (source == null) {
            return null;
        }
        try {
            Object target = targetType.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return targetType.cast(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("排程工作区对象无法复制：" + targetType.getName(), exception);
        }
    }

    /**
     * 专门复制规则轨迹，包括没有setter的两个final命中集合及其可变证据。
     *
     * @param source     来源轨迹
     * @param targetType 领域轨迹类型，必须具有无参构造器
     * @param <T>        领域轨迹类型
     * @return 命中条目与证据容器均隔离的轨迹；来源为空返回null
     * @throws IllegalStateException 轨迹对象无法实例化时抛出
     */
    public static <T extends ScheduleRuleTraceModel> T copyRuleTrace(T source, Class<? extends T> targetType) {
        T target = copyBean(source, targetType);
        if (target != null) {
            target.getRuleHits().addAll(copyList(source.getRuleHits(), item ->
                    new ScheduleRuleTraceItemModel(item.getRuleCode(), item.getResult(),
                            copyEvidenceValue(item.getEvidence()))));
            target.getExplainOnlyRuleHits().addAll(copyList(source.getExplainOnlyRuleHits(), item ->
                    new ScheduleRuleTraceItemModel(item.getRuleCode(), item.getResult(),
                            copyEvidenceValue(item.getEvidence()))));
        }
        return target;
    }

    /**
     * 按业务提供的元素复制器复制列表。
     *
     * @param source 来源列表
     * @param copier 元素复制器
     * @param <T>    元素类型
     * @return 新列表
     */
    public static <T> List<T> copyList(Collection<T> source, Function<T, T> copier) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().map(value -> value == null ? null : copier.apply(value))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 复制一层键值容器，适用于键和值均为不可变类型的账本快照。
     *
     * @param source 来源映射
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 新映射
     */
    public static <K, V> Map<K, V> copyMap(Map<K, V> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    /**
     * 使用值复制器复制映射。
     *
     * @param source 来源映射
     * @param copier 值复制器
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 值已隔离的新映射
     */
    public static <K, V> Map<K, V> copyMapValues(Map<K, V> source, Function<V, V> copier) {
        Map<K, V> target = source instanceof java.util.SortedMap
                ? new java.util.TreeMap<>(((java.util.SortedMap<K, V>) source).comparator())
                : new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> target.put(key, value == null ? null : copier.apply(value)));
        }
        return target;
    }

    /**
     * 复制过程证据中的常见可变容器，不递归复制业务对象和不可变数值。
     *
     * @param value 证据值
     * @return 独立的Map、List、Set、数组或Date；其他值保留原值
     */
    public static Object copyEvidenceValue(Object value) {
        if (value instanceof Date) {
            return new Date(((Date) value).getTime());
        }
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((key, nestedValue) ->
                    copy.put(key, copyEvidenceValue(nestedValue)));
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            ((List<?>) value).forEach(nestedValue -> copy.add(copyEvidenceValue(nestedValue)));
            return copy;
        }
        if (value instanceof Set) {
            Set<Object> copy = new LinkedHashSet<>();
            ((Set<?>) value).forEach(nestedValue -> copy.add(copyEvidenceValue(nestedValue)));
            return copy;
        }
        if (value instanceof Object[]) {
            Object[] source = (Object[]) value;
            Object[] copy = new Object[source.length];
            for (int index = 0; index < source.length; index++) {
                copy[index] = copyEvidenceValue(source[index]);
            }
            return copy;
        }
        return value;
    }

    /**
     * 复制证据Map及常见嵌套容器。
     *
     * @param source 来源映射
     * @return 可独立追加的证据Map
     */
    public static Map<String, Object> copyEvidenceMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> copy.put(key, copyEvidenceValue(value)));
        }
        return copy;
    }

    /**
     * 复制一层集合，适用于元素为不可变类型的业务键快照。
     *
     * @param source 来源集合
     * @param <T>    元素类型
     * @return 新集合
     */
    public static <T> Set<T> copySet(Collection<T> source) {
        return source == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source);
    }
}
