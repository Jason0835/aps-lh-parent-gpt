package com.ruoyi.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.api.gateway.system.model.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 用户操作时刷新缓存中的KEY
 * linbn
 */
@Component
public class SessionFilter extends AuthFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        out:
        {
            String token = getToken(exchange.getRequest());
            if (StringUtils.isBlank(token)) {
                break out;
            }
            String userStr = sops.get(getTokenKey(token));
            if (StringUtils.isNull(userStr)) {
                break out;
            }
            LoginUser loginUser = JSONObject.parseObject(userStr, LoginUser.class);
            String userid = loginUser.getUserid().toString();

            frushToken(token, userid, exchange.getRequest().getHeaders());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
