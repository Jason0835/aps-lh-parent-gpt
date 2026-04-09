package com.zlt.aps.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
            log.debug("结果数据反向转义完成");
        } catch (IllegalAccessException e) {
            log.error("结果数据反向转义失败", e);
        }
        return handleResult;
    }

    private final String findSuperObjectPrefix = "com.zlt.aps";

    @Before("execution(* com.zlt.aps.controller..*.export*(..))")
    public void beforeExport(JoinPoint joinPoint) throws Throwable {
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

    /**
     * 递归处理参数对象，对所有层级的String类型字段执行escapeHtml()
     */
    private Object escapeParamField(Object param) throws IllegalAccessException {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return escapeParamField(param, visited);
    }

    private Object escapeParamField(Object param, Set<Object> visited) throws IllegalAccessException {
        if (param == null) {
            return null;
        }

        Class<?> clazz = param.getClass();

        if (clazz == String.class) {
            return escapeHtml((String) param);
        }

        if (isPrimitiveLike(clazz)) {
            return param;
        }

        if (!visited.add(param)) {
            return param;
        }

        if (param instanceof Collection) {
            for (Object item : (Collection<?>) param) {
                escapeParamField(item, visited);
            }
            return param;
        }

        if (clazz.isArray()) {
            int len = Array.getLength(param);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(param, i);
                Object escaped = escapeParamField(item, visited);
                if (item instanceof String) {
                    Array.set(param, i, escaped);
                }
            }
            return param;
        }

        if (param instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) param;
            for (Object value : map.values()) {
                escapeParamField(value, visited);
            }
            return param;
        }

        List<Field> fields = getAllFields(clazz);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())
                    || Modifier.isFinal(field.getModifiers())
                    || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            Object fieldValue = field.get(param);
            Class<?> fieldType = field.getType();

            if (fieldType == String.class) {
                String escapeValue = escapeHtml((String) fieldValue);
                field.set(param, escapeValue);
            } else if (!isPrimitiveLike(fieldType)) {
                escapeParamField(fieldValue, visited);
            }
        }
        return param;
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        try {
            input = URLDecoder.decode(input, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
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
        return sb.toString().trim();
    }

    /**
     * 递归处理参数对象，对所有层级的String类型字段执行unescapeHtml()
     */
    private Object unescapeResultData(Object result) throws IllegalAccessException {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return unescapeResultData(result, visited);
    }

    private Object unescapeResultData(Object result, Set<Object> visited) throws IllegalAccessException {
        if (result == null) {
            return null;
        }

        Class<?> clazz = result.getClass();

        if (clazz == String.class) {
            return unescapeHtml((String) result);
        }

        if (isPrimitiveLike(clazz)) {
            return result;
        }

        if (!visited.add(result)) {
            return result;
        }

        if (result instanceof Collection) {
            for (Object item : (Collection<?>) result) {
                unescapeResultData(item, visited);
            }
            return result;
        }

        if (clazz.isArray()) {
            int len = Array.getLength(result);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(result, i);
                Object unescaped = unescapeResultData(item, visited);
                if (item instanceof String) {
                    Array.set(result, i, unescaped);
                }
            }
            return result;
        }

        if (result instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) result;
            for (Object value : map.values()) {
                unescapeResultData(value, visited);
            }
            return result;
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())
                    || Modifier.isFinal(field.getModifiers())
                    || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            Object fieldValue = field.get(result);
            Class<?> fieldType = field.getType();

            if (fieldType == String.class) {
                String newValue = unescapeHtml((String) fieldValue);
                field.set(result, newValue);
            } else if (!isPrimitiveLike(fieldType)) {
                unescapeResultData(fieldValue, visited);
            }
        }

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

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> allFields = new ArrayList<>();
        Class<?> current = clazz;
        String name = current.getName();
        if (name.startsWith(findSuperObjectPrefix)) {
            while (current != null && current != Object.class) {
                allFields.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
        }
        return allFields;
    }

    private boolean isPrimitiveLike(Class<?> clazz) {
        return clazz.isPrimitive()
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class == clazz
                || Character.class == clazz
                || Date.class.isAssignableFrom(clazz)
                || Enum.class.isAssignableFrom(clazz)
                || Class.class == clazz;
    }
}
