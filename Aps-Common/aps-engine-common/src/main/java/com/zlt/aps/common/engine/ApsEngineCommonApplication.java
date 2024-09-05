package com.zlt.aps.common.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
public class ApsEngineCommonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineCommonApplication.class, args);
    }

}
