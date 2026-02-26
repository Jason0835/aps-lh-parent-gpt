package com.zlt.aps.autoLogin.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.Map;

/**
 * 动态Header拦截器
 * @author zhangxh
 * @date 20250506
 * @description 动态Header拦截器
 */
public class DynamicHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        Map<String, String> headers = FeignRequestContext.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(template::header);
        }
    }
}