package com.zlt.aps.gdyy.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
@ComponentScan({"com.zlt.aps.gdyy.engine.*"})
public class ApsEngineGdyyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineGdyyApplication.class, args);
    }

}
