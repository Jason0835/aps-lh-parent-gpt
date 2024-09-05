package com.ruoyi.modules.monitor;

import com.ruoyi.common.i18n.utils.I18nUtil;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

/**
 * 监控中心
 * 
 * @author ruoyi
 */
@EnableAdminServer
@SpringCloudApplication
public class RuoYiMonitorApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RuoYiMonitorApplication.class, args);
        System.out.println(I18nUtil.getMessage("monitor.msg.startup"));
    }
}
