package com.zlt.aps.nc.engine.util;

import com.zlt.aps.common.engine.enums.ClassNumThreePlanEnums;

/**
 * 垫胶排程引擎工具类
 */
public class NcEngineUtil {

    /**
     * 根据排程首班班次构建班次索引映射数组
     * <p>例如：首班="03"(中班) → ["03","01","02","03","01","02"]</p>
     *
     * @param startShiftClass 首班班次 classIndex（"01"/"02"/"03"）
     * @return 长度6的数组，shiftIndex→classIndex
     */
    public static String[] buildShiftClassMap(String startShiftClass) {
        ClassNumThreePlanEnums current = ClassNumThreePlanEnums.getClassEnums(startShiftClass);
        if (current == null) {
            current = ClassNumThreePlanEnums.CLASS_DAY; // 默认中班
        }
        String[] map = new String[6];
        for (int i = 0; i < 6; i++) {
            map[i] = current.getClassIndex();
            current = current.getNextClass();
        }
        return map;
    }
}
