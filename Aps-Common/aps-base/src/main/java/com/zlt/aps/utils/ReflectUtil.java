package com.zlt.aps.utils;

import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.common.utils.PubUtil;

import java.util.Map;

/**
 * Copyright (c) 2024, All rights reserved。
 * 文件名称：ReflectUtil.java
 * 描    述：
 *
 * @author cxy
 * @version 1.0
 * @date 2024/4/11
 */
public class ReflectUtil {

    public static void setFieldBySourceMap(Map<String, String> sourceMap, Object obj, String getFieldName, String setFieldName, Boolean isI18n) {
        String objValue = ReflectUtils.getFieldValue(obj, getFieldName);
        if (StringUtils.isBlank(objValue)) {
            return;
        }
        String[] objValueArr = objValue.split(",");
        StringBuilder resultStr = new StringBuilder();
        for (String code : objValueArr) {
            if (sourceMap.containsKey(code)) {
                String str = sourceMap.get(code);
                String result = str;
                if (isI18n) {
                    result = JsonI18nConvertUtils.getConvertValue(str, I18nUtil.getLocaleFromRedis());
                }
                resultStr.append(result).append(",");
            }
        }
        if (StringUtils.isNotBlank(resultStr)) {
            String result = resultStr.substring(0, resultStr.length() - 1);
            ReflectUtils.setFieldValue(obj, setFieldName, result);
        }
    }

    public static void setFieldBySourceMap4Long(Map<Long, String> sourceMap, Object obj, String getFieldName, String setFieldName, Boolean isI18n) {
        String objValue = ReflectUtils.getFieldValue(obj, getFieldName);
        if (StringUtils.isBlank(objValue)) {
            return;
        }
        String[] objValueArr = objValue.split(",");
        StringBuilder resultStr = new StringBuilder();
        for (String code : objValueArr) {
            if (PubUtil.isNotEmpty(code)) {
                Long key = Long.valueOf(code);
                if (sourceMap.containsKey(key)) {
                    String str = sourceMap.get(key);
                    String result = str;
                    if (isI18n) {
                        result = JsonI18nConvertUtils.getConvertValue(str, I18nUtil.getLocaleFromRedis());
                    }
                    resultStr.append(result).append(",");
                }
            }
        }
        if (StringUtils.isNotBlank(resultStr)) {
            String result = resultStr.substring(0, resultStr.length() - 1);
            ReflectUtils.setFieldValue(obj, setFieldName, result);
        }
    }
}
