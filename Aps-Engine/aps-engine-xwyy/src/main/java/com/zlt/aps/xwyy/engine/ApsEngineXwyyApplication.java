package com.zlt.aps.xwyy.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
@ComponentScan({"com.zlt.aps.xwyy.engine.*"})
public class ApsEngineXwyyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineXwyyApplication.class, args);
    }

}
