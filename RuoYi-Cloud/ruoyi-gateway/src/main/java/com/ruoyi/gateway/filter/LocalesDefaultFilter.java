package com.ruoyi.gateway.filter;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TokenUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;

/***
 * 从请求头中过滤出语言，设置到Request中
 */
@Component
public class LocalesDefaultFilter implements GlobalFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
        String lang = TokenUtil.getUserLang(httpHeaders, LocaleContextHolder.getLocale()).toString();
        //如果没有语言的情况下，加默认语言
        ServerHttpRequest mutableReq = exchange.getRequest().mutate()
                .header(CacheConstants.TOKEN_LANG, lang).build();

        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();

        return chain.filter(mutableExchange);
    }

    @Override
    public int getOrder() {
        return -300;
    }
}
