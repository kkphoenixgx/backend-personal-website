package com.kkphoenixgx.Application;

import com.kkphoenixgx.domain.ports.out.PagesRepositoryPort;
import com.kkphoenixgx.domain.service.PagesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class BeanConfiguration {

    @Bean
    public PagesService pagesService(PagesRepositoryPort pagesRepositoryPort, 
                                     @Value("${app.pages.allowed-roots:Programing,RPG,Study}") List<String> allowedRoots) {
        return new PagesService(pagesRepositoryPort, allowedRoots);
    }
}