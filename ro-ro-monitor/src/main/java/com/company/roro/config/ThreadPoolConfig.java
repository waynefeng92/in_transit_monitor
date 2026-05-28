package com.company.roro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 线程池配置类
 *
 * 作用：
 * 1. 创建自定义线程池，用于异步处理耗时任务
 * 2. 避免阻塞主线程，提升系统响应速度
 *
 * 使用场景：
 * - Excel 文件上传解析（可能包含几千条数据，同步处理会导致接口超时）
 * - 批量数据处理
 * - 发送通知消息
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * Excel 处理专用线程池
     *
     * @return Executor 线程池实例
     */
    @Bean("excelExecutor")
    public Executor excelExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);        // 核心线程数：线程池常驻线程数量
        executor.setMaxPoolSize(10);        // 最大线程数：高峰期最多能创建的线程数
        executor.setQueueCapacity(100);     // 队列容量：等待执行的任务队列长度
        executor.setThreadNamePrefix("excel-");  // 线程名前缀：便于日志追踪
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略：队列满时由调用线程执行

        executor.initialize();  // 初始化线程池
        return executor;
    }
}