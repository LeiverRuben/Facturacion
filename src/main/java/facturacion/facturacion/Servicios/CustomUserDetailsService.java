package facturacion.facturacion.Servicios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import facturacion.facturacion.Entidades.Usuario;
import facturacion.facturacion.Repositorios.UsuarioRepositorio;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepositorio usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Username extraído del token: " + username);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        Usuario usuario = usuarioOpt
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + username));

        // Aquí debes convertir tu entidad Usuario a UserDetails
        return new org.springframework.security.core.userdetails.User(
                usuario.getUsername(),
                usuario.getPassword(),
                mapearRoles(usuario.getTipoUsuario().getRol()) // Método para convertir roles a GrantedAuthority
        );
    }

    private Collection<? extends GrantedAuthority> mapearRoles(String nombreRol) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + nombreRol.toUpperCase()));
    }
}
