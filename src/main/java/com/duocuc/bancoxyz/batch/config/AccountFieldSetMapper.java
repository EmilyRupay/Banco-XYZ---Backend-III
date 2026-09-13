package com.duocuc.bancoxyz.batch.config;

import com.duocuc.bancoxyz.batch.model.Account;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.math.BigDecimal;

/**
 * Convierte cada linea del CSV de cuentas a un {@link Account}. Igual que
 * {@link TransactionFieldSetMapper}, deja los campos numericos en {@code null}
 * cuando no se pueden interpretar en vez de lanzar una excepcion aqui, para
 * que sea el {@code InterestItemProcessor} quien aplique las reglas de negocio.
 */
public class AccountFieldSetMapper implements FieldSetMapper<Account> {

    @Override
    public Account mapFieldSet(FieldSet fieldSet) throws BindException {
        Account a = new Account();
        a.setAccountId(vacioComoNull(fieldSet.readString("accountId")));
        a.setAccountType(vacioComoNull(fieldSet.readString("accountType")));

        try {
            a.setBalance(new BigDecimal(fieldSet.readString("balance").trim()));
        } catch (Exception e) {
            a.setBalance(null);
        }
        try {
            a.setInterestRate(new BigDecimal(fieldSet.readString("interestRate").trim()));
        } catch (Exception e) {
            a.setInterestRate(null);
        }

        return a;
    }

    private String vacioComoNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
