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

    // 성능 최적화: 스레드 풀 확장
    executor.setCorePoolSize(20);       // 기본 스레드 개수 (5 -> 20)
    executor.setMaxPoolSize(100);       // 최대 스레드 개수 (10 -> 100)
    executor.setQueueCapacity(500);     // 큐 수용량 (100 -> 500)

    // 스레드 네임 prefix
    executor.setThreadNamePrefix("notification-");

    // 큐가 가득 차면 거절하지 않고, 호출한 스레드에서 직접 처리하도록
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // graceful shutdown 옵션
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);

    // 유휴 스레드 정리 (60초 이상 유휴시 core 사이즈까지 축소)
    executor.setKeepAliveSeconds(60);
    executor.setAllowCoreThreadTimeOut(true);

    executor.initialize();
    return executor;
  }

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // 일반 비동기 작업용 스레드 풀
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("task-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setKeepAliveSeconds(60);
    executor.setAllowCoreThreadTimeOut(true);

    executor.initialize();
    return executor;
  }
}