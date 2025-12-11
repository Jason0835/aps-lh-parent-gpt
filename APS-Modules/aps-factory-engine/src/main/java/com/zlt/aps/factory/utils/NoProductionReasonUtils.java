package com.zlt.aps.factory.utils;

import com.tlt.aps.enums.MonthPlanNoProductionReasonEnum;
import com.tlt.aps.utils.JsonUtils;

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
}
