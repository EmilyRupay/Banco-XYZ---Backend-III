/**
 * data.js
 * Simulación del origen de datos "legacy" del Banco XYZ.
 *
 * NOTA METODOLÓGICA:
 * El repositorio de referencia (github.com/KariVillagran/bank_legacy_data)
 * no pudo ser consultado en el momento de generar este proyecto (repositorio
 * privado / no indexado). Se modeló una estructura de datos legacy típica de
 * un core bancario (clientes, cuentas, tarjetas y movimientos), con campos
 * y nomenclatura "antigua" (mayúsculas, abreviaciones, tipos planos) tal
 * como suele encontrarse en sistemas legacy reales. Si el repositorio
 * entregado por la docente define un esquema distinto, basta con reemplazar
 * el contenido de este archivo (o el método de carga) por el dataset real:
 * el resto del proyecto (legacy-api + los 3 BFF) no necesita cambios,
 * porque todos consumen los datos a través de las funciones exportadas
 * más abajo (capa de desacople).
 */

const CLIENTES = [
  {
    ID_CLIENTE: "C001",
    RUT: "12.345.678-9",
    NOMBRE_COMPLETO: "María Fernanda Soto Rojas",
    EMAIL: "maria.soto@correo.cl",
    TELEFONO: "+56911111111",
    DIRECCION: "Av. Providencia 1234, Santiago",
    FECHA_NACIMIENTO: "1988-04-12",
    SEGMENTO: "PERSONAS",
  },
  {
    ID_CLIENTE: "C002",
    RUT: "9.876.543-2",
    NOMBRE_COMPLETO: "Jorge Andrés Pérez Muñoz",
    EMAIL: "jorge.perez@correo.cl",
    TELEFONO: "+56922222222",
    DIRECCION: "Los Aromos 456, Concepción",
    FECHA_NACIMIENTO: "1975-11-02",
    SEGMENTO: "PYME",
  },
  {
    ID_CLIENTE: "C003",
    RUT: "15.222.333-4",
    NOMBRE_COMPLETO: "Valentina Ignacia Díaz Contreras",
    EMAIL: "valentina.diaz@correo.cl",
    TELEFONO: "+56933333333",
    DIRECCION: "Camino Real 789, Viña del Mar",
    FECHA_NACIMIENTO: "1996-07-23",
    SEGMENTO: "PERSONAS",
  },
];

const CUENTAS = [
  {
    ID_CUENTA: "CTA-0001",
    ID_CLIENTE: "C001",
    TIPO_CUENTA: "CUENTA_CORRIENTE",
    NRO_CUENTA: "00112233445",
    SALDO_DISPONIBLE: 1250340,
    SALDO_CONTABLE: 1265340,
    MONEDA: "CLP",
    ESTADO: "ACTIVA",
    FECHA_APERTURA: "2015-03-10",
  },
  {
    ID_CUENTA: "CTA-0002",
    ID_CLIENTE: "C001",
    TIPO_CUENTA: "CUENTA_AHORRO",
    NRO_CUENTA: "00112233999",
    SALDO_DISPONIBLE: 4520000,
    SALDO_CONTABLE: 4520000,
    MONEDA: "CLP",
    ESTADO: "ACTIVA",
    FECHA_APERTURA: "2018-06-01",
  },
  {
    ID_CUENTA: "CTA-0003",
    ID_CLIENTE: "C002",
    TIPO_CUENTA: "CUENTA_CORRIENTE",
    NRO_CUENTA: "00299887766",
    SALDO_DISPONIBLE: 8750500,
    SALDO_CONTABLE: 8790500,
    MONEDA: "CLP",
    ESTADO: "ACTIVA",
    FECHA_APERTURA: "2012-01-20",
  },
  {
    ID_CUENTA: "CTA-0004",
    ID_CLIENTE: "C003",
    TIPO_CUENTA: "CUENTA_VISTA",
    NRO_CUENTA: "00355667788",
    SALDO_DISPONIBLE: 320750,
    SALDO_CONTABLE: 320750,
    MONEDA: "CLP",
    ESTADO: "ACTIVA",
    FECHA_APERTURA: "2021-09-14",
  },
];

const TARJETAS = [
  {
    ID_TARJETA: "TAR-1001",
    ID_CUENTA: "CTA-0001",
    TIPO_TARJETA: "DEBITO",
    NRO_TARJETA_MASK: "**** **** **** 4321",
    ESTADO: "VIGENTE",
    LIMITE_RETIRO_DIARIO: 500000,
  },
  {
    ID_TARJETA: "TAR-1002",
    ID_CUENTA: "CTA-0003",
    TIPO_TARJETA: "DEBITO",
    NRO_TARJETA_MASK: "**** **** **** 8765",
    ESTADO: "VIGENTE",
    LIMITE_RETIRO_DIARIO: 1000000,
  },
  {
    ID_TARJETA: "TAR-1003",
    ID_CUENTA: "CTA-0004",
    TIPO_TARJETA: "DEBITO",
    NRO_TARJETA_MASK: "**** **** **** 2468",
    ESTADO: "VIGENTE",
    LIMITE_RETIRO_DIARIO: 300000,
  },
];

const MOVIMIENTOS = [
  { ID_MOV: "M0001", ID_CUENTA: "CTA-0001", FECHA: "2026-09-01", TIPO: "CARGO", DESCRIPCION: "Compra supermercado", MONTO: -45320, CANAL: "WEB" },
  { ID_MOV: "M0002", ID_CUENTA: "CTA-0001", FECHA: "2026-09-03", TIPO: "ABONO", DESCRIPCION: "Transferencia recibida", MONTO: 300000, CANAL: "MOVIL" },
  { ID_MOV: "M0003", ID_CUENTA: "CTA-0001", FECHA: "2026-09-05", TIPO: "CARGO", DESCRIPCION: "Pago cuenta luz", MONTO: -38900, CANAL: "WEB" },
  { ID_MOV: "M0004", ID_CUENTA: "CTA-0001", FECHA: "2026-09-08", TIPO: "CARGO", DESCRIPCION: "Retiro cajero automático", MONTO: -100000, CANAL: "ATM" },
  { ID_MOV: "M0005", ID_CUENTA: "CTA-0001", FECHA: "2026-09-10", TIPO: "ABONO", DESCRIPCION: "Depósito sueldo", MONTO: 900000, CANAL: "SUCURSAL" },
  { ID_MOV: "M0006", ID_CUENTA: "CTA-0002", FECHA: "2026-09-02", TIPO: "ABONO", DESCRIPCION: "Interés mensual", MONTO: 12500, CANAL: "SISTEMA" },
  { ID_MOV: "M0007", ID_CUENTA: "CTA-0003", FECHA: "2026-09-04", TIPO: "CARGO", DESCRIPCION: "Pago proveedor", MONTO: -560000, CANAL: "WEB" },
  { ID_MOV: "M0008", ID_CUENTA: "CTA-0003", FECHA: "2026-09-06", TIPO: "ABONO", DESCRIPCION: "Pago cliente factura 221", MONTO: 1200000, CANAL: "WEB" },
  { ID_MOV: "M0009", ID_CUENTA: "CTA-0004", FECHA: "2026-09-07", TIPO: "CARGO", DESCRIPCION: "Compra en línea", MONTO: -25990, CANAL: "MOVIL" },
  { ID_MOV: "M0010", ID_CUENTA: "CTA-0004", FECHA: "2026-09-09", TIPO: "CARGO", DESCRIPCION: "Retiro cajero automático", MONTO: -50000, CANAL: "ATM" },
];

function getClientePorId(idCliente) {
  return CLIENTES.find((c) => c.ID_CLIENTE === idCliente) || null;
}

function getCuentasPorCliente(idCliente) {
  return CUENTAS.filter((c) => c.ID_CLIENTE === idCliente);
}

function getCuentaPorId(idCuenta) {
  return CUENTAS.find((c) => c.ID_CUENTA === idCuenta) || null;
}

function getTarjetasPorCuenta(idCuenta) {
  return TARJETAS.filter((t) => t.ID_CUENTA === idCuenta);
}

function getTarjetaPorId(idTarjeta) {
  return TARJETAS.find((t) => t.ID_TARJETA === idTarjeta) || null;
}

function getMovimientosPorCuenta(idCuenta, limite) {
  const movs = MOVIMIENTOS.filter((m) => m.ID_CUENTA === idCuenta).sort(
    (a, b) => new Date(b.FECHA) - new Date(a.FECHA)
  );
  return limite ? movs.slice(0, limite) : movs;
}

module.exports = {
  CLIENTES,
  CUENTAS,
  TARJETAS,
  MOVIMIENTOS,
  getClientePorId,
  getCuentasPorCliente,
  getCuentaPorId,
  getTarjetasPorCuenta,
  getTarjetaPorId,
  getMovimientosPorCuenta,
};
