package com.kkphoenixgx.Application;

import com.kkphoenixgx.domain.ports.out.PagesRepositoryPort;
import com.kkphoenixgx.domain.service.PagesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public PagesService pagesService(PagesRepositoryPort pagesRepositoryPort) {
        return new PagesService(pagesRepositoryPort);
    }
}