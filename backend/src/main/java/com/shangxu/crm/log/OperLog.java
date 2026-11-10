package com.shangxu.crm.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块标题 */
    String title();

    /** 操作类型 INSERT / UPDATE / DELETE / LOGIN / EXPORT / OTHER */
    String type() default "OTHER";
}
