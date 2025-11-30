package com.cainsgl.common.config

import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.common.util.TraceIdUtils
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler
{
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): Result
    {
        log.error("系统异常", e)
        return Result.error(e)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): Result
    {
        return Result.error(e)
    }

    @ExceptionHandler(BSystemException::class)
    fun handleSystemExceptionException(e: BSystemException): Result
    {
        return Result.error(e)
    }
}
