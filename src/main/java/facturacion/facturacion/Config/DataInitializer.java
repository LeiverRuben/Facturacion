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

        // 1. Crear Roles si no existen
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

    private void seedDataForTesting() {
        // 1. Empresa Default
        if (empresaRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Empresa empresa = new facturacion.facturacion.Entidades.Empresa();
            empresa.setRazonSocial("MI EMPRESA S.A.");
            empresa.setNombreComercial("TIENDA DE PRUEBA");
            empresa.setRuc("1790011223001");
            empresa.setDirMatriz("Av. Amazonas y Naciones Unidas");
            empresa.setDirEstablecimiento("Av. Amazonas y Naciones Unidas");
            empresa.setEstablecimiento("001");
            empresa.setPuntoEmision("001");
            empresa.setAmbiente(1); // Pruebas
            empresa.setTipoEmision(1);
            empresa.setObligadoContabilidad("SI");
            empresa.setRutaFirma("C:/firma/firma.p12");
            empresa.setClaveFirma("1234");
            empresaRepositorio.save(empresa);
            System.out.println("Empresa de prueba creada.");
        }

        // 2. Cliente Default
        if (clienteRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Cliente cliente = new facturacion.facturacion.Entidades.Cliente();
            cliente.setClienteNombre("Juan");
            cliente.setClienteApellido("Perez");
            cliente.setClienteTelefono("0991234567");
            cliente.setClienteEmail("juan@mail.com");
            cliente.setClienteDireccion("Quito, Ecuador");
            cliente.setClienteEstado(1); // 1 = Activo
            // cliente.setTipoIdentificacion("05"); // Campo no existe en Entidad
            clienteRepositorio.save(cliente);
            System.out.println("Cliente de prueba creado.");
        }

        // 3. Producto Default
        if (productoRepositorio.count() == 0) {
            facturacion.facturacion.Entidades.Producto producto = new facturacion.facturacion.Entidades.Producto();
            producto.setProductoNombre("Laptop Gamer");
            producto.setProductoPrecio(1200.00);
            producto.setProductoStock(10); // Integer
            producto.setProductoTasa(12.0); // Double
            producto.setProductoEstado(1); // Integer 1 = Activo
            producto.setProductoSerial("LAP-001");
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
    }
}
