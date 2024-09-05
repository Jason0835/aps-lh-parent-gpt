package com.ruoyi.common.i18n.resolver;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AbstractLocaleContextResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * redis的I18n处理器
 */

@Slf4j
public class RedisLocaleResolver extends AbstractLocaleContextResolver {

    @Autowired
    private RedisService redisService;

    private long tokenExpiresAfter = Constants.TOKEN_EXPIRE * 60;

    private String localeAttributeName = Constants.LOCALE_SESSION_ATTRIBUTE_NAME;
    private String timeZoneAttributeName = Constants.TIME_ZONE_SESSION_ATTRIBUTE_NAME;

    private Boolean isUI = false;

    RedisLocaleResolver() {
    }

    public RedisLocaleResolver(Boolean isUI) {
        this.isUI = isUI;
        if (isUI) {
            localeAttributeName = Constants.UI_SESSION_PREFIX + localeAttributeName;
            timeZoneAttributeName = Constants.UI_SESSION_PREFIX + timeZoneAttributeName;
        }
    }

    public void setLocaleAttributeName(String localeAttributeName) {
        this.localeAttributeName = localeAttributeName;
    }

    public void setTimeZoneAttributeName(String timeZoneAttributeName) {
        this.timeZoneAttributeName = timeZoneAttributeName;
    }

    private Locale getRedisLocale(HttpServletRequest request) {

        // 从 request 中获取 token
        String token = getToken(request);;
        Locale locale = null;
        try {
            String language = redisService.getCacheObject(this.localeAttributeName + token);
            if (!StringUtils.isEmpty(language)) {
                String[] array = language.split("_");
                locale = new Locale(array[0], array[1]);
            }
        } catch (Exception ex) {
            //log.error("没找到缓存TOKEN中的语言包设置:{}", ex.getMessage());
            String errorMsg = com.ruoyi.common.utils.StringUtils.format(I18nUtil.getMessage("common.error.i18n.locale.cache.token.nolocale"), ex.getMessage());
            log.error(errorMsg);
        }
        return locale;
    }

    private TimeZone getRedisTimeZone(HttpServletRequest request) {
        // 从 request 中获取 token
        String token = getToken(request);
        TimeZone timeZone = null;

        try {
            String timeZoneStr = redisService.getCacheObject(this.timeZoneAttributeName + token);
            if (!StringUtils.isEmpty(timeZoneStr)) {
                timeZone = TimeZone.getTimeZone(timeZoneStr);
            }
        } catch (Exception ex) {
            //log.error("没找到缓存TOKEN中的语言包时区设置:{}", ex.getMessage());
            String errorMsg = com.ruoyi.common.utils.StringUtils.format(I18nUtil.getMessage("common.error.i18n.locale.cache.token.notimezone"), ex.getMessage());
            log.error(errorMsg);
        }
        return timeZone;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = getRedisLocale(request);
        if (locale == null) {
            locale = determineDefaultLocale(request);
        }
        return locale;
    }

    @Override
    public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
        return new TimeZoneAwareLocaleContext() {
            @Override
            public Locale getLocale() {
                return resolveLocale(request);
            }

            @Override
            @Nullable
            public TimeZone getTimeZone() {
                TimeZone timeZone = getRedisTimeZone(request);
                if (timeZone == null) {
                    timeZone = determineDefaultTimeZone(request);
                }
                return timeZone;
            }
        };
    }

    /**
     * 设置 Locale
     *
     * @return {@link }
     */
    @Override
    public void setLocaleContext(HttpServletRequest request,
                                 @Nullable HttpServletResponse response,
                                 @Nullable LocaleContext localeContext) {
        Locale locale = null;
        TimeZone timeZone = null;
        if (localeContext != null) {
            locale = localeContext.getLocale();
            if (localeContext instanceof TimeZoneAwareLocaleContext) {
                timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
            }
        }

        timeZone = timeZone == null ? TimeZone.getDefault() : timeZone;

        String token = getToken(request);
        redisService.setCacheObject(this.localeAttributeName + token
                , locale.toString(), tokenExpiresAfter, TimeUnit.SECONDS);
        redisService.setCacheObject(this.timeZoneAttributeName + token
                , timeZone.getID(), tokenExpiresAfter, TimeUnit.SECONDS);

    }

    /**
     * 如果设置了 Locale，则用设置的，否则使用请求头中的 Locale
     *
     * @param request the request to resolve the locale for
     * @return the default locale (never {@code null})
     * @see #setDefaultLocale
     * @see javax.servlet.http.HttpServletRequest#getLocale()
     */
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        Locale defaultLocale = getDefaultLocale();
        if (defaultLocale == null) {
            defaultLocale = request.getLocale();
        }
        return defaultLocale;
    }

    /**
     * 请求的默认时区，如果未找到时区会话属性，则使用默认时区。
     *
     * @param request
     * @return
     */
    @Nullable
    protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
        return getDefaultTimeZone();
    }

    protected String getToken(HttpServletRequest request) {
        return isUI ? request.getSession().getId() : TokenUtil.getToken(request);
    }
}
