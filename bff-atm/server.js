/**
 * bff-atm/server.js
 *
 * BFF para el canal CAJERO AUTOMÁTICO (ATM) del Banco XYZ.
 * Estrategia: superficie de API mínima y estricta, pensada para
 * operaciones críticas (consulta de saldo y retiro). No expone datos
 * de contacto del cliente, ni historial extenso, ni información sensible
 * innecesaria para un cajero. Cada operación valida explícitamente el
 * estado de la tarjeta y los límites de retiro antes de responder.
 */

const express = require("express");
const morgan = require("morgan");
const cors = require("cors");
const legacy = require("../shared/legacyClient");

const app = express();
const PORT = process.env.ATM_PORT || 4003;

app.use(cors());
app.use(morgan("dev"));
app.use(express.json());

app.get("/health", (req, res) => {
  res.json({ status: "UP", service: "bff-atm", timestamp: new Date().toISOString() });
});

async function validarTarjeta(idTarjeta) {
  const tarjeta = await legacy.getTarjeta(idTarjeta);
  if (!tarjeta) {
    const err = new Error("Tarjeta no encontrada");
    err.status = 404;
    throw err;
  }
  if (tarjeta.ESTADO !== "VIGENTE") {
    const err = new Error("Tarjeta no vigente, operación rechazada");
    err.status = 403;
    throw err;
  }
  return tarjeta;
}

/**
 * GET /atm/tarjetas/:idTarjeta/saldo
 * Consulta de saldo disponible asociada a una tarjeta (no al cliente
 * directamente), como ocurre en un cajero real.
 */
app.get("/atm/tarjetas/:idTarjeta/saldo", async (req, res) => {
  try {
    const tarjeta = await validarTarjeta(req.params.idTarjeta);
    const cuenta = await legacy.getCuenta(tarjeta.ID_CUENTA);
    res.json({
      tarjeta: tarjeta.NRO_TARJETA_MASK,
      saldoDisponible: cuenta.SALDO_DISPONIBLE,
      moneda: cuenta.MONEDA,
    });
  } catch (err) {
    res.status(err.status || 500).json({ error: err.message });
  }
});

/**
 * POST /atm/tarjetas/:idTarjeta/retiro
 * body: { monto: number }
 * Valida vigencia de la tarjeta, límite diario y fondos suficientes
 * antes de "autorizar" el retiro. No persiste el movimiento (fuera del
 * alcance de esta actividad), pero sí devuelve el resultado de las
 * reglas de negocio críticas para este canal.
 */
app.post("/atm/tarjetas/:idTarjeta/retiro", async (req, res) => {
  try {
    const { monto } = req.body;
    if (typeof monto !== "number" || monto <= 0) {
      return res.status(400).json({ error: "Monto de retiro inválido" });
    }

    const tarjeta = await validarTarjeta(req.params.idTarjeta);
    const cuenta = await legacy.getCuenta(tarjeta.ID_CUENTA);

    if (monto > tarjeta.LIMITE_RETIRO_DIARIO) {
      return res.status(403).json({
        error: "Monto excede el límite de retiro diario",
        limiteDiario: tarjeta.LIMITE_RETIRO_DIARIO,
      });
    }
    if (monto > cuenta.SALDO_DISPONIBLE) {
      return res.status(403).json({ error: "Fondos insuficientes" });
    }

    res.json({
      autorizado: true,
      tarjeta: tarjeta.NRO_TARJETA_MASK,
      montoRetirado: monto,
      saldoDisponibleEstimado: cuenta.SALDO_DISPONIBLE - monto,
      moneda: cuenta.MONEDA,
    });
  } catch (err) {
    res.status(err.status || 500).json({ error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`[bff-atm] BFF Cajeros Automáticos escuchando en http://localhost:${PORT}`);
});

module.exports = app;
