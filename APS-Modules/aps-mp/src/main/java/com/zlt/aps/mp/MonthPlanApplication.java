package com.zlt.aps.mp;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * APS-月度计划服务-主启动程序
 *
 * @author ZLT
 * @date 20251201
 */
@Slf4j
@EnableAsync
@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@ComponentScan(value = {"com.zlt.*", "com.zlt.aps.**"})
public class MonthPlanApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(MonthPlanApplication.class, args);
        String ip = InetAddress.getLocalHost().getHostAddress();
        Environment env = application.getEnvironment();
        String port = env.getProperty("server.port");

        log.info("\n----------------------------------------------------------\n\t" +
                "Application MonthPlan is running!\n\t" +
                "APS 月度计划服务文档 URLs:\n\t" +
                "接口文档: \thttp://" + ip + ":" + port + "/swagger-ui/index.html\n\t" +
                "接口文档: \thttp://" + ip + ":" + port + "/doc.html\n" +
                "----------------------------------------------------------\n\t"
        );
    }

}
