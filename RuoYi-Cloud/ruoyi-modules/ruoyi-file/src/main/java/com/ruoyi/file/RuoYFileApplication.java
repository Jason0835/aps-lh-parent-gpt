package com.ruoyi.file;

import com.ruoyi.common.i18n.utils.I18nUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 文件服务
 * 
 * @author ruoyi
 */
@EnableCustomSwagger2
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class RuoYFileApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RuoYFileApplication.class, args);
        System.out.println(I18nUtil.getMessage("file.msg.startup"));
    }
}
