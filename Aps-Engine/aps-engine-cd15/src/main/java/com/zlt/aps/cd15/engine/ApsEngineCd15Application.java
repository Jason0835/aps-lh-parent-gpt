package com.zlt.aps.cd15.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
@ComponentScan({"com.zlt.aps.cd15.engine.*"})
public class ApsEngineCd15Application {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineCd15Application.class, args);
    }

}
