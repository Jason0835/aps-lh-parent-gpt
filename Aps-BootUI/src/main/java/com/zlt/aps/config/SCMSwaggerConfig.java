package com.zlt.aps.config;

import com.ruoyi.common.utils.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
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
public class SCMSwaggerConfig {

    /**
     * 过滤某些不需要提供给前端的API接口
     */
    private static final List<String> excludedPathPrefix = Arrays.asList(
            "/checkUnique",
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
        Contact contact = new Contact("TLT-APS系统", "http://localhost:端口/doc.html", "com.tlt.scm");
        return new ApiInfo(
                "TLT-APS系统",
                "接口API文档",
                "v1.0",
                "http://localhost:端口/doc.html",
                contact,
                "TLT",
                "https://com.tlt.scm",
                new ArrayList<>());
    }



    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForBD() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.cx"))
                .build().groupName("cx").pathMapping("/");
    }

    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForRaw() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.raw"))
                .paths((s) -> {
                    for (String pathPrefix : excludedPathPrefix) {
                        if (StringUtils.endsWith(s, pathPrefix)) {
                            return false;
                        }
                    }
                    return true;
                }).build().groupName("raw").pathMapping("/");
    }

    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForIN() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.lh"))
                .build().groupName("lh").pathMapping("/");
    }

    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForTm() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.tm")
                        .or(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.tc")))
                .paths(PathSelectors.any()).build().groupName("胎面").pathMapping("/");
    }

    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForMonthData() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.maindata"))
                .paths(PathSelectors.any()).build().groupName("主数据").pathMapping("/");
    }

    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForCd15() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.cd15")
                        .or(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.cd90"))
                        .or(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.nc"))
                        .or(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.tq"))
                        .or(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.gsq"))
                )
                .paths(PathSelectors.any()).build().groupName("cd15").pathMapping("/");
    }

    /**
     * 每个新模块就新建立一个Docket
     * 每个模块单独分组
     *
     * @return PathSelectors.any()
     */
    @Bean
    public Docket createRestApiForXwyy() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.xwyy")
                        .or(RequestHandlerSelectors.basePackage("com.zlt.aps.controller.gdyy"))
                )
                .paths(PathSelectors.any()).build().groupName("xwyy").pathMapping("/");
    }
}
