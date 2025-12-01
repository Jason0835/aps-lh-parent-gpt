package com.zlt.mix.schedule.common.aspect;

import java.lang.annotation.*;

/**
 * 封装用户的需要鉴定的权限资源相关
 *
 * @author Liam
 * @date 2022-07-12
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.METHOD})
public @interface PermissionAnnos {
    PermissionAnno[] value();
}
