package com.kkphoenixgx.domain.service;

import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.out.PagesRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagesServiceTest {

    @Mock
    private PagesRepositoryPort pagesRepositoryPort;

    private PagesService pagesService;

    private List<Pages> allPages;

    @BeforeEach
    void setUp() {
        pagesService = new PagesService(pagesRepositoryPort, Arrays.asList("Programing", "RPG", "Study"));
        
        allPages = Arrays.asList(
            new Pages("Programing", "/Programing"),
            new Pages("RPG", "/RPG"),
            new Pages("Study", "/Study"),
            new Pages("Personal", "/Personal"),
            new Pages("Another", "/Another")
        );
    }

    @Test
    void whenGetPages_thenShouldReturnFilteredPages() {
        when(pagesRepositoryPort.listStaticPages()).thenReturn(allPages);

        List<Pages> result = pagesService.getPages();

        List<String> resultTitles = result.stream().map(Pages::getTitle).collect(Collectors.toList());
        List<String> expectedTitles = Arrays.asList("Programing", "RPG", "Study");

        assertEquals(3, result.size());
        assertEquals(expectedTitles, resultTitles);
    }
}