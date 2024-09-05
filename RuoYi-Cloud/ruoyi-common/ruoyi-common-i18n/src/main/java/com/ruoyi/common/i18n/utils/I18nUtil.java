package com.ruoyi.common.i18n.utils;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.TimeZone;

@Slf4j
public class I18nUtil {
    private static MessageSource messageSource;

    private static RedisService redisService;

    private static Boolean isUI = false;

    private static String localeAttributeName = Constants.LOCALE_SESSION_ATTRIBUTE_NAME;
    private static String timeZoneAttributeName = Constants.TIME_ZONE_SESSION_ATTRIBUTE_NAME;

    /**
     * 构建读取配置对象用于注入
     *
     * @param messageSource
     */
    public I18nUtil(MessageSource messageSource, RedisService redisService, Boolean isUI) {
        I18nUtil.messageSource = messageSource;
        I18nUtil.redisService = redisService;
        I18nUtil.isUI = isUI;

        if (I18nUtil.isUI) {
            localeAttributeName = Constants.UI_SESSION_PREFIX + localeAttributeName;
            timeZoneAttributeName = Constants.UI_SESSION_PREFIX + timeZoneAttributeName;
        }
    }

    /**
     * 获取配置文件中key所对应的国际化语言
     *
     * @param msgKey
     * @return
     */
    public static String getMessage(String msgKey) {
        return getMessage(msgKey, null);
    }

    /**
     * 获取配置文件中key所对应的国际化信息,存在占位符通过args数组进行数据填充
     *
     * @param msgKey
     * @param args
     * @return
     */
    public static String getMessage(String msgKey, Object... args) {
        try {
            return messageSource.getMessage(msgKey, args, getLocaleFromRedis());
        } catch (Exception e) {
            //这里就不再国际化了
            log.error("读取国际化异常：{}", e.getStackTrace()[0].toString());
            return msgKey;
        }
    }

    /**
     * 从redis中获取当前语言
     *
     * @return
     */
    public static Locale getLocaleFromRedis() {
        String token = "";
        Locale locale = null;
        try {
            token = getToken(getRequest());
        } catch (Exception e) {
            log.error("加载token异常：{}", e.getStackTrace()[0].toString());
            return LocaleContextHolder.getLocale();
        }
        String lan = redisService.getCacheObject(localeAttributeName + token);

        if (StringUtils.isNotEmpty(lan)) {
            locale = org.springframework.util.StringUtils.parseLocale(lan);
        }
        //用户缓存没有还是空的时候，从请求头找
        //没有就用默认的浏览器请求
        if (StringUtils.isNull(locale)) {
            locale = TokenUtil.getUserLang(getRequest(), LocaleContextHolder.getLocale());
        }
        return locale;
    }

    /**
     * 从redis中获取当前用户的时区
     *
     * @return
     */
    public static TimeZone getTimezoneFromRedis() {

        String token = "";
        TimeZone timeZone = null;
        try {
            token = getToken(getRequest());
        } catch (Exception e) {
            return LocaleContextHolder.getTimeZone();
        }

        TimeZone zone = redisService.getCacheObject(localeAttributeName + token);
        if (StringUtils.isNotNull(zone)) {
            timeZone = zone;
        } else {
            timeZone = LocaleContextHolder.getTimeZone();//获取浏览器请求
        }
        return timeZone;
    }

    /**
     * 获取请求属性
     *
     * @return
     */
    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }

    /**
     * 获取请求request
     *
     * @return
     */
    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    protected static String getToken(HttpServletRequest request) {
        return isUI ? request.getSession().getId() : TokenUtil.getToken(request);
    }
}
