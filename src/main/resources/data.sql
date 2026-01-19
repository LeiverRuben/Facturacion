
INSERT IGNORE INTO empresa (empresa_id, razon_social, nombre_comercial, ruc, dir_matriz, dir_establecimiento, establecimiento, punto_emision, ambiente, tipo_emision, obligado_contabilidad, ruta_firma, clave_firma)
VALUES (1, 'Mi Empresa S.A.', 'Mi Empresa', '1790000000001', 'Av. Principal 123', 'Av. Principal 123', '001', '001', 1, 1, 'NO', 'C:/Users/lzamo/OneDrive/Escritorio/Clavesri/DANIEL EDUARDO URGILES SARMIENTO 0302152392-211025120131 .p12', 'DANIEL2025');
-- Actualizar si ya existía con datos incorrectos
UPDATE empresa 
SET ruta_firma = 'C:/Users/lzamo/OneDrive/Escritorio/Clavesri/DANIEL EDUARDO URGILES SARMIENTO 0302152392-211025120131 .p12', 
    clave_firma = 'DANIEL2025' 
WHERE empresa_id = 1;
-- Insertar Formas de Pago
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (1, '01', 'SIN UTILIZACION DEL SISTEMA FINANCIERO');
INSERT IGNORE INTO forma_pago (forma_pago_id, codigo_sri, nombre) VALUES (2, '19', 'TARJETA DE CREDITO');
-- Insertar Categoría por defecto
INSERT IGNORE INTO categoria (categoria_id, categoria_nombre, categoria_descripcion) VALUES (1, 'General', 'Categoría general');
-- Insertar Cliente por defecto
INSERT IGNORE INTO cliente (cliente_id, cliente_nombre, cliente_apellido, cliente_direccion, cliente_telefono, cliente_email, cliente_estado) 
VALUES (1, 'Consumidor Final', 'Genérico', 'S/D', '0999999999', 'consumidor@mail.com', 1);
