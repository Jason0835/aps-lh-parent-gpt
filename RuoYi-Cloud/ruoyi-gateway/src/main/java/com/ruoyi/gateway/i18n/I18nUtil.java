package com.ruoyi.gateway.i18n;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Locale;

/***
 * @author lbn
 */
@Slf4j
@Component
public class I18nUtil {

    private static MessageSource messageSource;

    private static RedisService redisService;

    /**
     * 国际化语言前缀
     */
    private static String localeAttributeName = Constants.LOCALE_SESSION_ATTRIBUTE_NAME;


    /**
     * 构建读取配置对象用于注入
     *
     * @param messageSource
     */
    public I18nUtil(MessageSource messageSource, RedisService redisService) {
        I18nUtil.messageSource = messageSource;
        I18nUtil.redisService = redisService;
    }

    /**
     * 获取配置文件中key所对应的国际化语言
     *
     * @param msgKey
     * @return
     */
    public static String getMessage(String msgKey, HttpHeaders headers) {

        try {
            return messageSource.getMessage(msgKey, null, getLocaleFromHeaders(headers));
        } catch (Exception e) {
            //这里就不再国际化了
            log.error("读取国际化异常：", e);
            return msgKey;
        }

    }

    /**
     * 从redis中获取当前语言
     *
     * @return
     */
    public static Locale getLocaleFromHeaders(HttpHeaders headers) {

        Locale locale = null;
        //用户缓存没有还是空的时候，从请求头找
        //没有就用默认的浏览器请求
        locale = TokenUtil.getUserLang(headers, LocaleContextHolder.getLocale());

        return locale;
    }
}
