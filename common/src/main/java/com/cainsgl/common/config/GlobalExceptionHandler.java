package com.cainsgl.common.config;

import com.cainsgl.common.dto.response.Result;
import com.cainsgl.common.exception.BusinessException;
import com.cainsgl.common.exception.BSystemException;
import com.cainsgl.common.util.TraceIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请联系管理员",e.getMessage(), TraceIdUtils.getTraceId());
    }
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e);
    }
    @ExceptionHandler(BSystemException.class)
    public Result handleSystemExceptionException(BSystemException e) {
        return Result.error(e);
    }
}
