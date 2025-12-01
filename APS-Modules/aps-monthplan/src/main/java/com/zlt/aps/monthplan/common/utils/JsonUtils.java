package com.zlt.aps.monthplan.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2024/12/2
 */
@Slf4j
public class JsonUtils {

    public static final String CHINESE = "zh_CN";

    public static final String ENGLISH = "en_US";


    /**
     * 获取语言Json对象
     *
     * @param languageKey 语言包key
     * @return
     */
    public static JSONObject getLanguageJsonObject(String languageKey) {
        String i18noverModCapsCN = I18nUtil.getMessage(languageKey, Locale.SIMPLIFIED_CHINESE);
        String i18noverModCapsUS = I18nUtil.getMessage(languageKey, Locale.US);

        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        reasonJson.put(CHINESE, i18noverModCapsCN);
        reasonJson.put(ENGLISH, i18noverModCapsUS);
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
        String i18noverModCapsCN = I18nUtil.getMessage(languageKey, Locale.SIMPLIFIED_CHINESE);
        String i18noverModCapsUS = I18nUtil.getMessage(languageKey, Locale.US);

        String CN = String.format(i18noverModCapsCN, format);
        String US = String.format(i18noverModCapsUS, format);
        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        reasonJson.put(CHINESE, CN);
        reasonJson.put(ENGLISH, US);
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
        String i18noverModCapsCN = I18nUtil.getMessage(languageKey, Locale.SIMPLIFIED_CHINESE);
        String i18noverModCapsUS = I18nUtil.getMessage(languageKey, Locale.US);

        String CN = String.format(i18noverModCapsCN, format1, format2);
        String US = String.format(i18noverModCapsUS, format1, format2);
        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        reasonJson.put(CHINESE, CN);
        reasonJson.put(ENGLISH, US);
        return reasonJson;
    }

    /**
     * 获取语言Json对象 传入reason
     *
     * @param languageKey 语言包key
     * @param format1     需要转换的参数
     * @return
     */
    public static JSONObject getLanguageJsonObjectByReason(String reason,String languageKey, Object format1) {
        String i18noverModCapsCN = I18nUtil.getMessage(languageKey, Locale.SIMPLIFIED_CHINESE);
        String i18noverModCapsUS = I18nUtil.getMessage(languageKey, Locale.US);

        String CN = "";
        String US = "";

        String reasonCN = "";
        String reasonUS = "";
        //reason需要判断是否是json对象  如果是还需要转换一下  否则就不转换了 直接拼接
        if (isJsonObject(reason)){
            JSONObject jsonObject = JSON.parseObject(reason);
            reasonCN = jsonObject.getString(CHINESE);
            reasonUS = jsonObject.getString(ENGLISH);
            CN = reasonCN + String.format(i18noverModCapsCN, format1);
            US = reasonUS + String.format(i18noverModCapsUS, format1);
        }else{
            CN = reason + String.format(i18noverModCapsCN, format1);
            US = reason + String.format(i18noverModCapsUS, format1);
        }

        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        reasonJson.put(CHINESE, CN);
        reasonJson.put(ENGLISH, US);
        return reasonJson;
    }

    /**
     * 获取语言Json对象 传入reason
     *
     * @param languageKey 语言包key
     * @param format1     需要转换的参数
     * @param format2     需要转换的参数
     * @return
     */
    public static JSONObject getLanguageJsonObjectByReason(String reason,String languageKey, Object format1, Object format2) {
        String i18noverModCapsCN = I18nUtil.getMessage(languageKey, Locale.SIMPLIFIED_CHINESE);
        String i18noverModCapsUS = I18nUtil.getMessage(languageKey, Locale.US);

        String CN = "";
        String US = "";

        String reasonCN = "";
        String reasonUS = "";
        //reason需要判断是否是json对象  如果是还需要转换一下  否则就不转换了 直接拼接
        if (isJsonObject(reason)){
            JSONObject jsonObject = JSON.parseObject(reason);
            reasonCN = jsonObject.getString(CHINESE);
            reasonUS = jsonObject.getString(ENGLISH);
            CN = reasonCN + String.format(i18noverModCapsCN, format1, format2);
            US = reasonUS + String.format(i18noverModCapsUS, format1, format2);
        }else{
            CN = reason + String.format(i18noverModCapsCN, format1, format2);
            US = reason + String.format(i18noverModCapsUS, format1, format2);
        }

        // 构建 JSON 对象
        JSONObject reasonJson = new JSONObject();
        reasonJson.put(CHINESE, CN);
        reasonJson.put(ENGLISH, US);
        return reasonJson;
    }

    /**
     * 判断字符串是否是有效的 JSON 对象
     *
     * @param str 要判断的字符串
     * @return 如果是有效的 JSON 对象，返回 true；否则返回 false
     */
    public static boolean isJsonObject(String str) {
        if(str == null || str.isEmpty()){
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
     * 解析json备注信息列表
     */
    public static void parseJsonRemarkList(List<? extends BaseEntity> list, String locale, String... fields) {
        if (CollectionUtils.isEmpty(list) || StringUtils.isEmpty(locale) || fields == null || fields.length == 0) {
            return;
        }

        for (BaseEntity item : list) {
            for (String field : fields) {
                String fieldJson = (String) item.getFieldValueByFieldName(field);
                if (StringUtils.isNotBlank(fieldJson)) {
                    item.setFieldValueByFieldName(field, parseJsonRemark(fieldJson, locale));
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
            if (json.startsWith("[")) {
                // 格式：[{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}...]
                JSONArray jsonArray = JSON.parseArray(json);
                for (Object item : jsonArray) {
                    JSONObject jsonItem = (JSONObject) item;
                    builder.append(jsonItem.get(locale));
                    builder.append(",");
                }

            } else if (json.startsWith("{")) {
                // 格式：{"en_US":"...","zh_CN":"..."},{"en_US":"...","zh_CN":"..."}...
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

            return builder.length() > 0 ? builder.substring(0, builder.length() - 1) : json;
        } catch (Exception e) {
            log.error("解析json备注信息失败", e);
        }

        return json;
    }

}
