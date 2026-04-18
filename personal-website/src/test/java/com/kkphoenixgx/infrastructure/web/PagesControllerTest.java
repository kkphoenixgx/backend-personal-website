package com.kkphoenixgx.infrastructure.web;

import com.kkphoenixgx.Application.App;
import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.in.PagesServicePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = App.class)
@AutoConfigureMockMvc
class PagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagesServicePort pagesServicePort;

    @Test
    void listAllPages_whenSyncIsComplete_shouldReturnPagesAndCompleteHeader() throws Exception {
        List<Pages> pages = List.of(new Pages("RPG", "/RPG"));
        when(pagesServicePort.getPages()).thenReturn(pages);

        mockMvc.perform(get("/api/pages/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Sync-Status", "COMPLETE"))
                .andExpect(jsonPath("$[0].title").value("RPG"));
    }

    @Test
    void listAllPages_whenSyncIsPending_shouldReturnEmptyAndPendingHeader() throws Exception {
        when(pagesServicePort.getPages()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/pages/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Sync-Status", "PENDING"))
                .andExpect(jsonPath("$").isEmpty());
    }
}