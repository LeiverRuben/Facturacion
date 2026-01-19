package facturacion.facturacion.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import facturacion.facturacion.Entidades.TipoUsuario;
import facturacion.facturacion.Entidades.Usuario;
import facturacion.facturacion.Repositorios.TipoUsuarioRepositorio;
import facturacion.facturacion.Repositorios.UsuarioRepositorio;

@Configuration
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TipoUsuarioRepositorio tipoUsuarioRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        TipoUsuario rolAdmin = crearRolSiNoExiste("ADMIN", "Administrador del sistema");

        crearRolSiNoExiste("VENDEDOR", "Vendedor de punto de venta");
        crearRolSiNoExiste("CONTADOR", "Contador encargado de reportes");

        // 2. Crear o Actualizar Usuario Admin
        if (usuarioRepositorio.existsByUsername("admin")) {
            // Actualizar contraseña si ya existe (para asegurar que sea 12345)
            Usuario admin = usuarioRepositorio.findByUsername("admin").get();
            admin.setPassword(passwordEncoder.encode("12345"));
            admin.setEstaActivo("SI"); // Asegurar que esté activo
            admin.setTipoUsuario(rolAdmin); // Asegurar rol
            usuarioRepositorio.save(admin);
            System.out.println("Usuario 'admin' actualizado correctamente.");
        } else {
            Usuario admin = new Usuario();
            admin.setNombre("Super Admin");
            admin.setCorreo("admin@facturacion.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("12345")); // Contraseña default
            admin.setEstaActivo("SI");
            admin.setTipoUsuario(rolAdmin);

            usuarioRepositorio.save(admin);
            System.out.println("Usuario 'admin' creado con contraseña '12345'");
        }

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
        String rucDefault = "0302152392001";
        String rutaFirma = "C:\\Users\\lzamo\\OneDrive\\Escritorio\\Clavesri\\DANIEL EDUARDO URGILES SARMIENTO 0302152392-211025120131 (1).p12";
        String claveFirma = "DANIEL2025";

        if (empresaRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Empresa empresa = new facturacion.facturacion.Entidades.Empresa();
            empresa.setRazonSocial("MI EMPRESA S.A.");
            empresa.setNombreComercial("TIENDA DE PRUEBA");
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
            // ACTUALIZACIÓN FORZADA: Si ya existe, aseguramos que tenga el RUC y Firma
            // correctos
            facturacion.facturacion.Entidades.Empresa empresa = empresaRepositorio.findAll().get(0);
            empresa.setRuc(rucDefault);
            empresa.setRutaFirma(rutaFirma);
            empresa.setClaveFirma(claveFirma);
            empresaRepositorio.save(empresa);
            System.out.println("Empresa actualizada con la nueva firma y RUC reales.");
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
