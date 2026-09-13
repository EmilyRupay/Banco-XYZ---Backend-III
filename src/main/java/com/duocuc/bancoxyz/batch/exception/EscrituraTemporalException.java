package com.duocuc.bancoxyz.batch.exception;

/**
 * Excepcion transitoria (por ejemplo, una contencion momentanea de la base de
 * datos o un timeout de red breve) que amerita reintentar la operacion en
 * lugar de descartar el registro. Usada por la politica de reintentos
 * (retryPolicy) configurada en cada Step.
 */
public class EscrituraTemporalException extends RuntimeException {

    public EscrituraTemporalException(String message) {
        super(message);
    }

    public EscrituraTemporalException(String message, Throwable cause) {
        super(message, cause);
    }
}
