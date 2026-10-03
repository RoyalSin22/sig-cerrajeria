package com.cerrajeria.sig.compras;

import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.compras.dto.ProveedorRequest;
import com.cerrajeria.sig.compras.dto.ProveedorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(request.nombre());
        proveedor.setTelefono(request.telefono());
        proveedor.setDireccion(request.direccion());
        return toResponse(proveedorRepository.save(proveedor));
    }

    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Proveedor buscarPorId(Long id) {
        return proveedorRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
    }

    private ProveedorResponse toResponse(Proveedor p) {
        return new ProveedorResponse(p.getId(), p.getNombre(), p.getTelefono(), p.getDireccion());
    }
}
