package com.zlt.common.utils;

import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Setter
@Getter
/***
 * webclient连接，取出验证码
 * @author linbn 20224
 */
@Component
@Configuration
@Slf4j
public class WebClientUtils {

    /**
     * 注入的话，要在getRequest(),重新设置，否则不能把参数带到新的对象
     */
    @Value("${webclient.url}")
    protected String url;

    @Value("${webclient.retryTimes}")
    protected Integer retryTimes;

    protected WebClient client;

    public WebClientUtils getRequest(String url, Integer retryTimes) {
        try {
            WebClient s = WebClient.create();
            WebClientUtils client = new WebClientUtils();
            client.setClient(s);
            client.setUrl(url);
            client.setRetryTimes(retryTimes);
            return client;
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("common.webclient.invoke.kettle.fail"), e);
            throw new RuntimeException(e);
        }
    }

    /***
     * 获得一个网关连接
     * @return
     */
    public WebClientUtils getRequest() {
        return getRequest(url, retryTimes);
    }

    public String invokeGet(String url) {
        return getClient(url, String.class).block();
    }

    public <T> Mono<T> getClient(String uri, Class<T> clazz) {

        return client.get()
                .uri(url + uri)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(new RuntimeException(I18nUtil.getMessage("ui.gateway.server.fail") + clientResponse.statusCode().value() + clientResponse.statusCode().getReasonPhrase())))
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(new RuntimeException(I18nUtil.getMessage("ui.gateway.request.fail") + clientResponse.statusCode().value() + clientResponse.statusCode().getReasonPhrase())))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(new RuntimeException(clientResponse.statusCode().value() + I18nUtil.getMessage("ui.gateway.request.other.fail") + clientResponse.statusCode().getReasonPhrase())))
                .bodyToMono(clazz)
                .doOnError(Throwable.class, err -> {
                    log.error(I18nUtil.getMessage("common.webclient.invoke.kettle.fail"), err);
                })
                .retry(retryTimes)
                ;
    }
}
