package com.beijixing.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    String key() default "";

    int count() default 10;

    long window() default 60;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    String message() default "操作过于频繁，请稍后再试";

    LimitType limitType() default LimitType.DEFAULT;

    enum LimitType {
        DEFAULT,
        IP,
        USER,
        CUSTOM
    }
}
