package com.cerrajeria.sig.reportes;

import com.cerrajeria.sig.domicilio.EstadoServicio;
import com.cerrajeria.sig.domicilio.ServicioDomicilioRepository;
import com.cerrajeria.sig.inventario.ProductoRepository;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReporteService - ingresos de ventas, taller y domicilio")
class ReporteServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private PagoOrdenRepository pagoOrdenRepository;
    @Mock private ServicioDomicilioRepository servicioDomicilioRepository;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    @DisplayName("Ingresos totales = ventas + taller (neto) + domicilio, en el rango completo de días")
    void sumaLasTresFuentes() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);
        when(ventaRepository.totalEntreFechas(inicio, fin)).thenReturn(new BigDecimal("500.00"));
        when(ventaRepository.contarEntreFechas(inicio, fin)).thenReturn(12L);
        when(ventaRepository.totalPorMetodoPago(inicio, fin)).thenReturn(List.of());
        when(pagoOrdenRepository.ingresosNetosEntre(inicio, fin)).thenReturn(new BigDecimal("230.00"));
        when(servicioDomicilioRepository.cobradoEntre(inicio, fin)).thenReturn(new BigDecimal("160.00"));
        when(servicioDomicilioRepository.countByEstadoAndFechaRealizadoBetween(eq(EstadoServicio.REALIZADO), any(), any()))
            .thenReturn(2L);

        ReporteVentasResponse r = reporteService.reporteVentas(desde, hasta);

        assertThat(r.totalVentas()).isEqualByComparingTo("500.00");
        assertThat(r.ingresosTaller()).isEqualByComparingTo("230.00");
        assertThat(r.ingresosDomicilio()).isEqualByComparingTo("160.00");
        assertThat(r.serviciosDomicilioRealizados()).isEqualTo(2L);
        assertThat(r.ingresosTotales()).isEqualByComparingTo("890.00");
    }
}
