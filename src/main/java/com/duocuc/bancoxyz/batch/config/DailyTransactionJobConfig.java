package com.duocuc.bancoxyz.batch.config;

import com.duocuc.bancoxyz.batch.exception.DatoInvalidoException;
import com.duocuc.bancoxyz.batch.exception.EscrituraTemporalException;
import com.duocuc.bancoxyz.batch.listener.RegistroErroresSkipListener;
import com.duocuc.bancoxyz.batch.model.Transaction;
import com.duocuc.bancoxyz.batch.processor.TransactionItemProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Job 1 - Reporte de Transacciones Diarias.
 * <p>
 * Lee {@code transactions.csv}, aplica validaciones/correcciones con
 * {@link TransactionItemProcessor} (detecta anomalias por monto) y persiste
 * el resultado en {@code daily_transaction_report}.
 * <p>
 * Tolerancia a fallos: se hace skip de filas con datos corruptos
 * ({@link DatoInvalidoException} y errores de parseo del CSV) hasta un limite,
 * y se reintenta la escritura ante fallos transitorios de la base de datos.
 * Escalamiento: 3 hilos en paralelo, chunk de tamano 5.
 */
@Configuration
public class DailyTransactionJobConfig {

    @Bean
    public Job reporteTransaccionesDiariasJob(JobRepository jobRepository, Step pasoReporteTransacciones) {
        return new JobBuilder("reporteTransaccionesDiariasJob", jobRepository)
                .start(pasoReporteTransacciones)
                .build();
    }

    @Bean
    public Step pasoReporteTransacciones(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          ItemReader<Transaction> lectorTransaccionesSincronizado,
                                          ItemWriter<Transaction> escritorTransacciones,
                                          TaskExecutor bancoXyzTaskExecutor,
                                          JdbcTemplate jdbcTemplate,
                                          @Value("${bancoxyz.batch.chunk-size:5}") int chunkSize) {
        return new StepBuilder("pasoReporteTransacciones", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(lectorTransaccionesSincronizado)
                .processor(new TransactionItemProcessor())
                .writer(escritorTransacciones)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skip(org.springframework.batch.item.file.FlatFileParseException.class)
                .skipLimit(1000)
                .retry(TransientDataAccessException.class)
                .retry(EscrituraTemporalException.class)
                .retryLimit(3)
                .listener(new RegistroErroresSkipListener<Transaction, Transaction>(jdbcTemplate, "reporteTransaccionesDiariasJob"))
                .taskExecutor(bancoXyzTaskExecutor)
                .build();
    }

    @Bean
    public FlatFileItemReader<Transaction> lectorTransaccionesBase(
            @Value("${bancoxyz.batch.archivos.transacciones}") Resource recurso) {
        return new FlatFileItemReaderBuilder<Transaction>()
                .name("lectorTransacciones")
                .resource(recurso)
                .linesToSkip(1) // encabezado
                .delimited()
                .names("transactionId", "accountId", "transactionDate", "type", "amount", "currency", "status")
                .fieldSetMapper(new TransactionFieldSetMapper())
                .build();
    }

    /**
     * {@link FlatFileItemReader} NO es thread-safe; como el Step usa un
     * TaskExecutor con 3 hilos, el reader se envuelve para sincronizar el acceso.
     */
    @Bean
    public SynchronizedItemStreamReader<Transaction> lectorTransaccionesSincronizado(
            FlatFileItemReader<Transaction> lectorTransaccionesBase) {
        return new SynchronizedItemStreamReaderBuilder<Transaction>()
                .delegate(lectorTransaccionesBase)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> escritorTransacciones(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction>()
                .dataSource(dataSource)
                .sql("INSERT INTO daily_transaction_report " +
                        "(transaction_id, account_id, transaction_date, type, amount, currency, status, " +
                        " is_anomaly, anomaly_reason, processed_at) " +
                        "VALUES (:transactionId, :accountId, :transactionDate, :type, :amount, :currency, :status, " +
                        " :anomaly, :anomalyReason, :processedAt)")
                .beanMapped()
                .build();
    }
}
