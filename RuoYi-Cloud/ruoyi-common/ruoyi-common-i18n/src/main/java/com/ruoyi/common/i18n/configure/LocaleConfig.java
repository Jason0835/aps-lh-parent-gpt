package com.ruoyi.common.i18n.configure;

import com.ruoyi.common.i18n.interceptor.I18nLocaleChangeInterceptor;
import com.ruoyi.common.i18n.messagesource.RedisMessageSource;
import com.ruoyi.common.i18n.resolver.RedisLocaleResolver;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.List;
import java.util.Locale;

/**
 * 国际化配置
 */
@Configuration
@EnableCaching
public class LocaleConfig {

    /**
     * 配置需要进行加载的配置文件
     */
    @Value("#{'${i18n_msg.baseName}'.split(',')}")
    private List<String> basenames;

    @Autowired
    private RedisService redisService;

      /**
     * 添加注入配置
     *
     * @return
     */
    @Bean
    public I18nUtil i18nUtil() {
        return new I18nUtil(messageSource(), redisService, false);
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
     * 验证标签
     *
     * @return
     * @throws Exception
     */
    @Bean
    public Validator getValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource());
        return validator;
    }


    /**
     * 默认解析器 其中locale表示默认语言
     */
    @Bean(name = "langLocaleResolver")
    public LocaleResolver localeResolver() {
        RedisLocaleResolver localeResolver = new RedisLocaleResolver(false);
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }

    /**
     * 默认拦截器 其中lang表示切换语言的参数名
     * */
    @Bean
    public WebMvcConfigurer localeInterceptor() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                I18nLocaleChangeInterceptor localeInterceptor = new I18nLocaleChangeInterceptor(localeResolver());
                localeInterceptor.setParamName("lang");
                registry.addInterceptor(localeInterceptor);
            }
        };
    }

   /* @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    *//*国际化 start*//*
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        //自定义参数
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }*/

}
