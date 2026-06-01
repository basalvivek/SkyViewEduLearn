package com.edulearn.util;

import org.springframework.data.domain.Page;

public record ApiResponse<T>(String status, String message, T data, PageMeta pagination) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "OK", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data, null);
    }

    public static <T> ApiResponse<T> paged(T data, Page<?> page) {
        return new ApiResponse<>("success", "OK", data,
            new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>("error", message, null, null);
    }
}
