package com.ruoyi.framework.config;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
class FeignConfiguration implements RequestInterceptor {

    /***
     * 拦截feign请求，如果session存在token的时候，加上access_token头
     * @param requestTemplate
     */
    @Override
    public void apply(RequestTemplate requestTemplate) {
        log.debug("requestUrl:{}", requestTemplate.url());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Object token = AuthorizationUtils.getAccessToken();

        String strToken = token == null ? "" : token.toString();

        //加入新的header，最好还是照搬一份过去，否则header会不全。
        if (null != attributes
                && !StringUtils.isEmpty(strToken)) {
            addHeader(CacheConstants.HEADER, CacheConstants.TOKEN_PREFIX + strToken, requestTemplate);
            setPageDomain(attributes, requestTemplate);
        }
        //接口加入默认的语言，主要给gateway使用，其它接口从用户token里面取值
        addHeader(CacheConstants.TOKEN_LANG, AuthorizationUtils.getLang(), requestTemplate);
        //加入用户选择工厂的头
        addHeader(CacheConstants.TOKEN_FACTORY, AuthorizationUtils.getFactory(), requestTemplate);

        log.debug("feign interceptor header:{}", requestTemplate.headers());
    }

    private void setPageDomain(ServletRequestAttributes attributes, RequestTemplate requestTemplate) {
        HttpServletRequest httpServletRequest = attributes.getRequest();
        addHeader(TableSupport.PAGE_NUM, httpServletRequest.getParameter(TableSupport.PAGE_NUM), requestTemplate);
        addHeader(TableSupport.PAGE_SIZE, httpServletRequest.getParameter(TableSupport.PAGE_SIZE), requestTemplate);
        addHeader(TableSupport.ORDER_BY_COLUMN, httpServletRequest.getParameter(TableSupport.ORDER_BY_COLUMN), requestTemplate);
        addHeader(TableSupport.IS_ASC, httpServletRequest.getParameter(TableSupport.IS_ASC), requestTemplate);

        log.debug(I18nUtil.getMessage("ui.feign.pageHeader.writeHeader"));
    }


    protected void addHeader(String header, String value, RequestTemplate requestTemplate) {
        if (com.ruoyi.common.utils.StringUtils.isNotNull(value)) {
            requestTemplate.header(header, value);
        }
    }
}
