# Sistema de Facturación Electrónica SRI (Web)

Este proyecto es una aplicación web completa para la gestión de facturación electrónica, inventario y compras, adaptada a la normativa del SRI de Ecuador.

## 🚀 Requisitos Previos
*   **Java JDK 17 o 21**
*   **Maven** (Apache Maven)
*   **Node.js** (v18 o superior)
*   **MySQL Server** (Base de datos)

## 🛠️ Configuración e Instalación

### 1. Base de Datos (MySQL)
1.  Crea una base de datos llamada `facturacion` (o el nombre que prefieras).
2.  Configura las credenciales en `backend/src/main/resources/application.properties`:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/facturacion
    spring.datasource.username=root
    spring.datasource.password=TU_CONTRASEÑA
    ```
    *Nota: El sistema creará las tablas automáticamente al iniciarse.*

### 2. Backend (Spring Boot)
1.  Navega a la carpeta `backend`.
2.  Ejecuta el servidor:
    ```bash
    mvn spring-boot:run
    ```
3.  El servidor iniciará en `http://localhost:9090`.
4.  **Usuarios por defecto:**
    *   **Admin:** `admin` / `12345`

### 3. Frontend (Angular)
1.  Navega a la carpeta `frontend`.
2.  Instala las dependencias (solo la primera vez):
    ```bash
    npm install
    ```
3.  Inicia la aplicación:
    ```bash
    npm start
    ```
    (o `ng serve -o`)
4.  Abre tu navegador en `http://localhost:4200`.

## 📦 Módulos Principales

### 🛒 Facturación (Ventas)
*   Requiere **Caja Abierta**.
*   Selección de Cliente y Productos.
*   **Integración SRI:** Firma, envía y autoriza facturas en tiempo real.
*   **PDF:** Generación automática e impresión del RIDE.

### 🛍️ Compras y Retenciones
*   Registro de facturas de proveedores.
*   Gestión de stock (Kardex).
*   Generación automática de **Comprobantes de Retención**.

### 👥 Gestión de Clientes y Proveedores
*   ABM completo (Altas, Bajas, Modificaciones).
*   Validación de RUC/Cédula y correo electrónico.

### 💰 Control de Caja
*   Apertura y Cierre de caja por usuario.
*   Reporte de movimientos y saldo final.

## 🧪 Pruebas
1.  Inicie sesión con `admin` / `12345`.
2.  Vaya a **Caja** y abra una nueva sesión.
3.  Vaya a **Facturación**, cree una nueva factura y guárdela.
4.  En la lista, haga clic en el icono **PDF** para imprimir o en **Enviar SRI** para autorizar.

---
**Desarrollado con dedicación técnica y enfoque en calidad.**
