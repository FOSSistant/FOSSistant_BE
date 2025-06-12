package Capstone.FOSSistant.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync                               // @Async 활성화
public class AsyncConfig {

    @Bean("classifierExecutor")
    public ThreadPoolTaskExecutor classifierExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // I/O 바운드: 높은 스레드 수로 대기 지연 최소화
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(0);         // 큐 대신 바로 스레드 확장
        executor.setKeepAliveSeconds(60);     // 유휴 스레드 60초 후 정리
        executor.setThreadNamePrefix("AI-Clf-");
        // 최대치 초과 시 호출 스레드가 직접 처리 → 작업 유실 방지
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }
}