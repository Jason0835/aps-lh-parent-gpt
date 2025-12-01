package com.zlt.aps.common.core.utils;

import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ApsCommonUtil {

    private static Pattern linePattern = Pattern.compile("_(\\w)");
    private static Pattern humpPattern = Pattern.compile("[A-Z]");

    /**
     * 字符串转成double
     * @param value 字符类型
     * @return
     */
    public static Double getDouble(String value) {
        return getDoubleOrDefault(value, 0D);
    }

    public static Double getDoubleOrDefault(Double value) {
        return value == null ? 0D : value;
    }

    public static int getIntOrDefault(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 字符串转成double，如果无法转换或字符串未空，则返回默认值
     * @param value
     * @param defaultValue
     * @return
     */
    public static Double getDoubleOrDefault(String value, Double defaultValue) {
        if(StringUtils.isBlank(value)) {
            return defaultValue;
        }
        Double num = defaultValue;
        try {
            num = Double.parseDouble(value);
        } catch (Exception e) {
            log.error("字符串转浮点错误");
        }
        return num;
    }

    /**
     * 日志字符串拼接并分割
     * @param value
     * @return
     */
    public static String logSplit(String ...value) {
        String division = "\r\n----------------------------------------------------------\r\n";  //分割符
        StringBuffer logDetail = new StringBuffer("");
        for(int i =0;i<value.length;i++) {
            logDetail.append(value[i]).append(division);;
        }
        return logDetail.toString();
    }

    /**
     * 验证一个字符串是否为数字
     * @param value
     * @return
     */
    public static boolean isNumber(String value) {
        if (value == null)
            return false;
        Pattern pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$");
        return pattern.matcher(value).matches();
    }

    /**
     * 判断一个字符的是否为整数
     * @param value
     * @return
     */
    public static boolean isInteger(String value) {
        if(StringUtils.isBlank(value)){
            return false;
        }
        Pattern pattern = Pattern.compile("^-?\\d+$");
        //添加判断，例如：100.0也是属于整数，需要去掉 ".0"，因为有些属性使用Double存整数值，这样默认数值会加".0"的后缀，导致在此校验过不了。
        if(value.endsWith(".0") || value.endsWith(".00")){
            value= value.substring(0,value.indexOf("."));
        }
        return pattern.matcher(value).matches();
    }

    /**
     * 判断一个字符串是否为：字母、数字以及英文字符
     * @param value
     * @return
     */
    public static boolean isCode(String value) {
        if (value == null)
            return false;
        Pattern pattern = Pattern.compile("^[\\x00-\\xff]*$");
        return pattern.matcher(value).matches();
    }

    /**
     * 判断一个字符的是否符合颜色表达式格式
     * @param value 要验证的字符
     * @return 是否符合
     */
    public static boolean isColorCode(String value) {
        if (value == null)
            return false;
        if ("".equals(value))
            return true;
        Pattern pattern = Pattern.compile("^#[0-9a-fA-F]{6}$");
        return pattern.matcher(value).matches();
    }

//    /**
//     * 判断一个字符串是否符合日期格式，如下：
//     *  "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
//     *  "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
//     *  "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"
//     * @param value 字符串
//     * @return 是否符合格式
//     */
//    public static boolean isDate(String value) {
//        if (value == null)
//            return false;
//        Date date = DateUtils.parseDate(value);
//        return date == null;
//    }


    /**
     * 判断value是否为空，如果不为空，则返回value；如果为空，则返回默认值defaultV
     * @param value
     * @param defaultV
     * @return
     */
    public static String blankDefault(String value, String defaultV) {
        return StringUtils.isNotBlank(value) ? value : defaultV;
    }

    /**
     * 驼峰转下划线,最后转为大写（首字母是小写的）
     * @param str
     * @return
     */
    public static String humpToLine(String str) {
        if(str == null) {
            return null;
        }
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString().toLowerCase();
    }

    /**
     * 下划线转驼峰,正常输出
     * @param str
     * @return
     */
    public static String lineToHump(String str) {
        if(str == null) {
            return null;
        }
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 如果浮点是个整数，那么去掉浮点后面的0（例如一个浮点为12.0，那么页面展示需要把。0去掉，直接展示12）
     * @param value
     * @return
     */
    public static String stripZeros(Object value) {
        if(value == null) {
            return "0";
        }
        return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
    }

    public static void main(String[] arags) {
        System.out.println(isNumber("300"));
        System.out.println(humpToLine("aseSpec"));
        System.out.println(lineToHump("ase_spec"));
    }
}


