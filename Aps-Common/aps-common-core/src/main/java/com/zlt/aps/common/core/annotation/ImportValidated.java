package com.zlt.aps.common.core.annotation;

import com.ruoyi.common.core.annotation.Excel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义导出Excel数据注解
 * 
 * @author ruoyi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ImportValidated
{
    /**
     * 列明，没有则从{@link Excel#name()}或{@link Excel#importName()}获取
     * @see com.ruoyi.common.core.annotation.Excel
     * @return
     */
    String name() default "";

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 必须输入字母、数字以及英文字符
     */
    boolean isCode() default false;

    /**
     * 必须输入合法的数字(负数，小数)
     */
    boolean number() default false;

    /**
     * 必须输入整数
     */
    boolean digits() default false;

    /**
     * 允许的最大长度
     */
    int maxLength() default Integer.MAX_VALUE;

    /**
     * 允许的最小长度
     */
    int minLength() default Integer.MIN_VALUE;

    /**
     * 允许的最大值
     */
    double max() default Double.MAX_VALUE;

    /**
     * 允许的最小值
     */
    double min() default Double.MIN_VALUE;

    /**
     * 必须输入日期，且符合格式yyyy-MM-dd
     */
    boolean date() default false;

    /**
     * 是否符合传入字典类型的值
     */
    String dictType() default "";

    /**
     * 是否符合颜色颜色表达式格式 例：#000000
     */
    boolean colorCode() default false;

    /**
     * 必须输入正负整数或0
     */
    boolean isInteger() default false;
}