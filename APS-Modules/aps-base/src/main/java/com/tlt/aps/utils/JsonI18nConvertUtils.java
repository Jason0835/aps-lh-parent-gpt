package com.tlt.aps.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.common.utils.PubUtil;

import java.lang.reflect.Field;
import java.util.*;

/**
 * i18n字段转义工具
 */
public class JsonI18nConvertUtils {

    private JsonI18nConvertUtils() {
    }

    /**
     * 根据字段名称，（类中带了 xxxI18n_zh_CN ） 自动组装赋值给字段
     * @param v 传的类
     * @param fields 字段名称， 多个可以用,格开
     * @param <V> 类
     */
    public static  <V> void setMoreI18nField(V v, Class<V> clazz) {
        Map<String, String> fields = getI18nMap(clazz);
        String [] langArray = {"zh_CN","en_US","vi_VN"};
        if (PubUtil.isNotEmpty(fields) ){
            //字段
            for (String key : fields.keySet()) {
                List<Map> lists = new ArrayList<>();
                //原始字段值
                Field field = null;
                try {
                    field = v.getClass().getDeclaredField(key);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
                //循环语言
                for (String lang : langArray) {
                    try {
                        // 获取带i18n和语言的字段对象
                        Field i18nField = v.getClass().getDeclaredField(key+"I18n_"+lang);
                        //设置可访问
                        i18nField.setAccessible(true);
                        String i18nValue = (String) i18nField.get(v); // 获取字段值
                        // 如果I18n+lang有值
                        if (StringUtils.isNotEmpty(i18nValue)){
                            Map map = new HashMap();
                            map.put(lang, i18nValue);
                            lists.add(map);
                        }
                    }catch (Exception e){
                        continue;
                    }
                }
                String jsonString = JSON.toJSONString(lists);
                try {
                    //设置值
                    field.setAccessible(true);
                    field.set(v, jsonString);
                } catch (Exception e) {
                   continue;
                }
            }

        }
    }

    /**
     * i18n字段转义
     *带i18n 则转义成当前语
     * 带 _zh_CN   _en_US 则会将值赋到当前属性
     * @param list 源list
     * @return list
     */
    public static <V> void conventJsonI18n(List<V> list, Class<V> clazz) {
        //当前用户语言
        Locale locale = I18nUtil.getLocaleFromRedis();
        conventJsonI18nByLocale(list, clazz, locale);
    }

    /**
     * 根据指定语言包处理多语言的JSON
     */
    public static <V> void conventJsonI18nByLocale(List<V> list, Class<V> clazz, Locale locale) {
        //获取当前类的所有带i18n字段集合
        Map<String, String> fields = getI18nMap(clazz);
        for (V v : list) {
            Set<String> set = fields.keySet();
            for (String fieldName : set) {
                try {
                    // 获取带i18n字段对象
                    Field field = v.getClass().getDeclaredField(fieldName);
                    //设置可访问
                    field.setAccessible(true);
                    String langJson = (String) field.get(v); // 获取字段值
                    //没有数据则跳过
                    if (StringUtils.isNotBlank(langJson)) {
                        //对字段值进行转义取值
                        String convertValue = getConvertValue(langJson, locale);
                        try {
                            Field i18nField = v.getClass().getDeclaredField(fields.get(fieldName));
                            i18nField.setAccessible(true);
                            i18nField.set(v, convertValue);

                        } catch (Exception e) {
                            throw new ServiceException("无法转义" + JSON.toJSONString(list));
                        }
                    }
                    //如果有带语言后缀的也同样赋值 xxx_zh_CN xxx_en_US...
                    List<Map> lists = null;
                    try {
                        lists = JSONArray.parseArray(langJson, Map.class);
                        if (PubUtil.isNotEmpty(lists)) {
                            for (Map<String, String> map : lists) {
                                for (String key : map.keySet()) {
                                    try {
                                        Field field1 = v.getClass().getDeclaredField(fieldName + "I18n_" + key);
                                        field1.setAccessible(true);
                                        field1.set(v, map.get(key));
                                    } catch (Exception e) {
                                        continue;
                                    }
                                }
                            }
//                            Map<String,Object> langMap = lists.get(0);

                        }
                    } catch (JSONException var5) {
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * 设置单个对象Json
     *
     * @param v
     * @param clazz
     * @param <V>
     * @return
     */
    public static <V> void setClassWithI18n(V v, Class<V> clazz) {
        //当前用户语言
        Locale locale = I18nUtil.getLocaleFromRedis();
        //获取当前类的所有带i18n字段集合
        Map<String, String> fields = getI18nMap(clazz);
        //设置对象
        setClzzI18n(fields, v, locale);
    }

    /**
     * 设置List对象转换
     *
     * @param
     * @param clazz
     * @param <V>
     * @return
     */
    public static <V> void setListWithI18n(List<V> list, Class<V> clazz) {
        //当前用户语言
        Locale locale = I18nUtil.getLocaleFromRedis();
        //获取当前类的所有带i18n字段集合
        Map<String, String> fields = getI18nMap(clazz);
        for (V v : list) {
            setClzzI18n(fields, v, locale);
        }
    }

    /**
     * 将数据与当前语言进行拼接，主要用于判断是否存在
     * @param value
     * @return
     */
    public static String geFieldValueWithtLocale(String value){
        Locale locale = I18nUtil.getLocaleFromRedis();
        Map<String,String> map = new HashMap<>();
        map.put(locale.toString(),value);
        return JSON.toJSONString(map);
    }

    /**
     * 转义当前Info
     * @param v
     * @param clazz
     * @return
     */
    public static <V> V getInfo(V v, Class<V> clazz) {
        if (PubUtil.isNotEmpty(v)) {
            List<V> infoList = new ArrayList();
            infoList.add(v);
            JsonI18nConvertUtils.conventJsonI18n(infoList, clazz);
            return infoList.get(0);
        } else {
            return (V) clazz;
        }
    }


    private static <V> void setClzzI18n(Map<String, String> fields, V v, Locale locale) {
        Set<String> set = fields.keySet();
        for (String fieldName : set) {
            try {
                // 获取带i18n字段对象
                Field i18nField = v.getClass().getDeclaredField(fields.get(fieldName));
                //设置可访问
                i18nField.setAccessible(true);
                String i18nValue = (String) i18nField.get(v); // 获取字段值

                //获取对应的字段
                Field field = v.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                String fieldValue = (String) field.get(v); // 获取字段值

                List<Map> lists = new ArrayList<>();
                //判断fieldValue是否有值
                if (StringUtils.isNotEmpty(fieldValue)) {
                    //如果转换不了，则跳过
                    try {
                        lists = JSONArray.parseArray(fieldValue, Map.class);
                    } catch (JSONException e) {
                        continue;
                    }
                    //更新或新增
                    for (Map map : lists) {
                        map.put(locale.toString(), i18nValue);
                    }
                    //fieldValue无值
                } else {
                    Map map = new HashMap();
                    map.put(locale.toString(), i18nValue);
                    lists.add(map);
                }
                String jsonString = JSON.toJSONString(lists);
                try {
                    //设置值
                    field.setAccessible(true);
                    field.set(v, jsonString);
                } catch (Exception e) {
                    throw new ServiceException("无法转义" + JSON.toJSONString(v));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * i18n 转换
     *
     * @param langJson
     * @param locale
     * @return
     */
    public static String getConvertValue(String langJson, Locale locale) {
        String convertValue = StringUtils.getLocaleName(langJson, locale, null);
        if (StringUtils.isBlank(convertValue)) {
            //没有的话就取中文
            Locale locale1 = new Locale("zh", "CN");
            convertValue = StringUtils.getLocaleName(langJson, locale1, langJson);
        }
        return convertValue;
    }


    /**
     * 取出所有带i18字符串的字段
     *
     * @param clazz
     * @param <V>
     * @return
     */
    private static <V> Map<String, String> getI18nMap(Class<V> clazz) {
        Map<String, String> map = new HashMap<String, String>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().endsWith("I18n")) {
                String fieldName = field.getName();
                String key = fieldName.replace("I18n", "");
                map.put(key, fieldName);
            }
        }
        return map;
    }

}
