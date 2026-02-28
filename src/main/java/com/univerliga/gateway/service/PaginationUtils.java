package com.univerliga.gateway.service;

import com.univerliga.gateway.dto.PageDto;

import java.util.List;

public final class PaginationUtils {
    private PaginationUtils() {
    }

    public static <T> List<T> slice(List<T> source, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = (safePage - 1) * safeSize;
        if (from >= source.size()) {
            return List.of();
        }
        int to = Math.min(source.size(), from + safeSize);
        return source.subList(from, to);
    }

    public static PageDto page(List<?> source, int page, int size) {
        int safeSize = Math.max(size, 1);
        int totalPages = (int) Math.ceil(source.size() / (double) safeSize);
        return new PageDto(Math.max(page, 1), safeSize, source.size(), totalPages);
    }
}
