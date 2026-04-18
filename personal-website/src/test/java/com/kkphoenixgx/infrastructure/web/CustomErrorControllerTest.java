package com.kkphoenixgx.infrastructure.web;

import com.kkphoenixgx.Application.App;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = App.class)
@AutoConfigureMockMvc
class CustomErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void handleError_whenNotFoundInRpg_shouldRedirectToRpg404() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/RPG/non-existent-page"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/RPG/404.html"));
    }

    @Test
    void handleError_whenNotFoundInStudy_shouldRedirectToStudy404() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/Study/another/non-existent/page"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/Study/404.html"));
    }

    @Test
    void handleError_whenNotFoundInRoot_shouldReturnDefaultErrorPage() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/non-existent-root-page"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    @Test
    void handleError_whenSyncConflictInRpgThrows404_shouldRedirectToRpg404() throws Exception {
        // Valida se arquivos sujos do git conflict estão caindo na página de erro correspondente ao RPG
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/RPG/-Excalidraw/Jwons.sync-conflict-20241101-230301-SCFHCJ2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/RPG/404.html"));
    }
}