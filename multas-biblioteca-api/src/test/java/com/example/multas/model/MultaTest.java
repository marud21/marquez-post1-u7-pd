package com.example.multas.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultaTest {

    @Test
    void calcularMontoMultiplicaDiasPorValorDiario() {
        assertThat(Multa.calcularMonto(4)).isEqualByComparingTo(new BigDecimal("2000"));
    }

    @Test
    void calcularMontoRespetaElTopeMaximo() {
        assertThat(Multa.calcularMonto(40)).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    void marcarComoPagadaDosVecesLanzaExcepcion() {
        Multa multa = new Multa("1152115", "Libro devuelto tarde", 2);
        multa.marcarComoPagada("VENTANILLA");

        assertThat(multa.getEstado()).isEqualTo(EstadoMulta.PAGADA);
        assertThat(multa.getMetodoPago()).isEqualTo("VENTANILLA");
        assertThatThrownBy(() -> multa.marcarComoPagada("VENTANILLA"))
                .isInstanceOf(MultaYaPagadaException.class);
    }
}
