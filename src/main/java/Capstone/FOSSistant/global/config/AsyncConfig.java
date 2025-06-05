package Capstone.FOSSistant.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean("classifierExecutor")
    public ThreadPoolTaskExecutor classifierExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);       // 최소 10개 스레드 유지
        executor.setMaxPoolSize(50);        // 최대 50개 스레드까지 확장
        executor.setQueueCapacity(100);     // 작업 대기 큐 크기
        executor.setThreadNamePrefix("AI-Clf-"); // 스레드 이름 접두어
        executor.initialize();
        return executor;
    }
}