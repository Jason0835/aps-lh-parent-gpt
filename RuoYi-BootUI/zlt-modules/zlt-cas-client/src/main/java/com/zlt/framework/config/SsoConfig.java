package com.zlt.framework.config;


import io.buji.pac4j.context.ShiroSessionStore;
import lombok.Getter;
import lombok.Setter;
import com.zlt.framework.cas.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.core.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Order(-999)
@ConfigurationProperties(prefix = "shiro.sso")
public class SsoConfig {

    /**
     * 是否开启单点
     */
    private Boolean enable;

    /**
     * cas的地址
     */
    private String cas;
    /**
     * 当前系统cas的地址
     */
    private String currentCas;
    /**
     * 登录的地址
     */
    private String loginUrl;
    /**
     * 退出的地址
     */
    private String logoutUrl;
    /**
     * 首页的地址
     */
    private String indexUrl;
    /**
     * 没有权限的地址
     */
    private String unauthorizedUrl;

    /**
     * 客户端名称
     */
    private String clientName;

    public String callbackUrl() {
        return currentCas + "/callback?client_name=" + clientName;
    }

    /**
     * 自定义存储
     *
     * @return
     */
    public ShiroSessionStore shiroSessionStore() {
        return new ShiroSessionStore();
    }

    /**
     * cas 客户端配置
     *
     * @param casConfig
     * @return
     */
    @Bean
    public CasClient casClient(CasConfiguration casConfig) {
        CasClient casClient = new CasClient(casConfig);
        //客户端回调地址
        casClient.setCallbackUrl(callbackUrl());
        casClient.setName(clientName);
        return casClient;
    }

    /**
     * pac4j配置
     *
     * @param casClient
     * @return
     */
    @Bean
    public Config config(CasClient casClient) {
        Config config = new Config(casClient);
        config.setSessionStore(shiroSessionStore());
        return config;
    }

    /**
     * 请求cas服务端配置
     */
    @Bean
    public CasConfiguration casConfig() {
        final CasConfiguration configuration = new CasConfiguration();
        if (enable) {
            //CAS server登录地址
            configuration.setLoginUrl(cas + "/login");
            //CAS 版本，默认为 CAS30
            configuration.setProtocol(CasProtocol.CAS30);
            configuration.setAcceptAnyProxy(true);
            configuration.setPrefixUrl(cas + "/");
        }
        return configuration;
    }
}
