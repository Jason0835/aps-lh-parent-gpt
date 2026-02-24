package com.zlt.aps.itf;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

@Slf4j
@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@ComponentScan(value = {"com.zlt.*", "com.zlt.aps.**"})
public class ApsItfApplication {
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext application = SpringApplication.run(ApsItfApplication.class, args);
        String ip = InetAddress.getLocalHost().getHostAddress();
        Environment env = application.getEnvironment();
        String port = env.getProperty("server.port");
        log.info("\n----------------------------------------------------------\n\t" +
                "Application APS-ITF is running!\n\t" +
                "接口服务文档: \thttp://" + ip + ":" + port + "/doc.html\n" +
                "----------------------------------------------------------\n\t"
        );
    }
}