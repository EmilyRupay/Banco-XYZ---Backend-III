package com.duocuc.bancoxyz.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una fila del archivo accounts.csv, luego de aplicar el calculo
 * de intereses mensuales (ItemProcessor del segundo job).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    private String accountId;
    private String accountType;      // SAVINGS | LOAN
    private BigDecimal balance;      // saldo leido del CSV (previo al interes)
    private BigDecimal interestRate; // tasa mensual, ej: 0.004 = 0.4%

    // Campos calculados por el ItemProcessor
    private BigDecimal previousBalance;
    private BigDecimal interestApplied;
    private BigDecimal newBalance;
    private LocalDateTime processedAt;
}
