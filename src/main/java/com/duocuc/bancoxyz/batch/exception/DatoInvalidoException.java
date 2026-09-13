package com.duocuc.bancoxyz.batch.exception;

/**
 * Se lanza desde un ItemProcessor cuando un registro del CSV no cumple las
 * reglas de negocio minimas (campos obligatorios vacios, montos no numericos,
 * tipos de cuenta/transaccion desconocidos, etc).
 * <p>
 * Los Steps estan configurados como fault-tolerant y usan un {@code skipPolicy}
 * que hace "skip" de esta excepcion hasta un limite maximo, registrando cada
 * caso en la tabla {@code batch_error_log} mediante {@link com.duocuc.bancoxyz.batch.listener.RegistroErroresSkipListener}.
 */
public class DatoInvalidoException extends RuntimeException {

    public DatoInvalidoException(String message) {
        super(message);
    }

    public DatoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
