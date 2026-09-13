package com.duocuc.bancoxyz.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una fila del archivo annual_accounts.csv luego de validar la
 * consistencia contable (openingBalance + depositos - giros + interes == closingBalance).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnualStatement {

    private String accountId;
    private String holderName;
    private int year;
    private BigDecimal openingBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalInterest;
    private BigDecimal closingBalance;

    // Campos calculados por el ItemProcessor
    private boolean consistent;
    private BigDecimal differenceAmount;
    private LocalDateTime generatedAt;
}
