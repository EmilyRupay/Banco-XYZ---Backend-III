# Banco XYZ — Backend for Frontend (BFF)

**PBY2203 – Desarrollo Backend III | Semana 5**
**Actividad:** Implementando el patrón arquitectónico Backend for Frontend (BFF)

## 1. Objetivo del proyecto

Implementar el patrón **Backend for Frontend (BFF)** para optimizar la comunicación
entre los distintos frontends del Banco XYZ (**web, móvil y cajeros automáticos**),
creando un backend a medida para cada tipo de cliente, de modo de manejar
eficientemente las solicitudes, mejorar la experiencia de usuario en cada
plataforma y mantener la integridad y consistencia de los datos.

Este proyecto da continuidad al análisis realizado en la Semana 4
("Analizando el patrón arquitectónico con Backend for Frontend (BFF)"),
avanzando ahora a la **implementación** de dicha estrategia.

## 2. Estructura del proyecto

```
bff-bank/
├── legacy-api/          # Simula el sistema núcleo (legacy) del Banco XYZ
│   ├── data.js          # Datos "legacy" (clientes, cuentas, tarjetas, movimientos)
│   └── server.js        # API REST que expone los datos crudos, sin adaptar
├── bff-web/
│   └── server.js        # BFF para banca WEB (datos completos y ricos)
├── bff-mobile/
│   └── server.js        # BFF para banca MÓVIL (respuestas livianas)
├── bff-atm/
│   └── server.js        # BFF para CAJEROS AUTOMÁTICOS (operaciones críticas y seguras)
├── shared/
│   └── legacyClient.js  # Cliente HTTP compartido para consumir legacy-api
├── evidencia/
│   ├── evidencia_ejecucion.txt      # Salidas reales de cada endpoint probado
│   └── logs_consola_servidores.txt  # Logs de consola de los 4 servidores
├── propuesta_tecnica.md # Análisis de la estrategia de implementación (punto 1)
├── package.json
└── README.md
```

## 3. Estrategia de implementación (resumen)

Se optó por la estrategia de **"un BFF por tipo de cliente"**, implementado como
**microservicios independientes**, cada uno con su propio proceso, puerto y
código base, en lugar de un único backend configurable o de un monolito con
lógica condicional por canal. El detalle completo del análisis y la
justificación de esta decisión está en [`propuesta_tecnica.md`](./propuesta_tecnica.md).

Los tres BFF **no acceden directamente a una base de datos**: consumen la
**API legacy** (`legacy-api`), que simula el sistema núcleo del banco, y cada
uno transforma esa respuesta según las necesidades de su canal. Esto respeta
el rol del BFF como capa de adaptación, sin duplicar la lógica de acceso a
datos.

## 4. Servicios y puertos

| Servicio | Puerto | Rol |
|---|---|---|
| `legacy-api` | 4000 | Sistema núcleo/legado del Banco XYZ (datos crudos) |
| `bff-web` | 4001 | BFF para navegadores (datos completos, interfaces ricas) |
| `bff-mobile` | 4002 | BFF para app móvil (respuestas livianas y esenciales) |
| `bff-atm` | 4003 | BFF para cajeros automáticos (operaciones críticas y seguras) |

## 5. Endpoints principales

### 5.1 `legacy-api` (puerto 4000)
- `GET /legacy/clientes/:idCliente`
- `GET /legacy/clientes/:idCliente/cuentas`
- `GET /legacy/cuentas/:idCuenta`
- `GET /legacy/cuentas/:idCuenta/tarjetas`
- `GET /legacy/tarjetas/:idTarjeta`
- `GET /legacy/cuentas/:idCuenta/movimientos?limite=N`

### 5.2 `bff-web` (puerto 4001) — datos completos
- `GET /web/clientes/:idCliente/dashboard` → cliente + todas sus cuentas, tarjetas
  y **historial completo** de movimientos, en una sola respuesta.
- `GET /web/cuentas/:idCuenta/movimientos` → historial completo de una cuenta.

### 5.3 `bff-mobile` (puerto 4002) — respuestas ligeras
- `GET /mobile/clientes/:idCliente/resumen` → nombre corto, saldo total y
  listado compacto de cuentas.
- `GET /mobile/cuentas/:idCuenta/saldo` → solo saldo y moneda.
- `GET /mobile/cuentas/:idCuenta/movimientos` → solo los **últimos 5** movimientos,
  con campos mínimos.

### 5.4 `bff-atm` (puerto 4003) — operaciones críticas y seguras
- `GET /atm/tarjetas/:idTarjeta/saldo` → consulta de saldo asociada a la tarjeta
  (no al cliente completo), validando que la tarjeta esté vigente.
- `POST /atm/tarjetas/:idTarjeta/retiro` con body `{ "monto": number }` →
  valida vigencia de la tarjeta, límite de retiro diario y fondos suficientes
  antes de autorizar.

Cada servicio expone además `GET /health` para verificar que está en línea.

## 6. Clientes de datos (dataset)

El dataset base se modeló siguiendo la estructura típica de un core bancario
legacy (clientes, cuentas, tarjetas y movimientos), inspirado en el
repositorio de referencia entregado por la docente
(`github.com/KariVillagran/bank_legacy_data`). El acceso a los datos está
encapsulado en `legacy-api/data.js`, de modo que si se requiere reemplazar
estos datos de ejemplo por el dataset real del repositorio, solo es necesario
modificar ese archivo (o el mecanismo de carga), sin tocar los tres BFF.

## 7. Cómo ejecutar el proyecto

### Requisitos
- Node.js 18 o superior
- npm

### Instalación
```bash
cd bff-bank
npm install
```

### Ejecución (4 terminales, una por servicio)
```bash
npm run start:legacy   # http://localhost:4000
npm run start:web      # http://localhost:4001
npm run start:mobile   # http://localhost:4002
npm run start:atm      # http://localhost:4003
```

O bien, todos juntos en una sola terminal (usa `concurrently`):
```bash
npm run start:all
```

### Pruebas rápidas con curl
```bash
curl http://localhost:4001/web/clientes/C001/dashboard
curl http://localhost:4002/mobile/clientes/C001/resumen
curl http://localhost:4003/atm/tarjetas/TAR-1001/saldo
curl -X POST http://localhost:4003/atm/tarjetas/TAR-1001/retiro \
  -H "Content-Type: application/json" -d '{"monto":100000}'
```

Clientes de prueba disponibles: `C001`, `C002`, `C003`.
Tarjetas de prueba: `TAR-1001`, `TAR-1002`, `TAR-1003`.

## 8. Evidencia de ejecución

En la carpeta [`evidencia/`](./evidencia) se incluyen:
- `evidencia_ejecucion.txt`: salida real (request + response JSON) de cada
  endpoint de los 4 servicios, incluyendo un caso de error controlado
  (cliente inexistente → 404) y un caso de regla de negocio rechazada
  (retiro que excede el límite diario → 403).
- `logs_consola_servidores.txt`: logs de consola de los 4 servidores durante
  la ejecución de las pruebas (equivalentes a las capturas de pantalla de
  consola solicitadas en la actividad).

## 9. Licencia y derechos

Proyecto desarrollado con fines académicos para la asignatura Desarrollo
Backend III (PBY2203), Duoc UC.
