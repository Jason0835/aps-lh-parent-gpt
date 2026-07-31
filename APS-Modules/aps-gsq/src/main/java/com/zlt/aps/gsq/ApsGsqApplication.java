package com.zlt.aps.gsq;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@EnableAsync
@ComponentScan(value={"com.zlt.*"})
public class ApsGsqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsGsqApplication.class, args);
    }

}
