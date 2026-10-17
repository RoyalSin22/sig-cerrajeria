package com.cerrajeria.sig.asistente;

import com.cerrajeria.sig.asistente.dto.ComandoInterpretado;
import com.cerrajeria.sig.asistente.dto.TurnoChat;
import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.exception.CajaNoAbiertaException;
import com.cerrajeria.sig.common.exception.StockInsuficienteException;
import com.cerrajeria.sig.domicilio.ServicioDomicilioService;
import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioRequest;
import com.cerrajeria.sig.inventario.Producto;
import com.cerrajeria.sig.inventario.ProductoRepository;
import com.cerrajeria.sig.ordenes.EstadoOrden;
import com.cerrajeria.sig.ordenes.OrdenTrabajoRepository;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.ventas.VentaService;
import com.cerrajeria.sig.ventas.dto.ItemVentaRequest;
import com.cerrajeria.sig.ventas.dto.VentaCreateRequest;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * El "cerebro" del asistente de IA (Fase 12): interpreta un mensaje en
 * lenguaje natural y ejecuta la accion resultante reutilizando los MISMOS
 * servicios que usa el resto del sistema (VentaService, ServicioDomicilioService,
 * etc.), de forma que las 3 reglas de negocio se cumplen igual sin importar
 * el canal (chat dentro de la app, o WhatsApp — ver whatsapp/ComandoWhatsAppService).
 */
@Service
@RequiredArgsConstructor
public class AsistenteIAService {

    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final VentaService ventaService;
    private final ServicioDomicilioService servicioDomicilioService;
    private final ClaudeClient claudeClient;

    /**
     * Punto de entrada unico para cualquier canal: autentica temporalmente
     * como el usuario dado (para que los servicios internos resuelvan bien
     * "quien esta operando"), interpreta el mensaje con el historial dado,
     * ejecuta la accion, y devuelve la respuesta en texto.
     */
    public String responder(Usuario usuario, List<TurnoChat> historial, String mensaje) {
        autenticarComo(usuario);
        try {
            return ejecutar(historial, mensaje);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String ejecutar(List<TurnoChat> historial, String texto) {
        ComandoInterpretado comando = claudeClient.interpretar(historial, texto);

        if (!comando.tieneAccion()) {
            return comando.textoLibre();
        }

        try {
            return switch (comando.herramienta()) {
                case "consultar_stock" -> consultarStock(comando.parametros());
                case "registrar_venta" -> registrarVenta(comando.parametros());
                case "agendar_servicio_domicilio" -> agendarServicio(comando.parametros());
                case "consultar_trabajos_pendientes" -> consultarTrabajosPendientes();
                default -> "No sé cómo hacer eso todavía.";
            };
        } catch (CajaNoAbiertaException ex) {
            return "No puedo hacer eso: no tienes una caja abierta. Ábrela primero desde la pantalla de Caja.";
        } catch (StockInsuficienteException ex) {
            return "No hay stock suficiente: " + ex.getMessage();
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        } catch (Exception ex) {
            return "Algo salió mal al procesar tu pedido. Intenta de nuevo o hazlo desde la pantalla correspondiente.";
        }
    }

    private String consultarStock(JsonNode params) {
        String termino = params.path("producto").asString();
        List<Producto> encontrados = productoRepository.buscar(termino);

        if (encontrados.isEmpty()) {
            return "No encontré ningún producto que coincida con \"" + termino + "\".";
        }
        if (encontrados.size() > 1) {
            return "Encontré varios productos con ese nombre:\n" + listarProductos(encontrados) +
                "\n¿Cuál te interesa? Sé más específico.";
        }

        Producto p = encontrados.get(0);
        return "%s (%s): %d unidades en stock%s"
            .formatted(p.getNombre(), p.getCodigo(), p.getCantidadStock(),
                p.tieneBajoStock() ? " ⚠️ está en stock bajo" : "");
    }

    private String registrarVenta(JsonNode params) {
        List<ItemVentaRequest> items = new ArrayList<>();
        List<String> resumen = new ArrayList<>();

        for (JsonNode item : params.path("items")) {
            String nombreProducto = item.path("producto").asString();
            int cantidad = item.path("cantidad").asInt();

            List<Producto> encontrados = productoRepository.buscar(nombreProducto);
            if (encontrados.isEmpty()) {
                return "No encontré el producto \"" + nombreProducto + "\". No registré nada — corrígelo e intenta de nuevo.";
            }
            if (encontrados.size() > 1) {
                return "Hay varios productos que coinciden con \"" + nombreProducto + "\":\n" + listarProductos(encontrados) +
                    "\nSé más específico. No registré nada.";
            }

            Producto p = encontrados.get(0);
            items.add(new ItemVentaRequest(p.getId(), cantidad));
            resumen.add(cantidad + " × " + p.getNombre());
        }

        String metodoPagoTexto = params.path("metodo_pago").asString("EFECTIVO");
        var venta = ventaService.crear(new VentaCreateRequest(null, metodoPagoTexto, items, null));

        return "Venta registrada ✅\n" + String.join("\n", resumen) +
            "\nTotal: S/ " + venta.montoTotal();
    }

    private String agendarServicio(JsonNode params) {
        String nombreCliente = params.path("cliente").asString();
        List<Cliente> encontrados = clienteRepository.buscar(nombreCliente);

        if (encontrados.isEmpty()) {
            return "No encontré ningún cliente llamado \"" + nombreCliente + "\". Regístralo primero.";
        }
        if (encontrados.size() > 1) {
            return "Hay varios clientes que coinciden con \"" + nombreCliente + "\": " +
                encontrados.stream().map(Cliente::getNombre).reduce((a, b) -> a + ", " + b).orElse("") +
                ". Sé más específico.";
        }

        Cliente cliente = encontrados.get(0);
        var request = new ServicioDomicilioRequest(
            cliente.getId(),
            params.path("direccion").asString(),
            cliente.getTelefono(),
            LocalDate.parse(params.path("fecha").asString()),
            LocalTime.parse(params.path("hora").asString()),
            params.path("tipo_servicio").asString(),
            "Agendado por el asistente de IA"
        );

        var servicio = servicioDomicilioService.crear(request);
        return "Servicio agendado ✅\n%s — %s\n%s a las %s"
            .formatted(cliente.getNombre(), servicio.tipoServicio(), servicio.fecha(), servicio.hora());
    }

    private String consultarTrabajosPendientes() {
        long pendientes = ordenTrabajoRepository.countByEstadoNotIn(EstadoOrden.FINALES);
        long listos = ordenTrabajoRepository.countByEstado(EstadoOrden.LISTO);
        return "Trabajos pendientes: %d\nDe esos, %d ya están listos para entregar.".formatted(pendientes, listos);
    }

    private String listarProductos(List<Producto> productos) {
        return productos.stream()
            .limit(5)
            .map(p -> "• " + p.getNombre() + " (" + p.getCodigo() + ")")
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
    }

    private void autenticarComo(Usuario usuario) {
        var authorities = List.of(new SimpleGrantedAuthority(usuario.getRole().name()));
        var auth = new UsernamePasswordAuthenticationToken(usuario.getUsername(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
