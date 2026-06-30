package com.zlt.aps.common.core.utils;

import java.util.Optional;

/**
 * APS数字处理工具
 * @author hak
 *
 */
public class ApsNumberUtils {
    /**
     * 安全相加
     * @param val1
     * @param val2
     * @return
     */
    public static Integer safeAdd(Integer... val) {
        Integer result = 0;
        for (Integer valItem: val) {
            result += intValue(valItem);
        }
        return result;
    }
    
    /**
     * 安全相加
     * @param val1
     * @param val2
     * @return
     */
    public static Integer safeAddDefaultNull(Integer... val) {
        Integer result = null;
        for (Integer valItem: val) {
            if (val == null && result == null) {
                continue;
            }
            if (result == null) {
                result = valItem;
            } else {
                result += intValue(valItem);
            }
        }
        return result;
    }
    
    /**
     * 空值自动转0
     * @param val
     * @return
     */
    public static Integer intValue(Object val) {
        if (val instanceof Integer) {
            return Optional.ofNullable((Integer) val).orElse(0);
        } else {
            return 0;
        }
    }
    /**
     * 安全相加
     * @param val1
     * @param val2
     * @return
     */
    public static Long safeAdd(Long... val) {
        Long result = 0L;
        for (Long valItem: val) {
            result += longValue(valItem);
        }
        return result;
    }
    
    /**
     * 空值自动转0
     * @param val
     * @return
     */
    public static Long longValue(Object val) {
        if (val instanceof Long) {
            return Optional.ofNullable((Long) val).orElse(0L);
        } else {
            return 0L;
        }
    }
}
