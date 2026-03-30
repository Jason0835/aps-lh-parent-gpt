package com.zlt.aps.cx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;

/**
 * 接口文档配置
 * <p>
 * 每新增一个模块，需要新增一个配置组
 * 每个模块单独分组
 * </p>
 * @author Nick
 */
@Configuration
public class SwaggerConfig {

    /**
     * 通用的文档信息
     *
     * @return ApiInfo
     */
    private ApiInfo apiInfo() {
        Contact contact = new Contact(
                "金宇 APS 成型",
                "http://localhost:端口/doc.html",
                "com.tlt.aps"
        );
        return new ApiInfo(
                "金宇 APS 成型硫化",
                "接口 API 文档",
                "v1.0",
                "http://localhost:端口/doc.html",
                contact,
                "TLT",
                "https://com.tlt.aps",
                new ArrayList<>()
        );
    }

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.cx"))
                .build()
                .groupName("APS-CX-MODULES");
    }
}
