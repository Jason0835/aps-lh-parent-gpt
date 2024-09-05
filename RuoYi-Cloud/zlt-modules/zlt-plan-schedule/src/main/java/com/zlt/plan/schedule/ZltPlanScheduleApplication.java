package com.zlt.plan.schedule;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

/**
 * 主计划启动
 */
@EnableCustomConfig
@SpringCloudApplication
@EnableCustomSwagger2
public class ZltPlanScheduleApplication {

    public static void main(String[] args)
    {
        SpringApplication.run(ZltPlanScheduleApplication.class, args);
        System.out.println(I18nUtil.getMessage("planschedule.msg.startup"));
    }
}
