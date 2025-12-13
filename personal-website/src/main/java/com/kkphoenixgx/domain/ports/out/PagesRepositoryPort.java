package com.kkphoenixgx.domain.ports.out;

import com.kkphoenixgx.domain.model.Pages;

import java.util.List;

public interface PagesRepositoryPort {
    List<Pages> listStaticPages();
}