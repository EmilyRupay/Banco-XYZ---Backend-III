package com.duocuc.bancoxyz.batch.processor;

import com.duocuc.bancoxyz.batch.exception.DatoInvalidoException;
import com.duocuc.bancoxyz.batch.model.AnnualStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Compila el estado de cuenta anual de cada cliente y valida su consistencia
 * contable: {@code openingBalance + totalDeposits - totalWithdrawals + totalInterest}
 * debe ser (aproximadamente) igual a {@code closingBalance}.
 * <p>
 * Las filas con campos obligatorios ausentes se descartan (skip). Las filas
 * con inconsistencia contable NO se descartan -ya que un banco no puede perder
 * el registro de una cuenta-, pero se marcan {@code consistent = false} y con
 * la diferencia detectada, para que el area de auditoria las revise.
 */
@Slf4j
public class AnnualStatementItemProcessor implements ItemProcessor<AnnualStatement, AnnualStatement> {

    /** Tolerancia por errores de redondeo de decimales legacy. */
    private static final BigDecimal TOLERANCIA = new BigDecimal("1.00");

    @Override
    public AnnualStatement process(AnnualStatement item) {
        if (item.getAccountId() == null || item.getAccountId().isBlank()) {
            throw new DatoInvalidoException("accountId vacio en estado de cuenta anual");
        }
        if (item.getHolderName() == null || item.getHolderName().isBlank()) {
            throw new DatoInvalidoException("Titular vacio para la cuenta " + item.getAccountId());
        }
        if (item.getOpeningBalance() == null || item.getTotalDeposits() == null
                || item.getTotalWithdrawals() == null || item.getTotalInterest() == null
                || item.getClosingBalance() == null) {
            throw new DatoInvalidoException("Montos incompletos en el estado de cuenta anual de " + item.getAccountId());
        }

        BigDecimal saldoEsperado = item.getOpeningBalance()
                .add(item.getTotalDeposits())
                .subtract(item.getTotalWithdrawals())
                .add(item.getTotalInterest());

        BigDecimal diferencia = item.getClosingBalance().subtract(saldoEsperado).abs();
        boolean esConsistente = diferencia.compareTo(TOLERANCIA) <= 0;

        item.setConsistent(esConsistente);
        item.setDifferenceAmount(diferencia);
        item.setGeneratedAt(LocalDateTime.now());

        if (!esConsistente) {
            log.warn("Inconsistencia contable en cuenta {} ({}): diferencia de {}",
                    item.getAccountId(), item.getHolderName(), diferencia);
        }

        return item;
    }
}
