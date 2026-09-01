package com.duocuc.bancoxyz.batch.config;

import com.duocuc.bancoxyz.batch.model.Transaction;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Convierte cada linea del CSV de transacciones a un {@link Transaction}.
 * <p>
 * A proposito NO lanza excepciones aqui cuando un campo numerico/fecha no se
 * puede interpretar: en su lugar deja el campo en {@code null} y es el
 * {@code TransactionItemProcessor} quien decide -con logica de negocio- si el
 * registro se descarta. Esto evita mezclar errores de "parsing" con errores
 * de "regla de negocio" y concentra el manejo de errores en el ItemProcessor,
 * tal como lo pide la actividad.
 */
public class TransactionFieldSetMapper implements FieldSetMapper<Transaction> {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public Transaction mapFieldSet(FieldSet fieldSet) throws BindException {
        Transaction t = new Transaction();
        t.setTransactionId(vacioComoNull(fieldSet.readString("transactionId")));
        t.setAccountId(vacioComoNull(fieldSet.readString("accountId")));
        t.setType(vacioComoNull(fieldSet.readString("type")));
        t.setCurrency(vacioComoNull(fieldSet.readString("currency")));
        t.setStatus(vacioComoNull(fieldSet.readString("status")));

        try {
            t.setTransactionDate(LocalDate.parse(fieldSet.readString("transactionDate").trim(), FORMATO_FECHA));
        } catch (Exception e) {
            t.setTransactionDate(null); // fecha ilegible -> el processor la rechaza
        }

        try {
            t.setAmount(new BigDecimal(fieldSet.readString("amount").trim()));
        } catch (Exception e) {
            t.setAmount(null); // monto no numerico -> el processor la rechaza
        }

        return t;
    }

    private String vacioComoNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
