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
}
