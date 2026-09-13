package com.duocuc.bancoxyz.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion batch del Banco XYZ.
 * <p>
 * Esta aplicacion moderniza tres procesos legacy del banco reescribiendolos como
 * Jobs de Spring Batch:
 * <ol>
 *     <li>Reporte de transacciones diarias (deteccion de anomalias)</li>
 *     <li>Calculo de intereses mensuales (cuentas de ahorro y prestamos)</li>
 *     <li>Generacion de estados de cuenta anuales</li>
 * </ol>
 * Los tres Jobs son disparados en orden por {@link com.duocuc.bancoxyz.batch.config.BatchJobRunner}
 * una vez que el contexto de Spring termina de iniciar.
 */
@SpringBootApplication
public class BancoXyzBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BancoXyzBatchApplication.class, args);
    }
}
