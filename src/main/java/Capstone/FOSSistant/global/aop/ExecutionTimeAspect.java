package Capstone.FOSSistant.global.aop;

import Capstone.FOSSistant.global.aop.annotation.MeasureExecutionTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {

    @Around("@annotation(measureExecutionTime)")
    public Object measure(ProceedingJoinPoint joinPoint, MeasureExecutionTime measureExecutionTime) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long end = System.currentTimeMillis();
            log.info("[{}] 실행 시간: {}ms", joinPoint.getSignature(), (end - start));
        }
    }
}