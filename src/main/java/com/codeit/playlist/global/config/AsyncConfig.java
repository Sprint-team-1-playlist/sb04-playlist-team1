package com.codeit.playlist.global.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableRetry
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

  @Bean(name = "mailExecutor")
  public Executor asyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("mail-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "websocketExecutor")
  public Executor websocketExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(30);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("ws-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "eventTaskExecutor")
  public Executor eventTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("event-");
    executor.initialize();
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return asyncExecutor();
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) -> {
      log.error(
          "[ASYNC ERROR] {} 실행 중 예외 발생, params={}",
          method.getName(),
          params,
          ex
      );
    };

  }
}
