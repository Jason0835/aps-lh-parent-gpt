package com.zlt.aps.tq.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
public class ApsEngineTqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineTqApplication.class, args);
    }

}
