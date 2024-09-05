package com.ruoyi.gateway.i18n;

import com.ruoyi.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

/**
 * 国际化配置
 */
@Configuration
@EnableCaching
public class LocaleConfig {


    @Autowired
    private RedisService redisService;

    /**
     * 配置需要进行加载的配置文件
     */
    @Value("${i18n.basename}")
    private String basenames;

    /**
     * 添加注入配置
     *
     * @return
     */
    @Bean
    public I18nUtil i18nUtil() {
        return new I18nUtil(messageSource(), redisService);
    }

    /**
     * 设置配置国际化配置文件所在位置
     *
     * @return
     */
    @Bean
    public ResourceBundleMessageSource messageSource() {

        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        RedisMessageSource source = new RedisMessageSource();
        source.setBasenames(basenames);
        // name of the resource bundle
        // source.setUseCodeAsDefaultMessage(true);
        source.setDefaultEncoding("UTF-8");
        return source;
    }

}
