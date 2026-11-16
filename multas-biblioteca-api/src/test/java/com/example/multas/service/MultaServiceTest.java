package com.example.multas.service;

import com.example.multas.domain.PagoRechazadoException;
import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.model.EstadoMulta;
import com.example.multas.model.LimiteMultasPendientesException;
import com.example.multas.model.Multa;
import com.example.multas.model.MultaYaPagadaException;
import com.example.multas.repository.MultaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultaServiceTest {

    private MultaRepository repository;
    private MultaService service;
    private ResultadoPago respuestaPasarela;

    @BeforeEach
    void setUp() {
        repository = mock(MultaRepository.class);
        when(repository.save(any(Multa.class))).thenAnswer(inv -> inv.getArgument(0));
        // El puerto se sustituye por una lambda: el dominio se prueba sin Spring ni HTTP
        PasarelaPagoPort pasarelaFalsa = multa -> respuestaPasarela;
        service = new MultaService(repository, pasarelaFalsa);
    }

    @Test
    void generarCalculaElMontoConLaReglaDeLaEntidad() {
        when(repository.countByEstudianteIdAndEstado("E1", EstadoMulta.PENDIENTE)).thenReturn(0L);

        Multa multa = service.generar("E1", "Retraso", 3);

        assertThat(multa.getMonto()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(multa.getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
    }

    @Test
    void generarRechazaCuandoSeAlcanzaElLimiteDePendientes() {
        when(repository.countByEstudianteIdAndEstado("E1", EstadoMulta.PENDIENTE)).thenReturn(3L);

        assertThatThrownBy(() -> service.generar("E1", "Retraso", 3))
                .isInstanceOf(LimiteMultasPendientesException.class)
                .hasMessageContaining("3 multas pendientes");
        verify(repository, never()).save(any());
    }

    @Test
    void pagarConPasarelaAprobadaMarcaLaMultaConElProveedor() {
        Multa multa = new Multa("E1", "Retraso", 2);
        when(repository.findById(1L)).thenReturn(Optional.of(multa));
        respuestaPasarela = new ResultadoPago("WOMPI", true, "multa-1", "ok");

        Multa pagada = service.pagarConPasarela(1L);

        assertThat(pagada.getEstado()).isEqualTo(EstadoMulta.PAGADA);
        assertThat(pagada.getMetodoPago()).isEqualTo("WOMPI");
    }

    @Test
    void pagarConPasarelaRechazadaLanzaPagoRechazado() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Multa("E1", "Retraso", 2)));
        respuestaPasarela = new ResultadoPago("PAGOSUDES", false, null, "Transaccion rechazada por PagosUDES");

        assertThatThrownBy(() -> service.pagarConPasarela(1L))
                .isInstanceOf(PagoRechazadoException.class)
                .hasMessage("Transaccion rechazada por PagosUDES");
    }

    @Test
    void pagarConPasarelaUnaMultaYaPagadaLanzaConflicto() {
        Multa multa = new Multa("E1", "Retraso", 2);
        multa.marcarComoPagada("VENTANILLA");
        when(repository.findById(1L)).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> service.pagarConPasarela(1L)).isInstanceOf(MultaYaPagadaException.class);
    }
}
