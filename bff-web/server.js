/**
 * bff-web/server.js
 *
 * BFF para el cliente WEB (banca en línea de escritorio).
 * Estrategia: entregar el modelo de datos MÁS COMPLETO posible en una sola
 * respuesta (cliente + todas sus cuentas + tarjetas + historial de
 * movimientos completo), porque el frontend web tiene ancho de banda de
 * sobra y renderiza vistas ricas (dashboards, gráficos, tablas extensas).
 * Así se evita que el frontend web tenga que hacer múltiples llamadas
 * encadenadas al backend.
 */

const express = require("express");
const morgan = require("morgan");
const cors = require("cors");
const legacy = require("../shared/legacyClient");

const app = express();
const PORT = process.env.WEB_PORT || 4001;

app.use(cors());
app.use(morgan("dev"));
app.use(express.json());

app.get("/health", (req, res) => {
  res.json({ status: "UP", service: "bff-web", timestamp: new Date().toISOString() });
});

/**
 * GET /web/clientes/:idCliente/dashboard
 * Vista "todo en uno" para el panel principal de banca web:
 * datos del cliente + todas las cuentas, cada una con sus tarjetas
 * y su historial de movimientos completo.
 */
app.get("/web/clientes/:idCliente/dashboard", async (req, res) => {
  try {
    const { idCliente } = req.params;
    const cliente = await legacy.getCliente(idCliente);
    if (!cliente) return res.status(404).json({ error: "Cliente no encontrado" });

    const cuentasLegacy = await legacy.getCuentasPorCliente(idCliente);

    const cuentas = await Promise.all(
      cuentasLegacy.map(async (cuenta) => {
        const [tarjetas, movimientos] = await Promise.all([
          legacy.getTarjetasPorCuenta(cuenta.ID_CUENTA),
          legacy.getMovimientos(cuenta.ID_CUENTA), // historial completo
        ]);
        return {
          idCuenta: cuenta.ID_CUENTA,
          tipoCuenta: cuenta.TIPO_CUENTA,
          numeroCuenta: cuenta.NRO_CUENTA,
          saldoDisponible: cuenta.SALDO_DISPONIBLE,
          saldoContable: cuenta.SALDO_CONTABLE,
          moneda: cuenta.MONEDA,
          estado: cuenta.ESTADO,
          fechaApertura: cuenta.FECHA_APERTURA,
          tarjetas: tarjetas.map((t) => ({
            idTarjeta: t.ID_TARJETA,
            tipo: t.TIPO_TARJETA,
            numeroEnmascarado: t.NRO_TARJETA_MASK,
            estado: t.ESTADO,
            limiteRetiroDiario: t.LIMITE_RETIRO_DIARIO,
          })),
          movimientos: movimientos.map((m) => ({
            id: m.ID_MOV,
            fecha: m.FECHA,
            tipo: m.TIPO,
            descripcion: m.DESCRIPCION,
            monto: m.MONTO,
            canal: m.CANAL,
          })),
          totalMovimientos: movimientos.length,
        };
      })
    );

    res.json({
      cliente: {
        idCliente: cliente.ID_CLIENTE,
        rut: cliente.RUT,
        nombreCompleto: cliente.NOMBRE_COMPLETO,
        email: cliente.EMAIL,
        telefono: cliente.TELEFONO,
        direccion: cliente.DIRECCION,
        segmento: cliente.SEGMENTO,
      },
      resumen: {
        totalCuentas: cuentas.length,
        patrimonioTotal: cuentas.reduce((acc, c) => acc + c.saldoDisponible, 0),
      },
      cuentas,
    });
  } catch (err) {
    console.error(err);
    res.status(err.status || 500).json({ error: "Error obteniendo dashboard web", detalle: err.message });
  }
});

/**
 * GET /web/cuentas/:idCuenta/movimientos
 * Historial completo de movimientos de una cuenta, con filtros de
 * fecha típicos de una vista web (from/to), soportado por el rango
 * completo que entrega la API legacy.
 */
app.get("/web/cuentas/:idCuenta/movimientos", async (req, res) => {
  try {
    const movimientos = await legacy.getMovimientos(req.params.idCuenta);
    res.json({ idCuenta: req.params.idCuenta, total: movimientos.length, movimientos });
  } catch (err) {
    res.status(err.status || 500).json({ error: "Error obteniendo movimientos", detalle: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`[bff-web] BFF Web escuchando en http://localhost:${PORT}`);
});

module.exports = app;
