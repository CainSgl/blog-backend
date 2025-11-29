package com.cainsgl.common.exception;

import lombok.Getter;

@Getter
public class BSystemException extends RuntimeException
{
    private static final Integer DEFAULT_ERROR_CODE = 501;
    private final Integer errorCode;
    public BSystemException(String message, Integer errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public BSystemException(String message)
    {
        this(message, DEFAULT_ERROR_CODE);
    }
}
