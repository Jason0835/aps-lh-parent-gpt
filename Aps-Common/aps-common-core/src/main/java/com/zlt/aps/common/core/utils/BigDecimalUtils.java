package com.zlt.aps.common.core.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * BigDecimal计算工具类
 *
 * @author pancd
 */
public class BigDecimalUtils {

    /**
     * 数值BigDecimal 100
     */
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal TWO = new BigDecimal("2"); // 用于计算平分
    public static final BigDecimal HOUR24 = new BigDecimal("24"); // 一天24小时

    public BigDecimalUtils() {
        super();
    }

    /**
     * 多个相加
     *
     * @param bigDecimals
     * @return
     */
    public static BigDecimal add(BigDecimal... bigDecimals) {
        BigDecimal result = null;
        for (BigDecimal data : bigDecimals) {
            //第一次进入
            if (result == null) {
                result = data;
            } else {
                result = BigDecimalUtils.add(result, data);
            }
        }
        return result;
    }

    /**
     * 多个相加
     *
     * @param bigDecimals
     * @return
     */
    public static BigDecimal add(Number... bigDecimals) {
        BigDecimal result = null;
        for (Number data : bigDecimals) {
            //第一次进入
            if (result == null) {
                result = valueOf(data);
            } else {
                result = add(result, data);
            }
        }
        return result;
    }

    /**
     * number1加number2
     *
     * @param number1
     * @param number2
     * @return
     */
    public static BigDecimal add(BigDecimal number1, BigDecimal number2) {
        number1 = (number1 == null) ? BigDecimal.ZERO : number1;
        number2 = (number2 == null) ? BigDecimal.ZERO : number2;
        return number1.add(number2);
    }

    /**
     * number1加number2
     *
     * @param number1
     * @param number2
     * @return
     */
    public static BigDecimal add(Object number1, Object number2) {
        return valueOf(number1).add(valueOf(number2));
    }

    /**
     * number1减number2
     *
     * @param number1
     * @param number2
     * @return
     */
    public static BigDecimal sub(BigDecimal number1, BigDecimal number2) {
        number1 = (number1 == null) ? BigDecimal.ZERO : number1;
        number2 = (number2 == null) ? BigDecimal.ZERO : number2;
        return number1.subtract(number2);
    }

    /**
     * number1减number2
     *
     * @param number1
     * @param number2
     * @return
     */
    public static BigDecimal sub(Object number1, Object number2) {
        return BigDecimalUtils.sub(valueOf(number1), valueOf(number2));
    }

    /**
     * 参数所有元素相乘
     *
     * @param numbers
     * @return
     */
    public static BigDecimal multiply(Object... numbers) {
        BigDecimal result = null;
        for (Object number : numbers) {
            if (result == null) {
                result = valueOf(number);
            } else {
                result = multiply(result, number, true);
            }
        }
        return result;
    }

    /**
     * number1乘number2
     *
     * @param number1
     * @param number2
     * @return
     */
    public static BigDecimal multiply(BigDecimal number1, BigDecimal number2) {
        return BigDecimalUtils.multiply(number1, number2, true);
    }

    /**
     * number1乘number2
     *
     * @param number1
     * @param number2
     * @param nullIsZero 是否允许number1,number2如果是null就赋值为0
     * @return
     */
    public static BigDecimal multiply(Object number1, Object number2, boolean nullIsZero) {
        BigDecimal bigDecimal1;
        BigDecimal bigDecimal2;
        if (nullIsZero) {
            bigDecimal1 = (number1 == null) ? BigDecimal.ZERO : valueOf(number1);
            bigDecimal2 = (number2 == null) ? BigDecimal.ZERO : valueOf(number2);
        } else {
            if (null == number1) {
                return null;
            }
            if (null == number2) {
                return null;
            }
            bigDecimal1 = valueOf(number1);
            bigDecimal2 = valueOf(number2);
        }
        return bigDecimal1.multiply(bigDecimal2);
    }

    /**
     * number1除number2(忽略小数位)
     *
     * @param number1
     * @param number2
     * @param power   小数位
     * @return
     */
    public static BigDecimal div(Object number1, Object number2, Integer power) {
        return BigDecimalUtils.div(number1, number2, power, true);
    }

    /**
     * number1除number2(保留小数位)
     *
     * @param number1
     * @param number2
     * @param power      小数位
     * @param nullIsZero 是否允许number1,number2如果是null就赋值为0
     * @return
     */
    public static BigDecimal div(Object number1, Object number2, Integer power, boolean nullIsZero) {
        return BigDecimalUtils.div(number1, number2, power, nullIsZero,BigDecimal.ROUND_HALF_UP);
    }

    /**
     * number1除number2(保留小数位)
     *
     * @param number1
     * @param number2
     * @param power      小数位
     * @param nullIsZero 是否允许number1,number2如果是null就赋值为0
     * @return
     */
    public static BigDecimal div(Object number1, Object number2, Integer power, boolean nullIsZero,int powerMode) {
        BigDecimal bigDecimal1;
        BigDecimal bigDecimal2;
        if (nullIsZero) {
            bigDecimal1 = (number1 == null) ? BigDecimal.ZERO : valueOf(number1);
            bigDecimal2 = (number2 == null) ? BigDecimal.ZERO : valueOf(number2);
        } else {
            if (null == number1) {
                return null;
            }
            if (null == number2) {
                return null;
            }
            bigDecimal1 = valueOf(number1);
            bigDecimal2 = valueOf(number2);
        }
        if (bigDecimal1.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        //被除数不能为0
        if (bigDecimal2.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        if (power == null)
            power = 8;
        return bigDecimal1.divide(bigDecimal2, power, powerMode);
    }

    /**
     * number1除number2(忽略小数位)
     *
     * @param number1
     * @param number2
     * @return
     */
    public static BigDecimal div(Object number1, Object number2) {
        return BigDecimalUtils.div(number1, number2, true);
    }

    /**
     * number1除number2(忽略小数位)
     *
     * @param number1
     * @param number2
     * @param nullIsZero 是否允许number1,number2如果是null就赋值为0
     * @return
     */
    public static BigDecimal div(Object number1, Object number2, boolean nullIsZero) {
        BigDecimal bigDecimal1;
        BigDecimal bigDecimal2;
        if (nullIsZero) {
            bigDecimal1 = (number1 == null) ? BigDecimal.ZERO : valueOf(number1);
            bigDecimal2 = (number2 == null) ? BigDecimal.ZERO : valueOf(number2);
        } else {
            if (null == number1) {
                return null;
            }
            if (null == number2) {
                return null;
            }
            bigDecimal1 = valueOf(number1);
            bigDecimal2 = valueOf(number2);
        }
        if (bigDecimal1.compareTo(BigDecimal.ZERO) == 0){
            return BigDecimal.ZERO;
        }
        //被除数不能为0
        if (bigDecimal2.compareTo(BigDecimal.ZERO) == 0){
            return BigDecimal.ZERO;
        }

        return bigDecimal1.divide(bigDecimal2, 2, RoundingMode.HALF_UP);
    }

    /**
     * 绝对值
     *
     * @param number 数值
     * @return
     */
    public static BigDecimal abs(BigDecimal number) {
        return number == null ? BigDecimal.ZERO : number.abs();
    }

    /**
     * 保留 指定小数位 且向上进位的四舍五入
     *
     * @param number    数值
     * @param precision 精度位数
     * @return
     */
    public static BigDecimal round(BigDecimal number, int precision) {
        if (null == number) {
            return null;
        } else {
            return number.setScale(precision, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * 将数据按照给定的保留位数和舍入模式转成字符串
     *
     * @param amt:待格式化的数据
     * @param amount：保留小数位数
     * @param mode：舍入模式（默认四舍五入）
     * @return
     */
    public static String roundFormat(BigDecimal amt, Integer amount, RoundingMode mode) {
        int len = amount == null ? 2 : amount;
        //小数位数
        String str = "";
        while (len != 0) {
            str += "0";
            len--;
        }
        DecimalFormat df = new DecimalFormat("#,##0." + str);
        //舍入模式
        if (null == mode) {
            mode = RoundingMode.HALF_UP;
        }
        df.setRoundingMode(mode);
        if (amt == null) {
            amt = BigDecimal.ZERO;
        }
        return df.format(amt);
    }

    /**
     * 安全比较
     */
    public static int safeCompare(Double d1, Double d2) {
        d1 = (d1 == null) ? 0 : d1;
        d2 = (d2 == null) ? 0 : d2;
        return d1.compareTo(d2);
    }

    /**
     * 安全比较
     */
    public static int safeCompare(BigDecimal d1, BigDecimal d2) {
        d1 = (d1 == null) ? BigDecimal.ZERO : d1;
        d2 = (d2 == null) ? BigDecimal.ZERO : d2;
        return d1.compareTo(d2);
    }


    public static BigDecimal getZeroIfNull(BigDecimal val){
        val=(val == null) ? BigDecimal.ZERO : val;
        return val;
    }

    /**
     * 计算除法取整
     * @param number1 被除数
     * @param number2 除数
     * @param power   保留位数
     * @param nullIsZero 是否可为空
     * @return 结果
     */
    public static Integer div4Int(Object number1, Object number2, Integer power, boolean nullIsZero) {
        BigDecimal bigDecimal1;
        BigDecimal bigDecimal2;
        if (nullIsZero) {
            bigDecimal1 = (number1 == null) ? BigDecimal.ZERO : valueOf(number1);
            bigDecimal2 = (number2 == null) ? BigDecimal.ZERO : valueOf(number2);
        } else {
            if (null == number1) {
                return null;
            }
            if (null == number2) {
                return null;
            }
            bigDecimal1 = valueOf(number1);
            bigDecimal2 = valueOf(number2);
        }
        if (bigDecimal1.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        //被除数不能为0
        if (bigDecimal2.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        if (power == null) {
            power = 8;
        }
        BigDecimal result = bigDecimal1.divide(bigDecimal2, power, BigDecimal.ROUND_HALF_UP);
        return result.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
    }

    public static Integer div4Int(Object number1, Object number2, Integer power) {
        return BigDecimalUtils.div4Int(number1, number2, power, true);
    }

    /**
     * 判断是否为整数 如果是，则返回string 值
     * @param input
     * @return
     */
    public static String isIntBigDecimal(BigDecimal input) {
        return  input.stripTrailingZeros().toPlainString();
    }

    /**
     * 安全获取值
     * @param input
     * @return
     */
    public static BigDecimal getBigDecimal(BigDecimal input){
        return  input != null ? input : BigDecimal.ZERO;
    }
    
    /**
     * 求平均值，小数位0
     *
     * @param bigDecimals
     * @return
     */
    public static BigDecimal avg(Number... bigDecimals) {
        return avg(0, RoundingMode.UP, bigDecimals);
    }

    /**
     * 求平均值
     *
     * @param bigDecimals
     * @return
     */
    public static BigDecimal avg(int precision, RoundingMode roundingMode, Number... bigDecimals) {
        BigDecimal total = add(bigDecimals);
        return total.divide(valueOf(bigDecimals.length), precision, roundingMode);
    }


    /**
     * 转换类型
     * @param obj 要转换对象
     * @return 结果
     */
    public static BigDecimal valueOf(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal bigDecimal = null;
        if (obj instanceof BigDecimal) {
            bigDecimal = (BigDecimal) obj;
        } else if (obj instanceof String) {
            bigDecimal = new BigDecimal((String) obj);
        } else if (obj instanceof Number) {
            bigDecimal = new BigDecimal(String.valueOf(obj));
        }
        if (bigDecimal != null) {
           return bigDecimal;
        } else {
            return BigDecimal.ZERO;
        }
    }

    
    /**
     * 数量增加，原数量为0时不处理
     * @param qty
     * @param addNum
     * @return
     */
    public static double qtyAdd(double qty, Number addNum) {
        if (qty == 0D) {
            return qty;
        }
        BigDecimal result = BigDecimalUtils.add(qty, addNum);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            return 0D;
        }
        return result.doubleValue();
    }
    
    /**
     * 数量减少，原数量为0时不处理，且结果不可小于0
     * @param qty
     * @param subNum
     * @return
     */
    public static double qtySub(double qty, Number subNum) {
        if (qty == 0D) {
            return qty;
        }
        BigDecimal result = BigDecimalUtils.sub(qty, subNum);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            return 0D;
        }
        return result.doubleValue();
    }
    
    /**
     * 获取最大值
     * @param vars
     * @return
     */
    public static BigDecimal greatest(Number... vars) {
        return Arrays.stream(vars).map(BigDecimalUtils::valueOf).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    
    /**
     * 获取最小值
     * @param vars
     * @return
     */
    public static BigDecimal least(Number... vars) {
        return Arrays.stream(vars).map(BigDecimalUtils::valueOf).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    
    /**
     * 按取整单位将数量取整计算（向上）
     * @param var
     * @param unit 取整单位
     * @return
     */
    public static BigDecimal ceil(Number var, BigDecimal unit) {
        return valueOf(var).divide(unit, 0, RoundingMode.UP).multiply(unit);
    }
    
    /**
     * 按取整单位将数量取整计算（向下）
     * @param var
     * @param unit 取整单位
     * @return
     */
    public static BigDecimal floor(Number var, BigDecimal unit) {
        return valueOf(var).divide(unit, 0, RoundingMode.DOWN).multiply(unit);
    }
    
    /**
     * 平分
     * @param var
     * @return
     */
    public static BigDecimal half(Number var) {
        return valueOf(var).divide(TWO, 0, RoundingMode.HALF_UP);
    }
    
    /**
     * 处理负数，小于0的数值处理成0
     * @param var
     * @return
     */
    public static BigDecimal upToZero(Number var) {
        BigDecimal bigDecimalVar = valueOf(var);
        return bigDecimalVar.compareTo(BigDecimal.ZERO) >= 0? bigDecimalVar: BigDecimal.ZERO;
    }
    
    /**
     * 百分数转换成小数
     * @param var
     * @return
     */
    public static BigDecimal percentages2Decimals(Number var) {
        BigDecimal bigDecimalVar = valueOf(var);
        return bigDecimalVar.divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }
    
    /**
     * 区间比较大小，闭区间
     * @param var
     * @param min
     * @param max
     * @return
     */
    public static boolean between(Number var, Number min, Number max) {
        return between(var, min, max, true, true);
    }
    /**
     * 区间比较大小，开区间
     * @param var
     * @param min
     * @param max
     * @return
     */
    public static boolean betweenOpen(Number var, Number min, Number max) {
        return between(var, min, max, false, false);
    }
    
    /**
     * 区间比较大小，左闭合
     * @param var
     * @param min
     * @param max
     * @return
     */
    public static boolean betweenLC(Number var, Number min, Number max) {
        return between(var, min, max, true, false);
    }
    
    /**
     * 区间比较大小，右闭合
     * @param var
     * @param min
     * @param max
     * @return
     */
    public static boolean betweenRC(Number var, Number min, Number max) {
        return between(var, min, max, false, true);
    }
    
    /**
     * 区间比较大小
     * @param var
     * @param min
     * @param max
     * @param isLetClosed   是否左闭合
     * @param isRightClosed 是否右闭合
     * @return
     */
    public static boolean between(Number var, Number min, Number max, boolean isLeftClosed, boolean isRightClosed) {
        BigDecimal bigVar = valueOf(var);
        BigDecimal bigMin = valueOf(min);
        BigDecimal bigMax = valueOf(max);
        boolean leftResult = isLeftClosed? bigVar.compareTo(bigMin) >= 0: bigVar.compareTo(bigMin) > 0;
        boolean rightResult = isRightClosed? bigVar.compareTo(bigMax) <= 0: bigVar.compareTo(bigMax) < 0;
        return leftResult && rightResult;
    }
}
