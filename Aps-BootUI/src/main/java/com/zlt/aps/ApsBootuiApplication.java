package com.zlt.aps;

import com.ruoyi.common.core.interceptor.FeignInterceptor;
import com.ruoyi.common.i18n.configure.LocaleConfig;
import com.ruoyi.starter.EnableZLTFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@ComponentScan(value = {"com.ruoyi","com.zlt","com.tlt"},excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {
        FeignInterceptor.class, LocaleConfig.class
}
))
@EnableFeignClients
@EnableZLTFrame
public class ApsBootuiApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(ApsBootuiApplication.class, args);
        String ip = InetAddress.getLocalHost().getHostAddress();
        Environment env = application.getEnvironment();
        String port = env.getProperty("server.port");
        log.info("\n----------------------------------------------------------\n\t" +
                "Application APS-BootUI is running!\n\t" +
                "特立通技术平台APS接口文档 URLs:\n\t" +
                "APS-UI接口文档1: \thttp://" + ip + ":" + port + "/swagger-ui/index.html?group=APS\n\t" +
                "----------------------------------------------------------\n\t"
        );
    }
}
