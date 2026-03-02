package com.zlt.aps.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.I18nConstant;
import com.zlt.aps.constant.StringConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 不排产原因等含有国际化信息的解析
 * 迁移到aps-base模块
 *
 * @author ZLT
 * @version 1.0
 * @Description
 * @date 20251205
 */
@Slf4j
public class JsonUtils {
    /**
     * 语言长度
     */
    private static int LANGUAGE_LENGTH = 2;

    /**
     * 获取语言包信息
     *
     * @return
     */
    public static Map<String, Locale> getLanguageMap() {
        List<String> languageList = getLanguageList();
        if (CollectionUtils.isEmpty(languageList)) {
            return Collections.emptyMap();
        }
        Map<String, Locale> languageMap = new HashMap<>();
        languageList.forEach(language -> {
            if (StringUtils.isBlank(language)) {
                return;
            }
            String[] languageArray = language.split(String.valueOf(StringConstant.CHAR_UNDERLINE));
            if (languageArray.length != LANGUAGE_LENGTH) {
                return;
            }
            languageMap.put(language, new Locale(languageArray[BigDecimal.ZERO.intValue()], languageArray[BigDecimal.ONE.intValue()]));
        });
        return languageMap;
    }

    /**
     * 获取语言包信息
     *
     * @return
     */
    public static List<String> getLanguageList() {
        List<String> languageList = new ArrayList<>();
        languageList.add(I18nConstant.ZH_CN);
        languageList.add(I18nConstant.EN_US);
        languageList.add(I18nConstant.VI_VN);
        return languageList;
    }

    /**
     * 获取语言Json对象
     *
     * @param languageKey 语言包key
     * @return
     */
    public static JSONObject getLanguageJsonObject(String languageKey) {
        Map<String, Locale> languageMap = getLanguageMap();
        if (CollectionUtils.isEmpty(languageMap)) {
            return new JSONObject();
        }
        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        languageMap.forEach((language, languageLocale) -> {
            String languageContent = I18nUtil.getMessage(languageKey, languageLocale);
            reasonJson.put(language, languageContent);
        });
        return reasonJson;
    }


    /**
     * 获取语言Json对象
     *
     * @param languageKey 语言包key
     * @param format      需要转换的参数
     * @return
     */
    public static JSONObject getLanguageJsonObject(String languageKey, Object format) {
        Map<String, Locale> languageMap = getLanguageMap();
        if (CollectionUtils.isEmpty(languageMap)) {
            return new JSONObject();
        }
        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        languageMap.forEach((language, languageLocale) -> {
            String languageFormat = I18nUtil.getMessage(languageKey, languageLocale);
            String languageContent = String.format(languageFormat, format);
            reasonJson.put(language, languageContent);
        });
        return reasonJson;
    }

    /**
     * 获取语言Json对象
     * 采用String.format拼接动态参数值
     *
     * @param languageKey 语言包key
     * @param params      参数值数组
     * @return
     */
    public static JSONObject getLanguageJsonObject(String languageKey, Object... params) {
        Map<String, Locale> languageMap = getLanguageMap();
        if (CollectionUtils.isEmpty(languageMap)) {
            return new JSONObject();
        }
        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        languageMap.forEach((language, languageLocale) -> {
            String languageFormat = I18nUtil.getMessage(languageKey, languageLocale);
            String languageContent = String.format(languageFormat, params);
            reasonJson.put(language, languageContent);
        });
        return reasonJson;
    }

    /**
     * 获取语言Json对象
     *
     * @param languageKey 语言包key
     * @param format1     需要转换的参数
     * @param format2     需要转换的参数
     * @return
     */
    public static JSONObject getLanguageJsonObject(String languageKey, Object format1, Object format2) {
        Map<String, Locale> languageMap = getLanguageMap();
        if (CollectionUtils.isEmpty(languageMap)) {
            return new JSONObject();
        }
        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        languageMap.forEach((language, languageLocale) -> {
            String languageFormat = I18nUtil.getMessage(languageKey, languageLocale);
            String languageContent = String.format(languageFormat, format1, format2);
            reasonJson.put(language, languageContent);
        });
        return reasonJson;
    }

    /**
     * 判断字符串是否是有效的 JSON 对象
     *
     * @param str 要判断的字符串
     * @return 如果是有效的 JSON 对象，返回 true；否则返回 false
     */
    public static boolean isJsonObject(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            JSON.parseObject(str);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 解析json字段信息列表
     *
     * @param list   数据集合
     * @param locale 语言
     * @param fields 字段
     */
    public static void parseJsonRemarkList(List<? extends BaseEntity> list, String locale, String... fields) {
        if (CollectionUtils.isEmpty(list) || StringUtils.isEmpty(locale) || fields == null || fields.length == 0) {
            return;
        }
        for (BaseEntity item : list) {
            for (String field : fields) {
                String fieldJson = (String) item.getFieldValueByFieldName(field);
                if (StringUtils.isBlank(fieldJson)) {
                    continue;
                }
                item.setFieldValueByFieldName(field, parseJsonRemark(fieldJson, locale));
            }
        }
    }

    /**
     * 解析json字段信息列表，并将多个错误内容用<br>换行显示
     *
     * @param list   数据集合
     * @param locale 语言
     * @param fields 字段
     */
    public static void parseJsonRemarkListWithLineBreak(List<? extends BaseEntity> list, String locale, String... fields) {
        if (CollectionUtils.isEmpty(list) || StringUtils.isEmpty(locale) || fields == null || fields.length == 0) {
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        for (BaseEntity item : list) {
            for (String field : fields) {
                String fieldJson = (String) item.getFieldValueByFieldName(field);
                if (StringUtils.isBlank(fieldJson)) {
                    continue;
                }
                String jsonToParse = fieldJson;
                if (!fieldJson.trim().startsWith("[")) {
                    jsonToParse = "[" + fieldJson + "]";
                }
                try {
                    // 使用处理过的 jsonToParse 进行解析
                    JsonNode rootNode = objectMapper.readTree(jsonToParse);
                    List<String> errorMessages = new ArrayList<>();

                    // 此时 rootNode 必定是数组，可以直接遍历
                    if (rootNode.isArray()) {
                        for (JsonNode node : rootNode) {
                            if (node.has(locale)) {
                                errorMessages.add(node.get(locale).asText());
                            }
                        }
                    } else if (rootNode.isObject()) {
                        // 兜底逻辑：万一加了括号还是对象（虽然不太可能）
                        if (rootNode.has(locale)) {
                            errorMessages.add(rootNode.get(locale).asText());
                        }
                    }

                    // 3. 用<br>连接各条错误信息
                    if (!errorMessages.isEmpty()) {
                        String contentWithLineBreak = String.join("</br>", errorMessages);
                        item.setFieldValueByFieldName(field, contentWithLineBreak);
                    }

                } catch (Exception e) {
                    // 解析失败时保持原值或记录日志
                    log.error("解析JSON失败: {}", fieldJson, e);
                }
            }
        }
    }


    /**
     * 解析json备注信息，可能存在多种拼接情况
     * 1、[{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}]
     * 2、{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}
     *
     * @param json json内容
     * @return 解析后的备注信息
     */
    public static String parseJsonRemark(String json, String locale) {
        if (StringUtils.isBlank(json) || StringUtils.isBlank(locale)) {
            return json;
        }
        StringBuilder builder = new StringBuilder();
        try {
            //格式：[{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}...]
            if (json.startsWith("[")) {
                analysisStandardArrayJson(builder, json, locale);
                return builder.length() > 0 ? builder.substring(0, builder.length() - 1) : json;
            }
            //格式：{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}...
            if (json.startsWith("{")) {
                analysisNoStandardArrayJson(builder, json, locale);
                return builder.length() > 0 ? builder.substring(0, builder.length() - 1) : json;
            }
            return builder.length() > 0 ? builder.substring(0, builder.length() - 1) : json;
        } catch (Exception e) {
            log.error("解析json备注信息失败", e);
        }
        return json;
    }

    /**
     * 解析标准数组格式的json
     * 格式：[{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}...]
     *
     * @param builder 数据存储
     * @param json    json格式数据
     * @param locale  语言
     */
    private static void analysisStandardArrayJson(StringBuilder builder, String json, String locale) {
        if (StringUtils.isBlank(json)) {
            return;
        }
        if (!json.startsWith("[")) {
            return;
        }
        JSONArray jsonArray = JSON.parseArray(json);
        for (Object item : jsonArray) {
            JSONObject jsonItem = (JSONObject) item;
            builder.append(jsonItem.get(locale));
            builder.append(",");
        }
    }

    /**
     * 解析标非准数组格式的json
     * 格式：{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}...
     *
     * @param builder 数据存储
     * @param json    json格式数据
     * @param locale  语言
     */
    private static void analysisNoStandardArrayJson(StringBuilder builder, String json, String locale) {
        if (StringUtils.isBlank(json)) {
            return;
        }
        if (!json.startsWith("{")) {
            return;
        }
        int index;
        while ((index = json.indexOf("},{")) >= 0) {
            String tempJson = json.substring(0, index + 1);
            builder.append(JSON.parseObject(tempJson).get(locale));
            builder.append(",");
            json = json.substring(index + 2);
        }
        builder.append(JSON.parseObject(json).get(locale));
        builder.append(",");
    }

}
