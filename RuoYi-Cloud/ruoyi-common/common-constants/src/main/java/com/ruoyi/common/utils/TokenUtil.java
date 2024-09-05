package com.ruoyi.common.utils;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.text.Convert;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class TokenUtil {

    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(CacheConstants.HEADER);
        if (StringUtils.isNotEmpty(token) && token.startsWith(CacheConstants.TOKEN_PREFIX)) {
            token = token.replace(CacheConstants.TOKEN_PREFIX, "");
        }
        return token;
    }

    /**
     * 获取用户语言
     */
    public static Locale getUserLang(HttpServletRequest request, Locale defaultLocale) {
        String lang = Convert.toStr(request.getHeader(CacheConstants.TOKEN_LANG));
        Locale locale = org.springframework.util.StringUtils.parseLocale(lang);
        if (org.springframework.util.StringUtils.isEmpty(locale)) {
            locale = defaultLocale;
        }
        return locale;
    }

    /**
     * 获取用户语言,找不到就用传入的默认语言
     */
    public static Locale getUserLang(HttpHeaders httpHeaders, Locale defaultLocale) {
        String lang = null;
        Locale locale = null;
        if (StringUtils.isNotNull(httpHeaders)) {
            lang = Convert.toStr(httpHeaders.getFirst(CacheConstants.TOKEN_LANG));
            locale = org.springframework.util.StringUtils.parseLocale(lang);
        }
        if (org.springframework.util.StringUtils.isEmpty(locale)) {
            locale = defaultLocale;
        }
        return locale;
    }

    /**
     * 获取用户指定参数
     */
    public static String getUserParam(HttpHeaders httpHeaders, String paramName) {
        String param = null;
        if (StringUtils.isNotNull(httpHeaders)) {
            param = Convert.toStr(httpHeaders.getFirst(paramName));
        }
        return param;
    }

    /**
     * 获取用户指定参数
     */
    public static String getUserParam(HttpServletRequest request, String paramName) {
        String param = null;
        if (StringUtils.isNotNull(request)) {
            param = Convert.toStr(request.getHeader(paramName));
        }
        return param;
    }
}
