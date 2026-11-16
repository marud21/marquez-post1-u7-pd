package com.example.multas.controller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de extremo a extremo contra el simulador embebido (perfil "simulador"),
 * una por cada valor de app.pagos.proveedor, sin tocar Controller ni Service.
 */
class PagoEnLineaIntegrationTest {

    private static Long crearMulta(TestRestTemplate http, String estudiante, int dias) {
        Map<?, ?> body = http.postForObject("/api/multas",
                Map.of("estudianteId", estudiante, "concepto", "Devolucion tardia", "diasAtraso", dias), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
            properties = {"server.port=18071", "app.pagos.proveedor=pagosudes"})
    @ActiveProfiles("simulador")
    class ConPagosUdes {
        @Autowired
        TestRestTemplate http;

        @Test
        void apruebaRechazaYDetectaDoblePago() {
            Long aprobada = crearMulta(http, "PU-1", 4);    // 2.000 COP -> aprobada
            Long rechazada = crearMulta(http, "PU-1", 30);  // 15.000 COP -> rechazada por el simulador

            ResponseEntity<Map> ok = http.postForEntity("/api/multas/" + aprobada + "/pagar-en-linea", null, Map.class);
            assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(ok.getBody()).containsEntry("metodoPago", "PAGOSUDES").containsEntry("estado", "PAGADA");

            ResponseEntity<Map> rechazo = http.postForEntity("/api/multas/" + rechazada + "/pagar-en-linea", null, Map.class);
            assertThat(rechazo.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);

            ResponseEntity<Map> doble = http.postForEntity("/api/multas/" + aprobada + "/pagar-en-linea", null, Map.class);
            assertThat(doble.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
            properties = {"server.port=18072", "app.pagos.proveedor=wompi"})
    @ActiveProfiles("simulador")
    class ConWompi {
        @Autowired
        TestRestTemplate http;

        @Test
        void elMismoEndpointUsaWompiSoloCambiandoLaPropiedad() {
            Long id = crearMulta(http, "W-1", 2);
            ResponseEntity<Map> ok = http.postForEntity("/api/multas/" + id + "/pagar-en-linea", null, Map.class);
            assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(ok.getBody()).containsEntry("metodoPago", "WOMPI");
        }
    }
}
