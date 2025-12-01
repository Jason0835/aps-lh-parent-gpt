package com.ruoyi.file;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.context.annotation.ComponentScan;

/**
 * 文件服务
 * 
 * @author ruoyi
 */
@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@ComponentScan(value={"com.ruoyi.*","com.zlt.*"})
public class ApsFileApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ApsFileApplication.class, args);
        System.out.println(I18nUtil.getMessage("file.msg.startup"));
    }
}
