package com.ruoyi.gateway.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.gateway.i18n.I18nUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import reactor.core.publisher.Mono;

/**
 * 黑名单过滤器
 * 
 * @author ruoyi
 */
@Component
public class BlackListUrlFilter extends AbstractGatewayFilterFactory<BlackListUrlFilter.Config>
{
    @Override
    public GatewayFilter apply(Config config)
    {
        return (exchange, chain) -> {

            String url = exchange.getRequest().getURI().getPath();
            HttpHeaders headers = exchange.getRequest().getHeaders();
            if (config.matchBlacklist(url))
            {
                ServerHttpResponse response = exchange.getResponse();

                //201026,LINBN 不引用ajaxResult,直接返回数据。循环引用包问题。
                HashMap<String, Object> codeResult = new HashMap();
                codeResult.put(Constants.CODE, HttpStatus.ERROR);
                codeResult.put(GatewayConstants.MSG_TAG, I18nUtil.getMessage("gateway.error.server.down",headers));

                return exchange.getResponse().writeWith(
                        Mono.just(response.bufferFactory().wrap(JSON.toJSONBytes(codeResult))));
            }

            return chain.filter(exchange);
        };
    }

    public BlackListUrlFilter()
    {
        super(Config.class);
    }

    public static class Config
    {
        private List<String> blacklistUrl;

        private List<Pattern> blacklistUrlPattern = new ArrayList<>();

        public boolean matchBlacklist(String url)
        {
            return blacklistUrlPattern.isEmpty() ? false : blacklistUrlPattern.stream().filter(p -> p.matcher(url).find()).findAny().isPresent();
        }

        public List<String> getBlacklistUrl()
        {
            return blacklistUrl;
        }

        public void setBlacklistUrl(List<String> blacklistUrl)
        {
            this.blacklistUrl = blacklistUrl;
            this.blacklistUrlPattern.clear();
            this.blacklistUrl.forEach(url -> {
                this.blacklistUrlPattern.add(Pattern.compile(url.replaceAll("\\*\\*", "(.*?)"), Pattern.CASE_INSENSITIVE));
            });
        }
    }

}
