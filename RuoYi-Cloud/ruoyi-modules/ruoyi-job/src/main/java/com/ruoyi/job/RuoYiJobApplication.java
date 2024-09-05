package com.ruoyi.job;

import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.job.service.SysJobServiceImpl;
import org.quartz.SchedulerException;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;

/**
 * 定时任务
 * 
 * @author ruoyi
 */
@EnableCustomConfig
@EnableCustomSwagger2   
@EnableRyFeignClients
@SpringCloudApplication
public class RuoYiJobApplication
{
    public static void main(String[] args) throws TaskException, SchedulerException {
        SpringApplication.run(RuoYiJobApplication.class, args);
        System.out.println(I18nUtil.getMessage("job.msg.startup"));

        SpringUtils.getBean(SysJobServiceImpl.class).init();
    }
}
