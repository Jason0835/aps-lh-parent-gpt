package com.zlt.aps.redissonLock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedissonLockAnno {

    /**
     * 全局唯一标识
     */
    String uniqueMark() default "";

    /**
     * 锁名需要关联到传入参数的某些字段
     */
    String[] expressions() default {};

    /**
     * 返回未获取锁的国际化
     */
    String msgKey() default "ui.data.column.common.busy";

    /**
     * 获取锁的最长等待时间
     */
    long waitTime() default 2;

    /**
     * 占用锁的最长时间
     */
    long leaseTime() default 20;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

}

