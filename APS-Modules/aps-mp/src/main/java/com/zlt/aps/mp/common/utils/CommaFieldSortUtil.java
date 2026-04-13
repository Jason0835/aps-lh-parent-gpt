package com.zlt.aps.mp.common.utils;

import org.springframework.util.StringUtils;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用逗号分隔字段排序工具类
 * 传入方式：实体类::getXxx (方法引用)
 */
public class CommaFieldSortUtil {

    /**
     * 通用排序方法
     * @param originList 任意类型的集合 List<T>
     * @param fieldGetter 字段getter方法引用
     * @return 排序后的新集合（按逗号分隔值升序）
     */
    public static <T> List<T> sortByCommaField(List<T> originList, Function<T, String> fieldGetter) {
        // 空集合直接返回空列表
        if (originList == null || originList.isEmpty()) {
            return new ArrayList<>();
        }

        return originList.stream()
                .sorted(Comparator.comparing(
                        // 生成排序键：处理逗号分隔字符串
                        obj -> processCommaString(fieldGetter.apply(obj)),
                        // 空值排最后，非空按字符串升序
                        Comparator.nullsLast(String::compareTo)
                ))
                .collect(Collectors.toList());
    }

    /**
     * 处理逗号分隔字符串：拆分、去空格、去重、排序、拼接
     */
    private static String processCommaString(String value) {
        // 空值处理
        if (!StringUtils.hasText(value)) {
            return null;
        }

        // 拆分 → 去空格 → 过滤空值 → 去重 → 升序 → 拼接
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }


    /**
     * 排序集合 + 同时更新对象内部的逗号分隔字段为有序
     */
    public static <T> List<T> sortAndUpdateCommaField(List<T> originList, Function<T, String> fieldGetter, BiConsumer<T, String> fieldSetter) {
        if (originList == null || originList.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> result = originList.stream().peek(obj -> {
            // 处理并更新字段值
            String newValue = processCommaString(fieldGetter.apply(obj));
            fieldSetter.accept(obj, newValue);
        }).sorted(Comparator.comparing(
                obj -> processCommaString(fieldGetter.apply(obj)),
                Comparator.nullsLast(String::compareTo)
        )).collect(Collectors.toList());

        return result;
    }


}