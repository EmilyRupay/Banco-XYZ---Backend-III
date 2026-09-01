package com.duocuc.bancoxyz.batch.config;

import com.duocuc.bancoxyz.batch.model.AnnualStatement;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.math.BigDecimal;

/**
 * Convierte cada linea del CSV de cuentas anuales a un {@link AnnualStatement}.
 */
public class AnnualStatementFieldSetMapper implements FieldSetMapper<AnnualStatement> {

    @Override
    public AnnualStatement mapFieldSet(FieldSet fieldSet) throws BindException {
        AnnualStatement s = new AnnualStatement();
        s.setAccountId(vacioComoNull(fieldSet.readString("accountId")));
        s.setHolderName(vacioComoNull(fieldSet.readString("holderName")));

        try {
            s.setYear(Integer.parseInt(fieldSet.readString("year").trim()));
        } catch (Exception e) {
            s.setYear(0);
        }

        s.setOpeningBalance(parseMonto(fieldSet, "openingBalance"));
        s.setTotalDeposits(parseMonto(fieldSet, "totalDeposits"));
        s.setTotalWithdrawals(parseMonto(fieldSet, "totalWithdrawals"));
        s.setTotalInterest(parseMonto(fieldSet, "totalInterest"));
        s.setClosingBalance(parseMonto(fieldSet, "closingBalance"));

        return s;
    }

    private BigDecimal parseMonto(FieldSet fieldSet, String campo) {
        try {
            String valor = fieldSet.readString(campo);
            if (valor == null || valor.isBlank()) {
                return null;
            }
            return new BigDecimal(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String vacioComoNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
