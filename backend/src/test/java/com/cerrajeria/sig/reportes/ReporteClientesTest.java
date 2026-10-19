package com.cerrajeria.sig.reportes;

import com.cerrajeria.sig.domicilio.ServicioDomicilioRepository;
import com.cerrajeria.sig.inventario.ProductoRepository;
import com.cerrajeria.sig.ordenes.OrdenTrabajoRepository;
import com.cerrajeria.sig.ordenes.PagoOrdenRepository;
import com.cerrajeria.sig.ventas.DetalleVentaRepository;
import com.cerrajeria.sig.ventas.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReporteService - ranking de clientes (RF-44)")
class ReporteClientesTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private PagoOrdenRepository pagoOrdenRepository;
    @Mock private ServicioDomicilioRepository servicioDomicilioRepository;
    @Mock private OrdenTrabajoRepository ordenTrabajoRepository;

    @InjectMocks
    private ReporteService reporteService;

    private static TotalAgrupadoProjection fila(long clave, String nombre, long cantidad, String total) {
        return new TotalAgrupadoProjection() {
            public Long getClave() { return clave; }
            public String getNombre() { return nombre; }
            public Long getCantidad() { return cantidad; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    @Test
    @DisplayName("Suma ventas, taller y domicilio por cliente, ordena de mayor a menor y recorta al límite")
    void combinaLasTresFuentes() {
        when(ventaRepository.comprasPorCliente(any(), any()))
            .thenReturn(List.of(fila(1, "Ana", 2, "30.00"), fila(2, "Beto", 1, "500.00")));
        when(pagoOrdenRepository.pagadoPorCliente(any(), any()))
            .thenReturn(List.of(fila(1, "Ana", 1, "150.00"), fila(3, "Carla", 1, "40.00")));
        when(servicioDomicilioRepository.cobradoPorCliente(any(), any()))
            .thenReturn(List.of(fila(1, "Ana", 1, "80.00")));
        when(ventaRepository.totalSinClienteEntreFechas(any(), any())).thenReturn(new BigDecimal("999.00"));

        ReporteClientesResponse r = reporteService.reporteClientes(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 2);

        assertThat(r.clientesConMovimiento()).isEqualTo(3);
        assertThat(r.ventasSinCliente()).isEqualByComparingTo("999.00");
        assertThat(r.ranking()).extracting(ReporteClientesResponse.Cliente::nombre).containsExactly("Beto", "Ana");
        ReporteClientesResponse.Cliente ana = r.ranking().get(1);
        assertThat(ana.total()).isEqualByComparingTo("260.00");
        assertThat(ana.taller().cantidad()).isEqualTo(1);
        assertThat(ana.domicilio().total()).isEqualByComparingTo("80.00");
        ReporteClientesResponse.Cliente beto = r.ranking().get(0);
        assertThat(beto.taller().cantidad()).as("sin órdenes").isZero();
        assertThat(beto.domicilio().total()).isEqualByComparingTo("0");
    }
}
