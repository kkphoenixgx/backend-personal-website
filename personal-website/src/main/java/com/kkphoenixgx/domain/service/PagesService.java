package com.kkphoenixgx.domain.service;

import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.in.PagesServicePort;
import com.kkphoenixgx.domain.ports.out.PagesRepositoryPort;

import java.util.List;
import java.util.stream.Collectors;

public class PagesService implements PagesServicePort {

    private final PagesRepositoryPort pagesRepositoryPort;
    private final List<String> allowedRoots;

    public PagesService(PagesRepositoryPort pagesRepositoryPort, List<String> allowedRoots) {
        this.pagesRepositoryPort = pagesRepositoryPort;
        this.allowedRoots = allowedRoots;
    }

    @Override
    public List<Pages> getPages() {
        return pagesRepositoryPort.listStaticPages().stream()
                .filter(page -> allowedRoots.contains(page.getTitle()))
                .collect(Collectors.toList());
    }
}