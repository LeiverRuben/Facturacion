
INSERT IGNORE INTO empresa (empresa_id, razon_social, nombre_comercial, ruc, dir_matriz, dir_establecimiento, establecimiento, punto_emision, ambiente, tipo_emision, obligado_contabilidad, ruta_firma, clave_firma)
VALUES (1, 'GLENN FRANCO VEGA JARAMILLO', 'GLENN FRANCO VEGA JARAMILLO', '0910683853001', 'Av. Principal 123', 'Av. Principal 123', '001', '001', 1, 1, 'NO', 'PENDIENTE', 'PENDIENTE');
-- Actualizar si ya existía con datos incorrectos
UPDATE empresa 
SET ruta_firma = 'PENDIENTE', 
    clave_firma = 'PENDIENTE' 
WHERE empresa_id = 1;
-- Insertar Formas de Pago
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (1, '01', 'EFECTIVO');
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (2, '19', 'TARJETA DE CREDITO');
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (3, '20', 'CHEQUE / TRANSFERENCIA');
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (4, '16', 'TARJETA DE DEBITO');
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (5, '17', 'DINERO ELECTRONICO');
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (6, '99', 'CREDITO');

-- Actualizar nombres si ya existen con textos largos
UPDATE forma_pago SET nombre = 'EFECTIVO' WHERE codigo_sri = '01';
UPDATE forma_pago SET nombre = 'CHEQUE / TRANSFERENCIA' WHERE codigo_sri = '20';
-- Insertar Categoría por defecto
INSERT IGNORE INTO categoria (categoria_id, categoria_nombre, categoria_descripcion) VALUES (1, 'General', 'Categoría general');
-- Insertar Cliente por defecto
INSERT IGNORE INTO cliente (cliente_id, cliente_nombre, cliente_apellido, cliente_direccion, cliente_telefono, cliente_email, cliente_estado) 
VALUES (1, 'Consumidor Final', 'Genérico', 'S/D', '0999999999', 'consumidor@mail.com', 1);
