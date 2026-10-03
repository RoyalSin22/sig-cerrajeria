package com.cerrajeria.sig.clientes;

import com.cerrajeria.sig.clientes.dto.ClienteRequest;
import com.cerrajeria.sig.clientes.dto.ClienteResponse;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        Cliente cliente = new Cliente();
        aplicarCampos(cliente, request);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    public ClienteResponse obtener(Long id) {
        return clienteMapper.toResponse(buscarPorId(id));
    }

    /** Listado paginado de todos los clientes (RF-19 / requisito de paginación del alcance original). */
    public org.springframework.data.domain.Page<ClienteResponse> listar(org.springframework.data.domain.Pageable pageable) {
        return clienteRepository.findAll(pageable).map(clienteMapper::toResponse);
    }

    public List<ClienteResponse> buscar(String termino) {
        return clienteMapper.toResponseList(clienteRepository.buscar(termino));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarPorId(id);
        aplicarCampos(cliente, request);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    private void aplicarCampos(Cliente cliente, ClienteRequest request) {
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setDireccion(request.direccion());
        cliente.setObservaciones(request.observaciones());
    }
}
