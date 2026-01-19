*# INFORME TÉCNICO DETALLADO
## SISTEMA INTEGRAL DE FACTURACIÓN ELECTRÓNICA Y GESTIÓN DE INVENTARIO

**Autor:** Equipo de Desarrollo
**Fecha:** 18 de Enero de 2026
**Versión:** 1.0.0

---

## 1. INTRODUCCIÓN Y ALCANCE

Este informe documenta el desarrollo técnico, la arquitectura y las decisiones de diseño implementadas en el **Sistema de Facturación Electrónica**. El proyecto no es solo un gestor de ventas, sino una solución empresarial completa que integra control de inventario, auditoría financiera (Caja) y cumplimiento tributario automatizado con el Servicio de Rentas Internas (SRI) del Ecuador.

El sistema ha sido diseñado para ser **seguro, escalable y auditale**, priorizando la integridad de los datos y la experiencia de usuario.

---

## 2. ARQUITECTURA DEL SISTEMA

El proyecto sigue una arquitectura **Monolítica Modular** con separación clara entre Frontend y Backend, comunicados a través de una API RESTful.

### 2.1 Stack Tecnológico

#### **Backend (Servidor)**
*   **Lenguaje:** Java 21 (LTS)
*   **Framework:** Spring Boot 3.5.7
*   **Seguridad:** Spring Security 6 + JWT (JSON Web Tokens)
*   **Persistencia:** Spring Data JPA (Hibernate) + MySQL 8.0
*   **Build Tool:** Maven
*   **Librerías Clave:**
    *   `jjwt`: Generación y validación de tokens de sesión.
    *   `OpenPDF`: Motor de generación de reportes (RIDE).
    *   `Lombok`: Reducción de código boilerplate.
    *   `Jakarta XML Binding (JAXB)`: Manipulación de XML para el SRI.

#### **Frontend (Cliente)**
*   **Framework:** Angular 17+ (Standalone Components)
*   **Lenguaje:** TypeScript 5.2
*   **Estilos:** CSS3 Nativo con Diseño Responsivo (Glassmorphism & Card UI)
*   **Librerías Clave:**
    *   `SweetAlert2`: Notificaciones visuales interactivas.
    *   `Boxicons`: Iconografía moderna.

---

## 3. DESARROLLO DEL BACKEND (DETALLE TÉCNICO)

### 3.1 Integración con el SRI (El "Corazón" del Sistema)
Uno de los módulos más complejos y críticos fue la integración con los servicios web del SRI. Se implementó un ciclo de vida completo para los comprobantes:

1.  **Generación de XML:** Se construyen estructuras XML bajo el estándar XSD del SRI (versión 2.1.0) usando clases Java mapeadas con JAXB.
2.  **Firma Electrónica (XAdES-BES):** Implementación manual de la firma digital utilizando certificados `.p12`. El sistema valida la integridad de la firma antes de enviar.
3.  **Consumo de Web Services (SOAP):**
    *   **Recepción:** Envío del XML firmado al endpoint de recepción del SRI (`cel.sri.gob.ec`).
    *   **Autorización:** Consulta asíncrona mediante la Clave de Acceso para obtener el estado final (`AUTORIZADO`, `DEVUELTA`, `RECHAZADA`).
4.  **Manejo de Errores:** Captura y parseo de respuestas XML del SRI para mostrar mensajes claros al usuario (ej. "RUC del comprador no válido").

### 3.2 Seguridad y Control de Acceso (RBAC)
La seguridad no es opcional. Se implementó una estrategia de **Defensa en Profundidad**:

*   **Autenticación Stateless:** No se usan sesiones de servidor. Cada petición va acompañada de un token JWT firmado.
*   **Filtro JWT (`JwtFilter`):** Intercepta cada petición HTTP, valida la firma criptográfica del token, verifica la expiración y extrae el usuario y rol.
*   **Roles y Permisos:**
    *   `ROLE_ADMIN`: Acceso total, incluyendo cierre de caja forzado y anulación de documentos.
    *   `ROLE_VENDEDOR`: Limitado a facturar y ver productos.
    *   `ROLE_CONTADOR`: Acceso de solo lectura a reportes contables.
*   **Encriptación:** Las contraseñas se almacenan hasheadas con **BCrypt**.

### 3.3 Gestión Financiera: Módulo de Caja
Para prevenir fraudes internos ("dinero fantasma"), se implementó una restricción lógica estricta: **No se puede facturar sin una sesión de caja abierta.**

*   **Entidades:** `Caja`, `SesionCaja`, `MovimientoCaja`.
*   **Auditoría:** Cada factura emitida queda vinculada a la `SesionCaja` activa del usuario. Al cerrar la caja, el sistema cruza el "Saldo Esperado" (calculado por el sistema) vs el "Saldo Real" (ingresado por el usuario), generando un reporte de diferencias.

### 3.4 Motor de Reportes (PDF)
Se desarrolló un servicio dedicado `PdfGenServicio` que genera documentos en memoria (byte arrays) para su descarga inmediata.
*   **Factura Comercial:** Diseño estándar con desglose de impuestos (IVA 12%, 0%).
*   **Comprobante de Retención:** Documento tributario generado automáticamente al registrar una compra calificada.

---

## 4. DESARROLLO DEL FRONTEND (EXPERIENCIA DE USUARIO)

### 4.1 Arquitectura de Componentes
Se migró a la arquitectura de **Standalone Components** de Angular, eliminando la complejidad de los `NgModules`. Esto permite una carga más rápida (Lazy Loading) y un código más modular.

### 4.2 Interceptores (`AuthInterceptor`)
Para garantizar la fluidez, se implementó un interceptor HTTP que:
1.  Inyecta automáticamente el token `Bearer` en cada petición al backend.
2.  Detecta errores `401/403` (Token expirado) y redirige automáticamente al Login, limpiando la sesión local para evitar estados inconsistentes.

### 4.3 Interfaces Críticas
*   **Facturación:** Diseñada para velocidad. Permite agregar productos rápidamente, calcula subtotales e impuestos en tiempo real, y bloquea la operación si el stock es insuficiente o la caja está cerrada.
*   **Kardex (Inventario):** Visualización histórica de movimientos. Cada venta resta stock, cada compra suma stock. El sistema impide vender productos sin existencia (validación doble: Frontend + Backend).

---

## 5. DISEÑO DE BASE DE DATOS

El modelo de datos relacional (DER) fue optimizado para integridad referencial:

*   **`Factura` <-> `DetalleFactura` <-> `Producto`:** Relación transaccional clásica.
*   **`Cliente`:** Separado de la factura para permitir gestión de CRM.
*   **`ComprobanteRetencion`:** Vinculado a `Compra` con una relación One-to-One, asegurando que no existan retenciones huérfanas.
*   **Indices:** Se crearon índices en columnas críticas (`clave_acceso`, `ruc`, `fecha_emision`) para acelerar las consultas de reportes.

---

## 6. CONCLUSIONES Y TRABAJO FUTURO

El sistema entregado cumple con el 100% de los requisitos funcionales y regulatorios. Es una plataforma sólida preparada para producción.

**Próximos Pasos Recomendados:**
1.  **Facturación Offline:** Implementar una cola de pendientes para facturar sin internet y sincronizar cuando regrese la conexión.
2.  **Dashboard BI:** Implementar gráficos avanzados con librerías como `Chart.js` para análisis de ventas mensuales.
3.  **Notificaciones Email:** Envío automático del PDF y XML al correo del cliente (actualmente solo se descargan).
