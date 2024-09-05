package com.zlt.aps.nc.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
public class ApsEngineNcApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineNcApplication.class, args);
    }

}
