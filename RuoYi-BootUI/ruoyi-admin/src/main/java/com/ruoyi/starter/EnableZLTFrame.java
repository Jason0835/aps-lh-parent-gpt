package com.ruoyi.starter;


import com.ruoyi.common.core.annotation.EnableRyFeignClients;
import com.ruoyi.common.core.interceptor.FeignInterceptor;
import com.ruoyi.common.i18n.configure.LocaleConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableRyFeignClients
@EnableAutoConfiguration(
        exclude = {DataSourceAutoConfiguration.class, LocaleConfig.class})
@ComponentScan(value = {"com.ruoyi","com.zlt"},excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {
        FeignInterceptor.class, LocaleConfig.class
}
))
public @interface EnableZLTFrame {
}
