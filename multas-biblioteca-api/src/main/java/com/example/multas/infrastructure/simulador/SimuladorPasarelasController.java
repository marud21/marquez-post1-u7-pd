package com.example.multas.infrastructure.simulador;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Simulador local de las dos pasarelas (solo con el perfil "simulador").
 * Permite probar el flujo completo sin servicios externos. Regla del simulador:
 * aprueba montos menores a 10.000 COP y rechaza los demas, para poder evidenciar el 402.
 * No forma parte del dominio: es un doble de prueba de infraestructura.
 */
@RestController
@Profile("simulador")
@RequestMapping("/simulador")
public class SimuladorPasarelasController {

    private static final BigDecimal LIMITE_APROBACION = new BigDecimal("10000");

    @PostMapping("/pagosudes/transacciones")
    public Map<String, String> pagosUdes(@RequestBody Map<String, Object> body) {
        BigDecimal monto = new BigDecimal(String.valueOf(body.get("monto")));
        String estado = monto.compareTo(LIMITE_APROBACION) < 0 ? "APROBADA" : "RECHAZADA";
        return Map.of("idTransaccion", "PU-" + UUID.randomUUID().toString().substring(0, 8), "estadoTransaccion", estado);
    }

    @PostMapping("/wompi/transactions")
    public Map<String, String> wompi(@RequestBody Map<String, Object> body) {
        long centavos = Long.parseLong(String.valueOf(body.get("amountInCents")));
        String status = centavos < LIMITE_APROBACION.longValue() * 100 ? "APPROVED" : "DECLINED";
        return Map.of("reference", String.valueOf(body.get("reference")), "status", status);
    }
}
