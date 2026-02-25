package com.zlt.aps.mp.api.annotation;

import com.zlt.aps.mp.api.enums.WeekAdjustTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 周程滚动调整类型注解
 * @author wengpc
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WeekAdjustType {
    WeekAdjustTypeEnum adjustType();
}

