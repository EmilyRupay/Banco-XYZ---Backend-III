# Banco XYZ — Migración de Procesos Batch a Spring Batch

**Curso:** PBY2203 — Desarrollo Backend III
**Actividad:** Experiencia 1, Semana 2 (continuación de la Semana 1: *Analizando la arquitectura batch para procesar datos*)

## 1. Objetivo del proyecto

Modernizar tres procesos legacy del Banco XYZ reescribiéndolos como **Jobs de Spring Batch**:

1. **Reporte de Transacciones Diarias** — procesa transacciones, detecta anomalías (montos sobre un umbral) y genera un resumen.
2. **Cálculo de Intereses Mensuales** — aplica interés sobre cuentas de ahorro (`SAVINGS`) y préstamos (`LOAN`), actualizando el saldo final en base de datos.
3. **Generación de Estados de Cuenta Anuales** — compila los datos anuales de cada cuenta, valida su consistencia contable y genera el informe para auditoría.

Los tres Jobs leen datos desde archivos **CSV**, los validan/transforman con un `ItemProcessor`, y escriben el resultado en una **base de datos relacional** (H2 para pruebas locales, PostgreSQL para el perfil de entrega), con **tolerancia a fallos** y **procesamiento paralelo**.

## 2. Arquitectura y decisiones de diseño

```
CSV (transactions.csv / accounts.csv / annual_accounts.csv)
        │
        ▼
FlatFileItemReader  ──(envuelto en SynchronizedItemStreamReader
        │              porque el Step corre en 3 hilos paralelos)
        ▼
ItemProcessor  ──valida, corrige formato, aplica reglas de negocio
        │          (anomalías / interés / consistencia contable)
        ▼
JdbcBatchItemWriter ──inserta/actualiza en la base de datos
        │
        ▼
Tabla de negocio (daily_transaction_report / account_balance / annual_statement)
```

Cada uno de los 3 Steps:

- Usa **chunk-oriented processing** con `chunk size = 5` (`bancoxyz.batch.chunk-size` en `application.yml`).
- Corre con **3 hilos de ejecución paralela** (`TaskExecutor` definido en `BatchExecutorConfig`, `bancoxyz.batch.hilos-paralelos`).
- Es **fault-tolerant**:
  - **Skip policy**: los registros con datos corruptos (`DatoInvalidoException`, errores de parseo del CSV) se descartan hasta un límite (`skipLimit(1000)`), y cada uno queda registrado en la tabla `batch_error_log` gracias a `RegistroErroresSkipListener`.
  - **Retry policy**: los fallos transitorios de base de datos (`TransientDataAccessException`, `EscrituraTemporalException`) se reintentan hasta 3 veces antes de hacer skip.

### Reglas de negocio implementadas por cada `ItemProcessor`

| Job | Processor | Reglas |
|---|---|---|
| Transacciones diarias | `TransactionItemProcessor` | Campos obligatorios, tipos válidos (`DEPOSIT/WITHDRAWAL/TRANSFER/PAYMENT`), monto > 0, normaliza moneda/estado a mayúsculas, marca anomalía si el monto supera $5.000.000 |
| Intereses mensuales | `InterestItemProcessor` | Tipo de cuenta válido (`SAVINGS/LOAN`), tasa de interés en rango [0, 5%], calcula `nuevoSaldo = saldo + saldo × tasa` |
| Estados de cuenta anuales | `AnnualStatementItemProcessor` | Campos obligatorios, valida `openingBalance + depósitos − giros + interés ≈ closingBalance` (tolerancia $1), marca `is_consistent = false` si no cuadra (no descarta la cuenta, la deja para auditoría) |

### Estructura del código

```
src/main/java/com/duocuc/bancoxyz/batch/
 ├── BancoXyzBatchApplication.java      # Punto de entrada
 ├── config/
 │    ├── BatchExecutorConfig.java      # TaskExecutor (3 hilos)
 │    ├── BatchJobRunner.java           # Ejecuta los 3 Jobs en orden al iniciar
 │    ├── DailyTransactionJobConfig.java
 │    ├── MonthlyInterestJobConfig.java
 │    ├── AnnualStatementJobConfig.java
 │    └── *FieldSetMapper.java          # Mapean cada línea CSV a su modelo
 ├── model/                             # Transaction, Account, AnnualStatement
 ├── processor/                         # Un ItemProcessor por Job
 ├── listener/                          # RegistroErroresSkipListener (auditoría)
 └── exception/                         # DatoInvalidoException, EscrituraTemporalException
src/main/resources/
 ├── application.yml                    # Perfiles h2 / postgres
 ├── schema.sql                         # Tablas de negocio (auto-ejecutado al iniciar)
 └── data/*.csv                         # Datos de entrada de ejemplo
```

### Origen de los datos

Los datos de ejemplo (`src/main/resources/data/*.csv`) están inspirados en la estructura de datos legacy del Banco XYZ (repositorio [`bank_legacy_data`](https://github.com/KariVillagran/bank_legacy_data) referenciado en la guía de la actividad) y contienen filas intencionalmente inválidas (montos no numéricos, campos vacíos, tipos desconocidos, fechas corruptas) para poder **demostrar en la ejecución** que el manejo de errores y la tolerancia a fallos funcionan.

Si tu equipo ya trabajó con datos propios en la Semana 1, simplemente reemplaza los 3 archivos CSV en `src/main/resources/data/` manteniendo las mismas columnas/encabezados.

## 3. Cómo ejecutar el proyecto

Ver la guía detallada paso a paso para **Visual Studio Code**: [`INSTRUCCIONES_VISUAL_STUDIO.md`](./INSTRUCCIONES_VISUAL_STUDIO.md).

Resumen rápido (con Maven ya instalado):

```bash
# Perfil H2 (por defecto, cero configuración)
mvn spring-boot:run

# Perfil PostgreSQL (requiere docker-compose up -d primero)
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Al iniciar, la aplicación ejecuta automáticamente los 3 Jobs en orden y deja un resumen en la consola.

## 4. Dónde ver los resultados

Con el perfil `h2` activo, entra a la consola web de H2 en `http://localhost:8080/h2-console` con:

- **JDBC URL:** `jdbc:h2:file:./data/bancoxyz;AUTO_SERVER=TRUE`
- **Usuario:** `sa` / **Password:** *(vacío)*

Tablas a revisar:

- `daily_transaction_report` — transacciones procesadas, columna `is_anomaly`
- `account_balance` — saldo final tras aplicar intereses
- `annual_statement` — estados de cuenta anuales, columna `is_consistent`
- `batch_error_log` — evidencia de cada registro descartado por el skip policy y el motivo

## 5. Evidencia de ejecución

La carpeta [`evidencias/`](./evidencias) queda lista para que agregues tus capturas de pantalla de:

1. La consola mostrando el arranque y la ejecución de los 3 Jobs (`BatchStatus: COMPLETED`).
2. El contenido de las 3 tablas de resultados (o de `batch_error_log` mostrando los registros con errores).
3. (Opcional) La consola H2 (`/h2-console`) o tu cliente de PostgreSQL con los datos cargados.

## 6. Notas técnicas

- **Java 17**, **Spring Boot 3.3.4**, **Spring Batch 5**.
- La primera compilación (`mvn compile` / `mvn spring-boot:run`) requiere conexión a internet para que Maven descargue las dependencias desde Maven Central — es normal que la primera vez tarde uno o dos minutos.
- `spring.batch.job.enabled=false` desactiva el *auto-runner* por defecto de Spring Boot; el orden de ejecución de los 3 Jobs lo controla `BatchJobRunner`, que además imprime un resumen final legible.
- Cada ejecución usa un `JobParameters` con timestamp único, así que puedes correr `mvn spring-boot:run` todas las veces que quieras sin el error *"A job instance already exists"*.
- Antes de subir tu entrega a GitHub, sube el proyecto **bajo una cuenta de tu propiedad** (requisito de la actividad) y luego comprime la carpeta completa con la nomenclatura pedida, por ejemplo: `Exp1_S2_Grupo4.zip`.

## 7. Integrantes del grupo

> _(Completar con los nombres del equipo antes de la entrega)_

- Nombre Apellido — rol/aporte
- Nombre Apellido — rol/aporte
