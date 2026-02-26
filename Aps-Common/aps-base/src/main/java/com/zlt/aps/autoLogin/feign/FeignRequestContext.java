package com.zlt.aps.autoLogin.feign;

import java.util.HashMap;
import java.util.Map;

/**
 * 上下文管理
 * @author zhangxh
 * @date 20250506
 * @description 上下文管理
 */
public class FeignRequestContext {
    private static final ThreadLocal<Map<String, String>> HEADERS = ThreadLocal.withInitial(HashMap::new);

    /**
     * 添加单个Header
     */
    public static void addHeader(String key, String value) {
        if (key != null && value != null) {
            HEADERS.get().put(key, value);
        }
    }

    /**
     * 添加多个Header
     */
    public static void addHeaders(Map<String, String> headerMap) {
        if (headerMap != null && !headerMap.isEmpty()) {
            HEADERS.get().putAll(headerMap);
        }
    }

    /**
     * 获取所有Header
     */
    public static Map<String, String> getHeaders() {
        return HEADERS.get();
    }

    /**
     * 清除当前线程的Header，防止内存泄漏
     */
    public static void clear() {
        HEADERS.remove();
    }
}