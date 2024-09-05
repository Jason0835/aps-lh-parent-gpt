package com.zlt.framework.monoclient;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.system.api.domain.SessionBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component("casWebClient")
@Configuration
@Slf4j
public class CasWebClient extends GatewayWebClient {

    @Value("${gateway.casUrl}")
    private String casUrl;

    /***
     * 获得一个网关连接
     * @return
     */
    public CasWebClient getRequest() {
        try {
            WebClient s = WebClient.create();
            CasWebClient client = new CasWebClient();
            client.setClient(s);
            client.setUrl(casUrl);
            client.setRetryTimes(retryTimes);
            return client;
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("gateway.get.fail"), e);
            throw new RuntimeException(e);
        }
    }

    public HashMap getCasSessionVaild(String uri) {
        try {
            Mono<HashMap> mono = getClient(uri, HashMap.class);
            return mono.block();
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("cas.get.fail"), e);
            return new HashMap();
        }
    }

    public List<SessionBody> getCasSessionKeys(String uri) {
        try {
            Mono<List> mono = getClient(uri, List.class);
            return mono.block();
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("cas.get.fail"), e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除CASsession
     * @param uri
     * @return
     */
    public HashMap deleteCasSession(String uri) {
        try {
            Mono<HashMap> mono = getClient(uri, HashMap.class);
            return mono.block();
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("cas.get.fail"), e);
            return new HashMap();
        }
    }
}
