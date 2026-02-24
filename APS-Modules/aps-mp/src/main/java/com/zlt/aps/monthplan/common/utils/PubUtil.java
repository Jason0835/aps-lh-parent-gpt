package com.zlt.aps.monthplan.common.utils;

import com.zlt.aps.monthplan.common.accept.IAccept;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.monthplan.common.hashlize.HashKeyAdapter;
import com.zlt.aps.monthplan.common.hashlize.Hashlize;
import com.zlt.aps.monthplan.common.hashlize.IHashKey;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class PubUtil {

    public static boolean isNotEmpty(Object src) {
        return !isEmpty(src);
    }

    private static Boolean emptyAng(Object src) {

        return null;
    }

    public static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if ((value instanceof String)) {
            if (((String) value).trim().length() <= 0) {
                return true;
            } else if (((String) value).trim().toLowerCase().equals("null")) {
                return true;
            } else {
                return false;
            }
        }

        if ((value instanceof Object[]) && (((Object[]) value).length <= 0)) {
            return true;
        }
        // 判断数组中的值是否全部为空null.
        if (value instanceof Object[]) {
            Object[] t = (Object[]) value;
            for (int i = 0; i < t.length; i++) {
                if (t[i] != null) {
                    return false;
                }
            }
            return true;
        }
        if ((value instanceof Collection) && ((Collection) value).size() <= 0) {
            return true;
        }
        if ((value instanceof Dictionary) && ((Dictionary) value).size() <= 0) {
            return true;
        }
        if ((value instanceof Map) && ((Map) value).size() <= 0) {
            return true;
        }
        if ((value instanceof StringBuffer) && ((StringBuffer) value).length() <= 0) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为数值
     *
     * @param str
     * @return
     */
    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    /**
     * 删除最后一个字符
     *
     * @param str
     * @return
     */
    public static String delLastChar(String str) {
        if (str == null) {
            return str;
        }

        return str.length() > 0 ? str.substring(0, str.length() - 1) : str;
    }

    public static String ListToString(List<Integer> openBalls) {
        StringBuffer buffer = new StringBuffer();
        if (isEmpty(openBalls)) {
            return "";
        } else {
            openBalls.forEach(aLong -> {
                buffer.append(aLong);
                buffer.append(",");
            });
            return delLastChar(buffer.toString());
        }
    }

    /**
     * List 转成字符串
     *
     * @param list
     * @return
     */
    public static String strListToString(List<String> list) {
        StringBuffer buffer = new StringBuffer();
        if (isEmpty(list)) {
            return "";
        } else {
            list.forEach(a -> {
                buffer.append(a);
                buffer.append(",");
            });
            return delLastChar(buffer.toString());
        }
    }

    /**
     * "1,2,3,4" 这样的字串转成List
     *
     * @param strValue
     * @return
     */
    public static List<Integer> strSplitToIntegerList(String strValue) {
        String[] split = strValue.split(",");
        List<Integer> list = new ArrayList<>();
        for (String s : split) {
            list.add(Integer.valueOf(s));
        }

        return list;
    }

    /**
     * "1,2,3,4" 这样的字串转成List<String>
     *
     * @param strValue
     * @return
     */
    public static List<String> strSplitToStringList(String strValue) {
        String[] split = strValue.split(",");
        List<String> list = new ArrayList<>();
        for (String s : split) {
            list.add(String.valueOf(s));
        }

        return list;
    }

    /**
     * 判断是否为true
     *
     * @param value
     * @return
     */
    public static boolean isTrue(Object value) {
        if (isEmpty(value)) {
            return false;
        }

        if (value instanceof Integer) {
            return ((Integer) value).intValue() == 1;
        }

        if (value instanceof Short) {
            return ((Short) value).intValue() == 1;
        }

        if (value instanceof String) {
            if (((String) value).toUpperCase().equals("TRUE") || ((String) value).equals("1")) {
                return true;
            }
        }

        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }

        return false;
    }

    public static boolean isFalse(Object value) {
        return !isTrue(value);
    }

    public static int random(int min, int max) {
        // (int) 强转 ,不进行四舍五入
        return (int) (Math.random() * (max - min) + min);
    }

    public static BigDecimal random(BigDecimal min, BigDecimal max) {
        return max.subtract(min).multiply(new BigDecimal(Math.random())).add(min);
    }

    /**
     * 字符串 转 list
     *
     * @param srcStr
     * @param splitStr
     * @return
     */
    public static List<String> strToList(String srcStr, String splitStr) {
        List<String> strList = new ArrayList<>();
        if (PubUtil.isNotEmpty(srcStr)) {
            String[] ids = srcStr.split(splitStr);
            for (String id : ids) {
                strList.add(id);
            }
        }
        return strList;
    }

    /**
     * 转成sql
     * 例： list a,b,c   TO  'a','b','c'
     *
     * @param strList
     * @return
     */
    public static String toSqlStr(List<String> strList) {
        StringBuffer buf = new StringBuffer();
        strList.forEach(item -> {
            buf.append("'").append(item).append("'").append(",");
        });

        return delLastChar(buf.toString());
    }

    /**
     * 转成定长的数字中
     *
     * @param no   数字
     * @param size
     * @return
     */

    public static String getFixedSizeNum(int no, int size) {
        return getFixedSizeNum(no, size, "0");
    }

    /**
     * 转成定长的数字中
     *
     * @param no     数字
     * @param size
     * @param preStr 前置字符
     * @return
     */
    private static String getFixedSizeNum(int no, int size, String preStr) {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < size; j++) {
            buf.append(preStr);
        }
        //"00000000"
        DecimalFormat decimalFormat = new DecimalFormat(buf.toString());
        return decimalFormat.format(no);
    }

    public static Integer getFixedSizeNum9(int size) {
        return Integer.valueOf(getFixedSizeNum(9, size - 1, "9"));
    }

    public static String getUnionString(Collection<String> strCols, String unionChar, String appendChar) {
        if (PubUtil.isEmpty(strCols)) {
            return null;
        }
        StringBuffer ret = new StringBuffer();
        int i = 0;
        for (String str : strCols) {
            if (PubUtil.isEmpty(str)) {
                continue;
            }

            if (i != 0) {
                ret.append(unionChar);
            }
            ret.append(appendChar + str + appendChar);
            i++;
        }
        return ret.toString();
    }

    public static String getUnionSQLStr(String str) {
        return StringConstant.SINGLE_QUOTES + str + StringConstant.SINGLE_QUOTES;
    }

    /**
     * 组合适合in 的SQL的字符串
     */
    public static String getUnionSQLStr(List<String> strList) {
        return getUnionString(strList, StringConstant.COMMA, StringConstant.SINGLE_QUOTES);
    }

    public static String getUnionSQLString(Collection<String> strcols) {
        return getUnionString(strcols, StringConstant.COMMA, StringConstant.SINGLE_QUOTES);
    }

    public static String getUnionSQLStr(String[] strs) {
        return StringUtil.getUnionStr(strs, StringConstant.COMMA, StringConstant.SINGLE_QUOTES);
    }

    /**
     * 安全比较
     */
    public static int safeCompare(BigDecimal d1, BigDecimal d2) {
        d1 = (d1 == null) ? new BigDecimal(0) : d1;
        d2 = (d2 == null) ? new BigDecimal(0) : d2;
        return d1.compareTo(d2);
    }

    public static int safeCompare(String d1, String d2) {
        d1 = (d1 == null) ? "" : d1;
        d2 = (d2 == null) ? "" : d2;
        return d1.compareTo(d2);
    }

    /**
     * 查找某个字符串是否在数组中
     *
     * @param strs
     * @param key
     * @return
     */
    public static boolean find(String[] strs, String key) {
        return find(strs, key, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.equals(o2) == true ? 0 : -1;
            }
        });
    }

    /**
     * 查找某个数字是否在数组中
     *
     * @param intnumbers
     * @param key
     * @return
     */
    public static boolean find(int[] intnumbers, int key) {
        if (intnumbers != null && intnumbers.length > 0) {
            for (int obj : intnumbers) {
                if (obj == key) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查找某对象是否在对象数组中
     *
     * @param objs
     * @param key
     * @param comparator
     * @return
     */
    public static boolean find(Object[] objs, Object key, Comparator comparator) {
        if (objs != null && objs.length > 0 && key != null) {
            for (Object obj : objs) {
                if (comparator.compare(obj, key) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断是否零值或空值
     * 零值或空值时反回 true
     */
    public static boolean isZeroOrNull(BigDecimal d1) {
        if (d1 == null) {
            return true;
        }
        if (d1 != null && d1.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否零值
     */
    public static boolean isZero(BigDecimal d1) {
        if (d1 != null && d1.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        return false;
    }

    public static boolean isTrue(Boolean value) {
        if (value == null) {
            return false;
        }
        return value.booleanValue();
    }

    /**
     * 复制对象属性
     *
     * @param from
     * @param to
     * @param accept 排除属性列表
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void copyPropertiesExclude(Object from, Object to, IAccept accept) throws Exception {
        if (from == null || to == null) {
            return;
        }

        BeanWrapperImpl fromBeanWrapper = new BeanWrapperImpl(from);
        BeanWrapperImpl toBeanWrapper = new BeanWrapperImpl(to);

        PropertyDescriptor[] fromPropertyDescriptors = fromBeanWrapper.getPropertyDescriptors();
        if (PubUtil.isEmpty(fromPropertyDescriptors)) {
            return;
        }

        PropertyDescriptor[] toPropertyDescriptors = toBeanWrapper.getPropertyDescriptors();
        if (PubUtil.isEmpty(toPropertyDescriptors)) {
            return;
        }

        Map<String, PropertyDescriptor> toPropertyDescriptorMap = Hashlize.hashlizeMap(toPropertyDescriptors, (IHashKey) new HashKeyAdapter(new String[]{"name"}));

        String name = null;
        PropertyDescriptor toPropertyDescriptor = null;
        for (PropertyDescriptor fromDescriptor : fromPropertyDescriptors) {
            name = fromDescriptor.getName();

            //不复制字段
            if (accept != null && !accept.isAccept(name)) {
                continue;
            }

            //只读字段不予复制
            toPropertyDescriptor = toPropertyDescriptorMap.get(name);
            if (PubUtil.isEmpty(toPropertyDescriptor) || !toBeanWrapper.isWritableProperty(name) || !toBeanWrapper.isReadableProperty(name)) {
                continue;
            }

            toBeanWrapper.setPropertyValue(name, fromBeanWrapper.getPropertyValue(name));
        }

        if (accept != null) {
            accept.afterAccept(to);
        }

    }

    /**
     * 对象属性值复制，仅复制指定名称的属性值
     *
     * @param from
     * @param to
     * @param includsArray
     * @throws Exception
     */
    public static void copyPropertiesInclude(Object from, Object to, String[] includsArray) throws Exception {
        List<String> includesList = null;
        if (includsArray != null && includsArray.length > 0) {
            includesList = Arrays.asList(includsArray); // 构造列表对象
        } else {
            return;
        }

        BeanWrapperImpl fromBeanWrapper = new BeanWrapperImpl(from);
        BeanWrapperImpl toBeanWrapper = new BeanWrapperImpl(to);

        PropertyDescriptor[] fromPropertyDescriptors = fromBeanWrapper.getPropertyDescriptors();
        if (PubUtil.isEmpty(fromPropertyDescriptors)) {
            return;
        }
        PropertyDescriptor[] toPropertyDescriptors = toBeanWrapper.getPropertyDescriptors();
        if (PubUtil.isEmpty(toPropertyDescriptors)) {
            return;
        }
        Map<String, PropertyDescriptor> toPropertyDescriptorMap = Hashlize.hashlizeMap(toPropertyDescriptors, (IHashKey) new HashKeyAdapter(new String[]{"name"}));

        String name = null;
        PropertyDescriptor toPropertyDescriptor = null;
        for (PropertyDescriptor fromDescriptor : fromPropertyDescriptors) {
            name = fromDescriptor.getName();

            //不复制字段
            if (includesList != null && !includesList.contains(name)) {
                continue;
            }
            //只读字段不予复制
            toPropertyDescriptor = toPropertyDescriptorMap.get(name);
            if (PubUtil.isEmpty(toPropertyDescriptor) || !toBeanWrapper.isWritableProperty(name) || !toBeanWrapper.isReadableProperty(name)) {
                continue;
            }
            toBeanWrapper.setPropertyValue(name, fromBeanWrapper.getPropertyValue(name));
        }

    }

    //返回打印使用精度格式化
    public static String getPrintDecimalFormat(String precision) {
        if (precision == null) {
            return "#,##0";
        } else {
            return "#,##0." + setZeroByPrecision(Integer.parseInt(precision));
        }
    }

    private static String setZeroByPrecision(Integer precision) {
        String str = "";
        for (int i = 0; i < precision; i++) {
            str += 0;
        }
        return str;
    }

    /**
     * 检查文本的匹配
     *
     * @param src
     * @param regex 表达式
     * @return
     */
    public static boolean checkString(String src, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(src);
        return m.matches();
    }
}
