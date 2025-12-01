package com.zlt.mix.common.engine.config;


import com.zlt.mix.common.engine.service.impl.IncrementService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
  *  引擎公共模块配置类
  * @ClassName EngineCommonConfig
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 19:01
  * @Version 1.0
**/
@Configuration
@EnableCaching
@ComponentScan("com.zlt.mix.common.engine.*")
@MapperScan("com.zlt.**.mapper")
public class EngineCommonConfig {

    @Bean
    public IncrementService incrementService() {
        return new IncrementService();
    }
}
