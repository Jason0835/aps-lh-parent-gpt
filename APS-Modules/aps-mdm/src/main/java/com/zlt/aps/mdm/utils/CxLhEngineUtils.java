package com.zlt.aps.mdm.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * CxEngineUtils 程序算法工具类
 *
 * @author nick
 */
public class CxLhEngineUtils {

    /**
     * 后缀流水号长度默认4位
     */
    public static final int DEFAULT_LENGTH = 4;

    /**
     * 根据指定日期和偏移量计算新日期
     *
     * @param date   指定日期
     * @param offset 偏移量（正数表示后几天，负数表示前几天）
     * @return 计算后的新日期
     */
    public static Date calculateDate(Date date, int offset) {
        if (date == null) {
            throw new IllegalArgumentException("日期不能为空");
        }

        // 使用 Calendar 进行日期计算
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        // 添加偏移量
        calendar.add(Calendar.DAY_OF_MONTH, offset);

        return calendar.getTime();
    }


    /**
     * 获取流水号
     *
     * @param prefix 前缀
     * @param seq    序列号
     * @return 批次号
     */
    public static String getSequence(String prefix, long seq) {
        String str = String.valueOf(seq);
        int len = str.length();
        // 取决于业务规模,应该不会到达3
        if (len > DEFAULT_LENGTH) {
            return str;
        }
        int rest = DEFAULT_LENGTH - len;
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < rest; i++) {
            sb.append('0');
        }
        sb.append(str);
        return sb.toString();
    }


    /**
     * 判断字段值是否为空（兼容多种空值情况）
     */
    public static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).trim().isEmpty();
        return false;
    }


    /**
     * 字段值比较（支持字符串比较）
     */
    public static boolean isFieldValueEqual(Object taskVal, Object machineVal) {
        String taskStr = String.valueOf(taskVal).trim();
        String machineStr = String.valueOf(machineVal).trim();
        return taskStr.equalsIgnoreCase(machineStr);
    }

}
