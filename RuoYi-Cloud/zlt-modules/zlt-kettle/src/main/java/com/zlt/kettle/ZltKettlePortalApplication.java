package com.zlt.kettle;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

@EnableCustomConfig
@SpringCloudApplication
@EnableCustomSwagger2
public class ZltKettlePortalApplication {

    public static void main(String[] args)
    {
        SpringApplication.run(ZltKettlePortalApplication.class, args);
        System.out.println(I18nUtil.getMessage("kettle.msg.startup"));

    }
}
