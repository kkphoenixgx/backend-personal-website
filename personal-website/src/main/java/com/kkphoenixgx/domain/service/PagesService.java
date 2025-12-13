package com.kkphoenixgx.domain.service;

import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.in.PagesServicePort;
import com.kkphoenixgx.domain.ports.out.PagesRepositoryPort;

import java.util.List;

public class PagesService implements PagesServicePort {

    private final PagesRepositoryPort pagesRepositoryPort;

    public PagesService(PagesRepositoryPort pagesRepositoryPort) {
        this.pagesRepositoryPort = pagesRepositoryPort;
    }

    @Override
    public List<Pages> getPages() {
        return pagesRepositoryPort.listStaticPages();
    }
}