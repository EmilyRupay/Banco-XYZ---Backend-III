package com.duocuc.bancoxyz.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Orquesta la ejecucion de los 3 Jobs, en orden, cada vez que arranca la
 * aplicacion. Se usa un {@code JobLauncher} propio (en vez del runner
 * automatico de Spring Boot, deshabilitado con
 * {@code spring.batch.job.enabled=false}) para poder controlar el orden y
 * dejar en consola un resumen legible -> util como evidencia de ejecucion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job reporteTransaccionesDiariasJob;
    private final Job calculoInteresesMensualesJob;
    private final Job generacionEstadosCuentaAnualesJob;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        imprimirEncabezado();

        JobExecution ejecucion1 = ejecutar(reporteTransaccionesDiariasJob, "1/3 - Reporte de Transacciones Diarias");
        JobExecution ejecucion2 = ejecutar(calculoInteresesMensualesJob, "2/3 - Calculo de Intereses Mensuales");
        JobExecution ejecucion3 = ejecutar(generacionEstadosCuentaAnualesJob, "3/3 - Generacion de Estados de Cuenta Anuales");

        imprimirResumenFinal(ejecucion1, ejecucion2, ejecucion3);
    }

    private JobExecution ejecutar(Job job, String etiqueta) throws Exception {
        log.info("=================================================================");
        log.info(" Ejecutando Job {} : {}", etiqueta, job.getName());
        log.info("=================================================================");

        // Cada JobParameter con timestamp unico permite volver a correr el mismo
        // Job varias veces (Spring Batch no deja re-ejecutar la misma instancia).
        JobParameters parametros = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution ejecucion = jobLauncher.run(job, parametros);

        log.info(">>> Job '{}' finalizo con estado: {}", job.getName(), ejecucion.getStatus());
        return ejecucion;
    }

    private void imprimirEncabezado() {
        log.info("#################################################################");
        log.info("# BANCO XYZ - MIGRACION DE PROCESOS BATCH LEGACY A SPRING BATCH  #");
        log.info("# PBY2203 - Desarrollo Backend III - Exp1 Semana 2               #");
        log.info("#################################################################");
    }

    private void imprimirResumenFinal(JobExecution... ejecuciones) {
        log.info("=================================================================");
        log.info(" RESUMEN DE EJECUCION");
        log.info("=================================================================");
        boolean huboFallos = false;
        for (JobExecution ejecucion : ejecuciones) {
            log.info(" - {} -> {}", ejecucion.getJobInstance().getJobName(), ejecucion.getStatus());
            if (ejecucion.getStatus() != BatchStatus.COMPLETED) {
                huboFallos = true;
            }
        }
        if (huboFallos) {
            log.warn(" Uno o mas jobs no terminaron en estado COMPLETED. Revisa los logs y la tabla batch_error_log.");
        } else {
            log.info(" Los 3 jobs finalizaron correctamente. Revisa las tablas daily_transaction_report, " +
                    "account_balance y annual_statement para ver los resultados.");
        }
        log.info("=================================================================");
    }
}
