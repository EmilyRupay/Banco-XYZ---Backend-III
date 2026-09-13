package com.duocuc.bancoxyz.batch.processor;

import com.duocuc.bancoxyz.batch.exception.DatoInvalidoException;
import com.duocuc.bancoxyz.batch.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Valida, corrige y enriquece cada transaccion diaria leida del CSV.
 * <p>
 * Reglas de negocio:
 * <ul>
 *     <li>{@code accountId} y {@code transactionId} son obligatorios.</li>
 *     <li>{@code type} debe ser uno de los tipos conocidos.</li>
 *     <li>{@code amount} debe ser numerico y mayor a 0 (montos negativos o no
 *         numericos se consideran datos corruptos del legacy y se descartan).</li>
 *     <li>{@code currency} se normaliza a mayusculas (correccion automatica de formato).</li>
 *     <li>Toda transaccion sobre el umbral {@link #MONTO_UMBRAL_ANOMALIA} se marca
 *         como anomalia para revision, pero igual se guarda (no se descarta).</li>
 * </ul>
 * Los registros que violan una regla "dura" lanzan {@link DatoInvalidoException},
 * la cual el Step captura mediante la politica de skip configurada en
 * {@code DailyTransactionJobConfig}.
 */
@Slf4j
public class TransactionItemProcessor implements ItemProcessor<Transaction, Transaction> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("DEPOSIT", "WITHDRAWAL", "TRANSFER", "PAYMENT");
    private static final BigDecimal MONTO_UMBRAL_ANOMALIA = new BigDecimal("5000000");

    @Override
    public Transaction process(Transaction item) {
        if (item.getAccountId() == null || item.getAccountId().isBlank()) {
            throw new DatoInvalidoException("accountId vacio en la transaccion " + item.getTransactionId());
        }
        if (item.getTransactionId() == null || item.getTransactionId().isBlank()) {
            throw new DatoInvalidoException("transactionId vacio para la cuenta " + item.getAccountId());
        }
        if (item.getTransactionDate() == null) {
            throw new DatoInvalidoException("Fecha invalida en transaccion " + item.getTransactionId());
        }
        if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatoInvalidoException("Monto invalido (<= 0 o no numerico) en transaccion " + item.getTransactionId());
        }

        String tipoNormalizado = item.getType() == null ? "" : item.getType().trim().toUpperCase();
        if (!TIPOS_VALIDOS.contains(tipoNormalizado)) {
            throw new DatoInvalidoException("Tipo de transaccion desconocido '" + item.getType()
                    + "' en transaccion " + item.getTransactionId());
        }
        item.setType(tipoNormalizado);

        // Correccion automatica de formato: la moneda siempre se normaliza a mayusculas
        if (item.getCurrency() != null) {
            item.setCurrency(item.getCurrency().trim().toUpperCase());
        }
        if (item.getStatus() != null) {
            item.setStatus(item.getStatus().trim().toUpperCase());
        }

        boolean esAnomalia = item.getAmount().compareTo(MONTO_UMBRAL_ANOMALIA) > 0;
        item.setAnomaly(esAnomalia);
        item.setAnomalyReason(esAnomalia
                ? "Monto superior al umbral de auditoria ($" + MONTO_UMBRAL_ANOMALIA + ")"
                : null);
        item.setProcessedAt(LocalDateTime.now());

        if (esAnomalia) {
            log.info("Anomalia detectada: {} por {} {}", item.getTransactionId(), item.getAmount(), item.getCurrency());
        }

        return item;
    }
}
