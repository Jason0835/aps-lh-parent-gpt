package com.zlt.aps.common.core.utils;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

public class BigDecimalUtil {

    // 除法运算默认精度
    private static final int DEF_DIV_SCALE = 10;

    private BigDecimalUtil() {
    }

    /**
     * 精确加法
     */
    public static double add(double value1, double value2) {
        BigDecimal b1 = BigDecimal.valueOf(value1);
        BigDecimal b2 = BigDecimal.valueOf(value2);
        return b1.add(b2).doubleValue();
    }

    public static double add(double ...value) {
        BigDecimal result = BigDecimal.valueOf(0D);
        for(int i=0;i<value.length;i++) {
            BigDecimal mid = BigDecimal.valueOf(value[i]);
            result = result.add(mid);
        }
        return result.doubleValue();
    }

    /**
     * 精确减法
     */
    public static double sub(double value1, double value2) {
        BigDecimal b1 = BigDecimal.valueOf(value1);
        BigDecimal b2 = BigDecimal.valueOf(value2);
        return b1.subtract(b2).doubleValue();
    }

    /**
     * 精确乘法
     */
    public static double mul(double value1, double value2) {
        if(value2 == 0) {
            return 0;
        }
        BigDecimal b1 = BigDecimal.valueOf(value1);
        BigDecimal b2 = BigDecimal.valueOf(value2);
        return b1.multiply(b2).doubleValue();
    }

    /**
     * 精确除法 使用默认精度
     */
    public static double div(double value1, double value2) {
        return div(value1, value2, DEF_DIV_SCALE);
    }

    /**
     * 精确除法
     *
     * @param scale
     * 精度
     */
    public static double div(double value1, double value2, int scale)  {
        if(value2 == 0) {
            return 0;
        }
        BigDecimal b1 = new BigDecimal(String.valueOf(value1));
        BigDecimal b2 = new BigDecimal(String.valueOf(value2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 四舍五入
     *
     * @param scale
     * 小数点后保留几位
     */
    public static double round(double v, int scale) throws IllegalAccessException {
        return div(v, 1, scale);
    }

    /**
     * 数值向上取整
     * @param value
     * @param scale 小数点后保留几位
     * @return
     */
    public static double roundUp(double value, int scale){
        return new BigDecimal(String.valueOf(value)).setScale(scale, BigDecimal.ROUND_UP).doubleValue();
    }

    /**
     * 数值向下取整
     * @param value
     * @param scale 小数点后保留几位
     * @return
     */
    public static double roundDown(double value, int scale){
        return new BigDecimal(String.valueOf(value)).setScale(scale, BigDecimal.ROUND_DOWN).doubleValue();
    }
    
    /**
     * 返回bigdecimal类型
     * @param value
     * @return
     */
    public static BigDecimal getValue(Double value) {
    	if (value == null) {
    		return BigDecimal.ZERO;
    	}
    	return new BigDecimal(String.valueOf(value));
    }
    
    /**
     * 判断字符串是否数字
     *
     * @param str
     * @return
     */
    public static boolean isDigits(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        return str.matches("-?[0-9]+.?[0-9]*");
    }
}


