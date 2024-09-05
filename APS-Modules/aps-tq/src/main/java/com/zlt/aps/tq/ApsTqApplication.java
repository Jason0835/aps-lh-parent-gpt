package com.zlt.aps.tq;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@ComponentScan(value={"com.zlt.*"})
public class ApsTqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsTqApplication.class, args);
    }

}
