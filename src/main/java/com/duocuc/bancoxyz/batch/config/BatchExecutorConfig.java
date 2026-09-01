package com.duocuc.bancoxyz.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Politica de escalamiento (punto 5 de las instrucciones): los tres Steps se
 * ejecutan con 3 hilos de ejecucion paralela y un tamano de chunk de 5,
 * definido en {@code application.yml} (bancoxyz.batch.chunk-size).
 */
@Configuration
public class BatchExecutorConfig {

    @Bean
    public TaskExecutor bancoXyzTaskExecutor(@Value("${bancoxyz.batch.hilos-paralelos:3}") int hilos) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(hilos);
        executor.setMaxPoolSize(hilos);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("batch-xyz-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
