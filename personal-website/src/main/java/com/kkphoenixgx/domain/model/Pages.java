package com.kkphoenixgx.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class Pages {
    private final String title;
    private final String path;
    private final List<Pages> items;

    public Pages(String title, String path) {
        this.title = title;
        this.path = path;
        this.items = new ArrayList<>();
    }
}