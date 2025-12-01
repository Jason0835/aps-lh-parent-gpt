package com.zlt.aps.job;

import org.mybatis.spring.annotation.MapperScan;
import org.quartz.SchedulerException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import com.ruoyi.job.service.SysJobServiceImpl;

@EnableTransactionManagement
@EnableCustomConfig
@SpringBootApplication
@EnableCustomSwagger2
@EnableRyFeignClients
@ComponentScan(value={"com.zlt.*","com.ruoyi"})
@MapperScan({"com.ruoyi.**.mapper,com.zlt.**.mapper"})
public class ApsJobApplication {

    public static void main(String[] args) throws TaskException, SchedulerException {
        SpringApplication.run(ApsJobApplication.class, args);
        System.out.println(I18nUtil.getMessage("job.msg.startup"));
        SpringUtils.getBean(SysJobServiceImpl.class).init();
    }

}
