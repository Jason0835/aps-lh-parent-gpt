package com.zlt.aps.cx;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;


/**
 * 金宇轮胎APS系统-成型排程模块启动类
 *
 * @author APS Team
 * @version 1.0.0
 */
@EnableAsync
@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@ComponentScan(value = {"com.zlt.*", "com.zlt.aps.**"})
public class ApsCxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsCxApplication.class, args);
        System.out.println("========================================");
        System.out.println("  APS成型排程系统启动成功!");
        System.out.println("  Swagger文档地址: http://localhost:5000/api/doc.html");
        System.out.println("========================================");
    }

}
