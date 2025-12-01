package com.zlt.aps.itf.Interceptor;

import com.zlt.aps.itf.filter.RequestBodyReserveFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Token 拦截器配置
 */
@Configuration
public class TokenAuthConfiguration implements WebMvcConfigurer {
    @Value("${itf.token.Urls}")
    String authUrls;

    /**
     * 初始需要拦截的配置
     * @param authUrls
     */
    public static String[] AUTH_URL_LIST = new String[]{
            "/api/token/*",
            "/aps/openApi/monthplan/*"
    };

    @Bean
    public TokenAuthInterceptor signAuthInterceptor() {
        return new TokenAuthInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //------------------------------------------------------------
        //查询需要进行签名拦截的接口 signUrls
        String[] authUrlsArray = null;
        if (StringUtils.isNotBlank(authUrls)) {
            authUrlsArray = StringUtils.split(",");
        } else {
            authUrlsArray = AUTH_URL_LIST;
        }
        //------------------------------------------------------------
        registry.addInterceptor(signAuthInterceptor()).addPathPatterns(authUrlsArray);
    }

    //2024-02-26 Nick+ 可配置路径

    @Bean
    public RequestBodyReserveFilter requestBodyReserveFilter(){
        return new RequestBodyReserveFilter();
    }

    @Bean
    public FilterRegistrationBean reqBodyFilterRegistrationBean(){
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(requestBodyReserveFilter());
        registration.setName("requestBodyReserveFilter");
        //------------------------------------------------------------
        //查询需要进行认证拦截的接口 authUrls
        String signUrls = authUrls;
        String[] signUrlsArray = null;
        if (StringUtils.isNotBlank(signUrls)) {
            signUrlsArray = signUrls.split(",");
        } else {
            signUrlsArray = AUTH_URL_LIST;
        }
        //------------------------------------------------------------
        // 建议此处只添加post请求地址而不是所有的都需要走过滤器
        registration.addUrlPatterns(signUrlsArray);
        return registration;
    }

}
