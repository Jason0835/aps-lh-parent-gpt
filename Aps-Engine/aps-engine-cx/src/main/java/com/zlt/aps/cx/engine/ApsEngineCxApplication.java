package com.zlt.aps.cx.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
@ComponentScan({"com.zlt.aps.cx.engine.*"})
public class ApsEngineCxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineCxApplication.class, args);
    }

}
