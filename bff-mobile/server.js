/**
 * bff-mobile/server.js
 *
 * BFF para el cliente MÓVIL (app del Banco XYZ).
 * Estrategia: minimizar el payload y el número de campos para reducir
 * consumo de datos móviles y batería, y acelerar el renderizado en
 * pantallas pequeñas. Se entregan solo los datos esenciales
 * (saldo disponible, últimos movimientos, tarjetas activas resumidas).
 */

const express = require("express");
const morgan = require("morgan");
const cors = require("cors");
const legacy = require("../shared/legacyClient");

const app = express();
const PORT = process.env.MOBILE_PORT || 4002;
const LIMITE_MOVIMIENTOS_MOBILE = 5; // solo los últimos 5, no el historial completo

app.use(cors());
app.use(morgan("dev"));
app.use(express.json());

app.get("/health", (req, res) => {
  res.json({ status: "UP", service: "bff-mobile", timestamp: new Date().toISOString() });
});

/**
 * GET /mobile/clientes/:idCliente/resumen
 * Vista resumida para la pantalla principal de la app:
 * nombre corto, saldo total y una lista compacta de cuentas.
 */
app.get("/mobile/clientes/:idCliente/resumen", async (req, res) => {
  try {
    const { idCliente } = req.params;
    const cliente = await legacy.getCliente(idCliente);
    if (!cliente) return res.status(404).json({ error: "Cliente no encontrado" });

    const cuentasLegacy = await legacy.getCuentasPorCliente(idCliente);

    const cuentas = cuentasLegacy.map((c) => ({
      id: c.ID_CUENTA,
      tipo: c.TIPO_CUENTA,
      saldo: c.SALDO_DISPONIBLE,
    }));

    res.json({
      nombre: cliente.NOMBRE_COMPLETO.split(" ")[0], // solo el primer nombre
      saldoTotal: cuentas.reduce((acc, c) => acc + c.saldo, 0),
      cuentas,
    });
  } catch (err) {
    res.status(err.status || 500).json({ error: "Error obteniendo resumen móvil", detalle: err.message });
  }
});

/**
 * GET /mobile/cuentas/:idCuenta/saldo
 * Endpoint ultra liviano: solo el saldo disponible (para widgets/notificaciones).
 */
app.get("/mobile/cuentas/:idCuenta/saldo", async (req, res) => {
  try {
    const cuenta = await legacy.getCuenta(req.params.idCuenta);
    if (!cuenta) return res.status(404).json({ error: "Cuenta no encontrada" });
    res.json({ id: cuenta.ID_CUENTA, saldo: cuenta.SALDO_DISPONIBLE, moneda: cuenta.MONEDA });
  } catch (err) {
    res.status(err.status || 500).json({ error: "Error obteniendo saldo", detalle: err.message });
  }
});

/**
 * GET /mobile/cuentas/:idCuenta/movimientos
 * Solo los últimos N movimientos (liviano), a diferencia del BFF web
 * que entrega el historial completo.
 */
app.get("/mobile/cuentas/:idCuenta/movimientos", async (req, res) => {
  try {
    const movimientos = await legacy.getMovimientos(req.params.idCuenta, LIMITE_MOVIMIENTOS_MOBILE);
    res.json(
      movimientos.map((m) => ({
        fecha: m.FECHA,
        desc: m.DESCRIPCION,
        monto: m.MONTO,
      }))
    );
  } catch (err) {
    res.status(err.status || 500).json({ error: "Error obteniendo movimientos", detalle: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`[bff-mobile] BFF Móvil escuchando en http://localhost:${PORT}`);
});

module.exports = app;
