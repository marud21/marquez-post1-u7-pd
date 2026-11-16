package com.example.multas.service;

import com.example.multas.model.EstadoMulta;
import com.example.multas.model.LimiteMultasPendientesException;
import com.example.multas.model.Multa;
import com.example.multas.repository.MultaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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

    @BeforeEach
    void setUp() {
        repository = mock(MultaRepository.class);
        when(repository.save(any(Multa.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new MultaService(repository);
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
}
