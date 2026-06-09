package com.zlt.aps.dj.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
public class ApsEngineDjApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineDjApplication.class, args);
    }

}
