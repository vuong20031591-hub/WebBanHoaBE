package com.florastore.web_ban_hoa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    @JsonProperty("number")
    public int number() {
        return currentPage;
    }

    @JsonProperty("first")
    public boolean first() {
        return currentPage == 0;
    }

    @JsonProperty("last")
    public boolean last() {
        return totalPages == 0 || currentPage >= totalPages - 1;
    }
}
