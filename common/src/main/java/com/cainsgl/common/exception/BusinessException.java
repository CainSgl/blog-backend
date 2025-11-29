package com.cainsgl.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException
{
    private static final int DEFAULT_ERROR_CODE = 501;
    private final Integer errorCode;
    private final Object data;
    public BusinessException(String message)
    {
        this(DEFAULT_ERROR_CODE, message);
    }
    public BusinessException(int errorCode, String message)
    {
        this(errorCode, message,null);
    }
    public BusinessException(int errorCode, String message,Object data)
    {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }
}
