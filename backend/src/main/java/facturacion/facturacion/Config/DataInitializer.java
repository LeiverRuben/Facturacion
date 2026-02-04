package facturacion.facturacion.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import facturacion.facturacion.Entidades.TipoUsuario;
import facturacion.facturacion.Entidades.Usuario;
import facturacion.facturacion.Repositorios.TipoUsuarioRepositorio;
import facturacion.facturacion.Repositorios.UsuarioRepositorio;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TipoUsuarioRepositorio tipoUsuarioRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        limpiarColumnasObsoletas();

        TipoUsuario rolAdmin = crearRolSiNoExiste("ADMIN", "Administrador del sistema");
        TipoUsuario rolVendedor = crearRolSiNoExiste("VENDEDOR", "Vendedor de punto de venta");
        TipoUsuario rolContador = crearRolSiNoExiste("CONTADOR", "Contador encargado de reportes");

        // 2. Crear o Actualizar Usuario Admin
        crearUsuarioSiNoExiste("admin", "Super Admin", "admin@facturacion.com", rolAdmin);

        // 2.1 Crear Usuario Vendedor default
        crearUsuarioSiNoExiste("vendedor", "Vendedor Default", "vendedor@facturacion.com", rolVendedor);

        // 2.2 Crear Usuario Contador default
        crearUsuarioSiNoExiste("contador", "Contador Default", "contador@facturacion.com", rolContador);

        seedDataForTesting();
    }

    private TipoUsuario crearRolSiNoExiste(String nombreRol, String descripcion) {
        return tipoUsuarioRepositorio.findByRol(nombreRol)
                .orElseGet(() -> {
                    TipoUsuario nuevoRol = new TipoUsuario();
                    nuevoRol.setRol(nombreRol);
                    nuevoRol.setDescripcion(descripcion);
                    return tipoUsuarioRepositorio.save(nuevoRol);
                });
    }

    private void crearUsuarioSiNoExiste(String username, String nombre, String correo, TipoUsuario rol) {
        if (usuarioRepositorio.existsByUsername(username)) {
            Usuario user = usuarioRepositorio.findByUsername(username).get();
            user.setPassword(passwordEncoder.encode("12345"));
            user.setEstaActivo("SI");
            user.setTipoUsuario(rol);
            usuarioRepositorio.save(user);
            System.out.println("Usuario '" + username + "' actualizado/verificado.");
        } else {
            Usuario user = new Usuario();
            user.setNombre(nombre);
            user.setCorreo(correo);
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("12345"));
            user.setEstaActivo("SI");
            user.setTipoUsuario(rol);
            usuarioRepositorio.save(user);
            System.out.println("Usuario '" + username + "' creado con éxito.");
        }
    }

    private void limpiarColumnasObsoletas() {
        System.out.println("Iniciando corrección de base de datos (Columnas Obsoletas)...");

        // Estrategia: Hacer la columna NULLABLE en lugar de borrarla para evitar
        // errores de Foreign Keys

        // 1. Liquidación de Compra
        try {
            jdbcTemplate.execute("ALTER TABLE liquidacion_de_compra MODIFY COLUMN cliente_id BIGINT NULL DEFAULT NULL");
            System.out.println("OK: Columna 'cliente_id' en liquidacion_de_compra ahora es NULLABLE.");
        } catch (Exception e) {
            System.err.println("WARN: Falló modificación de liquidacion_de_compra: " + e.getMessage());
        }

        // 2. Guía de Remisión
        try {
            jdbcTemplate.execute("ALTER TABLE guia_de_remision MODIFY COLUMN cliente_id BIGINT NULL DEFAULT NULL");
            System.out.println("OK: Columna 'cliente_id' en guia_de_remision ahora es NULLABLE.");
        } catch (Exception e) {
            System.err.println("WARN: Falló modificación de guia_de_remision: " + e.getMessage());
        }
    }

    @Autowired
    private facturacion.facturacion.Repositorios.EmpresaRepositorio empresaRepositorio;
    @Autowired
    private facturacion.facturacion.Repositorios.ClienteRepositorio clienteRepositorio;
    @Autowired
    private facturacion.facturacion.Repositorios.ProductoRepositorio productoRepositorio;
    @Autowired
    private facturacion.facturacion.Repositorios.FormaPagoRepositorio formaPagoRepositorio;
    @Autowired
    private facturacion.facturacion.Repositorios.CategoriaRepositorio categoriaRepositorio;
    @Autowired
    private facturacion.facturacion.Repositorios.FacturaRepositorio facturaRepositorio;

    @Autowired
    private facturacion.facturacion.Repositorios.CajaRepositorio cajaRepositorio;

    private void seedDataForTesting() {
        // ... (existing code) ...
        // 1. Empresa Default
        // 1. Empresa Default
        String rucDefault = "0910683853001";
        String rutaFirma = "PENDIENTE";
        String claveFirma = "PENDIENTE";

        if (empresaRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Empresa empresa = new facturacion.facturacion.Entidades.Empresa();
            empresa.setRazonSocial("GLENN FRANCO VEGA JARAMILLO");
            empresa.setNombreComercial("GLENN FRANCO VEGA JARAMILLO");
            empresa.setRuc(rucDefault);
            empresa.setDirMatriz("Av. Amazonas y Naciones Unidas");
            empresa.setDirEstablecimiento("Av. Amazonas y Naciones Unidas");
            empresa.setEstablecimiento("001");
            empresa.setPuntoEmision("001");
            empresa.setAmbiente(1); // Pruebas
            empresa.setTipoEmision(1);
            empresa.setObligadoContabilidad("SI");
            empresa.setRutaFirma(rutaFirma);
            empresa.setClaveFirma(claveFirma);
            empresaRepositorio.save(empresa);
            System.out.println("Empresa de prueba creada con firma configurada.");
        } else {
            // No sobrescribir datos de producción si ya existe la empresa
            System.out.println("Empresa ya existe. No se sobrescriben datos de firma.");
        }

        // 2. Cliente Default
        if (clienteRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Cliente cliente = new facturacion.facturacion.Entidades.Cliente();
            cliente.setClienteNombre("Juan");
            cliente.setClienteApellido("Perez");
            cliente.setClienteTelefono("0991234567");
            cliente.setClienteEmail("juan@mail.com");
            cliente.setClienteDireccion("Quito, Ecuador");
            cliente.setClienteEstado(true); // 1 = Activo
            // cliente.setTipoIdentificacion("05"); // Campo no existe en Entidad
            clienteRepositorio.save(cliente);
            System.out.println("Cliente de prueba creado.");
        }

        // 4. Default Category
        facturacion.facturacion.Entidades.Categoria defaultCategoria = null;
        if (categoriaRepositorio.count() == 0) {
            defaultCategoria = new facturacion.facturacion.Entidades.Categoria();
            defaultCategoria.setCategoriaNombre("Electrónica");
            defaultCategoria.setCategoriaDescripcion("Dispositivos electrónicos");
            defaultCategoria = categoriaRepositorio.save(defaultCategoria);
            System.out.println("Categoría de prueba creada.");
        } else {
            defaultCategoria = categoriaRepositorio.findAll().get(0);
        }

        // 3. Producto Default
        if (productoRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Producto producto = new facturacion.facturacion.Entidades.Producto();
            producto.setProductoNombre("Laptop Gamer");
            producto.setProductoPrecio(1200.00);
            producto.setProductoStock(10); // Integer
            producto.setProductoTasa(12.0); // Double
            producto.setProductoEstado(true); // Integer 1 = Activo
            producto.setProductoSerial("LAP-001");
            if (defaultCategoria != null) {
                producto.setCategoria(defaultCategoria);
            }
            productoRepositorio.save(producto);
            System.out.println("Producto de prueba creado.");
        }

        // 4. FormaPago Default
        if (formaPagoRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.FormaPago fp = new facturacion.facturacion.Entidades.FormaPago();
            fp.setNombre("SIN UTILIZACION DEL SISTEMA FINANCIERO");
            fp.setCodigoSri("01");
            formaPagoRepositorio.save(fp);
            System.out.println("FormaPago de prueba creada (01).");
        }

        // 5. Factura Default
        if (facturaRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Empresa empresa = empresaRepositorio.findAll().get(0);
            facturacion.facturacion.Entidades.Cliente cliente = clienteRepositorio.findAll().get(0);

            facturacion.facturacion.Entidades.Factura factura = new facturacion.facturacion.Entidades.Factura();
            factura.setEmpresa(empresa);
            factura.setCliente(cliente);
            factura.setFechaEmision(java.time.LocalDateTime.now());
            factura.setSecuencial("000000001");
            factura.setSubtotal12(100.00);
            factura.setSubtotal0(0.00);
            factura.setTotalIva(12.00);
            factura.setTotalFactura(112.00);
            factura.setEstado(1);
            factura.setEstadoSri("PENDIENTE");
            factura.setClaveAcceso("0000000000000000000000000000000000000000000000000"); // Dummy
            facturaRepositorio.save(factura);
            System.out.println("Factura de prueba creada.");
            facturaRepositorio.save(factura);
            System.out.println("Factura de prueba creada.");
        }

        // 6. Caja Default
        if (cajaRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Caja caja = new facturacion.facturacion.Entidades.Caja();
            caja.setNombre("Caja Principal");
            caja.setEstado("ACTIVO");
            cajaRepositorio.save(caja);
            System.out.println("Caja Principal creada.");
        }
    }
}
