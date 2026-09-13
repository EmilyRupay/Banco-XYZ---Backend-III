package com.duocuc.bancoxyz.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/**
 * Escucha los eventos de "skip" (lectura, procesamiento y escritura) de un Step
 * y deja evidencia de cada registro descartado en la tabla {@code batch_error_log},
 * ademas de loguearlo en consola. Esto cumple el requisito de "manejo de errores
 * y excepciones" dejando trazabilidad de por que un dato fue rechazado.
 * <p>
 * Implementa {@link SkipListener} directamente (en vez de extender la clase de
 * soporte {@code SkipListenerSupport}, que Spring Batch 5 marca como deprecada
 * en favor de los metodos default de la propia interfaz).
 *
 * @param <T> tipo del item leido (input)
 * @param <S> tipo del item escrito (output)
 */
@Slf4j
public class RegistroErroresSkipListener<T, S> implements SkipListener<T, S> {

    private final JdbcTemplate jdbcTemplate;
    private final String jobName;

    public RegistroErroresSkipListener(JdbcTemplate jdbcTemplate, String jobName) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobName = jobName;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[{}] Registro descartado durante la LECTURA: {}", jobName, t.getMessage());
        guardar(null, t);
    }

    @Override
    public void onSkipInProcess(T item, Throwable t) {
        log.warn("[{}] Registro descartado durante el PROCESAMIENTO: {} -> {}", jobName, item, t.getMessage());
        guardar(item, t);
    }

    @Override
    public void onSkipInWrite(S item, Throwable t) {
        log.warn("[{}] Registro descartado durante la ESCRITURA: {} -> {}", jobName, item, t.getMessage());
        guardar(item, t);
    }

    private void guardar(Object item, Throwable t) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO batch_error_log (job_name, raw_item, error_type, error_message, logged_at) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    jobName,
                    item == null ? "N/A" : truncar(item.toString()),
                    t.getClass().getSimpleName(),
                    truncar(t.getMessage()),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            // No queremos que un fallo al auditar detenga el batch: solo lo logueamos.
            log.error("No fue posible registrar el error en batch_error_log", e);
        }
    }

    private String truncar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 490 ? texto.substring(0, 490) : texto;
    }
}
