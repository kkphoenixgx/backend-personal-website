package com.kkphoenixgx.infrastructure.web;

import com.kkphoenixgx.Application.App;
import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.in.PagesServicePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = App.class)
@AutoConfigureMockMvc
class SeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagesServicePort pagesServicePort;

    @Test
    void getSitemap_shouldReturnXmlWithCleanUrls() throws Exception {
        Pages root = new Pages("RPG", "/RPG/index.html");
        root.getItems().add(new Pages("Personagens", "/RPG/personagens.html"));

        when(pagesServicePort.getPages()).thenReturn(List.of(root));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://api-personalwebsite.kkphoenix.com.br/</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://api-personalwebsite.kkphoenix.com.br/RPG/</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://api-personalwebsite.kkphoenix.com.br/RPG/personagens</loc>")));
    }

    @Test
    void getRobotsTxt_shouldReturnText() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sitemap: https://api-personalwebsite.kkphoenix.com.br/sitemap.xml")));
    }
}