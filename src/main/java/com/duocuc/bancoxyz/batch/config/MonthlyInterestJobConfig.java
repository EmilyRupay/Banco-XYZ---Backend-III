package com.duocuc.bancoxyz.batch.config;

import com.duocuc.bancoxyz.batch.exception.DatoInvalidoException;
import com.duocuc.bancoxyz.batch.exception.EscrituraTemporalException;
import com.duocuc.bancoxyz.batch.listener.RegistroErroresSkipListener;
import com.duocuc.bancoxyz.batch.model.Account;
import com.duocuc.bancoxyz.batch.processor.InterestItemProcessor;
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
 * Job 2 - Calculo de Intereses Mensuales.
 * <p>
 * Lee {@code accounts.csv}, aplica el interes mensual con
 * {@link InterestItemProcessor} y actualiza (upsert) el saldo final en
 * {@code account_balance}. Mismas politicas de tolerancia a fallos y
 * escalamiento (3 hilos, chunk 5) que el Job 1.
 */
@Configuration
public class MonthlyInterestJobConfig {

    @Bean
    public Job calculoInteresesMensualesJob(JobRepository jobRepository, Step pasoCalculoIntereses) {
        return new JobBuilder("calculoInteresesMensualesJob", jobRepository)
                .start(pasoCalculoIntereses)
                .build();
    }

    @Bean
    public Step pasoCalculoIntereses(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      ItemReader<Account> lectorCuentasSincronizado,
                                      ItemWriter<Account> escritorCuentas,
                                      TaskExecutor bancoXyzTaskExecutor,
                                      JdbcTemplate jdbcTemplate,
                                      @Value("${bancoxyz.batch.chunk-size:5}") int chunkSize) {
        return new StepBuilder("pasoCalculoIntereses", jobRepository)
                .<Account, Account>chunk(chunkSize, transactionManager)
                .reader(lectorCuentasSincronizado)
                .processor(new InterestItemProcessor())
                .writer(escritorCuentas)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skip(org.springframework.batch.item.file.FlatFileParseException.class)
                .skipLimit(1000)
                .retry(TransientDataAccessException.class)
                .retry(EscrituraTemporalException.class)
                .retryLimit(3)
                .listener(new RegistroErroresSkipListener<Account, Account>(jdbcTemplate, "calculoInteresesMensualesJob"))
                .taskExecutor(bancoXyzTaskExecutor)
                .build();
    }

    @Bean
    public FlatFileItemReader<Account> lectorCuentasBase(
            @Value("${bancoxyz.batch.archivos.cuentas}") Resource recurso) {
        return new FlatFileItemReaderBuilder<Account>()
                .name("lectorCuentas")
                .resource(recurso)
                .linesToSkip(1)
                .delimited()
                .names("accountId", "accountType", "balance", "interestRate")
                .fieldSetMapper(new AccountFieldSetMapper())
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<Account> lectorCuentasSincronizado(FlatFileItemReader<Account> lectorCuentasBase) {
        return new SynchronizedItemStreamReaderBuilder<Account>()
                .delegate(lectorCuentasBase)
                .build();
    }

    /**
     * Upsert (INSERT .. o actualiza si ya existe) del saldo de la cuenta.
     * H2 y PostgreSQL usan sintaxis distinta para el upsert, por eso hay un bean
     * por perfil; Spring solo activa el que corresponde al perfil en ejecucion
     * (ver application.yml: spring.profiles.active).
     */
    @org.springframework.context.annotation.Profile("h2")
    @Bean
    public JdbcBatchItemWriter<Account> escritorCuentas(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Account>()
                .dataSource(dataSource)
                .sql("MERGE INTO account_balance " +
                        "(account_id, account_type, balance, interest_rate, interest_applied, previous_balance, " +
                        " last_interest_applied, updated_at) " +
                        "KEY (account_id) " +
                        "VALUES (:accountId, :accountType, :newBalance, :interestRate, :interestApplied, " +
                        " :previousBalance, :processedAt, :processedAt)")
                .beanMapped()
                .build();
    }

    @org.springframework.context.annotation.Profile("postgres")
    @Bean
    public JdbcBatchItemWriter<Account> escritorCuentasPostgres(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Account>()
                .dataSource(dataSource)
                .sql("INSERT INTO account_balance " +
                        "(account_id, account_type, balance, interest_rate, interest_applied, previous_balance, " +
                        " last_interest_applied, updated_at) " +
                        "VALUES (:accountId, :accountType, :newBalance, :interestRate, :interestApplied, " +
                        " :previousBalance, :processedAt, :processedAt) " +
                        "ON CONFLICT (account_id) DO UPDATE SET " +
                        " account_type = EXCLUDED.account_type, balance = EXCLUDED.balance, " +
                        " interest_rate = EXCLUDED.interest_rate, interest_applied = EXCLUDED.interest_applied, " +
                        " previous_balance = EXCLUDED.previous_balance, " +
                        " last_interest_applied = EXCLUDED.last_interest_applied, updated_at = EXCLUDED.updated_at")
                .beanMapped()
                .build();
    }
}
