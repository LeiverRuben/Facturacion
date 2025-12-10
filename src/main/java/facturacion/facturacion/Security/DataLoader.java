package facturacion.facturacion.Security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import facturacion.facturacion.Entidades.TipoUsuario;
import facturacion.facturacion.Entidades.Usuario;
import facturacion.facturacion.Repositorios.TipoUsuarioRepositorio;
import facturacion.facturacion.Servicios.UsuarioServicio;

@Configuration
public class DataLoader {
    
    @Bean
    CommandLineRunner initData(
            TipoUsuarioRepositorio tipoRepo,
            UsuarioServicio usuarioService) {
        return args -> {
            TipoUsuario adminTipo = tipoRepo.findByRol("Admin")
                    .orElseGet(() -> {
                        TipoUsuario nuevo = new TipoUsuario();
                        nuevo.setRol("Admin");
                        nuevo.setDescripcion("Administrador del sistema");
                        return tipoRepo.save(nuevo);
                    });

           if (usuarioService.findByUsername("admin") == null) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setUsername("admin");
                admin.setPassword("admin"); 
                admin.setTipoUsuario(adminTipo);

                usuarioService.guardar(admin);

                System.out.println("Usuario 'admin' creado con éxito");
            } else {
                System.out.println("Usuario 'admin' ya existe, no se crea nuevamente");
            }
        };
    }
}
