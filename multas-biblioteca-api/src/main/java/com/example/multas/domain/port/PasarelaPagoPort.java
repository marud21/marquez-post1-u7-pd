package com.example.multas.domain.port;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.model.Multa;

/**
 * Puerto de salida (driven port) definido por el nucleo de la aplicacion.
 * Java puro: no conoce Spring, ni HTTP, ni el formato de ninguna pasarela concreta.
 * Los adaptadores de infrastructure/pago dependen de esta abstraccion (DIP).
 */
public interface PasarelaPagoPort {

    ResultadoPago procesar(Multa multa);
}
