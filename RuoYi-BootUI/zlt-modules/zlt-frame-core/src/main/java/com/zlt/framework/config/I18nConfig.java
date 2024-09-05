package com.zlt.framework.config;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.i18n.interceptor.I18nLocaleChangeInterceptor;
import com.ruoyi.common.i18n.messagesource.RedisMessageSource;
import com.ruoyi.common.i18n.resolver.RedisLocaleResolver;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Locale;

/**
 * 资源文件配置加载
 *
 * @author ruoyi
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 配置需要进行加载的配置文件
     */
    @Value("#{'${spring.messages.basename}'.split(',')}")
    private List<String> basenames;

    @Autowired
    private RedisService redisService;

    /**
     * 默认解析器 其中locale表示默认语言
     */
    @Bean
    public LocaleResolver localeResolver() {
        RedisLocaleResolver localeResolver = new RedisLocaleResolver(true);
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }


    /* *
     * 默认拦截器 其中lang表示切换语言的参数名*/
    @Bean
    public WebMvcConfigurer localeInterceptor() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                I18nLocaleChangeInterceptor localeInterceptor = new I18nLocaleChangeInterceptor(localeResolver());
                localeInterceptor.setParamName(CacheConstants.TOKEN_LANG);
                registry.addInterceptor(localeInterceptor);
            }
        };
    }

    /**
     * 设置配置国际化配置文件所在位置
     *
     * @return
     */
    @Bean
    public RedisMessageSource messageSource() {
        // Locale.setDefault(Locale.CHINESE);
        RedisMessageSource source = new RedisMessageSource(RedisMessageSource.LANG_KEY_PREFIX);

        source.setBasenames(basenames.toArray(new String[basenames.size()]));
        // name of the resource bundle
        // source.setUseCodeAsDefaultMessage(true);
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    /**
     * 添加注入配置
     *
     * @return
     */
    @Bean
    public I18nUtil i18nUtil() {
        return new I18nUtil(messageSource(),redisService, true);
    }
}