package com.duocuc.bancoxyz.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de humo (smoke test): confirma que el contexto de Spring levanta
 * correctamente y que los 3 Jobs terminan en estado COMPLETED al ejecutarlos
 * contra la base H2 de pruebas, usando los mismos CSV de ejemplo del proyecto.
 * <p>
 * Se usa el profile "h2" explicito para no depender de una instancia de
 * PostgreSQL durante las pruebas automatizadas / en el pipeline de CI.
 */
@SpringBootTest
@ActiveProfiles("h2")
class BancoXyzBatchApplicationTests {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("reporteTransaccionesDiariasJob")
    private Job reporteTransaccionesDiariasJob;

    @Autowired
    @Qualifier("calculoInteresesMensualesJob")
    private Job calculoInteresesMensualesJob;

    @Autowired
    @Qualifier("generacionEstadosCuentaAnualesJob")
    private Job generacionEstadosCuentaAnualesJob;

    @Test
    void contextLoads() {
        assertThat(reporteTransaccionesDiariasJob).isNotNull();
    }

    @Test
    void elJobDeTransaccionesDiariasCompletaCorrectamente() throws Exception {
        JobExecution ejecucion = jobLauncher.run(reporteTransaccionesDiariasJob, parametrosUnicos());
        assertThat(ejecucion.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void elJobDeInteresesMensualesCompletaCorrectamente() throws Exception {
        JobExecution ejecucion = jobLauncher.run(calculoInteresesMensualesJob, parametrosUnicos());
        assertThat(ejecucion.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void elJobDeEstadosDeCuentaAnualesCompletaCorrectamente() throws Exception {
        JobExecution ejecucion = jobLauncher.run(generacionEstadosCuentaAnualesJob, parametrosUnicos());
        assertThat(ejecucion.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    private JobParameters parametrosUnicos() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}
