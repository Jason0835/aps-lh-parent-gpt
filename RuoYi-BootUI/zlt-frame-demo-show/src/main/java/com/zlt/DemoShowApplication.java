package com.zlt;

import com.ruoyi.starter.EnableZLTFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

/**
 * 启动程序
 *
 * @author ruoyi
 */
@EnableZLTFrame
@SpringCloudApplication
public class DemoShowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoShowApplication.class, args);
        System.out.println("Startup is completed!!!");
    }
}
