/**
 * legacy-api/server.js
 *
 * Simula el "sistema legado" (core bancario) del Banco XYZ.
 * Expone los datos crudos, sin adaptar a ningún cliente en particular.
 * Los tres BFF (web, móvil, cajero) consumen esta API y transforman
 * la respuesta según las necesidades de cada canal.
 */

const express = require("express");
const morgan = require("morgan");
const cors = require("cors");
const data = require("./data");

const app = express();
const PORT = process.env.LEGACY_PORT || 4000;

app.use(cors());
app.use(morgan("dev"));
app.use(express.json());

app.get("/health", (req, res) => {
  res.json({ status: "UP", service: "legacy-api", timestamp: new Date().toISOString() });
});

// Cliente
app.get("/legacy/clientes/:idCliente", (req, res) => {
  const cliente = data.getClientePorId(req.params.idCliente);
  if (!cliente) return res.status(404).json({ error: "Cliente no encontrado" });
  res.json(cliente);
});

// Cuentas de un cliente
app.get("/legacy/clientes/:idCliente/cuentas", (req, res) => {
  const cuentas = data.getCuentasPorCliente(req.params.idCliente);
  res.json(cuentas);
});

// Detalle de una cuenta
app.get("/legacy/cuentas/:idCuenta", (req, res) => {
  const cuenta = data.getCuentaPorId(req.params.idCuenta);
  if (!cuenta) return res.status(404).json({ error: "Cuenta no encontrada" });
  res.json(cuenta);
});

// Tarjetas de una cuenta
app.get("/legacy/cuentas/:idCuenta/tarjetas", (req, res) => {
  res.json(data.getTarjetasPorCuenta(req.params.idCuenta));
});

// Detalle de una tarjeta (usado por el BFF de cajero para validar)
app.get("/legacy/tarjetas/:idTarjeta", (req, res) => {
  const tarjeta = data.getTarjetaPorId(req.params.idTarjeta);
  if (!tarjeta) return res.status(404).json({ error: "Tarjeta no encontrada" });
  res.json(tarjeta);
});

// Movimientos de una cuenta (?limite=N opcional)
app.get("/legacy/cuentas/:idCuenta/movimientos", (req, res) => {
  const limite = req.query.limite ? parseInt(req.query.limite, 10) : undefined;
  res.json(data.getMovimientosPorCuenta(req.params.idCuenta, limite));
});

app.listen(PORT, () => {
  console.log(`[legacy-api] Sistema legado del Banco XYZ escuchando en http://localhost:${PORT}`);
});

module.exports = app;
