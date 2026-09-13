package com.duocuc.bancoxyz.batch.processor;

import com.duocuc.bancoxyz.batch.exception.DatoInvalidoException;
import com.duocuc.bancoxyz.batch.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Aplica el interes mensual sobre cuentas de ahorro (SAVINGS) y prestamos (LOAN).
 * <ul>
 *     <li><b>SAVINGS:</b> el interes se suma al saldo (interes compuesto a favor del cliente).</li>
 *     <li><b>LOAN:</b> el interes se suma a la deuda pendiente (interes a favor del banco).</li>
 * </ul>
 * Se descartan (skip) las cuentas con tipo desconocido, tasa fuera de rango o
 * saldo/tasas no numericos, ya que son datos legacy corruptos.
 */
@Slf4j
public class InterestItemProcessor implements ItemProcessor<Account, Account> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("SAVINGS", "LOAN");
    private static final BigDecimal TASA_MAXIMA = new BigDecimal("0.05"); // 5% mensual tope de seguridad

    @Override
    public Account process(Account item) {
        if (item.getAccountId() == null || item.getAccountId().isBlank()) {
            throw new DatoInvalidoException("accountId vacio en registro de cuentas");
        }
        String tipoNormalizado = item.getAccountType() == null ? "" : item.getAccountType().trim().toUpperCase();
        if (!TIPOS_VALIDOS.contains(tipoNormalizado)) {
            throw new DatoInvalidoException("Tipo de cuenta desconocido '" + item.getAccountType()
                    + "' para la cuenta " + item.getAccountId());
        }
        item.setAccountType(tipoNormalizado);

        if (item.getBalance() == null) {
            throw new DatoInvalidoException("Saldo no numerico/ausente en la cuenta " + item.getAccountId());
        }
        if (item.getInterestRate() == null || item.getInterestRate().compareTo(BigDecimal.ZERO) < 0
                || item.getInterestRate().compareTo(TASA_MAXIMA) > 0) {
            throw new DatoInvalidoException("Tasa de interes fuera de rango [0, " + TASA_MAXIMA + "] en la cuenta "
                    + item.getAccountId());
        }

        BigDecimal saldoBase = item.getBalance();
        BigDecimal interes = saldoBase.multiply(item.getInterestRate()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal nuevoSaldo = "LOAN".equals(tipoNormalizado)
                ? saldoBase.add(interes)   // el interes incrementa la deuda
                : saldoBase.add(interes);  // el interes incrementa el ahorro

        item.setPreviousBalance(saldoBase);
        item.setInterestApplied(interes);
        item.setNewBalance(nuevoSaldo);
        item.setProcessedAt(LocalDateTime.now());

        log.debug("Cuenta {} ({}): saldo {} + interes {} = {}", item.getAccountId(), tipoNormalizado,
                saldoBase, interes, nuevoSaldo);

        return item;
    }
}
