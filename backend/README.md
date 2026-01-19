# Sistema de Facturación con Spring Boot

## Descripción
Este proyecto implementa un sistema de facturación electrónica con las siguientes características:
- **Seguridad**: Login y manejo de roles (ADMIN, VENDEDOR, CONTADOR).
- **Gestión de Usuarios**: CRUD completo de usuarios.
- **Reportes**: Generación de reportes en PDF (Clientes, Ventas) y Excel (Productos).
- **Simulación SRI**: Endpoint para simular la autorización de facturas electrónicas.
- **Documentación API**: Swagger/OpenAPI integrado.

## Requisitos previos
- Java 21 (o superior)
- Maven
- MySQL

## Ejecución
1. Configurar la base de datos en `src/main/resources/application.properties` (si aplica).
2. Ejecutar el comando:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Acceder a la documentación de la API en:
   `http://localhost:8080/swagger-ui.html`

## Roles y Usuarios
El sistema maneja los siguientes roles (insertar en tabla `tipo_usuario`):
1. **ADMIN**
2. **VENDEDOR**
3. **CONTADOR**

### Usuarios por defecto (Creados al iniciar)
La aplicación incluye un `DataInitializer` que crea automáticamente:

1.  **Admin**:
    *   Usuario: `admin`
    *   Contraseña: `12345`
    *   Rol: `ADMIN`

Una vez logueado como Admin, puedes crear otros usuarios (Vendedores, Contadores) usando el endpoint `POST /api/usuarios`.

## Uso de la API

### 1. Autenticación
- **POST** `/auth/login`: Obtener token JWT.
  ```json
  {
    "username": "admin",
    "password": "12345"
  }
  ```
  *Nota: Copiar el token devuelto y usarlo en el botón "Authorize" de Swagger (Bearer <token>).*

### 2. Gestión de Usuarios (Rol: ADMIN)
- **GET** `/api/usuarios`: Listar usuarios.
- **POST** `/api/usuarios`: Crear usuario (Ej. Vendedor).
  ```json
  {
    "nombre": "Juan Vendedor",
    "correo": "juan@mail.com",
    "username": "vendedor1",
    "password": "123",
    "tipoUsuario": { "id": 2 }
  }
  ```
  *(Roles IDs: 1=ADMIN, 2=VENDEDOR, 3=CONTADOR)*

### 3. Reportes
- **Clientes (PDF)**: `GET /api/reportes/clientes/pdf` (Roles: ADMIN, CONTADOR)
- **Ventas (PDF)**: `GET /api/reportes/ventas/pdf` (Roles: ADMIN, CONTADOR)
- **Productos (Excel)**: `GET /api/reportes/productos/excel` (Roles: ADMIN, CONTADOR)

### 4. Simulación SRI (Rol: VENDEDOR, ADMIN)
1.  **Crear Factura**: `POST /api/facturas` (Guardar el ID retornado).
2.  **Enviar a SRI**: `POST /api/facturas/enviar-sri/{id}`.
    *   Este endpoint validad la factura, simula la firma electrónica (bypass si no hay archivo p12) y cambia el estado a **AUTORIZADO**.
    *   Devuelve fecha y mensaje de confirmación.

## Estructura del Proyecto
- **Controladores**: Endpoints de la API.
- **Servicios**: Lógica de negocio (Reportes, SRI, Usuarios).
- **Entidades**: Modelos de base de datos.
- **Security**: Configuración JWT y roles.
