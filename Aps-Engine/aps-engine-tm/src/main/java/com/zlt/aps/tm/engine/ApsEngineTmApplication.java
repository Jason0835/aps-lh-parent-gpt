package com.zlt.aps.tm.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
public class ApsEngineTmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineTmApplication.class, args);
    }

}
