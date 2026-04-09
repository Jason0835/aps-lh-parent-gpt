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
    public static Integer safeAdd(Integer val1, Integer val2) {
        Integer newVal1 = intValue(val1);
        Integer newVal2 = intValue(val2);
        return newVal1 + newVal2;
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
    public static Long safeAdd(Long val1, Long val2) {
        Long newVal1 = longValue(val1);
        Long newVal2 = longValue(val2);
        return newVal1 + newVal2;
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
