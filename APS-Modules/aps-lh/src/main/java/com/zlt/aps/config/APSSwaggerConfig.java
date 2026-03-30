package com.zlt.aps.config;

import com.ruoyi.common.utils.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 接口文档配置
 * 每新增一个模块，需要在这里新增一个配置组
 * 每个模块单独分组
 */
@Configuration
public class APSSwaggerConfig {

    /**
     * 过滤某些不需要提供给前端的API接口
     */
    private static final List<String> excludedPathPrefix = Arrays.asList(
            "/importTemplate",
            "/add",
            "/edit/{id}",
            "/exportAsync"
    );

    /**
     * 过滤某些不需要提供给前端的API接口
     */
    private static final List<String> excludedOutPathPrefix = Arrays.asList(
            "/importTemplate",
            "/add",
            "/edit/{id}",
            "/exportAsync"
    );

    /**
     * 通用的文档信息
     *
     * @return
     */
    private ApiInfo apiInfo() {
        //作者信息
        Contact contact = new Contact("APS硫化", "http://localhost:端口/doc.html", "com.tlt.aps");
        return new ApiInfo(
                "金宇APS硫化",
                "接口API文档",
                "v1.0",
                "http://localhost:端口/doc.html",
                contact,
                "",
                "",
                new ArrayList<>());
    }

    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForLh() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.lh.controller"))
                .paths((s) -> {
                    for (String pathPrefix : excludedPathPrefix) {
                        if (StringUtils.endsWith(s, pathPrefix)) {
                            return false;
                        }
                    }
                    return true;
                }).build().groupName("APS-LH").pathMapping("/");
    }

}
