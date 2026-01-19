# INFORME TÉCNICO - SISTEMA DE FACTURACIÓN ELECTRÓNICA

**Fecha:** 18 de Enero de 2026
**Proyecto:** Sistema Integral de Facturación y Gestión de Inventario (Web Application)

## 1. Resumen Ejecutivo
Este sistema ha sido desarrollado como una aplicación web robusta y escalable utilizando **Spring Boot** para el backend y **Angular** para el frontend. El objetivo principal es la gestión eficiente de facturación electrónica, inventario, compras y reportes financieros, cumpliendo con los estándares del **SRI (Ecuador)**.

Se ha puesto especial énfasis en la **seguridad**, la **integridad de datos** y una **experiencia de usuario (UX)** moderna y "Premium".

## 2. Arquitectura del Sistema

### 2.1 Backend (Servidor)
- **Tecnología:** Java 21 + Spring Boot 3.5.7
- **Base de Datos:** MySQL
- **Seguridad:** Spring Security + JWT (JSON Web Tokens)
- **Funcionalidades Clave:**
    - **API RESTful:** Endpoints seguros para todas las operaciones CRUD.
    - **SRI Integration:** Servicios para Firma Electrónica (XAdES-BES), Recepción y Autorización de comprobantes.
    - **PDF Engine:** Generación automática de RIDE (Facturas y Retenciones) usando `OpenPDF`.
    - **Database Migration:** Scripts automáticos (`schema.sql`) para evolución segura de la base de datos sin pérdida de datos.
    - **Módulos:**
        - `Cliente`: Gestión completa de cartera.
        - `Producto`: Control de stock, precios y categorías.
        - `Factura`: Motor de facturación, cálculo de impuestos, y ciclo de vida SRI.
        - `Compra/Retencion`: Gestión de proveedores y obligaciones tributarias.
        - `Caja`: Control de flujo de efectivo (Apertura/Cierre) obligatoria para facturar (Seguridad financiera).

### 2.2 Frontend (Cliente)
- **Tecnología:** Angular (Standalone Components) + TypeScript
- **Diseño:** CSS Moderno (Glassmorphism, Card Layouts, Responsive).
- **Funcionalidades Clave:**
    - **Arquitectura Modular:** Componentes aislados para `Facturación`, `Clientes`, `Productos`, `Compras`.
    - **Seguridad:** `AuthInterceptor` para manejo automático de tokens y redirección en sesiones expiradas.
    - **UX:** Validaciones en tiempo real, modales para detalles y feedback visual inmediato (Toasts/Alertas).
    - **Impresión:** Visualización directa de PDFs generados por el backend en el navegador.

## 3. Detalle de Módulos Implementados

### 3.1 Gestión de Clientes y Proveedores
- **Funcionalidad:** Registro, actualización y eliminación.
- **Validaciones:** Campos obligatorios, formatos de email y RUC.
- **Interfaz:** Tablas modernas con búsqueda y acciones rápidas.

### 3.2 Inventario y Productos
- **Funcionalidad:** Catálogo de productos con precios y control de stock.
- **Stock Automático:** El stock disminuye al vender y aumenta al registrar una compra.

### 3.3 Facturación Electrónica (Ventas)
- **Flujo:** "Caja Abierta" -> Selección Cliente -> Selección Productos -> Generación.
- **SRI:** Botón "Enviar al SRI" que valida, firma y autoriza el comprobante en tiempo real.
- **PDF:** Generación instantánea del RIDE para impresión.

### 3.4 Compras y Retenciones
- **Funcionalidad:** Registro de gastos.
- **Retenciones:** Generación automática de comprobantes de retención (Impuesto a la Renta/IVA) asociados a una compra.
- **PDF:** Descarga automática del comprobante de retención.

### 3.5 Control de Caja
- **Seguridad:** Bloqueo de facturación si no hay una caja abierta por el usuario.
- **Auditoría:** Registro de movimientos de entrada/salida y cierre de caja con balance.

## 4. Trabajo Realizado y Dedicación
Este proyecto representa un esfuerzo de ingeniería completo, pasando por:
1.  **Diseño de Base de Datos:** Relaciones complejas (`OneToMany`, Bidireccionales) optimizadas.
2.  **Resolución de Problemas Críticos:** Depuración de errores de concurrencia, recursión infinita en JSON (Jackson) y conflictos de esquema (`schema.sql`).
3.  **Integración de Terceros:** Motor de firmas electrónicas real.
4.  **Refinamiento de UI:** Transformación de una interfaz básica a una experiencia visual de alta calidad.

## 5. Instrucciones de Entrega
El código fuente completo (Backend y Frontend) se encuentra listo para despliegue.
Consulte el archivo `README.md` para las guías de instalación y ejecución paso a paso.
