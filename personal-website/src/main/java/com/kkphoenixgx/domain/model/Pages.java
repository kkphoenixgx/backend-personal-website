package com.kkphoenixgx.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Pages {
    private final String title;
    private final String path;

    public Pages(String title, String path) {
        this.title = title;
        this.path = path;
    }
}