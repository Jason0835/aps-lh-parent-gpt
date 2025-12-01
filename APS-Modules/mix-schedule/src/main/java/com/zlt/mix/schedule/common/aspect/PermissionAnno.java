package com.zlt.mix.schedule.common.aspect;

import java.lang.annotation.*;
import java.util.ArrayList;

/**
 * 封装用户的需要鉴定的权限资源相关
 *
 * @author Liam
 * @date 2022-07-12
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.METHOD})
@Repeatable(PermissionAnnos.class)
public @interface PermissionAnno {

    /**
     * 没有结果时的返回值类型
     *
     * @return 返回值类型
     */
    Class<?> returnType() default ArrayList.class;

    /**
     * 获取处理的字段名
     *
     * @return 获取处理的字段名
     */
    String getName() default "mixArea";

    /**
     * 设置结果的字段名
     *
     * @return 设置结果的字段名
     */
    String setName() default "stringList";

    /**
     * 指定修改参数的下标
     *
     * @return 指定修改参数的下标
     */
    int index() default 0;

    /**
     * 对比的权限列表
     *
     * @return 对比的权限列表
     */
    String[] permissions() default {"M2", "M3", "M4", "M5"};
}
