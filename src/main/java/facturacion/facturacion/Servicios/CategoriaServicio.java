package facturacion.facturacion.Servicios;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import facturacion.facturacion.Dto.CategoriaDTO;
import facturacion.facturacion.Entidades.Categoria;
import facturacion.facturacion.Repositorios.CategoriaRepositorio;

@Service
public class CategoriaServicio {
    
    private final CategoriaRepositorio categoriaRepositorio;

    public CategoriaServicio(CategoriaRepositorio categoriaRepositorio) {
        this.categoriaRepositorio = categoriaRepositorio;
    }

    public List<CategoriaDTO> findAll() {
        return categoriaRepositorio.findAll()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public CategoriaDTO findById(Long id) {
        Categoria categoria = categoriaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        return mapEntityToDto(categoria);
    }

    public CategoriaDTO save(CategoriaDTO dto) {
        Categoria entity = mapDtoToEntity(dto);
        Categoria saved = categoriaRepositorio.save(entity);
        return mapEntityToDto(saved);
    }

    public void delete(Long id) {
        categoriaRepositorio.deleteById(id);
    }

    private Categoria mapDtoToEntity(CategoriaDTO dto) {
        Categoria entity = new Categoria();
        // Only set ID when DTO provides a valid (>0) id. For create operations the DB will generate it.
        if (dto.getCategoriaId() != null && dto.getCategoriaId() > 0) {
            entity.setCategoriaId(dto.getCategoriaId());
        }
        entity.setCategoriaNombre(dto.getCategoriaNombre());
        entity.setCategoriaDescripcion(dto.getCategoriaDescripcion());
        return entity;
    }

    private CategoriaDTO mapEntityToDto(Categoria entity) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setCategoriaId(entity.getCategoriaId());
        dto.setCategoriaNombre(entity.getCategoriaNombre());
        dto.setCategoriaDescripcion(entity.getCategoriaDescripcion());
        return dto;
    }

    public Categoria guardar(Categoria categoria) {
        return categoriaRepositorio.save(categoria);
    }

    public List<Categoria> listarAll() {
        return categoriaRepositorio.findAll();
    }

    public Categoria buscarId(Long id) {
        return categoriaRepositorio.findById(id).orElse(null);
    }

    public Categoria actualizar(Long id, Categoria categoria) {
        Categoria existente = categoriaRepositorio.findById(id).orElse(null);
        if (existente != null) {
            existente.setCategoriaNombre(categoria.getCategoriaNombre());
            existente.setCategoriaDescripcion(categoria.getCategoriaDescripcion());
            return categoriaRepositorio.save(existente);
        }
        return null;
    }

    public void eliminar(Long id) {
        categoriaRepositorio.deleteById(id);
    }

}
