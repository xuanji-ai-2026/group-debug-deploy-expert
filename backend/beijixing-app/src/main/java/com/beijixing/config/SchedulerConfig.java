package com.beijixing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class SchedulerConfig implements SchedulingConfigurer, AsyncConfigurer {

    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final String THREAD_NAME_PREFIX = "bx-scheduler-";

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix(THREAD_NAME_PREFIX);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setErrorHandler(t -> {
            log.error("[Scheduler] 定时任务执行异常 - thread: {} | error: {}",
                    Thread.currentThread().getName(), t.getMessage(), t);
        });
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
        log.info("[SchedulerConfig] 定时任务线程池初始化完成 - 核心线程数: {} | 前缀: {}",
                POOL_SIZE, THREAD_NAME_PREFIX);
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(Math.max(POOL_SIZE / 2, 4));
        executor.setThreadNamePrefix("bx-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("[SchedulerConfig] 异步任务线程池初始化完成 - 核心线程数: {}",
                Math.max(POOL_SIZE / 2, 4));
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("[Scheduler] 异步方法执行异常 - method: {}.{} | params: {} | error: {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    params != null ? java.util.Arrays.toString(params) : "null",
                    ex.getMessage(), ex);
        };
    }
}
