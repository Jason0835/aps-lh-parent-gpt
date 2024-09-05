package com.zlt.framework.monoclient;

import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.system.api.domain.SessionBody;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Setter
@Getter
/***
 * 网关的连接，取出验证码
 * @author linbn 201104
 */
@Component("gatewayWebClient")
@Configuration
@Slf4j
public class GatewayWebClient {

    /**
     * 注入的话，要在getRequest(),重新设置，否则不能把参数带到新的对象
     */
    @Value("${gateway.url}")
    protected String url;

    @Value("${gateway.retryTimes}")
    protected Integer retryTimes;

    protected WebClient client;

    /***
     * 获得一个网关连接
     * @return
     */
    public GatewayWebClient getRequest() {
        try {
            WebClient s = WebClient.create();
            GatewayWebClient client = new GatewayWebClient();
            client.setClient(s);
            client.setUrl(url);
            client.setRetryTimes(retryTimes);
            return client;
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("gateway.get.fail"), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 从网关读取一个验证码json报文转成MAP对象
     *
     * @return
     */
    public HashMap getCodeJson() {
        try {
            Mono<HashMap> mono = getClient(GatewayConstants.CODE_URI, HashMap.class);
            return mono.block();
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("gateway.get.fail"), e);
            return new HashMap();
        }
    }

    public HashMap getSessionVaild() {
        try {
            Mono<HashMap> mono = getClient(GatewayConstants.SESSION_CHECK_URI, HashMap.class);
            return mono.block();
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("gateway.get.fail"), e);
            return new HashMap();
        }
    }

    public List<SessionBody> getSessionKeys() {
        try {
            Mono<List> mono = getClient(GatewayConstants.ALIVE_SESSION_KEY, List.class);
            return mono.block();
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("gateway.get.fail"), e);
            return new ArrayList<>();
        }
    }

    protected  <T> Mono<T> getClient(String uri, Class<T> clazz) {

        return client.get()
                .uri(url + uri)
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(new RuntimeException(I18nUtil.getMessage("ui.gateway.server.fail") + clientResponse.statusCode().value() + clientResponse.statusCode().getReasonPhrase())))
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(new RuntimeException(I18nUtil.getMessage("ui.gateway.request.fail") + clientResponse.statusCode().value() + clientResponse.statusCode().getReasonPhrase())))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(new RuntimeException(clientResponse.statusCode().value() + I18nUtil.getMessage("ui.gateway.request.other.fail") + clientResponse.statusCode().getReasonPhrase())))
                .bodyToMono(clazz)
                .doOnError(Throwable.class, err -> {
                    log.error(I18nUtil.getMessage("gateway.get.fail"), err);
                })
                .retry(retryTimes)
                ;
    }

}
