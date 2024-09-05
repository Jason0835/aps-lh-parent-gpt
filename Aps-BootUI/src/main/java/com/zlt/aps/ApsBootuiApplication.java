package com.zlt.aps;

import com.ruoyi.starter.EnableZLTFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@EnableZLTFrame
@SpringBootApplication
public class ApsBootuiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsBootuiApplication.class, args);
    }

//    @PostConstruct
//    void setDefaultTimezone(){
//        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
//    }
}
