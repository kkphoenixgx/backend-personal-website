package com.kkphoenixgx.domain.service;

import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.in.PagesServicePort;
import com.kkphoenixgx.domain.ports.out.PagesRepositoryPort;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PagesService implements PagesServicePort {

    private final PagesRepositoryPort pagesRepositoryPort;

    public PagesService(PagesRepositoryPort pagesRepositoryPort) {
        this.pagesRepositoryPort = pagesRepositoryPort;
    }

    @Override
    public List<Pages> getPages() {
        return pagesRepositoryPort.listStaticPages().stream()
                .filter(page -> Arrays.asList("Programing", "RPG", "Study").contains(page.getTitle()))
                .collect(Collectors.toList());
    }
}