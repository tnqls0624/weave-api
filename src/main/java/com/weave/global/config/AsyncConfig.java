package com.weave.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "notificationExecutor")
  public Executor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // 기본 스레드 개수
    executor.setCorePoolSize(5);

    // 최대 스레드 개수
    executor.setMaxPoolSize(10);

    // 큐 수용량
    executor.setQueueCapacity(100);

    // 스레드 네임 prefix
    executor.setThreadNamePrefix("notification-");

    // 큐가 가득 차면 거절하지 않고, 호출한 스레드에서 직접 처리하도록
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // graceful shutdown 옵션
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);

    executor.initialize();
    return executor;
  }
}