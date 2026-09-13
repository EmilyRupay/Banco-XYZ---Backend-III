/**
 * shared/legacyClient.js
 * Cliente HTTP reutilizado por los 3 BFF para hablar con legacy-api.
 * Centralizar esta llamada evita duplicar lógica de integración
 * en cada BFF (cada uno solo se preocupa de la TRANSFORMACIÓN de datos).
 */
const fetch = require("node-fetch");

const LEGACY_BASE_URL = process.env.LEGACY_BASE_URL || "http://localhost:4000";

async function get(path) {
  const res = await fetch(`${LEGACY_BASE_URL}${path}`);
  if (!res.ok) {
    const error = new Error(`Legacy API respondió ${res.status} para ${path}`);
    error.status = res.status;
    throw error;
  }
  return res.json();
}

module.exports = {
  getCliente: (idCliente) => get(`/legacy/clientes/${idCliente}`),
  getCuentasPorCliente: (idCliente) => get(`/legacy/clientes/${idCliente}/cuentas`),
  getCuenta: (idCuenta) => get(`/legacy/cuentas/${idCuenta}`),
  getTarjetasPorCuenta: (idCuenta) => get(`/legacy/cuentas/${idCuenta}/tarjetas`),
  getTarjeta: (idTarjeta) => get(`/legacy/tarjetas/${idTarjeta}`),
  getMovimientos: (idCuenta, limite) =>
    get(`/legacy/cuentas/${idCuenta}/movimientos${limite ? `?limite=${limite}` : ""}`),
};
