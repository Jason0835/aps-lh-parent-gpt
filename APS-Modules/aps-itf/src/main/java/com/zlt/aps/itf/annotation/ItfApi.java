package com.zlt.aps.itf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author tlt
 * @version 1.0
 * @date 2024/02/26 16:08
 * @description 接口验证 token 注解
 * @since JDK 1.8
 */
@Target({ElementType.METHOD})  //注解的范围
@Retention(RetentionPolicy.RUNTIME) //被虚拟机保存，可用反射机制读取
public @interface ItfApi {

}


