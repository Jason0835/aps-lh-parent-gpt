package com.ruoyi.auth;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.security.config.ApplicationSecurityConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * 认证授权中心
 * 
 * @author ruoyi
 */
@EnableRyFeignClients
@SpringCloudApplication
//注入主数据的包，以后独立的话，去除
@ComponentScan(value = {"com.zlt.*","com.ruoyi.auth.*","com.ruoyi.common.core.interceptor"})
@MapperScan(value = "com.zlt.**.mapper")
@EnableCustomSwagger2
@Import({ApplicationSecurityConfig.class})
public class RuoYiAuthApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RuoYiAuthApplication.class, args);
        System.out.println(I18nUtil.getMessage("auth.msg.startup"));
    }
}
