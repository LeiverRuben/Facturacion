package facturacion.facturacion.Servicios;

import facturacion.facturacion.Entidades.Proveedor;
import facturacion.facturacion.Repositorios.ProveedorRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorServicio {

    @Autowired
    private ProveedorRepositorio proveedorRepositorio;

    public List<Proveedor> listarTodos() {
        return proveedorRepositorio.findAll();
    }

    public Proveedor obtenerPorId(Long id) {
        return proveedorRepositorio.findById(id).orElse(null);
    }

    public Proveedor guardar(Proveedor proveedor) {
        return proveedorRepositorio.save(proveedor);
    }

    public Proveedor actualizar(Long id, Proveedor proveedorActualizado) {
        return proveedorRepositorio.findById(id).map(proveedor -> {
            proveedor.setRazonSocial(proveedorActualizado.getRazonSocial());
            proveedor.setRuc(proveedorActualizado.getRuc());
            proveedor.setEmail(proveedorActualizado.getEmail());
            proveedor.setTelefono(proveedorActualizado.getTelefono());
            proveedor.setDireccion(proveedorActualizado.getDireccion());
            return proveedorRepositorio.save(proveedor);
        }).orElse(null);
    }

    public void eliminar(Long id) {
        proveedorRepositorio.deleteById(id);
    }

    public Optional<Proveedor> buscarPorRuc(String ruc) {
        return proveedorRepositorio.findByRuc(ruc);
    }
}
