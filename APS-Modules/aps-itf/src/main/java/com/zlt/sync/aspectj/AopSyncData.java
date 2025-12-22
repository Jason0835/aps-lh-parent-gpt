package com.zlt.sync.aspectj;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)   //定义注解的使用范围为方法
@Retention(RetentionPolicy.RUNTIME )
public @interface AopSyncData {
}
