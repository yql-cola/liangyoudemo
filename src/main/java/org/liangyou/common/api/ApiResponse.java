package org.liangyou.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一接口响应")
public class ApiResponse<T> {

    @Schema(description = "业务状态码，0 表示成功", example = "0")
    private final int code;
    @Schema(description = "响应消息", example = "success")
    private final String message;
    @Schema(description = "响应数据体")
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
