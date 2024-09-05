package com.zlt.auth;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.config.ApplicationSecurityConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * 认证授权中心
 *
 * @author ruoyi
 */
@EnableRyFeignClients
@SpringBootApplication()
//注入主数据的包，以后独立的话，去除
@ComponentScan(value = {"com.zlt.*", "com.ruoyi.common.core.interceptor"}
)
@MapperScan(value = "com.zlt.**.mapper")
@EnableCustomSwagger2
@Import({ApplicationSecurityConfig.class})
public class LLAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(LLAuthApplication.class, args);
//        System.out.println(I18nUtil.getMessage("ll.auth.startup"));
    }
}
