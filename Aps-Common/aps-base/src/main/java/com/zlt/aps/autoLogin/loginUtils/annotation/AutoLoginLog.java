package com.zlt.aps.autoLogin.loginUtils.annotation;


import java.lang.annotation.*;

/**
 * ApiLog 接口日志注解
 * @author zhangxh
 * @date 20250506
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoLoginLog {

    String value() default "";

    String serviceName() default "";

    /**
     * 来源系统
     * @return
     */
    String fromSystem() default "";
    /**
     * 目标系统
     * @return
     */
    String toSystem() default "";

    /**
     * 登录标识 传 Y
     * @return
     */
    String isLogin() default "Y";


    // 自定义参数
    String[] params() default {};
}