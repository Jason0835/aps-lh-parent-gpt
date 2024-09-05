package com.ruoyi.system;

import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.impl.SysConfigServiceImpl;
import com.ruoyi.system.utils.DictUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;

/**
 * 系统模块
 * 
 * @author ruoyi
 */
@EnableCustomConfig
@EnableCustomSwagger2
@SpringCloudApplication
@EnableRyFeignClients
public class RuoYiSystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RuoYiSystemApplication.class, args);
        System.out.println(I18nUtil.getMessage("system.msg.startup"));

        DictUtils.initCache();
        SpringUtils.getBean(SysConfigServiceImpl.class).init();
    }
}
