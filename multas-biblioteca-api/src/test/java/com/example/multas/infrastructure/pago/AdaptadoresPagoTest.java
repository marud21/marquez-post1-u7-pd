package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.model.Multa;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Verifica que cada adaptador traduce su formato HTTP propio al mismo ResultadoPago. */
class AdaptadoresPagoTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

    private Multa multa() {
        Multa multa = new Multa("1152115", "Retraso", 4); // monto 2000
        multa.setId(7L);
        return multa;
    }

    @Test
    void pagosUdesTraduceIdTransaccionYEstado() {
        server.expect(requestTo("http://pu/tx")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"estudianteId\":\"1152115\",\"monto\":2000}"))
                .andRespond(withSuccess("{\"idTransaccion\":\"PU-1\",\"estadoTransaccion\":\"APROBADA\"}", MediaType.APPLICATION_JSON));

        ResultadoPago r = new PagosUdesAdapter(restTemplate, "http://pu/tx").procesar(multa());

        assertThat(r).isEqualTo(new ResultadoPago("PAGOSUDES", true, "PU-1", "Pago aprobado por PagosUDES"));
    }

    @Test
    void wompiEnviaCentavosYTraduceReferenceStatus() {
        server.expect(requestTo("http://wompi/tx"))
                .andExpect(content().json("{\"reference\":\"multa-7\",\"amountInCents\":200000}"))
                .andRespond(withSuccess("{\"reference\":\"multa-7\",\"status\":\"DECLINED\"}", MediaType.APPLICATION_JSON));

        ResultadoPago r = new WompiAdapter(restTemplate, "http://wompi/tx").procesar(multa());

        assertThat(r.exitoso()).isFalse();
        assertThat(r.proveedor()).isEqualTo("WOMPI");
        assertThat(r.referenciaExterna()).isEqualTo("multa-7");
    }

    @Test
    void pasarelaCaidaSeTraduceAResultadoNoExitoso() {
        server.expect(requestTo("http://pu/tx")).andRespond(withServerError());

        ResultadoPago r = new PagosUdesAdapter(restTemplate, "http://pu/tx").procesar(multa());

        assertThat(r.exitoso()).isFalse();
        assertThat(r.mensaje()).startsWith("PagosUDES no disponible");
    }
}
