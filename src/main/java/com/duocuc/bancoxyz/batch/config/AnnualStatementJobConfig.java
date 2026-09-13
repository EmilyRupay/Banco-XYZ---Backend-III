package com.duocuc.bancoxyz.batch.config;

import com.duocuc.bancoxyz.batch.exception.DatoInvalidoException;
import com.duocuc.bancoxyz.batch.exception.EscrituraTemporalException;
import com.duocuc.bancoxyz.batch.listener.RegistroErroresSkipListener;
import com.duocuc.bancoxyz.batch.model.AnnualStatement;
import com.duocuc.bancoxyz.batch.processor.AnnualStatementItemProcessor;
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
 * Job 3 - Generacion de Estados de Cuenta Anuales.
 * <p>
 * Lee {@code annual_accounts.csv}, valida la consistencia contable con
 * {@link AnnualStatementItemProcessor} y guarda el resultado en
 * {@code annual_statement} para su posterior auditoria. Mismas politicas de
 * tolerancia a fallos y escalamiento (3 hilos, chunk 5) que los Jobs 1 y 2.
 */
@Configuration
public class AnnualStatementJobConfig {

    @Bean
    public Job generacionEstadosCuentaAnualesJob(JobRepository jobRepository, Step pasoEstadosCuentaAnuales) {
        return new JobBuilder("generacionEstadosCuentaAnualesJob", jobRepository)
                .start(pasoEstadosCuentaAnuales)
                .build();
    }

    @Bean
    public Step pasoEstadosCuentaAnuales(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          ItemReader<AnnualStatement> lectorEstadosCuentaSincronizado,
                                          ItemWriter<AnnualStatement> escritorEstadosCuenta,
                                          TaskExecutor bancoXyzTaskExecutor,
                                          JdbcTemplate jdbcTemplate,
                                          @Value("${bancoxyz.batch.chunk-size:5}") int chunkSize) {
        return new StepBuilder("pasoEstadosCuentaAnuales", jobRepository)
                .<AnnualStatement, AnnualStatement>chunk(chunkSize, transactionManager)
                .reader(lectorEstadosCuentaSincronizado)
                .processor(new AnnualStatementItemProcessor())
                .writer(escritorEstadosCuenta)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skip(org.springframework.batch.item.file.FlatFileParseException.class)
                .skipLimit(1000)
                .retry(TransientDataAccessException.class)
                .retry(EscrituraTemporalException.class)
                .retryLimit(3)
                .listener(new RegistroErroresSkipListener<AnnualStatement, AnnualStatement>(jdbcTemplate, "generacionEstadosCuentaAnualesJob"))
                .taskExecutor(bancoXyzTaskExecutor)
                .build();
    }

    @Bean
    public FlatFileItemReader<AnnualStatement> lectorEstadosCuentaBase(
            @Value("${bancoxyz.batch.archivos.cuentas-anuales}") Resource recurso) {
        return new FlatFileItemReaderBuilder<AnnualStatement>()
                .name("lectorEstadosCuenta")
                .resource(recurso)
                .linesToSkip(1)
                .delimited()
                .names("accountId", "holderName", "year", "openingBalance", "totalDeposits",
                        "totalWithdrawals", "totalInterest", "closingBalance")
                .fieldSetMapper(new AnnualStatementFieldSetMapper())
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<AnnualStatement> lectorEstadosCuentaSincronizado(
            FlatFileItemReader<AnnualStatement> lectorEstadosCuentaBase) {
        return new SynchronizedItemStreamReaderBuilder<AnnualStatement>()
                .delegate(lectorEstadosCuentaBase)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<AnnualStatement> escritorEstadosCuenta(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<AnnualStatement>()
                .dataSource(dataSource)
                .sql("INSERT INTO annual_statement " +
                        "(account_id, holder_name, statement_year, opening_balance, total_deposits, " +
                        " total_withdrawals, total_interest, closing_balance, is_consistent, difference_amount, generated_at) " +
                        "VALUES (:accountId, :holderName, :year, :openingBalance, :totalDeposits, " +
                        " :totalWithdrawals, :totalInterest, :closingBalance, :consistent, :differenceAmount, :generatedAt)")
                .beanMapped()
                .build();
    }
}
