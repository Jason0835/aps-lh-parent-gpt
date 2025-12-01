package com.zlt.mix.common.core.utils;

import java.math.BigDecimal;

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

    public static double add(Double ...value) {
        BigDecimal result = BigDecimal.ZERO;
        for(int i=0;i<value.length;i++) {
        	if (value[i] == null) {
        		continue;
        	}
            BigDecimal mid = new BigDecimal(value[i].toString());
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
	 * 取出最小值
	 * 
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static final <R extends Number> R least(Comparable<R>... value) {
		R minValue = null;
		for (Comparable<R> compareValue : value) {
			if (minValue == null) {
				minValue = (R) compareValue;
			} else if (compareValue.compareTo(minValue) < 0) {
				minValue = (R) compareValue;
			}
		}
		return (R) minValue;
	}

	/**
	 * 取出最大值
	 * 
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static final <R extends Number> R greatest(Comparable<R>... value) {
		R maxValue = null;
		for (Comparable<R> compareValue : value) {
			if (maxValue == null) {
				maxValue = (R) compareValue;
			} else if (compareValue.compareTo(maxValue) > 0) {
				maxValue = (R) compareValue;
			}
		}
		return (R) maxValue;
	}
	
	/**
	 * 将Number转换为BigDecimal对象
	 * 
	 * @param val 待转换的Number对象
	 * @return
	 */
	public static final BigDecimal valueOf(Number val) {
		if (val == null) {
			return null;
		} else if (val instanceof Integer) {
			return BigDecimal.valueOf((Integer) val);
		} else if (val instanceof Long) {
			return BigDecimal.valueOf((Long) val);
		} else if (val instanceof Double) {
			return BigDecimal.valueOf((Double) val);
		} else if (val instanceof BigDecimal) {
			return (BigDecimal) val;
		} else {
			return new BigDecimal(val.toString());
		}
	}

	/**
	 * 将Number转换为BigDecimal对象，如果Number值为空，则返回默认值
	 * 
	 * @param val        待转换的Number对象
	 * @param defaultVal val为空时返回的默认值
	 * @return
	 */
	public static final BigDecimal valueOf(Number val, BigDecimal defaultVal) {
		return val != null ? valueOf(val) : defaultVal;
	}

	/**
	 * 将Number转换为BigDecimal对象，如果Number值为空，则返回0
	 * 
	 * @param val 待转换的Number对象
	 * @return
	 */
	public static final BigDecimal valueOfZero(Number val) {
		return valueOf(val, BigDecimal.ZERO);
	}
	
	/**
	 * 判断数值等于0
	 * @param val
	 * @return
	 */
	public static final boolean eqZero(Number val) {
		return valueOfZero(val).compareTo(BigDecimal.ZERO) == 0;
	}
	
	/**
	 * 判断数值大于0
	 * @param val
	 * @return
	 */
	public static final boolean gtZero(Number val) {
		return valueOfZero(val).compareTo(BigDecimal.ZERO) > 0;
	}
	
	/**
	 * 判断数值大于等于0
	 * @param val
	 * @return
	 */
	public static final boolean geZero(Number val) {
		return valueOfZero(val).compareTo(BigDecimal.ZERO) >= 0;
	}
	
	/**
	 * 判断数值小于0
	 * @param val
	 * @return
	 */
	public static final boolean ltZero(Number val) {
		return valueOfZero(val).compareTo(BigDecimal.ZERO) < 0;
	}
	
	/**
	 * 判断数值小于等于0
	 * @param val
	 * @return
	 */
	public static final boolean leZero(Number val) {
		return valueOfZero(val).compareTo(BigDecimal.ZERO) <= 0;
	}
}


