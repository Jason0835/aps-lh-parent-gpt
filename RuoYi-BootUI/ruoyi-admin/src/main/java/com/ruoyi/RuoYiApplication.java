package com.ruoyi;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.core.interceptor.FeignInterceptor;
import com.ruoyi.common.i18n.configure.LocaleConfig;
import com.ruoyi.starter.EnableZLTFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 启动程序
 *
 * @author ruoyi
 */
@EnableZLTFrame
@SpringCloudApplication
public class RuoYiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuoYiApplication.class, args);
        System.out.println("Startup is completed!!!");
    }
}