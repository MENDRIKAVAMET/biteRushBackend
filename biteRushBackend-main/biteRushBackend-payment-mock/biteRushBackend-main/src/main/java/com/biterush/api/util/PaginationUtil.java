package com.biterush.api.util;

import org.springframework.data.domain.Page;

public class PaginationUtil {
    public static <T> PageResponse<T> mapPageToResponse(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast(),
            page.isFirst()
        );
    }
}
