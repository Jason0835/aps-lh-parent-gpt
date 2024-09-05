package com.zlt.aps.tc.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
public class ApsEngineTcApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineTcApplication.class, args);
    }

}
