package com.ruoyi.common.i18n.messagesource;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RedisMessageSource extends ResourceBundleMessageSource {

    RedisMessageSource() {
    }

    public RedisMessageSource(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * 语言包key前缀
     */
    public final static String LANG_KEY_PREFIX = "lang:{}:{}";
    public final static String LANG_UI_KEY_PREFIX = "lang:ui:{}:{}";
    private final static long EXPIRE_TIME = Constants.TOKEN_EXPIRE * 60;

    private String keyPrefix;

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
            String redisKey = StringUtils.format(keyPrefix, locale, key);
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
            log.error("缓存没有找到语言数据{}:{}", locale.toString(), code);
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
        String key = StringUtils.format(keyPrefix, locale.toString(), code);
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
