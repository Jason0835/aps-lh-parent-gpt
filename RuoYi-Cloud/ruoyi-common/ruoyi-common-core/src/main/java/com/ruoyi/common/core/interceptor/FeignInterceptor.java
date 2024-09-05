package com.ruoyi.common.core.interceptor;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TokenUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@Configuration
@Slf4j
public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        log.debug("requestUrl:{}", requestTemplate.url());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest currentRequest = attributes.getRequest();

        String token = TokenUtil.getUserParam(currentRequest, CacheConstants.HEADER);
        Locale locale = TokenUtil.getUserLang(currentRequest, Locale.getDefault());
        String factory = TokenUtil.getUserParam(currentRequest, CacheConstants.TOKEN_FACTORY);

        //重要：重新把当前请求中的语言、工厂、token放入feign,让下一个调用点，可以取到数据。
        addHeader(CacheConstants.HEADER, token, requestTemplate);
        //接口加入默认的语言，主要给gateway使用，其它接口从用户token里面取值
        addHeader(CacheConstants.TOKEN_LANG, locale.toString(), requestTemplate);
        //加入用户选择工厂的头
        addHeader(CacheConstants.TOKEN_FACTORY, factory, requestTemplate);

        log.debug("feign interceptor header:{}", requestTemplate.headers());
    }

    protected void addHeader(String header, String value, RequestTemplate requestTemplate) {
        if (StringUtils.isNotNull(value)) {
            requestTemplate.header(header, value);
        }
    }


}
