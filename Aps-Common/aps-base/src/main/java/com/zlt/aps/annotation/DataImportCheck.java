package com.zlt.aps.annotation;

import java.lang.annotation.*;

/**
 * 数据导入数量检查注解
 * @author wengpc
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataImportCheck {

    /**
     * 最大导入数量
     */
    int maxCount() default 1000;

    /**
     * 国际化提示语key（优先级高于defaultMessage）
     */
    String messageKey() default "ui.data.import.count.exceed";

    /**
     * 默认提示语（国际化解析失败时使用）
     */
    String defaultMessage() default "导入数据数量超过最大限制";

    /**
     * 填充国际化文本占位符的参数（支持SpEL表达式）
     */
    String[] params() default {};
}