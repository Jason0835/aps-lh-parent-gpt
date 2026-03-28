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
        Integer newVal1 = Optional.ofNullable(val1).orElse(0);
        Integer newVal2 = Optional.ofNullable(val2).orElse(0);
        return newVal1 + newVal2;
    }
}
