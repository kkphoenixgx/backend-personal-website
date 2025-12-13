package com.kkphoenixgx.infrastructure.web;

import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.in.PagesServicePort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pages/")
public class PagesController {

    private final PagesServicePort pagesServicePort;

    public PagesController(PagesServicePort pagesServicePort) {
        this.pagesServicePort = pagesServicePort;
    }

    @GetMapping
    public ResponseEntity<List<Pages>> listAllPages() {
        List<Pages> pages = pagesServicePort.getPages();
        HttpHeaders headers = new HttpHeaders();
        if (pages.isEmpty()) {
            headers.add("X-Sync-Status", "PENDING");
        } else {
            headers.add("X-Sync-Status", "COMPLETE");
        }
        return ResponseEntity.ok().headers(headers).body(pages);
    }
}