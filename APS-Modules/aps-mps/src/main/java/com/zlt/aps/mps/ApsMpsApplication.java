package com.zlt.aps.mps;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@ComponentScan(value={"com.zlt.*"})
public class ApsMpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsMpsApplication.class, args);
        System.out.println("mps启动成功");
    }
}
