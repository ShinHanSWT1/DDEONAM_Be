package com.gorani.ecodrive.common.response;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "COMMON_SUCCESS", "요청이 성공했습니다.", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "COMMON_SUCCESS", message, data);
    }
}