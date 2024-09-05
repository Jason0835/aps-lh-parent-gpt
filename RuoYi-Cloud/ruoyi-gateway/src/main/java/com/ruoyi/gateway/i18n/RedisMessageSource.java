package com.ruoyi.gateway.i18n;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.Nullable;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/***
 * 语言包加载到redis,并从redis读取数据
 * @author linbn 201022
 */
public class RedisMessageSource extends ResourceBundleMessageSource {

    /**
     * 语言包key前缀
     */
    private final static String LANG_KEY_PREFIX = "lang:{}:{}";
    private final static long EXPIRE_TIME = Constants.TOKEN_EXPIRE * 60;

    @Autowired
    RedisService redisService;

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        Set<String> basenames = this.getBasenameSet();
        Iterator var4 = basenames.iterator();

        while (var4.hasNext()) {
            String basename = (String) var4.next();
            ResourceBundle bundle = this.getResourceBundle(basename, locale);
            if (bundle != null) {
                loadBundle2Redis(bundle);
                String result = this.getStringOrNull(bundle, code);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /***
     * 加载数据到redis缓存
     * @author linbn 201022
     * @param bundle
     */
    private void loadBundle2Redis(ResourceBundle bundle) {

        String locale = bundle.getLocale().toString();
        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String value = bundle.getString(key);
            String redisKey = StringUtils.format(LANG_KEY_PREFIX, locale, key);
            redisService.setCacheObject(redisKey, value, EXPIRE_TIME, TimeUnit.SECONDS);
        }
    }

    @Override
    @Nullable
    protected MessageFormat resolveCode(String code, Locale locale) {
        Set<String> basenames = this.getBasenameSet();
        Iterator var4 = basenames.iterator();

        while (var4.hasNext()) {
            String basename = (String) var4.next();
            ResourceBundle bundle = this.getResourceBundle(basename, locale);
            loadBundle2Redis(bundle);
            if (bundle != null) {
                MessageFormat messageFormat = this.getMessageFormat(bundle, code, locale);
                if (messageFormat != null) {
                    return messageFormat;
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    protected String getMessageInternal(@Nullable String code, @Nullable Object[] args, @Nullable Locale locale) {

        String value = getFromRedis(code, locale);
        if (StringUtils.isEmpty(value)) {
            value = super.getMessageInternal(code, args, locale);
        }
        return value;
    }

    /***
     * 从redis取出语言包的字符
     * @param code 字符key
     * @param locale 语言
     * @return 字符
     */
    private String getFromRedis(@Nullable String code, @Nullable Locale locale) {
        String key = StringUtils.format(LANG_KEY_PREFIX, locale.toString(), code);
        return redisService.getCacheObject(key);
    }

    @Override
    @Nullable
    protected String getDefaultMessage(String code) {
        String value = getFromRedis(code, Locale.getDefault());
        if (StringUtils.isEmpty(value)) {
            value = super.getDefaultMessage(code);
        }
        return value;
    }

}
