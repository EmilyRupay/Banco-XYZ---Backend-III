package com.duocuc.bancoxyz.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una fila del archivo transactions.csv, ya validada y enriquecida
 * por el {@code ItemProcessor} del job de reporte de transacciones diarias.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String transactionId;
    private String accountId;
    private LocalDate transactionDate;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String status;

    // Campos calculados por el ItemProcessor
    private boolean anomaly;
    private String anomalyReason;
    private LocalDateTime processedAt;
}
