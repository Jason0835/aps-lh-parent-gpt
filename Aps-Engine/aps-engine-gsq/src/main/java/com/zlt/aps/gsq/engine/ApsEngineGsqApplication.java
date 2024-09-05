package com.zlt.aps.gsq.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
public class ApsEngineGsqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineGsqApplication.class, args);
    }

}
