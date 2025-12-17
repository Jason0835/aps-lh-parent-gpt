package com.zlt.aps.config;

import com.ruoyi.framework.interceptor.FeignInterceptor4UI;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 拦截器配置
 */
@Configuration
@Slf4j
public class ApsFilterConfig {

    @Autowired
    private ShiroFilterFactoryBean shiroFilterFactoryBean;
    @Autowired
    private FeignInterceptor4UI feignInterceptor4UI;

    ApsFilterConfig() {

    }

    /**
     * 补充Feign调用token校验的国际化接口的非token路径
     */
    @PostConstruct
    public void changeNoTokenPath() {
        if (feignInterceptor4UI == null) {
            return;
        }

        try {

            Field noTokenPathField = feignInterceptor4UI.getClass().getDeclaredField("noTokenPath");

            noTokenPathField.setAccessible(true);
            Object noTokenPath = noTokenPathField.get(feignInterceptor4UI);
            if (!(noTokenPath instanceof List)) {
                return;
            }

            List noTokenPathList = (List) noTokenPath;
            noTokenPathList.add("/i18nChange/pageJson");

        } catch (Exception e) {
            log.error("补充Feign调用非token路径出现异常", e);
        }
    }
}
