package com.mahaoyang.maaiagent.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mahaoyang.mapicturebackend.exception.ErrorCode;

/**
 * 全局响应封装类
 * @param <T>
 */
@Data
@AllArgsConstructor
public class BaseResponse<T> {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
