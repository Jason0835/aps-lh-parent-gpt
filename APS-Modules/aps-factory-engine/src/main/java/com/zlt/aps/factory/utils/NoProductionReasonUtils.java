package com.zlt.aps.factory.utils;

import com.alibaba.fastjson.JSONObject;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.MonthPlanNoProductionReasonEnum;
import com.tlt.aps.utils.JsonUtils;
import com.zlt.aps.common.core.constant.I18nConstant;
import com.zlt.aps.factory.daylimit.MouldProductionLimitTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 不排产原因管理工具类
 * 转化成json格式，支持可多语言切换提取
 *
 * @author ZLT
 * @date 20251211
 */
public class NoProductionReasonUtils {

    /**
     * 根据不排产原因业务，获取不排产原因信息
     *
     * @param noProductionReason 不排产原因业务类型
     * @param params             业务参数
     * @return
     */
    public static String getNoProductionReason(MonthPlanNoProductionReasonEnum noProductionReason, Object... params) {
        String i18nKey = noProductionReason.getI18nKey();
        String errorCode = noProductionReason.getErrorCode();
        List<Object> newParams = new ArrayList<>();
        newParams.add(errorCode);
        if (null != params) {
            for (Object param : params) {
                newParams.add(param);
            }
        }
        return JsonUtils.getLanguageJsonObject(i18nKey, newParams.toArray()).toString();
    }

    /**
     * 根据限制类型，组装复杂的不排产原因
     * 因%s不排 或是 因%s部分未排
     *
     * @param noProductionReason 不排产原因 因%s不排 或是 因%s部分未排
     * @param limitTypeList      限制类型
     * @return
     */
    public static String getNoProductionReasonByLimit(MonthPlanNoProductionReasonEnum noProductionReason, List<MouldProductionLimitTypeEnum> limitTypeList) {
        List<String> languageList = getLanguageList();
        String noProductionReasonI18nKey = noProductionReason.getI18nKey();
        String errorCode = noProductionReason.getErrorCode();
        JSONObject noProductionReasonInfo = JsonUtils.getLanguageJsonObject(noProductionReasonI18nKey);
        if (CollectionUtils.isEmpty(limitTypeList)) {
            //成型或是模具产能不足
            String i18nKey = MonthPlanNoProductionReasonEnum.NO_ENOUGH_PRODUCTION_CAPACITY.getI18nKey();
            JSONObject i18nInfo = JsonUtils.getLanguageJsonObject(i18nKey);
            fullText(noProductionReasonInfo, errorCode, i18nInfo, languageList);
            return noProductionReasonInfo.toString();
        }
        //20260224 取得最后一个限制
        int limitSize = limitTypeList.size();
        MouldProductionLimitTypeEnum lastLimit = limitTypeList.get(limitSize - BigDecimal.ONE.intValue());
        String i18nKey = lastLimit.getI18nKey();
        JSONObject i18nInfo = JsonUtils.getLanguageJsonObject(i18nKey);
        fullText(noProductionReasonInfo, errorCode, i18nInfo, languageList);
        return noProductionReasonInfo.toString();
    }

    /**
     * 从fullI18nInfo中获取对应语言信息补充填充文本信息到noProductionReasonInfo的语言中
     *
     * @param noProductionReasonInfo 需要补充的国际化信息对象
     * @param fullI18nInfo           提供补充信息的国际信息对象
     * @param languageList           国际化语言包
     */
    private static void fullText(JSONObject noProductionReasonInfo, String errorCode, JSONObject fullI18nInfo, List<String> languageList) {
        if (CollectionUtils.isEmpty(languageList)) {
            return;
        }
        if (null == noProductionReasonInfo || null == fullI18nInfo) {
            return;
        }
        languageList.forEach(language -> {
            String infoFormat = noProductionReasonInfo.getString(language);
            if (StringUtils.isBlank(infoFormat)) {
                return;
            }
            String fullInfoText = fullI18nInfo.getString(language);
            if (null == fullInfoText) {
                fullInfoText = "";
            }
            noProductionReasonInfo.put(language, String.format(infoFormat, errorCode, fullInfoText));
        });
    }

    /**
     * 获取所有的补充国际化信息对象
     *
     * @param limitTypeList
     * @return
     */
    private static JSONObject buildAllFullText(List<MouldProductionLimitTypeEnum> limitTypeList, List<String> languageList) {
        JSONObject fullI18nInfo = null;
        for (MouldProductionLimitTypeEnum limitType : limitTypeList) {
            JSONObject nextFullI18nInfo = JsonUtils.getLanguageJsonObject(limitType.getI18nKey());
            if (null == fullI18nInfo) {
                fullI18nInfo = nextFullI18nInfo;
                continue;
            }
            for (String language : languageList) {
                List<String> languageInfo = new ArrayList<>();
                languageInfo.add(fullI18nInfo.getString(language));
                languageInfo.add(nextFullI18nInfo.getString(language));
                fullI18nInfo.put(language, String.join(StringConstant.COMMA, languageInfo));
            }
        }
        return fullI18nInfo;
    }

    /**
     * 获取语言包信息
     *
     * @return
     */
    private static List<String> getLanguageList() {
        List<String> languageList = new ArrayList<>();
        languageList.add(I18nConstant.CHINESE);
        languageList.add(I18nConstant.ENGLISH);
        return languageList;
    }
}
