package com.zlt.aps.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 含有<></>转义切面
 *
 * @author Chen
 * @since 2026/1/5
 */
@Slf4j
@Aspect
@Component
public class HtmlDecodeAspect {

    @Before("execution(* com.zlt.aps.controller..*.save*(..)) || execution(* com.zlt.aps.controller..*.edit*(..))")
    public void beforeSave(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            try {
                Object escapeParamField = escapeParamField(arg);
                args[i] = escapeParamField;
            } catch (IllegalAccessException e) {
                log.error("参数转义失败", e);
            }
        }
    }

    @Around("execution(* com.zlt.aps.controller..*.list*(..))")
    public Object afterList(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object originalResult = joinPoint.proceed(args);
        Object handleResult = originalResult;
        try {
            handleResult = unescapeResultData(originalResult);
            log.debug("结果数据反向转义：{}", handleResult);
        } catch (IllegalAccessException e) {
            log.error("结果数据反向转义失败", e);
        }
        return handleResult;
    }

    /**
     * 递归处理参数对象，对所有层级的String类型字段执行escapeHtml()
     *
     * @param param 方法入参（支持：单个对象、集合、数组、基本类型+String）
     */
    private Object escapeParamField(Object param) throws IllegalAccessException {
        if (param == null) {
            return null;
        }
        Class<?> clazz = param.getClass();

        //  情况1：参数是【字符串】→ 直接转义
        if (clazz == String.class) {
            return escapeHtml((String) param);
        }

        //  情况2：参数是【集合】（List/Set）→ 遍历集合元素递归转义
        if (param instanceof Collection) {
            Collection<?> collection = (Collection<?>) param;
            collection.forEach(item -> {
                try {
                    escapeParamField(item);
                } catch (IllegalAccessException e) {
                    log.error("集合参数转义失败", e);
                }
            });
            return param;
        }

        //  情况3：参数是【数组】→ 遍历数组元素递归转义
        if (clazz.isArray()) {
            Object[] array = (Object[]) param;
            for (Object item : array) {
                escapeParamField(item);
            }
            return param;
        }

        //  情况4：参数是【基本类型/包装类型】（Integer/Long/Boolean等）→ 不处理，直接返回
        if (clazz.isPrimitive() || Number.class.isAssignableFrom(clazz)
                || Boolean.class == clazz || Character.class == clazz || Date.class == clazz) {
            return param;
        }

        //  情况5：参数是【自定义实体对象】→ 反射遍历所有字段（含私有），递归转义
        // 获取对象的所有字段（包括私有字段、父类字段）
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // 暴力反射：允许访问私有字段（核心，否则无法修改private字段）
            field.setAccessible(true);
            String fieldName = field.getName();
            Object fieldValue = field.get(param);
            Class<?> fieldType = field.getType();

            // 字段是String类型 → 执行转义并重新赋值
            if (fieldType == String.class) {
                String escapeValue = escapeHtml((String) fieldValue);
                field.set(param, escapeValue);
                log.debug("字段【{}】转义完成，原值：{} → 新值：{}", fieldName, fieldValue, escapeValue);
            }
            // 字段是【对象/集合】→ 递归处理嵌套字段
            else if (!fieldType.isPrimitive() && !Number.class.isAssignableFrom(fieldType)
                    && !Boolean.class.isAssignableFrom(fieldType)) {
                escapeParamField(fieldValue);
            }
        }
        return param;
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 递归处理参数对象，对所有层级的String类型字段执行unescapeHtml()
     * 支持：单个对象、集合、数组、基本类型+String、嵌套对象（与转义工具完全兼容）
     */
    private Object unescapeResultData(Object result) throws IllegalAccessException {
        if (result == null) {
            return null; // 空结果直接返回
        }
        Class<?> clazz = result.getClass();

        // 场景1：返回值是【String字符串】→ 直接反向转义（核心）
        if (clazz == String.class) {
            String escapeStr = unescapeHtml((String) result);
            log.debug("字符串返回值反向转义：{} → {}", result, escapeStr);
            return escapeStr;
        }

        // 场景2：返回值是【集合】（List/Set）→ 遍历元素递归处理
        if (result instanceof Collection) {
            Collection<?> collection = (Collection<?>) result;
            collection.forEach(item -> {
                try {
                    unescapeResultData(item);
                } catch (IllegalAccessException e) {
                    log.error("集合返回值元素反向转义失败", e);
                }
            });
            return result;
        }

        // 场景3：返回值是【数组】→ 遍历数组元素递归处理
        if (clazz.isArray()) {
            Object[] array = (Object[]) result;
            for (int i = 0; i < array.length; i++) {
                array[i] = unescapeResultData(array[i]);
            }
            return result;
        }

        if (result instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) result;
            Set<? extends Map.Entry<String, Object>> entrySet = map.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                Object value = entry.getValue();
                try {
                    value = unescapeResultData(value);
                } catch (IllegalAccessException e) {
                    log.error("map返回值元素反向转义失败", e);
                }
                entry.setValue(value);
            }
            return result;
        }

        // 场景4：返回值是【基本类型/包装类型】→ 不处理，直接返回
        if (clazz.isPrimitive() || Number.class.isAssignableFrom(clazz)
                || Boolean.class == clazz || Character.class == clazz || Date.class == clazz) {
            return result;
        }

        // 场景5：返回值是【自定义实体/业务对象】→ 反射遍历所有字段（含私有）递归处理
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object fieldValue = field.get(result);
            Class<?> fieldType = field.getType();

            // 字段是String类型 → 执行反向转义并重新赋值给对象
            if (fieldType == String.class) {
                String oldValue = (String) fieldValue;
                String newValue = unescapeHtml(oldValue);
                field.set(result, newValue);
                log.debug("对象字段【{}】反向转义完成：{} → {}", fieldName, oldValue, newValue);
            }
            // 字段是【对象/集合/数组】→ 递归处理嵌套字段（多层对象全覆盖）
            else if (!fieldType.isPrimitive()
                    && !Number.class.isAssignableFrom(fieldType)
                    && !Boolean.class.isAssignableFrom(fieldType)) {
                unescapeResultData(fieldValue);
            }
        }

        // 场景6：兜底返回（所有类型处理完毕，返回处理后的结果）
        return result;
    }

    private String unescapeHtml(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        int len = input.length();
        int i = 0;
        while (i < len) {
            char c = input.charAt(i);
            if (c == '&') {
                if (i + 4 < len && input.startsWith("&amp;", i)) {
                    sb.append('&');
                    i += 5;
                    continue;
                } else if (i + 3 < len && input.startsWith("&lt;", i)) {
                    sb.append('<');
                    i += 4;
                    continue;
                } else if (i + 3 < len && input.startsWith("&gt;", i)) {
                    sb.append('>');
                    i += 4;
                    continue;
                } else if (i + 5 < len && input.startsWith("&quot;", i)) {
                    sb.append('"');
                    i += 6;
                    continue;
                } else if (i + 4 < len && input.startsWith("&#39;", i)) {
                    sb.append('\'');
                    i += 5;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}
