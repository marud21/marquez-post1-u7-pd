package com.example.multas.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MultaControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private static String json(String estudiante, int dias) {
        return """
                {"estudianteId":"%s","concepto":"Devolucion tardia","diasAtraso":%d}
                """.formatted(estudiante, dias);
    }

    @Test
    void flujoCompletoDeLaParte1() throws Exception {
        mvc.perform(get("/api/multas")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("1152115", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(2000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("1152115", 2)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("1152115", 50)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(15000));
        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON).content(json("1152115", 1)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/multas/99")).andExpect(status().isNotFound());

        mvc.perform(patch("/api/multas/1/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.metodoPago").value("VENTANILLA"));
        mvc.perform(patch("/api/multas/1/pagar")).andExpect(status().isConflict());
    }

    @Test
    void validacionRetorna400() throws Exception {
        mvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"x\",\"diasAtraso\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estudianteId").exists())
                .andExpect(jsonPath("$.diasAtraso").exists());
    }
}
