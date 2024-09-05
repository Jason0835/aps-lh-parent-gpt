package com.ruoyi.framework.interceptor.impl;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.framework.interceptor.UserParamsInterceptor;
import org.springframework.stereotype.Component;

@Component
public class FactoryParamInterceptor extends UserParamsInterceptor {

    public FactoryParamInterceptor(){
        paramName = CacheConstants.TOKEN_FACTORY;
    }
}
