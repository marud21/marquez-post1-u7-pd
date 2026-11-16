package com.example.multas.domain;

/**
 * Tipo de dominio neutral respecto a los proveedores: ningun DTO de PagosUDES
 * o Wompi atraviesa el puerto, solo este contrato comun (Punto de decision 4).
 */
public record ResultadoPago(
        String proveedor,
        boolean exitoso,
        String referenciaExterna,
        String mensaje
) {
}
