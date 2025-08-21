package com.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于在 Mapper 接口上显式声明对应的真实数据库表名。
 * 数据访问拦截器将优先读取该注解的表名进行权限检查，避免类名推断带来的歧义。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataTable {
    String value();
}

