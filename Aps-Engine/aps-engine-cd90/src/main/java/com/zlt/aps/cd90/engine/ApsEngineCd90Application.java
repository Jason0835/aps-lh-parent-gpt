package com.zlt.aps.cd90.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
@ComponentScan({"com.zlt.aps.cd90.engine.*"})
public class ApsEngineCd90Application {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineCd90Application.class, args);
    }

}
