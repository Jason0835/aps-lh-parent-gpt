package com.zlt.aps.lh.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.zlt.**.mapper")
@SpringBootApplication
@ComponentScan({"com.zlt.aps.lh.engine.*"})
public class ApsEngineLhApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsEngineLhApplication.class, args);
    }

}
