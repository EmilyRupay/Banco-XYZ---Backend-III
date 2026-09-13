# Propuesta Técnica — Estrategia de Implementación BFF

**Proyecto:** Backend for Frontend (BFF) — Banco XYZ
**Actividad 1:** Analizar la estrategia de implementación de BFF

## 1. Contexto

El Banco XYZ tiene tres frontends con necesidades muy distintas:

| Cliente | Contexto de uso | Necesidad principal |
|---|---|---|
| **Web** | Banca en línea de escritorio | Vistas ricas, dashboards, historial completo, buena conectividad |
| **Móvil** | App del banco | Ahorro de datos y batería, respuestas rápidas y livianas |
| **Cajero automático** | Operación presencial crítica | Seguridad, superficie mínima de API, validaciones estrictas |

## 2. Estrategias evaluadas

### Opción A — Un único backend genérico con parámetros de "vista"
El cliente indica (por query param o header) qué "modo" de respuesta quiere,
y un solo backend arma la respuesta según ese parámetro.

- ✅ Un solo código base, un solo despliegue.
- ❌ El backend termina lleno de `if/else` por canal, mezclando reglas de
  negocio de canales muy distintos (web, móvil, ATM) en un mismo servicio.
- ❌ Un cambio para el canal ATM (alta criticidad, requiere revisión de
  seguridad) obliga a re-desplegar también web y móvil.
- ❌ No permite escalar ni versionar cada canal de forma independiente.

### Opción B — Un BFF por tipo de cliente (microservicios independientes)
Cada frontend tiene su propio backend, con su propio ciclo de vida, que
consume la(s) fuente(s) de datos comunes (en este caso, el sistema legacy)
y las transforma según las necesidades de su canal.

- ✅ Cada equipo/canal puede evolucionar su BFF sin afectar a los demás.
- ✅ El BFF de ATM puede tener controles de seguridad y despliegue más
  estrictos que el de web, sin comprometer a los otros canales.
- ✅ Cada BFF expone exactamente los campos que su frontend necesita
  (principio de menor privilegio / menor superficie de datos expuesta).
- ✅ Permite optimizar payload por canal: respuestas completas en web,
  respuestas mínimas en móvil.
- ❌ Hay algo de duplicación de código de integración (mitigado con un
  cliente HTTP compartido, ver sección 4).
- ❌ Más piezas que desplegar y monitorear que en la Opción A.

### Opción C — BFF único por equipo/dominio, pero compartido entre canales similares
(por ejemplo, un solo BFF para "web + móvil" y otro solo para ATM)

- Es un punto intermedio, pero en este caso las diferencias entre web y
  móvil (payload completo vs. liviano) son lo suficientemente marcadas como
  para justificar separarlos igual que a ATM.

## 3. Estrategia seleccionada

Se seleccionó la **Opción B: un BFF independiente por tipo de cliente**
(`bff-web`, `bff-mobile`, `bff-atm`), cada uno como su propio servicio HTTP,
por las siguientes razones:

1. **Aislamiento de riesgo:** el canal de cajero automático maneja
   operaciones críticas (retiros); mantenerlo como servicio independiente
   permite aplicar reglas de negocio y controles de seguridad más estrictos
   sin acoplarlo a los cambios de web o móvil.
2. **Optimización de payload por canal:** el BFF web entrega el objeto
   completo (cliente + cuentas + tarjetas + historial completo) porque el
   frontend web renderiza vistas ricas; el BFF móvil entrega solo lo esencial
   (saldo, últimos 5 movimientos) para minimizar consumo de datos.
3. **Evolución independiente:** un cambio de UI en la app móvil no requiere
   tocar ni redesplegar el BFF web ni el de cajeros.
4. **Simplicidad de cada servicio:** cada BFF tiene responsabilidad única
   (Single Responsibility a nivel de servicio), lo que facilita pruebas y
   mantenimiento.

## 4. Mitigación de la duplicación de código

Para no duplicar la lógica de integración con el sistema legacy en los tres
BFF, se extrajo un **cliente HTTP compartido** (`shared/legacyClient.js`) que
todos los BFF importan. De esta forma:

- La lógica de *transformación* de datos (qué campos exponer, cómo
  resumirlos) es propia de cada BFF.
- La lógica de *acceso* a la API legacy está centralizada y se reutiliza.

## 5. Vista general de la arquitectura

```
                         ┌────────────────┐
                         │   legacy-api    │  (sistema núcleo / legado)
                         │   puerto 4000   │
                         └────────┬────────┘
                                  │ HTTP (REST)
             ┌────────────────────┼────────────────────┐
             │                    │                     │
     ┌───────▼──────┐    ┌────────▼───────┐    ┌────────▼───────┐
     │   bff-web     │    │   bff-mobile   │    │    bff-atm     │
     │  puerto 4001  │    │  puerto 4002   │    │  puerto 4003   │
     │ datos completos│   │ datos livianos │    │ operaciones    │
     │               │    │                │    │ críticas/seguras│
     └───────┬───────┘    └────────┬───────┘    └────────┬───────┘
             │                     │                     │
       Frontend Web          App Móvil              Cajero Automático
```

## 6. Conclusión

La estrategia de **un BFF por cliente**, implementada como microservicios
independientes que consumen una API legacy común a través de un cliente HTTP
compartido, es la que mejor equilibra: aislamiento de responsabilidades,
seguridad diferenciada por canal, optimización de payload y mantenibilidad
a futuro del proyecto del Banco XYZ.
