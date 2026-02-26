package com.zlt.aps.autoLogin.loginUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangxh
 * @date 20250605
 * 用于记录方法执行过程中的变量值
 */
public class ProcessContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT =
            ThreadLocal.withInitial(HashMap::new);
    /**
     * 过程数据
     */
    public static final String PROCESS = "process";

    /**
     * 设置变量值
     */
    public static void setVariable(String name, Object value) {
        CONTEXT.get().put(name, value);
    }

    /**
     * 获取变量值
     */
    public static Object getVariable(String name) {
        return CONTEXT.get().get(name);
    }

    /**
     * 获取所有变量
     */
    public static Map<String, Object> getAllVariables() {
        return new HashMap<>(CONTEXT.get());
    }
    
    /**
     * 清理上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
